<template>
  <div class="ap">
    <PageHero tone="trace" icon="Guide" chip="CUSTODY LEDGER" live
      title="包裹去向台账"
      sub="每一次入库、上架、开箱查验、移架、催领、授权、代取与出库都会留痕：发生时间、具体地点、经手检查员与角色一并入账，可按节点、驿站、检查员与时间区间检索。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Plus" @click="openAdd()">人工登记</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="reload">刷新台账</el-button>
      </template>
    </PageHero>

    <div class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </div>

    <div class="ap-row wide-left">
      <PanelCard title="节点分布" note="全部去向条目按节点归类" icon="PieChart" accent="trace" :index="1">
        <div ref="nodeRef" class="ap-chart" />
        <div class="node-legend">
          <button v-for="n in nodeRows" :key="n.name" class="nl-item" :class="{ on: query.node === n.name }"
                  :style="{ '--nc': n.color }" @click="pickNode(n.name)">
            <i class="nl-dot" />
            <span class="nl-name">{{ n.label }}</span>
            <b class="nl-val">{{ n.value }}</b>
            <span class="nl-pct">{{ n.pct }}%</span>
          </button>
        </div>
      </PanelCard>

      <PanelCard title="检查员榜" note="按经手条目排序" icon="Avatar" accent="mint" :index="2">
        <div v-if="inspectors.length" class="ap-list">
          <div v-for="(p, i) in inspectors" :key="p.userId" class="ap-list-item insp"
               :style="{ '--la': rankColor(i) }">
            <div class="ap-list-avatar">{{ i + 1 }}</div>
            <div class="ap-list-body">
              <div class="ap-list-l1">
                <b>{{ p.realName }}</b>
                <el-tag size="small" effect="plain" round>{{ OPERATOR_ROLE[p.role] || p.role }}</el-tag>
              </div>
              <div class="ap-list-l2">
                查验 {{ p.inspections }} · 出库 {{ p.pickups }} · 最近 {{ fromNow(p.lastAt) }}
              </div>
              <div class="insp-bar"><i :style="{ width: inspPct(p) + '%', background: rankColor(i) }" /></div>
            </div>
            <div class="ap-list-right"><b class="insp-num">{{ p.total }}</b><span>条</span></div>
          </div>
        </div>
        <div v-else class="ap-empty"><el-empty description="暂无检查员记录" :image-size="70" /></div>
      </PanelCard>
    </div>

    <PanelCard title="最近流向" note="最新 12 条留痕" icon="Clock" accent="parcel" :index="3">
      <template #extra>
        <el-button class="ap-link" text @click="scrollLedger">查看完整台账<el-icon><ArrowRight /></el-icon></el-button>
      </template>
      <TraceTimeline :nodes="recent" show-parcel />
    </PanelCard>

    <!-- 台账 -->
    <div ref="ledgerRef">
      <PanelCard title="去向明细台账" :note="`共 ${total} 条`" icon="Tickets" accent="trace" :index="4">
        <template #extra>
          <el-button type="primary" size="small" :icon="Plus" @click="openAdd()">登记去向</el-button>
        </template>

        <div class="ap-toolbar">
          <div class="ap-field ap-grow">
            <label>关键词</label>
            <el-input v-model="query.keyword" placeholder="运单号 / 检查员 / 地点 / 物主" clearable
                      :prefix-icon="Search" @keyup.enter="search" @clear="search" />
          </div>
          <div class="ap-field">
            <label>驿站</label>
            <el-select v-model="query.stationId" placeholder="全部驿站" clearable style="width:170px" @change="search">
              <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
          <div class="ap-field">
            <label>时间区间</label>
            <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="→"
                            start-placeholder="开始" end-placeholder="结束" style="width:250px" @change="search" />
          </div>
          <div class="ap-actions">
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
          </div>
        </div>

        <div class="ap-seg node-seg">
          <button :class="{ on: !query.node }" @click="pickNode('')">全部节点<span class="n">{{ total }}</span></button>
          <button v-for="n in nodeRows" :key="n.name" :class="{ on: query.node === n.name }"
                  :style="{ '--nc': n.color }" @click="pickNode(n.name)">
            {{ n.label }}<span class="n">{{ n.value }}</span>
          </button>
        </div>

        <div v-loading="loading" class="ap-table-wrap">
          <table class="ap-table">
            <thead>
              <tr>
                <SortTh prop="happenedAt" label="发生时间" width="150px" :sort="sorter" @sort="search" />
                <SortTh prop="node" label="节点" width="130px" :sort="sorter" @sort="search" />
                <SortTh prop="title" label="事项 / 详情" :sort="sorter" @sort="search" />
                <SortTh prop="trackingNo" label="运单号" width="180px" :sort="sorter" @sort="search" />
                <SortTh prop="location" label="地点" width="200px" :sort="sorter" @sort="search" />
                <SortTh prop="operatorName" label="检查员" width="140px" :sort="sorter" @sort="search" />
                <th style="width:80px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in rows" :key="t.id" @click="openDetail(t)">
                <td class="ap-mono">{{ fmtTime(t.happenedAt) }}</td>
                <td>
                  <span class="node-chip" :style="{ '--nc': nodeColor(t.node) }">
                    <el-icon :size="12"><component :is="nodeIcon(t.node)" /></el-icon>{{ nodeLabel(t.node) }}
                  </span>
                </td>
                <td>
                  <b class="ap-strong">{{ t.title }}</b>
                  <div v-if="t.detail" class="ap-sub ap-ellipsis">{{ t.detail }}</div>
                </td>
                <td>
                  <span class="ap-mono">{{ t.trackingNo }}</span>
                  <div class="ap-sub">{{ courierName(t.courierCompany) }} · {{ t.ownerName || '—' }}</div>
                </td>
                <td class="ap-ellipsis">{{ t.location }}</td>
                <td>
                  <span class="op-cell">
                    <i class="op-dot" :style="{ background: roleColor(t.operatorRole) }" />
                    {{ t.operatorName || '系统' }}
                  </span>
                  <div class="ap-sub">{{ OPERATOR_ROLE[t.operatorRole] || '—' }}</div>
                </td>
                <td><el-button text type="primary" size="small" :icon="View" @click.stop="openDetail(t)">详情</el-button></td>
              </tr>
              <tr v-if="!rows.length && !loading">
                <td colspan="7"><div class="ap-empty"><el-empty description="没有符合条件的去向记录" :image-size="80" /></div></td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 移动端卡片 -->
        <div v-loading="loading" class="ap-cards">
          <article v-for="t in rows" :key="t.id" class="ap-card-item" :style="{ '--ci': nodeColor(t.node) }" @click="openDetail(t)">
            <div class="ap-card-top">
              <span class="node-chip" :style="{ '--nc': nodeColor(t.node) }">
                <el-icon :size="12"><component :is="nodeIcon(t.node)" /></el-icon>{{ nodeLabel(t.node) }}
              </span>
              <span class="ap-mono ap-sub">{{ fmtTime(t.happenedAt) }}</span>
            </div>
            <b class="ap-strong">{{ t.title }}</b>
            <div class="ap-card-grid">
              <div><label>运单号</label><span class="ap-mono">{{ t.trackingNo }}</span></div>
              <div><label>物主</label><span>{{ t.ownerName || '—' }}</span></div>
              <div><label>地点</label><span>{{ t.location }}</span></div>
              <div><label>检查员</label><span>{{ t.operatorName || '系统' }}（{{ OPERATOR_ROLE[t.operatorRole] || '—' }}）</span></div>
            </div>
            <p v-if="t.detail" class="card-detail">{{ t.detail }}</p>
          </article>
          <div v-if="!rows.length && !loading" class="ap-empty"><el-empty description="没有符合条件的去向记录" :image-size="80" /></div>
        </div>

        <template #foot>
          <div class="ap-pager">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                           :page-sizes="[10, 20, 50, 100]" @current-change="loadRows" @size-change="search" />
          </div>
        </template>
      </PanelCard>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailOpen" title="去向条目详情" size="540px" :with-header="true">
      <template v-if="current">
        <div class="dt-head" :style="{ '--nc': nodeColor(current.node) }">
          <div class="dt-icon"><el-icon :size="22"><component :is="nodeIcon(current.node)" /></el-icon></div>
          <div class="dt-txt">
            <b>{{ current.title }}</b>
            <span>{{ nodeLabel(current.node) }} · {{ fmtTime(current.happenedAt) }}</span>
          </div>
        </div>
        <div class="ap-kv">
          <div><label>运单号</label><span class="ap-mono">{{ current.trackingNo }}</span></div>
          <div><label>承运商</label><span>{{ courierName(current.courierCompany) }}</span></div>
          <div><label>物主</label><span>{{ current.ownerName || '—' }}</span></div>
          <div><label>包裹状态</label>
            <span><el-tag size="small" :type="dict(PARCEL_STATUS, current.parcelStatus).type" effect="light">{{ dict(PARCEL_STATUS, current.parcelStatus).label }}</el-tag></span>
          </div>
          <div><label>所属驿站</label><span>{{ current.stationName || '—' }}</span></div>
          <div><label>发生地点</label><span>{{ current.location }}</span></div>
          <div><label>检查员</label><span>{{ current.operatorName || '系统自动' }}</span></div>
          <div><label>经手角色</label><span>{{ OPERATOR_ROLE[current.operatorRole] || '—' }}</span></div>
          <div><label>发生时间</label><span class="ap-mono">{{ fmtTime(current.happenedAt) }}</span></div>
          <div><label>入账时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
        </div>
        <div v-if="current.detail" class="ap-note" style="--nc:#0d9488;margin-top:14px">
          <el-icon><Document /></el-icon><div>{{ current.detail }}</div>
        </div>
        <div class="dt-actions">
          <el-button type="primary" :icon="Plus" @click="openAdd(current)">为此包裹追加登记</el-button>
        </div>
        <h4 class="dt-h">该包裹完整去向链</h4>
        <div v-loading="chainLoading">
          <TraceTimeline :nodes="chain" />
        </div>
      </template>
    </el-drawer>

    <!-- 人工登记 -->
    <el-dialog v-model="addOpen" title="人工登记去向" width="560px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>选择包裹<em>*</em></label>
          <el-select v-model="form.parcelId" filterable remote reserve-keyword clearable
                     placeholder="输入运单号搜索" :remote-method="searchParcels" :loading="parcelLoading"
                     style="width:100%" @change="onParcelChange">
            <el-option v-for="p in parcelOptions" :key="p.id" :label="`${p.trackingNo} · ${p.ownerName || '—'}`" :value="p.id">
              <div class="po-line">
                <b class="ap-mono">{{ p.trackingNo }}</b>
                <span>{{ p.ownerName }} · {{ p.stationName }} · 货架 {{ p.shelfCode || '—' }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        <div v-if="chosenParcel" class="ap-kv chosen-kv">
          <div><label>承运商</label><span>{{ courierName(chosenParcel.courierCompany) }}</span></div>
          <div><label>当前状态</label><span>{{ dict(PARCEL_STATUS, chosenParcel.status).label }}</span></div>
          <div><label>所在驿站</label><span>{{ chosenParcel.stationName }}</span></div>
          <div><label>当前货架</label><span>{{ chosenParcel.shelfCode || '未上架' }}</span></div>
        </div>
        <div class="ap-field-row">
          <label>节点类型<em>*</em></label>
          <div class="node-pick">
            <button v-for="n in TRACE_MANUAL" :key="n.value" type="button"
                    :class="{ on: form.node === n.value }" :style="{ '--nc': nodeColor(n.value) }"
                    @click="form.node = n.value; onNodeChange()">
              <el-icon :size="15"><component :is="nodeIcon(n.value)" /></el-icon>{{ n.label }}
            </button>
          </div>
        </div>
        <div v-if="needShelf" class="ap-field-row">
          <label>目标货架<em>*</em></label>
          <el-select v-model="form.shelfId" placeholder="选择同驿站可用货架" style="width:100%" :loading="shelfLoading" @change="onShelfChange">
            <el-option v-for="s in shelves" :key="s.id" :label="`${s.code} · 占用 ${s.occupied}/${s.capacity}`"
                       :value="s.id" :disabled="s.status !== 1 || (s.occupied >= s.capacity && s.id !== chosenParcel?.shelfId)" />
          </el-select>
          <p class="ap-form-tip">移架与重新上架会同步更新包裹货架位置，并自动写入「由原货架移至本货架」的详情。</p>
        </div>
        <div class="ap-field-row">
          <label>发生地点<em>*</em></label>
          <el-input v-model="form.location" maxlength="255" show-word-limit placeholder="如：一号驿站 · A 区查验台" />
        </div>
        <div class="ap-field-row">
          <label>事项标题</label>
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="留空则使用节点默认标题" />
        </div>
        <div class="ap-field-row">
          <label>详情说明</label>
          <el-input v-model="form.detail" type="textarea" :rows="3" maxlength="512" show-word-limit
                    placeholder="开箱查验请写明外观、封条、重量异常等情况；将同步推送提醒给物主。" />
        </div>
        <div class="ap-note" style="--nc:#f59e0b">
          <el-icon><InfoFilled /></el-icon>
          <div>登记后系统自动记录<b>当前登录员工为检查员</b>、服务器时间与所选地点，条目不可修改删除，确保台账可追溯。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="addOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" :icon="Check" @click="submitAdd">提交登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search, Check, ArrowRight, View } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import TraceTimeline from '../../components/TraceTimeline.vue'
