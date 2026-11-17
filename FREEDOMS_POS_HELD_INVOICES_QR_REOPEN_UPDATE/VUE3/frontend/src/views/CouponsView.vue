<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
const route = useRoute()
const channel = computed(() => route.meta.channel || 'ONLINE')
const coupons = ref([]), q = ref(''), error = ref(''), success = ref(''), saving = ref(false), editingId = ref(null)
const emptyForm = () => ({ couponCode:'', discountType:'PERCENT', discountValue:10, minOrderAmount:0, maxDiscount:'', usageLimit:'', startsAt:'', endsAt:'' })
const form = ref(emptyForm())
function dateInput(value) { return value ? String(value).slice(0,16) : '' }
function payload() { return { couponCode:form.value.couponCode.trim().toUpperCase(), salesChannel:channel.value, discountType:form.value.discountType,
  discountValue:Number(form.value.discountValue), minOrderAmount:Number(form.value.minOrderAmount||0),
  maxDiscount:form.value.maxDiscount===''?null:Number(form.value.maxDiscount), usageLimit:form.value.usageLimit===''?null:Number(form.value.usageLimit),
  startsAt:form.value.startsAt||null, endsAt:form.value.endsAt||null, active:true } }
async function load() { error.value=''; try { coupons.value=(await api.get('/api/admin/coupons',{params:{channel:channel.value,q:q.value}})).data } catch(e){error.value=errorMessage(e)} }
function edit(c) { editingId.value=c.id; form.value={couponCode:c.code,discountType:c.discountType,discountValue:c.discountValue,minOrderAmount:c.minOrderAmount||0,maxDiscount:c.maxDiscount??'',usageLimit:c.usageLimit??'',startsAt:dateInput(c.startsAt),endsAt:dateInput(c.endsAt)}; document.querySelector('.coupon-editor')?.scrollIntoView({behavior:'smooth',block:'start'}) }
function reset() { editingId.value=null; form.value=emptyForm() }
async function save() { saving.value=true;error.value='';success.value='';try{const data=payload();if(editingId.value){await api.patch(`/api/admin/coupons/${editingId.value}`,data);success.value=`Đã cập nhật mã ${data.couponCode}.`}else{await api.post('/api/admin/coupons',data);success.value=`Đã tạo mã ${data.couponCode}.`}reset();await load()}catch(e){error.value=errorMessage(e)}finally{saving.value=false} }
async function toggle(c) { error.value='';try{await api.patch(`/api/admin/coupons/${c.id}`,{couponCode:c.code,salesChannel:c.salesChannel,discountType:c.discountType,discountValue:Number(c.discountValue),minOrderAmount:Number(c.minOrderAmount||0),maxDiscount:c.maxDiscount,usageLimit:c.usageLimit,startsAt:c.startsAt||null,endsAt:c.endsAt||null,active:!c.active});success.value=c.active?'Đã chuyển mã sang Dừng.':'Đã chuyển mã sang Hoạt động.';await load()}catch(e){error.value=errorMessage(e)} }
async function remove(c) { if(!confirm(`Xóa vĩnh viễn mã ${c.code}? Chỉ xóa được khi mã chưa phát sinh lượt sử dụng.`))return;try{await api.delete(`/api/admin/coupons/${c.id}`);success.value=`Đã xóa mã ${c.code}.`;await load()}catch(e){error.value=errorMessage(e)} }
function money(v){return `${Number(v||0).toLocaleString('vi-VN')}₫`}
watch(channel,()=>{reset();load()});onMounted(load)
</script>
<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">PROMOTIONS / {{channel}}</span><h1>Mã giảm giá {{channel==='ONLINE'?'Online':'Offline'}}</h1><p>Tìm kiếm, thêm, sửa và dừng coupon theo kênh. Mã BOTH vẫn được áp dụng ở Online và POS theo cấu hình.</p></div><button class="btn btn-outline" @click="load">Làm mới</button></div>
    <p v-if="error" class="notice notice-error">{{error}}</p><p v-if="success" class="notice notice-success">{{success}}</p>
    <section class="surface-card coupon-editor"><div class="section-toolbar"><div><span class="eyebrow">{{editingId?'UPDATE COUPON':'CREATE COUPON'}}</span><h2>{{editingId?`Chỉnh sửa mã #${editingId}`:`Tạo coupon ${channel}`}}</h2></div><button v-if="editingId" class="btn btn-outline btn-sm" type="button" @click="reset">Hủy sửa</button></div>
      <form class="coupon-form-grid" @submit.prevent="save"><label>Mã giảm giá<input v-model.trim="form.couponCode" required maxlength="40" placeholder="VD: FREEDOMS15"/></label><label>Kiểu giảm<select v-model="form.discountType"><option value="PERCENT">Phần trăm (%)</option><option value="AMOUNT">Số tiền (₫)</option></select></label><label>Giá trị<input v-model="form.discountValue" type="number" min="1" required/></label><label>Đơn tối thiểu<input v-model="form.minOrderAmount" type="number" min="0"/></label><label>Giảm tối đa (₫)<input v-model="form.maxDiscount" type="number" min="0"/></label><label>Giới hạn lượt<input v-model="form.usageLimit" type="number" min="1"/></label><label>Bắt đầu<input v-model="form.startsAt" type="datetime-local"/></label><label>Kết thúc<input v-model="form.endsAt" type="datetime-local"/></label><button class="btn btn-primary" :disabled="saving">{{saving?'Đang lưu…':(editingId?'Lưu thay đổi':'Tạo mã')}}</button></form>
    </section>
    <section class="surface-card"><div class="section-toolbar"><div><h2>Danh sách mã {{channel}}</h2><small class="muted">{{coupons.length}} mã phù hợp</small></div><div class="search-tools"><input v-model="q" @keyup.enter="load" placeholder="Tìm mã coupon…"/><button class="btn btn-outline btn-sm" @click="load">Tìm kiếm</button></div></div>
      <div class="coupon-list"><article v-for="c in coupons" :key="c.id" class="coupon-admin-card"><div><b>{{c.code}} <span class="channel-pill">{{c.salesChannel}}</span></b><small>{{c.discountType==='PERCENT'?`${c.discountValue}%`:`${money(c.discountValue)}`}} · đơn tối thiểu {{money(c.minOrderAmount)}}</small><small>Đã dùng {{c.usageCount}} / {{c.usageLimit??'không giới hạn'}} · {{c.startsAt?new Date(c.startsAt).toLocaleString('vi-VN'):'Ngay lập tức'}} → {{c.endsAt?new Date(c.endsAt).toLocaleString('vi-VN'):'Không thời hạn'}}</small></div><div class="coupon-card-actions"><span :class="c.active?'text-success':'muted'">{{c.active?'Hoạt động':'Dừng'}}</span><button class="btn btn-outline btn-sm" @click="edit(c)">Sửa</button><button class="btn btn-outline btn-sm" @click="toggle(c)">{{c.active?'Dừng':'Hoạt động'}}</button><button class="btn btn-danger-outline btn-sm" @click="remove(c)">Xóa</button></div></article><div v-if="!coupons.length" class="empty-state">Chưa có coupon trong kênh này.</div></div>
    </section>
  </div>
</template>
