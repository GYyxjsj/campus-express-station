<template>
  <div class="layout" :class="{ mobile: isMobile }">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && drawerOpen" class="mask" @click="drawerOpen = false" />

    <!-- art 风浅色侧边栏 -->
    <aside class="sidebar" :class="{ open: drawerOpen }">
      <div class="logo" @click="router.push('/')">
        <div class="logo-icon"><el-icon :size="20"><Box /></el-icon></div>
        <span class="logo-text">校园快递驿站</span>
      </div>
      <div class="menu-wrap">
        <el-menu :default-active="route.path" :default-openeds="openeds" router
                 class="menu" @select="drawerOpen = false">
          <template v-if="store.isStudent">
            <el-menu-item index="/student/parcels"><el-icon><Box /></el-icon>我的包裹</el-menu-item>
            <el-menu-item index="/student/wallet"><el-icon><Wallet /></el-icon>我的钱包</el-menu-item>
            <el-menu-item index="/student/identity"><el-icon><Postcard /></el-icon>身份条码</el-menu-item>
            <el-sub-menu index="proxy">
              <template #title><el-icon><Bicycle /></el-icon><span>代取服务</span></template>
              <el-menu-item index="/student/proxy-market">服务大厅</el-menu-item>
              <el-menu-item index="/student/proxy-orders">我的服务单</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/student/granted"><el-icon><Promotion /></el-icon>我发起的授权</el-menu-item>
            <el-menu-item index="/student/received"><el-icon><Handbag /></el-icon>受托代取</el-menu-item>
            <el-sub-menu index="ship">
              <template #title><el-icon><Van /></el-icon><span>寄件服务</span></template>
              <el-menu-item index="/student/ship">寄件与运费</el-menu-item>
              <el-menu-item index="/student/insurance">超时保险</el-menu-item>
              <el-menu-item index="/student/addresses">地址簿</el-menu-item>
              <el-menu-item index="/student/contacts">电话簿</el-menu-item>
              <el-menu-item index="/student/preference">收货偏好</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="benefit">
              <template #title><el-icon><Present /></el-icon><span>权益中心</span></template>
              <el-menu-item index="/student/coupons">红包卡券</el-menu-item>
              <el-menu-item index="/student/invite">邀请有礼</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="support">
              <template #title><el-icon><Service /></el-icon><span>帮助支持</span></template>
              <el-menu-item index="/student/tickets">客服工单</el-menu-item>
              <el-menu-item index="/student/feedback">意见反馈</el-menu-item>
              <el-menu-item index="/student/about">关于我们</el-menu-item>
            </el-sub-menu>
          </template>
          <template v-if="store.isStaff">
            <el-sub-menu index="staff">
              <template #title><el-icon><OfficeBuilding /></el-icon><span>驿站工作台</span></template>
              <el-menu-item index="/staff/inbound">入库工作台</el-menu-item>
              <el-menu-item index="/staff/import">批量导入包裹</el-menu-item>
              <el-menu-item index="/staff/pickup">取件核销台</el-menu-item>
              <el-menu-item index="/staff/identity">身份核验台</el-menu-item>
              <el-menu-item index="/staff/parcels">包裹管理</el-menu-item>
              <el-menu-item index="/staff/ship-orders">自寄单管理</el-menu-item>
              <el-menu-item index="/staff/tickets">客服工单台</el-menu-item>
              <el-menu-item index="/staff/traces">去向台账</el-menu-item>
              <el-menu-item index="/staff/overdue">滞留件管理</el-menu-item>
              <el-menu-item index="/staff/shelves">货架查看</el-menu-item>
            </el-sub-menu>
          </template>
          <template v-if="store.isAdmin">
            <el-sub-menu index="admin">
              <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
              <el-menu-item index="/admin/dashboard">运营看板</el-menu-item>
              <el-menu-item index="/admin/users">用户管理</el-menu-item>
              <el-menu-item index="/admin/stations">驿站与货架</el-menu-item>
              <el-menu-item index="/admin/wallet">资金账务</el-menu-item>
              <el-menu-item index="/admin/proxy-orders">代取监管</el-menu-item>
              <el-menu-item index="/admin/policies">协议治理</el-menu-item>
              <el-menu-item index="/admin/configs">策略配置</el-menu-item>
              <el-menu-item index="/admin/notices">公告管理</el-menu-item>
              <el-menu-item index="/admin/ads">广告位管理</el-menu-item>
              <el-menu-item index="/admin/coupons">红包卡券工厂</el-menu-item>
              <el-menu-item index="/admin/feedback">意见反馈处理</el-menu-item>
              <el-menu-item index="/admin/logs">操作日志</el-menu-item>
              <el-menu-item index="/admin/settings">系统设置</el-menu-item>
            </el-sub-menu>
          </template>
          <el-menu-item index="/notifications"><el-icon><Bell /></el-icon>{{ store.isStudent ? '提醒与好友' : '提醒中心' }}</el-menu-item>
          <el-menu-item v-if="store.isStudent" index="/student/chat"><el-icon><ChatDotRound /></el-icon>消息聊天</el-menu-item>
          <el-menu-item index="/notices"><el-icon><ChatLineSquare /></el-icon>驿站公告</el-menu-item>
          <el-menu-item index="/policies"><el-icon><Document /></el-icon>政策协议</el-menu-item>
          <el-menu-item index="/profile"><el-icon><User /></el-icon>个人中心</el-menu-item>
        </el-menu>
      </div>
    </aside>

    <!-- 右侧主区 -->
    <div class="main-wrap">
      <!-- 全局氛围背景：色雾光斑 + 蓝图网格 + 半调点阵（不拦截交互） -->
      <div class="ambient-field" aria-hidden="true"><i /><i /><i /></div>
      <header class="header">
        <div class="left">
          <button v-if="isMobile" class="icon-btn" @click="drawerOpen = true" aria-label="菜单">
            <el-icon :size="18"><Fold /></el-icon>
          </button>
          <el-breadcrumb v-else separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="groupTitle">{{ groupTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '' }}</el-breadcrumb-item>
          </el-breadcrumb>
          <span v-if="isMobile" class="mobile-title">{{ route.meta.title || '校园快递驿站' }}</span>
        </div>
        <div class="right">
          <button class="icon-btn" @click="theme.toggle()" :aria-label="theme.mode === 'dark' ? '切换亮色' : '切换暗色'">
            <el-icon :size="17"><Sunny v-if="theme.mode === 'dark'" /><Moon v-else /></el-icon>
          </button>
          <button class="icon-btn bell-btn" @click="router.push('/notifications')" aria-label="提醒中心">
            <el-icon :size="17"><Bell /></el-icon>
            <span v-if="store.unread" class="bell-badge">{{ store.unread > 99 ? '99+' : store.unread }}</span>
          </button>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="30" class="avatar">{{ (store.user?.realName || 'U').slice(0, 1) }}</el-avatar>
              <span v-if="!isMobile" class="uname">{{ store.user?.realName }}</span>
              <el-tag v-if="!isMobile" size="small" effect="light" round>{{ roleLabel }}</el-tag>
              <el-icon v-if="!isMobile" class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="theme"><el-icon><Moon /></el-icon>{{ theme.mode === 'dark' ? '亮色模式' : '暗色模式' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="main" ref="mainRef">
        <router-view v-slot="{ Component }">
          <transition name="slide-left" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 移动端底部 TabBar（按角色切换，中间为凸起主操作） -->
    <nav v-if="isMobile" class="tabbar" :class="{ 'has-fab': !!fabTab }">
      <template v-for="t in tabs" :key="t.path">
        <button v-if="t.fab" class="tab fab" :class="{ active: route.path === t.path }"
                @click="router.push(t.path)">
          <span class="fab-ring"><el-icon :size="22"><component :is="t.icon" /></el-icon></span>
          <span class="fab-label">{{ t.label }}</span>
        </button>
        <button v-else class="tab" :class="{ active: route.path === t.path }" @click="router.push(t.path)">
          <el-icon :size="19"><component :is="t.icon" /></el-icon>
          <span>{{ t.label }}</span>
          <span v-if="t.path === '/notifications' && store.unread" class="tab-dot" />
        </button>
      </template>
    </nav>

    <!-- AI 助手悬浮入口（置顶按钮上方，远离边缘） -->
    <button class="ai-fab" :class="{ mobile: isMobile }" @click="aiOpen = true" aria-label="AI 快递助手">
      <el-icon :size="20"><ChatDotRound /></el-icon>
    </button>
    <AiAssistant v-model="aiOpen" />

    <!-- 回到顶部悬浮 -->
    <transition name="fade-up">
      <button v-show="showTop" class="back-top" :class="{ mobile: isMobile }" @click="toTop" aria-label="回到顶部">
        <el-icon :size="20"><Top /></el-icon>
      </button>
    </transition>

    <!-- 登录后：必签协议门禁 + 公告弹窗 -->
    <EntryGate />
  </div>
</template><script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useThemeStore } from '../stores/theme'
import EntryGate from '../components/EntryGate.vue'
import AiAssistant from '../components/AiAssistant.vue'
import { unreadCount } from '../api'
import { installParallax } from '../utils/parallax'

const store = useUserStore()
const theme = useThemeStore()
const route = useRoute()
const router = useRouter()
const aiOpen = ref(false)

const roleLabel = computed(() =>
  store.isAdmin ? '管理员' : store.isStaff ? '驿站员工' : '学生')

const groupTitle = computed(() => {
  const p = route.path
  if (p.startsWith('/student/')) return '学生服务'
  if (p.startsWith('/staff/')) return '驿站工作台'
  if (p.startsWith('/admin/')) return '系统管理'
  return ''
})

const openeds = ['staff', 'admin', 'proxy', 'ship', 'benefit', 'support']

// ---- 移动端响应式 ----
const isMobile = ref(window.innerWidth <= 768)
const drawerOpen = ref(false)
const mainRef = ref(null)
const showTop = ref(false)
function onResize() {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) drawerOpen.value = false
}
function onMainScroll() {
  showTop.value = !!mainRef.value && mainRef.value.scrollTop > 300
}
function toTop() {
  mainRef.value && mainRef.value.scrollTo({ top: 0, behavior: 'smooth' })
}
watch(() => route.path, () => {
  drawerOpen.value = false
  showTop.value = false
  mainRef.value && mainRef.value.scrollTo({ top: 0 })
})

