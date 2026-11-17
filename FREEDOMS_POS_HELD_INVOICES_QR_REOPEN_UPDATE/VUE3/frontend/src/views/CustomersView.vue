<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, errorMessage } from '../api'

const route = useRoute()
const isEmployee = computed(() => route.path.startsWith('/employee'))
const base = computed(() => isEmployee.value ? '/api/pos/customers' : '/api/admin/customers')
const rows = ref([]), selected = ref(null), loading = ref(false), error = ref('')
const filters = reactive({ q: '', tier: '' })
const money = value => `${Number(value || 0).toLocaleString('vi-VN')}₫`
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '—'

async function load() {
  loading.value = true; error.value = ''
  try { rows.value = (await api.get(base.value, { params: filters })).data
    if (selected.value) { const found = rows.value.find(x => x.id === selected.value.customer?.id || x.id === selected.value.id); if (!found) selected.value = null }
  } catch (e) { error.value = errorMessage(e) }
  finally { loading.value = false }
}
async function openCustomer(customer) {
  error.value = ''
  try { selected.value = (await api.get(`${base.value}/${customer.id}`)).data }
  catch (e) { error.value = errorMessage(e) }
}
function clearFilters() { filters.q = ''; filters.tier = ''; load() }
onMounted(load)
</script>

<template>
  <div class="view-wrap customer-directory-view">
    <div class="view-heading"><div><span class="eyebrow">CUSTOMER DIRECTORY</span><h1>Quản lý khách hàng</h1><p>Theo dõi tài khoản, hạng thành viên, chi tiêu tích lũy, địa chỉ mặc định và lịch sử mua hàng.</p></div><button class="btn btn-outline" @click="load">Làm mới</button></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p>
    <section class="surface-card">
      <div class="customer-filter-grid"><label>Tìm khách hàng<input v-model.trim="filters.q" placeholder="Tên, email, số điện thoại hoặc địa chỉ" @keyup.enter="load"></label><label>Hạng thành viên<select v-model="filters.tier"><option value="">Tất cả hạng</option><option value="NEW">Mới</option><option value="SILVER">Bạc</option><option value="GOLD">Vàng</option><option value="PLATINUM">Bạch kim</option><option value="DIAMOND">Kim cương</option></select></label><button class="btn btn-primary" :disabled="loading" @click="load">{{ loading ? 'Đang tìm…' : 'Tìm kiếm' }}</button><button class="btn btn-outline" @click="clearFilters">Đặt lại</button></div>
      <div class="table-scroll"><table class="data-table"><thead><tr><th>Khách hàng</th><th>Liên hệ</th><th>Hạng</th><th>Chi tiêu tích lũy</th><th>Ưu đãi/đơn</th><th></th></tr></thead><tbody>
        <tr v-for="c in rows" :key="c.id" :class="{ 'selected-customer-row': selected?.customer?.id === c.id }"><td><b>{{ c.fullName }}</b><small>{{ c.email }}</small></td><td>{{ c.phone || 'Chưa có' }}</td><td><span class="tier-pill" :class="'tier-'+c.tierCode.toLowerCase()">{{ c.tierName }}</span></td><td class="money-red">{{ money(c.totalSpent) }}</td><td><b class="tier-discount-text">{{ Number(c.discountPercent || 0) }}%</b></td><td><button class="btn btn-outline btn-sm" @click="openCustomer(c)">Chi tiết</button></td></tr>
        <tr v-if="!rows.length"><td colspan="6" class="empty-cell">Không tìm thấy tài khoản khách hàng.</td></tr>
      </tbody></table></div>
      <p class="muted small-text">Tìm thấy {{ rows.length }} tài khoản khách hàng.</p>
    </section>

    <section v-if="selected" class="surface-card customer-detail-panel">
      <div class="section-toolbar"><div><span class="eyebrow">CUSTOMER PROFILE #{{ selected.customer.id }}</span><h2>Thông tin chi tiết khách hàng</h2></div><button class="btn btn-outline btn-sm" @click="selected=null">Đóng chi tiết</button></div>
      <div class="customer-detail-grid">
        <div><small>Họ và tên</small><b>{{ selected.customer.fullName }}</b></div>
        <div><small>Email</small><b>{{ selected.customer.email }}</b></div>
        <div><small>Số điện thoại</small><b>{{ selected.customer.phone || 'Chưa cập nhật' }}</b></div>
        <div><small>Ngày tạo tài khoản</small><b>{{ dateTime(selected.customer.createdAt) }}</b></div>
        <div><small>Hạng thành viên</small><b><span class="tier-pill" :class="'tier-'+selected.customer.tierCode.toLowerCase()">{{ selected.customer.tierName }}</span></b></div>
        <div><small>Ưu đãi mỗi đơn</small><b class="tier-discount-text">{{ Number(selected.customer.discountPercent || 0) }}%</b></div>
        <div><small>Tổng chi tiêu tích lũy</small><b class="money-red">{{ money(selected.customer.totalSpent) }}</b></div>
        <div class="customer-address-field"><small>Địa chỉ mặc định</small><b>{{ selected.customer.defaultAddress || 'Khách hàng chưa thiết lập địa chỉ mặc định.' }}</b></div>
      </div>
      <div class="customer-order-history"><div class="section-toolbar"><div><h3>Lịch sử đơn hàng</h3><p class="muted small-text">Tổng cộng {{ selected.orderCount }} đơn hàng; hiển thị tối đa 10 đơn gần nhất.</p></div></div>
        <div v-for="o in selected.recentOrders" :key="o.id" class="employee-recent-row"><div><b>{{ o.code }}</b><small>{{ o.salesChannel }} · {{ dateTime(o.createdAt) }} · {{ o.paymentStatus || 'Chưa thanh toán' }}</small></div><strong>{{ money(o.totalAmount) }}</strong><span class="status-pill" :class="'status-'+String(o.status).toLowerCase()">{{ o.status }}</span></div>
        <p v-if="!selected.recentOrders?.length" class="empty-state">Khách hàng chưa có đơn hàng.</p>
      </div>
    </section>
  </div>
</template>
