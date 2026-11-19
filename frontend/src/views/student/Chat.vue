<template>
  <div class="chat-page flair-in">
    <div class="chat-shell" :class="{ 'show-panel': mobilePanel }">
      <!-- 左栏：会话列表 -->
      <aside class="chat-side">
        <div class="side-head">
          <b>消息</b>
          <span v-if="totalUnread" class="side-total">{{ totalUnread }} 条未读</span>
        </div>
        <div class="side-search">
          <el-input v-model="keyword" placeholder="搜索好友" clearable :prefix-icon="Search" size="default" />
        </div>
        <div class="side-list" ref="listRef">
          <!-- AI 助手固定会话 -->
          <button class="conv ai" :class="{ active: activeKey === 'ai' }" @click="openConv('ai')">
            <span class="conv-avatar ai-avatar"><el-icon :size="17"><ChatDotRound /></el-icon></span>
            <span class="conv-main">
              <span class="conv-line1"><b>驿站 AI 助手</b><i class="conv-time">在线</i></span>
              <span class="conv-line2">快递 / 驿站业务智能问答</span>
            </span>
          </button>
          <div class="side-divider">好友会话</div>
          <button v-for="c in filteredConvs" :key="c.peerId" class="conv"
                  :class="{ active: activeKey === String(c.peerId) }" @click="openConv(c.peerId)">
            <span class="conv-avatar" :style="{ background: avatarBg(c.realName) }">{{ (c.realName || '?').slice(0, 1) }}</span>
            <span class="conv-main">
              <span class="conv-line1">
                <b>{{ c.remark || c.realName }}</b>
                <i class="conv-time">{{ shortTime(c.lastTime) }}</i>
              </span>
              <span class="conv-line2">
                <em v-if="c.lastMine" class="mine-mark">我：</em>{{ c.lastContent || '还没有消息，打个招呼吧' }}
              </span>
            </span>
            <span v-if="c.unread > 0" class="conv-badge">{{ c.unread > 99 ? '99+' : c.unread }}</span>
          </button>
          <div v-if="!filteredConvs.length" class="side-empty">
            <el-icon :size="26"><User /></el-icon>
            <p>{{ keyword ? '没有匹配的好友' : '还没有好友会话' }}</p>
            <el-button size="small" text type="primary" @click="$router.push('/notifications?tab=friends')">去添加好友</el-button>
          </div>
        </div>
      </aside>

      <!-- 右栏：消息区 -->
      <section class="chat-main">
        <template v-if="activeKey">
          <header class="main-head">
            <button class="head-back" @click="mobilePanel = false" aria-label="返回会话列表"><el-icon><ArrowLeft /></el-icon></button>
            <span class="head-avatar" :class="{ 'ai-avatar': isAi }"
                  :style="isAi ? {} : { background: avatarBg(activeName) }">
              <el-icon v-if="isAi" :size="17"><ChatDotRound /></el-icon>
              <template v-else>{{ (activeName || '?').slice(0, 1) }}</template>
            </span>
            <div class="head-info">
              <b>{{ isAi ? '驿站 AI 助手' : activeName }}</b>
              <span>{{ headSub }}</span>
            </div>
            <div class="head-actions">
              <el-tooltip v-if="isAi" content="清空对话" placement="bottom">
                <button class="head-btn" @click="aiMessages = []" aria-label="清空对话"><el-icon><Delete /></el-icon></button>
              </el-tooltip>
              <el-tooltip v-else content="刷新消息" placement="bottom">
                <button class="head-btn" @click="loadMessages(activePeer)" aria-label="刷新消息"><el-icon><Refresh /></el-icon></button>
              </el-tooltip>
              <el-tooltip v-if="!isAi" content="发起代取授权" placement="bottom">
                <button class="head-btn" @click="$router.push('/student/granted')" aria-label="发起代取授权"><el-icon><Promotion /></el-icon></button>
              </el-tooltip>
            </div>
          </header>

          <div class="main-body" ref="bodyRef">
            <!-- AI 会话 -->
            <template v-if="isAi">
              <div v-if="!aiMessages.length" class="msg-empty">
                <div class="empty-icon"><el-icon :size="30"><MagicStick /></el-icon></div>
                <p class="empty-title">快递问题，问我更快</p>
                <p class="empty-sub">包裹、代取、运费、保险等驿站问题都可以问</p>
                <button v-for="q in presets" :key="q" class="preset" @click="sendAi(q)">{{ q }}</button>
              </div>
              <div v-for="(m, i) in aiMessages" :key="'a' + i" class="msg" :class="{ mine: m.role === 'user' }">
                <span class="msg-avatar" :class="{ 'ai-avatar': m.role !== 'user' }">
                  <el-icon v-if="m.role !== 'user'" :size="14"><ChatDotRound /></el-icon>
                  <template v-else>{{ meInitial }}</template>
                </span>
                <div class="msg-col">
                  <div class="msg-meta">{{ m.role === 'user' ? meName : 'AI 助手' }}</div>
                  <div class="msg-bubble">
                    <span v-if="m.pending" class="typing"><i /><i /><i /></span>
                    <template v-else>{{ m.content }}</template>
                  </div>
                </div>
              </div>
            </template>
            <!-- 好友会话 -->
            <template v-else>
              <div v-if="!messages.length && !loadingMsgs" class="msg-empty">
                <div class="empty-icon"><el-icon :size="30"><ChatLineSquare /></el-icon></div>
                <p class="empty-title">和 {{ activeName }} 打个招呼</p>
                <p class="empty-sub">聊聊代取、约取件时间，消息仅双方可见</p>
              </div>
              <div v-for="m in messages" :key="m.id" class="msg" :class="{ mine: m.senderId === store.user?.id }">
                <span class="msg-avatar" :style="{ background: m.senderId === store.user?.id ? '' : avatarBg(activeName) }">
                  {{ (m.senderId === store.user?.id ? meName : activeName).slice(0, 1) }}
                </span>
                <div class="msg-col">
                  <div class="msg-meta">{{ m.senderId === store.user?.id ? meName : activeName }} · {{ fmtTime(m.createdAt) }}</div>
                  <div class="msg-bubble">{{ m.content }}</div>
                </div>
              </div>
            </template>
          </div>          <footer class="main-foot">
            <el-input v-model="draft" type="textarea" :rows="2" resize="none"
                      :maxlength="isAi ? 300 : 500"
                      :placeholder="isAi ? '输入快递相关问题，Enter 发送' : '输入消息，Enter 发送，Shift+Enter 换行'"
                      @keydown.enter.exact.prevent="sendCurrent()" />
            <div class="foot-bar">
              <span class="foot-note">{{ isAi ? '回复由 AI 生成，仅供参考' : '仅好友之间可互发消息 · 单条最多 500 字' }}</span>
              <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="sendCurrent()">
                发送<el-icon class="send-icon"><Promotion /></el-icon>
              </el-button>
            </div>
          </footer>
        </template>
        <div v-else class="main-placeholder">
          <div class="ph-icon"><el-icon :size="38"><ChatDotRound /></el-icon></div>
          <p>选择左侧会话开始聊天</p>
          <span>AI 助手随时在线，好友消息实时送达</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { chatConversations, chatMessages, chatSend, aiChat, aiStatus } from '../../api'
