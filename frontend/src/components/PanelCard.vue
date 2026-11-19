<template>
  <section class="ap-panel flair-in" :style="{ '--pa': a, '--pb': b, '--i': index }">
    <header v-if="title || $slots.extra" class="ap-panel-head">
      <span class="ap-panel-title">
        <el-icon v-if="icon" :size="16"><component :is="icon" /></el-icon>
        {{ title }}
        <span v-if="note" class="ap-panel-note">{{ note }}</span>
      </span>
      <div v-if="$slots.extra" class="ap-panel-extra"><slot name="extra" /></div>
    </header>
    <div class="ap-panel-body" :class="{ tight, flush }"><slot /></div>
    <footer v-if="$slots.foot" class="ap-panel-foot"><slot name="foot" /></footer>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const ACCENTS = {
  parcel: ['#5b7cfa', '#8b5cf6'],
  wallet: ['#f59e0b', '#fbbf24'],
  identity: ['#8b5cf6', '#c084fc'],
  proxy: ['#0ea5e9', '#22d3ee'],
  trace: ['#0d9488', '#34d399'],
  policy: ['#6366f1', '#818cf8'],
  notice: ['#fb7185', '#fda4af'],
  mint: ['#10d9b5', '#38bdf8'],
  amber: ['#ffb020', '#fb7185'],
  slate: ['#7987a1', '#a1abbf']
}

const props = defineProps({
  title: { type: String, default: '' },
  note: { type: String, default: '' },
  icon: { type: String, default: '' },
  accent: { type: String, default: 'parcel' },
  index: { type: Number, default: 0 },
  tight: { type: Boolean, default: false },
  flush: { type: Boolean, default: false }
})

const a = computed(() => (ACCENTS[props.accent] || ACCENTS.parcel)[0])
const b = computed(() => (ACCENTS[props.accent] || ACCENTS.parcel)[1])
</script>