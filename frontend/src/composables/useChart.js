import { onBeforeUnmount, onMounted, shallowRef, watch, watchEffect } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '../stores/theme'

/* 八色可分辨色板：色相跨度大 + 明度分层，配合直标与图案仍可区分 */
export const CHART_COLORS = [
  '#5b7cfa', '#10d9b5', '#f59e0b', '#8b5cf6',
  '#0ea5e9', '#fb7185', '#34d399', '#a1abbf'
]

export function useChartStyle() {
  const theme = useThemeStore()
  const isDark = () => theme.mode === 'dark'
  const chartText = () => (isDark() ? '#ababba' : '#7987a1')
  const headText = () => (isDark() ? '#e8e8f2' : '#232338')
  const gridLine = () => (isDark() ? '#2c2c36' : '#eceff3')

  function axisStyle() {
    return {
      axisLabel: { color: chartText(), fontSize: 11, fontFamily: 'Inter, PingFang SC, sans-serif' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: gridLine(), type: [4, 4] } }
    }
  }
  function tipStyle() {
    const dark = isDark()
    return {
      backgroundColor: dark ? '#1c1c22' : '#ffffff',
      borderColor: dark ? '#33333e' : '#e6eaef',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: headText(), fontSize: 12.5 },
      extraCssText: `border-radius:10px;box-shadow:0 10px 28px -8px rgba(15,18,35,${dark ? 0.65 : 0.16});`
    }
  }
  const tipDot = c => `<span style="display:inline-block;width:8px;height:8px;border-radius:2px;background:${c};margin-right:7px;"></span>`
  const tipRow = (color, name, value) =>
    `<div style="display:flex;align-items:center;margin-top:5px;">${tipDot(color)}<span>${name}</span>` +
    `<b style="margin-left:auto;padding-left:24px;font-variant-numeric:tabular-nums;">${value}</b></div>`

  function area(rgb) {
    return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: `rgba(${rgb},${isDark() ? 0.3 : 0.2})` },
      { offset: 1, color: `rgba(${rgb},0.01)` }
    ])
  }
  function barGrad(from, to) {
    return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
      { offset: 0, color: from }, { offset: 1, color: to }
    ])
  }
  return { theme, isDark, chartText, headText, gridLine, axisStyle, tipStyle, tipDot, tipRow, area, barGrad }
}

/** 图表生命周期：init / 数据与主题自动重绘 / 容器与窗口 resize / dispose，全部自动收尾 */
export function useChart(elRef, renderer) {
  const { theme } = useChartStyle()
  const inst = shallowRef(null)
  let ro = null

  function draw() { if (inst.value) renderer(inst.value) }
  function resize() { inst.value?.resize(); draw() }

  // renderer 内读取的响应式数据（接口返回、筛选条件）变化时自动重绘
  const stop = watchEffect(() => { if (inst.value) renderer(inst.value) })

  onMounted(() => {
    if (!elRef.value) return
    inst.value = echarts.init(elRef.value)
    window.addEventListener('resize', resize)
    if (typeof ResizeObserver !== 'undefined') {
      ro = new ResizeObserver(() => inst.value?.resize())
      ro.observe(elRef.value)
    }
  })
  watch(() => theme.mode, draw)
  onBeforeUnmount(() => {
    stop()
    window.removeEventListener('resize', resize)
    ro?.disconnect(); ro = null
    inst.value?.dispose(); inst.value = null
  })

  return { draw, resize }
}