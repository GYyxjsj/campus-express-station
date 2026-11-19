<template>
  <!-- 必签协议门禁 -->
  <el-dialog v-model="policyOpen" width="860px" top="6vh" append-to-body
             :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false"
             class="gate-dialog">
    <template #header>
      <div class="gate-head">
        <div class="gh-icon"><el-icon :size="22"><Stamp /></el-icon></div>
        <div class="gh-txt">
          <b>使用前请签署必签协议</b>
          <span>共 {{ pending.length }} 份待签署 · 签署后即可正常使用全部服务</span>
        </div>
        <div class="gh-count">{{ signedCount }} / {{ pending.length }}</div>
      </div>
    </template>

    <div class="gate-body">
      <aside class="gate-list">
        <button v-for="(p, i) in pending" :key="p.id" class="gl-item"
                :class="{ on: i === policyIndex, done: p.signed }"
                :style="{ '--nc': catColor(p.category) }" @click="policyIndex = i">
          <i class="gl-state">
            <el-icon :size="13"><component :is="p.signed ? 'CircleCheckFilled' : 'Document'" /></el-icon>
          </i>
          <span class="gl-txt">
            <b>{{ p.title }}</b>
            <em>{{ catLabel(p.category) }} · v{{ p.version }}</em>
          </span>
        </button>
        <div class="gl-foot">
          <el-button size="small" text :icon="Link" @click="goPolicies">协议中心</el-button>
        </div>
      </aside>

      <div class="gate-reader">
        <template v-if="currentPolicy">
          <div class="gr-head">
            <span class="cat-chip" :style="{ '--nc': catColor(currentPolicy.category) }">{{ catLabel(currentPolicy.category) }}</span>
            <el-tag size="small" type="danger" effect="light">必签</el-tag>
            <span class="gr-ver">v{{ currentPolicy.version }}</span>
            <span class="gr-date">生效 {{ fmtTime(currentPolicy.effectiveAt) }}</span>
          </div>
          <h2 class="gr-title">{{ currentPolicy.title }}</h2>
          <p v-if="currentPolicy.summary" class="gr-summary">{{ currentPolicy.summary }}</p>
          <div class="gr-progress"><i :style="{ width: readPct + '%' }" /></div>
          <div ref="readerRef" class="gr-body" @scroll="onReadScroll">
            <section v-for="(s, i) in policySections" :key="i" class="pv-sec">
              <h3>{{ s.title }}</h3>
              <p v-for="(c, j) in s.clauses" :key="j"><b v-if="c.no">{{ c.no }}</b>{{ c.text }}</p>
            </section>
          </div>
          <div class="gr-foot">
            <span class="gr-hint">
              <el-icon :size="14"><InfoFilled /></el-icon>
              {{ readPct >= 98 ? '已阅读完毕，可签署' : `请阅读至底部（${readPct}%）` }}
            </span>
            <div class="gr-btns">
              <el-button v-if="pending.length > 1" size="small" :icon="Check" :loading="signingAll" @click="signAll">
                一键签署全部
              </el-button>
              <el-button type="primary" :icon="EditPen" :disabled="readPct < 98 || currentPolicy.signed"
                         :loading="signing" @click="signCurrent">
                {{ currentPolicy.signed ? '已签署' : '我已阅读并同意签署' }}
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>

  <!-- 公告弹窗 -->
  <el-dialog v-model="noticeOpen" width="640px" top="8vh" append-to-body class="notice-dialog"
             :close-on-click-modal="false" @close="onNoticeClose">
    <template #header>
      <div class="np-banner" :style="{ '--na': priColor(currentNotice?.priority), '--nb': catColor(currentNotice?.category) }">
        <div class="np-glow" />
        <div class="np-top">
          <span class="np-cat"><el-icon :size="13"><component :is="catIcon(currentNotice?.category)" /></el-icon>{{ catLabel(currentNotice?.category) }}</span>
          <span class="np-pri" :class="'p' + (currentNotice?.priority || 0)">
            <i v-if="currentNotice?.priority === 2" class="np-pulse" />{{ priLabel(currentNotice?.priority) }}公告
          </span>
          <span class="np-idx">{{ noticeIndex + 1 }} / {{ notices.length }}</span>
        </div>
        <h2 class="np-title">{{ currentNotice?.title }}</h2>
        <div class="np-meta">
          <span><el-icon :size="13"><Clock /></el-icon>{{ fmtTime(currentNotice?.createdAt) }}</span>
          <span><el-icon :size="13"><OfficeBuilding /></el-icon>校园快递驿站运营组</span>
        </div>
      </div>
    </template>

    <div class="np-body">
      <template v-for="(b, i) in noticeBlocks" :key="i">
        <h4 v-if="b.type === 'h'">{{ b.text }}</h4>
        <div v-else-if="b.type === 'li'" class="np-li"><i>{{ b.no }}</i><span>{{ b.text }}</span></div>
        <p v-else>{{ b.text }}</p>
      </template>
    </div>

    <template #footer>
      <div class="np-foot">
        <div class="np-dots">
          <button v-for="(n, i) in notices" :key="n.id" class="np-dot" :class="{ on: i === noticeIndex }"
                  :style="{ '--nc': catColor(n.category) }" :aria-label="`第 ${i + 1} 条公告`" @click="noticeIndex = i" />
        </div>
        <div class="np-btns">
          <el-checkbox v-model="neverAgain" class="np-never" size="small">下次不再弹出</el-checkbox>
          <el-button text :icon="ChatLineSquare" @click="goNotices">公告中心</el-button>
          <el-button v-if="notices.length > 1" text :icon="Finished" @click="dismissAll">跳过全部</el-button>
          <el-button type="primary" :icon="Check" :loading="dismissing" @click="dismissCurrent">
            {{ noticeIndex < notices.length - 1 ? '知道了，下一条' : '我知道了' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Stamp, Check, EditPen, Link, Clock, ChatLineSquare, Finished } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { pendingPolicies, acceptPolicies, popupNotices, dismissNotice, dismissAllNotices } from '../api'
import { POLICY_CATEGORY, NOTICE_CATEGORY, NOTICE_PRIORITY, fmtTime } from '../utils/format'
import { parsePolicy } from '../utils/policyParse'

const store = useUserStore()
const router = useRouter()

/* ---------- 协议门禁 ---------- */
const policyOpen = ref(false)
const pending = ref([])
const policyIndex = ref(0)
const signing = ref(false)
const signingAll = ref(false)
const readerRef = ref(null)
const readPct = ref(0)

const currentPolicy = computed(() => pending.value[policyIndex.value] || null)
const policySections = computed(() => parsePolicy(currentPolicy.value?.content))
const signedCount = computed(() => pending.value.filter(p => p.signed).length)

function catLabel(c) { return POLICY_CATEGORY[c]?.label || NOTICE_CATEGORY[c]?.label || c || '通知' }
function catColor(c) { return POLICY_CATEGORY[c]?.color || NOTICE_CATEGORY[c]?.color || '#5b7cfa' }
function catIcon(c) { return NOTICE_CATEGORY[c]?.icon || 'Bell' }
function priLabel(p) { return NOTICE_PRIORITY[p]?.label || '普通' }
function priColor(p) { return NOTICE_PRIORITY[p]?.color || '#7987a1' }

function measureRead() {
  const el = readerRef.value
  if (!el) { readPct.value = 100; return }
  const max = el.scrollHeight - el.clientHeight
  readPct.value = max <= 4 ? 100 : Math.min(100, Math.round((el.scrollTop / max) * 100))
}
function onReadScroll() { measureRead() }
watch(policyIndex, async () => {
  readPct.value = 0
  await nextTick()
  if (readerRef.value) readerRef.value.scrollTop = 0
  measureRead()
})

async function signCurrent() {
  const p = currentPolicy.value
  if (!p || p.signed) return
  signing.value = true
  try {
    await acceptPolicies([p.id])
    p.signed = true
    ElMessage.success(`《${p.title}》v${p.version} 签署成功`)
    const next = pending.value.findIndex(x => !x.signed)
    if (next >= 0) policyIndex.value = next
    else { policyOpen.value = false; await loadNotices() }
  } catch (e) { ElMessage.error(e?.message || '签署失败') } finally { signing.value = false }
}

async function signAll() {
  const rest = pending.value.filter(p => !p.signed)
  if (!rest.length) return
  try {
    await ElMessageBox.confirm(
      `将一次性签署 ${rest.length} 份必签协议：${rest.map(p => `《${p.title}》`).join('、')}。签署即表示你已阅读并同意全部条款。`,
      '一键签署', { type: 'warning', confirmButtonText: '全部同意并签署' }
    )
  } catch { return }
  signingAll.value = true
  try {
    await acceptPolicies(rest.map(p => p.id))
    rest.forEach(p => { p.signed = true })
    ElMessage.success('全部必签协议已签署')
    policyOpen.value = false
    await loadNotices()
  } catch (e) { ElMessage.error(e?.message || '签署失败') } finally { signingAll.value = false }
}

function goPolicies() { policyOpen.value = false; router.push('/policies') }

/* ---------- 公告弹窗 ---------- */
const noticeOpen = ref(false)
const notices = ref([])
const noticeIndex = ref(0)
const dismissing = ref(false)
const neverAgain = ref(false)
const currentNotice = computed(() => notices.value[noticeIndex.value] || null)

/* 「下次不再弹出」本地记录：勾选后按公告 id 记录在本机，未勾选则每次登录仍弹出 */
const NEVER_KEY = 'ce_notice_never'
function readNeverSet() {
  try { return new Set(JSON.parse(localStorage.getItem(NEVER_KEY) || '[]')) } catch { return new Set() }
}
function rememberNever(id) {
  const set = readNeverSet()
  set.add(id)
  localStorage.setItem(NEVER_KEY, JSON.stringify([...set]))
}

const noticeBlocks = computed(() => {
  const out = []
  for (const raw of String(currentNotice.value?.content || '').split('\n')) {
    const line = raw.trim()
    if (!line) continue
    const h = line.match(/^#+\s*(.*)$/)
    if (h) { out.push({ type: 'h', text: h[1] }); continue }
    const li = line.match(/^(?:(\d+)[.、)]|[·•▪-])\s*(.*)$/)
    if (li) { out.push({ type: 'li', no: li[1] || '·', text: li[2] }); continue }
    out.push({ type: 'p', text: line })
  }
  return out
})

async function loadNotices() {
  // 公告弹窗仅面向学生端；管理端/员工端不弹
  if (!store.isStudent) return
  // 只在「本次登录进入」时弹一次：消费 login() 写入的一次性标记，刷新页面无标记不弹
  if (sessionStorage.getItem('ce_notice_pending') !== '1') return
  sessionStorage.removeItem('ce_notice_pending')
  try {
    const r = await popupNotices()
    const never = readNeverSet()
    const list = (r?.notices || []).filter(n => !never.has(n.id))
    if (!list.length) return
    notices.value = list
    noticeIndex.value = 0
    neverAgain.value = false
    noticeOpen.value = true
  } catch { /* 弹窗失败不阻断主流程 */ }
}
function stepNotice(removeId) {
  notices.value = notices.value.filter(x => x.id !== removeId)
  if (!notices.value.length) { noticeOpen.value = false; return }
  noticeIndex.value = Math.min(noticeIndex.value, notices.value.length - 1)
}
async function dismissCurrent() {
  const n = currentNotice.value
  if (!n) return
  dismissing.value = true
  try {
    if (neverAgain.value) {
      rememberNever(n.id)
      await dismissNotice(n.id).catch(() => {})
    }
    stepNotice(n.id)
  } finally { dismissing.value = false }
}
async function dismissAll() {
  dismissing.value = true
  try {
    // 勾选了才本地记录不再弹；已读留痕照记，但未勾选时下次登录仍会弹出
    if (neverAgain.value) notices.value.forEach(n => rememberNever(n.id))
    await dismissAllNotices().catch(() => {})
    notices.value = []
    noticeOpen.value = false
    ElMessage.success(neverAgain.value ? '已记录，这些公告下次不再弹出' : '已跳过，未勾选的公告下次登录仍会弹出')
  } finally { dismissing.value = false }
}
function onNoticeClose() {
  // 直接关闭：勾选了「下次不再弹出」才本地记录并标记已读，否则下次登录仍会弹出
  const n = currentNotice.value
  if (n && neverAgain.value) {
    rememberNever(n.id)
    dismissNotice(n.id).catch(() => {})
  }
  noticeOpen.value = false
}
function goNotices() { noticeOpen.value = false; router.push('/notices') }

/* ---------- 入口 ---------- */
let running = false
async function run() {
  if (!store.logged || running) return
  running = true
  try {
    const list = await pendingPolicies().catch(() => [])
    if (list?.length) {
      pending.value = list.map(p => ({ ...p, signed: false }))
      policyIndex.value = 0
      readPct.value = 0
      policyOpen.value = true
      await nextTick()
      measureRead()
      return
    }
    await loadNotices()
  } finally { running = false }
}

watch(() => store.user?.id, (id, old) => { if (id && id !== old) run() }, { immediate: true })
</script>

<style scoped>
/* ---------- 协议门禁 ---------- */
.gate-head { display: flex; align-items: center; gap: 13px; }
.gh-icon {
  width: 44px; height: 44px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff;
  background: linear-gradient(135deg, #4338ca, #818cf8);
  box-shadow: 0 10px 22px -12px rgba(99, 102, 241, 0.9);
}
.gh-txt { min-width: 0; flex: 1; }
.gh-txt b { display: block; font-size: 17px; font-weight: 800; color: var(--art-gray-900); }
.gh-txt span { display: block; font-size: 12px; color: var(--art-gray-500); margin-top: 3px; }
.gh-count {
  flex-shrink: 0; font-size: 13px; font-weight: 800; color: var(--ap-policy);
  background: rgba(99, 102, 241, 0.12); border: 1px solid rgba(99, 102, 241, 0.3);
  padding: 6px 13px; border-radius: 999px; font-variant-numeric: tabular-nums;
}

.gate-body { display: grid; grid-template-columns: 232px 1fr; gap: 16px; min-height: 380px; }
.gate-list { display: flex; flex-direction: column; gap: 7px; min-width: 0; }
.gl-item {
  display: flex; align-items: flex-start; gap: 9px; text-align: left;
  padding: 10px 11px; border-radius: 11px; cursor: pointer;
  border: 1.5px solid var(--default-border); background: var(--default-box-color);
  transition: all 0.22s var(--flair-ease);
}
.gl-item:hover { border-color: var(--nc); transform: translateX(2px); }
.gl-item.on { border-color: var(--nc); background: color-mix(in srgb, var(--nc) 10%, transparent); }
.gl-item.done { opacity: 0.62; }
.gl-state {
  width: 24px; height: 24px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--nc) 15%, transparent); color: var(--nc);
}
.gl-item.done .gl-state { background: rgba(16, 217, 181, 0.16); color: #0d9488; }
.gl-txt { min-width: 0; }
.gl-txt b { display: block; font-size: 12.5px; font-weight: 700; color: var(--art-gray-900); line-height: 1.45; }
.gl-txt em { display: block; font-style: normal; font-size: 10.5px; color: var(--art-gray-500); margin-top: 3px; }
.gl-foot { margin-top: auto; padding-top: 8px; }

.gate-reader { display: flex; flex-direction: column; min-width: 0; }
.gr-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.cat-chip {
  display: inline-flex; align-items: center; font-size: 11px; font-weight: 700; color: var(--nc);
  background: color-mix(in srgb, var(--nc) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--nc) 32%, transparent);
  padding: 3px 9px; border-radius: 999px;
}
.gr-ver { font-size: 11.5px; font-weight: 700; color: var(--art-gray-600); }
.gr-date { font-size: 11px; color: var(--art-gray-500); margin-left: auto; }
.gr-title { margin: 11px 0 0; font-size: 19px; font-weight: 800; color: var(--art-gray-900); line-height: 1.4; }
.gr-summary { margin: 8px 0 0; font-size: 12.5px; line-height: 1.7; color: var(--art-gray-600); }
.gr-progress { height: 3px; border-radius: 2px; background: var(--art-gray-200); overflow: hidden; margin: 12px 0 0; }
html.dark .gr-progress { background: #26262e; }
.gr-progress i { display: block; height: 100%; background: linear-gradient(90deg, #6366f1, #10d9b5); transition: width 0.2s linear; }
.gr-body { flex: 1; overflow-y: auto; max-height: 340px; margin-top: 12px; padding-right: 6px; }
.pv-sec { margin-bottom: 15px; }
.pv-sec h3 { margin: 0 0 8px; font-size: 14px; font-weight: 800; color: var(--art-gray-900); padding-left: 10px; border-left: 3px solid var(--ap-policy); }
.pv-sec p { margin: 0 0 6px; font-size: 12.6px; line-height: 1.85; color: var(--art-gray-700); }
.pv-sec p b { color: var(--ap-policy); font-weight: 800; margin-right: 6px; }
.gr-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-top: 14px; padding-top: 13px; border-top: 1px dashed var(--default-border); }
.gr-hint { display: inline-flex; align-items: center; gap: 6px; font-size: 11.5px; color: var(--art-gray-500); }
.gr-btns { display: flex; gap: 8px; flex-wrap: wrap; }

/* ---------- 公告弹窗 ---------- */
.np-banner {
  position: relative; overflow: hidden;
  padding: 20px 22px 18px;
  background: linear-gradient(120deg, var(--na), var(--nb) 62%, color-mix(in srgb, var(--nb) 55%, #ffffff));
  color: #fff;
}
.np-banner::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.22) 1px, transparent 1px);
  background-size: 9px 9px; opacity: 0.5;
  -webkit-mask-image: linear-gradient(115deg, #000, transparent 62%);
  mask-image: linear-gradient(115deg, #000, transparent 62%);
}
.np-glow {
  position: absolute; width: 260px; height: 260px; right: -70px; top: -130px;
  border-radius: 50%; pointer-events: none;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.4), transparent 68%);
  filter: blur(6px); animation: np-drift 9s ease-in-out infinite;
}
@keyframes np-drift { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(-26px, 22px); } }
.np-top { position: relative; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.np-cat {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 700; letter-spacing: 0.4px;
  background: rgba(255, 255, 255, 0.22); border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 4px 10px; border-radius: 999px; backdrop-filter: blur(4px);
}
.np-pri {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 800; padding: 4px 10px; border-radius: 999px;
  background: rgba(12, 14, 30, 0.28); border: 1px solid rgba(255, 255, 255, 0.28);
}
.np-pri.p2 { background: rgba(255, 255, 255, 0.94); color: #e11d48; }
.np-pri.p1 { background: rgba(255, 255, 255, 0.9); color: #b45309; }
.np-pulse { width: 7px; height: 7px; border-radius: 50%; background: #e11d48; animation: art-breathe 1.2s infinite; }
.np-idx { margin-left: auto; font-size: 11.5px; font-weight: 700; opacity: 0.9; font-variant-numeric: tabular-nums; }
.np-title { position: relative; margin: 13px 0 0; font-size: clamp(18px, 2.4vw, 23px); font-weight: 800; line-height: 1.4; text-shadow: 0 2px 12px rgba(10, 12, 30, 0.25); }
.np-meta { position: relative; display: flex; gap: 16px; flex-wrap: wrap; margin-top: 11px; font-size: 11.5px; opacity: 0.92; }
.np-meta span { display: inline-flex; align-items: center; gap: 5px; }

.np-body { max-height: 46vh; overflow-y: auto; padding: 4px 2px; }
.np-body p { margin: 0 0 11px; font-size: 13.2px; line-height: 1.9; color: var(--art-gray-700); }
.np-body h4 { margin: 16px 0 9px; font-size: 14px; font-weight: 800; color: var(--art-gray-900); padding-left: 10px; border-left: 3px solid var(--ap-notice); }
.np-li { display: flex; gap: 10px; margin-bottom: 9px; }
.np-li i {
  flex-shrink: 0; min-width: 22px; height: 22px; padding: 0 5px;
  border-radius: 7px; font-style: normal; font-size: 11.5px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  background: rgba(251, 113, 133, 0.14); color: #e11d48;
}
html.dark .np-li i { color: #ff8fa3; }
.np-li span { font-size: 13px; line-height: 1.8; color: var(--art-gray-700); }

.np-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.np-dots { display: flex; gap: 6px; }
.np-dot {
  width: 8px; height: 8px; border-radius: 50%; border: none; cursor: pointer; padding: 0;
  background: var(--art-gray-300); transition: all 0.25s var(--flair-ease);
}
html.dark .np-dot { background: #33333e; }
.np-dot.on { width: 22px; border-radius: 5px; background: var(--nc); }
.np-btns { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-left: auto; }
.np-never { margin-right: 6px; }
.np-never :deep(.el-checkbox__label) { font-size: 12px; color: var(--art-gray-600); }

@media (max-width: 768px) {
  .gate-body { grid-template-columns: 1fr; min-height: 0; }
  .gate-list { flex-direction: row; overflow-x: auto; padding-bottom: 4px; }
  .gl-item { min-width: 190px; flex-shrink: 0; }
  .gl-foot { display: none; }
  .gr-body { max-height: 40vh; }
  .np-banner { padding: 16px 16px 15px; }
  .np-body { max-height: 44vh; }
  .np-foot { justify-content: center; }
  .np-btns { margin-left: 0; width: 100%; justify-content: center; }
}
@media (prefers-reduced-motion: reduce) {
  .np-glow, .np-pulse { animation: none; }
}
</style>

<!-- 弹层外壳样式：dialog 被 append-to-body 传送，需用非 scoped 块按自定义类名命中 -->
<style>
.notice-dialog .el-dialog__header { padding: 0; margin-right: 0; border-bottom: none; }
.notice-dialog .el-dialog__headerbtn { top: 12px; right: 12px; z-index: 3; }
.notice-dialog .el-dialog__headerbtn .el-dialog__close { color: #fff; font-size: 18px; }
.notice-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #fff; opacity: 0.75; }
.notice-dialog .el-dialog__body { padding: 18px 22px; }
.notice-dialog .el-dialog__footer { padding: 12px 22px 18px; }
.notice-dialog { border-radius: 16px; overflow: hidden; }

.gate-dialog { border-radius: 16px; overflow: hidden; }
.gate-dialog .el-dialog__body { padding: 16px 20px 20px; }

@media (max-width: 768px) {
  .notice-dialog .el-dialog__body { padding: 15px 15px; max-height: none; overflow: visible; }
  .notice-dialog .el-dialog__footer { padding: 11px 15px 15px; }
  .gate-dialog .el-dialog__body { padding: 14px 14px 16px; max-height: 74vh; }
}
</style>