import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import DepartmentsPage from '@/pages/DepartmentsPage.vue'
import EmployeesPage from '@/pages/EmployeesPage.vue'
import UsersPage from '@/pages/UsersPage.vue'
import ProductsPage from '@/pages/ProductsPage.vue'
import InventoryPage from '@/pages/InventoryPage.vue'
import WarehousesPage from '@/pages/WarehousesPage.vue'
import PurchasesPage from '@/pages/PurchasesPage.vue'
import PosPage from '@/pages/PosPage.vue'
import OrdersPage from '@/pages/OrdersPage.vue'
import ReturnsPage from '@/pages/ReturnsPage.vue'
import DocumentsPage from '@/pages/DocumentsPage.vue'
import SecurityCenterPage from '@/pages/SecurityCenterPage.vue'
import StockOperationsPage from '@/pages/StockOperationsPage.vue'
import CustomersPage from '@/pages/CustomersPage.vue'
import SuppliersPage from '@/pages/SuppliersPage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import NotFoundPage from '@/pages/NotFoundPage.vue'
import ForbiddenPage from '@/pages/ForbiddenPage.vue'
import SessionsPage from '@/pages/SessionsPage.vue'
import ItLayout from '@/layouts/ItLayout.vue'
import ItOverviewPage from '@/pages/ItOverviewPage.vue'
import ItCapabilityPage from '@/pages/ItCapabilityPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', name: 'dashboard', component: DashboardPage },
        { path: 'reports', name: 'reports', component: DashboardPage, meta: { permissions: ['report.view'] } },
        { path: 'departments', name: 'departments', component: DepartmentsPage, meta: { permissions: ['department.view'] } },
        { path: 'employees', name: 'employees', component: EmployeesPage, meta: { permissions: ['employee.view'] } },
        { path: 'settings/users', name: 'users', component: UsersPage, meta: { permissions: ['user.view'] } },
        { path: 'settings/sessions', name: 'sessions', component: SessionsPage },
        { path: 'products', name: 'products', component: ProductsPage, meta: { permissions: ['product.view'] } },
        { path: 'customers', name: 'customers', component: CustomersPage, meta: { permissions: ['customer.view'] } },
        { path: 'suppliers', name: 'suppliers', component: SuppliersPage, meta: { permissions: ['supplier.view'] } },
        { path: 'inventory', name: 'inventory', component: InventoryPage, meta: { permissions: ['inventory.view'] } },
        { path: 'warehouses', name: 'warehouses', component: WarehousesPage, meta: { permissions: ['inventory.view'] } },
        { path: 'purchasing', name: 'purchasing', component: PurchasesPage, meta: { permissions: ['purchase.view'] } },
        { path: 'pos', name: 'pos', component: PosPage, meta: { permissions: ['order.create'] } },
        { path: 'orders', name: 'orders', component: OrdersPage, meta: { permissions: ['order.view'] } },
        { path: 'returns', name: 'returns', component: ReturnsPage, meta: { permissions: ['return.create', 'return.approve'], permissionMode: 'any' } },
        { path: 'documents', name: 'documents', component: DocumentsPage, meta: { permissions: ['document.view'] } },
        { path: 'security', name: 'security', component: SecurityCenterPage, meta: { permissions: ['security.view'] } },
        { path: 'stock-operations', name: 'stock-operations', component: StockOperationsPage, meta: { permissions: ['inventory.transfer', 'inventory.stocktake'], permissionMode: 'any' } },
      ],
    },
    { path:'/it',component:ItLayout,children:[
      {path:'',redirect:'/it/overview'},
      {path:'overview',name:'it-overview',component:ItOverviewPage,meta:{permissions:['system.health.view']}},
      {path:'users',name:'it-users',component:UsersPage,meta:{permissions:['user.view']}},
      {path:'sessions',name:'it-sessions',component:ItCapabilityPage,props:{title:'Quản lý phiên đăng nhập',description:'Chọn một tài khoản trong trang Tài khoản để xem số phiên và thu hồi toàn bộ phiên.'},meta:{permissions:['session.view']}},
      {path:'security',name:'it-security',component:SecurityCenterPage,meta:{permissions:['security.view']}},
      {path:'alerts',redirect:'/it/security'},
      {path:'ai-models',name:'it-models',component:ItCapabilityPage,props:{title:'Mô hình AI',description:'Metadata model hiện tại được hiển thị ở Tổng quan. Workflow train/deploy/rollback chưa được cấu hình.'},meta:{permissions:['ai.model.view']}},
      {path:'ai-policies',name:'it-policies',component:ItCapabilityPage,props:{title:'Chính sách AI',description:'Quyền kiến trúc đã sẵn sàng; chưa có endpoint thay đổi chính sách an toàn.'},meta:{permissions:['ai.policy.view']}},
      {path:'integrations',name:'it-integrations',component:ItCapabilityPage,props:{title:'Tích hợp',description:'Chưa có tích hợp bên thứ ba được xác minh.'},meta:{permissions:['integration.view']}},
      {path:'system-health',name:'it-health',component:ItOverviewPage,meta:{permissions:['system.health.view']}},
      {path:'audit-logs',name:'it-audit',component:ItCapabilityPage,props:{title:'Nhật ký kiểm toán',description:'Dữ liệu audit đang được ghi nhận; màn hình truy vấn chuyên biệt chưa có endpoint phân trang.'},meta:{permissions:['audit.view']}},
    ]},
    { path: '/forbidden', name: 'forbidden', component: ForbiddenPage, meta: { public: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage, meta: { public: true } },
  ],
})

export default router
