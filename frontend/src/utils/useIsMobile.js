import { onMounted, onUnmounted, ref } from 'vue'

/** 响应式移动端判断（<=768px），供页面切换表格/卡片布局 */
export function useIsMobile(breakpoint = 768) {
  const isMobile = ref(window.innerWidth <= breakpoint)
  const onResize = () => { isMobile.value = window.innerWidth <= breakpoint }
  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))
  return isMobile
}