const STUDENT_TABS = [
  { path: '/student/parcels', icon: 'Box', label: '包裹' },
  { path: '/student/wallet', icon: 'Wallet', label: '钱包' },
  { path: '/student/identity', icon: 'Postcard', label: '身份码', fab: true },
  { path: '/student/proxy-market', icon: 'Bicycle', label: '代取' },
  { path: '/profile', icon: 'User', label: '我的' }
]
const STAFF_TABS = [
  { path: '/staff/inbound', icon: 'Download', label: '入库' },
  { path: '/staff/pickup', icon: 'Upload', label: '核销' },
  { path: '/staff/identity', icon: 'Aim', label: '核验', fab: true },
  { path: '/staff/traces', icon: 'Guide', label: '台账' },
  { path: '/staff/parcels', icon: 'Box', label: '包裹' }
]
const ADMIN_TABS = [
  { path: '/admin/dashboard', icon: 'DataAnalysis', label: '看板' },
  { path: '/admin/wallet', icon: 'Coin', label: '账务' },
  { path: '/admin/proxy-orders', icon: 'Bicycle', label: '代取', fab: true },
  { path: '/admin/policies', icon: 'Document', label: '协议' },
  { path: '/admin/users', icon: 'User', label: '用户' }
]
const tabs = computed(() =>
  store.isAdmin ? ADMIN_TABS : store.isStaff ? STAFF_TABS : STUDENT_TABS)
