<template>
  <section class="ap-hero flair-in" :style="vars">
    <div class="ap-hero-orbs" aria-hidden="true">
      <el-icon class="ap-hero-orb o1"><component :is="icon || 'Box'" /></el-icon>
      <el-icon class="ap-hero-orb o2"><Van /></el-icon>
      <el-icon class="ap-hero-orb o3"><Bell /></el-icon>
      <i class="ap-hero-ring r1" />
      <i class="ap-hero-ring r2" />
    </div>
    <el-icon v-if="icon" class="ap-hero-glyph" aria-hidden="true"><component :is="icon" /></el-icon>
    <div class="ap-hero-main">
      <span v-if="chip" class="ap-hero-chip"><i v-if="live" class="live-dot" />{{ chip }}</span>
      <h1 class="ap-hero-title">{{ title }}</h1>
      <p v-if="sub" class="ap-hero-sub">{{ sub }}</p>
      <slot name="main" />
    </div>
    <div v-if="stats && stats.length" class="ap-hero-side">
      <div v-for="s in stats" :key="s.label" class="ap-hero-mini">
        <b><CountUp :value="s.value" :decimals="s.decimals || 0" :duration="1200" /><small v-if="s.unit">{{ s.unit }}</small></b>
        <span>{{ s.label }}</span>
      </div>
    </div>
    <div v-if="$slots.actions" class="ap-hero-actions"><slot name="actions" /></div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import CountUp from './CountUp.vue'

const TONES = {
  parcel: ['#4c6ef5', '#5b7cfa', '#8b5cf6'],
  wallet: ['#b45309', '#f59e0b', '#fbbf24'],
  identity: ['#5b21b6', '#8b5cf6', '#c084fc'],
  proxy: ['#0369a1', '#0ea5e9', '#22d3ee'],
  trace: ['#0f766e', '#0d9488', '#34d399'],
  policy: ['#3730a3', '#6366f1', '#818cf8'],
  notice: ['#be123c', '#fb7185', '#fda4af'],
  staff: ['#1d4ed8', '#3b82f6', '#38bdf8'],
  admin: ['#4338ca', '#6366f1', '#a855f7'],
  danger: ['#9f1239', '#e11d48', '#fb7185']
}

const props = defineProps({
  title: { type: String, required: true },
  sub: { type: String, default: '' },
  chip: { type: String, default: '' },
  icon: { type: String, default: '' },
  tone: { type: String, default: 'parcel' },
  live: { type: Boolean, default: false },
  stats: { type: Array, default: () => [] }
})

const vars = computed(() => {
  const t = TONES[props.tone] || TONES.parcel
  return { '--ha': t[0], '--hb': t[1], '--hc': t[2] }
})
</script>

<style scoped>
.ap-hero-mini b small { font-size: 0.55em; font-weight: 700; margin-left: 2px; opacity: 0.85; }
</style>