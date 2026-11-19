<template>
  <div class="ap">
    <PageHero tone="proxy" icon="Tickets" chip="MY SERVICE ORDERS"
      title="我的代取服务单"
      sub="左侧查看我发布的委托，右侧查看我接下的跑腿。每一步状态变更都会写入包裹去向台账并推送提醒。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Plus" @click="$router.push('/student/proxy-market')">去大厅接单</el-button>
        <el-button type="default" :icon="Wallet" @click="$router.push('/student/wallet')">钱包余额</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard :title="tab === 'requests' ? '我发布的委托' : '我接下的跑腿'" :note="`${total} 单`"
               :icon="tab === 'requests' ? 'Promotion' : 'Bicycle'" accent="proxy" :index="4">
      <template #extra>
        <div class="ap-seg">
          <button :class="{ on: tab === 'requests' }" @click="switchTab('requests')">
            <el-icon :size="13"><Promotion /></el-icon>我发布的<span class="n">{{ reqCount }}</span>
          </button>
          <button :class="{ on: tab === 'runs' }" @click="switchTab('runs')">
            <el-icon :size="13"><Bicycle /></el-icon>我接的<span class="n">{{ runCount }}</span>
          </button>
        </div>
      </template>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="s in statusTabs" :key="s.key" :class="{ on: status === s.key }" @click="status = s.key; page = 1; load()">
            {{ s.label }}
          </button>
        </div>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <div v-loading="loading" class="order-list">
        <article v-for="(o, i) in rows" :key="o.id" class="order-card flair-in" :style="{ '--i': Math.min(i, 10), '--oc': statusColor(o.status) }">
          <div class="oc-head">
            <span class="oc-no ap-mono">{{ o.orderNo }}</span>
            <el-tag size="small" :type="dict(ORDER_STATUS, o.status).type" effect="dark">{{ dict(ORDER_STATUS, o.status).label }}</el-tag>
            <span class="oc-time">{{ fmtTime(o.createdAt) }}</span>
          </div>

          <div class="oc-stepper">
            <div v-for="(s, si) in STEPS" :key="s.key" class="oc-step"
                 :class="{ done: stepIndex(o.status) > si, now: stepIndex(o.status) === si, dead: isDead(o.status) }">
              <span class="oc-dot"><el-icon :size="11"><component :is="s.icon" /></el-icon></span>
              <span class="oc-step-label">{{ s.label }}</span>
            </div>
          </div>

          <div class="oc-body">
            <div class="oc-parcel">
              <el-icon :size="15"><Box /></el-icon>
              <b class="ap-mono">{{ o.trackingNo }}</b>
              <span>{{ courierName(o.courierCompany) }}</span>
              <el-tag size="small" :type="dict(PARCEL_STATUS, o.parcelStatus).type" effect="plain">
                {{ dict(PARCEL_STATUS, o.parcelStatus).label }}
              </el-tag>
            </div>
            <div class="oc-route">
              <span><el-icon :size="13"><OfficeBuilding /></el-icon>{{ o.stationName || '驿站' }}{{ o.shelfCode ? ` · 货架 ${o.shelfCode}` : '' }}</span>
              <el-icon :size="13" class="oc-arrow"><Right /></el-icon>
              <span><el-icon :size="13"><LocationInformation /></el-icon>{{ o.deliverTo || '—' }}</span>
            </div>
            <div class="oc-people">
              <span>委托人 <b>{{ o.requesterName || '—' }}</b></span>
              <span>跑腿员 <b>{{ o.runnerName || '待接单' }}</b></span>
              <span v-if="o.rating">评分 <b class="oc-rate">{{ '★'.repeat(o.rating) }}</b></span>
            </div>
            <p v-if="o.note" class="oc-note">{{ o.note }}</p>
            <p v-if="o.cancelReason" class="oc-cancel">取消原因：{{ o.cancelReason }}</p>
          </div>

          <div class="oc-foot">
            <div class="oc-money">
              <span class="oc-total"><i>托管</i>{{ money(o.total) }}</span>
              <span class="oc-sub">服务费 {{ money(o.fee) }}<template v-if="Number(o.tip)"> + 小费 {{ money(o.tip) }}</template></span>
              <span class="oc-sub" v-if="tab === 'runs'">实得 <b class="ap-money bare in">{{ money(o.runnerIncome) }}</b></span>
              <span class="oc-sub" v-else>平台服务费 {{ money(o.commission) }}</span>
            </div>
            <div class="oc-actions">
              <el-button size="small" :icon="View" @click="openDetail(o)">详情</el-button>
              <template v-if="tab === 'requests'">
                <el-button v-if="o.status === 'DELIVERED'" size="small" type="success" :icon="CircleCheck"
                           :loading="acting === o.id" @click="doConfirm(o)">确认收货</el-button>
                <el-button v-if="o.status === 'DONE' && !o.rating" size="small" type="warning" :icon="Star"
                           @click="openRate(o)">评价</el-button>
                <el-button v-if="['OPEN', 'ACCEPTED'].includes(o.status)" size="small" type="danger" plain
                           @click="doCancel(o)">取消</el-button>
              </template>
              <template v-else>
                <el-button v-if="o.status === 'ACCEPTED'" size="small" type="primary" :icon="Box"
                           :loading="acting === o.id" @click="doPicked(o)">标记已取件</el-button>
                <el-button v-if="o.status === 'PICKED'" size="small" type="success" :icon="Van"
                           :loading="acting === o.id" @click="doDelivered(o)">标记已送达</el-button>
                <el-button v-if="['OPEN', 'ACCEPTED'].includes(o.status)" size="small" type="danger" plain
                           @click="doCancel(o)">放弃</el-button>
              </template>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty :description="tab === 'requests' ? '还没有发布过代取委托' : '还没有接下跑腿单'" :image-size="86" />
        <p>{{ tab === 'requests' ? '在包裹详情或大厅发布需求，同学接单后帮你取件送达。' : '去大厅看看有没有顺路的代取需求，赚取跑腿收入。' }}</p>
        <el-button type="primary" :icon="Plus" style="margin-top:12px" @click="$router.push('/student/proxy-market')">
          前往代取大厅
        </el-button>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="page" :page-size="size" @current-change="load" />
      </div>
    </PanelCard>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailOpen" :title="current ? `服务单 ${current.orderNo}` : '服务单详情'" size="520px" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-status" :style="{ '--oc': statusColor(current.status) }">
          <el-tag :type="dict(ORDER_STATUS, current.status).type" effect="dark" size="large">{{ dict(ORDER_STATUS, current.status).label }}</el-tag>
          <span v-if="!isDead(current.status) && current.expireAt">接单截止 {{ fmtTime(current.expireAt) }} · 剩余 {{ countdown(current.expireAt) }}</span>
        </div>

        <div v-if="current.authCode && tab === 'runs'" class="auth-box">
          <label>柜台代取授权码</label>
          <b class="ap-mono">{{ current.authCode }}</b>
          <p>向驿站员工出示此授权码 + 运单号即可代取，无需委托人取件码。</p>
          <el-button size="small" type="primary" plain :icon="DocumentCopy" @click="copyAuth">复制授权码</el-button>
        </div>

        <h4 class="d-h">包裹与路线</h4>
        <div class="ap-kv">
          <div><label>运单号</label><span class="ap-mono">{{ current.trackingNo }}</span></div>
          <div><label>快递公司</label><span>{{ courierName(current.courierCompany) }}</span></div>
          <div><label>所在驿站</label><span>{{ current.stationName || '—' }}</span></div>
          <div><label>货架</label><span>{{ current.shelfCode || '—' }}</span></div>
          <div><label>驿站地址</label><span class="muted">{{ current.stationLocation || '—' }}</span></div>
          <div><label>送达地址</label><span>{{ current.deliverTo || '—' }}</span></div>
        </div>

        <h4 class="d-h">参与人</h4>
        <div class="ap-kv">
          <div><label>委托人</label><span>{{ current.requesterName || '—' }} <em class="ap-mono">{{ current.requesterUsername || '' }}</em></span></div>
          <div><label>跑腿员</label><span>{{ current.runnerName || '待接单' }} <em class="ap-mono">{{ current.runnerUsername || '' }}</em></span></div>
          <div v-if="current.requesterPhone"><label>委托人电话</label><span class="ap-mono">{{ current.requesterPhone }}</span></div>
          <div v-if="current.runnerPhone"><label>跑腿员电话</label><span class="ap-mono">{{ current.runnerPhone }}</span></div>
        </div>

        <h4 class="d-h">费用结算</h4>
        <div class="money-box">
          <div><label>服务费</label><span>{{ money(current.fee) }}</span></div>
          <div><label>小费</label><span>{{ money(current.tip) }}</span></div>
          <div class="hl"><label>托管总额</label><span>{{ money(current.total) }}</span></div>
          <div><label>平台服务费</label><span>{{ money(current.commission) }}</span></div>
          <div class="hl in"><label>跑腿员实得</label><span>{{ money(current.runnerIncome) }}</span></div>
        </div>

        <h4 class="d-h">流转时间</h4>
        <div class="ap-kv">
          <div><label>发布</label><span class="muted">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>接单</label><span class="muted">{{ current.acceptedAt ? fmtTime(current.acceptedAt) : '—' }}</span></div>
          <div><label>取件</label><span class="muted">{{ current.pickedAt ? fmtTime(current.pickedAt) : '—' }}</span></div>
          <div><label>送达</label><span class="muted">{{ current.deliveredAt ? fmtTime(current.deliveredAt) : '—' }}</span></div>
          <div><label>完成</label><span class="muted">{{ current.doneAt ? fmtTime(current.doneAt) : '—' }}</span></div>
          <div><label>取消</label><span class="muted">{{ current.cancelledAt ? fmtTime(current.cancelledAt) : '—' }}</span></div>
        </div>

        <template v-if="current.note || current.review || current.cancelReason">
          <h4 class="d-h">备注与评价</h4>
          <div v-if="current.note" class="d-text"><label>委托备注</label>{{ current.note }}</div>
          <div v-if="current.cancelReason" class="d-text"><label>取消原因</label>{{ current.cancelReason }}</div>
          <div v-if="current.rating" class="d-text">
            <label>服务评价 <b class="oc-rate">{{ '★'.repeat(current.rating) }}{{ '☆'.repeat(5 - current.rating) }}</b></label>
            {{ current.review || '未填写评价内容' }}
          </div>
        </template>

        <h4 class="d-h">包裹去向台账</h4>
        <TraceTimeline :nodes="traces" />
      </div>
    </el-drawer>

    <!-- 评价 -->
    <el-dialog v-model="rateOpen" title="评价跑腿服务" width="460px" append-to-body>
      <div v-if="rateTarget" class="rate-box">
        <p class="rate-no ap-mono">{{ rateTarget.orderNo }}</p>
        <p class="rate-runner">跑腿员 <b>{{ rateTarget.runnerName }}</b> · 实得 {{ money(rateTarget.runnerIncome) }}</p>
        <el-rate v-model="rateForm.rating" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text size="large" />
        <el-input v-model="rateForm.review" type="textarea" :rows="3" maxlength="200" show-word-limit
                  placeholder="说说取件速度、沟通与包裹完好情况（选填）" style="margin-top:14px" />
      </div>
      <template #footer>
        <el-button @click="rateOpen = false">稍后评价</el-button>
        <el-button type="primary" :loading="rating" @click="doRate">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Wallet, Refresh, View, CircleCheck, Star, Box, Van, DocumentCopy } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import MetricTile from '../../components/MetricTile.vue'
