<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOrdersPage, type Order } from '@/services/business'
import { formatDateTime, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import DataState from '@/components/DataState.vue'
import AppPagination from '@/components/AppPagination.vue'
const items = ref<Order[]>([]), error = ref(''), status = ref(''), sort = ref('createdAt'), page = ref(0), totalPages = ref(0), loading = ref(false)
async function load(reset = false) { if (reset) page.value = 0; loading.value = true; error.value = ''; try { const result = await getOrdersPage(status.value, page.value, 20, sort.value); items.value = result.items; totalPages.value = result.totalPages } catch { error.value = 'Không thể tải danh sách hóa đơn.' } finally { loading.value = false } }
function changePage(value: number) { page.value = value; void load() }
onMounted(() => void load())
</script>

<template>
  <section class="page-shell">
    <PageHeader eyebrow="Bán hàng" title="Hóa đơn" description="Tra cứu hóa đơn, trạng thái thanh toán và giá trị đơn hàng."><template #actions><RouterLink to="/pos" class="btn btn-primary">＋ Tạo hóa đơn</RouterLink></template></PageHeader>
    <form class="filter-bar mt-6" @submit.prevent="load(true)"><label class="field">Trạng thái<select v-model="status" class="input"><option value="">Tất cả trạng thái</option><option value="COMPLETED">Hoàn tất</option><option value="PARTIALLY_RETURNED">Đã trả một phần</option><option value="RETURNED">Đã trả hàng</option></select></label><label class="field">Sắp xếp<select v-model="sort" class="input"><option value="createdAt">Mới nhất</option><option value="total">Tổng tiền giảm dần</option></select></label><button class="btn btn-secondary">Lọc dữ liệu</button></form>
    <DataState v-if="loading" class="mt-5" type="loading" />
    <DataState v-else-if="error" class="mt-5" type="error" :message="error" />
    <div v-else-if="items.length" class="table-wrap mt-5"><table class="data-table"><thead><tr><th>Mã đơn</th><th>Thời gian</th><th>Trạng thái đơn</th><th>Thanh toán</th><th class="text-right">Tổng tiền</th><th>Số hóa đơn</th></tr></thead><tbody><tr v-for="order in items" :key="order.id"><td class="font-mono font-semibold text-cyan-800">{{ order.orderNumber }}</td><td>{{ formatDateTime(order.createdAt) }}</td><td><StatusBadge :value="order.status" /></td><td><StatusBadge :value="order.paymentStatus" /></td><td class="text-right font-semibold">{{ formatMoney(order.total) }}</td><td class="font-mono">{{ order.invoiceNumber }}</td></tr></tbody></table></div>
    <DataState v-else class="mt-5" type="empty" title="Chưa có hóa đơn phù hợp" message="Thử thay đổi bộ lọc hoặc tạo hóa đơn bán hàng mới." />
    <AppPagination :page="page" :total-pages="totalPages" @change="changePage" />
  </section>
</template>