import SortTh from '../../components/SortTh.vue'
import { tracePage, traceOverview, addTrace, parcelTraces, parcelDetail, listStations, listShelves, pageParcels } from '../../api'
import { TRACE_NODE, TRACE_MANUAL, OPERATOR_ROLE, PARCEL_STATUS, dict, fmtTime, fromNow, courierName } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { useChart, useChartStyle, CHART_COLORS } from '../../composables/useChart'

const { isDark, chartText, headText, axisStyle, tipStyle, tipRow } = useChartStyle()

const ov = ref({ stats: {}, nodes: [], inspectors: [], recent: [] })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stations = ref([])
const range = ref(null)
const ledgerRef = ref(null)
const nodeRef = ref(null)

const query = reactive({ page: 1, size: 10, node: '', stationId: null, keyword: '' })
const sorter = useServerSort('happenedAt')

const stats = computed(() => ov.value.stats || {})
const recent = computed(() => ov.value.recent || [])
const inspectors = computed(() => ov.value.inspectors || [])
const nodeRows = computed(() => {
  const list = (ov.value.nodes || []).map(n => ({
    name: n.name, value: Number(n.value), label: nodeLabel(n.name), color: nodeColor(n.name)
  }))
  const sum = list.reduce((s, n) => s + n.value, 0) || 1
  return list.map(n => ({ ...n, pct: Math.round((n.value / sum) * 1000) / 10 }))
})
const heroStats = computed(() => [
  { label: '台账条目', value: Number(stats.value.total || 0) },
  { label: '今日登记', value: Number(stats.value.today || 0) },
  { label: '开箱查验', value: Number(stats.value.inspections || 0) },
  { label: '覆盖包裹', value: Number(stats.value.parcels || 0) }
])
const metrics = computed(() => [
  { label: '累计去向条目', value: Number(stats.value.total || 0), foot: '全部节点合计', color: '#0d9488', icon: 'Tickets' },
  { label: '今日新增留痕', value: Number(stats.value.today || 0), foot: '含系统自动写入', color: '#5b7cfa', icon: 'Calendar' },
  { label: '开箱查验次数', value: Number(stats.value.inspections || 0), foot: `检查员 ${stats.value.inspectors || 0} 人`, color: '#f59e0b', icon: 'Search' },
  { label: '涉及地点数', value: Number(stats.value.locations || 0), foot: `覆盖 ${stats.value.parcels || 0} 件包裹`, color: '#8b5cf6', icon: 'LocationInformation' }
])