import { useUserStore } from '../../stores/user'
import { fmtTime } from '../../utils/format'

const route = useRoute()
const store = useUserStore()
const meName = computed(() => store.user?.realName || '我')
const meInitial = computed(() => meName.value.slice(0, 1))

const convs = ref([])
const keyword = ref('')
const activeKey = ref('')            // 'ai' 或好友 peerId 字符串
const mobilePanel = ref(false)       // 移动端：是否展示右侧消息区
const messages = ref([])
const aiMessages = ref([])
const draft = ref('')
const sending = ref(false)
const loadingMsgs = ref(false)
const aiOnline = ref(null)
const bodyRef = ref(null)
let pollTimer = null

const presets = [
  '我的包裹到站多久了？',
  '怎么授权同学帮我代取？',
  '包裹超时了会怎么样？',
  '驿站自寄怎么收费？'
]

const isAi = computed(() => activeKey.value === 'ai')
const activePeer = computed(() => (isAi.value ? null : Number(activeKey.value)))
const activeConv = computed(() => convs.value.find(c => String(c.peerId) === activeKey.value))
const activeName = computed(() => activeConv.value?.remark || activeConv.value?.realName || '好友')
const totalUnread = computed(() => convs.value.reduce((s, c) => s + (c.unread || 0), 0))
const headSub = computed(() => {
  if (isAi.value) return aiOnline.value ? 'AI 在线 · 仅限快递业务咨询' : '规则应答模式 · 仅限快递业务咨询'
  const c = activeConv.value
  return c ? `${c.realName} · 学号 ${c.username}` : ''
})
const filteredConvs = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return convs.value
  return convs.value.filter(c =>
    (c.realName || '').toLowerCase().includes(k) ||
    (c.username || '').toLowerCase().includes(k) ||
    (c.remark || '').toLowerCase().includes(k))
})

