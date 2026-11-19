<template>
  <div class="ap">
    <PageHero tone="notice" icon="Message" chip="NOTIFY INBOX" live title="提醒中心"
      sub="到站、催领、滞留、代取与钱包动账的全渠道消息存档。点击消息自动标记已读，关联包裹可直接展开完整去向轨迹。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" plain :icon="Check" :disabled="!store.unread" @click="doReadAll">全部已读</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="消息列表" :note="`${total} 条`" icon="ChatDotSquare" accent="notice" :index="4">
      <template v-if="store.isStudent" #extra>
        <el-button size="small" text type="primary" :icon="UserFilled" @click="scrollToFriends">我的好友<template v-if="incoming.length">（{{ incoming.length }} 条申请）</template></el-button>
      </template>
      <div class="ap-seg read-seg">
        <button :class="{ on: !unreadOnly }" @click="unreadOnly = false; load(1)">全部消息<span class="n">{{ total }}</span></button>
        <button :class="{ on: unreadOnly }" @click="unreadOnly = true; load(1)">只看未读<span class="n">{{ store.unread }}</span></button>
      </div>

      <div v-loading="loading" class="notify-list">
        <article v-for="(n, i) in rows" :key="n.id" class="notify-item flair-in" :style="{ '--i': Math.min(i, 10) }"
                 :class="{ unread: !n.isRead }" @click="open(n)">
          <div class="n-avatar" :style="{ background: typeBg(n.type) }">
            <el-icon :size="17"><component :is="typeIcon(n.type)" /></el-icon>
          </div>
          <div class="n-body">
            <div class="n-l1">
              <el-tag :type="dict(NOTIFY_TYPE, n.type).type" size="small" effect="light">{{ dict(NOTIFY_TYPE, n.type).label }}</el-tag>
              <b>{{ n.title }}</b>
              <span v-if="!n.isRead" class="dot" />
            </div>
            <p class="n-content">{{ n.content }}</p>
            <div class="n-time ap-mono">{{ fmtTime(n.sentAt) }}<span v-if="n.parcelId" class="n-link">查看包裹去向<el-icon :size="12"><ArrowRight /></el-icon></span></div>
          </div>
        </article>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty :description="unreadOnly ? '没有未读消息' : '暂无提醒消息'" :image-size="86" />
        <p>{{ unreadOnly ? '所有消息都已处理，保持得不错。' : '包裹入库、催领与代取动态会第一时间出现在这里。' }}</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>

    <ParcelDetail v-model="detailVisible" :parcel-id="detailId" />

    <!-- 好友（学生社区功能，与提醒中心合并） -->
    <div v-if="store.isStudent" ref="friendsAnchor">
      <PanelCard title="我的好友" :note="`${friends.length} 位 · 申请 ${incoming.length} 条待处理`" icon="UserFilled" accent="identity" :index="5">
        <template #extra>
          <el-button type="primary" size="small" :icon="Plus" @click="friendFormVisible = true">添加好友</el-button>
        </template>

        <div v-if="incoming.length" class="fr-req">
          <div v-for="r in incoming" :key="r.id" class="fr-req-item">
            <div class="fr-req-info">
              <b>{{ r.realName || r.username }}</b>
              <span class="ap-mono">@{{ r.username }}</span>
              <p v-if="r.remark">「{{ r.remark }}」</p>
            </div>
            <span class="fr-req-time ap-mono">{{ fmtTime(r.createdAt) }}</span>
            <el-button size="small" type="success" @click="respond(r, true)">接受</el-button>
            <el-button size="small" type="danger" plain @click="respond(r, false)">拒绝</el-button>
          </div>
        </div>

        <div v-loading="friendLoading" class="fr-grid">
          <article v-for="(f, i) in friends" :key="f.id" class="fr-item flair-in" :style="{ '--i': Math.min(i, 10) }">
            <div class="fr-avatar">{{ (f.realName || f.username || '?').slice(0, 1) }}</div>
            <div class="fr-main">
              <div class="fr-l1"><b>{{ f.realName || f.username }}</b><span class="ap-mono">@{{ f.username }}</span></div>
              <div class="fr-meta ap-mono">{{ f.phone || '手机号未公开' }} · {{ fmtTime(f.createdAt) }} 成为好友</div>
              <div v-if="f.remark" class="fr-remark">备注：{{ f.remark }}</div>
            </div>
            <el-tooltip content="删除好友" placement="top">
              <el-button size="small" circle type="danger" plain :icon="Delete" @click="removeFriendRow(f)" />
            </el-tooltip>
          </article>
        </div>

        <div v-if="!friends.length && !friendLoading" class="ap-empty">
          <el-empty description="还没有好友" :image-size="80" />
          <p>添加同学为好友后，可互相发起代取委托，取件更灵活。</p>
        </div>

        <div v-if="outgoing.length" class="fr-out">
          <span>等待对方确认：</span>
          <el-tag v-for="o in outgoing" :key="o.id" size="small" effect="plain" type="warning">
            {{ o.realName || o.username }} · {{ fmtTime(o.createdAt) }}
          </el-tag>
        </div>
      </PanelCard>

      <el-dialog v-model="friendFormVisible" title="添加好友" width="440px" append-to-body>
        <div class="ap-form">
          <div class="ap-field-row">
            <label>对方学号 / 用户名 / 手机号<em>*</em></label>
            <el-input v-model="friendForm.keyword" placeholder="输入对方的用户名或手机号" maxlength="32" @keyup.enter="sendRequest" />
          </div>
          <div class="ap-field-row">
            <label>申请备注</label>
            <el-input v-model="friendForm.remark" placeholder="如：同班同学，方便互相代取" maxlength="64" />
            <div class="ap-form-tip">对方接受后即成为好友，申请进度可在本页跟踪。</div>
          </div>
        </div>
        <template #footer>
          <el-button @click="friendFormVisible = false">取消</el-button>
          <el-button type="primary" :icon="Promotion" :loading="friendSending" :disabled="!friendForm.keyword.trim()" @click="sendRequest">发送申请</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight, Check, Delete, Message, Plus, Promotion, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  myNotifications, readNotification, readAllNotifications, unreadCount,
  myFriends, friendRequests, sendFriendRequest, respondFriendRequest, removeFriend
} from '../api'
import { useUserStore } from '../stores/user'
import { fmtTime, dict, NOTIFY_TYPE } from '../utils/format'
import PageHero from '../components/PageHero.vue'
import MetricTile from '../components/MetricTile.vue'
import PanelCard from '../components/PanelCard.vue'
import ParcelDetail from '../components/ParcelDetail.vue'