function nodeLabel(n) { return TRACE_NODE[n]?.label || n || '—' }
function nodeIcon(n) { return TRACE_NODE[n]?.icon || 'MoreFilled' }
function nodeColor(n) { return TRACE_NODE[n]?.color || '#7987a1' }
function roleColor(r) {
  return { STAFF: '#5b7cfa', ADMIN: '#8b5cf6', COURIER: '#f59e0b', STUDENT: '#0ea5e9', SYSTEM: '#7987a1' }[r] || '#7987a1'
}
function rankColor(i) { return CHART_COLORS[i % CHART_COLORS.length] }
function inspPct(p) {
  const max = Number(inspectors.value[0]?.total || 1)
  return Math.max(8, Math.round((Number(p.total) / max) * 100))
}

/* ---- 节点分布环图 ---- */
useChart(nodeRef, chart => {
  const data = nodeRows.value
  const sum = data.reduce((s, d) => s + d.value, 0)
  const narrow = (nodeRef.value?.clientWidth || 500) < 420
  chart.setOption({
    color: CHART_COLORS,
    tooltip: {
      trigger: 'item', ...tipStyle(),
      formatter: p => tipRow(p.color, p.name, `${p.value} 条`) +
        `<div style="font-size:11.5px;opacity:0.6;margin-top:4px;text-align:right;">占比 ${p.percent}%</div>`
    },
    title: {
      text: String(sum), subtext: '去向条目',
      left: 'center', top: narrow ? '32%' : '34%',
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

/* ---- 数据加载 ---- */
async function loadOverview() {
  ov.value = await traceOverview()
}
async function loadRows() {
  loading.value = true
  try {
    const r = await tracePage({
      page: query.page, size: query.size,
      node: query.node || undefined,
      stationId: query.stationId || undefined,
      keyword: query.keyword?.trim() || undefined,
      from: range.value?.[0] || undefined,
      to: range.value?.[1] || undefined,
      ...sorter.sortParams()
    })
    rows.value = r.records || []
    total.value = Number(r.total || 0)
  } finally { loading.value = false }
}
function search() { query.page = 1; loadRows() }
function resetQuery() {
  query.node = ''; query.stationId = null; query.keyword = ''; range.value = null
  search()
}
function pickNode(n) { query.node = query.node === n ? '' : n; search() }
function scrollLedger() { ledgerRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
async function reload() { await Promise.all([loadOverview(), loadRows()]); ElMessage.success('台账已刷新') }

/* ---- 详情抽屉 ---- */
const detailOpen = ref(false)
const current = ref(null)
const chain = ref([])
const chainLoading = ref(false)
async function openDetail(t) {
  current.value = t
  detailOpen.value = true
  chain.value = []
  chainLoading.value = true
  try { chain.value = await parcelTraces(t.parcelId) } catch { chain.value = [] } finally { chainLoading.value = false }
}

/* ---- 人工登记 ---- */
const addOpen = ref(false)
const saving = ref(false)
const parcelLoading = ref(false)
const parcelOptions = ref([])
const shelfLoading = ref(false)
const shelves = ref([])
const form = reactive({ parcelId: null, node: 'INSPECT', location: '', title: '', detail: '', shelfId: null })
const chosenParcel = computed(() => parcelOptions.value.find(p => p.id === form.parcelId) || null)
const needShelf = computed(() => form.node === 'TRANSFER' || form.node === 'SHELVE')

async function searchParcels(kw) {
  parcelLoading.value = true
  try {
    const r = await pageParcels({ page: 1, size: 20, keyword: kw?.trim() || undefined })
    parcelOptions.value = r.records || []
  } finally { parcelLoading.value = false }
}
async function openAdd(t) {
  addOpen.value = true
  form.node = 'INSPECT'; form.title = ''; form.detail = ''; form.shelfId = null; form.location = ''
  if (t?.parcelId) {
    form.parcelId = t.parcelId
    form.location = t.location || ''
    parcelOptions.value = []
    const p = await parcelDetail(t.parcelId).catch(() => null)
    parcelOptions.value = p ? [p] : [{
      id: t.parcelId, trackingNo: t.trackingNo, ownerName: t.ownerName,
      courierCompany: t.courierCompany, status: t.parcelStatus, stationName: t.stationName
    }]
    if (p) form.location = `${p.stationName || '驿站'} · ${p.shelfCode ? '货架 ' + p.shelfCode : '收货台'}`
    await loadShelves()
  } else {
    form.parcelId = null
    parcelOptions.value = []
    await searchParcels('')
  }
}
async function loadShelves() {
  const sid = chosenParcel.value?.stationId
  shelves.value = []
  if (!sid) return
  shelfLoading.value = true
  try { shelves.value = await listShelves(sid) } finally { shelfLoading.value = false }
}
async function onParcelChange() {
  form.shelfId = null
  const p = chosenParcel.value
  form.location = p ? `${p.stationName || '驿站'} · ${p.shelfCode ? '货架 ' + p.shelfCode : '收货台'}` : ''
  await loadShelves()
}
function onNodeChange() {
  form.shelfId = null
  if (!needShelf.value && chosenParcel.value) onParcelChange()
}
function onShelfChange() {
  const s = shelves.value.find(x => x.id === form.shelfId)
  if (s && chosenParcel.value) form.location = `${chosenParcel.value.stationName} · 货架 ${s.code}`
}
async function submitAdd() {
  if (!form.parcelId) return ElMessage.warning('请选择包裹')
  if (needShelf.value && !form.shelfId) return ElMessage.warning('请选择目标货架')
  if (!form.location.trim()) return ElMessage.warning('请填写发生地点')
  saving.value = true
  try {
    await addTrace({
      parcelId: form.parcelId, node: form.node, location: form.location.trim(),
      title: form.title.trim() || undefined, detail: form.detail.trim() || undefined,
      shelfId: needShelf.value ? form.shelfId : undefined
    })
    ElMessage.success('去向已登记，检查员与时间已自动留痕')
    addOpen.value = false
    await Promise.all([loadOverview(), loadRows()])
  } catch (e) { ElMessage.error(e?.message || '登记失败') } finally { saving.value = false }
}

onMounted(async () => {
  stations.value = await listStations().catch(() => [])
  await Promise.all([loadOverview(), loadRows(), searchParcels('')])
})
</script>

<style scoped>
.node-legend { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 148px), 1fr)); gap: 7px; margin-top: 12px; }
.nl-item {
  display: flex; align-items: center; gap: 7px;
  padding: 7px 10px; border-radius: 10px; cursor: pointer;
  border: 1px solid var(--default-border);
  background: var(--art-gray-100);
  font-size: 12px; color: var(--art-gray-700);
  transition: all 0.22s var(--flair-ease);
}
html.dark .nl-item { background: #1a1a20; }
.nl-item:hover { border-color: var(--nc); transform: translateY(-1px); }
.nl-item.on { border-color: var(--nc); background: color-mix(in srgb, var(--nc) 12%, transparent); color: var(--art-gray-900); font-weight: 700; }
.nl-dot { width: 8px; height: 8px; border-radius: 3px; background: var(--nc); flex-shrink: 0; }
.nl-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.nl-val { font-variant-numeric: tabular-nums; color: var(--art-gray-900); }
.nl-pct { font-size: 10.5px; color: var(--art-gray-500); }

.insp .ap-list-avatar { font-size: 14px; font-weight: 800; }
.insp-num { font-size: 17px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.ap-list-right span { display: block; font-size: 10.5px; color: var(--art-gray-500); }
.insp-bar { height: 4px; border-radius: 3px; background: var(--art-gray-200); overflow: hidden; margin-top: 6px; }
html.dark .insp-bar { background: #26262e; }
.insp-bar i { display: block; height: 100%; border-radius: 3px; transition: width 0.7s var(--flair-ease); }

.node-seg { flex-wrap: wrap; margin-bottom: 12px; }
.node-seg button.on { border-color: var(--nc, var(--flair-primary)); color: var(--nc, var(--flair-primary)); }

.node-chip {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11.5px; font-weight: 700; white-space: nowrap;
  color: var(--nc);
  background: color-mix(in srgb, var(--nc) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--nc) 32%, transparent);
  padding: 3px 9px; border-radius: 999px;
}
.op-cell { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; color: var(--art-gray-800); }
.op-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.card-detail { margin: 0; font-size: 12px; line-height: 1.6; color: var(--art-gray-600); }
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
.dt-txt b { display: block; font-size: 17px; font-weight: 800; color: var(--art-gray-900); }
.dt-txt span { display: block; font-size: 12px; color: var(--art-gray-500); margin-top: 3px; }
.dt-actions { margin: 16px 0 4px; }
.dt-h { margin: 20px 0 10px; font-size: 11.5px; font-weight: 800; letter-spacing: 1.2px; color: var(--art-gray-500); text-transform: uppercase; }

/* 登记表单 */
.chosen-kv { margin: -4px 0 4px; padding: 11px 13px; border-radius: 11px; background: var(--art-gray-100); }
html.dark .chosen-kv { background: #1a1a20; }
.node-pick { display: grid; grid-template-columns: repeat(auto-fit, minmax(112px, 1fr)); gap: 8px; }
.node-pick button {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 8px; border-radius: 11px; cursor: pointer;
  border: 1.5px solid var(--default-border);
  background: var(--default-box-color);
  font-size: 12.5px; font-weight: 600; color: var(--art-gray-700);
  transition: all 0.22s var(--flair-ease);
}
.node-pick button:hover { border-color: var(--nc); color: var(--nc); transform: translateY(-1px); }
.node-pick button.on {
  border-color: var(--nc); color: #fff;
  background: linear-gradient(135deg, var(--nc), color-mix(in srgb, var(--nc) 62%, #ffffff));
  box-shadow: 0 8px 18px -10px var(--nc);
}
.po-line { display: flex; flex-direction: column; line-height: 1.45; padding: 2px 0; }
.po-line span { font-size: 11.5px; color: var(--art-gray-500); }

@media (max-width: 560px) {
  .node-pick { grid-template-columns: repeat(2, 1fr); }
}
</style>