/* 稳定的头像底色（按姓名 hash 取色板） */
const palette = ['#5b7cfa', '#8b5cf6', '#0d9488', '#f59e0b', '#fb7185', '#38bdf8']
function avatarBg(name) {
  let h = 0
  for (const ch of String(name || '?')) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return `linear-gradient(135deg, ${palette[h % palette.length]}, color-mix(in srgb, ${palette[h % palette.length]} 60%, #1e293b))`
}

function shortTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return ''
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) return d.toTimeString().slice(0, 5)
  const diff = (now - d) / 86400000
  if (diff < 7) return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function scrollBottom() {
  nextTick(() => { const el = bodyRef.value; if (el) el.scrollTop = el.scrollHeight })
}

async function loadConvs(keepActive = true) {
  try {
    convs.value = await chatConversations()
    if (!keepActive || !activeKey.value) {
      const q = route.query.peer
      if (q && convs.value.some(c => String(c.peerId) === String(q))) openConv(Number(q))
      else if (!activeKey.value) openConv('ai', false)
    }
  } catch { convs.value = [] }
}

async function loadMessages(peerId) {
  if (!peerId) return
  loadingMsgs.value = true
  try {
    messages.value = await chatMessages(peerId)
    const c = convs.value.find(x => x.peerId === peerId)
    if (c) c.unread = 0
    scrollBottom()
  } catch (e) {
    ElMessage.error(e?.message || '消息加载失败')
  } finally { loadingMsgs.value = false }
}

function openConv(key, focusPanel = true) {
  activeKey.value = String(key)
  draft.value = ''
  if (focusPanel) mobilePanel.value = true
  if (String(key) === 'ai') {
    if (aiOnline.value === null) aiStatus().then(s => { aiOnline.value = !!s?.enabled }).catch(() => { aiOnline.value = false })
    scrollBottom()
  } else {
    messages.value = []
    loadMessages(Number(key))
  }
}

function sendCurrent() { isAi.value ? sendAi() : sendFriend() }

