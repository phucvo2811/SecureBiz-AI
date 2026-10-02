<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { checkout, getCustomers, getInventory, getVariants, getWarehouses, type Customer, type Inventory, type Order, type Variant, type Warehouse } from '@/services/business'
import { formatMoney } from '@/utils/format'
import { calculateDiscount, type DiscountMode } from '@/utils/pricing'
import DataState from '@/components/DataState.vue'
type CartLine = { variant: Variant; quantity: number }
const variants = ref<Variant[]>([]), inventory = ref<Inventory[]>([]), warehouses = ref<Warehouse[]>([]), customers = ref<Customer[]>([]), cart = ref<CartLine[]>([])
const warehouseId = ref(''), customerId = ref(''), search = ref(''), discount = ref(0), discountMode = ref<DiscountMode>('AMOUNT'), paymentMethod = ref('CASH'), loading = ref(false), loadingData = ref(true), error = ref(''), completed = ref<Order | null>(null)
const searchInput = ref<HTMLInputElement>()
const stockByVariant = computed(() => new Map(inventory.value.filter((item) => !warehouseId.value || item.warehouseName === warehouses.value.find((warehouse) => warehouse.id === warehouseId.value)?.name).map((item) => [item.variantId, item.availableQuantity])))
const filtered = computed(() => { const query = search.value.toLocaleLowerCase('vi-VN'); return variants.value.filter((variant) => variant.status === 'ACTIVE' && (!query || variant.sku.toLocaleLowerCase().includes(query) || variant.barcode?.toLocaleLowerCase().includes(query) || variant.productName.toLocaleLowerCase().includes(query))).slice(0, 40) })
const subtotal = computed(() => cart.value.reduce((sum, line) => sum + Number(line.variant.sellingPrice) * line.quantity, 0))
const discountAmount = computed(() => calculateDiscount(subtotal.value, discount.value, discountMode.value))
const total = computed(() => Math.max(0, subtotal.value - discountAmount.value))
function available(variant: Variant) { return stockByVariant.value.get(variant.id) ?? 0 }
function add(variant: Variant) { if (available(variant) <= 0) return; const line = cart.value.find((item) => item.variant.id === variant.id); if (line) line.quantity = Math.min(line.quantity + 1, available(variant)); else cart.value.push({ variant, quantity: 1 }); search.value = '' }
function changeQuantity(line: CartLine, delta: number) { line.quantity = Math.max(1, Math.min(available(line.variant), Number(line.quantity || 1) + delta)) }
function remove(line: CartLine) { cart.value = cart.value.filter((item) => item.variant.id !== line.variant.id) }
function onSearchEnter() { if (filtered.value.length === 1) add(filtered.value[0]) }
function newOrder() { completed.value = null; customerId.value = ''; cart.value = []; discount.value = 0; discountMode.value = 'AMOUNT'; nextTick(() => searchInput.value?.focus()) }
async function loadInventory() { inventory.value = (await getInventory('', warehouseId.value || undefined)).items }
async function submit() { if (!warehouseId.value || !cart.value.length || loading.value || total.value <= 0) return; loading.value = true; error.value = ''; try { completed.value = await checkout({ warehouseId: warehouseId.value, customerId: customerId.value || undefined, items: cart.value.map((line) => ({ variantId: line.variant.id, quantity: line.quantity })), discountTotal: discountAmount.value, paymentMethod: paymentMethod.value, idempotencyKey: crypto.randomUUID() }); cart.value = []; discount.value = 0; discountMode.value = 'AMOUNT'; await loadInventory() } catch { error.value = 'Không thể thanh toán. Vui lòng kiểm tra tồn kho, mức giảm giá và thử lại.' } finally { loading.value = false } }
onMounted(async () => { try { [variants.value, warehouses.value, customers.value] = await Promise.all([getVariants(), getWarehouses(), getCustomers()]); if (warehouses.value.length === 1) warehouseId.value = warehouses.value[0].id; await loadInventory() } catch { error.value = 'Không thể tải dữ liệu bán hàng.' } finally { loadingData.value = false } })
</script>

