<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import RefundRequestForm from '../components/RefundRequestForm.vue'

const orders = ref([])
const loading = ref(true)
const error = ref('')
const success = ref('')
const busyId = ref(null)
const refundOpenId = ref(null)
async function load() {
  loading.value = true
  try { orders.value = (await api.get('/api/orders/mine')).data }
  catch (e) { error.value = errorMessage(e) }
  finally { loading.value = false }
}
async function cancel(order) {
  const reason = prompt(`Bạn muốn hủy đơn ${order.code}? Vui lòng nhập lý do:`)
  if (!reason?.trim()) return
  busyId.value = order.id
  try { await api.post(`/api/orders/${order.id}/cancel`, { reason }); success.value = 'Đơn hàng đã được hủy.'; await load() }
  catch (e) { error.value = errorMessage(e) }
  finally { busyId.value = null }
}
async function requestRefund(order, payload) {
  busyId.value = order.id; error.value = ''; success.value = ''
  try {
    await api.post(`/api/orders/${order.id}/refund-request`, payload)
    success.value = `Đã gửi yêu cầu hoàn tiền cho đơn ${order.code}. Cửa hàng sẽ xử lý và cập nhật trạng thái giao dịch.`
    refundOpenId.value = null
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busyId.value = null }
}
function canCancel(o) {
  return ['PENDING_PAYMENT','CONFIRMED'].includes(o.status)
    && !['PAID','WAITING_CONFIRMATION','REFUND_PENDING','REFUNDED'].includes(o.paymentStatus)
}
function fmt(value) { return `${Number(value || 0).toLocaleString('vi-VN')}₫` }
onMounted(load)
</script>

<template>
  <div class="simple-page ocean-backdrop">
    <main class="narrow-main">
      <div class="page-heading"><span class="eyebrow">ORDER HISTORY</span><h1>Đơn mua của tôi</h1><p>Theo dõi trạng thái giao hàng, thanh toán và yêu cầu sau mua.</p></div>
      <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
      <div v-if="loading" class="surface-card empty-state">Đang tải…</div>
      <div v-else-if="!orders.length" class="surface-card empty-state">Bạn chưa có đơn hàng nào. <RouterLink to="/store">Tiếp tục mua sắm</RouterLink></div>
      <article v-for="o in orders" :key="o.id" class="surface-card order-card">
        <div class="order-card-head"><div><small>MÃ ĐƠN</small><b>{{ o.code }}</b></div><span class="status-pill" :class="'status-'+String(o.status).toLowerCase()">{{ o.status }}</span></div>
        <div v-for="item in o.items" :key="item.sku + item.variantLabel" class="order-item-row"><span>{{ item.productName }} · {{ item.variantLabel }} × {{ item.quantity }}</span><b>{{ fmt(item.lineTotal) }}</b></div>
        <div class="summary-row"><span>Thanh toán</span><b>{{ o.paymentMethod }} / {{ o.paymentStatus }}</b></div>
        <div class="summary-total"><span>Tổng</span><strong>{{ fmt(o.totalAmount) }}</strong></div>
        <p v-if="o.paymentStatus==='REFUND_PENDING'" class="notice notice-warning">Yêu cầu hoàn tiền đã được gửi và đang chờ cửa hàng xử lý.</p>
        <p v-else-if="o.paymentStatus==='REFUNDED'" class="notice notice-success">Giao dịch đã được hoàn đủ.</p>
        <div class="order-actions">
          <RouterLink v-if="o.paymentMethod==='BANK_TRANSFER' && ['PENDING','WAITING_CONFIRMATION'].includes(o.paymentStatus)" :to="`/payment/${o.id}`" class="btn btn-outline btn-sm">Thanh toán / xem QR</RouterLink>
          <button v-if="canCancel(o)" class="btn btn-danger-outline btn-sm" :disabled="busyId===o.id" @click="cancel(o)">{{ busyId===o.id ? 'Đang xử lý…' : 'Hủy đơn chưa thanh toán' }}</button>
          <button v-if="o.refundEligibility?.eligible" class="btn btn-outline btn-sm" :disabled="busyId===o.id" @click="refundOpenId=refundOpenId===o.id?null:o.id">{{ refundOpenId===o.id ? 'Đóng yêu cầu' : 'Yêu cầu hoàn tiền' }}</button>
        </div>
        <p v-if="['PAID','REFUND_PENDING'].includes(o.paymentStatus) && !o.refundEligibility?.eligible && o.paymentStatus!=='REFUNDED'" class="refund-policy-note">{{ o.refundEligibility?.message }} · Thời hạn {{ o.refundEligibility?.windowHours || (o.salesChannel==='POS'?24:48) }} giờ.</p>
        <RefundRequestForm v-if="refundOpenId===o.id && o.refundEligibility?.eligible" :order="o" @cancel="refundOpenId=null" @submit="requestRefund(o,$event)" />
      </article>
      <section class="surface-card policy-card"><h3>Quy định hủy đơn và hoàn tiền</h3>
        <p>Đơn chưa thanh toán có thể gửi yêu cầu hủy khi đang chờ thanh toán hoặc chờ xác nhận. Khi giao dịch đã thanh toán, khách gửi yêu cầu hoàn tiền thay vì hủy trực tiếp đơn đã hoàn thành.</p>
        <p>Thời hạn gửi yêu cầu: đơn Online trong 48 giờ; đơn tại quầy (POS) trong 24 giờ kể từ thời điểm thanh toán. Mỗi giao dịch được gửi tối đa 2 yêu cầu; tổng tiền hoàn của tất cả yêu cầu không vượt quá số tiền giao dịch.</p>
        <p>Yêu cầu hoàn tiền chỉ được xác nhận hoàn tất sau khi cửa hàng thực hiện chuyển/chi tiền và lưu mã tham chiếu.</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.refund-policy-note{font-size:10px;color:#777;margin:10px 0 0;line-height:1.6}
</style>
