<template>
  <div class="art-page dashboard">
    <!-- 欢迎横幅：流动渐变 + 白色半调 + 玻璃拟态统计 + 打字机 + 多图标分层视差 -->
    <section class="welcome flair-in" style="--i:0">
      <div class="hero-orbs" aria-hidden="true">
        <el-icon class="orb orb-1"><Box /></el-icon>
        <el-icon class="orb orb-2"><Van /></el-icon>
        <el-icon class="orb orb-3"><Bell /></el-icon>
        <el-icon class="orb orb-4"><Postcard /></el-icon>
        <el-icon class="orb orb-5"><Promotion /></el-icon>
        <i class="orb-ring ring-1" />
        <i class="orb-ring ring-2" />
      </div>
      <el-icon class="hero-glyph" aria-hidden="true"><Box /></el-icon>
      <div class="welcome-main">
        <span class="hero-chip"><i class="live-dot" />运营实况 · LIVE</span>
        <div class="hello">{{ greeting }}，{{ userName }}</div>
        <div class="quote"><TypeWriter :texts="quotes" loop :speed="80" :hold="2600" /></div>
        <div class="today">{{ todayText }} · 校园快递驿站运营工作台</div>
      </div>
      <div class="welcome-side">
        <div class="mini" v-for="m in miniStats" :key="m.label">
          <CountUp class="mini-num" :value="m.value" :duration="1300" />
          <span class="mini-label">{{ m.label }}</span>
        </div>
      </div>
    </section>

    <!-- 四大主指标：数字滚动 + 渐变图标 + 角落色雾 -->
    <section class="stat-grid">
      <div v-for="(s, idx) in stats" :key="s.label"
           class="art-card art-card-hover metric flair-in"
           :style="{ '--i': idx + 1, '--mc': s.color, '--mc-soft': s.bg }">
        <div class="metric-top">
          <span class="metric-label">{{ s.label }}</span>
          <div class="metric-icon">
            <el-icon :size="20"><component :is="s.icon" /></el-icon>
          </div>
        </div>
        <CountUp class="metric-num" :value="s.value" :duration="1200" :delay="idx * 90" />
        <div class="metric-foot">
          <span class="dot" :style="{ background: s.color }" />{{ s.foot }}
        </div>
        <div class="metric-bar" />
      </div>
    </section>

    <!-- 趋势 + 状态分布 -->
    <section class="chart-row">
      <el-card shadow="never" class="chart-card flair-in" style="--i:5">
        <template #header>
          <div class="chart-head">
            <b>出入库趋势 <span class="chart-note">按日统计</span></b>
            <el-radio-group v-model="days" size="small" @change="loadTrend">
              <el-radio-button :value="7">近7日</el-radio-button>
              <el-radio-button :value="14">近14日</el-radio-button>
              <el-radio-button :value="30">近30日</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendRef" class="chart" />
      </el-card>
      <el-card shadow="never" class="chart-card flair-in" style="--i:6">
        <template #header><b>包裹状态分布</b></template>
        <div ref="statusRef" class="chart" />
      </el-card>
    </section>

    <!-- 热力图 + 快递公司占比：占比卡纵向延伸，占据热力图右侧空间 -->
    <section class="heat-zone">
      <el-card shadow="never" class="chart-card heat-card flair-in" :class="heatMetric" style="--i:7">
      <template #header>
        <div class="chart-head">
          <b>时段热力图 <span class="chart-note">星期 × 小时 · 近 {{ heatWeeks }} 周</span></b>
          <div class="heat-tools">
            <el-radio-group v-model="heatMetric" size="small">
              <el-radio-button value="inbound">入库</el-radio-button>
              <el-radio-button value="outbound">出库</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="heatWeeks" size="small" @change="loadHeatmap">
              <el-radio-button :value="2">2周</el-radio-button>
              <el-radio-button :value="4">4周</el-radio-button>
              <el-radio-button :value="8">8周</el-radio-button>
              <el-radio-button :value="12">12周</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="heat-scroll">
        <table class="ms-heat ms-clock">
          <thead>
            <tr>
              <th class="ms-day-col"></th>
              <th v-for="h in 24" :key="h" class="ms-hour">
                <span v-if="(h - 1) % 3 === 0">{{ h - 1 }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, d) in heatGrid" :key="d">
              <th class="ms-day">周{{ DAY_NAMES[d] }}</th>
              <td v-for="c in row" :key="c.hour"
                  class="ms-clock-cell" :class="{ 'is-empty': c.value === 0 }"
                  :style="{ '--v': c.intensity }"
                  :title="`周${DAY_NAMES[d]} ${String(c.hour).padStart(2, '0')}:00 · ${c.value} 件`" />
            </tr>
          </tbody>
        </table>
      </div>
      <div class="heat-foot">
        <span class="heat-legend">
          少
          <i v-for="v in [0, 25, 50, 75, 100]" :key="v" class="ms-clock-cell legend-cell"
             :class="{ 'is-empty': v === 0 }" :style="{ '--v': v }" />
          多
        </span>
        <span class="heat-summary" v-if="heatPeak">
          峰值 <b>周{{ DAY_NAMES[heatPeak.dow] }} {{ String(heatPeak.hour).padStart(2, '0') }}:00</b> · {{ heatPeak.value }} 件
          · 合计 {{ heatTotal }} 件
        </span>
        <span class="heat-summary" v-else>该区间暂无{{ heatMetric === 'inbound' ? '入库' : '出库' }}记录</span>
      </div>
    </el-card>

      <el-card shadow="never" class="list-card courier-card flair-in" style="--i:8">
        <template #header><b>快递公司占比</b></template>
        <div class="courier-list">
          <div v-for="(c, i) in couriers" :key="c.name" class="courier-item">
            <div class="c-head">
              <span class="c-name">{{ c.name }}</span>
              <span class="c-val">{{ c.value }} 件 · {{ c.pct }}%</span>
            </div>
            <div class="flair-bar"><i :style="{ width: c.pct + '%', background: palette[i % palette.length] }" /></div>
          </div>
          <el-empty v-if="!couriers.length" description="暂无数据" :image-size="60" />
        </div>
        <div class="card-foot">共 {{ courierTotal }} 件在途与历史包裹 · {{ courierRows.length }} 家承运商（展示前 {{ couriers.length }}）</div>
      </el-card>
    </section>

    <!-- 底部两卡：到站动态 / 操作时间线 -->
    <section class="bottom-row">
      <el-card shadow="never" class="list-card flair-in" style="--i:9">
        <template #header>
          <div class="chart-head"><b>最新到站</b>
            <router-link class="more" to="/staff/parcels">全部包裹 →</router-link>
          </div>
        </template>
        <div class="parcel-list">
          <div v-for="p in parcels" :key="p.id || p.trackingNo" class="parcel-item">
            <div class="p-avatar" :style="{ background: statusMeta(p.status).bg, color: statusMeta(p.status).color }">
              <el-icon :size="17"><Box /></el-icon>
            </div>
            <div class="p-body">
              <div class="p-line1">
                <b class="p-no">{{ p.trackingNo }}</b>
                <el-tag size="small" :type="dict(PARCEL_STATUS, p.status).type" effect="light">
                  {{ dict(PARCEL_STATUS, p.status).label }}
                </el-tag>
              </div>
              <div class="p-line2">{{ p.ownerName || p.ownerUsername }} · {{ fmtTime(p.inboundTime) }}</div>
              <div class="flair-bar"><i :style="{ width: statusMeta(p.status).pct + '%', background: statusMeta(p.status).color }" /></div>
            </div>
          </div>
          <el-empty v-if="!parcels.length" description="暂无到站记录" :image-size="60" />
        </div>
      </el-card>

      <el-card shadow="never" class="list-card flair-in" style="--i:10">
        <template #header>
          <div class="chart-head"><b>系统动态</b>
            <router-link class="more" to="/admin/logs">审计日志 →</router-link>
          </div>
        </template>
        <div class="timeline">
          <div v-for="(l, i) in logs" :key="l.id || i" class="tl-item">
            <span class="tl-dot" :class="l.success === 1 ? 'ok' : 'fail'" />
            <div class="tl-body">
              <div class="tl-line1"><b>{{ l.operatorName || '系统' }}</b> {{ l.action }}</div>
              <div class="tl-line2">{{ prettyDetail(l) }}</div>
              <div class="tl-time">{{ fmtTime(l.createdAt) }}</div>
            </div>
          </div>
          <el-empty v-if="!logs.length" description="暂无动态" :image-size="60" />
        </div>
        <div class="card-foot">全量操作留痕 · 支持按模块检索与审计追溯</div>
      </el-card>
    </section>
  </div>