const fabTab = computed(() => tabs.value.find(t => t.fab))

let timer = null
let parallaxCleanup = null
let scrollEl = null
async function pollUnread() {
  if (!store.logged || document.hidden) return
  try { store.setUnread(await unreadCount()) } catch (e) { /* 忽略轮询错误 */ }
}

function onCommand(cmd) {
  if (cmd === 'profile') return router.push('/profile')
  if (cmd === 'theme') return theme.toggle()
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
    store.logout()
    router.push('/login')
  }).catch(() => {})
}

onMounted(() => {
  pollUnread()
  timer = setInterval(pollUnread, 30000)
  window.addEventListener('resize', onResize)
  scrollEl = mainRef.value
  parallaxCleanup = installParallax(scrollEl)
  if (scrollEl) scrollEl.addEventListener('scroll', onMainScroll, { passive: true })
})
onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  if (parallaxCleanup) parallaxCleanup()
  if (scrollEl) scrollEl.removeEventListener('scroll', onMainScroll)
})
</script><style scoped>
.layout { display: flex; height: 100vh; height: 100dvh; background: var(--default-bg-color); }

/* ---- 侧边栏：art DESIGN 风浅色 ---- */
.sidebar {
  width: 230px;
  flex-shrink: 0;
  background: var(--default-box-color);
  border-right: 1px solid var(--art-card-border);
  display: flex;
  flex-direction: column;
  z-index: 100;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  cursor: pointer;
  flex-shrink: 0;
}
.logo-icon {
  width: 34px; height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #5d87ff, #8eabff);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 10px rgba(93, 135, 255, 0.35);
}
.logo-text { font-size: 16px; font-weight: 600; color: var(--art-gray-900); letter-spacing: 0.5px; }
.menu-wrap { flex: 1; overflow-y: auto; padding: 4px 12px 16px; }

