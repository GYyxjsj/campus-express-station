<template>
  <span class="flair-num">{{ display }}</span>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  duration: { type: Number, default: 1100 },
  delay: { type: Number, default: 0 },
  decimals: { type: Number, default: 0 }
})

const display = ref('0')
let raf = null, timeout = null

function animate(to) {
  const target = Number(to)
  if (!Number.isFinite(target)) { display.value = String(to ?? '—'); return }
  const from = Number(display.value.replace(/,/g, '')) || 0
  const t0 = performance.now()
  cancelAnimationFrame(raf)
  const tick = now => {
    const p = Math.min(1, (now - t0) / props.duration)
    const eased = 1 - Math.pow(1 - p, 3) // easeOutCubic
    const cur = from + (target - from) * eased
    display.value = cur.toLocaleString('en-US', {
      minimumFractionDigits: props.decimals,
      maximumFractionDigits: props.decimals
    })
    if (p < 1) raf = requestAnimationFrame(tick)
  }
  raf = requestAnimationFrame(tick)
}

watch(() => props.value, v => {
  clearTimeout(timeout)
  timeout = setTimeout(() => animate(v), props.delay)
}, { immediate: true })

onBeforeUnmount(() => { cancelAnimationFrame(raf); clearTimeout(timeout) })
</script>