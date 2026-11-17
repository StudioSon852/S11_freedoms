import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import CartView from './views/CartView.vue'
import ProductDetailView from './views/ProductDetailView.vue'
import CheckoutView from './views/CheckoutView.vue'
import MyOrdersView from './views/MyOrdersView.vue'
import PaymentView from './views/PaymentView.vue'
import DashboardLayout from './layouts/DashboardLayout.vue'
import DashboardView from './views/DashboardView.vue'
import PosView from './views/PosView.vue'
import HeldInvoicesView from './views/HeldInvoicesView.vue'
import EmployeeOrdersView from './views/EmployeeOrdersView.vue'
import ProductsView from './views/ProductsView.vue'
import CouponsView from './views/CouponsView.vue'
import VariantDiscountsView from './views/VariantDiscountsView.vue'
import OrdersView from './views/OrdersView.vue'
import RefundsView from './views/RefundsView.vue'
import AccountsView from './views/AccountsView.vue'
import CatalogManagementView from './views/CatalogManagementView.vue'
import AccountProfileView from './views/AccountProfileView.vue'
import EmployeeOverviewView from './views/EmployeeOverviewView.vue'
import PrintDocumentView from './views/PrintDocumentView.vue'
import CustomersView from './views/CustomersView.vue'

const routes = [
  { path: '/', redirect: '/store' },
  { path: '/store', component: HomeView, meta: { title: 'Trang chủ', denyRoles: ['EMPLOYEE'] } },
  { path: '/login', component: LoginView, meta: { title: 'Đăng nhập' } },
  { path: '/register', component: LoginView, meta: { title: 'Đăng ký khách hàng' } },
  { path: '/signup', redirect: '/register' },
  { path: '/cart', component: CartView, meta: { title: 'Giỏ hàng', denyRoles: ['EMPLOYEE'] } },
  { path: '/product/:id', component: ProductDetailView, meta: { title: 'Chi tiết sản phẩm', denyRoles: ['EMPLOYEE'] } },
  { path: '/checkout', component: CheckoutView, meta: { requiresAuth: true, roles: ['CUSTOMER'], title: 'Thanh toán', denyRoles: ['EMPLOYEE'] } },
  { path: '/orders', component: MyOrdersView, meta: { requiresAuth: true, roles: ['CUSTOMER'], title: 'Đơn mua', denyRoles: ['EMPLOYEE'] } },
  { path: '/account', component: AccountProfileView, meta: { requiresAuth: true, roles: ['CUSTOMER'], title: 'Tài khoản của tôi', denyRoles: ['EMPLOYEE'] } },
  { path: '/payment/:id', component: PaymentView, meta: { requiresAuth: true, roles: ['CUSTOMER'], title: 'Thanh toán đơn hàng', denyRoles: ['EMPLOYEE'] } },
  {
    path: '/admin', component: DashboardLayout,
    meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'], shellRole: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/overview' },
      { path: 'overview', component: DashboardView, meta: { title: 'Tổng quan' } },
      { path: 'pos', component: PosView, meta: { title: 'Bán hàng tại quầy' } },
      { path: 'held', component: HeldInvoicesView, meta: { title: 'Hóa đơn chờ' } },
      { path: 'orders', component: OrdersView, meta: { title: 'Đơn hàng & thanh toán' } },
      { path: 'products', component: ProductsView, meta: { title: 'Quản lý sản phẩm' } },
      { path: 'catalog/:type', component: CatalogManagementView, meta: { title: 'Danh mục sản phẩm' } },
      { path: 'orders/:id/print', component: PrintDocumentView, meta: { title: 'In hóa đơn' } },
      { path: 'refunds/:id/print', component: PrintDocumentView, meta: { title: 'In phiếu hoàn tiền' } },
      { path: 'variant-discounts', component: VariantDiscountsView, meta: { title: 'Giảm giá biến thể' } },
      { path: 'coupons/online', component: CouponsView, meta: { title: 'Mã giảm giá Online', channel: 'ONLINE' } },
      { path: 'coupons/offline', component: CouponsView, meta: { title: 'Mã giảm giá Offline', channel: 'OFFLINE' } },
      { path: 'refunds', component: RefundsView, meta: { title: 'Hoàn tiền' } },
      { path: 'customers', component: CustomersView, meta: { title: 'Khách hàng' } },
      { path: 'accounts', component: AccountsView, meta: { title: 'Tài khoản & nhân viên' } }
    ]
  },
  {
    path: '/employee', component: DashboardLayout,
    meta: { requiresAuth: true, roles: ['EMPLOYEE'], shellRole: 'EMPLOYEE' },
    children: [
      { path: '', redirect: '/employee/overview' },
      { path: 'overview', component: EmployeeOverviewView, meta: { title: 'Tổng quan nhân viên' } },
      { path: 'pos', component: PosView, meta: { title: 'Bán hàng tại quầy' } },
      { path: 'held', component: HeldInvoicesView, meta: { title: 'Hóa đơn chờ' } },
      { path: 'orders', component: EmployeeOrdersView, meta: { title: 'Đơn hàng & thanh toán' } },
      { path: 'customers', component: CustomersView, meta: { title: 'Khách hàng' } },
      { path: 'refunds', component: RefundsView, meta: { title: 'Yêu cầu hoàn tiền' } },
      { path: 'profile', component: AccountProfileView, meta: { title: 'Thông tin tài khoản' } },
      { path: 'orders/:id/print', component: PrintDocumentView, meta: { title: 'In hóa đơn' } },
      { path: 'refunds/:id/print', component: PrintDocumentView, meta: { title: 'In phiếu hoàn tiền' } }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
router.beforeEach(async to => {
  const auth = useAuthStore()
  // Read the server-side role even on public storefront routes so employees cannot
  // open online shopping pages after logging into the staff workspace.
  if (to.meta.requiresAuth || to.meta.denyRoles?.length) await auth.loadMe()
  if (to.meta.denyRoles?.includes(auth.user?.role)) {
    return auth.user.role === 'EMPLOYEE' ? '/employee/pos' : '/admin/overview'
  }
  if (to.meta.requiresAuth) {
    if (!auth.user) return { path: '/login', query: { redirect: to.fullPath } }
    if (to.meta.roles && !to.meta.roles.includes(auth.user.role)) {
      return auth.user.role === 'EMPLOYEE' ? '/employee/overview' : auth.user.role === 'CUSTOMER' ? '/store' : '/admin/overview'
    }
  }
  if (to.path === '/login' || to.path === '/register') {
    await auth.loadMe()
    if (auth.user) return auth.user.role === 'EMPLOYEE' ? '/employee/overview' : auth.user.role === 'CUSTOMER' ? '/store' : '/admin/overview'
  }
  return true
})

export default router
