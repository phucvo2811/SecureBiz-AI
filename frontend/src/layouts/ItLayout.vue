<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const auth=useAuthStore(),route=useRoute()
const items=[
 {label:'Tổng quan',to:'/it/overview',permission:'system.health.view'},
 {label:'Tài khoản',to:'/it/users',permission:'user.view'},
 {label:'Phiên đăng nhập',to:'/it/sessions',permission:'session.view'},
 {label:'An ninh & Cảnh báo',to:'/it/security',permission:'security.view'},
 {label:'Mô hình AI',to:'/it/ai-models',permission:'ai.model.view'},
 {label:'Chính sách AI',to:'/it/ai-policies',permission:'ai.policy.view'},
 {label:'Tích hợp',to:'/it/integrations',permission:'integration.view'},
 {label:'Sức khỏe hệ thống',to:'/it/system-health',permission:'system.health.view'},
 {label:'Nhật ký kiểm toán',to:'/it/audit-logs',permission:'audit.view'},
]
const visible=computed(()=>items.filter(i=>auth.hasPermission(i.permission)))
const title=computed(()=>visible.value.find(i=>route.path.startsWith(i.to))?.label??'Trung tâm Công nghệ & AI')
</script>
<template><div class="min-h-screen bg-slate-950 text-slate-100"><aside class="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-800 bg-slate-900 lg:block"><div class="border-b border-slate-800 p-5"><div class="flex items-center gap-3"><span class="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-cyan-400 to-violet-600 font-black">S</span><div><b>SecureBiz AI</b><p class="text-xs text-cyan-300">Trung tâm Công nghệ & AI</p></div></div></div><nav class="space-y-1 p-3"><RouterLink v-for="item in visible" :key="item.to" :to="item.to" class="block rounded-xl px-4 py-2.5 text-sm text-slate-300 hover:bg-slate-800" active-class="!bg-cyan-500/15 !text-cyan-200">{{item.label}}</RouterLink></nav></aside><div class="lg:ml-64"><header class="sticky top-0 z-20 flex h-16 items-center border-b border-slate-800 bg-slate-950/90 px-5 backdrop-blur"><div><p class="text-xs text-slate-400">Công nghệ / {{title}}</p><h1 class="font-semibold">{{title}}</h1></div><RouterLink to="/" class="ml-auto rounded-lg border border-slate-700 px-3 py-2 text-sm hover:bg-slate-800">Vận hành doanh nghiệp →</RouterLink></header><main class="p-4 sm:p-6 lg:p-8"><RouterView/></main></div></div></template>
