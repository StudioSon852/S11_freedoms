<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
const route=useRoute(), data=ref(null), error=ref(''), loading=ref(true)
const employee=computed(()=>route.path.startsWith('/employee'))
const isRefund=computed(()=>route.path.includes('/refunds/'))
const id=computed(()=>route.params.id)
const basePath=computed(()=>employee.value?'/employee':'/admin')
const money=v=>`${Number(v||0).toLocaleString('vi-VN')}₫`
async function load(){loading.value=true;error.value='';try{const ordersUrl=employee.value?'/api/pos/orders':'/api/admin/orders';if(isRefund.value){const [r,o]=await Promise.all([api.get('/api/admin/refunds'),api.get(ordersUrl)]);const refund=r.data.find(x=>String(x.id)===String(id.value));const order=o.data.find(x=>String(x.id)===String(refund?.orderId));data.value={refund,order}}else{const o=(await api.get(ordersUrl)).data;data.value={order:o.find(x=>String(x.id)===String(id.value))}}if(isRefund.value&&!data.value?.refund||!isRefund.value&&!data.value?.order)throw new Error('Không tìm thấy dữ liệu hóa đơn.')}catch(e){error.value=errorMessage(e)}finally{loading.value=false}}
function print(){window.print()}
onMounted(load)
</script>
<template>
  <div class="print-screen">
    <div class="print-toolbar"><RouterLink :to="isRefund?`${basePath}/refunds`:`${basePath}/orders`">← Quay lại</RouterLink><button class="btn btn-primary" :disabled="!data" @click="print">In / Lưu PDF</button></div>
    <p v-if="error" class="notice notice-error">{{error}}</p><p v-if="loading" class="muted">Đang tải dữ liệu hóa đơn…</p>
    <article v-if="data" class="print-paper">
      <header class="print-brand"><div><b>FREEDOM<span>S</span></b><small>STEP INTO YOUR STYLE</small></div><h1>{{isRefund?'PHIẾU HOÀN TIỀN':'HÓA ĐƠN BÁN HÀNG'}}</h1></header>
      <div class="print-meta"><span>Mã đơn: <b>{{data.order?.code||data.refund?.orderCode}}</b></span><span>Ngày lập: {{new Date(data.order?.createdAt||data.refund?.requestedAt).toLocaleString('vi-VN')}}</span><span>Khách hàng: {{data.order?.customerName||data.refund?.customerName}}</span><span>Điện thoại: {{data.order?.customerPhone||data.refund?.customerPhone||'—'}}</span><span>Kênh bán: {{data.order?.salesChannel||'—'}}</span></div>
      <template v-if="isRefund"><div class="print-refund-box"><b>THÔNG TIN HOÀN TIỀN</b><p>Mã yêu cầu: #{{data.refund?.id}}</p><p>Trạng thái: {{data.refund?.status}}</p><p>Phương thức hoàn: {{data.refund?.method}}</p><p>Lý do: {{data.refund?.reason||'—'}}</p><p>Mã giao dịch hoàn: {{data.refund?.reference||'Chưa xác nhận chi trả'}}</p><strong>Số tiền hoàn: {{money(data.refund?.amount)}}</strong></div><p class="print-note">Phiếu ghi nhận yêu cầu hoàn tiền. Chỉ xác nhận đã hoàn khi cửa hàng đã thực hiện chuyển khoản/chi tiền thực tế.</p></template>
      <template v-else><table class="print-lines"><thead><tr><th>Sản phẩm</th><th>SKU / Phân loại</th><th>SL</th><th>Thành tiền</th></tr></thead><tbody><tr v-for="(item,i) in (data.order?.items||[])" :key="i"><td>{{item.productName}}</td><td>{{item.sku}} · {{item.variantLabel}}</td><td>{{item.quantity}}</td><td>{{money(item.lineTotal)}}</td></tr><tr v-if="!(data.order?.items||[]).length"><td colspan="4">Không có dòng sản phẩm trong dữ liệu hóa đơn.</td></tr></tbody></table><div class="print-totals"><p>Tạm tính <b>{{money(data.order?.subtotal)}}</b></p><p>Giảm giá biến thể <b>-{{money(data.order?.variantDiscount)}}</b></p><p>Giảm giá thành viên <b>-{{money(data.order?.memberDiscount)}}</b></p><p>Coupon <b>-{{money(data.order?.couponDiscount)}}</b></p><p>Phí vận chuyển <b>{{money(data.order?.shippingFee)}}</b></p><p class="print-grand">TỔNG THANH TOÁN <strong>{{money(data.order?.totalAmount)}}</strong></p><p>Thanh toán: {{data.order?.paymentMethod}} / {{data.order?.paymentStatus}}</p><p>Địa chỉ: {{data.order?.shippingAddress||'Bán tại quầy'}}</p></div></template>
      <footer class="print-thanks">Cảm ơn quý khách đã mua sắm tại FREEDOMS!</footer>
    </article>
  </div>
</template>