</template><script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { statsOverview, statsTrend, statsDistribution, statsHeatmap, pageLogs, pageParcels } from '../../api'
import { PARCEL_STATUS, dict, fmtTime } from '../../utils/format'
import { useThemeStore } from '../../stores/theme'
import { useUserStore } from '../../stores/user'
import TypeWriter from '../../components/TypeWriter.vue'
import CountUp from '../../components/CountUp.vue'
import { prettyDetail } from '../../utils/logDetail'

const theme = useThemeStore()
const userStore = useUserStore()
const ov = ref({})
const days = ref(7)
const trendRef = ref()
const statusRef = ref()
let trendChart = null, statusChart = null
let trendData = [], distData = {}
const logs = ref([])
const parcels = ref([])
const courierRows = ref([])

/* ---- 7×24 时段热力图 ---- */
const DAY_NAMES = ['一', '二', '三', '四', '五', '六', '日']
const heatMetric = ref('inbound')
const heatWeeks = ref(4)
const heatCells = ref([])
const heatGrid = computed(() => {
  const map = new Map(heatCells.value.map(c => [`${c.dow}-${c.hour}`, Number(c[heatMetric.value] || 0)]))
  const max = Math.max(1, ...heatCells.value.map(c => Number(c[heatMetric.value] || 0)))
  return Array.from({ length: 7 }, (_, dow) =>
    Array.from({ length: 24 }, (_, hour) => {
      const value = map.get(`${dow}-${hour}`) || 0
      return { hour, value, intensity: value === 0 ? 0 : Math.max(12, Math.round((value / max) * 100)) }
    }))
})
const heatTotal = computed(() => heatCells.value.reduce((s, c) => s + Number(c[heatMetric.value] || 0), 0))
const heatPeak = computed(() => {
  let best = null
  for (const c of heatCells.value) {
    const v = Number(c[heatMetric.value] || 0)
    if (v > 0 && (!best || v > best.value)) best = { dow: Number(c.dow), hour: Number(c.hour), value: v }
  }
  return best
})
async function loadHeatmap() {
  try {
    const r = await statsHeatmap(heatWeeks.value)
    heatCells.value = r.cells || []
  } catch { heatCells.value = [] }
}

