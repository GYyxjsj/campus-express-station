import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 与后端 context-path 一致；开发态由 Vite 代理，生产态由 Nginx 反代
const http = axios.create({
  baseURL: '/campus-express',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('ce_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 40100) {
        handleUnauthorized()
        return Promise.reject(new Error(body.msg))
      }
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(new Error(body.msg || 'biz error ' + body.code))
    }
    return body
  },
  error => {
    const status = error.response && error.response.status
    const body = error.response && error.response.data
    if ((body && body.code === 40100) || status === 401) {
      handleUnauthorized()
    } else {
      /* 同一时刻多个请求失败只弹一条，避免错误提示刷屏 */
      throttledError((body && body.msg) || (status ? `请求失败(${status})` : '网络异常，请稍后重试'))
    }
    return Promise.reject(error)
  }
)

/* 登录失效去重：并发请求同时 401 时只提示、只跳转一次 */
let authHandling = false
function handleUnauthorized() {
  if (authHandling) return
  authHandling = true
  localStorage.removeItem('ce_token')
  localStorage.removeItem('ce_user')
  ElMessage.warning('登录已失效，请重新登录')
  router.push('/login')
  setTimeout(() => { authHandling = false }, 3000)
}

let lastErrAt = 0
function throttledError(msg) {
  const now = Date.now()
  if (now - lastErrAt < 2500) return
  lastErrAt = now
  ElMessage.error(msg)
}

export default http