.menu { border-right: none; --el-menu-bg-color: transparent; }
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  border-radius: calc(var(--custom-radius) / 3 + 4px);
  color: var(--art-gray-700);
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: var(--art-hover-color);
  color: var(--art-gray-900);
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
.menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px; height: 20px;
  border-radius: 4px;
  background: var(--el-color-primary);
}
.menu :deep(.el-menu-item) { position: relative; }
.menu :deep(.el-sub-menu .el-menu-item) { padding-left: 50px !important; min-width: auto; }
.menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) { color: var(--art-gray-900); font-weight: 600; }

/* ---- 主区 ---- */
.main-wrap { flex: 1; display: flex; flex-direction: column; min-width: 0; position: relative; }
.header {
  height: 60px;
  flex-shrink: 0;
  position: relative; z-index: 2;
  background: var(--default-box-color);
  border-bottom: 1px solid var(--art-card-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  gap: 12px;
}
.left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.icon-btn {
  width: 36px; height: 36px;
  border: 1.5px solid transparent;
  background: transparent;
  border-radius: 10px;
  color: var(--art-gray-700);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, transform 0.15s cubic-bezier(0.23, 1, 0.32, 1);
  position: relative;
}
.icon-btn:hover { background: var(--art-hover-color); border-color: var(--default-border); color: var(--art-gray-900); }
.icon-btn:active { transform: translateY(1px); box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.12); }

.bell-badge {
  position: absolute;
  top: 2px; right: 0;
  min-width: 16px; height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #fd4e4e;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
}

.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 6px; border-radius: 10px; outline: none; }
.user-chip:hover { background: var(--art-hover-color); }
.avatar { background: linear-gradient(135deg, #5d87ff, #8eabff); color: #fff; font-weight: 600; }
.uname { font-size: 14px; color: var(--art-gray-800); font-weight: 500; }
.arrow { color: var(--art-gray-500); font-size: 12px; }

.main { flex: 1; overflow-y: auto; overflow-x: hidden; position: relative; z-index: 1; }

.mobile-title { font-size: 16px; font-weight: 600; color: var(--art-gray-900); }

/* ---- 移动端 ---- */
.mask {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 99;
}
.layout.mobile .sidebar {
  position: fixed;
  left: 0; top: 0; bottom: 0;
  transform: translateX(-100%);
  transition: transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: none;
}
.layout.mobile .sidebar.open {
  transform: translateX(0);
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.2);
}
.layout.mobile .header { padding: 0 12px; }

/* ---- 底部 TabBar ---- */
.tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  height: 58px;
  padding-bottom: env(safe-area-inset-bottom);
  background: color-mix(in srgb, var(--default-box-color) 92%, transparent);
  backdrop-filter: blur(14px) saturate(1.4);
  border-top: 1px solid var(--art-card-border);
  display: flex;
  z-index: 98;
}
.tab {
  flex: 1;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 10.5px;
  color: var(--art-gray-600);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}
