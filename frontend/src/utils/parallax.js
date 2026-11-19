// 鼠标 + 滚动视差：在 :root 写入 --mx/--my（-1..1）与 --sp（滚动像素），rAF 阻尼插值。
// 用独立 translate 属性驱动，避免与元素自身 transform 动画冲突；尊重减弱动效偏好。
export function installParallax(scrollEl) {
  const root = document.documentElement
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return () => {}

  let tx = 0, ty = 0, cx = 0, cy = 0
  let sp = 0, csp = 0
  let raf = null, running = true

  const onPointer = e => {
    tx = (e.clientX / window.innerWidth) * 2 - 1
    ty = (e.clientY / window.innerHeight) * 2 - 1
  }
  const onScroll = () => { sp = scrollEl ? scrollEl.scrollTop : window.scrollY }

  function tick() {
    if (!running) return
    const dx = tx - cx, dy = ty - cy, ds = sp - csp
    if (Math.abs(dx) > 0.0004 || Math.abs(dy) > 0.0004 || Math.abs(ds) > 0.05) {
      cx += dx * 0.06; cy += dy * 0.06; csp += ds * 0.09
      root.style.setProperty('--mx', cx.toFixed(4))
      root.style.setProperty('--my', cy.toFixed(4))
      root.style.setProperty('--sp', csp.toFixed(2))
    }
    raf = requestAnimationFrame(tick)
  }

  window.addEventListener('pointermove', onPointer, { passive: true })
  if (scrollEl) scrollEl.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  raf = requestAnimationFrame(tick)

  return () => {
    running = false
    cancelAnimationFrame(raf)
    window.removeEventListener('pointermove', onPointer)
    if (scrollEl) scrollEl.removeEventListener('scroll', onScroll)
    root.style.removeProperty('--mx')
    root.style.removeProperty('--my')
    root.style.removeProperty('--sp')
  }
}