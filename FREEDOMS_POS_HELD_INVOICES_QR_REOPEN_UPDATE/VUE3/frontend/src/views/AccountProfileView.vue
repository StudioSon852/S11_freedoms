<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { useAuthStore } from '../stores/auth'
const route = useRoute(), router = useRouter(), auth = useAuthStore()
const employee = computed(() => route.path.startsWith('/employee'))
const validTabs = ['profile', 'orders', 'coupons']
const activeTab = ref(validTabs.includes(String(route.query.tab || '')) ? String(route.query.tab) : 'profile')
const profile = ref({ fullName: '', phone: '', defaultAddress: '', email: '', tier: 'Mới', totalSpent: 0, tierDiscountPercent: 0, role: '' })
const coupons = ref([]), error = ref(''), success = ref(''), busy = ref(false)
function setTab(tab) {
  if (!validTabs.includes(tab) || (employee.value && tab !== 'profile')) return
  activeTab.value = tab
  if (!employee.value) router.replace({ path: '/account', query: tab === 'profile' ? {} : { tab } })
}
watch(() => route.query.tab, value => {
  if (validTabs.includes(String(value || ''))) activeTab.value = String(value)
  else activeTab.value = 'profile'
})
async function load() {
  error.value = ''
  try {
    profile.value = (await api.get('/api/account/me')).data
    if (!employee.value) coupons.value = (await api.get('/api/store/coupons', { params: { channel: 'ONLINE' } })).data
  } catch (e) { error.value = errorMessage(e) }
}
async function save() {
  busy.value = true; error.value = ''; success.value = ''
  try {
    await api.patch('/api/account/me', { fullName: profile.value.fullName, phone: profile.value.phone, defaultAddress: profile.value.defaultAddress })
    success.value = 'Đã lưu thông tin tài khoản.'
    await auth.loadMe(); await load()
  } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
const money = v => `${Number(v || 0).toLocaleString('vi-VN')}₫`
onMounted(load)
</script>
<template>
  <div class="simple-page ocean-backdrop account-page">
    <main class="narrow-main">
      <div class="page-heading"><span class="eyebrow">{{ employee ? 'FREEDOMS / EMPLOYEE PROFILE' : 'MY FREEDOMS ACCOUNT' }}</span><h1>{{ employee ? 'Thông tin chủ tài khoản nhân viên' : 'Tài khoản của tôi' }}</h1><p>{{ employee ? 'Quản lý thông tin liên hệ của tài khoản nhân viên.' : 'Quản lý hồ sơ, xem đơn mua và sử dụng các ưu đãi dành cho thành viên FREEDOMS.' }}</p></div>
      <p v-if="error" class="notice notice-error">{{ error }}</p><p v-if="success" class="notice notice-success">{{ success }}</p>

      <div v-if="!employee" class="customer-account-layout">
        <aside class="surface-card customer-account-nav">
          <div class="customer-account-identity"><div class="profile-avatar">{{ (profile.fullName || 'F').slice(0, 1).toUpperCase() }}</div><b>{{ profile.fullName || 'Khách hàng FREEDOMS' }}</b><small>{{ profile.email }}</small></div>
          <nav aria-label="Các mục tài khoản">
            <button :class="{ active: activeTab === 'profile' }" @click="setTab('profile')">◉ <span>Thông tin cá nhân</span></button>
            <button :class="{ active: activeTab === 'orders' }" @click="setTab('orders')">▤ <span>Đơn hàng của tôi</span></button>
            <button :class="{ active: activeTab === 'coupons' }" @click="setTab('coupons')">◇ <span>Mã giảm giá</span></button>
          </nav>
        </aside>

        <div class="customer-account-content">
          <section v-if="activeTab === 'profile'" class="surface-card account-profile-card">
            <div class="profile-avatar">{{ (profile.fullName || 'F').slice(0, 1).toUpperCase() }}</div><div class="profile-readonly"><span class="eyebrow">THÔNG TIN ĐĂNG NHẬP</span><b>{{ profile.email }}</b><small>Vai trò: {{ profile.role }}</small></div>
            <form class="profile-form" @submit.prevent="save"><label>Họ và tên<input v-model.trim="profile.fullName" required maxlength="150" /></label><label>Số điện thoại<input v-model.trim="profile.phone" maxlength="30" /></label><label>Địa chỉ mặc định<textarea v-model.trim="profile.defaultAddress" maxlength="500" rows="3" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố" /></label><label>Email đăng nhập<input :value="profile.email" disabled /></label><button class="btn btn-primary" :disabled="busy">{{ busy ? 'Đang lưu…' : 'Lưu thay đổi' }}</button></form>
            <div class="member-profile-card"><div><span class="eyebrow">MEMBERSHIP</span><h2>Hạng thành viên {{ profile.tier }}</h2><p>Tổng chi tiêu tích lũy: <strong>{{ money(profile.totalSpent) }}</strong></p><p>Mức ưu đãi thành viên: <strong>{{ profile.tierDiscountPercent }}%</strong></p></div></div>
          </section>

          <section v-else-if="activeTab === 'orders'" class="surface-card account-panel-placeholder"><span class="eyebrow">ORDER HISTORY</span><h2>Đơn hàng của tôi</h2><p>Theo dõi trạng thái, phương thức thanh toán, thời hạn yêu cầu hoàn tiền và lịch sử mua hàng.</p><RouterLink to="/orders" class="btn btn-primary">Mở danh sách đơn mua</RouterLink></section>

          <section v-else class="surface-card"><div class="section-toolbar"><div><span class="eyebrow">ONLINE COUPONS</span><h2>Mã giảm giá dành cho tài khoản</h2><p class="muted small-text">Bao gồm mã kênh ONLINE và BOTH; chỉ hiển thị mã đang hoạt động, còn hạn và còn lượt sử dụng.</p></div></div>
            <div v-for="c in coupons" :key="c.id" class="account-coupon"><div><b>{{ c.code }}</b><small>{{ c.salesChannel === 'BOTH' ? 'Online + tại quầy' : 'Online' }} · Đơn tối thiểu {{ money(c.minOrderAmount) }}</small><small>Hạn: {{ c.endsAt ? new Date(c.endsAt).toLocaleString('vi-VN') : 'Không giới hạn' }}</small></div><strong>{{ c.discountType === 'PERCENT' ? `${c.discountValue}%` : `-${money(c.discountValue)}` }}</strong></div>
            <div v-if="!coupons.length" class="empty-state">Hiện chưa có mã giảm giá Online hoặc BOTH phù hợp.</div>
          </section>
        </div>
      </div>

      <section v-else class="surface-card account-profile-card employee-profile-panel">
        <div class="profile-avatar">{{ (profile.fullName || 'F').slice(0, 1).toUpperCase() }}</div><div class="profile-readonly"><span class="eyebrow">EMPLOYEE ACCOUNT</span><b>{{ profile.email }}</b><small>Vai trò: {{ profile.role }}</small></div>
        <form class="profile-form" @submit.prevent="save"><label>Họ và tên<input v-model.trim="profile.fullName" required maxlength="150" /></label><label>Số điện thoại<input v-model.trim="profile.phone" maxlength="30" /></label><label>Email đăng nhập<input :value="profile.email" disabled /></label><button class="btn btn-primary" :disabled="busy">{{ busy ? 'Đang lưu…' : 'Lưu cài đặt tài khoản' }}</button></form>
      </section>
    </main>
  </div>
</template>
