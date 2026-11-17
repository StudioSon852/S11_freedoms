<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const email = ref('')
const password = ref('')
const error = ref('')
const showRegister = ref(route.path === '/register' || route.path === '/signup')
const registerForm = ref({ email: '', password: '', confirmPassword: '', fullName: '', phone: '' })

watch(() => route.path, path => { showRegister.value = path === '/register' || path === '/signup' })

function openRegister() {
  error.value = ''
  showRegister.value = true
  if (route.path !== '/register') router.replace({ path: '/register', query: route.query })
}
function openLogin() {
  error.value = ''
  showRegister.value = false
  if (route.path !== '/login') router.replace({ path: '/login', query: route.query })
}

async function login() {
  error.value = ''
  try {
    const user = await auth.login(email.value, password.value)
    const destination = route.query.redirect
    router.push(destination || (user.role === 'EMPLOYEE' ? '/employee/overview' : user.role === 'CUSTOMER' ? '/store' : '/admin/overview'))
  } catch { error.value = auth.error }
}

async function register() {
  error.value = ''
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    error.value = 'Mật khẩu xác nhận không khớp.'
    return
  }
  try {
    await auth.register(registerForm.value)
    showRegister.value = false
    email.value = registerForm.value.email
    password.value = ''
    error.value = 'Đăng ký thành công. Hãy đăng nhập bằng mật khẩu vừa tạo.'
    if (route.path !== '/login') router.replace('/login')
  } catch (e) { error.value = e?.response?.data?.message || auth.error }
}
</script>

