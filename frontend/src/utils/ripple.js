/**
 * v-ripple —— 按钮/卡片点击涟漪（Material 风格，配合 art-flair.css）
 * 用法：<el-button v-ripple> 或任意元素
 */
export const ripple = {
  mounted(el) {
    el.addEventListener('pointerdown', e => {
      if (el.classList.contains('is-disabled')) return
      const rect = el.getBoundingClientRect()
      const size = Math.max(rect.width, rect.height)
      const span = document.createElement('span')
      span.className = 'flair-ripple'
      span.style.width = span.style.height = size + 'px'
      span.style.left = e.clientX - rect.left - size / 2 + 'px'
      span.style.top = e.clientY - rect.top - size / 2 + 'px'
      const pos = getComputedStyle(el).position
      if (pos === 'static') el.style.position = 'relative'
      el.appendChild(span)
      span.addEventListener('animationend', () => span.remove())
    })
  }
}