.tab.active { color: var(--el-color-primary); font-weight: 700; }
.tab.active::after {
  content: '';
  position: absolute; top: 0; left: 50%; transform: translateX(-50%);
  width: 22px; height: 3px; border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--flair-primary), var(--flair-violet));
}
.tab-dot {
  position: absolute;
  top: 7px;
  left: calc(50% + 9px);
  width: 7px; height: 7px;
  border-radius: 50%;
  background: #ff3860;
  animation: art-breathe 1.5s infinite;
}

/* 凸起主操作 */
.tabbar.has-fab { height: 62px; }
.tab.fab { justify-content: flex-end; padding-bottom: 5px; overflow: visible; }
.tab.fab.active::after { display: none; }
.fab-ring {
  position: absolute;
  top: -20px; left: 50%; transform: translateX(-50%);
  width: 50px; height: 50px; border-radius: 17px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--flair-primary), var(--flair-violet));
  border: 3px solid var(--default-box-color);
  box-shadow: 0 10px 22px -8px color-mix(in srgb, var(--flair-primary) 75%, transparent);
  transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.25s;
}
.tab.fab:active .fab-ring { transform: translateX(-50%) scale(0.93); }
.tab.fab.active .fab-ring {
  background: linear-gradient(135deg, #10d9b5, var(--flair-primary));
  box-shadow: 0 12px 26px -8px rgba(16, 217, 181, 0.8);
}
.fab-label { font-size: 10px; font-weight: 700; }

/* ---- 回到顶部悬浮（内缩远离屏幕边缘） ---- */
/* ---- 悬浮按钮统一规范：同尺寸/圆角/阴影，整体抬高远离下边缘 ---- */
.back-top, .ai-fab {
  position: fixed;
  right: 52px;
  width: 46px; height: 46px;
  border: none; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--flair-primary), var(--flair-violet));
  box-shadow: 0 12px 26px -10px color-mix(in srgb, var(--flair-primary) 80%, transparent), inset 0 1px 0 rgba(255, 255, 255, 0.3);
  z-index: 95;
  transition: opacity 0.25s var(--flair-ease), transform 0.22s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.22s, filter 0.22s;
}
.back-top { bottom: 72px; opacity: 0.45; }
.back-top:hover { opacity: 1; transform: translateY(-3px); filter: saturate(1.15); box-shadow: 0 16px 32px -10px color-mix(in srgb, var(--flair-primary) 85%, transparent); }
.back-top:active { transform: translateY(0) scale(0.94); }
.back-top.mobile { right: 28px; bottom: calc(112px + env(safe-area-inset-bottom)); width: 42px; height: 42px; }

/* AI 助手悬浮入口：位于置顶按钮正上方，样式完全一致 */
.ai-fab { bottom: 130px; }
.ai-fab:hover {
  transform: translateY(-3px);
  filter: saturate(1.15);
  box-shadow: 0 16px 32px -10px color-mix(in srgb, var(--flair-primary) 85%, transparent);
}
.ai-fab:active { transform: translateY(0) scale(0.94); }
.ai-fab.mobile { right: 28px; bottom: calc(166px + env(safe-area-inset-bottom)); width: 42px; height: 42px; }
.fade-up-enter-active, .fade-up-leave-active { transition: opacity 0.28s var(--flair-ease), transform 0.28s var(--flair-ease); }
.fade-up-enter-from, .fade-up-leave-to { opacity: 0; transform: translateY(12px) scale(0.9); }
</style>