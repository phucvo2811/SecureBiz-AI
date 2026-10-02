<script setup lang="ts">
withDefaults(defineProps<{ type: 'loading' | 'empty' | 'error'; title?: string; message?: string }>(), {
  title: '', message: '',
})
</script>

<template>
  <div class="data-state" :class="`data-state--${type}`" :role="type === 'error' ? 'alert' : 'status'">
    <div v-if="type === 'loading'" class="data-state__spinner" aria-hidden="true" />
    <div v-else class="data-state__icon" aria-hidden="true">{{ type === 'error' ? '!' : '○' }}</div>
    <div>
      <p class="font-semibold">{{ title || (type === 'loading' ? 'Đang tải dữ liệu' : type === 'empty' ? 'Chưa có dữ liệu' : 'Không thể tải dữ liệu') }}</p>
      <p v-if="message" class="mt-1 text-sm text-slate-500">{{ message }}</p>
    </div>
    <slot />
  </div>
</template>
