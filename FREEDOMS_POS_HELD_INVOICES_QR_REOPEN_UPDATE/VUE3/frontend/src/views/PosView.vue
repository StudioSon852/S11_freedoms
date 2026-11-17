<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api, errorMessage } from '../api'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const prefix = computed(() => route.path.startsWith('/admin') ? '/admin' : '/employee')
const isEmployeePos = computed(() => route.path.startsWith('/employee'))
const sessions = ref([])
const activeId = ref(null)
const products = ref([])
const coupons = ref([])
const search = ref('')
const salesOnly = ref(false)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const success = ref('')
const paymentMethod = ref('CASH')
const cashReceived = ref('')
const cashTotal = computed(() => Number(preview.value?.total ?? active.value?.subtotal ?? 0))
const cashReceivedAmount = computed(() => Math.max(0, Number(cashReceived.value || 0)))
const cashChange = computed(() => Math.max(0, cashReceivedAmount.value - cashTotal.value))
const cashShortage = computed(() => Math.max(0, cashTotal.value - cashReceivedAmount.value))
const cashAmountIsValid = computed(() => cashReceived.value !== '' && Number.isFinite(Number(cashReceived.value)) && Number.isInteger(Number(cashReceived.value)) && Number(cashReceived.value) >= 0)
const cashIsEnough = computed(() => cashAmountIsValid.value && cashReceivedAmount.value >= cashTotal.value)
const paymentConfig = ref(null)
const preview = ref(null)
const variantChoice = ref({})
const lastOrder = ref(null)
const active = computed(() => sessions.value.find(s => s.id === activeId.value) || null)
const selectedVariant = product => product?.variants?.find(v => Number(v.id) === Number(variantChoice.value[product.id])) || null
function productImage(product) {
  const variant = selectedVariant(product)
  return variant?.images?.[0]?.imageUrl || product?.imageUrl || '/img/picture1.png'
}
function imageFallback(event) {
  const img = event?.target
  if (!img || img.dataset.fallbackApplied === '1') return
  img.dataset.fallbackApplied = '1'
  img.src = '/img/picture1.png'
}
let searchTimer

