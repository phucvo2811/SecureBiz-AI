<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createEmployee, getDepartments, getEmployees } from '@/services/organization'
import type { Department, Employee } from '@/types/organization'
import { useAuthStore } from '@/stores/auth'

const items = ref<Employee[]>([])
const search = ref('')
const loading = ref(false)
const error = ref('')
const total = ref(0)
const departments = ref<Department[]>([])
const showCreate = ref(false)
const saving = ref(false)
const auth = useAuthStore()
const form = ref({ employeeCode: '', fullName: '', email: '', departmentId: '', position: '', hireDate: '', status: 'ACTIVE' as const })

async function load() {
  loading.value = true; error.value = ''
  try { const page = await getEmployees(search.value); items.value = page.items; total.value = page.totalElements }
  catch { error.value = 'Không thể tải danh sách nhân viên.' }
  finally { loading.value = false }
}
onMounted(async () => { await load(); if (auth.hasPermission('employee.manage')) departments.value = (await getDepartments('', 0, 100)).items })

async function submitCreate() {
  saving.value = true; error.value = ''
  try {
    await createEmployee(form.value)
    form.value = { employeeCode: '', fullName: '', email: '', departmentId: '', position: '', hireDate: '', status: 'ACTIVE' }
    showCreate.value = false; await load()
  } catch { error.value = 'Không thể tạo nhân viên. Kiểm tra mã, phòng ban và dữ liệu nhập.' }
  finally { saving.value = false }
}
</script>

<template>
  <section class="p-6 lg:p-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div><p class="text-sm font-semibold text-cyan-700">Organization</p><h2 class="text-3xl font-bold">Nhân viên</h2></div>
      <div class="flex flex-wrap gap-2"><form class="flex gap-2" @submit.prevent="load"><input v-model.trim="search" aria-label="Tìm nhân viên" placeholder="Mã, tên hoặc email…" class="rounded-lg border border-slate-300 bg-white px-3 py-2" /><button class="rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white">Tìm</button></form><button v-if="auth.hasPermission('employee.manage')" type="button" class="rounded-lg bg-cyan-600 px-4 py-2 font-semibold text-white" @click="showCreate = !showCreate">Thêm nhân viên</button></div>
    </div>
    <form v-if="showCreate" class="mt-6 grid gap-4 rounded-xl border border-slate-200 bg-white p-5 md:grid-cols-2" @submit.prevent="submitCreate">
      <label class="grid gap-1 text-sm font-medium">Mã nhân viên<input v-model.trim="form.employeeCode" required pattern="[A-Za-z0-9_-]+" maxlength="50" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium">Họ tên<input v-model.trim="form.fullName" required maxlength="200" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium">Email<input v-model.trim="form.email" type="email" maxlength="254" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium">Phòng ban<select v-model="form.departmentId" required class="rounded-lg border border-slate-300 px-3 py-2"><option value="" disabled>Chọn phòng ban</option><option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option></select></label>
      <label class="grid gap-1 text-sm font-medium">Chức vụ<input v-model.trim="form.position" required maxlength="120" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label class="grid gap-1 text-sm font-medium">Ngày vào làm<input v-model="form.hireDate" required type="date" class="rounded-lg border border-slate-300 px-3 py-2" /></label>
      <div class="md:col-span-2 flex justify-end gap-2"><button type="button" class="px-4 py-2" @click="showCreate=false">Hủy</button><button :disabled="saving" class="rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white disabled:opacity-60">{{ saving ? 'Đang lưu…' : 'Tạo nhân viên' }}</button></div>
    </form>
    <p class="mt-3 text-sm text-slate-500">{{ total }} nhân viên</p>
    <p v-if="loading" class="mt-8 text-slate-500">Đang tải…</p>
    <p v-else-if="error" role="alert" class="mt-8 rounded-lg bg-red-50 p-4 text-red-800">{{ error }}</p>
    <div v-else-if="items.length" class="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table class="w-full text-left text-sm"><thead class="bg-slate-50 text-slate-600"><tr><th class="p-4">Mã</th><th class="p-4">Nhân viên</th><th class="p-4">Phòng ban</th><th class="p-4">Chức vụ</th><th class="p-4">Trạng thái</th></tr></thead>
        <tbody class="divide-y divide-slate-100"><tr v-for="item in items" :key="item.id"><td class="p-4 font-mono font-semibold">{{ item.employeeCode }}</td><td class="p-4"><p class="font-medium">{{ item.fullName }}</p><p class="text-xs text-slate-500">{{ item.email || 'Chưa có email' }}</p></td><td class="p-4">{{ item.departmentName }}</td><td class="p-4">{{ item.position }}</td><td class="p-4"><span class="rounded-full bg-slate-100 px-2 py-1 text-xs">{{ item.status }}</span></td></tr></tbody>
      </table>
    </div>
    <div v-else class="mt-8 rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center text-slate-500">Chưa có nhân viên phù hợp.</div>
  </section>
</template>
