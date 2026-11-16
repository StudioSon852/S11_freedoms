<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
import RefundRequestForm from '../components/RefundRequestForm.vue'
const route=useRoute()
const orders=ref([])
const error=ref('')
const success=ref('')
const busy=ref(null)
const reference=ref({})
const note=ref({})
const refundOpenId=ref(null)
const filters=reactive({date:String(route.query.date||''),orderCode:String(route.query.orderCode||''),status:String(route.query.status||'')})
async function load(){try{error.value='';orders.value=(await api.get('/api/admin/orders',{params:filters})).data}catch(e){error.value=errorMessage(e)}}
function resetFilters(){filters.date='';filters.orderCode='';filters.status='';load()}
async function updateStatus(o,status){busy.value=o.id;error.value='';success.value='';try{await api.post(`/api/admin/orders/${o.id}/status`,{status,note:note.value[o.id]||`Admin cập nhật ${status}`});success.value='Đã cập nhật trạng thái đơn hàng.';await load()}catch(e){error.value=errorMessage(e)}finally{busy.value=null}}
async function confirmPayment(o){busy.value=o.id;error.value='';try{await api.post(`/api/admin/orders/${o.id}/confirm-payment`,{reference:reference.value[o.id]||`MANUAL-${Date.now()}`});await load()}catch(e){error.value=errorMessage(e)}finally{busy.value=null}}
async function cancelNoPayment(o){if(!confirm('Đã kiểm tra tài khoản ngân hàng và xác nhận chưa nhận tiền? Chỉ chọn sau khi đã đối soát thật.'))return;busy.value=o.id;error.value='';try{await api.post(`/api/admin/orders/${o.id}/cancel-no-payment`,{note:note.value[o.id]||'Đã đối soát không nhận được tiền'});await load()}catch(e){error.value=errorMessage(e)}finally{busy.value=null}}
async function requestRefund(o,payload){busy.value=o.id;error.value='';success.value='';try{await api.post(`/api/admin/orders/${o.id}/refund-request`,payload);success.value=`Đã tạo yêu cầu hoàn tiền cho ${o.code}.`;refundOpenId.value=null;await load()}catch(e){error.value=errorMessage(e)}finally{busy.value=null}}
function options(o){if(o.paymentStatus==='WAITING_CONFIRMATION')return [];if(o.status==='PENDING_PAYMENT')return ['CANCELLED'];if(o.status==='CONFIRMED')return ['PROCESSING','CANCELLED'];if(o.status==='PROCESSING')return ['SHIPPING','CANCELLED'];if(o.status==='SHIPPING')return ['COMPLETED'];return []}
function fmt(v){return `${Number(v||0).toLocaleString('vi-VN')}₫`}
onMounted(load)
</script>
<template>
  <div class="view-wrap"><div class="view-heading"><div><span class="eyebrow">ORDER MANAGEMENT</span><h1>Đơn hàng & thanh toán</h1><p>Chuyển khoản online được xác nhận xong sẽ sang CONFIRMED; chuyển khoản POS được xác nhận xong tự sang COMPLETED.</p></div><button class="btn btn-outline" @click="load">Làm mới</button></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
    <section class="surface-card order-filter-panel"><div class="order-filter-grid"><label>Ngày tạo đơn<input v-model="filters.date" type="date" /></label><label>Mã đơn<input v-model.trim="filters.orderCode" placeholder="Nhập mã đơn" @keyup.enter="load" /></label><label>Trạng thái đơn/thanh toán<select v-model="filters.status"><option value="">Tất cả trạng thái</option><option value="PENDING_PAYMENT">PENDING_PAYMENT</option><option>CONFIRMED</option><option>PROCESSING</option><option>SHIPPING</option><option>COMPLETED</option><option>CANCELLED</option><option>WAITING_CONFIRMATION</option><option>PAID</option><option>REFUND_PENDING</option><option>REFUNDED</option><option>UNPAID</option><option>PENDING</option></select></label><button class="btn btn-primary" @click="load">Tìm kiếm</button><button class="btn btn-outline" @click="resetFilters">Đặt lại</button></div></section>
    <article v-for="o in orders" :key="o.id" class="surface-card order-admin-card"><div class="order-card-head"><div><small>{{ o.salesChannel }} · {{ o.code }}</small><b>{{ o.customerName }} · {{ o.customerPhone }}</b></div><div class="order-card-head-actions"><span class="status-pill" :class="'status-'+String(o.status).toLowerCase()">{{ o.status }}</span><RouterLink class="btn btn-outline btn-sm" :to="`/admin/orders/${o.id}/print`">In hóa đơn</RouterLink></div></div>
      <div class="order-grid-info"><span>Tiền hàng: <b>{{ fmt(o.subtotal) }}</b></span><span>Phí ship: <b>{{ fmt(o.shippingFee) }}</b></span><span>Phương thức: <b>{{ o.paymentMethod }} / {{ o.paymentStatus }}</b></span><span>Tổng: <b>{{ fmt(o.totalAmount) }}</b></span></div>
      <div v-if="o.paymentMethod==='BANK_TRANSFER' && ['PENDING','WAITING_CONFIRMATION'].includes(o.paymentStatus)" class="confirm-row"><input v-model="reference[o.id]" placeholder="Mã giao dịch ngân hàng / ghi chú"/><button class="btn btn-primary btn-sm" :disabled="busy===o.id" @click="confirmPayment(o)">Xác nhận tiền vào</button></div>
      <div v-if="o.paymentStatus==='WAITING_CONFIRMATION'" class="confirm-row"><span class="muted small-text">Khách đã báo chuyển khoản: xác nhận tiền vào hoặc đối soát chưa nhận.</span><button class="btn btn-danger-outline btn-sm" :disabled="busy===o.id" @click="cancelNoPayment(o)">Đối soát chưa nhận → hủy đơn</button></div>
      <div v-if="options(o).length" class="order-status-actions"><input v-model="note[o.id]" placeholder="Lý do / ghi chú thay đổi trạng thái"/><button v-for="status in options(o)" :key="status" :class="status==='CANCELLED'?'btn btn-danger-outline btn-sm':'btn btn-outline btn-sm'" :disabled="busy===o.id" @click="updateStatus(o,status)">{{ status==='CANCELLED'?'Hủy đơn / tạo yêu cầu hoàn tiền':`Chuyển ${status}` }}</button></div>
      <div v-if="o.refundEligibility?.eligible" class="order-after-sale"><span>Khả năng hoàn: <b>{{ fmt(o.refundEligibility.remainingAmount) }}</b> · còn {{ o.refundEligibility.requestsRemaining }}/2 lượt · hạn {{ new Date(o.refundEligibility.deadline).toLocaleString('vi-VN') }}</span><button class="btn btn-outline btn-sm" @click="refundOpenId=refundOpenId===o.id?null:o.id">{{ refundOpenId===o.id?'Đóng':'Tạo yêu cầu hoàn tiền' }}</button></div>
      <p v-else-if="['PAID','REFUND_PENDING'].includes(o.paymentStatus) && o.paymentStatus!=='REFUNDED'" class="muted small-text">{{ o.refundEligibility?.message }}</p>
      <RefundRequestForm v-if="refundOpenId===o.id && o.refundEligibility?.eligible" :order="o" @cancel="refundOpenId=null" @submit="requestRefund(o,$event)" />
      <p v-if="o.status==='COMPLETED'" class="completion-lock-note">Đơn đã COMPLETED: không thể hủy trực tiếp. Chỉ tiếp nhận yêu cầu hoàn tiền trong thời hạn hợp lệ.</p>
    </article><div v-if="!orders.length" class="surface-card empty-state">Chưa có đơn hàng.</div>
    <section class="surface-card policy-card"><h3>Quy tắc hủy đơn và hoàn tiền</h3><ul><li>Đơn chưa giao có thể được hủy theo luồng trạng thái; nếu đã thanh toán, hệ thống tự tạo yêu cầu hoàn phần tiền còn lại.</li><li>Đơn `COMPLETED` không được chuyển thẳng sang `CANCELLED`, kể cả POS. Phải gửi yêu cầu hoàn tiền từ giao dịch đã thanh toán.</li><li>Online: 48 giờ; POS: 24 giờ kể từ thời điểm thanh toán. Mỗi giao dịch tối đa 2 yêu cầu, tổng tiền được hoàn không vượt số tiền giao dịch.</li></ul></section>
  </div>
</template>
<style scoped>
.order-after-sale{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-top:12px;padding:11px;background:#f8f8f8;border:1px solid #e8e8e8;border-radius:6px;font-size:10px;color:#555}.order-after-sale b{color:#111}.completion-lock-note{font-size:10px;color:#8a5a12;background:#fff9e9;padding:8px 10px;border-radius:5px;margin:10px 0 0}
</style>
