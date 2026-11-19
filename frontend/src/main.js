import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/art-theme.css'
import './styles/art-flair.css'
import './styles/art-page.css'
import { ripple } from './utils/ripple'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// v-perm 指令：无权限点则移除元素（ADMIN 全权限）
app.directive('perm', {
  mounted(el, binding) {
    const store = useUserStore()
    if (!store.hasPerm(binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

app.directive('ripple', ripple)

app.mount('#app')