import TraceTimeline from '../../components/TraceTimeline.vue'
import {
  myProxyRequests, myProxyRuns, proxyOrderDetail, parcelTraces,
  confirmProxyOrder, cancelProxyOrder, pickedProxyOrder, deliveredProxyOrder, rateProxyOrder
} from '../../api'
import { ORDER_STATUS, PARCEL_STATUS, dict, fmtTime, money, countdown, courierName } from '../../utils/format'

const route = useRoute()
const STEPS = [
  { key: 'OPEN', label: '发布', icon: 'Promotion' },
  { key: 'ACCEPTED', label: '接单', icon: 'Bicycle' },
  { key: 'PICKED', label: '取件', icon: 'Box' },
  { key: 'DELIVERED', label: '送达', icon: 'Van' },
  { key: 'DONE', label: '完成', icon: 'CircleCheck' }
]

const tab = ref(route.query.tab === 'runs' ? 'runs' : 'requests')
const status = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 6
const loading = ref(false)
const acting = ref(null)
const reqCount = ref(0)
const runCount = ref(0)
const allReq = ref([])
const allRun = ref([])

const statusTabs = [
  { key: '', label: '全部' }, { key: 'OPEN', label: '待接单' }, { key: 'ACCEPTED', label: '已接单' },
  { key: 'PICKED', label: '已取件' }, { key: 'DELIVERED', label: '已送达' }, { key: 'DONE', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

function stepIndex(s) {
  const i = STEPS.findIndex(x => x.key === s)
  return i < 0 ? (s === 'EXPIRED' ? 0 : -1) : i
}
function isDead(s) { return ['CANCELLED', 'EXPIRED'].includes(s) }
function statusColor(s) { return ORDER_STATUS[s]?.color || '#7987a1' }

const source = computed(() => tab.value === 'requests' ? allReq.value : allRun.value)
const heroStats = computed(() => [
  { label: '我发布的', value: reqCount.value, unit: '单' },
  { label: '我接的', value: runCount.value, unit: '单' },
  { label: '跑腿累计收入', value: allRun.value.filter(o => o.status === 'DONE').reduce((s, o) => s + Number(o.runnerIncome || 0), 0), decimals: 2, unit: '元' }
])
const metrics = computed(() => {
  const list = source.value
  const cnt = k => list.filter(o => o.status === k).length
  return [
    { label: '进行中', value: list.filter(o => ['OPEN', 'ACCEPTED', 'PICKED', 'DELIVERED'].includes(o.status)).length, unit: '单', color: '#0ea5e9', icon: 'Loading', foot: '含待接单与配送中' },
    { label: '已完成', value: cnt('DONE'), unit: '单', color: '#10d9b5', icon: 'CircleCheck', foot: '已结算并入账' },
    { label: '待处理', value: tab.value === 'requests' ? cnt('DELIVERED') : cnt('ACCEPTED') + cnt('PICKED'), unit: '单', color: '#f59e0b', icon: 'Bell', foot: tab.value === 'requests' ? '等待你确认收货' : '等待你取件 / 送达' },
    { label: '已关闭', value: cnt('CANCELLED') + cnt('EXPIRED'), unit: '单', color: '#7987a1', icon: 'CircleClose', foot: '取消或超时，托管已退' }
  ]
})

async function load() {
  loading.value = true
  try {
    const api = tab.value === 'requests' ? myProxyRequests : myProxyRuns
    const r = await api({ page: page.value, size, status: status.value || undefined })
    rows.value = r.records || []
    total.value = Number(r.total || 0)
  } finally { loading.value = false }
}
async function loadCounts() {
  const [a, b] = await Promise.all([
    myProxyRequests({ page: 1, size: 100 }),
    myProxyRuns({ page: 1, size: 100 })
  ])
  allReq.value = a.records || []
  allRun.value = b.records || []
  reqCount.value = Number(a.total || 0)
  runCount.value = Number(b.total || 0)
}
function switchTab(t) {
  tab.value = t
  status.value = ''
  page.value = 1
  load()
}
watch(() => route.query.tab, v => { if (v && v !== tab.value) switchTab(v) })

async function refreshAll() { await Promise.all([load(), loadCounts()]) }

async function doConfirm(o) {
  try {
    await ElMessageBox.confirm(`确认已收到包裹？确认后托管金额 ${money(o.total)} 将结算给跑腿员 ${o.runnerName || ''}。`, '确认收货', { type: 'success' })
  } catch { return }
  acting.value = o.id
  try { await confirmProxyOrder(o.id); ElMessage.success('已确认收货，费用已结算'); await refreshAll() }
  finally { acting.value = null }
}
async function doPicked(o) {
  acting.value = o.id
  try { await pickedProxyOrder(o.id); ElMessage.success('已标记取件，请尽快送达'); await refreshAll() }
  finally { acting.value = null }
}
async function doDelivered(o) {
  acting.value = o.id
  try { await deliveredProxyOrder(o.id); ElMessage.success('已标记送达，等待委托人确认（24 小时后自动确认）'); await refreshAll() }
  finally { acting.value = null }
}
async function doCancel(o) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt('请填写取消原因，托管金额将全额退回钱包。', '取消服务单', {
      inputPlaceholder: '如：临时不需要 / 自行取件', inputValidator: v => (v && v.trim() ? true : '请填写取消原因')
    })
    reason = r.value
  } catch { return }
  await cancelProxyOrder(o.id, reason)
  ElMessage.success('服务单已取消，托管金额已退回')
  await refreshAll()
}

