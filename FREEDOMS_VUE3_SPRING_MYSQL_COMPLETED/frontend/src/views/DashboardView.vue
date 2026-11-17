<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, errorMessage } from '../api'

const stats = ref(null)
const revenue = ref({ totalRevenue: 0, orderCount: 0, orders: [], truncated: false })
const error = ref('')
const loadingRevenue = ref(false)
const filters = reactive({ fromDate: '', toDate: '', customer: '', orderCode: '' })
const money = value => `${Number(value || 0).toLocaleString('vi-VN')}₫`
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '—'

async function loadOverview() {
  try { stats.value = (await api.get('/api/admin/overview')).data }
  catch (e) { error.value = errorMessage(e) }
}
async function loadRevenue() {
  loadingRevenue.value = true; error.value = ''
  try {
    revenue.value = (await api.get('/api/admin/revenue', { params: { ...filters } })).data
  } catch (e) { error.value = errorMessage(e) }
  finally { loadingRevenue.value = false }
}
function resetFilters() { Object.assign(filters, { fromDate: '', toDate: '', customer: '', orderCode: '' }); loadRevenue() }
onMounted(async () => { await Promise.all([loadOverview(), loadRevenue()]) })
</script>

<template>
  <div class="view-wrap">
    <div class="view-heading"><div><span class="eyebrow">FREEDOMS / ADMIN</span><h1>Tổng quan hoạt động</h1><p>Doanh thu, đơn hàng gần đây, vận hành kho và thanh toán trên toàn hệ thống.</p></div><button class="btn btn-outline" @click="loadOverview(); loadRevenue()">Làm mới</button></div>
    <p v-if="error" class="notice notice-error">{{ error }}</p>
    <div v-if="stats" class="stats-grid">
      <div class="stat-card"><small>Sản phẩm</small><strong>{{ stats.products }}</strong><span>Đang quản lý trong kho</span></div>
      <div class="stat-card"><small>Đơn hàng</small><strong>{{ stats.orders }}</strong><span>Online và tại quầy</span></div>
      <div class="stat-card"><small>Hóa đơn chờ</small><strong>{{ stats.heldInvoices }}</strong><span>Chưa trừ kho</span></div>
      <div class="stat-card"><small>Chờ xác nhận thanh toán</small><strong>{{ stats.pendingPayments }}</strong><span>Chuyển khoản cần đối soát</span></div>
      <div class="stat-card"><small>Chờ hoàn tiền</small><strong>{{ stats.pendingRefunds }}</strong><span>Cần thao tác hoàn trả</span></div>
      <div class="stat-card stat-highlight"><small>Doanh thu hoàn thành</small><strong>{{ money(stats.revenue) }}</strong><span>Đơn COMPLETED và PAID</span></div>
    </div>

    <section class="surface-card dashboard-revenue-panel">
      <div class="section-toolbar"><div><span class="eyebrow">REVENUE REPORT</span><h2>Bộ lọc doanh thu</h2><p class="muted small-text">Chỉ tính đơn đã hoàn thành và thanh toán thành công; ngày lọc theo ngày tạo đơn; tổng doanh thu đã trừ các khoản hoàn tiền thực tế được xác nhận.</p></div><button class="btn btn-outline btn-sm" @click="resetFilters">Xóa bộ lọc</button></div>
      <div class="revenue-filter-grid">
        <label>Từ ngày<input v-model="filters.fromDate" type="date"></label>
        <label>Đến ngày<input v-model="filters.toDate" type="date"></label>
        <label>Khách hàng<input v-model.trim="filters.customer" placeholder="Tên, số điện thoại hoặc email"></label>
        <label>Mã đơn<input v-model.trim="filters.orderCode" placeholder="Nhập mã đơn hàng"></label>
        <button class="btn btn-primary" :disabled="loadingRevenue" @click="loadRevenue">{{ loadingRevenue ? 'Đang lọc…' : 'Lọc doanh thu' }}</button>
      </div>
      <div class="revenue-result-strip"><div><small>Tổng doanh thu phù hợp</small><strong>{{ money(revenue.totalRevenue) }}</strong></div><div><small>Số đơn đã thanh toán</small><strong>{{ revenue.orderCount }}</strong></div></div>
      <div class="table-scroll"><table class="data-table"><thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Ngày tạo đơn</th><th>Kênh</th><th>Trạng thái</th><th class="text-right">Tổng tiền</th></tr></thead><tbody>
        <tr v-for="o in revenue.orders" :key="o.id"><td><b>{{ o.code }}</b></td><td>{{ o.customerName }}<small>{{ o.customerPhone }}</small></td><td>{{ dateTime(o.createdAt) }}</td><td>{{ o.salesChannel }}</td><td><span class="status-pill status-completed">COMPLETED / PAID</span></td><td class="text-right money-red">{{ money(o.totalAmount) }}</td></tr>
        <tr v-if="!revenue.orders.length"><td colspan="6" class="empty-cell">Không có đơn hoàn thành phù hợp với bộ lọc.</td></tr>
      </tbody></table></div>
      <p v-if="revenue.truncated" class="muted small-text">Đang hiển thị 100 đơn đầu tiên; tổng doanh thu và số lượng đã tính trên toàn bộ kết quả.</p>
    </section>

    <section class="surface-card dashboard-recent-orders">
      <div class="section-toolbar"><div><span class="eyebrow">LATEST ORDERS</span><h2>Đơn hàng gần đây</h2><p class="muted small-text">Danh sách mới nhất trên toàn hệ thống, tương tự Tổng quan nhân viên.</p></div><RouterLink to="/admin/orders" class="btn btn-outline btn-sm">Quản lý đơn hàng</RouterLink></div>
      <div v-for="o in stats?.recentOrders || []" :key="o.id" class="employee-recent-row"><div><b>{{ o.code }}</b><small>{{ o.customerName }} · {{ o.salesChannel }} · {{ dateTime(o.createdAt) }}</small></div><strong>{{ money(o.totalAmount) }}</strong><span class="status-pill" :class="'status-'+String(o.status).toLowerCase()">{{ o.status }}</span></div>
      <p v-if="!stats?.recentOrders?.length" class="empty-state">Chưa có đơn hàng.</p>
    </section>

    <div class="dashboard-quick-grid">
      <RouterLink to="/admin/pos" class="quick-card"><span>▣</span><b>Mở bán hàng tại quầy</b><small>Tạo nhiều phiên khách, tìm kiếm hàng và chốt thanh toán.</small></RouterLink>
      <RouterLink to="/admin/products" class="quick-card"><span>◈</span><b>Quản lý sản phẩm</b><small>Giá bán, size/màu, tồn kho và ngừng kinh doanh.</small></RouterLink>
      <RouterLink to="/admin/customers" class="quick-card"><span>♙</span><b>Khách hàng</b><small>Theo dõi hạng thành viên, thông tin liên hệ và đơn mua.</small></RouterLink>
    </div>
  </div>
</template>