<template>
  <section class="page-shell !max-w-none">
    <div class="mb-5 flex flex-wrap items-end justify-between gap-3"><div><p class="page-eyebrow">Bán hàng tại quầy</p><h1 class="page-title">Tạo hóa đơn</h1><p class="page-description">Tìm hàng hóa, thêm vào giỏ và thanh toán trong một màn hình.</p></div><span class="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">● Sẵn sàng bán hàng</span></div>
    <DataState v-if="loadingData" type="loading" />
    <div v-else class="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_420px]">
      <div class="min-w-0 space-y-4">
        <div class="panel p-4"><label for="pos-search" class="mb-2 block text-sm font-semibold">Tìm sản phẩm</label><div class="relative"><span class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">⌕</span><input id="pos-search" ref="searchInput" v-model.trim="search" autofocus class="input min-h-12 pl-11 text-base" placeholder="Nhập tên, SKU hoặc quét mã vạch rồi nhấn Enter" @keydown.enter.prevent="onSearchEnter" /></div></div>
        <div v-if="filtered.length" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4"><button v-for="variant in filtered" :key="variant.id" class="group rounded-2xl border bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-cyan-500 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-55" :disabled="available(variant) <= 0" @click="add(variant)"><div class="mb-3 grid h-20 place-items-center rounded-xl bg-gradient-to-br from-slate-50 to-cyan-50 text-2xl font-bold text-cyan-700">{{ variant.productName.slice(0, 1).toUpperCase() }}</div><h2 class="line-clamp-2 min-h-10 text-sm font-semibold">{{ variant.productName }}</h2><p class="mt-1 font-mono text-xs text-slate-500">{{ variant.sku }}</p><div class="mt-3 flex items-end justify-between gap-2"><b class="text-cyan-800">{{ formatMoney(variant.sellingPrice) }}</b><span class="text-xs" :class="available(variant) > 0 ? 'text-slate-500' : 'font-semibold text-red-600'">{{ available(variant) > 0 ? `Còn ${available(variant)}` : 'Hết hàng' }}</span></div></button></div>
        <DataState v-else type="empty" title="Không tìm thấy sản phẩm" message="Thử tìm bằng tên, SKU hoặc mã vạch khác." />
      </div>
      <aside class="panel flex flex-col xl:sticky xl:top-20 xl:max-h-[calc(100vh-6rem)] xl:overflow-y-auto">
        <div class="panel-header"><div><h2 class="font-bold">Giỏ hàng</h2><p class="text-xs text-slate-500">{{ cart.length }} mặt hàng</p></div><button v-if="cart.length" class="text-xs font-semibold text-red-600" @click="cart = []">Xóa giỏ hàng</button></div>
        <div class="grid gap-3 border-b p-4 sm:grid-cols-2 xl:grid-cols-1"><label class="field">Kho xuất hàng *<select v-model="warehouseId" required class="input" @change="loadInventory"><option value="" disabled>Chọn kho xuất</option><option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">{{ warehouse.name }}</option></select></label><label class="field">Khách hàng<select v-model="customerId" class="input"><option value="">Khách lẻ</option><option v-for="customer in customers" :key="customer.id" :value="customer.id">{{ customer.name }}{{ customer.phone ? ` · ${customer.phone}` : '' }}</option></select></label></div>
        <div class="min-h-32 flex-1 overflow-y-auto px-4"><div v-for="line in cart" :key="line.variant.id" class="border-b py-4"><div class="flex justify-between gap-3"><div class="min-w-0"><p class="truncate text-sm font-semibold">{{ line.variant.productName }}</p><p class="font-mono text-xs text-slate-500">{{ line.variant.sku }} · {{ formatMoney(line.variant.sellingPrice) }}</p></div><button class="shrink-0 text-xs font-semibold text-red-600" :aria-label="`Xóa ${line.variant.productName}`" @click="remove(line)">Xóa</button></div><div class="mt-3 flex items-center justify-between"><div class="flex items-center rounded-lg border"><button class="h-8 w-9" @click="changeQuantity(line, -1)">−</button><input v-model.number="line.quantity" type="number" min="1" :max="available(line.variant)" class="h-8 w-14 border-x text-center text-sm outline-none" @change="changeQuantity(line, 0)" /><button class="h-8 w-9" @click="changeQuantity(line, 1)">＋</button></div><b>{{ formatMoney(Number(line.variant.sellingPrice) * line.quantity) }}</b></div></div><div v-if="!cart.length" class="grid min-h-44 place-items-center text-center text-sm text-slate-400"><div><div class="mb-2 text-4xl">▱</div><p>Chưa có sản phẩm trong giỏ</p><p class="mt-1 text-xs">Chọn sản phẩm bên trái để bắt đầu</p></div></div></div>
        <div class="border-t bg-slate-50 p-4">
          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <div class="field">
              <span>Giảm giá đơn hàng</span>
              <div class="grid grid-cols-[1fr_auto] overflow-hidden rounded-xl border border-slate-300 bg-white shadow-sm focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
                <input v-model.number="discount" min="0" :max="discountMode === 'PERCENT' ? 100 : subtotal" type="number" class="min-w-0 px-4 py-3 text-right outline-none" :placeholder="discountMode === 'PERCENT' ? '0 - 100' : 'Số tiền giảm'" />
                <div class="flex border-l bg-slate-50 p-1">
                  <button type="button" class="rounded-lg px-3 text-sm font-semibold" :class="discountMode === 'AMOUNT' ? 'bg-white text-cyan-800 shadow-sm' : 'text-slate-500'" @click="discountMode = 'AMOUNT'">₫</button>
                  <button type="button" class="rounded-lg px-3 text-sm font-semibold" :class="discountMode === 'PERCENT' ? 'bg-white text-cyan-800 shadow-sm' : 'text-slate-500'" @click="discountMode = 'PERCENT'">%</button>
                </div>
              </div>
              <small v-if="discountMode === 'PERCENT' && discount > 0" class="text-right text-xs font-normal text-slate-500">{{ Math.min(discount, 100) }}% = {{ formatMoney(discountAmount) }}</small>
            </div>
            <label class="field">Phương thức thanh toán<select v-model="paymentMethod" class="input"><option value="CASH">Tiền mặt</option><option value="CARD">Thẻ</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="E_WALLET">Ví điện tử</option></select></label>
          </div>
          <div class="mt-4 space-y-2 text-sm"><p class="flex justify-between"><span class="text-slate-500">Tạm tính</span><span>{{ formatMoney(subtotal) }}</span></p><p class="flex justify-between"><span class="text-slate-500">Giảm giá</span><span>-{{ formatMoney(discountAmount) }}</span></p><p class="flex justify-between border-t pt-3 text-lg"><b>Tổng thanh toán</b><b class="text-cyan-800">{{ formatMoney(total) }}</b></p></div><p v-if="error" class="alert-error mt-3">{{ error }}</p><div v-if="completed" class="alert-success mt-3"><b>Thanh toán thành công</b><p class="mt-1">Hóa đơn {{ completed.invoiceNumber }} · Đơn {{ completed.orderNumber }}</p><button class="mt-2 font-semibold underline" @click="newOrder">Tạo đơn mới</button></div><button :disabled="loading || !cart.length || !warehouseId || total <= 0" class="btn btn-primary mt-4 min-h-12 w-full text-base" @click="submit">{{ loading ? 'Đang xử lý thanh toán…' : `Thanh toán · ${formatMoney(total)}` }}</button>
        </div>
      </aside>
    </div>
  </section>
</template>