/* ---- 详情 ---- */
const detailOpen = ref(false)
const current = ref(null)
const traces = ref([])
async function openDetail(o) {
  detailOpen.value = true
  current.value = o
  traces.value = []
  const [d, t] = await Promise.all([
    proxyOrderDetail(o.id).catch(() => o),
    parcelTraces(o.parcelId).catch(() => [])
  ])
  current.value = d
  traces.value = t
}
async function copyAuth() {
  try { await navigator.clipboard.writeText(current.value.authCode); ElMessage.success('授权码已复制') }
  catch { ElMessage.warning('请手动复制授权码') }
}

/* ---- 评价 ---- */
const rateOpen = ref(false)
const rateTarget = ref(null)
const rating = ref(false)
const rateForm = ref({ rating: 5, review: '' })
function openRate(o) { rateTarget.value = o; rateForm.value = { rating: 5, review: '' }; rateOpen.value = true }
async function doRate() {
  rating.value = true
  try {
    await rateProxyOrder(rateTarget.value.id, { ...rateForm.value })
    ElMessage.success('评价已提交，感谢反馈')
    rateOpen.value = false
    await refreshAll()
  } finally { rating.value = false }
}

onMounted(async () => { await Promise.all([load(), loadCounts()]) })
</script>

<style scoped>
.status-seg { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }

.order-list { display: flex; flex-direction: column; gap: 13px; min-height: 100px; }
.order-card {
  position: relative; overflow: hidden;
  border: 1px solid var(--default-border);
  border-left: 3px solid var(--oc);
  border-radius: var(--art-radius-card-sm);
  background: var(--default-box-color);
  padding: 13px 15px;
  transition: box-shadow 0.3s, transform 0.3s var(--flair-ease);
}
.order-card:hover { box-shadow: 0 16px 32px -22px rgba(20, 25, 60, 0.5); transform: translateY(-2px); }
.oc-head { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.oc-no { font-size: 12.5px; font-weight: 700; color: var(--art-gray-900); }
.oc-time { margin-left: auto; font-size: 11.5px; color: var(--art-gray-500); }

/* 状态步进器 */
.oc-stepper { display: flex; align-items: flex-start; gap: 4px; margin: 13px 0 12px; }
.oc-step { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 5px; position: relative; min-width: 0; }
.oc-step::before {
  content: ''; position: absolute; top: 10px; left: -50%; width: 100%; height: 2px;
  background: var(--default-border); z-index: 0;
}
.oc-step:first-child::before { display: none; }
.oc-step.done::before, .oc-step.now::before { background: var(--oc); }
.oc-step.dead::before { background: var(--default-border); }
.oc-dot {
  position: relative; z-index: 1;
  width: 21px; height: 21px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: var(--default-box-color);
  border: 2px solid var(--default-border);
  color: var(--art-gray-400);
  transition: all 0.3s var(--flair-ease);
}
.oc-step.done .oc-dot { background: var(--oc); border-color: var(--oc); color: #fff; }
.oc-step.now .oc-dot { border-color: var(--oc); color: var(--oc); box-shadow: 0 0 0 4px color-mix(in srgb, var(--oc) 18%, transparent); }
.oc-step.dead .oc-dot { opacity: 0.45; }
.oc-step-label { font-size: 10.5px; color: var(--art-gray-500); white-space: nowrap; }
.oc-step.done .oc-step-label, .oc-step.now .oc-step-label { color: var(--art-gray-800); font-weight: 700; }

.oc-body { display: flex; flex-direction: column; gap: 7px; }
.oc-parcel { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 12.5px; color: var(--art-gray-600); }
.oc-parcel .el-icon { color: var(--ap-proxy); }
.oc-parcel b { font-size: 13px; color: var(--art-gray-900); }
.oc-route { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 12px; color: var(--art-gray-700); }
.oc-route span { display: inline-flex; align-items: center; gap: 5px; min-width: 0; }
.oc-route .el-icon { color: var(--art-gray-500); flex-shrink: 0; }
.oc-arrow { color: var(--art-gray-400) !important; }
.oc-people { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: var(--art-gray-500); }
.oc-people b { color: var(--art-gray-800); }
.oc-rate { color: #f59e0b; letter-spacing: 1px; }
.oc-note, .oc-cancel {
  margin: 0; font-size: 11.5px; line-height: 1.6; color: var(--art-gray-600);
  background: var(--art-gray-100); border-radius: 8px; padding: 6px 10px;
}
.oc-cancel { background: rgba(251, 113, 133, 0.1); color: #c2455e; }
html.dark .oc-note { background: #1c1c22; }
html.dark .oc-cancel { color: #ff8fa3; }

.oc-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  margin-top: 12px; padding-top: 11px; border-top: 1px dashed var(--default-border);
}
.oc-money { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.oc-total { font-size: 18px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.oc-total i { font-style: normal; font-size: 11px; font-weight: 600; color: var(--art-gray-500); margin-right: 4px; }
.oc-sub { font-size: 11.5px; color: var(--art-gray-500); }
.oc-actions { display: flex; gap: 6px; flex-wrap: wrap; margin-left: auto; }

/* 详情 */
.detail { display: flex; flex-direction: column; gap: 4px; }
.d-status { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding-bottom: 12px; border-bottom: 1px dashed var(--default-border); }
.d-status span { font-size: 11.5px; color: var(--art-gray-500); }
.d-h {
  margin: 18px 0 10px; font-size: 12px; font-weight: 800; letter-spacing: 1.2px;
  color: var(--art-gray-500); text-transform: uppercase;
  display: flex; align-items: center; gap: 8px;
}
.d-h::after { content: ''; flex: 1; height: 1px; background: var(--default-border); }
.auth-box {
  margin-top: 14px; padding: 14px 16px; border-radius: 13px; text-align: center;
  background: linear-gradient(140deg, rgba(14, 165, 233, 0.12), rgba(139, 92, 246, 0.08));
  border: 1px dashed color-mix(in srgb, #0ea5e9 45%, transparent);
}
.auth-box label { display: block; font-size: 11px; letter-spacing: 1.2px; color: var(--art-gray-500); text-transform: uppercase; }
.auth-box b { display: block; font-size: 32px; font-weight: 800; letter-spacing: 6px; color: var(--ap-proxy); margin: 6px 0 4px; }
.auth-box p { margin: 0 0 10px; font-size: 11.5px; color: var(--art-gray-600); line-height: 1.6; }
.detail .ap-kv em { font-style: normal; font-size: 11.5px; color: var(--art-gray-500); margin-left: 5px; }

.money-box { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 120px), 1fr)); gap: 9px; }
.money-box > div { padding: 9px 11px; border-radius: 10px; background: var(--art-gray-100); border: 1px solid var(--default-border); }
.money-box label { display: block; font-size: 10.5px; color: var(--art-gray-500); letter-spacing: 0.5px; }
.money-box span { display: block; font-size: 15px; font-weight: 800; color: var(--art-gray-900); margin-top: 3px; font-variant-numeric: tabular-nums; }
.money-box .hl { background: color-mix(in srgb, #0ea5e9 10%, transparent); border-color: color-mix(in srgb, #0ea5e9 30%, transparent); }
.money-box .hl.in { background: color-mix(in srgb, #10d9b5 12%, transparent); border-color: color-mix(in srgb, #10d9b5 32%, transparent); }
.money-box .hl.in span { color: #0d9488; }
html.dark .money-box > div { background: #1c1c22; }
html.dark .money-box .hl.in span { color: #35e0c2; }
.d-text { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); margin-bottom: 9px; }
.d-text label { display: block; font-size: 11px; color: var(--art-gray-500); margin-bottom: 3px; }

.rate-box { text-align: center; }
.rate-no { font-size: 12px; color: var(--art-gray-500); margin: 0 0 4px; }
.rate-runner { font-size: 13px; color: var(--art-gray-700); margin: 0 0 14px; }
.rate-runner b { color: var(--art-gray-900); }

@media (max-width: 560px) {
  .oc-step-label { font-size: 9.5px; }
  .oc-actions { margin-left: 0; width: 100%; }
  .oc-actions .el-button { flex: 1; }
  .oc-time { margin-left: 0; width: 100%; }
  .auth-box b { font-size: 25px; letter-spacing: 4px; }
}
</style>