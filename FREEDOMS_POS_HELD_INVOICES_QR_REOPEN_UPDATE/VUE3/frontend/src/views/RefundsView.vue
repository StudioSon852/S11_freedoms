<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
const route = useRoute()
const basePath = computed(() => route.path.startsWith('/employee') ? '/employee' : '/admin')
const refunds = ref([])
const error = ref('')
const success = ref('')
const reference = ref({})
const note = ref({})
const busy = ref(null)
async function load() {
  error.value = ''
  try { refunds.value = (await api.get('/api/admin/refunds')).data }
  catch (e) { error.value = errorMessage(e) }
}
async function complete(r) {
  if (!reference.value[r.id]?.trim()) return error.value = 'Nhập mã giao dịch hoàn tiền hoặc số phiếu chi trước khi xác nhận.'
  if (!confirm(`Xác nhận đã hoàn ${Number(r.amount).toLocaleString('vi-VN')}₫ cho đơn ${r.orderCode}? Chỉ xác nhận sau khi đã chuyển/chi tiền thật.`)) return
  busy.value = r.id; error.value = ''; success.value = ''
  try {
    await api.post(`/api/admin/refunds/${r.id}/complete`, { reference: reference.value[r.id].trim(), note: note.value[r.id] || 'Đã hoàn tiền thủ công' })
    success.value = 'Đã ghi nhận khoản hoàn tiền thực tế.'
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = null }
}
async function reject(r) {
  const reason = prompt(`Lý do từ chối yêu cầu hoàn tiền #${r.id} của đơn ${r.orderCode}:`)
  if (!reason?.trim()) return
  if (!confirm('Từ chối yêu cầu này? Số tiền sẽ được giải phóng khỏi hạn mức hoàn và khách có thể gửi yêu cầu khác nếu còn lượt/thời hạn.')) return
  busy.value = r.id; error.value = ''; success.value = ''
  try {
    await api.post(`/api/admin/refunds/${r.id}/reject`, { note: reason.trim() })
    success.value = `Đã từ chối yêu cầu #${r.id}.`
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = null }
}
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')}₫` }
onMounted(load)
</script>
<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">AFTER-SALES / REFUND CENTER</span><h1>Quản lý yêu cầu hoàn tiền</h1><p>Hỗ trợ hoàn toàn phần và hoàn một phần. Chỉ ghi nhận REFUNDED sau khi nhân viên đã chuyển/chi tiền thực tế.</p></div><button class="btn btn-outline" @click="load">Làm mới</button></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
    <div v-for="r in refunds" :key="r.id" class="surface-card refund-card">
      <div class="order-card-head"><div><small>YÊU CẦU #{{ r.id }} · ĐƠN {{ r.orderCode }}</small><b>{{ r.customerName }} · {{ r.customerPhone || 'Không có SĐT' }}</b></div><div class="order-card-head-actions"><span class="status-pill" :class="'status-'+String(r.status).toLowerCase()">{{ r.status }}</span><RouterLink class="btn btn-outline btn-sm" :to="`${basePath}/refunds/${r.id}/print`">In phiếu hoàn tiền</RouterLink></div></div>
      <div class="refund-amount">{{ money(r.amount) }}</div>
      <p class="muted">Phương thức gốc: {{ r.method }} · Yêu cầu: {{ new Date(r.requestedAt).toLocaleString('vi-VN') }}</p>
      <p>{{ r.reason }}</p>
      <div v-if="r.status==='PENDING'" class="refund-action-panel">
        <label>Mã giao dịch hoàn tiền / số phiếu chi<input v-model="reference[r.id]" placeholder="Nhập tham chiếu đối soát" /></label>
        <label>Ghi chú xử lý<input v-model="note[r.id]" placeholder="Ghi chú tùy chọn" /></label>
        <div class="refund-action-buttons"><button class="btn btn-primary btn-sm" :disabled="busy===r.id || !reference[r.id]?.trim()" @click="complete(r)">{{ busy===r.id?'Đang lưu…':'Xác nhận đã hoàn tiền' }}</button><button class="btn btn-danger-outline btn-sm" :disabled="busy===r.id" @click="reject(r)">Từ chối yêu cầu</button></div>
      </div>
      <div v-else-if="r.status==='REFUNDED'" class="muted small-text">Đã xử lý · mã hoàn: {{ r.reference || 'Đã ghi nhận' }}</div>
      <div v-else class="muted small-text">Yêu cầu đã bị từ chối.</div>
    </div>
    <div v-if="!refunds.length" class="surface-card empty-state">Chưa phát sinh yêu cầu hoàn tiền.</div>
    <section class="surface-card policy-card"><h3>Kiểm soát số tiền và số lượt</h3><ul><li>Mỗi giao dịch thanh toán được gửi tối đa 2 yêu cầu, kể cả yêu cầu đã bị từ chối.</li><li>Tổng tiền của các khoản đang chờ và đã hoàn không vượt số tiền của giao dịch gốc.</li><li>Hoàn một phần: trạng thái giao dịch chỉ chuyển REFUNDED khi tổng số tiền thực tế đã hoàn đủ; nếu còn yêu cầu chờ xử lý, vẫn giữ REFUND_PENDING.</li></ul></section>
  </div>
</template>
<style scoped>
.refund-action-panel{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:13px;background:#fafafa;border:1px solid #eee;border-radius:7px}.refund-action-panel label{display:flex;flex-direction:column;gap:6px;font-size:10px;font-weight:800}.refund-action-panel input{padding:9px;border:1px solid #ddd;border-radius:5px;font:inherit}.refund-action-buttons{grid-column:1/-1;display:flex;gap:8px;flex-wrap:wrap}@media(max-width:640px){.refund-action-panel{grid-template-columns:1fr}}
</style>
