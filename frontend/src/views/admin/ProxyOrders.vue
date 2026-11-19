<template>
  <div class="ap">
    <PageHero tone="proxy" icon="Bicycle" chip="SERVICE SUPERVISION" live
      title="代取服务监管"
      sub="校园跑腿代取市场的全量服务单监管：托管资金、抽成结算、履约时效与评分一览。管理员可强制关闭异常单并全额退款，或对已送达单代为确认结算。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="reload">刷新监管数据</el-button>
        <el-button type="default" :icon="Setting" @click="$router.push('/admin/configs')">费率策略</el-button>
      </template>
    </PageHero>

    <div class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </div>

    <div class="ap-note" style="--nc:#0ea5e9">
      <el-icon><InfoFilled /></el-icon>
      <div>
        当前规则：服务费 <b>{{ money(ov.feeMin) }} ~ {{ money(ov.feeMax) }}</b>，
        平台抽成 <b>{{ ov.commissionPercent }}%</b>，最长履约 <b>{{ ov.maxHours }} 小时</b>。
        发布时全额托管，完成结算后跑腿员实得「服务费 + 小费 − 抽成」，关闭则原路全额退回。
      </div>
    </div>

    <div class="ap-row wide-left">
      <PanelCard title="服务单状态分布" note="全量单据" icon="PieChart" accent="proxy" :index="1">
        <div ref="distRef" class="ap-chart" />
        <div class="dist-legend">
          <button v-for="d in distRows" :key="d.name" class="dl-item" :class="{ on: query.status === d.name }"
                  :style="{ '--nc': d.color }" @click="pickStatus(d.name)">
            <i /><span>{{ d.label }}</span><b>{{ d.value }}</b><em>{{ d.pct }}%</em>
          </button>
        </div>
      </PanelCard>

      <PanelCard title="跑腿达人榜" note="按完成单量排序" icon="Trophy" accent="mint" :index="2">
        <div v-if="runners.length" class="ap-list">
          <div v-for="(r, i) in runners" :key="r.userId" class="ap-list-item runner" :style="{ '--la': rankColor(i) }">
            <div class="ap-list-avatar">{{ i + 1 }}</div>
            <div class="ap-list-body">
              <div class="ap-list-l1">
                <b>{{ r.realName }}</b>
                <span class="ap-mono r-uname">{{ r.username }}</span>
              </div>
              <div class="ap-list-l2">
                <el-rate :model-value="Number(r.avgRating || 0)" disabled size="small" allow-half />
                <span class="r-score">{{ Number(r.avgRating || 0).toFixed(2) }}</span>
              </div>
            </div>
            <div class="ap-list-right">
              <b class="r-count">{{ r.doneCount }} 单</b>
              <span class="r-income">{{ money(r.income) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="ap-empty"><el-empty description="暂无完成的代取服务" :image-size="70" /></div>
      </PanelCard>
    </div>

    <!-- 服务单列表 -->
    <PanelCard title="服务单监管台账" :note="`共 ${total} 单`" icon="Tickets" accent="proxy" :index="3">
      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" placeholder="服务单号 / 运单号 / 发布人 / 跑腿员" clearable :prefix-icon="Search"
                    @keyup.enter="search" @clear="search" />
        </div>
        <div class="ap-field">
          <label>驿站</label>
          <el-select v-model="query.stationId" placeholder="全部驿站" clearable style="width:170px" @change="search">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="ap-seg">
        <button :class="{ on: !query.status }" @click="pickStatus('')">全部<span class="n">{{ total }}</span></button>
        <button v-for="(v, k) in ORDER_STATUS" :key="k" :class="{ on: query.status === k }"
                :style="{ '--nc': v.color }" @click="pickStatus(k)">{{ v.label }}</button>
      </div>

      <div v-loading="listLoading" class="ap-table-wrap">
        <table class="ap-table">
          <thead>
            <tr>
              <SortTh prop="orderNo" label="服务单号" width="180px" :sort="sorter" @sort="search" />
              <SortTh prop="status" label="状态" width="100px" :sort="sorter" @sort="search" />
              <SortTh prop="trackingNo" label="包裹" width="180px" :sort="sorter" @sort="search" />
              <SortTh prop="requesterName" label="发布人 → 跑腿员" width="150px" :sort="sorter" @sort="search" />
              <SortTh prop="total" label="托管金额" width="120px" :sort="sorter" @sort="search" />
              <SortTh prop="commission" label="平台抽成" width="110px" :sort="sorter" @sort="search" />
              <SortTh prop="createdAt" label="时效" width="130px" :sort="sorter" @sort="search" />
              <SortTh prop="rating" label="评分" width="90px" :sort="sorter" @sort="search" />
              <th style="width:170px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in rows" :key="o.id" @click="openDetail(o)">
              <td>
                <span class="ap-mono">{{ o.orderNo }}</span>
                <div class="ap-sub">{{ fmtTime(o.createdAt) }}</div>
              </td>
              <td><span class="st-chip" :style="{ '--nc': statusColor(o.status) }">{{ statusLabel(o.status) }}</span></td>
              <td>
                <span class="ap-mono">{{ o.trackingNo }}</span>
                <div class="ap-sub">{{ courierName(o.courierCompany) }} · {{ o.stationName }}</div>
              </td>
              <td>
                <b class="ap-strong">{{ o.requesterName }}</b>
                <div class="ap-sub">{{ o.runnerName ? '→ ' + o.runnerName : '→ 待接单' }}</div>
              </td>
              <td><span class="ap-money bare">{{ money(o.total) }}</span></td>
              <td class="ap-mono">{{ money(o.commission) }}</td>
              <td>
                <span v-if="isLive(o)" class="cd" :class="{ urgent: urgent(o) }">{{ countdown(o.expireAt) }}</span>
                <span v-else class="ap-sub">{{ o.doneAt ? fmtTime(o.doneAt) : fmtTime(o.cancelledAt) }}</span>
              </td>
              <td>
                <span v-if="o.rating" class="rate-cell"><el-icon :size="12"><StarFilled /></el-icon>{{ o.rating }}</span>
                <span v-else class="ap-sub">—</span>
              </td>
              <td @click.stop>
                <div class="ap-ops">
                  <el-button text type="primary" size="small" :icon="View" @click="openDetail(o)">详情</el-button>
                  <el-button v-if="o.status === 'DELIVERED'" text type="success" size="small" :icon="CircleCheck" @click="doConfirm(o)">代确认</el-button>
                  <el-button v-if="closable(o)" text type="danger" size="small" :icon="CircleClose" @click="openCancel(o)">关闭</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length && !listLoading">
              <td colspan="9"><div class="ap-empty"><el-empty description="没有匹配的服务单" :image-size="80" /></div></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-loading="listLoading" class="ap-cards">
        <article v-for="o in rows" :key="o.id" class="ap-card-item" :style="{ '--ci': statusColor(o.status) }" @click="openDetail(o)">
          <div class="ap-card-top">
            <span class="st-chip" :style="{ '--nc': statusColor(o.status) }">{{ statusLabel(o.status) }}</span>
            <span class="ap-mono ap-sub">{{ o.orderNo }}</span>
          </div>
          <b class="ap-strong ap-mono">{{ o.trackingNo }}</b>
          <div class="ap-card-grid">
            <div><label>发布人</label><span>{{ o.requesterName }}</span></div>
            <div><label>跑腿员</label><span>{{ o.runnerName || '待接单' }}</span></div>
            <div><label>托管金额</label><span class="ap-money bare">{{ money(o.total) }}</span></div>
            <div><label>平台抽成</label><span class="ap-mono">{{ money(o.commission) }}</span></div>
            <div><label>送达地址</label><span>{{ o.deliverTo }}</span></div>
            <div><label>时效</label><span>{{ isLive(o) ? countdown(o.expireAt) : fmtTime(o.doneAt || o.cancelledAt) }}</span></div>
          </div>
          <div class="ap-card-actions" @click.stop>
            <el-button size="small" type="primary" plain :icon="View" @click="openDetail(o)">详情</el-button>
            <el-button v-if="o.status === 'DELIVERED'" size="small" type="success" plain :icon="CircleCheck" @click="doConfirm(o)">代确认</el-button>
            <el-button v-if="closable(o)" size="small" type="danger" plain :icon="CircleClose" @click="openCancel(o)">强制关闭</el-button>
          </div>
        </article>
        <div v-if="!rows.length && !listLoading" class="ap-empty"><el-empty description="没有匹配的服务单" :image-size="80" /></div>
      </div>

      <template #foot>
        <div class="ap-pager">
          <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                         v-model:current-page="query.page" v-model:page-size="query.size"
                         :page-sizes="[10, 20, 50]" @current-change="loadRows" @size-change="search" />
        </div>
      </template>
    </PanelCard>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailOpen" title="服务单详情" size="560px">
      <template v-if="current">
        <div class="dt-head" :style="{ '--nc': statusColor(current.status) }">
          <div class="dt-icon"><el-icon :size="22"><Bicycle /></el-icon></div>
          <div class="dt-txt">
            <b class="ap-mono">{{ current.orderNo }}</b>
            <span>{{ statusLabel(current.status) }} · 发布于 {{ fmtTime(current.createdAt) }}</span>
          </div>
        </div>

        <div class="stepper">
          <div v-for="(s, i) in steps" :key="s.key" class="step" :class="stepClass(i)">
            <i class="s-dot"><el-icon :size="12"><component :is="s.icon" /></el-icon></i>
            <span>{{ s.label }}</span>
            <em v-if="s.at">{{ fmtTime(s.at) }}</em>
          </div>
        </div>

        <h4 class="dt-h">包裹与路线</h4>
        <div class="ap-kv">
          <div><label>运单号</label><span class="ap-mono">{{ current.trackingNo }}</span></div>
          <div><label>承运商</label><span>{{ courierName(current.courierCompany) }}</span></div>
          <div><label>包裹状态</label>
            <span><el-tag size="small" :type="dict(PARCEL_STATUS, current.parcelStatus).type" effect="light">{{ dict(PARCEL_STATUS, current.parcelStatus).label }}</el-tag></span>
          </div>
          <div><label>所在驿站</label><span>{{ current.stationName }}</span></div>
          <div><label>驿站位置</label><span>{{ current.stationLocation || '—' }}</span></div>
          <div><label>货架</label><span>{{ current.shelfCode || '—' }}</span></div>
          <div><label>送达地址</label><span>{{ current.deliverTo }}</span></div>
          <div><label>履约截止</label><span>{{ fmtTime(current.expireAt) }}</span></div>
        </div>

        <h4 class="dt-h">参与人</h4>
        <div class="people">
          <div class="person" style="--nc:#0ea5e9">
            <div class="p-avatar">{{ (current.requesterName || 'U').slice(0, 1) }}</div>
            <div class="p-info"><b>{{ current.requesterName }}</b><span>{{ current.requesterUsername }} · {{ current.requesterPhone || '—' }}</span><em>发布人 / 物主</em></div>
          </div>
          <el-icon class="p-arrow" :size="18"><Right /></el-icon>
          <div class="person" style="--nc:#10d9b5">
            <div class="p-avatar">{{ (current.runnerName || '?').slice(0, 1) }}</div>
            <div class="p-info"><b>{{ current.runnerName || '待接单' }}</b><span>{{ current.runnerUsername || '—' }} · {{ current.runnerPhone || '—' }}</span><em>跑腿员</em></div>
          </div>
        </div>
        <div v-if="current.authCode" class="ap-note" style="--nc:#8b5cf6;margin-top:12px">
          <el-icon><Postcard /></el-icon>
          <div>代取授权码 <b class="ap-mono">{{ current.authCode }}</b>（监管可见，用于柜台核验代取身份）</div>
        </div>

        <h4 class="dt-h">费用结算</h4>
        <div class="money-box">
          <div class="mb-row"><span>服务费</span><b>{{ money(current.fee) }}</b></div>
          <div class="mb-row"><span>小费</span><b>{{ money(current.tip) }}</b></div>
          <div class="mb-row"><span>平台抽成（{{ ov.commissionPercent }}%）</span><b class="minus">−{{ money(current.commission) }}</b></div>
          <div class="mb-total"><span>跑腿员实得</span><b>{{ money(current.runnerIncome) }}</b></div>
        </div>
        <div v-if="current.cancelReason" class="ap-note" style="--nc:#fb7185;margin-top:12px">
          <el-icon><CircleCloseFilled /></el-icon><div><b>关闭原因：</b>{{ current.cancelReason }}（托管金额已全额退回发布人钱包）</div>
        </div>
        <div v-if="current.rating" class="review-box">
          <div class="rb-head"><el-rate :model-value="Number(current.rating)" disabled size="small" /><b>{{ current.rating }}.0</b></div>
          <p>{{ current.review || '发布人未填写文字评价' }}</p>
        </div>
        <div v-if="current.note" class="ap-note" style="--nc:#0ea5e9;margin-top:12px">
          <el-icon><ChatDotSquare /></el-icon><div><b>发布备注：</b>{{ current.note }}</div>
        </div>

        <div class="dt-actions">
          <el-button v-if="current.status === 'DELIVERED'" type="success" :icon="Check" @click="doConfirm(current)">代为确认完成</el-button>
          <el-button v-if="closable(current)" type="danger" plain :icon="CircleClose" @click="openCancel(current)">强制关闭并退款</el-button>
        </div>

        <h4 class="dt-h">包裹去向链</h4>
        <div v-loading="chainLoading"><TraceTimeline :nodes="chain" /></div>
      </template>
    </el-drawer>

    <!-- 强制关闭 -->
    <el-dialog v-model="cancelOpen" title="强制关闭服务单" width="480px" append-to-body>
      <div class="ap-form">
        <div class="ap-note" style="--nc:#fb7185">
          <el-icon><WarningFilled /></el-icon>
          <div>关闭后托管金额 <b>{{ money(cancelTarget?.total) }}</b> 将全额退回发布人钱包，代取授权同步撤销，包裹去向写入「服务取消」节点。此操作不可撤销。</div>
        </div>
        <div class="ap-field-row">
          <label>服务单</label>
          <div class="cancel-target">
            <b class="ap-mono">{{ cancelTarget?.orderNo }}</b>
            <span>{{ cancelTarget?.trackingNo }} · {{ statusLabel(cancelTarget?.status) }}</span>
          </div>
        </div>
        <div class="ap-field-row">
          <label>关闭原因<em>*</em></label>
          <el-input v-model="cancelReason" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：跑腿员长时间未履约 / 发布人申诉 / 包裹已退回，将通知双方" />
          <div class="quick-reason">
            <button v-for="r in QUICK_REASONS" :key="r" type="button" @click="cancelReason = r">{{ r }}</button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelOpen = false">取消</el-button>
        <el-button type="danger" :loading="cancelling" :icon="CircleClose" @click="submitCancel">确认关闭并退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search, Setting, Check, CircleClose, Right, Bicycle, View, CircleCheck } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import TraceTimeline from '../../components/TraceTimeline.vue'
import SortTh from '../../components/SortTh.vue'
import {
  adminProxyOverview, adminProxyOrders, adminCancelProxyOrder, adminConfirmProxyOrder,
  parcelTraces, listStations
} from '../../api'
import { ORDER_STATUS, PARCEL_STATUS, dict, fmtTime, money, countdown, courierName } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { useChart, useChartStyle, CHART_COLORS } from '../../composables/useChart'

const { isDark, chartText, headText, tipStyle, tipRow } = useChartStyle()

const ov = ref({ stats: {}, distribution: [], topRunners: [], feeMin: 2, feeMax: 50, commissionPercent: 10, maxHours: 48 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const listLoading = ref(false)
const stations = ref([])
const distRef = ref(null)
const query = reactive({ page: 1, size: 10, status: '', stationId: null, keyword: '' })
const sorter = useServerSort('createdAt')

const stats = computed(() => ov.value.stats || {})
const runners = computed(() => ov.value.topRunners || [])
const distRows = computed(() => {
  const list = (ov.value.distribution || []).map(d => ({
    name: d.name, value: Number(d.value), label: statusLabel(d.name), color: statusColor(d.name)
  }))
  const sum = list.reduce((s, d) => s + d.value, 0) || 1
  return list.map(d => ({ ...d, pct: Math.round((d.value / sum) * 1000) / 10 }))
})
const heroStats = computed(() => [
  { label: '服务单总量', value: Number(stats.value.total || 0), unit: '单' },
  { label: '履约中', value: Number(stats.value.running || 0), unit: '单' },
  { label: '已完成', value: Number(stats.value.done || 0), unit: '单' },
  { label: '平台佣金', value: Number(stats.value.commission || 0), decimals: 2, unit: '元' }
])
const metrics = computed(() => [
  { label: '累计服务单', value: Number(stats.value.total || 0), foot: `今日新增 ${stats.value.today || 0} 单`, color: '#0ea5e9', icon: 'ShoppingBag' },
  { label: '待接单', value: Number(stats.value.open || 0), foot: '大厅公开招募中', color: '#f59e0b', icon: 'Bell' },
  { label: '履约进行中', value: Number(stats.value.running || 0), foot: '已接单 / 已取件 / 已送达', color: '#8b5cf6', icon: 'Bicycle' },
  { label: '已完成结算', value: Number(stats.value.done || 0), foot: `已关闭 ${stats.value.closed || 0} 单`, color: '#10d9b5', icon: 'CircleCheck' },
  { label: '成交总额 GMV', value: Number(stats.value.gmv || 0), decimals: 2, unit: '元', foot: '含服务费与小费', color: '#5b7cfa', icon: 'Money' },
  { label: '平均评分', value: Number(stats.value.avgRating || 0), decimals: 2, foot: `${stats.value.runners || 0} 位跑腿员参与`, color: '#fb7185', icon: 'StarFilled' }
])

function statusLabel(s) { return ORDER_STATUS[s]?.label || s || '—' }
function statusColor(s) { return ORDER_STATUS[s]?.color || '#7987a1' }
function rankColor(i) { return CHART_COLORS[i % CHART_COLORS.length] }
function isLive(o) { return ['OPEN', 'ACCEPTED', 'PICKED', 'DELIVERED'].includes(o.status) }
function closable(o) { return isLive(o) }
function urgent(o) {
  const t = new Date(String(o.expireAt).replace(' ', 'T')).getTime()
  return Number.isFinite(t) && t - Date.now() < 3600000
}

useChart(distRef, chart => {
  const data = distRows.value
  const sum = data.reduce((s, d) => s + d.value, 0)
  const narrow = (distRef.value?.clientWidth || 500) < 420
  chart.setOption({
    tooltip: {
      trigger: 'item', ...tipStyle(),
      formatter: p => tipRow(p.color, p.name, `${p.value} 单`) +
        `<div style="font-size:11.5px;opacity:0.6;margin-top:4px;text-align:right;">占比 ${p.percent}%</div>`
    },
    title: {
      text: String(sum), subtext: '服务单总量', left: 'center', top: narrow ? '32%' : '34%',
      textStyle: { fontSize: narrow ? 21 : 27, fontWeight: 800, color: headText() },
      subtextStyle: { fontSize: narrow ? 10 : 12, color: chartText() }
    },
    animationDuration: 1000, animationEasing: 'cubicOut',
    series: [{
      type: 'pie', radius: narrow ? ['46%', '66%'] : ['52%', '72%'], center: ['50%', '42%'],
      itemStyle: { borderRadius: 6, borderColor: isDark() ? '#161618' : '#fff', borderWidth: 3 },
      label: { show: false }, labelLine: { show: false },
      emphasis: { scaleSize: 7, itemStyle: { shadowBlur: 18, shadowColor: isDark() ? 'rgba(0,0,0,0.55)' : 'rgba(20,40,60,0.25)' } },
      data: data.map(d => ({ name: d.label, value: d.value, itemStyle: { color: d.color } }))
    }]
  }, true)
})

async function loadOverview() { ov.value = await adminProxyOverview() }
async function loadRows() {
  listLoading.value = true
  try {
    const r = await adminProxyOrders({
      page: query.page, size: query.size,
      status: query.status || undefined, stationId: query.stationId || undefined,
      keyword: query.keyword?.trim() || undefined,
      ...sorter.sortParams()
    })
    rows.value = r.records || []
    total.value = Number(r.total || 0)
  } finally { listLoading.value = false }
}
function search() { query.page = 1; loadRows() }
function resetQuery() { query.status = ''; query.stationId = null; query.keyword = ''; search() }
function pickStatus(s) { query.status = query.status === s ? '' : s; search() }
async function reload() {
  loading.value = true
  try { await Promise.all([loadOverview(), loadRows()]); ElMessage.success('监管数据已刷新') } finally { loading.value = false }
}

/* ---- 详情 ---- */
const detailOpen = ref(false)
const current = ref(null)
const chain = ref([])
const chainLoading = ref(false)
const steps = computed(() => {
  const o = current.value
  if (!o) return []
  return [
    { key: 'open', label: '发布', icon: 'ShoppingBag', at: o.createdAt },
    { key: 'accept', label: '接单', icon: 'Bicycle', at: o.acceptedAt },
    { key: 'pick', label: '取件', icon: 'Box', at: o.pickedAt },
    { key: 'deliver', label: '送达', icon: 'Van', at: o.deliveredAt },
    { key: 'done', label: '完成', icon: 'Money', at: o.doneAt }
  ]
})
const dead = computed(() => ['CANCELLED', 'EXPIRED'].includes(current.value?.status))
function stepClass(i) {
  const at = steps.value[i]?.at
  if (dead.value) return at ? 'done dead' : 'dead'
  if (at) {
    const next = steps.value[i + 1]
    return next && !next.at ? 'now' : 'done'
  }
  return ''
}
async function openDetail(o) {
  current.value = o
  detailOpen.value = true
  chain.value = []
  chainLoading.value = true
  try { chain.value = await parcelTraces(o.parcelId) } catch { chain.value = [] } finally { chainLoading.value = false }
}

async function doConfirm(o) {
  try {
    await ElMessageBox.confirm(
      `服务单 ${o.orderNo} 已送达但发布人未确认。代为确认后将立即结算：跑腿员 ${o.runnerName} 入账 ${money(o.runnerIncome)}，平台抽成 ${money(o.commission)}。`,
      '代为确认完成', { type: 'warning', confirmButtonText: '确认结算' }
    )
  } catch { return }
  try {
    const r = await adminConfirmProxyOrder(o.id)
    ElMessage.success('已代为确认并完成结算')
    if (detailOpen.value) current.value = r
    await Promise.all([loadOverview(), loadRows()])
  } catch (e) { ElMessage.error(e?.message || '确认失败') }
}

/* ---- 强制关闭 ---- */
const QUICK_REASONS = ['跑腿员超时未履约', '发布人申诉取消', '包裹已退回承运商', '疑似违规交易']
const cancelOpen = ref(false)
const cancelling = ref(false)
const cancelTarget = ref(null)
const cancelReason = ref('')
function openCancel(o) { cancelTarget.value = o; cancelReason.value = ''; cancelOpen.value = true }
async function submitCancel() {
  if (!cancelReason.value.trim()) return ElMessage.warning('请填写关闭原因')
  cancelling.value = true
  try {
    await adminCancelProxyOrder(cancelTarget.value.id, cancelReason.value.trim())
    ElMessage.success('服务单已关闭，托管金额已全额退回')
    cancelOpen.value = false
    if (detailOpen.value && current.value?.id === cancelTarget.value.id) detailOpen.value = false
    await Promise.all([loadOverview(), loadRows()])
  } catch (e) { ElMessage.error(e?.message || '关闭失败') } finally { cancelling.value = false }
}

onMounted(async () => {
  stations.value = await listStations().catch(() => [])
  await Promise.all([loadOverview(), loadRows()])
})
</script>

<style scoped>
.dist-legend { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 140px), 1fr)); gap: 7px; margin-top: 12px; }
.dl-item {
  display: flex; align-items: center; gap: 7px;
  padding: 7px 10px; border-radius: 10px; cursor: pointer;
  border: 1px solid var(--default-border); background: var(--art-gray-100);
  font-size: 12px; color: var(--art-gray-700);
  transition: all 0.22s var(--flair-ease);
}
html.dark .dl-item { background: #1a1a20; }
.dl-item:hover { border-color: var(--nc); transform: translateY(-1px); }
.dl-item.on { border-color: var(--nc); background: color-mix(in srgb, var(--nc) 12%, transparent); font-weight: 700; color: var(--art-gray-900); }
.dl-item i { width: 8px; height: 8px; border-radius: 3px; background: var(--nc); flex-shrink: 0; }
.dl-item span { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dl-item b { font-variant-numeric: tabular-nums; color: var(--art-gray-900); }
.dl-item em { font-style: normal; font-size: 10.5px; color: var(--art-gray-500); }

.runner .ap-list-avatar { font-size: 14px; font-weight: 800; }
.r-uname { font-size: 11.5px; color: var(--art-gray-500); }
.ap-list-l2 { display: flex; align-items: center; gap: 7px; }
.r-score { font-size: 11.5px; font-weight: 700; color: #f59e0b; }
.r-count { display: block; font-size: 14px; font-weight: 800; color: var(--art-gray-900); }
.r-income { display: block; font-size: 11.5px; color: #0d9488; font-weight: 700; }
html.dark .r-income { color: #35e0c2; }

.st-chip {
  display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;
  font-size: 11.5px; font-weight: 700; color: var(--nc);
  background: color-mix(in srgb, var(--nc) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--nc) 32%, transparent);
  padding: 3px 9px; border-radius: 999px;
}
.cd { font-size: 12px; font-weight: 700; color: var(--art-gray-700); font-variant-numeric: tabular-nums; }
.cd.urgent { color: #e11d48; }
html.dark .cd.urgent { color: #ff8fa3; }
.rate-cell { display: inline-flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 700; color: #f59e0b; }
.ap-table tbody tr { cursor: pointer; }

/* 抽屉 */
.dt-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.dt-icon {
  width: 46px; height: 46px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; color: #fff;
  background: linear-gradient(135deg, var(--nc), color-mix(in srgb, var(--nc) 55%, #ffffff));
  box-shadow: 0 10px 20px -12px var(--nc);
}
.dt-txt { min-width: 0; }
.dt-txt b { display: block; font-size: 16px; font-weight: 800; color: var(--art-gray-900); word-break: break-all; }
.dt-txt span { display: block; font-size: 12px; color: var(--art-gray-500); margin-top: 3px; }
.dt-h { margin: 20px 0 10px; font-size: 11.5px; font-weight: 800; letter-spacing: 1.2px; color: var(--art-gray-500); text-transform: uppercase; }
.dt-actions { display: flex; gap: 9px; flex-wrap: wrap; margin-top: 18px; }

.stepper { display: flex; gap: 4px; padding: 13px 10px; border-radius: 12px; background: var(--art-gray-100); overflow-x: auto; }
html.dark .stepper { background: #1a1a20; }
.step { flex: 1; min-width: 62px; display: flex; flex-direction: column; align-items: center; gap: 4px; position: relative; text-align: center; }
.step::before { content: ''; position: absolute; top: 11px; left: -50%; width: 100%; height: 2px; background: var(--art-gray-300); }
html.dark .step::before { background: #2c2c36; }
.step:first-child::before { display: none; }
.step.done::before, .step.now::before { background: linear-gradient(90deg, #0ea5e9, #10d9b5); }
.s-dot {
  position: relative; z-index: 1;
  width: 24px; height: 24px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: var(--default-box-color); border: 2px solid var(--art-gray-300); color: var(--art-gray-500);
}
html.dark .s-dot { border-color: #33333e; }
.step.done .s-dot { background: linear-gradient(135deg, #0ea5e9, #10d9b5); border-color: transparent; color: #fff; }
.step.now .s-dot { border-color: #0ea5e9; color: #0ea5e9; box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.16); animation: art-breathe 1.8s infinite; }
.step.dead .s-dot { border-color: #fb7185; color: #fb7185; }
.step span { font-size: 11.5px; font-weight: 700; color: var(--art-gray-600); }
.step.done span, .step.now span { color: var(--art-gray-900); }
.step em { font-style: normal; font-size: 10px; color: var(--art-gray-500); line-height: 1.35; }

.people { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.person { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 180px; padding: 11px 12px; border-radius: 12px; border: 1px solid color-mix(in srgb, var(--nc) 30%, transparent); background: color-mix(in srgb, var(--nc) 7%, transparent); }
.p-avatar { width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 17px; font-weight: 800; color: #fff; background: linear-gradient(135deg, var(--nc), color-mix(in srgb, var(--nc) 55%, #ffffff)); }
.p-info { min-width: 0; }
.p-info b { display: block; font-size: 14px; font-weight: 700; color: var(--art-gray-900); }
.p-info span { display: block; font-size: 11px; color: var(--art-gray-500); margin-top: 1px; word-break: break-all; }
.p-info em { display: block; font-style: normal; font-size: 10.5px; font-weight: 700; color: var(--nc); margin-top: 3px; }
.p-arrow { color: var(--art-gray-400); flex-shrink: 0; }

.money-box { padding: 13px 15px; border-radius: 12px; background: var(--art-gray-100); border: 1px solid var(--default-border); }
html.dark .money-box { background: #1a1a20; }
.mb-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 12.5px; color: var(--art-gray-600); }
.mb-row b { font-variant-numeric: tabular-nums; color: var(--art-gray-900); }
.mb-row b.minus { color: #e11d48; }
html.dark .mb-row b.minus { color: #ff8fa3; }
.mb-total { display: flex; justify-content: space-between; align-items: baseline; margin-top: 9px; padding-top: 10px; border-top: 1px dashed var(--default-border); }
.mb-total span { font-size: 12.5px; font-weight: 700; color: var(--art-gray-700); }
.mb-total b { font-size: 23px; font-weight: 800; color: #0d9488; font-variant-numeric: tabular-nums; }
html.dark .mb-total b { color: #35e0c2; }

.review-box { margin-top: 13px; padding: 12px 14px; border-radius: 12px; background: rgba(245, 158, 11, 0.09); border: 1px solid rgba(245, 158, 11, 0.3); }
.rb-head { display: flex; align-items: center; gap: 9px; }
.rb-head b { font-size: 14px; font-weight: 800; color: #f59e0b; }
.review-box p { margin: 7px 0 0; font-size: 12.5px; line-height: 1.65; color: var(--art-gray-700); }

.cancel-target { display: flex; flex-direction: column; gap: 2px; padding: 10px 13px; border-radius: 11px; background: var(--art-gray-100); }
html.dark .cancel-target { background: #1a1a20; }
.cancel-target b { font-size: 14px; color: var(--art-gray-900); }
.cancel-target span { font-size: 11.5px; color: var(--art-gray-500); }
.quick-reason { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 9px; }
.quick-reason button {
  border: 1px solid var(--default-border); background: var(--art-gray-100);
  color: var(--art-gray-600); font-size: 11.5px; padding: 5px 10px; border-radius: 999px; cursor: pointer;
  transition: all 0.2s var(--flair-ease);
}
html.dark .quick-reason button { background: #1a1a20; }
.quick-reason button:hover { border-color: #fb7185; color: #e11d48; }

@media (max-width: 560px) {
  .p-arrow { display: none; }
  .person { min-width: 100%; }
}
</style>