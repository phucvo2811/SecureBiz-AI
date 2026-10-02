<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createDepartment, getDepartments } from '@/services/organization'
import type { Department } from '@/types/organization'
import { useAuthStore } from '@/stores/auth'

const items = ref<Department[]>([])
const search = ref('')
const loading = ref(false)
const error = ref('')
const total = ref(0)
const showCreate = ref(false)
const saving = ref(false)
const form = ref({ code: '', name: '', description: '', status: 'ACTIVE' as const })
const auth = useAuthStore()

async function load() {
  loading.value = true; error.value = ''
  try {
    const page = await getDepartments(search.value)
    items.value = page.items; total.value = page.totalElements
  } catch { error.value = 'Không thể tải danh sách phòng ban.' }
  finally { loading.value = false }
}

onMounted(load)

async function submitCreate() {
  saving.value = true; error.value = ''
  try {
    await createDepartment(form.value)
    form.value = { code: '', name: '', description: '', status: 'ACTIVE' }
    showCreate.value = false
    await load()
  } catch { error.value = 'Không thể tạo phòng ban. Kiểm tra mã phòng ban và quyền truy cập.' }
  finally { saving.value = false }
}
</script>

<template>
  <section class="p-6 lg:p-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div><p class="text-sm font-semibold text-cyan-700">Organization</p><h2 class="text-3xl font-bold">Phòng ban</h2></div>
      <div class="flex flex-wrap gap-2"><form class="flex gap-2" @submit.prevent="load">
        <input v-model.trim="search" aria-label="Tìm phòng ban" placeholder="Mã hoặc tên…" class="rounded-lg border border-slate-300 bg-white px-3 py-2" />
        <button class="rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white">Tìm</button>
      </form><button v-if="auth.hasPermission('department.manage')" type="button" class="rounded-lg bg-cyan-600 px-4 py-2 font-semibold text-white" @click="showCreate = !showCreate">Thêm phòng ban</button></div>
    </div>
    <form v-if="showCreate" class="mt-6 grid gap-4 rounded-xl border border-slate-200 bg-white p-5 md:grid-cols-2" @submit.prevent="submitCreate">
      <label class="grid gap-1 text-sm font-medium">Mã<input v-model.trim="form.code" required maxlength="50" pattern="[A-Za-z0-9_-]+" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium">Tên<input v-model.trim="form.name" required maxlength="150" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium md:col-span-2">Mô tả<textarea v-model.trim="form.description" maxlength="1000" class="rounded-lg border border-slate-300 px-3 py-2"></textarea></label>
      <div class="md:col-span-2 flex justify-end gap-2"><button type="button" class="px-4 py-2" @click="showCreate=false">Hủy</button><button :disabled="saving" class="rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white disabled:opacity-60">{{ saving ? 'Đang lưu…' : 'Tạo phòng ban' }}</button></div>
    </form>
    <p class="mt-3 text-sm text-slate-500">{{ total }} phòng ban</p>
    <p v-if="loading" class="mt-8 text-slate-500">Đang tải…</p>
    <p v-else-if="error" role="alert" class="mt-8 rounded-lg bg-red-50 p-4 text-red-800">{{ error }}</p>
    <div v-else-if="items.length" class="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table class="w-full text-left text-sm"><thead class="bg-slate-50 text-slate-600"><tr><th class="p-4">Mã</th><th class="p-4">Tên</th><th class="p-4">Trạng thái</th><th class="p-4">Mô tả</th></tr></thead>
        <tbody class="divide-y divide-slate-100"><tr v-for="item in items" :key="item.id"><td class="p-4 font-mono font-semibold">{{ item.code }}</td><td class="p-4 font-medium">{{ item.name }}</td><td class="p-4"><span class="rounded-full bg-slate-100 px-2 py-1 text-xs">{{ item.status }}</span></td><td class="p-4 text-slate-500">{{ item.description || '—' }}</td></tr></tbody>
      </table>
    </div>
    <div v-else class="mt-8 rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-slate-500">Chưa có phòng ban phù hợp.</div>
  </section>
</template>