<template>
  <section class="auth-page" :class="{ 'register-mode': showRegister }">
    <div class="auth-card">
      <RouterLink to="/store" class="auth-brand">
        <img src="/img/freedoms-bird.png" alt="FREEDOMS" />
        <span><b>FREEDOM<span class="freedom-red-s">S</span></b><small>STEP INTO YOUR STYLE</small></span>
      </RouterLink>
      <span class="auth-eyebrow">TÀI KHOẢN FREEDOMS</span>
      <h1>{{ showRegister ? 'Tạo tài khoản' : 'Chào mừng trở lại' }}</h1>
      <p class="auth-description">{{ showRegister ? 'Đăng ký tài khoản khách hàng để mua sắm và theo dõi đơn hàng thuận tiện hơn.' : 'Đăng nhập để tiếp tục mua sắm hoặc quản lý cửa hàng.' }}</p>

      <form v-if="!showRegister" @submit.prevent="login" class="auth-form">
        <label>Email<input v-model.trim="email" type="email" autocomplete="username" required placeholder="email@example.com" /></label>
        <label>Mật khẩu<input v-model="password" type="password" autocomplete="current-password" required placeholder="Nhập mật khẩu" /></label>
        <p v-if="error" class="auth-message" :class="error.includes('thành công') ? 'success' : 'error'">{{ error }}</p>
        <button class="auth-submit" type="submit" :disabled="auth.busy">{{ auth.busy ? 'ĐANG ĐĂNG NHẬP…' : 'ĐĂNG NHẬP' }} <span>→</span></button>
        <p class="auth-switch">Chưa có tài khoản? <button type="button" @click="openRegister">Đăng ký khách hàng</button></p>
      </form>

      <form v-else @submit.prevent="register" class="auth-form">
        <label>Họ và tên<input v-model.trim="registerForm.fullName" required maxlength="150" autocomplete="name" placeholder="Nguyễn Văn A" /></label>
        <div class="auth-two-fields">
          <label>Số điện thoại<input v-model.trim="registerForm.phone" maxlength="30" autocomplete="tel" placeholder="09xxxxxxxx" /></label>
          <label>Email<input v-model.trim="registerForm.email" type="email" required maxlength="190" autocomplete="email" placeholder="email@example.com" /></label>
        </div>
        <div class="auth-two-fields">
          <label>Mật khẩu<input v-model="registerForm.password" type="password" required minlength="8" autocomplete="new-password" placeholder="Tối thiểu 8 ký tự" /></label>
          <label>Nhập lại mật khẩu<input v-model="registerForm.confirmPassword" type="password" required minlength="8" autocomplete="new-password" placeholder="Xác nhận mật khẩu" /></label>
        </div>
        <p v-if="error" class="auth-message" :class="error.includes('thành công') ? 'success' : 'error'">{{ error }}</p>
        <button class="auth-submit" type="submit" :disabled="auth.busy">{{ auth.busy ? 'ĐANG TẠO TÀI KHOẢN…' : 'TẠO TÀI KHOẢN' }} <span>→</span></button>
        <p class="auth-switch">Đã có tài khoản? <button type="button" @click="openLogin">Quay lại đăng nhập</button></p>
      </form>
      <RouterLink to="/store" class="auth-back">← Quay lại cửa hàng</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.auth-page{box-sizing:border-box;width:100%;min-height:calc(100vh - 0px);display:grid;place-items:center;padding:28px 18px;background:linear-gradient(to bottom,#2c3e50 0%,#3498db 43%,#eaf4ff 79%,#fff 100%);}
.auth-card{box-sizing:border-box;width:min(480px,100%);padding:34px 36px;background:rgba(255,255,255,.96);border:1px solid rgba(255,255,255,.75);border-radius:14px;box-shadow:0 22px 65px rgba(18,44,70,.20);backdrop-filter:blur(12px);color:#17324d;}
.auth-brand{display:flex;align-items:center;gap:12px;margin-bottom:23px;text-decoration:none;color:#17324d;}.auth-brand img{width:54px;height:52px;object-fit:contain;}.auth-brand b{display:block;font-size:17px;font-weight:900;letter-spacing:.14em;}.freedom-red-s{color:#c62828}.auth-brand small{display:block;margin-top:4px;color:#6d879d;font-size:8px;font-weight:700;letter-spacing:.16em;}
.auth-eyebrow{display:block;color:#4d7290;font-size:9px;font-weight:900;letter-spacing:.16em;margin-bottom:7px;}.auth-card h1{font-family:Manrope,system-ui,sans-serif;font-size:29px;line-height:1.2;margin:0 0 8px;color:#172b3b;letter-spacing:-.035em;}.auth-description{font-size:12px;line-height:1.6;color:#718092;margin:0 0 22px;}
.auth-form{display:flex;flex-direction:column;gap:13px;}.auth-form label{display:flex;flex-direction:column;gap:7px;font-size:11px;color:#25394b;font-weight:800;}.auth-form input{box-sizing:border-box;width:100%;min-width:0;border:1px solid #d6e0e8;border-radius:7px;padding:12px;background:#fff;color:#172b3b;outline:none;font-size:12px;}.auth-form input:focus{border-color:#2e7fb9;box-shadow:0 0 0 3px rgba(52,152,219,.12);}.auth-two-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:11px;}
.auth-submit{display:flex;justify-content:space-between;align-items:center;width:100%;margin-top:3px;border:0;border-radius:7px;background:#172b3b;color:#fff;padding:14px 15px;font-size:10px;font-weight:900;letter-spacing:.08em;cursor:pointer;}.auth-submit:hover{background:#244b69;}.auth-submit:disabled{opacity:.65;cursor:wait;}.auth-message{font-size:11px;line-height:1.5;border-radius:6px;padding:10px 12px;margin:0;}.auth-message.error{color:#9c2727;background:#fff1f0;border:1px solid #f0ceca;}.auth-message.success{color:#176b4c;background:#eefaf3;border:1px solid #ccebd8;}
.auth-switch{text-align:center;font-size:11px;color:#66788a;margin:3px 0 0;}.auth-switch button{border:0;background:none;padding:0;color:#17699f;font:inherit;font-weight:900;cursor:pointer;}.auth-switch button:hover{text-decoration:underline;}.auth-back{display:block;margin-top:21px;text-align:center;font-size:10px;color:#61778a;text-decoration:none;}.auth-back:hover{color:#17699f;}
@media(max-width:520px){.auth-page{padding:14px;}.auth-card{padding:27px 20px;}.auth-two-fields{grid-template-columns:1fr;gap:13px;}}
</style>
