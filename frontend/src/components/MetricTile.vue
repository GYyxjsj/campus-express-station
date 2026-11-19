<template>
  <div class="ap-metric flair-in" :style="{ '--mc': color, '--i': index }">
    <div class="ap-metric-top">
      <span class="ap-metric-label">{{ label }}</span>
      <div v-if="icon" class="ap-metric-icon"><el-icon :size="18"><component :is="icon" /></el-icon></div>
    </div>
    <span class="ap-metric-num">
      <CountUp :value="value" :decimals="decimals" :duration="1200" :delay="index * 80" />
      <small v-if="unit">{{ unit }}</small>
    </span>
    <div v-if="foot" class="ap-metric-foot"><span class="dot" />{{ foot }}</div>
    <div class="ap-metric-bar" />
  </div>
</template>

<script setup>
import CountUp from './CountUp.vue'

defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: 0 },
  foot: { type: String, default: '' },
  unit: { type: String, default: '' },
  color: { type: String, default: '#5b7cfa' },
  icon: { type: String, default: '' },
  decimals: { type: Number, default: 0 },
  index: { type: Number, default: 0 }
})
</script>