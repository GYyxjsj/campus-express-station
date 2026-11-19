<template>
  <div class="ap">
    <PageHero tone="identity" icon="Share" chip="GRANTED AUTH" title="我发起的授权"
      sub="把在库包裹免费授权给同学代取：系统生成一次性授权码，代取人凭授权码 + 取件码到柜台核销，出库后授权自动失效。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Box" @click="$router.push('/student/parcels')">去包裹列表发起</el-button>
        <el-button type="default" :icon="Bicycle" @click="$router.push('/student/proxy-market')">付费代取大厅</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="授权记录" :note="`${filtered.length} / ${all.length} 条`" icon="Tickets" accent="identity" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索运单号 / 代取人</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 YT4087、林晚晴" />
        </div>
      </div>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="s in statusTabs" :key="s.key" :class="{ on: status === s.key }" @click="status = s.key; page = 1">
            {{ s.label }}<span class="n">{{ countOf(s.key) }}</span>
          </button>
        </div>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="paged" stripe :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
            <el-table-column label="运单号" prop="trackingNo" sortable="custom" min-width="170">
              <template #default="{ row }">
                <b class="ap-mono ap-strong">{{ row.trackingNo }}</b>
                <div class="ap-sub">{{ courierName(row.courierCompany) }} · {{ row.stationName }}</div>
              </template>
            </el-table-column>
            <el-table-column label="代取人" prop="granteeName" sortable="custom" width="150">
              <template #default="{ row }">
                {{ row.granteeName }}
                <div class="ap-sub ap-mono">{{ row.granteeUsername }}</div>
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
            <el-table-column label="状态" prop="status" sortable="custom" width="94">
              <template #default="{ row }">
                <el-tag size="small" :type="dict(AUTH_STATUS, row.status).type" effect="light">{{ dict(AUTH_STATUS, row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="包裹状态" prop="parcelStatus" sortable="custom" width="104">
              <template #default="{ row }">
                <el-tag size="small" :type="dict(PARCEL_STATUS, row.parcelStatus).type" effect="plain">{{ dict(PARCEL_STATUS, row.parcelStatus).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" prop="expireAt" sortable="custom" width="164">
              <template #default="{ row }">
                <span class="ap-mono">{{ fmtTime(row.expireAt) }}</span>
                <div v-if="row.status === 'ACTIVE'" class="ap-sub" :class="{ urgent: remainHours(row) < 2 }">剩余 {{ remainText(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
                  <el-popconfirm v-if="row.status === 'ACTIVE'" title="确定撤销该授权？撤销后授权码立即失效" width="240" @confirm="doRevoke(row)">
                    <template #reference>
                      <el-button link type="danger" :icon="CircleClose" @click.stop>撤销</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in paged" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <b class="ap-mono">{{ row.trackingNo }}</b>
              <el-tag size="small" :type="dict(AUTH_STATUS, row.status).type" effect="light">{{ dict(AUTH_STATUS, row.status).label }}</el-tag>
              <el-tag size="small" :type="dict(PARCEL_STATUS, row.parcelStatus).type" effect="plain">{{ dict(PARCEL_STATUS, row.parcelStatus).label }}</el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>代取人</label><span>{{ row.granteeName }}（{{ row.granteeUsername }}）</span></div>
              <div><label>有效期至</label><span class="ap-mono">{{ fmtTime(row.expireAt) }}</span></div>
              <div v-if="row.status === 'ACTIVE'"><label>授权码</label><span class="ap-mono code-big">{{ row.authCode }}</span></div>
              <div v-if="row.status === 'ACTIVE'"><label>剩余时间</label><span :class="{ urgent: remainHours(row) < 2 }">{{ remainText(row) }}</span></div>
            </div>
            <p v-if="row.remark" class="card-remark">{{ row.remark }}</p>
            <div class="ap-card-actions">
              <el-button size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'ACTIVE'" size="small" type="primary" plain :icon="DocumentCopy" @click.stop="copyCode(row)">复制授权码</el-button>
              <el-popconfirm v-if="row.status === 'ACTIVE'" title="确定撤销该授权？撤销后授权码立即失效" width="240" @confirm="doRevoke(row)">
                <template #reference>
                  <el-button size="small" type="danger" plain :icon="CircleClose" @click.stop>撤销</el-button>
                </template>
              </el-popconfirm>
            </div>
          </article>
        </div>

        <div v-if="!filtered.length && !loading" class="ap-empty">
          <el-empty :description="all.length ? '没有匹配的授权' : '还没有发起过代取授权'" :image-size="86" />
          <p>{{ all.length ? '换个关键词或状态筛选试试。' : '在「我的包裹」里对在库包裹点击「找人代取」，即可生成一次性授权码。' }}</p>
          <el-button v-if="!all.length" type="primary" :icon="Box" style="margin-top:12px" @click="$router.push('/student/parcels')">前往我的包裹</el-button>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="filtered.length"
                       v-model:current-page="page" :page-size="size" />
      </div>
    </PanelCard>

    <el-drawer v-model="detailOpen" :title="current ? `授权详情 · ${current.trackingNo}` : '授权详情'" size="480px" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-status">
          <el-tag :type="dict(AUTH_STATUS, current.status).type" effect="dark" size="large">{{ dict(AUTH_STATUS, current.status).label }}</el-tag>
          <el-tag :type="dict(PARCEL_STATUS, current.parcelStatus).type" effect="plain">{{ dict(PARCEL_STATUS, current.parcelStatus).label }}</el-tag>
        </div>
        <div v-if="current.status === 'ACTIVE'" class="auth-box">
          <label>一次性授权码</label>
          <b class="ap-mono">{{ current.authCode }}</b>
          <p>代取人凭此码 + 物主取件码到柜台核销；核销或撤销后立即失效。</p>
          <el-button size="small" type="primary" plain :icon="DocumentCopy" @click="copyCode(current)">复制授权码</el-button>
        </div>
        <div class="ap-kv">
          <div><label>代取人</label><span>{{ current.granteeName }} <em class="ap-mono">{{ current.granteeUsername }}</em></span></div>
          <div><label>授权人</label><span>{{ current.grantorName }} <em class="ap-mono">{{ current.grantorUsername }}</em></span></div>
          <div><label>驿站 / 货架</label><span>{{ current.stationName }}{{ current.shelfCode ? ` · ${current.shelfCode}` : '' }}</span></div>
          <div><label>有效时长</label><span>{{ validHoursOf(current) }} 小时</span></div>
          <div><label>创建时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>有效期至</label><span class="ap-mono">{{ fmtTime(current.expireAt) }}</span></div>
          <div v-if="current.usedAt"><label>核销时间</label><span class="ap-mono">{{ fmtTime(current.usedAt) }}</span></div>
          <div v-if="current.revokedAt"><label>撤销时间</label><span class="ap-mono">{{ fmtTime(current.revokedAt) }}</span></div>
        </div>
        <p v-if="current.remark" class="d-remark">留言：{{ current.remark }}</p>
        <h4 class="d-h">包裹去向轨迹</h4>
        <TraceTimeline :nodes="traces" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bicycle, Box, CircleClose, DocumentCopy, Refresh, Search, View } from '@element-plus/icons-vue'
import { grantedAuths, revokeAuth, parcelTraces } from '../../api'
import { fmtTime, dict, courierName, AUTH_STATUS, PARCEL_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import TraceTimeline from '../../components/TraceTimeline.vue'

const loading = ref(false)
const all = ref([])
const keyword = ref('')
const status = ref('')
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
  { key: 'ACTIVE', label: '生效中' },
  { key: 'USED', label: '已使用' },
  { key: 'REVOKED', label: '已撤销' },
  { key: 'EXPIRED', label: '已过期' }
]
const countOf = key => (key ? all.value.filter(a => a.status === key).length : all.value.length)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const rows = all.value.filter(a => (!status.value || a.status === status.value) &&
    (!kw || [a.trackingNo, a.granteeName, a.granteeUsername, a.authCode].some(v => (v || '').toLowerCase().includes(kw))))
  return sorted(rows)
})
const paged = computed(() => filtered.value.slice((page.value - 1) * size, page.value * size))

const heroStats = computed(() => [
  { label: '生效中', value: countOf('ACTIVE'), unit: '条' },
  { label: '已核销', value: countOf('USED'), unit: '条' },
  { label: '累计授权', value: all.value.length, unit: '条' }
])
const metrics = computed(() => [
  { label: '累计授权', value: all.value.length, unit: '条', icon: 'Share', color: '#8b5cf6', foot: '免费同学互助代取' },
  { label: '生效中', value: countOf('ACTIVE'), unit: '条', icon: 'Timer', color: '#10d9b5', foot: '授权码仍可核销' },
  { label: '已使用', value: countOf('USED'), unit: '条', icon: 'CircleCheck', color: '#0ea5e9', foot: '代取人已完成出库' },
  { label: '已失效', value: countOf('REVOKED') + countOf('EXPIRED'), unit: '条', icon: 'CircleClose', color: '#a1abbf', foot: '撤销或到期自动失效' }
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
    const data = await grantedAuths({ page: 1, size: 100 })
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

function validHoursOf(row) {
  if (!row?.createdAt || !row?.expireAt) return '—'
  return Math.round((new Date(row.expireAt) - new Date(row.createdAt)) / 3600000)
}

async function copyCode(row) {
  try {
    await navigator.clipboard.writeText(row.authCode)
    ElMessage.success(`授权码 ${row.authCode} 已复制`)
  } catch { ElMessage.warning('复制失败，请手动记录') }
}

async function doRevoke(row) {
  await revokeAuth(row.id)
  row.status = 'REVOKED'
  row.revokedAt = new Date().toISOString()
  ElMessage.success('已撤销，授权码立即失效')
}

onMounted(load)
</script>

<style scoped>
.status-seg { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.code-chip {
  display: inline-flex; align-items: center; gap: 6px;
  font-family: 'JetBrains Mono', Consolas, monospace; font-size: 14px; font-weight: 800; letter-spacing: 2px;
  color: #6d28d9; background: linear-gradient(120deg, #f5f3ff, #ede9fe);
  border: 1px dashed #8b5cf6; border-radius: 8px; padding: 3px 9px; cursor: pointer;
  transition: transform 0.2s var(--flair-ease), box-shadow 0.2s;
}
.code-chip:hover { transform: translateY(-1px); box-shadow: 0 6px 16px -8px rgba(139, 92, 246, 0.7); }
html.dark .code-chip { color: #c4b5fd; background: rgba(139, 92, 246, 0.14); }
.code-big { font-size: 16px; font-weight: 800; letter-spacing: 2px; color: #6d28d9; }
html.dark .code-big { color: #c4b5fd; }
.urgent { color: #e11d48; font-weight: 700; }
html.dark .urgent { color: #fb7185; }
.card-remark { margin-top: 9px; font-size: 12px; color: var(--art-gray-700); background: var(--art-gray-100); padding: 7px 10px; border-radius: 8px; }
html.dark .card-remark { background: #1c1c22; }
.detail { display: flex; flex-direction: column; gap: 14px; }
.d-status { display: flex; gap: 8px; flex-wrap: wrap; }
.auth-box {
  border: 1px dashed #8b5cf6; border-radius: 12px; padding: 14px;
  background: linear-gradient(140deg, rgba(139, 92, 246, 0.1), rgba(192, 132, 252, 0.05));
  display: flex; flex-direction: column; gap: 6px; align-items: flex-start;
}
.auth-box label { font-size: 11px; letter-spacing: 1px; color: var(--art-gray-600); font-weight: 700; }
.auth-box b { font-size: 26px; letter-spacing: 5px; color: #6d28d9; }
html.dark .auth-box b { color: #c4b5fd; }
.auth-box p { font-size: 11.5px; color: var(--art-gray-600); line-height: 1.6; }
.d-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 2px; }
.d-remark { font-size: 12.5px; color: var(--art-gray-700); background: var(--art-gray-100); padding: 8px 11px; border-radius: 9px; }
html.dark .d-remark { background: #1c1c22; }
</style>