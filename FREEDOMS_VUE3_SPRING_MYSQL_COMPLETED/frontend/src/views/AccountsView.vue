<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
const canGrantAdmin = auth.user?.role === 'ADMIN'
const users = ref([])
const roleDrafts = ref({})
const error = ref('')
const success = ref('')
const creating = ref(false)
const savingRole = ref(null)
const form = ref({email:'',password:'',fullName:'',phone:'',roleCode:'EMPLOYEE'})
async function load() {
  error.value = ''
  try {
    users.value = (await api.get('/api/admin/users')).data
    roleDrafts.value = Object.fromEntries(users.value.map(u => [u.id, u.role]))
  } catch (e) { error.value = errorMessage(e) }
}
async function create() {
  error.value = ''; success.value = ''; creating.value = true
  try {
    await api.post('/api/admin/users', form.value)
    success.value = 'Đã tạo tài khoản nhân sự.'
    form.value = {email:'',password:'',fullName:'',phone:'',roleCode:'EMPLOYEE'}
    await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { creating.value = false }
}
async function toggle(u) {
  error.value = ''; success.value = ''
  try { await api.patch(`/api/admin/users/${u.id}/active`, {active:!u.active}); success.value='Đã cập nhật trạng thái tài khoản.'; await load() }
  catch (e) { error.value = errorMessage(e) }
}
async function saveRole(u) {
  const roleCode = roleDrafts.value[u.id]
  if (roleCode === u.role) return
  savingRole.value = u.id; error.value = ''; success.value = ''
  try {
    await api.patch(`/api/admin/users/${u.id}/role`, {roleCode})
    success.value = `Đã cập nhật quyền ${u.fullName} thành ${roleCode}.`
    await load()
  } catch (e) { error.value = errorMessage(e); roleDrafts.value[u.id] = u.role }
  finally { savingRole.value = null }
}
onMounted(load)
</script>
<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">ADMIN / ACCOUNTS & ACCESS</span><h1>Tài khoản, nhân viên & phân quyền</h1><p>Quản lý hồ sơ nhân sự, vai trò và quyền đăng nhập. Mật khẩu được băm BCrypt; không thể tự khóa hoặc hạ quyền Admin cuối cùng.</p></div></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>
    <section class="surface-card staff-crud-card">
      <div class="section-toolbar"><div><span class="eyebrow">CREATE / STAFF</span><h2>Tạo tài khoản nhân sự</h2></div><span class="channel-pill">Admin · Manager · Employee</span></div>
      <form class="coupon-form-grid" @submit.prevent="create">
        <label>Họ và tên<input v-model.trim="form.fullName" required maxlength="150" /></label>
        <label>Email đăng nhập<input v-model.trim="form.email" type="email" required /></label>
        <label>Số điện thoại<input v-model.trim="form.phone" maxlength="30" /></label>
        <label>Mật khẩu ban đầu<input v-model="form.password" type="password" minlength="8" maxlength="72" required autocomplete="new-password" /></label>
        <label>Vai trò<select v-model="form.roleCode"><option value="EMPLOYEE">Nhân viên</option><option value="MANAGER">Quản lý</option><option v-if="canGrantAdmin" value="ADMIN">Quản trị viên</option><option v-else value="ADMIN" disabled>Quản trị viên</option></select></label>
        <button class="btn btn-primary" :disabled="creating">{{ creating?'Đang tạo…':'Tạo tài khoản' }}</button>
      </form>
    </section>
    <section class="surface-card">
      <div class="section-toolbar"><div><span class="eyebrow">READ / UPDATE / ACCESS CONTROL</span><h2>Danh sách tài khoản</h2></div><button class="btn btn-outline btn-sm" @click="load">Làm mới</button></div>
      <div v-for="u in users" :key="u.id" class="staff-account-row">
        <div class="staff-account-main"><div class="staff-avatar">{{ (u.fullName || u.email || 'U').slice(0,1).toUpperCase() }}</div><div><b>{{ u.fullName }}</b><small>#{{ u.id }} · {{ u.email }} · {{ u.phone || 'Chưa có số điện thoại' }}</small><span :class="u.active?'text-success':'muted'">{{ u.active?'Đang hoạt động':'Đã khóa' }}</span></div></div>
        <div class="staff-account-controls">
          <label v-if="u.role!=='CUSTOMER'">Phân quyền<select v-model="roleDrafts[u.id]" :disabled="savingRole===u.id"><option value="EMPLOYEE">Nhân viên</option><option value="MANAGER">Quản lý</option><option v-if="canGrantAdmin" value="ADMIN">Quản trị viên</option><option v-else value="ADMIN" disabled>Quản trị viên</option></select></label>
          <span v-else class="channel-pill">Khách hàng</span>
          <button v-if="u.role!=='CUSTOMER'" class="btn btn-outline btn-sm" :disabled="savingRole===u.id || roleDrafts[u.id]===u.role" @click="saveRole(u)">{{ savingRole===u.id?'Đang lưu…':'Lưu quyền' }}</button>
          <button class="btn btn-danger-outline btn-sm" :disabled="u.id===auth.user?.id" @click="toggle(u)">{{ u.active?'Khóa':'Mở khóa' }}</button>
        </div>
      </div>
      <div v-if="!users.length" class="empty-state">Chưa có tài khoản.</div>
    </section>
    <p class="muted small-text">Có thể khóa/mở khóa tài khoản khách hàng và nhân viên. Vai trò của tài khoản CUSTOMER không thể chuyển thành vai trò nhân sự; không thể tự khóa tài khoản hoặc khóa Admin cuối cùng còn hoạt động.</p>
  </div>
</template>
<style scoped>
.staff-account-row{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:15px 0;border-top:1px solid #eee}.staff-account-main{display:flex;align-items:center;gap:12px;min-width:0}.staff-avatar{display:grid;place-items:center;width:38px;height:38px;border-radius:50%;background:#edf0f3;color:#222;font-size:13px;font-weight:900;flex:none}.staff-account-main>div:last-child{display:grid;gap:4px;min-width:0}.staff-account-main b{font-size:12px}.staff-account-main small{font-size:10px;color:#777;overflow-wrap:anywhere}.staff-account-main span{font-size:10px}.staff-account-controls{display:flex;align-items:end;justify-content:flex-end;gap:8px;flex-wrap:wrap}.staff-account-controls label{display:grid;gap:4px;font-size:9px;font-weight:800}.staff-account-controls select{min-width:130px;padding:8px;border:1px solid #ddd;border-radius:4px;background:#fff;color:#222;font:inherit}@media(max-width:700px){.staff-account-row{align-items:flex-start;flex-direction:column}.staff-account-controls{justify-content:flex-start;width:100%}}
</style>
