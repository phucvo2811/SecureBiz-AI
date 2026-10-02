<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel, statusTone } from '@/utils/format'

const props = defineProps<{ value?: string; label?: string }>()
const tone = computed(() => statusTone(props.value))
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <span class="status-badge__dot" aria-hidden="true" />
    {{ label ?? statusLabel(value) }}
  </span>
</template>
