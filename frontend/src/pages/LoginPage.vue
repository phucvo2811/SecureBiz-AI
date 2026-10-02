<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const identifier = ref(''), password = ref(''), loading = ref(false), error = ref(''), showPassword = ref(false)
const auth = useAuthStore(), router = useRouter(), route = useRoute()
async function submit() {
  error.value = ''; loading.value = true
  try { await auth.login(identifier.value, password.value, navigator.userAgent); await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/') }
  catch { error.value = 'Không thể đăng nhập. Vui lòng kiểm tra thông tin hoặc thử lại sau.' }
  finally { loading.value = false }
}
</script>

<template>
  <main class="grid min-h-screen bg-white lg:grid-cols-[1.05fr_.95fr]">
    <section class="relative hidden overflow-hidden bg-gradient-to-br from-cyan-800 via-cyan-700 to-blue-800 p-12 text-white lg:flex lg:flex-col lg:justify-between">
      <div class="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-white/10" />
      <div class="relative flex items-center gap-3"><div class="grid h-11 w-11 place-items-center rounded-2xl bg-white font-black text-cyan-800">S</div><div><p class="text-xl font-bold">SecureBiz AI</p><p class="text-sm text-cyan-100">Quản lý bán hàng thông minh</p></div></div>
      <div class="relative max-w-xl"><p class="text-sm font-semibold uppercase tracking-[.2em] text-cyan-100">Vận hành hiệu quả · An toàn chủ động</p><h1 class="mt-5 text-4xl font-bold leading-tight xl:text-5xl">Quản lý cửa hàng rõ ràng hơn trong từng thao tác.</h1><p class="mt-5 max-w-lg text-lg leading-8 text-cyan-50/90">Bán hàng, tồn kho, nhập hàng, báo cáo và alert an ninh trên một hệ thống thống nhất.</p><div class="mt-10 grid grid-cols-3 gap-3 text-sm"><div class="rounded-2xl bg-white/10 p-4"><b class="block text-xl">Nhanh</b><span class="text-cyan-100">Tối ưu bán hàng</span></div><div class="rounded-2xl bg-white/10 p-4"><b class="block text-xl">Rõ</b><span class="text-cyan-100">Số liệu tức thời</span></div><div class="rounded-2xl bg-white/10 p-4"><b class="block text-xl">An toàn</b><span class="text-cyan-100">Giám sát bằng AI</span></div></div></div>
      <p class="relative text-xs text-cyan-100/80">© 2026 SecureBiz AI</p>
    </section>
    <section class="flex items-center justify-center bg-slate-50 p-5 sm:p-10">
      <div class="w-full max-w-md">
        <div class="mb-8 flex items-center gap-3 lg:hidden"><div class="grid h-10 w-10 place-items-center rounded-xl bg-cyan-700 font-black text-white">S</div><div><p class="font-bold">SecureBiz AI</p><p class="text-xs text-slate-500">Quản lý bán hàng thông minh</p></div></div>
        <div class="rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/50 sm:p-9">
          <p class="text-sm font-semibold text-cyan-700">Chào mừng trở lại</p><h2 class="mt-2 text-3xl font-bold tracking-tight">Đăng nhập hệ thống</h2><p class="mt-2 text-sm leading-6 text-slate-500">Sử dụng tài khoản do quản trị viên cửa hàng cấp.</p>
          <form class="mt-8 grid gap-5" @submit.prevent="submit">
            <label class="field">Tên đăng nhập hoặc email <span class="sr-only">bắt buộc</span><input v-model.trim="identifier" required autocomplete="username" maxlength="254" class="input" placeholder="Nhập tên đăng nhập hoặc email" /></label>
            <label class="field">Mật khẩu <span class="relative"><input v-model="password" required :type="showPassword ? 'text' : 'password'" autocomplete="current-password" maxlength="200" class="input pr-14" placeholder="Nhập mật khẩu" /><button type="button" class="absolute right-2 top-1/2 -translate-y-1/2 rounded px-2 py-1 text-xs font-semibold text-cyan-700" @click="showPassword = !showPassword">{{ showPassword ? 'Ẩn' : 'Hiện' }}</button></span></label>
            <p v-if="error" class="alert-error" role="alert">{{ error }}</p>
            <button :disabled="loading" class="btn btn-primary min-h-12 w-full text-base"><span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />{{ loading ? 'Đang đăng nhập…' : 'Đăng nhập' }}</button>
          </form>
        </div>
      </div>
    </section>
  </main>
</template>