async function sendFriend() {
  const content = draft.value.trim()
  if (!content || sending.value || !activePeer.value) return
  sending.value = true
  try {
    const res = await chatSend({ peerId: activePeer.value, content })
    if (res?.message) messages.value.push(res.message)
    draft.value = ''
    const c = activeConv.value
    if (c) { c.lastContent = content; c.lastTime = res?.message?.createdAt; c.lastMine = true }
    scrollBottom()
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally { sending.value = false }
}

async function sendAi(text) {
  const q = (text || draft.value).trim()
  if (!q || sending.value) return
  draft.value = ''
  const history = aiMessages.value.filter(m => !m.pending).slice(-8).map(m => ({ role: m.role, content: m.content }))
  aiMessages.value.push({ role: 'user', content: q })
  const pendingMsg = { role: 'assistant', content: '', pending: true }
  aiMessages.value.push(pendingMsg)
  sending.value = true
  scrollBottom()
  try {
    const res = await aiChat({ message: q, history })
    pendingMsg.content = res?.reply || '暂时没有得到有效回复，请稍后再试。'
    if (typeof res?.enabled === 'boolean') aiOnline.value = res.enabled
  } catch (e) {
    pendingMsg.content = e?.message || '网络异常，请稍后再试。'
  } finally {
    pendingMsg.pending = false
    sending.value = false
    scrollBottom()
  }
}

onMounted(async () => {
  await loadConvs(false)
  /* 轮询：刷新会话未读；打开好友会话时同步新消息 */
  pollTimer = setInterval(async () => {
    if (document.hidden) return
    try {
      const fresh = await chatConversations()
      const cur = activePeer.value
      convs.value = fresh
      if (cur && !sending.value) {
        const c = fresh.find(x => x.peerId === cur)
        if (c && c.unread > 0) await loadMessages(cur)
      }
    } catch { /* 静默 */ }
  }, 15000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script><style scoped>
.chat-page { height: calc(100vh - 148px); min-height: 480px; }
.chat-shell {
  display: grid; grid-template-columns: 300px 1fr;
  height: 100%; overflow: hidden;
  border: 1px solid var(--default-border); border-radius: 16px;
  background: var(--default-box-color, #fff);
  box-shadow: var(--art-card-shadow, 0 2px 12px rgba(15, 23, 42, 0.04));
}

/* ---- 左栏 ---- */
.chat-side { display: flex; flex-direction: column; min-height: 0; border-right: 1px solid var(--default-border); }
.side-head { display: flex; align-items: baseline; justify-content: space-between; padding: 16px 16px 10px; }
.side-head b { font-size: 16px; font-weight: 800; letter-spacing: 0.4px; color: var(--art-gray-900); }
.side-total { font-size: 11.5px; color: var(--el-color-danger); font-weight: 600; }
.side-search { padding: 0 12px 10px; }
.side-list { flex: 1; overflow-y: auto; padding: 2px 8px 12px; }
.side-divider {
  margin: 8px 8px 6px; font-size: 11px; letter-spacing: 1.2px;
  color: var(--art-gray-500); font-weight: 600;
}
.conv {
  position: relative; display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 10px 10px; margin-bottom: 2px;
  border: none; border-radius: 12px; cursor: pointer; text-align: left;
  background: transparent; color: inherit;
  transition: background 0.18s, transform 0.18s;
}
.conv:hover { background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 7%, transparent); }
.conv.active {
  background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 12%, transparent);
}
.conv.active::before {
  content: ''; position: absolute; left: 0; top: 20%; bottom: 20%; width: 3px;
  border-radius: 3px; background: var(--flair-primary, #5b7cfa);
}
.conv-avatar {
  width: 40px; height: 40px; border-radius: 12px; flex: none;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-size: 15px; font-weight: 700;
}
.ai-avatar { background: linear-gradient(135deg, #5b7cfa, #8b5cf6) !important; color: #fff; }
.conv-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.conv-line1 { display: flex; align-items: baseline; justify-content: space-between; gap: 6px; }
.conv-line1 b {
  font-size: 13.5px; font-weight: 650; color: var(--art-gray-900);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.conv-time { font-style: normal; font-size: 11px; color: var(--art-gray-500); flex: none; }
.conv-line2 {
  font-size: 12px; color: var(--art-gray-600);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.mine-mark { font-style: normal; color: var(--art-gray-500); }
.conv-badge {
  flex: none; min-width: 18px; height: 18px; padding: 0 5px;
  border-radius: 9px; background: var(--el-color-danger); color: #fff;
  font-size: 11px; font-weight: 700; line-height: 18px; text-align: center;
}
.side-empty { text-align: center; padding: 26px 10px; color: var(--art-gray-500); }
.side-empty p { font-size: 12.5px; margin: 8px 0 4px; }

/* ---- 右栏 ---- */
.chat-main { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.main-head {
  display: flex; align-items: center; gap: 11px;
  padding: 13px 18px;
  border-bottom: 1px solid color-mix(in srgb, var(--flair-primary, #5b7cfa) 12%, var(--default-border));
}
.head-back { display: none; }
.head-avatar {
  width: 38px; height: 38px; border-radius: 12px; flex: none;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; font-weight: 700;
}
.head-info { flex: 1; min-width: 0; }
.head-info b { display: block; font-size: 14.5px; font-weight: 700; color: var(--art-gray-900); letter-spacing: 0.2px; }
.head-info span { display: block; margin-top: 2px; font-size: 11.5px; color: var(--art-gray-500); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-actions { display: flex; gap: 6px; }
.head-btn {
  width: 32px; height: 32px; border-radius: 9px; border: none; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; color: var(--art-gray-600);
  transition: background 0.18s, color 0.18s;
}
.head-btn:hover { background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 10%, transparent); color: var(--flair-primary, #5b7cfa); }

.main-body {
  flex: 1; overflow-y: auto; padding: 20px 20px 12px;
  display: flex; flex-direction: column; gap: 16px;
  background:
    radial-gradient(circle at 88% -10%, color-mix(in srgb, var(--flair-primary, #5b7cfa) 5%, transparent), transparent 42%),
    radial-gradient(circle at 0% 110%, color-mix(in srgb, #8b5cf6 4%, transparent), transparent 38%);
}
.msg { display: flex; gap: 10px; align-items: flex-start; }
.msg.mine { flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 10px; flex: none;
  display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
  color: #fff; font-size: 13px; font-weight: 700;
}
.msg-col { max-width: 80%; display: flex; flex-direction: column; gap: 4px; }
.msg.mine .msg-col { align-items: flex-end; }
.msg-meta { font-size: 11px; color: var(--art-gray-500); letter-spacing: 0.3px; }
.msg-bubble {
  padding: 10px 14px; border-radius: 4px 14px 14px 14px;
  font-size: 13.5px; line-height: 1.75; letter-spacing: 0.15px;
  white-space: pre-wrap; word-break: break-word;
  background: var(--default-box-color, #fff);
  border: 1px solid var(--default-border);
  color: var(--art-gray-800);
  box-shadow: 0 2px 8px -4px rgba(15, 23, 42, 0.08);
}
.msg.mine .msg-bubble {
  border-radius: 14px 4px 14px 14px;
  background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 14%, var(--default-box-color, #fff));
  border-color: color-mix(in srgb, var(--flair-primary, #5b7cfa) 26%, transparent);
  color: var(--art-gray-900);
}
.typing { display: inline-flex; gap: 4px; padding: 4px 2px; }
.typing i { width: 6px; height: 6px; border-radius: 50%; background: var(--flair-primary, #5b7cfa); opacity: 0.4; animation: chat-dot 1.1s ease-in-out infinite; }
.typing i:nth-child(2) { animation-delay: 0.18s; }
.typing i:nth-child(3) { animation-delay: 0.36s; }
@keyframes chat-dot { 40% { opacity: 1; transform: translateY(-3px); } }

.msg-empty { text-align: center; padding: 34px 12px 10px; max-width: 380px; margin: 0 auto; }
.empty-icon {
  width: 60px; height: 60px; margin: 0 auto 14px; border-radius: 19px;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 10%, transparent);
  color: var(--flair-primary, #5b7cfa);
}
.empty-title { font-size: 14.5px; font-weight: 700; color: var(--art-gray-900); }
.empty-sub { font-size: 12px; color: var(--art-gray-500); margin: 6px 0 16px; line-height: 1.6; }
.preset {
  display: block; width: 100%; text-align: left; margin-bottom: 8px;
  padding: 10px 14px; border-radius: 11px; cursor: pointer;
  border: 1px solid color-mix(in srgb, var(--flair-primary, #5b7cfa) 16%, var(--default-border));
  background: var(--default-box-color, #fff); color: var(--art-gray-800);
  font-size: 13px; transition: border-color 0.18s, background 0.18s, transform 0.18s;
}
.preset:hover {
  border-color: var(--flair-primary, #5b7cfa);
  background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 6%, var(--default-box-color, #fff));
  transform: translateX(3px);
}

.main-foot { padding: 12px 18px 14px; border-top: 1px solid var(--default-border); }
.foot-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 9px; gap: 10px; }
.foot-note { font-size: 11px; color: var(--art-gray-500); letter-spacing: 0.4px; }
.send-icon { margin-left: 5px; }

.main-placeholder {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  color: var(--art-gray-500);
}
.ph-icon {
  width: 76px; height: 76px; border-radius: 24px; margin-bottom: 6px;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--flair-primary, #5b7cfa) 8%, transparent);
  color: var(--flair-primary, #5b7cfa);
}
.main-placeholder p { font-size: 14px; font-weight: 650; color: var(--art-gray-700); }
.main-placeholder span { font-size: 12px; }

/* ---- 响应式：窄屏单栏切换 ---- */
@media (max-width: 860px) {
  .chat-page { height: calc(100vh - 200px); }
  .chat-shell { grid-template-columns: 1fr; }
  .chat-side { border-right: none; }
  .chat-main { display: none; }
  .chat-shell.show-panel .chat-side { display: none; }
  .chat-shell.show-panel .chat-main { display: flex; }
  .head-back {
    display: inline-flex; width: 32px; height: 32px; border-radius: 9px;
    border: none; cursor: pointer; align-items: center; justify-content: center;
    background: transparent; color: var(--art-gray-600);
  }
  .msg-col { max-width: 82%; }
}
@media (prefers-reduced-motion: reduce) {
  .typing i, .preset, .conv { animation: none !important; transition: none !important; }
}
</style>