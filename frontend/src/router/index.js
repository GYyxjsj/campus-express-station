import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { anon: true, title: '登录' } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { anon: true, title: '注册' } },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: () => {
      const s = useUserStore()
      if (s.isAdmin) return '/admin/dashboard'
      if (s.isStaff) return '/staff/inbound'
      return '/student/parcels'
    },
    children: [
      { path: 'student/parcels', component: () => import('../views/student/Parcels.vue'), meta: { roles: ['STUDENT'], title: '我的包裹' } },
      { path: 'student/chat', component: () => import('../views/student/Chat.vue'), meta: { roles: ['STUDENT'], title: '消息聊天' } },
      { path: 'student/granted', component: () => import('../views/student/Granted.vue'), meta: { roles: ['STUDENT'], title: '我发起的授权' } },
      { path: 'student/received', component: () => import('../views/student/Received.vue'), meta: { roles: ['STUDENT'], title: '受托代取' } },
      { path: 'student/wallet', component: () => import('../views/student/Wallet.vue'), meta: { roles: ['STUDENT'], title: '我的钱包' } },
      { path: 'student/identity', component: () => import('../views/student/Identity.vue'), meta: { roles: ['STUDENT'], title: '身份条码' } },
      { path: 'student/proxy-market', component: () => import('../views/student/ProxyMarket.vue'), meta: { roles: ['STUDENT'], title: '代取服务大厅' } },
      { path: 'student/proxy-orders', component: () => import('../views/student/ProxyOrders.vue'), meta: { roles: ['STUDENT'], title: '我的代取服务' } },
      { path: 'student/ship', component: () => import('../views/student/Ship.vue'), meta: { roles: ['STUDENT'], title: '寄件与运费' } },
      { path: 'student/insurance', component: () => import('../views/student/Insurance.vue'), meta: { roles: ['STUDENT'], title: '超时保险' } },
      { path: 'student/coupons', component: () => import('../views/student/Coupons.vue'), meta: { roles: ['STUDENT'], title: '红包卡券' } },
      { path: 'student/invite', component: () => import('../views/student/Invite.vue'), meta: { roles: ['STUDENT'], title: '邀请有礼' } },
      { path: 'student/addresses', component: () => import('../views/student/Addresses.vue'), meta: { roles: ['STUDENT'], title: '地址簿' } },
      { path: 'student/contacts', component: () => import('../views/student/Contacts.vue'), meta: { roles: ['STUDENT'], title: '电话簿' } },
      { path: 'student/preference', component: () => import('../views/student/Preference.vue'), meta: { roles: ['STUDENT'], title: '收货偏好' } },
      { path: 'student/tickets', component: () => import('../views/student/Tickets.vue'), meta: { roles: ['STUDENT'], title: '客服工单' } },
      { path: 'student/feedback', component: () => import('../views/student/Feedback.vue'), meta: { roles: ['STUDENT'], title: '意见反馈' } },
      { path: 'student/about', component: () => import('../views/student/About.vue'), meta: { roles: ['STUDENT'], title: '关于我们' } },
      { path: 'staff/inbound', component: () => import('../views/staff/Inbound.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '入库工作台' } },
      { path: 'staff/parcels', component: () => import('../views/staff/Parcels.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '包裹管理' } },
      { path: 'staff/pickup', component: () => import('../views/staff/Pickup.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '取件核销台' } },
      { path: 'staff/identity', component: () => import('../views/staff/IdentityVerify.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '身份核验台' } },
      { path: 'staff/traces', component: () => import('../views/staff/Trace.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '去向台账' } },
      { path: 'staff/overdue', component: () => import('../views/staff/Overdue.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '滞留件管理' } },
      { path: 'staff/shelves', component: () => import('../views/staff/Shelves.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '货架查看' } },
      { path: 'staff/import', component: () => import('../views/staff/Import.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '批量导入包裹' } },
      { path: 'staff/ship-orders', component: () => import('../views/staff/ShipOrders.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '自寄单管理' } },
      { path: 'staff/tickets', component: () => import('../views/staff/Tickets.vue'), meta: { roles: ['STAFF', 'ADMIN'], title: '客服工单台' } },
      { path: 'admin/dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { roles: ['ADMIN'], title: '运营看板' } },
      { path: 'admin/users', component: () => import('../views/admin/Users.vue'), meta: { roles: ['ADMIN'], title: '用户管理' } },
      { path: 'admin/stations', component: () => import('../views/admin/Stations.vue'), meta: { roles: ['ADMIN'], title: '驿站与货架' } },
      { path: 'admin/wallet', component: () => import('../views/admin/Wallet.vue'), meta: { roles: ['ADMIN'], title: '资金账务' } },
      { path: 'admin/proxy-orders', component: () => import('../views/admin/ProxyOrders.vue'), meta: { roles: ['ADMIN'], title: '代取监管' } },
      { path: 'admin/policies', component: () => import('../views/admin/Policies.vue'), meta: { roles: ['ADMIN'], title: '协议治理' } },
      { path: 'admin/configs', component: () => import('../views/admin/Configs.vue'), meta: { roles: ['ADMIN'], title: '策略配置' } },
      { path: 'admin/notices', component: () => import('../views/admin/Notices.vue'), meta: { roles: ['ADMIN'], title: '公告管理' } },
      { path: 'admin/logs', component: () => import('../views/admin/Logs.vue'), meta: { roles: ['ADMIN'], title: '操作日志' } },
      { path: 'admin/ads', component: () => import('../views/admin/Ads.vue'), meta: { roles: ['ADMIN'], title: '广告位管理' } },
      { path: 'admin/coupons', component: () => import('../views/admin/Coupons.vue'), meta: { roles: ['ADMIN'], title: '红包卡券工厂' } },
      { path: 'admin/feedback', component: () => import('../views/admin/Feedback.vue'), meta: { roles: ['ADMIN'], title: '意见反馈处理' } },
      { path: 'admin/settings', component: () => import('../views/admin/Settings.vue'), meta: { roles: ['ADMIN'], title: '系统设置' } },
      { path: 'notifications', component: () => import('../views/Notifications.vue'), meta: { title: '提醒中心' } },
      { path: 'notices', component: () => import('../views/Notices.vue'), meta: { title: '驿站公告' } },
      { path: 'policies', component: () => import('../views/Policies.vue'), meta: { title: '政策协议' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(import.meta.env.BASE_URL), routes })

router.beforeEach((to) => {
  const s = useUserStore()
  if (to.meta.anon) {
    return s.logged ? '/' : true
  }
  if (!s.logged) return { path: '/login', query: { redirect: to.fullPath } }
  const need = to.meta.roles
  if (need && !need.some(r => s.roles.includes(r))) {
    return '/'
  }
  return true
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '校园快递驿站管理系统'
})

/* 发版后旧页面引用已失效的 chunk：捕获动态导入失败，自动刷新一次拿新版本。
   用 sessionStorage 加 10s 时间窗防死循环（持续失败则不再刷新，避免闪烁）。 */
router.onError((err, to) => {
  const msg = String(err?.message || '')
  if (/Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module|Loading chunk .* failed|Loading CSS chunk/i.test(msg)) {
    const last = Number(sessionStorage.getItem('ce_chunk_reload') || 0)
    if (Date.now() - last > 10000) {
      sessionStorage.setItem('ce_chunk_reload', String(Date.now()))
      // resolve().href 会带上部署 base（/campus-express/），避免刷新丢上下文
      window.location.assign(router.resolve(to?.fullPath || '/').href)
    }
  }
})
window.addEventListener('vite:preloadError', (e) => {
  e.preventDefault()
  const last = Number(sessionStorage.getItem('ce_chunk_reload') || 0)
  if (Date.now() - last > 10000) {
    sessionStorage.setItem('ce_chunk_reload', String(Date.now()))
    window.location.reload()
  }
})

export default router