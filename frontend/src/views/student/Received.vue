<template>
  <div class="ap">
    <PageHero tone="proxy" icon="Handbag" chip="RECEIVED AUTH" title="受托代取"
      sub="同学授权给你代取的包裹。携带授权码并向授权人索取取件码，到驿站柜台由工作人员核销出库，全程记入去向台账。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Share" @click="$router.push('/student/granted')">我发起的授权</el-button>
        <el-button type="default" :icon="Bicycle" @click="$router.push('/student/proxy-orders')">我的服务单</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="受托清单" :note="`${filtered.length} / ${all.length} 条`" icon="Tickets" accent="proxy" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索运单号 / 授权人 / 驿站</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 SF6031、陈嘉睿、西区" />
        </div>
      </div>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="s in statusTabs" :key="s.key" :class="{ on: status === s.key }" @click="status = s.key; page = 1">
            {{ s.label }}<span class="n">{{ countOf(s.key) }}</span>
          </button>
        </div>
      </div>

      <div class="ap-note" style="--nc:#0ea5e9">
        <el-icon><InfoFilled /></el-icon>
        <span><b>核销方式：</b>向授权人索取该包裹取件码，连同下方授权码一并出示给柜台员工；员工核销后授权自动标记为已使用。</span>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="paged" stripe :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
            <el-table-column label="运单号" prop="trackingNo" sortable="custom" min-width="170">
              <template #default="{ row }">
                <b class="ap-mono ap-strong">{{ row.trackingNo }}</b>
                <div class="ap-sub">{{ courierName(row.courierCompany) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="授权人" prop="grantorName" sortable="custom" width="150">
              <template #default="{ row }">
                {{ row.grantorName }}
                <div class="ap-sub ap-mono">{{ row.grantorUsername }}</div>
              </template>
            </el-table-column>
            <el-table-column label="授权码" prop="authCode" sortable="custom" width="120">
              <template #default="{ row }">
                <button v-if="row.status === 'ACTIVE'" class="code-chip" title="点击复制" @click.stop="copyCode(row)">
                  {{ row.authCode }}<el-icon :size="12"><DocumentCopy /></el-icon>
                </button>
                <span v-else class="ap-sub">已失效</span>
              </template>
            </el-table-column>
            <el-table-column label="驿站 / 货架" prop="stationName" sortable="custom" min-width="160">
              <template #default="{ row }">
                {{ row.stationName }}
                <div class="ap-sub ap-mono">{{ row.shelfCode ? `货架 ${row.shelfCode}` : '未上架' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" sortable="custom" width="94">
              <template #default="{ row }">
                <el-tag size="small" :type="dict(AUTH_STATUS, row.status).type" effect="light">{{ dict(AUTH_STATUS, row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" prop="expireAt" sortable="custom" width="164">
              <template #default="{ row }">
                <span class="ap-mono">{{ fmtTime(row.expireAt) }}</span>
                <div v-if="row.status === 'ACTIVE'" class="ap-sub" :class="{ urgent: remainHours(row) < 2 }">剩余 {{ remainText(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="留言" min-width="140">
              <template #default="{ row }"><span class="ap-ellipsis">{{ row.remark || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in paged" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <b class="ap-mono">{{ row.trackingNo }}</b>
              <el-tag size="small" :type="dict(AUTH_STATUS, row.status).type" effect="light">{{ dict(AUTH_STATUS, row.status).label }}</el-tag>
              <span v-if="row.status === 'ACTIVE' && remainHours(row) < 2" class="urgent soon">即将到期</span>
            </div>
            <div class="ap-card-grid">
              <div><label>授权人</label><span>{{ row.grantorName }}（{{ row.grantorUsername }}）</span></div>
              <div><label>驿站 / 货架</label><span>{{ row.stationName }}{{ row.shelfCode ? ` · ${row.shelfCode}` : '' }}</span></div>
              <div v-if="row.status === 'ACTIVE'"><label>授权码</label><span class="ap-mono code-big">{{ row.authCode }}</span></div>
              <div><label>有效期至</label><span class="ap-mono">{{ fmtTime(row.expireAt) }}</span></div>
            </div>
            <p v-if="row.remark" class="card-remark">留言：{{ row.remark }}</p>
            <div class="ap-card-actions">
              <el-button size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'ACTIVE'" size="small" type="primary" plain :icon="DocumentCopy" @click.stop="copyCode(row)">复制授权码</el-button>
            </div>
          </article>
        </div>

        <div v-if="!filtered.length && !loading" class="ap-empty">
          <el-empty :description="all.length ? '没有匹配的受托记录' : '还没有同学授权你代取'" :image-size="86" />
          <p>{{ all.length ? '换个关键词或状态筛选试试。' : '让同学在「我的包裹」里对你发起授权，或去大厅接付费代取单赚取收入。' }}</p>
          <el-button v-if="!all.length" type="primary" :icon="Bicycle" style="margin-top:12px" @click="$router.push('/student/proxy-market')">前往代取大厅</el-button>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="filtered.length"
                       v-model:current-page="page" :page-size="size" />
      </div>
    </PanelCard>

    <el-drawer v-model="detailOpen" :title="current ? `受托详情 · ${current.trackingNo}` : '受托详情'" size="480px" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-status">
          <el-tag :type="dict(AUTH_STATUS, current.status).type" effect="dark" size="large">{{ dict(AUTH_STATUS, current.status).label }}</el-tag>
          <span v-if="current.status === 'ACTIVE'" :class="{ urgent: remainHours(current) < 2 }">剩余 {{ remainText(current) }}</span>
        </div>
        <div v-if="current.status === 'ACTIVE'" class="auth-box">
          <label>柜台核销授权码</label>
          <b class="ap-mono">{{ current.authCode }}</b>
          <p>另需向授权人 {{ current.grantorName }} 索取该包裹取件码，两码齐全方可出库。</p>
          <el-button size="small" type="primary" plain :icon="DocumentCopy" @click="copyCode(current)">复制授权码</el-button>
        </div>
        <div class="ap-kv">
          <div><label>授权人</label><span>{{ current.grantorName }} <em class="ap-mono">{{ current.grantorUsername }}</em></span></div>
          <div><label>快递公司</label><span>{{ courierName(current.courierCompany) }}</span></div>
          <div><label>驿站 / 货架</label><span>{{ current.stationName }}{{ current.shelfCode ? ` · ${current.shelfCode}` : '' }}</span></div>
          <div><label>创建时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>有效期至</label><span class="ap-mono">{{ fmtTime(current.expireAt) }}</span></div>
          <div v-if="current.usedAt"><label>核销时间</label><span class="ap-mono">{{ fmtTime(current.usedAt) }}</span></div>
        </div>
        <p v-if="current.remark" class="d-remark">授权人留言：{{ current.remark }}</p>
        <h4 class="d-h">包裹去向轨迹</h4>
        <TraceTimeline :nodes="traces" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bicycle, DocumentCopy, InfoFilled, Refresh, Search, Share, View } from '@element-plus/icons-vue'
import { receivedAuths, parcelTraces } from '../../api'
import { fmtTime, dict, courierName, AUTH_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import TraceTimeline from '../../components/TraceTimeline.vue'

const loading = ref(false)
const all = ref([])
const keyword = ref('')
const status = ref('ACTIVE')
const { defaultSort, onSortChange, sorted } = useTableSort('createdAt')
const page = ref(1)
const size = 10
const detailOpen = ref(false)
const current = ref(null)
const traces = ref([])
const now = ref(Date.now())
const timer = setInterval(() => { now.value = Date.now() }, 30000)
onBeforeUnmount(() => clearInterval(timer))

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'ACTIVE', label: '待代取' },
  { key: 'USED', label: '已完成' },
  { key: 'REVOKED', label: '已撤销' },
  { key: 'EXPIRED', label: '已过期' }
]
const countOf = key => (key ? all.value.filter(a => a.status === key).length : all.value.length)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const rows = all.value.filter(a => (!status.value || a.status === status.value) &&
    (!kw || [a.trackingNo, a.grantorName, a.grantorUsername, a.stationName, a.authCode].some(v => (v || '').toLowerCase().includes(kw))))
  return sorted(rows)
})
const paged = computed(() => filtered.value.slice((page.value - 1) * size, page.value * size))

const heroStats = computed(() => [
  { label: '待代取', value: countOf('ACTIVE'), unit: '件' },
  { label: '已完成', value: countOf('USED'), unit: '件' },
  { label: '累计受托', value: all.value.length, unit: '件' }
])
const metrics = computed(() => [
  { label: '待代取', value: countOf('ACTIVE'), unit: '件', icon: 'Handbag', color: '#0ea5e9', foot: '授权码仍可核销' },
  { label: '即将到期', value: all.value.filter(a => a.status === 'ACTIVE' && remainHours(a) < 2).length, unit: '件', icon: 'AlarmClock', color: '#f59e0b', foot: '剩余不足 2 小时' },
  { label: '已完成', value: countOf('USED'), unit: '件', icon: 'CircleCheck', color: '#10d9b5', foot: '柜台核销出库' },
  { label: '已失效', value: countOf('REVOKED') + countOf('EXPIRED'), unit: '件', icon: 'CircleClose', color: '#a1abbf', foot: '撤销或到期' }
])

function remainHours(row) { return Math.max(0, (new Date(row.expireAt).getTime() - now.value) / 3600000) }
function remainText(row) {
  const h = remainHours(row)
  if (h <= 0) return '已到期'
  if (h < 1) return `${Math.ceil(h * 60)} 分钟`
  if (h < 48) return `${h.toFixed(1)} 小时`
  return `${Math.floor(h / 24)} 天 ${Math.round(h % 24)} 小时`
}

async function load() {
  loading.value = true
  try {
    const data = await receivedAuths({ page: 1, size: 100 })
    all.value = data.records
    page.value = 1
  } finally { loading.value = false }
}

async function openDetail(row) {
  current.value = row
  traces.value = []
  detailOpen.value = true
  try { traces.value = await parcelTraces(row.parcelId) } catch { traces.value = [] }
}

async function copyCode(row) {
  try {
    await navigator.clipboard.writeText(row.authCode)
    ElMessage.success(`授权码 ${row.authCode} 已复制`)
  } catch { ElMessage.warning('复制失败，请手动记录') }
}

onMounted(load)
</script>

<style scoped>
.status-seg { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.ap-note { margin-bottom: 12px; }
.code-chip {
  display: inline-flex; align-items: center; gap: 6px;
  font-family: 'JetBrains Mono', Consolas, monospace; font-size: 14px; font-weight: 800; letter-spacing: 2px;
  color: #0369a1; background: linear-gradient(120deg, #f0f9ff, #e0f2fe);
  border: 1px dashed #0ea5e9; border-radius: 8px; padding: 3px 9px; cursor: pointer;
  transition: transform 0.2s var(--flair-ease), box-shadow 0.2s;
}
.code-chip:hover { transform: translateY(-1px); box-shadow: 0 6px 16px -8px rgba(14, 165, 233, 0.7); }
html.dark .code-chip { color: #7dd3fc; background: rgba(14, 165, 233, 0.14); }
.code-big { font-size: 16px; font-weight: 800; letter-spacing: 2px; color: #0369a1; }
html.dark .code-big { color: #7dd3fc; }
.urgent { color: #e11d48; font-weight: 700; }
html.dark .urgent { color: #fb7185; }
.soon { margin-left: auto; font-size: 11px; }
.card-remark { margin-top: 9px; font-size: 12px; color: var(--art-gray-700); background: var(--art-gray-100); padding: 7px 10px; border-radius: 8px; }
html.dark .card-remark { background: #1c1c22; }
.detail { display: flex; flex-direction: column; gap: 14px; }
.d-status { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; font-size: 12.5px; }
.auth-box {
  border: 1px dashed #0ea5e9; border-radius: 12px; padding: 14px;
  background: linear-gradient(140deg, rgba(14, 165, 233, 0.1), rgba(34, 211, 238, 0.05));
  display: flex; flex-direction: column; gap: 6px; align-items: flex-start;
}
.auth-box label { font-size: 11px; letter-spacing: 1px; color: var(--art-gray-600); font-weight: 700; }
.auth-box b { font-size: 26px; letter-spacing: 5px; color: #0369a1; }
html.dark .auth-box b { color: #7dd3fc; }
.auth-box p { font-size: 11.5px; color: var(--art-gray-600); line-height: 1.6; }
.d-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 2px; }
.d-remark { font-size: 12.5px; color: var(--art-gray-700); background: var(--art-gray-100); padding: 8px 11px; border-radius: 9px; }
html.dark .d-remark { background: #1c1c22; }
</style>