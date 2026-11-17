<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'
const route = useRoute()
const type = computed(() => ['categories','brands','colors','sizes'].includes(route.params.type) ? route.params.type : 'categories')
const titles = { categories:'Danh mục', brands:'Thương hiệu', colors:'Màu sắc', sizes:'Kích cỡ' }
const items = ref([]), q = ref(''), error = ref(''), success = ref(''), form = ref(empty()), editId = ref(null), saving = ref(false)
function empty(){return {name:'',hex:'#000000',sortOrder:0}}
const filtered = computed(() => items.value.filter(x => `${x.name} ${x.id}`.toLowerCase().includes(q.value.toLowerCase())))
async function load(){error.value='';try{items.value=(await api.get(`/api/admin/catalog/${type.value}`)).data}catch(e){error.value=errorMessage(e)}}
function edit(x){editId.value=x.id;form.value={name:x.name,hex:x.hex||'#000000',sortOrder:x.sortOrder||0}}
function reset(){editId.value=null;form.value=empty()}
async function save(){saving.value=true;error.value='';success.value='';try{const payload={name:form.value.name.trim(),hex:form.value.hex,sortOrder:Number(form.value.sortOrder||0)};if(editId.value)await api.patch(`/api/admin/catalog/${type.value}/${editId.value}`,payload);else await api.post(`/api/admin/catalog/${type.value}`,payload);success.value=editId.value?'Đã cập nhật mục danh mục.':'Đã tạo mục danh mục.';reset();await load()}catch(e){error.value=errorMessage(e)}finally{saving.value=false}}
async function remove(x){if(!confirm(`Xóa ${titles[type.value].toLowerCase()} “${x.name}”?`))return;try{await api.delete(`/api/admin/catalog/${type.value}/${x.id}`);success.value='Đã xóa mục danh mục.';await load()}catch(e){error.value=errorMessage(e)}}
watch(type,()=>{reset();load()});onMounted(load)
</script>
<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">PRODUCT MASTER DATA</span><h1>Quản lý {{ titles[type] }}</h1><p>CRUD danh mục dùng chung cho sản phẩm và biến thể. Dữ liệu đang được sản phẩm sử dụng sẽ không cho xóa để tránh mất liên kết.</p></div></div>
    <nav class="catalog-tabs"><RouterLink v-for="tab in ['categories','brands','colors','sizes']" :key="tab" :to="`/admin/catalog/${tab}`" :class="{active:type===tab}">{{ titles[tab] }}</RouterLink></nav>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
    <section class="surface-card"><div class="section-toolbar"><div><span class="eyebrow">{{ editId?'UPDATE':'CREATE' }}</span><h2>{{ editId?'Chỉnh sửa':'Thêm mới' }} {{ titles[type].toLowerCase() }}</h2></div></div>
      <form class="catalog-edit-form" @submit.prevent="save"><label>Tên<input v-model.trim="form.name" required maxlength="100" /></label><label v-if="type==='colors'">Mã màu<input v-model="form.hex" type="color" /></label><label v-if="type==='sizes'">Thứ tự sắp xếp<input v-model="form.sortOrder" type="number" min="0" /></label><button class="btn btn-primary" :disabled="saving">{{ saving?'Đang lưu…':(editId?'Lưu thay đổi':'Thêm') }}</button><button v-if="editId" class="btn btn-outline" type="button" @click="reset">Hủy</button></form>
    </section>
    <section class="surface-card"><div class="section-toolbar"><h2>Danh sách {{ titles[type].toLowerCase() }}</h2><div class="search-tools"><input v-model="q" placeholder="Tìm theo tên / mã"/><button class="btn btn-outline btn-sm" @click="load">Làm mới</button></div></div>
      <div class="table-responsive"><table class="data-table"><thead><tr><th>Mã</th><th>Tên</th><th v-if="type==='colors'">Mã màu</th><th v-if="type==='sizes'">Thứ tự</th><th>Thao tác</th></tr></thead><tbody><tr v-for="x in filtered" :key="x.id"><td>#{{x.id}}</td><td>{{x.name}}</td><td v-if="type==='colors'"><span class="catalog-color-dot" :style="{background:x.hex||'#000'}"></span>{{x.hex}}</td><td v-if="type==='sizes'">{{x.sortOrder}}</td><td><button class="btn btn-outline btn-sm" @click="edit(x)">Sửa</button> <button class="btn btn-danger-outline btn-sm" @click="remove(x)">Xóa</button></td></tr><tr v-if="!filtered.length"><td :colspan="type==='colors'||type==='sizes'?4:3" class="empty-state">Không có dữ liệu.</td></tr></tbody></table></div>
    </section>
  </div>
</template>
