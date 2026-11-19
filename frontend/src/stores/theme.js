import { defineStore } from 'pinia'
import { ref } from 'vue'

const KEY = 'ce_theme'

function resolveInitial() {
  const saved = localStorage.getItem(KEY) || 'auto'
  if (saved === 'dark' || saved === 'light') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function apply(mode) {
  const html = document.documentElement
  html.classList.add('theme-transition')
  html.classList.toggle('dark', mode === 'dark')
  setTimeout(() => html.classList.remove('theme-transition'), 300)
}

/** art 风主题 store：light / dark / auto，localStorage 持久化 */
export const useThemeStore = defineStore('theme', () => {
  const setting = ref(localStorage.getItem(KEY) || 'auto')
  const mode = ref(resolveInitial())
  apply(mode.value)

  const mq = window.matchMedia('(prefers-color-scheme: dark)')
  mq.addEventListener('change', () => {
    if (setting.value === 'auto') {
      mode.value = mq.matches ? 'dark' : 'light'
      apply(mode.value)
    }
  })

  function set(v) {
    setting.value = v
    localStorage.setItem(KEY, v)
    mode.value = v === 'auto' ? (mq.matches ? 'dark' : 'light') : v
    apply(mode.value)
  }

  function toggle() { set(mode.value === 'dark' ? 'light' : 'dark') }

  return { setting, mode, set, toggle }
})