const store = useUserStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const unreadOnly = ref(false)
const query = reactive({ page: 1, size: 10 })
const detailVisible = ref(false)
const detailId = ref(null)

const TYPE_STYLE = {
  ARRIVE: { icon: 'Box', bg: 'linear-gradient(140deg,#3b82f6,#38bdf8)' },
  REMIND: { icon: 'Bell', bg: 'linear-gradient(140deg,#f59e0b,#fbbf24)' },
  OVERDUE: { icon: 'AlarmClock', bg: 'linear-gradient(140deg,#e11d48,#fb7185)' },
  PROXY_GRANTED: { icon: 'Share', bg: 'linear-gradient(140deg,#0d9488,#34d399)' },
  PROXY_USED: { icon: 'CircleCheck', bg: 'linear-gradient(140deg,#0d9488,#34d399)' },
  NOTICE: { icon: 'ChatDotSquare', bg: 'linear-gradient(140deg,#64748b,#94a3b8)' },
  WALLET_RECHARGE: { icon: 'Wallet', bg: 'linear-gradient(140deg,#f59e0b,#fbbf24)' },
  PROXY_ORDER: { icon: 'Handbag', bg: 'linear-gradient(140deg,#8b5cf6,#a78bfa)' },
  INSPECT: { icon: 'View', bg: 'linear-gradient(140deg,#f59e0b,#fbbf24)' },
  POLICY_UPDATE: { icon: 'Document', bg: 'linear-gradient(140deg,#64748b,#94a3b8)' }
}
const typeIcon = t => TYPE_STYLE[t]?.icon || 'Message'
const typeBg = t => TYPE_STYLE[t]?.bg || 'linear-gradient(140deg,#64748b,#94a3b8)'

