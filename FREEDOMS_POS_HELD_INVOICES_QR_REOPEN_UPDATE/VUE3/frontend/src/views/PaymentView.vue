<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'

const route = useRoute()
const order = ref(null)
const bank = ref(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const message = ref('')
const qrUrl = ref('')

async function load() {
  loading.value = true
  try {
    const [detail, config] = await Promise.all([api.get(`/api/orders/${route.params.id}`), api.get('/api/store/payment-config')])
    order.value = detail.data; bank.value = config.data
    const stored = JSON.parse(sessionStorage.getItem('freedoms-last-order') || '{}')
    qrUrl.value = stored.orderId === order.value.id ? stored.qrUrl : ''
    if (!qrUrl.value && bank.value.bankAccount) {
      const q = new URLSearchParams({ amount: String(Math.round(Number(order.value.totalAmount))), addInfo: order.value.code, accountName: bank.value.bankAccountName })
      qrUrl.value = `https://img.vietqr.io/image/${bank.value.bankCode}-${bank.value.bankAccount}-compact2.png?${q}`
    }
  } catch (e) { error.value = errorMessage(e) }
  finally { loading.value = false }
}
async function submitted() {
  busy.value = true; error.value = ''; message.value = ''
  try { message.value = (await api.post(`/api/orders/${order.value.id}/transfer-submitted`)).data.message; await load() }
  catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <div class="simple-page ocean-backdrop">
    <main class="narrow-main"><div class="page-heading"><span class="eyebrow">BANK TRANSFER</span><h1>Thanh toán đơn hàng</h1></div>
      <section v-if="loading" class="surface-card empty-state">Đang tải thông tin thanh toán…</section>
      <section v-else-if="order" class="surface-card payment-panel">
        <div class="payment-head"><div><small>Mã đơn hàng</small><h2>{{ order.code }}</h2></div><span class="status-pill" :class="'status-'+String(order.paymentStatus).toLowerCase()">{{ order.paymentStatus }}</span></div>
        <div class="payment-total"><small>Số tiền cần chuyển</small><strong>{{ Number(order.totalAmount).toLocaleString('vi-VN') }}đ</strong></div>
        <div v-if="qrUrl" class="qr-wrap"><img :src="qrUrl" alt="Mã QR chuyển khoản"/><p>Quét QR bằng ứng dụng ngân hàng và kiểm tra đúng số tiền, nội dung chuyển khoản.</p></div>
        <div v-else class="notice notice-warning">Chưa cấu hình tài khoản ngân hàng. Cửa hàng cần điền BANK_CODE, BANK_ACCOUNT và BANK_ACCOUNT_NAME trong backend/.env.</div>
        <div class="bank-info"><p><span>Ngân hàng</span><b>{{ bank?.bankCode }}</b></p><p><span>Số tài khoản</span><b>{{ bank?.bankAccount || 'Chưa cấu hình' }}</b></p><p><span>Chủ tài khoản</span><b>{{ bank?.bankAccountName }}</b></p><p><span>Nội dung chuyển khoản</span><b>{{ order.code }}</b></p></div>
        <p class="muted">Sau khi chuyển khoản, bấm nút bên dưới để thông báo. Đơn online sẽ chuyển sang <b>CONFIRMED</b> sau khi nhân viên xác nhận tiền thực nhận.</p>
        <p v-if="message" class="notice notice-success">{{ message }}</p><p v-if="error" class="notice notice-error">{{ error }}</p>
        <button class="btn btn-primary btn-block" :disabled="busy || order.paymentStatus==='WAITING_CONFIRMATION' || order.paymentStatus==='PAID'" @click="submitted">{{ busy ? 'Đang gửi…' : order.paymentStatus==='WAITING_CONFIRMATION' ? 'Đã gửi thông báo – chờ xác nhận' : 'Tôi đã chuyển khoản' }}</button>
        <RouterLink to="/orders" class="back-link">Xem trạng thái đơn hàng</RouterLink>
      </section>
    </main>
  </div>
</template>
