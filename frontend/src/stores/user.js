import { defineStore } from 'pinia'
import { login as loginApi, getMe } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('ce_token') || '',
    user: JSON.parse(localStorage.getItem('ce_user') || 'null'),
    unread: 0
  }),
  getters: {
    logged: s => !!s.token,
    roles: s => (s.user && s.user.roles) || [],
    perms: s => (s.user && s.user.perms) || [],
    isStudent() { return this.roles.includes('STUDENT') },
    isStaff() { return this.roles.includes('STAFF') || this.roles.includes('ADMIN') },
    isAdmin() { return this.roles.includes('ADMIN') }
  },
  actions: {
    hasPerm(perm) {
      return this.roles.includes('ADMIN') || this.perms.includes(perm)
    },
    async login(form) {
      const data = await loginApi(form)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('ce_token', data.token)
      localStorage.setItem('ce_user', JSON.stringify(data.user))
      // 公告弹窗只在「本次登录进入」时弹一次：写入一次性标记，由 EntryGate 消费；刷新页面不会再弹
      sessionStorage.setItem('ce_notice_pending', '1')
      await this.refreshMe()
    },
    async refreshMe() {
      // 拉取最新用户信息；/me 不含 perms 时保留登录返回的权限
      const me = await getMe()
      if (me) {
        this.user = { ...this.user, ...me, perms: me.perms || this.perms }
        localStorage.setItem('ce_user', JSON.stringify(this.user))
      }
    },
    logout() {
      this.token = ''
      this.user = null
      this.unread = 0
      localStorage.removeItem('ce_token')
      localStorage.removeItem('ce_user')
      sessionStorage.removeItem('ce_notice_pending')
    },
    setUnread(n) { this.unread = n }
  }
})