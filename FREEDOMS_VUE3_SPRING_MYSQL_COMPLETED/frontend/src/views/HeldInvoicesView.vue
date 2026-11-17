<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'

const route = useRoute()
const prefix = computed(() => route.path.startsWith('/admin') ? '/admin' : '/employee')
const sessions = ref([])
const orders = ref([])
const paymentConfig = ref(null)
const loading = ref(false)
const error = ref('')
const search = ref('')
const activeTab = ref('all')
const selectedQrOrder = ref(null)

const pendingQrOrders = computed(() => orders.value.filter(order =>
  String(order.paymentMethod || '').toUpperCase() === 'BANK_TRANSFER' &&
  ['PENDING', 'WAITING_CONFIRMATION'].includes(String(order.paymentStatus || '').toUpperCase()) &&
  !['CANCELLED', 'COMPLETED'].includes(String(order.status || '').toUpperCase())
))
const unfinishedSessions = computed(() => {
  const q = search.value.trim().toLocaleLowerCase('vi-VN')
  return sessions.value.filter(session => {
    if (!q) return true
    return [session.code, session.customerName, session.customerPhone]
      .some(value => String(value || '').toLocaleLowerCase('vi-VN').includes(q))
  })
})
const filteredQrOrders = computed(() => {
  const q = search.value.trim().toLocaleLowerCase('vi-VN')
  return pendingQrOrders.value.filter(order => {
    if (!q) return true
    return [order.code, order.customerName, order.customerPhone, order.paymentStatus]
      .some(value => String(value || '').toLocaleLowerCase('vi-VN').includes(q))
  })
})
const emptySessionCount = computed(() => sessions.value.filter(s => Number(s.itemCount || 0) === 0).length)
const qrUrl = computed(() => {
  const order = selectedQrOrder.value
  const bank = paymentConfig.value
  if (!order || !bank?.bankCode || !bank?.bankAccount) return ''
  const query = new URLSearchParams({
    amount: String(Math.round(Number(order.totalAmount || 0))),
    addInfo: order.code,
    accountName: bank.bankAccountName || 'FREEDOMS SHOE STORE'
  })
  return `https://img.vietqr.io/image/${bank.bankCode}-${bank.bankAccount}-compact2.png?${query}`
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [sessionResult, orderResult] = await Promise.all([
      api.get('/api/pos/sessions'),
      api.get('/api/pos/orders')
    ])
    sessions.value = sessionResult.data || []
    orders.value = orderResult.data || []
    try { paymentConfig.value = (await api.get('/api/store/payment-config')).data } catch (_) { paymentConfig.value = null }
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
function continueSession(sessionId) {
  return { path: `${prefix.value}/pos`, query: { sessionId: String(sessionId) } }
}
function orderManagementPath(order) { return { path: `${prefix.value}/orders`, query: { orderCode: order?.code || '', status: order?.paymentStatus || '' } } }
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')}₫` }
function dateTime(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—' }
function sessionState(session) {
  return Number(session.itemCount || 0) === 0 ? 'CHƯA CHỌN SẢN PHẨM' : 'ĐANG CHỌN SẢN PHẨM'
}
function showQr(order) { selectedQrOrder.value = order }
function closeQr() { selectedQrOrder.value = null }
onMounted(load)
</script>

<template>
  <div class="view-wrap held-invoices-view">
    <div class="view-heading">
      <div>
        <span class="eyebrow">POS / HELD INVOICES</span>
        <h1>Hóa đơn chờ POS</h1>
        <p>Theo dõi phiên khách đang chọn hàng và các đơn chuyển khoản QR chưa được xác nhận. Có thể mở lại đúng phiên hoặc hiển thị lại mã QR.</p>
      </div>
      <div class="held-heading-actions">
        <RouterLink :to="`${prefix}/pos`" class="btn btn-primary">＋ Mở POS</RouterLink>
        <button class="btn btn-outline" :disabled="loading" @click="load">Làm mới</button>
      </div>
    </div>

    <p v-if="error" class="notice notice-error">{{ error }}</p>
    <section class="held-summary-grid">
      <button class="held-summary-card" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
        <small>Tất cả công việc chờ</small><strong>{{ sessions.length + pendingQrOrders.length }}</strong><span>Phiên POS và giao dịch QR</span>
      </button>
      <button class="held-summary-card" :class="{ active: activeTab === 'sessions' }" @click="activeTab = 'sessions'">
        <small>Phiên đang chọn hàng</small><strong>{{ sessions.length }}</strong><span>{{ emptySessionCount }} phiên chưa có sản phẩm</span>
      </button>
      <button class="held-summary-card" :class="{ active: activeTab === 'qr' }" @click="activeTab = 'qr'">
        <small>Đơn QR chưa xác nhận</small><strong>{{ pendingQrOrders.length }}</strong><span>Chờ kiểm tra tiền vào</span>
      </button>
    </section>

    <section class="surface-card held-filter-panel">
      <label>Tìm mã phiên, mã đơn, tên hoặc số điện thoại khách hàng
        <input v-model.trim="search" placeholder="Nhập mã POS, mã đơn, tên khách hoặc số điện thoại…" />
      </label>
      <div class="held-tabs" role="tablist" aria-label="Loại hóa đơn chờ">
        <button :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">Tất cả</button>
        <button :class="{ active: activeTab === 'sessions' }" @click="activeTab = 'sessions'">Phiên đang chọn hàng</button>
        <button :class="{ active: activeTab === 'qr' }" @click="activeTab = 'qr'">Chờ xác nhận QR</button>
      </div>
    </section>

    <section v-if="loading" class="surface-card empty-state">Đang tải hóa đơn chờ và giao dịch POS…</section>

    <section v-if="!loading && activeTab !== 'qr'" class="surface-card held-section">
      <div class="held-section-heading">
        <div><span class="eyebrow">DRAFT POS SESSIONS</span><h2>Phiên khách chưa hoàn tất mua hàng</h2><p>Phiên chưa tạo đơn thanh toán và chưa trừ kho. Phiên chưa có sản phẩm vẫn được giữ để tiếp tục chọn hàng sau.</p></div>
        <span class="held-count">{{ unfinishedSessions.length }} phiên</span>
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead><tr><th>Mã phiên</th><th>Khách hàng</th><th>Số điện thoại</th><th>Sản phẩm</th><th>Tạm tính</th><th>Trạng thái</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
          <tbody>
            <tr v-for="session in unfinishedSessions" :key="session.id">
              <td><b>{{ session.code }}</b></td>
              <td>{{ session.customerName || 'Khách lẻ' }}</td>
              <td>{{ session.customerPhone || '—' }}</td>
              <td>{{ session.itemCount }} món</td>
              <td class="held-money">{{ money(session.subtotal) }}</td>
              <td><span class="held-state-pill" :class="Number(session.itemCount || 0) === 0 ? 'is-empty' : 'is-draft'">{{ sessionState(session) }}</span></td>
              <td>{{ dateTime(session.updatedAt) }}</td>
              <td><RouterLink :to="continueSession(session.id)" class="btn btn-primary btn-sm">{{ Number(session.itemCount || 0) === 0 ? 'Chọn sản phẩm' : 'Tiếp tục POS' }}</RouterLink></td>
            </tr>
            <tr v-if="!unfinishedSessions.length"><td colspan="8" class="empty-state">Không có phiên khách đang chọn hàng phù hợp.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="!loading && activeTab !== 'sessions'" class="surface-card held-section">
      <div class="held-section-heading">
        <div><span class="eyebrow">UNCONFIRMED QR PAYMENTS</span><h2>Đơn POS chờ xác nhận chuyển khoản QR</h2><p>Các đơn đã tạo mã QR nhưng chưa được xác nhận đã nhận tiền. Mở QR để khách quét lại hoặc sang Đơn hàng & thanh toán để đối soát.</p></div>
        <span class="held-count held-count-warning">{{ filteredQrOrders.length }} giao dịch</span>
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Số điện thoại</th><th>Ngày tạo</th><th>Tổng tiền</th><th>Trạng thái thanh toán</th><th>Thao tác</th></tr></thead>
          <tbody>
            <tr v-for="order in filteredQrOrders" :key="order.id">
              <td><b>{{ order.code }}</b><small class="held-subtext">{{ order.status }}</small></td>
              <td>{{ order.customerName || 'Khách lẻ' }}</td>
              <td>{{ order.customerPhone || '—' }}</td>
              <td>{{ dateTime(order.createdAt) }}</td>
              <td class="held-money">{{ money(order.totalAmount) }}</td>
              <td><span class="held-state-pill is-qr">{{ order.paymentStatus === 'WAITING_CONFIRMATION' ? 'KHÁCH ĐÃ BÁO CHUYỂN' : 'CHỜ THANH TOÁN' }}</span></td>
              <td class="held-actions-cell">
                <button class="btn btn-outline btn-sm" @click="showQr(order)">Mở QR</button>
                <RouterLink :to="orderManagementPath(order)" class="btn btn-primary btn-sm">Đối soát tiền</RouterLink>
              </td>
            </tr>
            <tr v-if="!filteredQrOrders.length"><td colspan="7" class="empty-state">Không có đơn POS chuyển khoản QR đang chờ xác nhận.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="!loading && activeTab === 'all' && !sessions.length && !pendingQrOrders.length" class="surface-card empty-state">
      Chưa có phiên POS hoặc giao dịch QR nào đang chờ. Bạn có thể mở POS để bắt đầu một phiên khách mới.
    </section>

    <section v-if="selectedQrOrder" class="modal-backdrop" @click.self="closeQr">
      <div class="surface-card held-qr-modal">
        <button class="modal-close" aria-label="Đóng" @click="closeQr">×</button>
        <span class="eyebrow">POS / QR PAYMENT</span>
        <h2>{{ selectedQrOrder.code }}</h2>
        <div class="held-qr-summary">
          <div><small>Khách hàng</small><b>{{ selectedQrOrder.customerName || 'Khách lẻ' }}</b></div>
          <div><small>Trạng thái</small><b>{{ selectedQrOrder.paymentStatus }}</b></div>
          <div><small>Tổng thanh toán</small><strong>{{ money(selectedQrOrder.totalAmount) }}</strong></div>
        </div>
        <div v-if="qrUrl" class="held-qr-code">
          <img :src="qrUrl" :alt="`Mã QR thanh toán đơn ${selectedQrOrder.code}`" />
          <p>Quét mã bằng ứng dụng ngân hàng và nhập đúng nội dung chuyển khoản <b>{{ selectedQrOrder.code }}</b>.</p>
        </div>
        <p v-else class="notice notice-warning">Chưa cấu hình đầy đủ ngân hàng nhận tiền. Kiểm tra BANK_CODE, BANK_ACCOUNT và BANK_ACCOUNT_NAME trong cấu hình backend.</p>
        <div v-if="paymentConfig?.bankAccount" class="held-bank-details">
          <span>Ngân hàng: <b>{{ paymentConfig.bankCode }}</b></span>
          <span>Số tài khoản: <b>{{ paymentConfig.bankAccount }}</b></span>
          <span>Chủ tài khoản: <b>{{ paymentConfig.bankAccountName }}</b></span>
          <span>Nội dung: <b>{{ selectedQrOrder.code }}</b></span>
        </div>
        <div class="held-qr-items">
          <b>Sản phẩm trong đơn</b>
          <div v-for="(item, index) in selectedQrOrder.items || []" :key="`${selectedQrOrder.id}-${index}`">
            <span>{{ item.productName }} · {{ item.variantLabel }} × {{ item.quantity }}</span><b>{{ money(item.lineTotal) }}</b>
          </div>
        </div>
        <p v-if="selectedQrOrder.paymentStatus === 'WAITING_CONFIRMATION'" class="notice notice-warning">Khách đã thông báo chuyển khoản. Chỉ xác nhận sau khi kiểm tra giao dịch thực tế trong tài khoản ngân hàng.</p>
        <p v-else class="muted small-text">Giao dịch chưa được khách thông báo đã chuyển khoản. Giữ đơn ở trạng thái chờ cho đến khi kiểm tra được tiền vào.</p>
        <div class="held-modal-actions">
          <RouterLink :to="orderManagementPath(selectedQrOrder)" class="btn btn-primary">Mở đối soát thanh toán</RouterLink>
          <button class="btn btn-outline" @click="closeQr">Đóng</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.held-heading-actions{display:flex;gap:9px;align-items:center;flex-wrap:wrap}
.held-summary-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:14px}
.held-summary-card{display:flex;flex-direction:column;align-items:flex-start;gap:6px;text-align:left;padding:16px 18px;background:#fff;border:1px solid #e3e8ed;border-radius:10px;cursor:pointer;color:#243447}
.held-summary-card.active{border-color:#31835a;box-shadow:0 0 0 2px rgba(49,131,90,.1)}
.held-summary-card small{font-size:11px;color:#637487;font-weight:700}.held-summary-card strong{font-size:27px;color:#172b3e}.held-summary-card span{font-size:11px;color:#718094}
.held-filter-panel{display:grid;grid-template-columns:minmax(220px,1fr) auto;gap:18px;align-items:end;margin-bottom:14px;padding:16px}
.held-filter-panel>label{display:grid;gap:7px;font-size:11px;font-weight:700;color:#44566a}
.held-filter-panel input{width:100%;padding:11px 12px;border:1px solid #d7dfe7;border-radius:8px;color:#192b3d;background:#fff}
.held-tabs{display:flex;gap:5px;flex-wrap:wrap}.held-tabs button{border:1px solid #dce3e9;background:#f6f8fa;padding:10px 12px;border-radius:7px;color:#47596c;font-size:11px;font-weight:700;cursor:pointer}.held-tabs button.active{background:#173d31;color:#fff;border-color:#173d31}
.held-section{padding:18px;margin-bottom:14px}.held-section-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:18px;margin-bottom:14px}.held-section-heading h2{margin:5px 0;font-size:18px;color:#1b2b3a}.held-section-heading p{margin:0;color:#68798a;font-size:11px;line-height:1.5;max-width:760px}
.held-count{white-space:nowrap;border-radius:999px;background:#edf4f0;color:#246044;padding:7px 11px;font-size:11px;font-weight:800}.held-count-warning{background:#fff2d6;color:#865b0c}
.table-scroll{overflow:auto}.held-invoices-view .data-table{min-width:900px}.held-invoices-view .data-table td{vertical-align:middle}.held-money{font-weight:800;color:#c43b42;white-space:nowrap}.held-subtext{display:block;color:#82909d;font-size:10px;margin-top:3px}
.held-state-pill{display:inline-block;padding:6px 8px;border-radius:999px;font-size:9px;font-weight:800;white-space:nowrap}.held-state-pill.is-empty{background:#edf2f6;color:#4c6174}.held-state-pill.is-draft{background:#e8f5eb;color:#267247}.held-state-pill.is-qr{background:#fff1d3;color:#8b5b08}
.held-actions-cell{display:flex;gap:6px;align-items:center;white-space:nowrap}
.held-qr-modal{width:min(570px,calc(100vw - 28px));max-height:min(90vh,900px);overflow:auto;padding:25px;position:relative}.held-qr-modal h2{margin:7px 0 15px;font-size:22px;color:#172b3e}
.held-qr-summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px;padding:13px;background:#f6f8fa;border-radius:9px}.held-qr-summary div{display:flex;flex-direction:column;gap:5px;min-width:0}.held-qr-summary small{font-size:10px;color:#728194}.held-qr-summary b{font-size:11px;color:#243447;overflow-wrap:anywhere}.held-qr-summary strong{font-size:17px;color:#c03942}
.held-qr-code{display:flex;flex-direction:column;align-items:center;padding:14px 0 8px;text-align:center}.held-qr-code img{width:min(280px,100%);height:auto;border:1px solid #e6ebef;border-radius:9px}.held-qr-code p{font-size:11px;color:#526477;line-height:1.5}.held-bank-details{display:grid;gap:7px;padding:12px;background:#f8fafb;border:1px solid #e6ebef;border-radius:8px;font-size:11px;color:#657486}.held-bank-details b{color:#172b3e}.held-qr-items{display:grid;gap:8px;margin-top:14px;font-size:11px;color:#243447}.held-qr-items>div{display:flex;justify-content:space-between;gap:12px;padding-bottom:7px;border-bottom:1px solid #edf0f2}.held-qr-items>div span{color:#526477}.held-modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
@media(max-width:850px){.held-filter-panel{grid-template-columns:1fr}.held-summary-grid{grid-template-columns:1fr}.held-heading-actions{width:100%}.held-section-heading{flex-direction:column}.held-qr-summary{grid-template-columns:1fr 1fr}}
</style>
