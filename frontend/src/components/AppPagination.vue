<script setup lang="ts">
defineProps<{ page: number; totalPages: number }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="Phân trang">
    <button class="btn btn-secondary btn-sm" :disabled="page === 0" @click="$emit('change', page - 1)">← Trước</button>
    <span class="pagination__label">Trang <b>{{ page + 1 }}</b> / {{ totalPages }}</span>
    <button class="btn btn-secondary btn-sm" :disabled="page + 1 >= totalPages" @click="$emit('change', page + 1)">Sau →</button>
  </nav>
</template>
