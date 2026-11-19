<template>
  <svg ref="svgRef" xmlns="http://www.w3.org/2000/svg"
       :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`"
       preserveAspectRatio="xMidYMid meet"
       role="img" :aria-label="`条码 ${value}`"
       style="width:100%;height:auto;display:block">
    <rect x="0" y="0" :width="width" :height="height" fill="#ffffff" />
    <rect v-for="(b, i) in bars" :key="i" :x="b.x" y="0" :width="b.w" :height="barHeight" fill="#101018" />
    <text v-if="showText" :x="width / 2" :y="barHeight + 16" text-anchor="middle"
          font-family="'JetBrains Mono',Consolas,'Courier New',monospace"
          font-size="13" font-weight="700" letter-spacing="2.5" fill="#101018">{{ value }}</text>
  </svg>
</template>

<script setup>
import { computed, ref } from 'vue'

/* Code 128 模块宽度表（值 0-106，最后一项为终止符） */
const PATTERNS = [
  '212222', '222122', '222221', '121223', '121322', '131222', '122213', '122312', '132212', '221213',
  '221312', '231212', '112232', '122132', '122231', '113222', '123122', '123221', '223211', '221132',
  '221231', '213212', '223112', '312131', '311222', '321122', '321221', '312212', '322112', '322211',
  '212123', '212321', '232121', '111323', '131123', '131321', '112313', '132113', '132311', '211313',
  '231113', '231311', '112133', '112331', '132131', '113123', '113321', '133121', '313121', '211331',
  '231131', '213113', '213311', '213131', '311123', '311321', '331121', '312113', '312311', '332111',
  '314111', '221411', '431111', '111224', '111422', '121124', '121421', '141122', '141221', '112214',
  '112412', '122114', '122411', '142112', '142211', '241211', '221114', '413111', '241112', '134111',
  '111242', '121142', '121241', '114212', '124112', '124211', '411212', '421112', '421211', '212141',
  '214121', '412121', '111143', '111341', '131141', '114113', '114311', '411113', '411311', '113141',
  '114131', '311141', '411131', '211412', '211214', '211232', '2331112'
]
const START_B = 104
const STOP = 106

const props = defineProps({
  value: { type: String, default: '' },
  scale: { type: Number, default: 2 },
  barHeight: { type: Number, default: 82 },
  showText: { type: Boolean, default: true }
})

const svgRef = ref(null)

const codes = computed(() => {
  const text = String(props.value || '').replace(/[^\x20-\x7e]/g, '')
  if (!text) return []
  const vals = [START_B]
  let sum = START_B
  for (let i = 0; i < text.length; i++) {
    const v = text.charCodeAt(i) - 32
    vals.push(v)
    sum += v * (i + 1)
  }
  vals.push(sum % 103)
  vals.push(STOP)
  return vals
})

const bars = computed(() => {
  const out = []
  let x = 10 * props.scale // 左侧静区
  for (const c of codes.value) {
    const p = PATTERNS[c]
    for (let i = 0; i < p.length; i++) {
      const w = Number(p[i]) * props.scale
      if (i % 2 === 0) out.push({ x, w })
      x += w
    }
  }
  return out
})

// 每个符号的模块数 = 图案各位数字之和（普通符 11、终止符 13），而非数字个数；两侧各留 10 模块静区
const modules = computed(() => codes.value.reduce((s, c) => s + PATTERNS[c].split('').reduce((a, d) => a + Number(d), 0), 0) + 20)
const width = computed(() => modules.value * props.scale)
const height = computed(() => props.barHeight + (props.showText ? 22 : 0))

function svgString() {
  return svgRef.value ? new XMLSerializer().serializeToString(svgRef.value) : ''
}
function download(filename) {
  const blob = new Blob([svgString()], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || `barcode-${props.value}.svg`
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
defineExpose({ download, svgString })
</script>