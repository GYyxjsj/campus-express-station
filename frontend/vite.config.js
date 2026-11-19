import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发态：/campus-express 代理到本机 Tomcat(8080)
// 对外分享建议用 `npm run preview`（托管构建产物，无 HMR，不会因热更新反复刷新）
const proxy = {
  '/campus-express': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true
  }
}

// 开发态跑在 5173（根路径），构建产物部署到 Tomcat 的 /campus-express/ 上下文
export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/campus-express/' : '/',
  plugins: [vue()],
  server: {
    port: 5173,
    host: true,
    allowedHosts: true,
    // 经隧道/内网穿透访问时 HMR WebSocket 常连不上，关闭错误遮罩避免干扰
    hmr: { overlay: false },
    proxy
  },
  preview: {
    port: 4173,
    host: true,
    allowedHosts: true,
    proxy,
    headers: {
      // SPA 入口不缓存，带 hash 的静态资源长缓存（发版后文件名变化自动失效）
      'Cache-Control': 'no-cache'
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
}))