async function loadProducts() {
  try { products.value = (await api.get('/api/pos/products', { params: { q: search.value, salesOnly: salesOnly.value } })).data; for (const p of products.value) if (p.variants?.length && !variantChoice.value[p.id]) variantChoice.value[p.id] = p.variants.find(v => v.stockQuantity > 0)?.id || p.variants[0].id }
  catch (e) { error.value = errorMessage(e) }
}
async function loadSessions() {
  const data = (await api.get('/api/pos/sessions')).data
  sessions.value = data
  const requestedId = Number(route.query.sessionId || 0)
  if (requestedId && sessions.value.some(s => Number(s.id) === requestedId)) {
    activeId.value = requestedId
  } else if (!sessions.value.some(s => s.id === activeId.value)) {
    activeId.value = sessions.value[0]?.id || null
  }
}
async function loadPreview() {
  if (!activeId.value) { preview.value = null; return }
  try { preview.value = (await api.get(`/api/pos/sessions/${activeId.value}/preview`)).data }
  catch (e) { preview.value = null; if (active.value?.items?.length) error.value = errorMessage(e) }
}
async function load() {
  loading.value = true; error.value = ''
  try {
    await Promise.all([loadSessions(), loadProducts()])
    coupons.value = (await api.get('/api/pos/coupons')).data
    paymentConfig.value = (await api.get('/api/store/payment-config')).data
    if (!sessions.value.length) await newBuyer()
  } catch (e) { error.value = errorMessage(e) }
  finally { loading.value = false }
}
async function newBuyer() {
  error.value = ''
  try {
    const data = (await api.post('/api/pos/sessions', { customerName: 'Khách lẻ', customerPhone: '' })).data
    sessions.value.unshift(data); activeId.value = data.id; paymentMethod.value = 'CASH'; cashReceived.value = ''
  } catch (e) { error.value = errorMessage(e) }
}
function updateLine(variantId, quantity) {
  if (!active.value) return
  const items = active.value.items.map(x => ({ variantId: x.variantId, quantity: x.quantity }))
  const found = items.find(x => x.variantId === variantId)
  if (found) found.quantity = quantity
  save({ items })
}
async function addProduct(product, variant) {
  if (!active.value) await newBuyer()
  if (!active.value || variant.stockQuantity < 1) return
  const items = active.value.items.map(x => ({ variantId: x.variantId, quantity: x.quantity }))
  const found = items.find(x => x.variantId === variant.id)
  if (found) found.quantity += 1
  else items.push({ variantId: variant.id, quantity: 1 })
  await save({ items })
  success.value = `Đã thêm ${product.name} vào ${active.value?.customerName || 'quầy khách'}.`
}
async function save(patch = {}, throwOnError = false) {
  if (!active.value) return
  busy.value = true; error.value = ''
  const body = {
    customerName: active.value.customerName,
    customerPhone: active.value.customerPhone,
    note: active.value.note,
    couponCode: active.value.couponCode,
    items: active.value.items.map(x => ({ variantId: x.variantId, quantity: x.quantity })),
    ...patch
  }
  try {
    const saved = (await api.put(`/api/pos/sessions/${active.value.id}`, body)).data
    const i = sessions.value.findIndex(s => s.id === saved.id)
    if (i >= 0) sessions.value.splice(i, 1, saved)
    await loadPreview()
    return saved
  } catch (e) { error.value = errorMessage(e); if (throwOnError) throw e; return null }
  finally { busy.value = false }
}
async function removeLine(variantId) {
  if (!active.value) return
  const items = active.value.items.filter(x => x.variantId !== variantId).map(x => ({ variantId:x.variantId, quantity:x.quantity }))
  await save({ items })
}
async function chooseCoupon(code) { await save({ couponCode: code || null }) }
async function checkout() {
  if (!active.value?.items?.length) return error.value = 'Quầy khách chưa có sản phẩm.'
  if (paymentMethod.value === 'CASH' && !cashIsEnough.value) {
    error.value = `Số tiền khách đưa chưa đủ. Cần thêm ${fmt(cashShortage.value)}.`
    return
  }
  busy.value = true; error.value = ''; success.value = ''
  try {
    await save({}, true)
    const request = {
      paymentMethod: paymentMethod.value,
      cashReceived: paymentMethod.value === 'CASH' ? cashReceivedAmount.value : null
    }
    const result = (await api.post(`/api/pos/sessions/${active.value.id}/checkout`, request)).data
    const isCash = result.paymentMethod === 'CASH'
    const received = isCash ? cashReceivedAmount.value : null
    lastOrder.value = {
      ...result,
      cashReceived: received,
      cashChange: isCash ? Math.max(0, received - Number(result.total || 0)) : null,
      qrUrl: ''
    }
    if (!isCash && result.paymentMethod === 'BANK_TRANSFER' && paymentConfig.value?.bankAccount) {
      lastOrder.value.qrUrl = `https://img.vietqr.io/image/${paymentConfig.value.bankCode}-${paymentConfig.value.bankAccount}-compact2.png?amount=${Math.round(Number(result.total))}&addInfo=${encodeURIComponent(result.orderCode)}&accountName=${encodeURIComponent(paymentConfig.value.bankAccountName)}`
    }
    success.value = isCash ? 'Đã nhận đủ tiền mặt, tính tiền thừa và hoàn thành đơn tại quầy.' : 'Đã tạo đơn chuyển khoản. Khi xác nhận tiền vào, đơn POS sẽ tự động hoàn thành.'
    await loadSessions()
    activeId.value = sessions.value[0]?.id || null
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
function printLastInvoice() {
  if (!lastOrder.value?.orderId) return
  const printPath = `${prefix.value}/orders/${lastOrder.value.orderId}/print`
  lastOrder.value = null
  router.push(printPath)
}
async function cancelSession() {
  if (!active.value || !confirm(`Đóng phiên ${active.value.code}? Thao tác này không tạo đơn và không trừ kho.`)) return
  try { await api.post(`/api/pos/sessions/${active.value.id}/cancel`, { reason: 'Nhân viên đóng quầy' }); await loadSessions(); if (!sessions.value.length) await newBuyer() }
  catch (e) { error.value = errorMessage(e) }
}
function fmt(v) { return Number(v || 0).toLocaleString('vi-VN') + 'đ' }
watch(activeId, () => loadPreview())
watch(() => route.query.sessionId, value => {
  const requestedId = Number(value || 0)
  if (requestedId && sessions.value.some(s => Number(s.id) === requestedId)) activeId.value = requestedId
})
onMounted(load)
</script>

<template>
  <div class="view-wrap pos-view" :class="{ 'employee-pos-view': isEmployeePos }">
    <div class="view-heading"><div><span class="eyebrow">FREEDOMS / POS</span><h1>Bán hàng tại quầy</h1><p>Mỗi phiên quầy có một người mua riêng. Có thể chuyển khách qua lại mà không gộp giỏ hàng.</p></div><button class="btn btn-primary" @click="newBuyer">＋ Khách mới / quầy mới</button></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
    <div class="pos-tabs"><button v-for="s in sessions" :key="s.id" :class="['pos-tab', { active: activeId===s.id }]" @click="activeId=s.id"><b>{{ s.customerName }}</b><small>{{ s.code }} · {{ s.itemCount }} món</small></button><span v-if="!sessions.length" class="muted">Chưa có phiên khách.</span></div>
    <div v-if="active" class="pos-grid">
      <section class="surface-card pos-products-panel">
        <div class="pos-panel-head"><div><h2>Tìm kiếm sản phẩm</h2><p class="muted">Tìm theo tên, SKU hoặc bật lọc sales.</p></div></div>
        <div class="pos-search-row"><input v-model="search" @input="() => { clearTimeout(searchTimer); searchTimer=setTimeout(loadProducts,220) }" placeholder="Tìm kiếm: sales / tên giày / SKU…"/><label class="toggle-line"><input v-model="salesOnly" type="checkbox" @change="loadProducts"/> Sales</label></div>
        <div v-if="coupons.length" class="pos-coupon-strip">
          <div class="pos-coupon-title">Mã giảm giá OFFLINE đang có</div>
          <div class="pos-coupon-chips"><button v-for="c in coupons" :key="c.id" class="pos-coupon-chip" @click="chooseCoupon(c.code)"><b>{{ c.code }}</b><small>{{ c.discountType==='PERCENT' ? `${c.discountValue}%` : fmt(c.discountValue) }}</small></button></div>
        </div>
        <template v-if="isEmployeePos">
        <div v-if="loading" class="empty-state">Đang tải sản phẩm…</div>
        <div v-else class="pos-product-grid">
          <article v-for="p in products" :key="p.id" class="pos-product-card">
            <div class="pos-product-card-image">
              <span v-if="selectedVariant(p)?.hasSale" class="pos-product-sale-badge">ĐANG GIẢM</span>
              <img :src="productImage(p)" :alt="p.name" loading="lazy" @error="imageFallback" />
            </div>
            <div class="pos-product-card-body">
              <div class="pos-product-card-meta">{{ p.brand || 'FREEDOMS' }} <span>·</span> {{ p.category || 'Giày thể thao' }}</div>
              <h3>{{ p.name }}</h3>
              <p class="pos-product-card-desc">{{ p.description || 'Thiết kế năng động, thoải mái cho mọi hoạt động.' }}</p>
              <label class="pos-card-variant-label">Chọn size / màu
                <select v-model.number="variantChoice[p.id]">
                  <option v-for="v in p.variants || []" :key="v.id" :value="v.id" :disabled="Number(v.stockQuantity) < 1">
                    {{ v.size || 'Size tự do' }} · {{ v.color || 'Mặc định' }} · {{ v.sku }}
                  </option>
                </select>
              </label>
              <div v-if="selectedVariant(p)" class="pos-card-price-row">
                <template v-if="selectedVariant(p).hasSale">
                  <strong class="pos-card-sale-price">{{ fmt(selectedVariant(p).salePrice) }}</strong>
                  <del>{{ fmt(selectedVariant(p).unitPrice) }}</del>
                </template>
                <strong v-else>{{ fmt(selectedVariant(p).unitPrice) }}</strong>
                <span class="pos-card-stock" :class="{ 'out-of-stock': selectedVariant(p).stockQuantity < 1 }">
                  {{ selectedVariant(p).stockQuantity > 0 ? `Còn ${selectedVariant(p).stockQuantity}` : 'Hết hàng' }}
                </span>
              </div>
              <button class="btn btn-primary pos-card-add" :disabled="!selectedVariant(p) || selectedVariant(p).stockQuantity < 1 || busy" @click="addProduct(p, selectedVariant(p))">
                ＋ Thêm vào hóa đơn
              </button>
            </div>
          </article>
          <div v-if="!products.length" class="empty-state">Không tìm thấy sản phẩm hoặc hàng đang hết.</div>
        </div>
        </template>
        <template v-else>
        <div class="pos-product-list">
          <article v-for="p in products" :key="p.id" class="pos-product-row">
            <img :src="p.imageUrl || '/img/picture1.png'" :alt="p.name"/>
            <div class="pos-product-details"><b>{{ p.name }}</b><small>{{ p.brand }} · {{ p.sku }}</small><span v-if="p.hasSale" class="inline-sale-label">Có mặt hàng đang giảm</span>
              <div class="pos-variant-row"><select v-model.number="variantChoice[p.id]"><option v-for="v in p.variants" :key="v.id" :value="v.id" :disabled="v.stockQuantity < 1">{{ v.size }} · {{ v.color }} · {{ v.hasSale ? fmt(v.salePrice) : fmt(v.unitPrice) }} · kho {{ v.stockQuantity }}{{ v.hasSale ? ' · SALES' : '' }}</option></select><button class="btn btn-primary btn-sm" :disabled="!p.variants.some(v => v.stockQuantity>0)" @click="addProduct(p,p.variants.find(v => v.id === variantChoice[p.id]))">Thêm</button></div>
            </div>
          </article>
          <div v-if="!products.length" class="empty-state">Không tìm thấy sản phẩm hoặc hàng đang hết.</div>
        </div>
        </template>
      </section>
      <section class="surface-card pos-ticket-panel">
        <div class="pos-ticket-header"><div><h2>{{ active.customerName || 'Khách lẻ' }}</h2><small>{{ active.code }}</small></div><button class="icon-btn danger-text" title="Đóng phiên" @click="cancelSession">×</button></div>
        <div class="form-stack compact-form">
          <div class="two-fields"><label>Loại khách<select :value="active.customerPhone ? 'ACCOUNT' : 'GUEST'" @change="save({ customerName: $event.target.value==='GUEST' ? 'Khách lẻ' : active.customerName })"><option value="GUEST">Khách lẻ</option><option value="ACCOUNT">Có tài khoản</option></select></label><label>Tên khách<input :value="active.customerName" @change="save({ customerName: $event.target.value })" placeholder="Tên khách"/></label></div>
          <label>Số điện thoại<input :value="active.customerPhone" @change="save({ customerPhone: $event.target.value })" placeholder="Nhập số để nhận diện tài khoản thành viên"/></label>
          <small class="muted">Nếu số điện thoại khớp tài khoản khách hàng, hệ thống tự áp dụng ưu đãi hạng thành viên.</small>
        </div>
        <div class="ticket-lines">
          <div v-for="line in active.items" :key="line.variantId" class="ticket-line">
            <div><b>{{ line.productName }}</b><small>{{ line.variantLabel }} · {{ line.sku }}</small><strong>{{ fmt(line.lineTotal) }}</strong></div>
            <div class="quantity-control"><button @click="updateLine(line.variantId,line.quantity-1)" :disabled="line.quantity<=1">−</button><input :value="line.quantity" type="number" min="1" @change="updateLine(line.variantId,Number($event.target.value))"/><button @click="updateLine(line.variantId,line.quantity+1)">+</button><button title="Xóa mặt hàng" @click="removeLine(line.variantId)">×</button></div>
          </div>
          <div v-if="!active.items.length" class="empty-state small-empty">Chưa có mặt hàng. Tìm sản phẩm bên trái để thêm vào quầy khách này.</div>
        </div>
        <div class="form-stack compact-form">
          <label>Mã giảm giá Offline<select :value="active.couponCode || ''" @change="chooseCoupon($event.target.value)"><option value="">Không áp dụng coupon</option><option v-for="c in coupons" :key="c.id" :value="c.code">{{ c.code }} · {{ c.discountType==='PERCENT' ? `${c.discountValue}%` : fmt(c.discountValue) }}</option></select></label>
          <label>Ghi chú<input :value="active.note" @change="save({ note: $event.target.value })" placeholder="Ghi chú đơn hàng"/></label>
        </div>
        <div v-if="preview" class="pos-pricing-breakdown">
          <div><span>Tiền hàng</span><b>{{ fmt(preview.subtotal) }}</b></div>
          <div><span>Giảm giá biến thể</span><b>-{{ fmt(preview.variantDiscount) }}</b></div>
          <div><span>Giảm giá thành viên</span><b>-{{ fmt(preview.memberDiscount) }}</b></div>
          <div><span>Coupon Offline</span><b>-{{ fmt(preview.couponDiscount) }}</b></div>
        </div>
        <div class="pos-total"><span>Tổng sau ưu đãi</span><b>{{ fmt(preview?.total ?? active.subtotal) }}</b></div>
        <div class="payment-select"><b>Chọn phương thức thanh toán</b><div class="payment-switch"><label :class="{chosen:paymentMethod==='CASH'}"><input v-model="paymentMethod" type="radio" value="CASH"/> Tiền mặt</label><label :class="{chosen:paymentMethod==='BANK_TRANSFER'}"><input v-model="paymentMethod" type="radio" value="BANK_TRANSFER"/> Chuyển khoản</label></div><small class="muted">Tiền mặt: nhập số tiền khách đưa để tính tiền thừa. Chuyển khoản: tạo mã QR riêng.</small></div>
        <div v-if="paymentMethod==='CASH'" class="pos-cash-panel">
          <label>Số tiền khách đưa (VNĐ)
            <input v-model="cashReceived" type="number" min="0" step="1" inputmode="numeric" placeholder="Nhập số tiền khách đưa" />
          </label>
          <div class="pos-cash-calculation"><span>Tổng khách phải trả</span><b>{{ fmt(cashTotal) }}</b></div>
          <div class="pos-cash-calculation"><span>Tiền khách đưa</span><b>{{ fmt(cashReceivedAmount) }}</b></div>
          <div class="pos-cash-change"><span>Tiền thừa trả khách</span><strong>{{ fmt(cashChange) }}</strong></div>
          <small v-if="cashReceived!=='' && !Number.isInteger(Number(cashReceived))" class="pos-cash-warning">Vui lòng nhập số tiền nguyên theo đơn vị VNĐ.</small>
          <small v-else-if="cashReceived!=='' && !cashIsEnough" class="pos-cash-warning">Khách còn thiếu {{ fmt(cashShortage) }}.</small>
          <small v-else class="muted">Nhập đủ số tiền khách đưa để xác nhận thanh toán.</small>
        </div>
        <button class="btn btn-primary btn-block" :disabled="busy || !active.items.length || (paymentMethod==='CASH' && !cashIsEnough)" @click="checkout">{{ busy ? 'Đang xử lý…' : `Thanh toán ${paymentMethod==='CASH'?'tiền mặt':'chuyển khoản'}` }}</button>
        <button class="btn btn-outline btn-block" :disabled="busy" @click="save">Lưu phiên khách / hóa đơn chờ</button>
      </section>
    </div>
    <section v-if="lastOrder" class="modal-backdrop" @click.self="lastOrder=null"><div class="surface-card payment-modal"><button class="modal-close" @click="lastOrder=null">×</button><span class="eyebrow">ĐƠN POS ĐÃ TẠO</span><h2>{{ lastOrder.orderCode }}</h2><div class="payment-total"><small>Tổng thanh toán</small><strong>{{ fmt(lastOrder.total) }}</strong></div><div v-if="lastOrder.paymentMethod==='CASH'" class="pos-cash-receipt"><p><span>Phương thức</span><b>Tiền mặt</b></p><p><span>Số tiền khách đưa</span><b>{{ fmt(lastOrder.cashReceived) }}</b></p><p class="pos-cash-change"><span>Tiền thừa trả khách</span><strong>{{ fmt(lastOrder.cashChange) }}</strong></p><p class="notice notice-success">Đã nhận tiền mặt. Đơn tại quầy đã hoàn thành.</p><button class="btn btn-primary btn-block" @click="printLastInvoice">In hóa đơn</button></div><div v-else-if="lastOrder.qrUrl" class="qr-wrap"><img :src="lastOrder.qrUrl" alt="QR chuyển khoản"/><p>Khách chuyển khoản theo nội dung mã đơn. Nhân viên xác nhận tiền vào tại mục Đơn hàng & thanh toán để tự động hoàn thành.</p></div><p v-else class="notice notice-warning">Đơn đã tạo ở trạng thái chờ thanh toán. Hãy cấu hình tài khoản ngân hàng trong cài đặt cửa hàng để hiển thị QR.</p><button class="btn btn-outline btn-block" @click="lastOrder=null">Đóng</button></div></section>
  </div>
</template>