const palette = ['#5b7cfa', '#8b5cf6', '#38bdf8', '#10d9b5', '#ffb020', '#fb7185', '#7987a1']

const userName = computed(() => userStore.user?.name || userStore.user?.username || '管理员')
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayText = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 · 星期${week}`
})
const quotes = [
  '出入库、滞留与授权数据每日汇总于此。',
  '滞留超期包裹已单独标记，可一键催领。',
  '代取授权凭码核销，操作记录全程留痕。',
  '切换时间区间，查看近 7 / 14 / 30 日趋势。'
]

const stats = computed(() => [
  { label: '今日入库', value: Number(ov.value.todayInbound ?? 0), foot: '实时扫码入库', color: '#5b7cfa', bg: 'rgba(91,124,250,0.12)', icon: 'Download' },
  { label: '今日出库', value: Number(ov.value.todayOutbound ?? 0), foot: '取件码 / 代取核销', color: '#10d9b5', bg: 'rgba(16,217,181,0.12)', icon: 'Upload' },
  { label: '在库待取', value: Number(ov.value.inStock ?? 0), foot: '货架实时库存', color: '#38bdf8', bg: 'rgba(56,189,248,0.12)', icon: 'Box' },
  { label: '滞留超期', value: Number(ov.value.overdue ?? 0), foot: '需优先催领', color: '#fb7185', bg: 'rgba(251,113,133,0.12)', icon: 'AlarmClock' }
])
const miniStats = computed(() => [
  { label: '生效授权', value: Number(ov.value.activeAuth ?? 0) },
  { label: '累计代取', value: Number(ov.value.usedAuth ?? 0) },
  { label: '注册用户', value: Number(ov.value.totalUsers ?? 0) }
])

const courierTotal = computed(() => courierRows.value.reduce((s, r) => s + Number(r.value), 0))

const COURIER_NAMES = { SF: '顺丰速运', ZT: '中通快递', YT: '圆通速递', YD: '韵达快递', ST: '申通快递', STO: '申通快递', JT: '极兔速递', JD: '京东物流', EMS: '邮政EMS', DB: '德邦快递' }
const couriers = computed(() => {
  const rows = courierRows.value.map(r => ({ name: COURIER_NAMES[r.name] || r.name, value: Number(r.value) }))
  const total = rows.reduce((s, r) => s + r.value, 0) || 1
  return rows.sort((a, b) => b.value - a.value).slice(0, 6)
    .map(r => ({ ...r, pct: Math.round((r.value / total) * 100) }))
})

/* 日志详情可读化：见 utils/logDetail.js（与日志管理页共用） */

function statusMeta(s) {
  return {
    IN_STOCK: { bg: 'rgba(91,124,250,0.12)', color: '#5b7cfa', pct: 55 },
    OVERDUE: { bg: 'rgba(251,113,133,0.12)', color: '#fb7185', pct: 100 },
    PICKED_UP: { bg: 'rgba(16,217,181,0.12)', color: '#10d9b5', pct: 100 },
    RETURNED: { bg: 'rgba(121,135,161,0.12)', color: '#7987a1', pct: 25 }
  }[s] || { bg: 'rgba(121,135,161,0.12)', color: '#7987a1', pct: 40 }
}
function chartText() { return theme.mode === 'dark' ? '#ababba' : '#7987a1' }
function axisStyle() {
  const c = chartText()
  const line = theme.mode === 'dark' ? '#2c2c36' : '#eceff3'
  return {
    axisLabel: { color: c, fontSize: 11, fontFamily: 'Inter, PingFang SC, sans-serif' },
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: { lineStyle: { color: line, type: [4, 4] } }
  }
}
/* 卡片式 tooltip：与页面卡片同底色、细边框、柔和投影 */
function tipStyle() {
  const dark = theme.mode === 'dark'
  return {
    backgroundColor: dark ? '#1c1c22' : '#ffffff',
    borderColor: dark ? '#33333e' : '#e6eaef',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: dark ? '#e8e8f2' : '#232338', fontSize: 12.5 },
    extraCssText: `border-radius:10px;box-shadow:0 10px 28px -8px rgba(15,18,35,${dark ? 0.65 : 0.16});`
  }
}
const tipDot = (color) => `<span style="display:inline-block;width:8px;height:8px;border-radius:2px;background:${color};margin-right:7px;"></span>`

function renderTrend() {
  const data = trendData
  const ax = axisStyle()
  const dark = theme.mode === 'dark'
  const area = (rgb) => new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: `rgba(${rgb},${dark ? 0.28 : 0.18})` },
    { offset: 1, color: `rgba(${rgb},0.01)` }
  ])
  const fmtDay = (v) => { const p = String(v).split('-'); return p.length === 3 ? `${+p[1]}/${+p[2]}` : v }
  trendChart.setOption({
    color: ['#5b7cfa', '#10d9b5'],
    tooltip: {
      trigger: 'axis', ...tipStyle(),
      axisPointer: { type: 'line', lineStyle: { color: dark ? '#4a4a58' : '#c9d2e0', type: [3, 3] } },
      formatter: (ps) => {
        const rows = ps.map(p =>
          `<div style="display:flex;align-items:center;margin-top:5px;">${tipDot(p.color)}<span>${p.seriesName}</span>` +
          `<b style="margin-left:auto;padding-left:24px;font-variant-numeric:tabular-nums;">${p.value} 件</b></div>`).join('')
        return `<div style="font-size:11.5px;opacity:0.6;">${ps[0]?.axisValue ?? ''}</div>${rows}`
      }
    },
    legend: {
      data: ['入库', '出库'], top: 0, right: 0,
      icon: 'roundRect', itemWidth: 10, itemHeight: 10, itemGap: 18,
      textStyle: { color: chartText(), fontSize: 12 }
    },
    grid: { left: 8, right: 32, top: 34, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category', boundaryGap: false, data: data.map(d => d.day),
      axisLabel: { ...ax.axisLabel, formatter: fmtDay, hideOverlap: true },
      axisLine: { lineStyle: { color: dark ? '#2c2c36' : '#eceff3' } }, axisTick: ax.axisTick
    },
    yAxis: { type: 'value', minInterval: 1, min: 0, splitNumber: 4, max: v => Math.max(10, Math.ceil((v.max * 1.2) / 10) * 10), ...ax },
    animationDuration: 1100,
    animationEasing: 'cubicOut',
    series: [
      { name: '入库', type: 'line', smooth: 0.4, symbol: 'circle', symbolSize: 7, showSymbol: false,
        data: data.map(d => Number(d.inbound)),
        itemStyle: { color: '#5b7cfa', borderColor: dark ? '#161618' : '#fff', borderWidth: 2 },
        lineStyle: { width: 2.5 },
        areaStyle: { color: area('91,124,250') }, emphasis: { focus: 'series' } },
      { name: '出库', type: 'line', smooth: 0.4, symbol: 'circle', symbolSize: 7, showSymbol: false,
        data: data.map(d => Number(d.outbound)),
        itemStyle: { color: '#10d9b5', borderColor: dark ? '#161618' : '#fff', borderWidth: 2 },
        lineStyle: { width: 2.5 },
        areaStyle: { color: area('16,217,181') }, emphasis: { focus: 'series' } }
    ]
  }, true)
}

function renderStatus() {
  const STATUS_COLOR = { IN_STOCK: '#5b7cfa', OVERDUE: '#fb7185', PICKED_UP: '#10d9b5', RETURNED: '#7987a1' }
  const rows = (distData.status || []).map(r => ({
    name: dict(PARCEL_STATUS, r.name).label, value: Number(r.value),
    itemStyle: { color: STATUS_COLOR[r.name] || '#8b5cf6' }
  }))
  const total = rows.reduce((s, r) => s + r.value, 0)
  const narrow = (statusRef.value?.clientWidth || 500) < 430
  /* 与状态标签同色系的语义配色，而非彩虹色板 */
  const dark = theme.mode === 'dark'
  statusChart.setOption({
    tooltip: {
      trigger: 'item', ...tipStyle(),
      formatter: (p) =>
        `<div style="display:flex;align-items:center;">${tipDot(p.color)}<span>${p.name}</span>` +
        `<b style="margin-left:auto;padding-left:24px;font-variant-numeric:tabular-nums;">${p.value} 件</b></div>` +
        `<div style="font-size:11.5px;opacity:0.6;margin-top:4px;text-align:right;">占比 ${p.percent}%</div>`
    },
    legend: {
      bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, itemGap: 14,
      textStyle: { color: chartText(), fontSize: 12 }
    },
    title: {
      text: String(total), subtext: '包裹总量',
      left: 'center', top: narrow ? '34%' : '36%',
      textStyle: { fontSize: narrow ? 20 : 26, fontWeight: 800, color: dark ? '#e8e8f2' : '#232338' },
      subtextStyle: { fontSize: narrow ? 10 : 12, color: chartText() }
    },
    animationDuration: 1000,
    animationEasing: 'cubicOut',
    series: [{
      type: 'pie',
      radius: narrow ? ['46%', '66%'] : ['52%', '72%'], center: ['50%', '44%'],
      itemStyle: { borderRadius: 6, borderColor: dark ? '#161618' : '#fff', borderWidth: 3 },
      label: narrow
        ? { position: 'inner', formatter: '{c}', color: '#fff', fontSize: 12, fontWeight: 700, textBorderColor: 'rgba(20,20,45,0.4)', textBorderWidth: 2 }
        : { formatter: '{b} {c}', color: chartText(), fontSize: 12 },
      labelLine: { show: !narrow, length: 12, length2: 8, lineStyle: { color: dark ? '#3a3a46' : '#d5dbe4' } },
      emphasis: { scaleSize: 6, itemStyle: { shadowBlur: 16, shadowColor: dark ? 'rgba(0,0,0,0.5)' : 'rgba(35,35,56,0.22)' } },
      data: rows
    }]
  }, true)
}

async function loadTrend() {
  trendData = await statsTrend(days.value)
  renderTrend()
}
async function loadDistribution() {
  distData = await statsDistribution()
  courierRows.value = distData.courier || []
  renderStatus()
}
async function loadFeeds() {
  const [lg, pc] = await Promise.all([
    pageLogs({ page: 1, size: 6 }).catch(() => ({ records: [] })),
    pageParcels({ page: 1, size: 5 }).catch(() => ({ records: [] }))
  ])
  logs.value = lg.records || []
  parcels.value = pc.records || []
}

function resize() { trendChart?.resize(); statusChart?.resize(); renderStatus() }
watch(() => theme.mode, () => { renderTrend(); renderStatus() })

onMounted(async () => {
  trendChart = echarts.init(trendRef.value)
  statusChart = echarts.init(statusRef.value)
  window.addEventListener('resize', resize)
  ov.value = await statsOverview()
  await Promise.all([loadTrend(), loadDistribution(), loadFeeds(), loadHeatmap()])
})
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose(); statusChart?.dispose()
})
</script><style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

/* ---- 欢迎横幅：流动渐变 hero ---- */
.welcome {
  position: relative; overflow: hidden;
  border-radius: var(--custom-radius);
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(120deg, #4c6ef5 0%, #5b7cfa 26%, #7c6bfa 58%, #9b5cf6 84%, #8b5cf6 100%);
  background-size: 220% 220%;
  animation: flair-rise 0.6s var(--flair-ease) forwards, hero-flow 14s ease-in-out infinite alternate;
  box-shadow: 0 18px 44px -18px rgba(91, 124, 250, 0.55);
  padding: 30px 34px;
  display: flex; align-items: center; gap: 24px;
  color: #fff;
}
@keyframes hero-flow {
  0% { background-position: 0% 30%; }
  100% { background-position: 100% 70%; }
}
/* 白色半调点阵：右上密、向左下渐隐 */
.welcome::before {
  content: ''; position: absolute; width: 340px; height: 340px;
  right: -70px; top: -90px; pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.34) 1.7px, transparent 1.7px);
  background-size: 12px 12px;
  -webkit-mask-image: radial-gradient(closest-side, #000 26%, transparent 100%);
  mask-image: radial-gradient(closest-side, #000 26%, transparent 100%);
  transform: rotate(9deg);
}
/* 底部斜切光带 */
.welcome::after {
  content: ''; position: absolute; left: -10%; right: -10%; bottom: -70px; height: 130px;
  background: linear-gradient(100deg, transparent 20%, rgba(255, 255, 255, 0.14) 50%, transparent 80%);
  transform: rotate(-3deg); pointer-events: none;
}
.hero-glyph {
  position: absolute; right: 210px; bottom: -34px;
  font-size: 150px; color: rgba(255, 255, 255, 0.10);
  transform: rotate(-12deg); pointer-events: none;
  translate: calc(var(--mx, 0) * 26px) calc(var(--my, 0) * 20px + var(--sp, 0) * -0.05px);
  will-change: translate;
}
/* ---- 多图标分层视差：每个图标不同深度系数，随鼠标/滚动漂移 ---- */
.hero-orbs { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.orb {
  position: absolute; color: rgba(255, 255, 255, 0.26);
  filter: drop-shadow(0 6px 14px rgba(20, 18, 70, 0.22));
  will-change: translate;
  animation: orb-bob 6s ease-in-out infinite alternate;
}
.orb-1 {
  right: 30%; top: 12%; font-size: 44px; transform: rotate(-8deg);
  translate: calc(var(--mx, 0) * 34px) calc(var(--my, 0) * 26px + var(--sp, 0) * -0.07px);
}
.orb-2 {
  right: 8%; top: 20%; font-size: 58px; color: rgba(255, 255, 255, 0.2); transform: rotate(6deg);
  translate: calc(var(--mx, 0) * -46px) calc(var(--my, 0) * -34px + var(--sp, 0) * -0.04px);
  animation-delay: -2s; animation-duration: 7.5s;
}
.orb-3 {
  right: 44%; bottom: 14%; font-size: 34px; color: rgba(255, 255, 255, 0.3); transform: rotate(10deg);
  translate: calc(var(--mx, 0) * 55px) calc(var(--my, 0) * 40px + var(--sp, 0) * -0.1px);
  animation-delay: -4s; animation-duration: 5.5s;
}
.orb-4 {
  left: 34%; top: 10%; font-size: 30px; color: rgba(255, 255, 255, 0.16); transform: rotate(-14deg);
  translate: calc(var(--mx, 0) * -24px) calc(var(--my, 0) * 18px + var(--sp, 0) * -0.03px);
  animation-delay: -1s; animation-duration: 8s;
}
.orb-5 {
  left: 22%; bottom: 8%; font-size: 38px; color: rgba(255, 255, 255, 0.14); transform: rotate(16deg);
  translate: calc(var(--mx, 0) * 42px) calc(var(--my, 0) * -30px + var(--sp, 0) * -0.08px);
  animation-delay: -3s; animation-duration: 6.8s;
}
.orb-ring {
  position: absolute; border-radius: 50%;
  border: 1.5px dashed rgba(255, 255, 255, 0.22);
  will-change: translate;
}
.ring-1 {
  width: 130px; height: 130px; right: 18%; top: -34px;
  translate: calc(var(--mx, 0) * -18px) calc(var(--my, 0) * -14px + var(--sp, 0) * -0.02px);
}
.ring-2 {
  width: 90px; height: 90px; right: 38%; bottom: -26px;
  border-color: rgba(255, 255, 255, 0.15);
  translate: calc(var(--mx, 0) * 24px) calc(var(--my, 0) * 20px + var(--sp, 0) * -0.06px);
}
@keyframes orb-bob {
  from { margin-top: 0; }
  to { margin-top: -10px; }
}
.welcome-main { position: relative; z-index: 1; flex: 1; min-width: 0; }
.hero-chip {
  display: inline-flex; align-items: center; gap: 7px;
  font-size: 11px; font-weight: 700; letter-spacing: 2px;
  padding: 4px 12px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.32);
  backdrop-filter: blur(6px);
}
.live-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #7ef7d4; box-shadow: 0 0 8px #7ef7d4;
  animation: live-pulse 1.6s ease-in-out infinite;
}
@keyframes live-pulse { 50% { opacity: 0.35; transform: scale(0.8); } }
.hello { margin-top: 12px; font-size: 25px; font-weight: 800; color: #fff; letter-spacing: 0.5px; text-shadow: 0 2px 12px rgba(30, 20, 90, 0.25); }
.quote { margin-top: 8px; min-height: 26px; font-size: 14px; font-weight: 500; color: rgba(255, 255, 255, 0.92); }
.today { margin-top: 8px; font-size: 12.5px; color: rgba(255, 255, 255, 0.72); letter-spacing: 1px; }
.welcome-side { position: relative; z-index: 1; display: flex; gap: 12px; flex-shrink: 0; }
.mini {
  min-width: 100px; text-align: center;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: transform 0.3s var(--flair-ease), background 0.3s;
}
.mini:hover { transform: translateY(-3px); background: rgba(255, 255, 255, 0.2); }
.mini-num { display: block; font-size: 24px; font-weight: 800; color: #fff; font-variant-numeric: tabular-nums; }
.mini-label { font-size: 12px; color: rgba(255, 255, 255, 0.78); }
html.dark .welcome {
  border-color: rgba(139, 156, 255, 0.25);
  background: linear-gradient(120deg, #2b3a8f 0%, #3d55c8 30%, #5a48b8 62%, #6d4fc0 100%);
  background-size: 220% 220%;
  box-shadow: 0 18px 44px -18px rgba(20, 24, 70, 0.85);
}

/* ---- 主指标卡：角落色雾 + 渐变图标 + 悬浮描边光 ---- */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.metric { position: relative; overflow: hidden; padding: 18px 20px 16px; }
.metric::before {
  content: ''; position: absolute; width: 150px; height: 150px;
  right: -46px; top: -52px; border-radius: 50%;
  background: var(--mc-soft); filter: blur(26px);
  opacity: 0.85; pointer-events: none;
  transition: transform 0.45s var(--flair-ease), opacity 0.3s;
}
.metric:hover::before { transform: scale(1.45); opacity: 1; }
.metric::after {
  content: ''; position: absolute; width: 84px; height: 84px;
  right: -22px; top: -26px; pointer-events: none;
  background-image: radial-gradient(var(--mc) 1.2px, transparent 1.2px);
  background-size: 8px 8px; opacity: 0.22;
  -webkit-mask-image: radial-gradient(closest-side, #000 30%, transparent 100%);
  mask-image: radial-gradient(closest-side, #000 30%, transparent 100%);
}
.metric-top { display: flex; justify-content: space-between; align-items: center; position: relative; }
.metric-label { font-size: 13px; color: var(--art-gray-600); font-weight: 600; letter-spacing: 0.5px; }
.metric-icon {
  width: 44px; height: 44px; border-radius: 13px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, color-mix(in srgb, var(--mc) 82%, #fff), var(--mc));
  box-shadow: 0 8px 18px -8px var(--mc);
  transition: transform 0.35s var(--flair-ease), box-shadow 0.35s;
}
.metric:hover .metric-icon { transform: scale(1.1) rotate(-6deg); box-shadow: 0 12px 24px -8px var(--mc); }
.metric-num { display: block; margin-top: 10px; font-size: 32px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; letter-spacing: 0.5px; position: relative; }
.metric-foot { margin-top: 8px; font-size: 12px; color: var(--art-gray-600); display: flex; align-items: center; gap: 6px; position: relative; }
.metric-foot .dot { width: 6px; height: 6px; border-radius: 50%; box-shadow: 0 0 6px var(--mc); }
.metric-bar {
  position: absolute; left: 0; bottom: 0; height: 3px; width: 100%;
  background: linear-gradient(90deg, var(--mc), transparent 92%);
  opacity: 0.55;
  transform: scaleX(0.25); transform-origin: left;
  transition: transform 0.5s var(--flair-ease), opacity 0.3s;
}
.metric:hover .metric-bar { transform: scaleX(1); opacity: 1; }
.metric:hover { border-color: color-mix(in srgb, var(--mc) 45%, transparent); }

/* ---- 图表区 ---- */
.chart-row { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-card, .list-card { position: relative; overflow: hidden; }
.chart-card::before, .list-card::before {
  content: ''; position: absolute; left: 0; right: 0; top: 0; height: 3px; z-index: 1;
  background: linear-gradient(90deg, #5b7cfa, #8b5cf6 46%, transparent 88%);
  opacity: 0.65; pointer-events: none;
}
.bottom-row .list-card:nth-child(2)::before { background: linear-gradient(90deg, #10d9b5, #38bdf8 46%, transparent 88%); }
.courier-card::before { background: linear-gradient(90deg, #ffb020, #fb7185 46%, transparent 88%); }
.chart-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.chart-head b { display: inline-flex; align-items: center; gap: 8px; }
.chart-note { font-size: 11px; font-weight: 500; color: var(--art-gray-500); border: 1px solid var(--default-border); border-radius: 4px; padding: 1px 6px; letter-spacing: 0.5px; }
.chart { height: 320px; }
.more { font-size: 12.5px; color: var(--el-color-primary); text-decoration: none; font-weight: 500; transition: opacity 0.2s; }
.more:hover { opacity: 0.75; }

/* ---- 7×24 时段热力图 + 右侧快递占比 ---- */
.heat-zone { display: grid; grid-template-columns: minmax(0, 1fr) minmax(250px, 1fr); gap: 16px; align-items: stretch; }
.heat-zone .heat-card { min-width: 0; }
.heat-tools { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.heat-scroll { overflow-x: auto; padding-bottom: 2px; }
.ms-heat { border-collapse: separate; border-spacing: 2px; width: 100%; max-width: none; min-width: 560px; table-layout: fixed; }
.ms-hour { font-size: 10px; font-weight: 500; color: var(--art-gray-500); text-align: left; padding: 0 0 3px 1px; font-variant-numeric: tabular-nums; height: 15px; }
.ms-day-col { width: 36px; }
.ms-day {
  width: 36px; font-size: 10.5px; font-weight: 600; color: var(--art-gray-600);
  text-align: left; padding-right: 6px; white-space: nowrap;
}
.ms-clock-cell {
  --heat-rgb: 91, 124, 250;
  height: 14px; border-radius: 3px; cursor: default;
  background: rgba(var(--heat-rgb), calc(var(--v, 0) / 100 * 0.88 + 0.06));
  border: 1px solid rgba(var(--heat-rgb), calc(var(--v, 0) / 100 * 0.35));
  transition: transform 0.18s var(--flair-ease), box-shadow 0.18s, background 0.25s;
}
.heat-card.outbound .ms-clock-cell { --heat-rgb: 16, 217, 181; }
.ms-clock-cell.is-empty {
  background: var(--art-gray-100, rgba(121, 135, 161, 0.08));
  border-color: var(--default-border);
}
html.dark .ms-clock-cell.is-empty { background: rgba(255, 255, 255, 0.045); border-color: rgba(255, 255, 255, 0.07); }
html.dark .ms-clock-cell { border-color: rgba(var(--heat-rgb), calc(var(--v, 0) / 100 * 0.5)); }
.ms-clock-cell:not(.is-empty):hover {
  transform: scale(1.22);
  box-shadow: 0 4px 14px -2px rgba(var(--heat-rgb), 0.55);
  position: relative; z-index: 2;
}
.heat-foot {
  margin-top: 10px; padding-top: 8px; border-top: 1px dashed var(--default-border);
  max-width: 780px;
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
}
.heat-legend { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--art-gray-500); }
.legend-cell { display: inline-block; width: 16px; height: 12px; border-radius: 3px; }
.legend-cell:hover { transform: none; box-shadow: none; }
.heat-summary { font-size: 12px; color: var(--art-gray-600); font-variant-numeric: tabular-nums; }
.heat-summary b { color: var(--art-gray-900); font-weight: 700; }

/* ---- 底部两卡 ---- */
.bottom-row { display: grid; grid-template-columns: 1.5fr 1fr; gap: 16px; }
.list-card :deep(.el-card__body) { padding-top: 8px; }
.parcel-list { display: flex; flex-direction: column; }
.parcel-item { display: flex; gap: 12px; padding: 10px 4px; border-bottom: 1px dashed var(--default-border); }
.parcel-item:last-child { border-bottom: none; }
.p-avatar { width: 38px; height: 38px; border-radius: 10px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.p-body { flex: 1; min-width: 0; }
.p-line1 { display: flex; align-items: center; gap: 8px; }
.p-no { font-size: 13.5px; color: var(--art-gray-900); font-family: 'JetBrains Mono', Consolas, monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-line2 { margin: 3px 0 6px; font-size: 12px; color: var(--art-gray-600); }

.timeline { position: relative; padding-left: 16px; }
.timeline::before { content: ''; position: absolute; left: 4px; top: 8px; bottom: 8px; width: 2px; background: var(--default-border); border-radius: 2px; }
.tl-item { position: relative; padding: 8px 0 10px; }
.tl-dot { position: absolute; left: -16px; top: 14px; width: 10px; height: 10px; border-radius: 50%; border: 2px solid var(--default-box-color); }
.tl-dot.ok { background: #10d9b5; box-shadow: 0 0 0 3px rgba(16,217,181,0.18); }
.tl-dot.fail { background: #fb7185; box-shadow: 0 0 0 3px rgba(251,113,133,0.18); }
.tl-line1 { font-size: 13px; color: var(--art-gray-800); }
.tl-line1 b { color: var(--art-gray-900); }
.tl-line2 { font-size: 12px; color: var(--art-gray-600); margin-top: 2px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.tl-time { font-size: 11px; color: var(--art-gray-500); margin-top: 3px; }

/* 快递公司占比：随热力图纵向延伸，列表均匀铺满，脚注沉底 */
.courier-card { display: flex; flex-direction: column; }
.courier-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding-top: 8px; }
.courier-list { flex: 1; display: flex; flex-direction: column; justify-content: space-evenly; gap: 14px; padding: 6px 0 14px; }
.courier-card .card-foot { margin-top: auto; }
.c-head { display: flex; justify-content: space-between; margin-bottom: 6px; }
.c-name { font-size: 13px; font-weight: 600; color: var(--art-gray-800); }
.c-val { font-size: 12px; color: var(--art-gray-600); font-variant-numeric: tabular-nums; }
.card-foot {
  margin-top: 14px; padding-top: 12px;
  border-top: 1px dashed var(--default-border);
  font-size: 11.5px; color: var(--art-gray-500); letter-spacing: 0.3px;
}

/* ---- 响应式 ---- */
@media (max-width: 1200px) {
  .heat-zone { grid-template-columns: 1fr; }
}
@media (max-width: 1000px) {
  .chart-row { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .welcome { flex-direction: column; align-items: stretch; }
  .welcome-side { justify-content: space-between; }
  .mini { flex: 1; }
}
@media (max-width: 640px) {
  .welcome { padding: 22px 18px; }
  .ms-heat { border-spacing: 2px; }
  .ms-clock-cell { height: 12px; border-radius: 3px; }
  .heat-tools { gap: 6px; }
  .hero-glyph { display: none; }
  .orb-4, .orb-5, .ring-2 { display: none; }
  .orb-1 { font-size: 32px; right: 26%; }
  .orb-2 { font-size: 40px; }
  .orb-3 { font-size: 26px; right: 52%; }
  .hello { font-size: 19px; }
  .quote { font-size: 13.5px; }
  .welcome-side { gap: 8px; }
  .mini { min-width: 0; padding: 10px 8px; }
  .mini-num { font-size: 18px; }
  .mini-label { font-size: 11px; white-space: nowrap; }
  .stat-grid { gap: 10px; }
  .metric { padding: 14px 14px 12px; }
  .metric-num { font-size: 24px; }
  .metric-icon { width: 38px; height: 38px; }
  .chart { height: 250px; }
  .bottom-row { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .metric-icon, .mini { animation: none !important; transition: none !important; }
  .welcome, .live-dot, .orb { animation: none !important; }
  .orb, .orb-ring, .hero-glyph, .metric-icon { translate: none !important; }
}
</style>