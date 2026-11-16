<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
import RefundRequestForm from '../components/RefundRequestForm.vue'

const route = useRoute()
const orders = ref([])
const error = ref('')
const success = ref('')
const reference = ref({})
const busy = ref(null)
const refundOpenId = ref(null)
const filters = reactive({ date: String(route.query.date || ''), orderCode: String(route.query.orderCode || ''), status: String(route.query.status || '') })

async function load() {
  error.value = ''
  try { orders.value = (await api.get('/api/pos/orders', { params: filters })).data }
  catch (e) { error.value = errorMessage(e) }
}
async function confirmPayment(o) {
  busy.value = o.id; error.value = ''; success.value = ''
  try {
    await api.post(`/api/pos/orders/${o.id}/confirm-payment`, {
      reference: reference.value[o.id] || `POS-MANUAL-${Date.now()}`
    })
    success.value = `Đã xác nhận thanh toán cho đơn ${o.code}.`
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = null }
}
async function requestRefund(o, payload) {
  busy.value = o.id; error.value = ''; success.value = ''
  try {
    await api.post(`/api/pos/orders/${o.id}/refund-request`, payload)
    success.value = `Đã gửi yêu cầu hoàn tiền cho đơn POS ${o.code}.`
    refundOpenId.value = null
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = null }
}
function resetFilters() { filters.date=''; filters.orderCode=''; filters.status=''; load() }
function money(v) { return `${Number(v || 0).toLocaleString('vi-VN')}₫` }
onMounted(load)
</script>

<template>
  <div class="view-wrap">
    <div class="view-heading">
      <div><span class="eyebrow">POS / ORDERS</span><h1>Đơn hàng & thanh toán tại quầy</h1><p>Giữ nguyên thao tác POS phong cách Victor. Đơn chuyển khoản chỉ hoàn thành sau khi xác nhận tiền thực nhận; tiền mặt hoàn tất ngay lúc thu tiền.</p></div>
      <button class="btn btn-outline" @click="load">Làm mới</button>
    </div>
    <p v-if="error" class="notice notice-error">{{ error }}</p>
    <p v-if="success" class="notice notice-success">{{ success }}</p>
    <section class="surface-card order-filter-panel"><div class="order-filter-grid"><label>Ngày tạo đơn<input v-model="filters.date" type="date" /></label><label>Mã đơn<input v-model.trim="filters.orderCode" placeholder="Nhập mã đơn" @keyup.enter="load" /></label><label>Trạng thái đơn/thanh toán<select v-model="filters.status"><option value="">Tất cả trạng thái</option><option value="PENDING_PAYMENT">PENDING_PAYMENT</option><option value="CONFIRMED">CONFIRMED</option><option value="PROCESSING">PROCESSING</option><option value="SHIPPING">SHIPPING</option><option value="COMPLETED">COMPLETED</option><option value="CANCELLED">CANCELLED</option><option value="WAITING_CONFIRMATION">WAITING_CONFIRMATION</option><option value="PAID">PAID</option><option value="REFUND_PENDING">REFUND_PENDING</option><option value="REFUNDED">REFUNDED</option><option value="UNPAID">UNPAID</option><option value="PENDING">PENDING</option></select></label><button class="btn btn-primary" @click="load">Tìm kiếm</button><button class="btn btn-outline" @click="resetFilters">Đặt lại</button></div></section>

    <article v-for="o in orders" :key="o.id" class="surface-card order-admin-card">
      <div class="order-card-head">
        <div><small>MÃ ĐƠN</small><b>{{ o.code }}</b><span class="muted small-text">Tạo lúc {{ new Date(o.createdAt).toLocaleString('vi-VN') }}</span></div>
        <div class="order-card-head-actions"><span class="status-pill" :class="'status-'+String(o.status).toLowerCase()">{{ o.status }}</span><RouterLink class="btn btn-outline btn-sm" :to="`/employee/orders/${o.id}/print`">In hóa đơn</RouterLink></div>
      </div>
      <div class="order-grid-info">
        <span>Khách: <b>{{ o.customerName }}</b></span>
        <span>Điện thoại: <b>{{ o.customerPhone || 'Khách lẻ' }}</b></span>
        <span>Thanh toán: <b>{{ o.paymentMethod }} / {{ o.paymentStatus }}</b></span>
        <span>Tổng: <b>{{ money(o.totalAmount) }}</b></span>
      </div>
      <div v-if="o.paymentMethod==='BANK_TRANSFER' && ['PENDING','WAITING_CONFIRMATION'].includes(o.paymentStatus)" class="confirm-row">
        <input v-model="reference[o.id]" placeholder="Mã giao dịch / ghi chú đối soát" />
        <button class="btn btn-primary btn-sm" :disabled="busy===o.id" @click="confirmPayment(o)">{{ busy===o.id ? 'Đang xác nhận…' : 'Xác nhận đã nhận tiền' }}</button>
      </div>
      <div v-if="o.refundEligibility?.eligible" class="order-after-sale">
        <span>Được yêu cầu hoàn tối đa <b>{{ money(o.refundEligibility.remainingAmount) }}</b> · còn {{ o.refundEligibility.requestsRemaining }}/2 lượt · hạn {{ new Date(o.refundEligibility.deadline).toLocaleString('vi-VN') }}</span>
        <button class="btn btn-outline btn-sm" @click="refundOpenId=refundOpenId===o.id?null:o.id">{{ refundOpenId===o.id ? 'Đóng' : 'Tạo yêu cầu hoàn tiền' }}</button>
      </div>
      <p v-else-if="['PAID','REFUND_PENDING'].includes(o.paymentStatus)" class="muted small-text">{{ o.refundEligibility?.message }}</p>
      <RefundRequestForm v-if="refundOpenId===o.id && o.refundEligibility?.eligible" :order="o" @cancel="refundOpenId=null" @submit="requestRefund(o,$event)" />
      <p v-if="o.status==='COMPLETED'" class="completion-lock-note">Đơn đã COMPLETED: nhân viên không hủy trực tiếp; chỉ tiếp nhận yêu cầu hoàn tiền trong 24 giờ kể từ lúc thanh toán.</p>
      <p v-else-if="!['PENDING','WAITING_CONFIRMATION'].includes(o.paymentStatus)" class="muted small-text">Không còn thao tác xác nhận thanh toán cần xử lý.</p>
    </article>
    <div v-if="!orders.length" class="surface-card empty-state">Chưa có đơn POS nào thuộc phạm vi nhân viên.</div>
    <section class="surface-card policy-card">
      <h3>Quy định hoàn tiền POS</h3>
      <p>Chỉ giao dịch đã thanh toán mới gửi được yêu cầu. Thời hạn là 24 giờ kể từ thời điểm thu tiền; tối đa 2 yêu cầu trên mỗi giao dịch, tổng tiền hoàn không vượt số tiền đã thanh toán.</p>
      <p>Yêu cầu có thể hoàn toàn phần hoặc một phần. Nhân viên chỉ xác nhận đã hoàn sau khi thực hiện chi tiền/chuyển khoản thật và nhập mã tham chiếu hoặc số phiếu chi.</p>
    </section>
  </div>
</template>
