<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const cart = useCartStore()
const auth = useAuthStore()
const router = useRouter()
const coupons = ref([])
const couponCode = ref('')
const method = ref('BANK_TRANSFER')
const address = ref('')
const province = ref('Hà Nội')
const districtId = ref('')
const wardCode = ref('')
const phone = ref(auth.user?.phone || '')
const customerName = ref(auth.user?.fullName || '')
const note = ref('')
const quote = ref(null)
const busy = ref(false)
const error = ref('')
const selected = computed(() => cart.selected)
const subtotal = computed(() => selected.value.reduce((s, i) => s + (i.salePrice || i.unitPrice) * i.quantity, 0))

async function loadCoupons() {
  try { coupons.value = (await api.get('/api/store/coupons', { params: { channel: 'ONLINE' } })).data } catch { coupons.value = [] }
}
async function loadDefaultCustomerAddress() {
  try {
    const profile = (await api.get('/api/account/me')).data
    if (!customerName.value) customerName.value = profile.fullName || ''
    if (!phone.value) phone.value = profile.phone || ''
    if (!address.value) address.value = profile.defaultAddress || ''
  } catch { /* public checkout view still handles unauthenticated state via route guard */ }
}
function payload() {
  return {
    items: selected.value.map(i => ({ variantId: i.variantId, quantity: i.quantity })),
    couponCode: couponCode.value || null, paymentMethod: method.value,
    customerName: customerName.value, customerPhone: phone.value, shippingAddress: address.value,
    province: province.value, toDistrictId: districtId.value ? Number(districtId.value) : null,
    toWardCode: wardCode.value || null, note: note.value
  }
}
async function calculateShipping() {
  error.value = ''; quote.value = null
  try { quote.value = (await api.post('/api/checkout/preview', payload())).data }
  catch (e) { error.value = errorMessage(e) }
}
async function submit() {
  if (!selected.value.length) return error.value = 'Giỏ hàng chưa chọn sản phẩm để mua.'
  busy.value = true; error.value = ''
  try {
    const response = (await api.post('/api/checkout', payload())).data
    const boughtIds = cart.checkoutIds.slice()
    cart.removeCheckedOut()
    sessionStorage.setItem('freedoms-last-order', JSON.stringify({ ...response, qrUrl: response.qrUrl, ids: boughtIds }))
    if (response.paymentMethod === 'BANK_TRANSFER') router.push(`/payment/${response.orderId}`)
    else router.push('/orders')
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
onMounted(async () => {
  if (!cart.checkoutIds.length && cart.items.length) cart.selectAll(true)
  await loadDefaultCustomerAddress()
  await loadCoupons()
  if (selected.value.length) await calculateShipping()
})
</script>

<template>
  <div class="simple-page ocean-backdrop">
    
    <main class="narrow-main checkout-main">
      <div class="page-heading"><span class="eyebrow">CHECKOUT</span><h1>Thông tin thanh toán</h1></div>
      <div v-if="!selected.length" class="surface-card empty-state">Chưa có sản phẩm nào được chọn để thanh toán. <RouterLink to="/cart">Quay lại giỏ hàng</RouterLink></div>
      <div v-else class="checkout-grid">
        <section class="surface-card form-stack">
          <h2>Thông tin nhận hàng</h2>
          <label>Người nhận<input v-model.trim="customerName" required/></label>
          <label>Số điện thoại<input v-model.trim="phone" required/></label>
          <label>Địa chỉ cụ thể<input v-model.trim="address" placeholder="Số nhà, đường, phường/xã" required/></label>
          <div class="two-fields"><label>Tỉnh / thành phố<select v-model="province" @change="quote=null"><option>Hà Nội</option><option>Đà Nẵng</option><option>TP. Hồ Chí Minh</option><option>Tỉnh/thành khác</option></select></label><label>Mã quận GHN (nếu có)<input v-model="districtId" inputmode="numeric" placeholder="VD: 1482"/></label></div>
          <label>Mã phường GHN (nếu có)<input v-model.trim="wardCode" placeholder="Mã ward do GHN cấp"/></label>
          <label>Ghi chú<textarea v-model="note" rows="2" placeholder="Lưu ý giao hàng (không bắt buộc)"></textarea></label>
          <div class="divider"></div>
          <h2>Mã giảm giá Online</h2>
          <div class="coupon-picker"><input v-model.trim="couponCode" placeholder="Nhập mã Online"/><button class="btn btn-outline" @click="calculateShipping">Áp dụng / tính lại</button></div>
          <div v-if="coupons.length" class="coupon-strip"><button v-for="c in coupons" :key="c.id" class="coupon-mini" @click="couponCode=c.code;calculateShipping()"><b>{{ c.code }}</b><small>{{ c.discountType==='PERCENT' ? `${c.discountValue}%` : `${Number(c.discountValue).toLocaleString('vi-VN')}đ` }} · Tối thiểu {{ Number(c.minOrderAmount).toLocaleString('vi-VN') }}đ</small></button></div>
          <div class="divider"></div>
          <h2>Phương thức thanh toán</h2>
          <label class="payment-option"><input v-model="method" type="radio" value="BANK_TRANSFER"/> <span><b>Chuyển khoản QR</b><small>Đơn chuyển sang chờ thanh toán; cửa hàng xác nhận sau khi đối soát.</small></span></label>
          <label class="payment-option"><input v-model="method" type="radio" value="COD"/> <span><b>Thanh toán khi nhận hàng (COD)</b><small>Đơn chuyển sang chờ xác nhận.</small></span></label>
          <p v-if="error" class="notice notice-error">{{ error }}</p>
          <button class="btn btn-primary btn-block" :disabled="busy" @click="submit">{{ busy ? 'Đang tạo đơn…' : 'Đặt hàng và thanh toán' }}</button>
        </section>
        <aside class="surface-card order-summary">
          <h2>Đơn hàng đã chọn</h2>
          <div v-for="item in selected" :key="item.variantId" class="summary-product"><img :src="item.imageUrl"/><div><b>{{ item.productName }}</b><small>{{ item.variantLabel }} × {{ item.quantity }}</small></div><strong>{{ (item.salePrice * item.quantity).toLocaleString('vi-VN') }}đ</strong></div>
          <div class="summary-row"><span>Tạm tính</span><b>{{ subtotal.toLocaleString('vi-VN') }}đ</b></div>
          <div v-if="quote" class="summary-row"><span>Giảm giá biến thể</span><b>-{{ Number(quote.variantDiscount).toLocaleString('vi-VN') }}đ</b></div>
          <div v-if="quote" class="summary-row"><span>Giảm giá thành viên</span><b>-{{ Number(quote.memberDiscount).toLocaleString('vi-VN') }}đ</b></div>
          <div v-if="quote" class="summary-row"><span>Coupon</span><b>-{{ Number(quote.couponDiscount).toLocaleString('vi-VN') }}đ</b></div>
          <div class="summary-row"><span>Phí vận chuyển</span><b>{{ quote ? Number(quote.shippingFee).toLocaleString('vi-VN')+'đ' : 'Chưa tính' }}</b></div>
          <p v-if="quote" class="shipping-note">{{ quote.shippingMessage }}</p>
          <div class="summary-total"><span>Tổng thanh toán</span><strong>{{ quote ? Number(quote.total).toLocaleString('vi-VN') : subtotal.toLocaleString('vi-VN') }}đ</strong></div>
          <button class="btn btn-outline btn-block" @click="calculateShipping">Tính / cập nhật phí vận chuyển</button>
          <small class="muted">Phí được backend tính lại khi tạo đơn. Giá cuối cùng không lấy từ dữ liệu trình duyệt.</small>
        </aside>
      </div>
    </main>
  </div>
</template>
