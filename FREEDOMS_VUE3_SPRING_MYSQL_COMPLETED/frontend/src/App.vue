<script setup>
import { computed, ref, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'
import CartDrawer from './components/CartDrawer.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const searchText = ref('')

const isDashboard = computed(() => route.path.startsWith('/admin') || route.path.startsWith('/employee'))
const isAuthPage = computed(() => ['/login', '/register', '/signup'].includes(route.path))
const showCustomerChrome = computed(() => !isDashboard.value && !isAuthPage.value)
const displayName = computed(() => auth.user?.fullName || auth.user?.name || auth.user?.email || 'Tài khoản')

watch(() => route.query.q, value => { searchText.value = String(value || '') }, { immediate: true })
onMounted(() => { auth.loadMe() })

function search() {
  const query = searchText.value.trim()
  router.push({ path: '/store', query: query ? { q: query } : {} })
}

async function goToCatalog() {
  if (route.path !== '/store') await router.push('/store')
  await nextTick()
  document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function goToAbout() {
  if (route.path !== '/store') await router.push('/store')
  await nextTick()
  document.getElementById('home-about')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function logout() {
  try { await auth.logout() } finally { router.push('/store') }
}
</script>

<template>
  <div class="app-shell" :class="{ 'dashboard-app': isDashboard, 'auth-app': isAuthPage }">
    <template v-if="showCustomerChrome">
      <div class="top-strip">MIỄN PHÍ VẬN CHUYỂN CHO ĐƠN TỪ 1.000.000₫ <span>·</span> ĐỔI SIZE DỄ DÀNG</div>
      <header class="site-header">
        <div class="header-inner site-container">
          <RouterLink to="/store" class="site-logo" aria-label="FREEDOMS - Trang chủ">
            <img src="/img/freedoms-bird.png" alt="" class="site-logo-image" />
            <span><strong>FREEDOM<span class="freedom-red-s">S</span></strong><small>STEP INTO YOUR STYLE</small></span>
          </RouterLink>
          <form class="site-search" role="search" @submit.prevent="search">
            <input v-model="searchText" aria-label="Tìm kiếm sản phẩm" placeholder="Tìm Nike, Adidas, giày chạy bộ..." />
            <button type="submit" aria-label="Tìm kiếm">⌕</button>
          </form>
          <nav class="header-actions" aria-label="Tài khoản và giỏ hàng">
            <template v-if="auth.user">
              <RouterLink to="/orders" class="header-link">Đơn mua</RouterLink>
              <RouterLink to="/account" class="header-link customer-greeting">Xin chào, {{ displayName }}</RouterLink>
              <button class="header-link logout-button" type="button" @click="logout">Thoát</button>
            </template>
            <RouterLink v-else to="/login" class="header-link">Đăng nhập</RouterLink>
            <button type="button" class="cart-link cart-link-button" aria-label="Mở giỏ hàng" @click="cart.openDrawer()">
              <span class="cart-icon" aria-hidden="true">▢</span><span>Giỏ hàng</span><b>{{ cart.count }}</b>
            </button>
          </nav>
        </div>
        <div class="nav-row">
          <nav class="site-container main-nav" aria-label="Điều hướng cửa hàng">
            <RouterLink to="/store">Trang chủ</RouterLink>
            <button type="button" @click="goToCatalog">Sản phẩm</button>
            <button type="button" @click="goToAbout">Giới thiệu</button>
            <RouterLink to="/orders">Theo dõi đơn hàng</RouterLink>
          </nav>
        </div>
      </header>
    </template>

    <main class="app-main" :class="{ 'auth-main': isAuthPage }">
      <RouterView />
    </main>

    <footer v-if="showCustomerChrome" class="site-footer">
      <div class="site-container footer-grid">
        <div class="footer-brand-block">
          <RouterLink to="/store" class="site-logo footer-logo">
            <img src="/img/freedoms-bird.png" alt="" class="site-logo-image" />
            <span><strong>FREEDOM<span class="freedom-red-s">S</span></strong><small>STEP INTO YOUR STYLE</small></span>
          </RouterLink>
          <p>Chọn đôi giày phù hợp, tự do trên từng bước chân.</p>
        </div>
        <div>
          <h3>Hỗ trợ khách hàng</h3>
          <RouterLink to="/cart">Hướng dẫn mua hàng</RouterLink>
          <RouterLink to="/orders">Theo dõi đơn hàng</RouterLink>
          <RouterLink to="/account">Tài khoản khách hàng</RouterLink>
        </div>
        <div>
          <h3>Liên hệ</h3>
          <p>Email: support@freedomshoe.local</p>
          <p>Hotline: 1900 0000</p>
          <p>Thứ 2 - Chủ nhật: 8:00 - 21:00</p>
        </div>
      </div>
      <div class="copyright">© 2026 FREEDOMS · SD_11</div>
    </footer>
    <CartDrawer :open="cart.drawerOpen" @close="cart.closeDrawer()" />
  </div>
</template>

<style>
.app-shell{min-height:100vh;display:flex;flex-direction:column;background:#f8fbff;color:#17324d}
.app-main{flex:1;min-width:0}
.app-main.auth-main{min-height:calc(100vh - 0px);background:linear-gradient(to bottom,#2c3e50 0%,#3498db 42%,#eaf4ff 78%,#fff 100%);display:flex;align-items:stretch;justify-content:center}
.site-container{width:min(1240px,calc(100% - 48px));margin-inline:auto}
.top-strip{background:#eaf4ff;color:#123e67;text-align:center;padding:9px 12px;font-size:10px;letter-spacing:.1em;font-weight:900;border-bottom:1px solid #d8eafa}
.top-strip span{padding:0 5px;color:#7eaacb}
.site-header{background:#fff;position:relative;z-index:20;box-shadow:0 2px 10px rgba(23,50,77,.035)}
.header-inner{min-height:82px;display:flex;align-items:center;gap:30px}
.site-logo{display:flex;align-items:center;gap:10px;color:#17324d;text-decoration:none;white-space:nowrap}
.site-logo-image{width:42px;height:42px;object-fit:contain}
.site-logo strong{display:block;font-size:17px;font-weight:900;letter-spacing:.07em;line-height:1.05}.freedom-red-s{color:#c62828}
.site-logo small{display:block;margin-top:4px;color:#71879a;font-size:8px;font-weight:700;letter-spacing:.16em}
.site-search{height:42px;display:flex;align-items:center;flex:1;max-width:510px;border:1px solid #d8e5ef;border-radius:6px;background:#fff;overflow:hidden}
.site-search:focus-within{border-color:#6aa8d3;box-shadow:0 0 0 3px rgba(47,139,203,.1)}
.site-search input{width:100%;min-width:0;height:100%;padding:0 14px;border:0;outline:0;background:transparent;color:#17324d;font-size:12px}
.site-search input::placeholder{color:#93a4b3}
.site-search button{height:100%;width:46px;border:0;background:#17324d;color:#fff;font-size:23px;cursor:pointer}
.header-actions{display:flex;align-items:center;justify-content:flex-end;gap:16px;margin-left:auto}
.header-link,.cart-link{font-size:11px;color:#243c54;text-decoration:none;white-space:nowrap}
.header-link:hover,.main-nav a:hover,.main-nav button:hover{color:#2f8bcb}
.customer-greeting{max-width:180px;overflow:hidden;text-overflow:ellipsis}
.logout-button{border:0;background:transparent;padding:0;cursor:pointer}
.cart-link{display:flex;align-items:center;gap:7px;border-left:1px solid #e5edf4;padding-left:16px}
.cart-link-button{border:0;background:transparent;cursor:pointer;padding-top:0;padding-bottom:0;font-family:inherit}
.cart-icon{font-size:18px;color:#2f8bcb}
.cart-link b{display:grid;place-items:center;min-width:19px;height:19px;padding:0 4px;border-radius:50%;background:#2f8bcb;color:white;font-size:9px}
.nav-row{border-top:1px solid #f0f4f8;border-bottom:1px solid #e5edf4}
.main-nav{display:flex;align-items:center;gap:30px;min-height:42px}
.main-nav a,.main-nav button{font-size:11px;font-weight:800;color:#425a70;text-decoration:none;border:0;background:transparent;padding:0;cursor:pointer}
.site-footer{margin-top:32px;background:#fff;color:#111;border-top:1px solid #e8e8e8}
.footer-grid{display:grid;grid-template-columns:1.4fr 1fr 1fr;gap:38px;padding:34px 0}
.footer-logo{color:#111}
.footer-logo small{color:#777}
.footer-grid p,.footer-grid a:not(.site-logo){font-size:11px;color:#555;line-height:1.8}
.footer-grid a:not(.site-logo){display:block;margin:4px 0;text-decoration:none}
.footer-grid h3{font-size:12px;margin:5px 0 10px;color:#111}
.footer-grid a:hover{color:#000;text-decoration:underline}
.footer-brand-block>p{max-width:300px}
.copyright{border-top:1px solid #e8e8e8;text-align:center;padding:13px;font-size:9px;color:#777}
@media(max-width:900px){.header-inner{gap:16px;flex-wrap:wrap;padding:14px 0}.site-search{order:3;max-width:none;flex-basis:100%}.header-actions{gap:10px}.main-nav{gap:22px}}
@media(max-width:620px){.site-container{width:calc(100% - 28px)}.top-strip{font-size:8px;letter-spacing:.04em}.header-inner{gap:11px}.site-logo-image{width:34px;height:34px}.site-logo strong{font-size:14px}.header-actions{gap:8px}.header-link{font-size:10px}.customer-greeting{display:none}.cart-link{padding-left:9px}.cart-link>span:nth-child(2){display:none}.main-nav{gap:18px;overflow-x:auto}.main-nav a,.main-nav button{white-space:nowrap}.footer-grid{grid-template-columns:1fr;gap:15px;padding:25px 0}}
</style>
