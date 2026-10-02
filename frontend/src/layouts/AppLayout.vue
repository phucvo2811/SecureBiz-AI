<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getNotifications, readNotification, type NotificationItem } from '@/services/business'
import { formatDateTime } from '@/utils/format'
import { canAccessItConsole } from '@/utils/rbac'

interface MenuItem { label: string; to: string; permission?: string }
interface MenuGroup { label: string; icon: string; items: MenuItem[] }
const auth = useAuthStore(), route = useRoute(), router = useRouter()
const drawerOpen = ref(false), collapsed = ref(false), openGroups = ref(['Bán hàng', 'Hàng hóa'])
const notifications = ref<NotificationItem[]>([]), showNotifications = ref(false), showAccount = ref(false)
const unread = computed(() => notifications.value.filter((item) => !item.readAt).length)
const groups: MenuGroup[] = [
  { label: '', icon: '⌂', items: [{ label: 'Tổng quan', to: '/' }] },
  { label: 'Bán hàng', icon: '▣', items: [{ label: 'Màn hình bán hàng', to: '/pos', permission: 'order.create' }, { label: 'Hóa đơn', to: '/orders', permission: 'order.view' }, { label: 'Trả hàng', to: '/returns', permission: 'return.create|return.approve' }] },
  { label: 'Hàng hóa', icon: '◇', items: [{ label: 'Sản phẩm', to: '/products', permission: 'product.view' }, { label: 'Tồn kho', to: '/inventory', permission: 'inventory.view' }, { label: 'Kho hàng', to: '/warehouses', permission: 'inventory.view' }, { label: 'Chuyển kho và kiểm kho', to: '/stock-operations', permission: 'inventory.transfer|inventory.stocktake' }] },
  { label: 'Nhập hàng', icon: '↓', items: [{ label: 'Phiếu nhập hàng', to: '/purchasing', permission: 'purchase.view' }, { label: 'Nhà cung cấp', to: '/suppliers', permission: 'supplier.view' }] },
  { label: 'Đối tác', icon: '♙', items: [{ label: 'Khách hàng', to: '/customers', permission: 'customer.view' }] },
  { label: '', icon: '▥', items: [{ label: 'Báo cáo', to: '/reports', permission: 'report.view' }] },
  { label: 'Nhân sự', icon: '♧', items: [{ label: 'Nhân viên', to: '/employees', permission: 'employee.view' }, { label: 'Phòng ban', to: '/departments', permission: 'department.view' }] },
  { label: 'Quản trị', icon: '⚙', items: [{ label: 'Tài khoản và phân quyền', to: '/settings/users', permission: 'user.view' }, { label: 'Phiên đăng nhập', to: '/settings/sessions' }, { label: 'Tài liệu', to: '/documents', permission: 'document.view' }] },
  { label: 'An ninh AI', icon: '✦', items: [{ label: 'Trung tâm an ninh AI', to: '/security', permission: 'security.view' }] },
]
const visibleGroups = computed(() => groups.map((group) => ({ ...group, items: group.items.filter((item) => !item.permission || item.permission.split('|').some(auth.hasPermission)) })).filter((group) => group.items.length))
const currentTitle = computed(() => visibleGroups.value.flatMap((group) => group.items).find((item) => item.to === route.path)?.label ?? 'SecureBiz AI')
function toggleGroup(label: string) { openGroups.value = openGroups.value.includes(label) ? openGroups.value.filter((value) => value !== label) : [...openGroups.value, label] }
async function logout() { await auth.logout(); await router.replace({ name: 'login' }) }
async function toggleNotifications() { showNotifications.value = !showNotifications.value; showAccount.value = false; if (showNotifications.value) notifications.value = await getNotifications() }
async function markRead(item: NotificationItem) { if (!item.readAt) { await readNotification(item.id); notifications.value = await getNotifications() } }
watch(() => route.fullPath, () => { drawerOpen.value = false; showNotifications.value = false; showAccount.value = false })
onMounted(async () => { if (auth.hasPermission('notification.view')) notifications.value = await getNotifications() })
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-800">
    <div v-if="drawerOpen" class="fixed inset-0 z-40 bg-slate-950/40 lg:hidden" @click="drawerOpen = false" />
    <aside class="fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-slate-200 bg-white transition-all lg:translate-x-0" :class="[drawerOpen ? 'translate-x-0' : '-translate-x-full', collapsed ? 'lg:w-20' : 'lg:w-72']">
      <div class="flex h-16 items-center gap-3 border-b border-slate-100 px-5">
        <div class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-700 font-black text-white">S</div>
        <div v-if="!collapsed" class="min-w-0"><p class="truncate font-bold">SecureBiz AI</p><p class="truncate text-xs text-slate-500">Quản lý bán hàng thông minh</p></div>
        <button class="ml-auto hidden h-8 w-8 rounded-lg hover:bg-slate-100 lg:block" :aria-label="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'" @click="collapsed = !collapsed">{{ collapsed ? '›' : '‹' }}</button>
      </div>
      <nav class="min-h-0 flex-1 overflow-y-auto p-3" aria-label="Điều hướng chính">
        <div v-for="group in visibleGroups" :key="group.label || group.items[0]?.to" class="mb-2">
          <RouterLink v-if="!group.label && group.items[0]" :to="group.items[0].to" class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50" active-class="!bg-cyan-50 !font-semibold !text-cyan-800"><span class="w-5 text-center text-lg">{{ group.icon }}</span><span v-if="!collapsed">{{ group.items[0].label }}</span></RouterLink>
          <template v-else>
            <button class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50" @click="toggleGroup(group.label)"><span class="w-5 text-center text-lg">{{ group.icon }}</span><span v-if="!collapsed" class="flex-1 text-left">{{ group.label }}</span><span v-if="!collapsed">{{ openGroups.includes(group.label) ? '⌃' : '⌄' }}</span></button>
            <div v-if="!collapsed && openGroups.includes(group.label)" class="ml-5 mt-1 border-l border-slate-200 pl-3"><RouterLink v-for="item in group.items" :key="item.to" :to="item.to" class="block rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-50" active-class="!bg-cyan-50 !font-semibold !text-cyan-800">{{ item.label }}</RouterLink></div>
          </template>
        </div>
      </nav>
      <div class="border-t border-slate-100 p-3"><div class="flex items-center gap-3 rounded-xl bg-slate-50 p-3"><div class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-cyan-700 text-sm font-bold text-white">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</div><div v-if="!collapsed" class="min-w-0"><p class="truncate text-sm font-semibold">{{ auth.user?.username }}</p><p class="truncate text-xs text-slate-500">{{ auth.user?.email }}</p></div></div></div>
    </aside>
    <div class="transition-[margin]" :class="collapsed ? 'lg:ml-20' : 'lg:ml-72'">
      <header class="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/95 px-4 backdrop-blur sm:px-6">
        <button class="grid h-10 w-10 place-items-center rounded-lg border lg:hidden" aria-label="Mở menu" @click="drawerOpen = true">☰</button>
        <div class="min-w-0"><p class="truncate text-xs text-slate-500">SecureBiz AI</p><p class="truncate font-semibold">{{ currentTitle }}</p></div>
        <div class="ml-auto flex items-center gap-2">
          <RouterLink v-if="canAccessItConsole(auth.hasPermission)" to="/it/overview" class="hidden rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-semibold text-violet-800 md:inline-flex">Trung tâm Công nghệ & AI</RouterLink>
          <RouterLink v-if="auth.hasPermission('order.create')" to="/pos" class="btn btn-primary hidden sm:inline-flex">＋ Bán hàng</RouterLink>
          <div v-if="auth.hasPermission('notification.view')" class="relative">
            <button class="relative grid h-10 w-10 place-items-center rounded-lg border hover:bg-slate-50" aria-label="Thông báo" @click="toggleNotifications">♢<span v-if="unread" class="absolute -right-1 -top-1 min-w-5 rounded-full bg-red-600 px-1 text-xs font-bold leading-5 text-white">{{ unread > 99 ? '99+' : unread }}</span></button>
            <div v-if="showNotifications" class="absolute right-0 top-12 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border bg-white shadow-xl"><div class="border-b px-4 py-3"><h2 class="font-bold">Thông báo</h2><p class="text-xs text-slate-500">{{ unread }} thông báo chưa đọc</p></div><div class="max-h-96 overflow-auto"><button v-for="item in notifications" :key="item.id" class="block w-full border-b px-4 py-3 text-left hover:bg-slate-50" :class="{ 'bg-cyan-50/50': !item.readAt }" @click="markRead(item)"><span class="block text-sm font-semibold">{{ item.title }}</span><span class="mt-1 block text-xs text-slate-600">{{ item.message }}</span><span class="mt-1 block text-[11px] text-slate-400">{{ formatDateTime(item.createdAt) }}</span></button><p v-if="!notifications.length" class="p-8 text-center text-sm text-slate-400">Chưa có thông báo.</p></div></div>
          </div>
          <div class="relative"><button class="flex h-10 items-center gap-2 rounded-lg border px-2 hover:bg-slate-50" @click="showAccount = !showAccount; showNotifications = false"><span class="grid h-7 w-7 place-items-center rounded-full bg-slate-800 text-xs font-bold text-white">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</span><span class="hidden max-w-28 truncate text-sm font-semibold md:block">{{ auth.user?.username }}</span><span>⌄</span></button><div v-if="showAccount" class="absolute right-0 top-12 w-56 rounded-xl border bg-white p-2 shadow-xl"><RouterLink to="/settings/sessions" class="block rounded-lg px-3 py-2 text-sm hover:bg-slate-50">Phiên đăng nhập</RouterLink><button class="block w-full rounded-lg px-3 py-2 text-left text-sm font-semibold text-red-700 hover:bg-red-50" @click="logout">Đăng xuất</button></div></div>
        </div>
      </header>
      <main><RouterView /></main>
    </div>
  </div>
</template>