const pageUnread = computed(() => rows.value.filter(n => !n.isRead).length)
const heroStats = computed(() => [
  { label: '未读', value: store.unread, unit: '条' },
  { label: '消息总数', value: total.value, unit: '条' },
  { label: '本页未读', value: pageUnread.value, unit: '条' }
])
const metrics = computed(() => [
  { label: '未读消息', value: store.unread, unit: '条', icon: 'Bell', color: '#e11d48', foot: '点击消息即标记已读' },
  { label: '消息总数', value: total.value, unit: '条', icon: 'Message', color: '#5b7cfa', foot: '当前筛选命中' },
  { label: '本页未读', value: pageUnread.value, unit: '条', icon: 'ChatDotSquare', color: '#f59e0b', foot: '本页待处理' },
  { label: '关联包裹', value: rows.value.filter(n => n.parcelId).length, unit: '条', icon: 'Box', color: '#10d9b5', foot: '可展开去向轨迹' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myNotifications({ ...query, unreadOnly: unreadOnly.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function refreshUnread() {
  store.setUnread(await unreadCount())
}

async function open(n) {
  if (!n.isRead) {
    await readNotification(n.id)
    n.isRead = 1
    refreshUnread()
  }
  if (n.parcelId) { detailId.value = n.parcelId; detailVisible.value = true }
}

async function doReadAll() {
  await readAllNotifications()
  await refreshUnread()
  load()
}

// ---- 好友（学生） ----
const friends = ref([])
const incoming = ref([])
const outgoing = ref([])
const friendLoading = ref(false)
const friendFormVisible = ref(false)
const friendSending = ref(false)
const friendForm = reactive({ keyword: '', remark: '' })

async function loadFriends() {
  friendLoading.value = true
  try {
    const [list, reqs] = await Promise.all([myFriends(), friendRequests()])
    friends.value = list || []
    incoming.value = reqs.incoming || []
    outgoing.value = reqs.outgoing || []
  } finally { friendLoading.value = false }
}

async function respond(r, accept) {
  await respondFriendRequest(r.id, accept)
  ElMessage.success(accept ? '已接受好友申请' : '已拒绝该申请')
  await loadFriends()
  refreshUnread()
}

async function removeFriendRow(f) {
  await ElMessageBox.confirm(
    `确定删除好友「${f.realName || f.username}」吗？删除后需重新申请才能互相委托代取。`,
    '删除好友', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await removeFriend(f.id)
  ElMessage.success('已删除好友')
  loadFriends()
}

async function sendRequest() {
  const keyword = friendForm.keyword.trim()
  if (!keyword) return
  friendSending.value = true
  try {
    await sendFriendRequest({ keyword, remark: friendForm.remark.trim() || undefined })
    ElMessage.success('申请已发送，等待对方确认')
    friendFormVisible.value = false
    friendForm.keyword = ''
    friendForm.remark = ''
    loadFriends()
  } finally { friendSending.value = false }
}

onMounted(async () => {
  load(); refreshUnread()
  if (store.isStudent) {
    await loadFriends()
    if (route.query.tab === 'friends') scrollToFriends()
  }
})

/* 好友面板锚点：消息列表右上角快捷入口 / ?tab=friends 直达 */
const route = useRoute()
const friendsAnchor = ref(null)
async function scrollToFriends() {
  await nextTick()
  friendsAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.read-seg { margin-bottom: 14px; }
.notify-list { display: flex; flex-direction: column; gap: 10px; }
.notify-item {
  display: flex; gap: 12px; padding: 13px 15px; cursor: pointer;
  border: 1px solid var(--default-border); border-radius: 14px; background: var(--art-card-bg, #fff);
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s, border-color 0.22s;
}
html.dark .notify-item { background: #17171c; }
.notify-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.notify-item.unread { border-left: 3px solid #f59e0b; background: linear-gradient(120deg, rgba(245, 158, 11, 0.07), transparent 55%); }
html.dark .notify-item.unread { background: linear-gradient(120deg, rgba(245, 158, 11, 0.12), #17171c 55%); }
.n-avatar {
  width: 38px; height: 38px; border-radius: 12px; flex: none;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.n-body { min-width: 0; flex: 1; }
.n-l1 { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.n-l1 b { font-size: 13.5px; color: var(--art-gray-900); }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #e11d48; box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.18); }
.n-content { margin: 5px 0 6px; font-size: 12.5px; color: var(--art-gray-600); line-height: 1.65; }
.n-time { display: flex; align-items: center; gap: 12px; font-size: 11.5px; color: var(--art-gray-500); flex-wrap: wrap; }
.n-link { display: inline-flex; align-items: center; gap: 3px; color: #0d9488; font-weight: 700; }
html.dark .n-link { color: #34d399; }

/* 好友段 */
.fr-req { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
.fr-req-item {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; border-radius: 12px;
  border: 1px dashed rgba(245, 158, 11, 0.55);
  background: linear-gradient(120deg, rgba(245, 158, 11, 0.06), transparent 60%);
}
html.dark .fr-req-item { background: linear-gradient(120deg, rgba(245, 158, 11, 0.12), #17171c 60%); }
.fr-req-info { flex: 1; min-width: 180px; display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.fr-req-info b { font-size: 13px; color: var(--art-gray-900); }
.fr-req-info span { font-size: 11.5px; color: var(--art-gray-500); }
.fr-req-info p { margin: 0; width: 100%; font-size: 12px; color: var(--art-gray-600); }
.fr-req-time { font-size: 11.5px; color: var(--art-gray-500); }

.fr-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.fr-item {
  display: flex; align-items: center; gap: 12px; padding: 13px 15px;
  border: 1px solid var(--default-border); border-radius: 14px; background: var(--art-card-bg, #fff);
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s;
}
html.dark .fr-item { background: #17171c; }
.fr-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.fr-avatar {
  width: 40px; height: 40px; border-radius: 13px; flex: none;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 800; color: #fff;
  background: linear-gradient(140deg, #6366f1, #8b5cf6);
}
.fr-main { flex: 1; min-width: 0; }
.fr-l1 { display: flex; align-items: baseline; gap: 7px; flex-wrap: wrap; }
.fr-l1 b { font-size: 13.5px; color: var(--art-gray-900); }
.fr-l1 span { font-size: 11.5px; color: var(--art-gray-500); }
.fr-meta { margin-top: 3px; font-size: 11.5px; color: var(--art-gray-500); }
.fr-remark { margin-top: 4px; font-size: 12px; color: var(--art-gray-600); }
.fr-out { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 14px; font-size: 12px; color: var(--art-gray-500); }

@media (max-width: 640px) {
  .fr-grid { grid-template-columns: 1fr; }
}
</style>