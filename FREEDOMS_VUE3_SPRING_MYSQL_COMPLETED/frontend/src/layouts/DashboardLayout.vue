<script setup>
import { computed } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isAdmin = computed(() => route.path.startsWith('/admin'))
const pageTitle = computed(() => route.meta.title || (isAdmin.value ? 'Không gian quản trị' : 'Không gian nhân viên'))
const displayName = computed(() => auth.user?.fullName || auth.user?.name || auth.user?.email || 'Tài khoản')

const sections = computed(() => isAdmin.value ? [
  { title: 'KHÔNG GIAN LÀM VIỆC', items: [
    { to: '/admin/overview', icon: '▦', label: 'Tổng quan' }
  ] },
  { title: 'BÁN HÀNG', items: [
    { to: '/admin/pos', icon: '▣', label: 'Bán hàng tại quầy' },
    { to: '/admin/held', icon: '◷', label: 'Hóa đơn chờ' },
    { to: '/admin/orders', icon: '▤', label: 'Đơn hàng & thanh toán' }
  ] },
  { title: 'SẢN PHẨM', items: [
    { to: '/admin/products', icon: '◈', label: 'Quản lý sản phẩm' },
    { to: '/admin/catalog/categories', icon: '▦', label: 'Danh mục' },
    { to: '/admin/catalog/brands', icon: '◇', label: 'Thương hiệu' },
    { to: '/admin/catalog/colors', icon: '●', label: 'Màu sắc' },
    { to: '/admin/catalog/sizes', icon: '↔', label: 'Kích cỡ' }
  ] },
  { title: 'ƯU ĐÃI', items: [
    { to: '/admin/variant-discounts', icon: '％', label: 'Giảm giá biến thể' },
    { to: '/admin/coupons/online', icon: '◇', label: 'Mã giảm giá Online' },
    { to: '/admin/coupons/offline', icon: '◇', label: 'Mã giảm giá Offline' }
  ] },
  { title: 'QUẢN LÝ', items: [
    { to: '/admin/refunds', icon: '↩', label: 'Hoàn tiền' },
    { to: '/admin/customers', icon: '♙', label: 'Khách hàng' },
    { to: '/admin/accounts', icon: '♙', label: 'Tài khoản & nhân viên' }
  ] }
] : [])

async function logout() {
  try { await auth.logout() } finally { router.push('/login') }
}
</script>

<template>
  <div class="dashboard-shell" :class="{ 'admin-shell': isAdmin, 'employee-shell': !isAdmin }">
    <div class="dashboard-body">
      <aside v-if="isAdmin" class="sidebar">
        <RouterLink :to="isAdmin ? '/admin/overview' : '/employee/pos'" class="sidebar-brand">
          <img src="/img/freedoms-bird.png" alt="" class="sidebar-brand-bird" />
          <span><strong>FREEDOM<span class="freedom-red-s">S</span></strong><small>QUẢN LÝ CỬA HÀNG</small></span>
        </RouterLink>

        <div class="side-caption">HỆ THỐNG QUẢN LÝ</div>
        <nav class="sidebar-navigation" aria-label="Điều hướng quản trị">
          <section v-for="section in sections" :key="section.title" class="side-section">
            <h2 class="side-section-title">{{ section.title }}</h2>
            <RouterLink v-for="item in section.items" :key="item.to" :to="item.to" class="side-link" active-class="active">
              <span class="side-icon" aria-hidden="true">{{ item.icon }}</span>
              <span class="side-link-label">{{ item.label }}</span>
            </RouterLink>
          </section>
        </nav>
        <div class="sidebar-foot"><span class="online-dot"></span> Hệ thống đang hoạt động</div>
      </aside>

      <div class="dashboard-workspace">
        <header class="dashboard-topbar">
          <div class="dashboard-page-title">
            <span>{{ isAdmin ? 'QUẢN LÝ' : 'NHÂN VIÊN' }} <span class="breadcrumb-separator">›</span> FREEDOMS</span>
            <h1>{{ pageTitle }}</h1>
            <small>{{ isAdmin ? 'Không gian quản trị cửa hàng' : 'Không gian bán hàng tại quầy và xử lý đơn' }}</small>
          </div>
          <div class="dashboard-topbar-actions">
            <RouterLink v-if="isAdmin" to="/store" class="topbar-store-link"><span aria-hidden="true">⌂</span> Trang bán hàng</RouterLink>
            <RouterLink class="dashboard-user-block" :to="isAdmin ? '/admin/accounts' : '/employee/profile'" :title="isAdmin ? 'Quản lý tài khoản' : 'Cài đặt hồ sơ của tôi'"><span class="dashboard-user-avatar">{{displayName.slice(0,1).toUpperCase()}}</span><span><b>{{ displayName }}</b><small>{{ auth.user?.email }} · {{isAdmin?'Tài khoản':'Hồ sơ của tôi'}}</small></span></RouterLink>
            <button class="dashboard-logout" type="button" @click="logout">Đăng xuất</button>
          </div>
        </header>
        <nav v-if="!isAdmin" class="employee-horizontal-nav" aria-label="Chức năng nhân viên">
          <RouterLink to="/employee/overview" active-class="active"><span>▦</span>Tổng quan</RouterLink>
          <RouterLink to="/employee/pos" active-class="active"><span>▣</span>POS bán hàng</RouterLink>
          <RouterLink to="/employee/held" active-class="active"><span>◷</span>Hóa đơn chờ</RouterLink>
          <RouterLink to="/employee/orders" active-class="active"><span>▤</span>Đơn hàng & thanh toán</RouterLink>
          <RouterLink to="/employee/customers" active-class="active"><span>♙</span>Khách hàng</RouterLink>
          <RouterLink to="/employee/refunds" active-class="active"><span>↩</span>Hoàn tiền</RouterLink>
        </nav>
        <main class="dashboard-main"><RouterView /></main>
        <footer class="dashboard-footer"><strong>FREEDOM<span class="freedom-red-s">S</span></strong><span>Không gian quản lý cửa hàng</span></footer>
      </div>
    </div>
  </div>
</template>
