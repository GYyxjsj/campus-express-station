<template>
  <div class="ap">
    <PageHero tone="danger" icon="AlarmClock" chip="OVERDUE CONTROL" live title="滞留件管理"
      sub="入库超过滞留阈值（默认 72 小时）仍未取的包裹。系统已自动升级状态并通知物主与管理员，可在此批量催领或登记退回。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Bell" :loading="bulkLoading" :disabled="!selected.size" @click="bulkRemind">批量催领</el-button>
        <el-button type="default" :icon="RefreshLeft" :disabled="!selected.size" @click="openBulkReturn">批量退回</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="滞留清单" :note="`${filtered.length} / ${all.length} 件`" icon="Warning" accent="notice" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索运单号 / 物主 / 手机号</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 YT4087、陈嘉睿、158…" />
        </div>
        <div class="ap-field">
          <label>驿站</label>
          <el-select v-model="stationId" clearable placeholder="全部驿站">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
      </div>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="s in stayTabs" :key="s.key" :class="{ on: stayTab === s.key }" @click="stayTab = s.key; page = 1">
            {{ s.label }}<span class="n">{{ stayCount(s.key) }}</span>
          </button>
        </div>
        <el-button v-if="selected.size" size="small" type="warning" :icon="Bell" :loading="bulkLoading" @click="bulkRemind">
          催领已选 {{ selected.size }} 件
        </el-button>
        <el-button v-if="selected.size" size="small" type="danger" plain :icon="RefreshLeft" @click="openBulkReturn">
          退回已选 {{ selected.size }} 件
        </el-button>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="paged" stripe :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail" @selection-change="onSelect">
            <el-table-column type="selection" width="42" />
            <el-table-column label="运单号" prop="trackingNo" sortable="custom" min-width="165">
              <template #default="{ row }">
                <b class="ap-mono ap-strong">{{ row.trackingNo }}</b>
                <div class="ap-sub">{{ courierName(row.courierCompany) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="物主" prop="ownerName" sortable="custom" width="150">
              <template #default="{ row }">
                {{ row.ownerName }}
                <div class="ap-sub ap-mono">{{ row.ownerPhone }}</div>
              </template>
            </el-table-column>
            <el-table-column label="驿站 / 货架" prop="stationName" sortable="custom" min-width="150">
              <template #default="{ row }">
                {{ row.stationName }}
                <div class="ap-sub ap-mono">{{ row.shelfCode || '未上架' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="入库时间" prop="inboundTime" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></template>
            </el-table-column>
            <el-table-column label="滞留时长" prop="stayHours" sortable="custom" width="120">
              <template #default="{ row }">
                <span class="stay-chip" :class="stayClass(row)">{{ stayHours(row) }} 小时</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="188" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
                  <el-button v-perm="'reminder:send'" link type="warning" :icon="Bell" @click.stop="doResend(row)">催领</el-button>
                  <el-button link type="danger" :icon="RefreshLeft" @click.stop="openReturn(row)">退回</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in paged" :key="row.id" class="ap-card-item" :class="{ picked: selected.has(row.id) }" @click="openDetail(row)">
            <div class="ap-card-top">
              <el-checkbox :model-value="selected.has(row.id)" @click.stop @change="v => toggle(row, v)" />
              <b class="ap-mono">{{ row.trackingNo }}</b>
              <span class="stay-chip" :class="stayClass(row)">{{ stayHours(row) }}h</span>
            </div>
            <div class="ap-card-grid">
              <div><label>物主</label><span>{{ row.ownerName }} · {{ row.ownerPhone }}</span></div>
              <div><label>驿站 / 货架</label><span>{{ row.stationName }}{{ row.shelfCode ? ` · ${row.shelfCode}` : '' }}</span></div>
              <div><label>入库时间</label><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></div>
              <div><label>取件码</label><span class="ap-mono">{{ row.pickupCode || '—' }}</span></div>
            </div>
            <div class="ap-card-actions">
              <el-button size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-perm="'reminder:send'" size="small" type="warning" plain :icon="Bell" @click.stop="doResend(row)">催领</el-button>
              <el-button size="small" type="danger" plain :icon="RefreshLeft" @click.stop="openReturn(row)">退回</el-button>
            </div>
          </article>
        </div>

        <div v-if="!filtered.length && !loading" class="ap-empty">
          <el-empty :description="all.length ? '没有匹配的滞留件' : '当前没有滞留包裹'" :image-size="86" />
          <p>{{ all.length ? '调整筛选条件后重试。' : '所有包裹都在阈值内被领走，保持得不错。' }}</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="filtered.length"
                       v-model:current-page="page" :page-size="size" />
      </div>
    </PanelCard>

    <ParcelDetail v-model="detailVisible" :parcel-id="detailId" />

    <el-dialog v-model="returnVisible" :title="bulkReturn ? `批量退回（${selected.size} 件）` : '退回登记'" width="460px" append-to-body>
      <div class="ap-form">
        <div class="ap-note" style="--nc:#e11d48">
          <el-icon><Warning /></el-icon>
          <span v-if="bulkReturn">将对已选 <b>{{ selected.size }}</b> 件滞留包裹统一登记退回，逐件写入台账并通知物主。</span>
          <span v-else>退回后状态置为已退回并写入去向台账。运单号 <b class="ap-mono">{{ returnRow?.trackingNo }}</b></span>
        </div>
        <div class="ap-field-row">
          <label>退回原因<em>*</em></label>
          <el-input v-model="returnReason" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：超期未取退回快递公司" />
        </div>
        <div class="quick-reasons">
          <button v-for="r in QUICK_REASONS" :key="r" type="button" @click="returnReason = r">{{ r }}</button>
        </div>
      </div>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="danger" :icon="RefreshLeft" :loading="returnLoading" @click="doReturn">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AlarmClock, Bell, Refresh, RefreshLeft, Search, View, Warning } from '@element-plus/icons-vue'
import { overdueParcels, listStations, resendNotification, returnParcel } from '../../api'
import { fmtTime, courierName } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import ParcelDetail from '../../components/ParcelDetail.vue'

const loading = ref(false)
const bulkLoading = ref(false)
const returnLoading = ref(false)
const all = ref([])
const stations = ref([])
const keyword = ref('')
const stationId = ref(null)
const stayTab = ref('')
const page = ref(1)
const size = 10
const selected = ref(new Set())
const detailVisible = ref(false)
const detailId = ref(null)
const returnVisible = ref(false)
const returnRow = ref(null)
const bulkReturn = ref(false)
const returnReason = ref('')
const now = ref(Date.now())
const timer = setInterval(() => { now.value = Date.now() }, 60000)
onBeforeUnmount(() => clearInterval(timer))
const QUICK_REASONS = ['超期未取退回快递公司', '物主拒收', '包裹破损退回', '多次催领无响应']

const stayHours = row => (row.inboundTime ? Math.floor((now.value - new Date(row.inboundTime).getTime()) / 3600000) : 0)
const { defaultSort, onSortChange, sorted } = useTableSort('inboundTime', { stayHours })
const stayClass = row => (stayHours(row) >= 168 ? 'critical' : stayHours(row) >= 96 ? 'hot' : '')
const stayTabs = [
  { key: '', label: '全部' },
  { key: '72', label: '72h+' },
  { key: '96', label: '96h+' },
  { key: '168', label: '7天+' }
]
const stayCount = key => (key ? all.value.filter(p => stayHours(p) >= Number(key)).length : all.value.length)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const rows = all.value.filter(p =>
    (!stationId.value || p.stationId === stationId.value) &&
    (!stayTab.value || stayHours(p) >= Number(stayTab.value)) &&
    (!kw || [p.trackingNo, p.ownerName, p.ownerUsername, p.ownerPhone, p.stationName].some(v => (v || '').toLowerCase().includes(kw))))
  return sorted(rows)
})
const paged = computed(() => filtered.value.slice((page.value - 1) * size, page.value * size))

const heroStats = computed(() => [
  { label: '滞留件', value: all.value.length, unit: '件' },
  { label: '7天以上', value: stayCount('168'), unit: '件' },
  { label: '已选', value: selected.value.size, unit: '件' }
])
const metrics = computed(() => {
  const hs = all.value.map(stayHours)
  const avg = hs.length ? Math.round(hs.reduce((a, b) => a + b, 0) / hs.length) : 0
  const max = hs.length ? Math.max(...hs) : 0
  return [
    { label: '滞留包裹', value: all.value.length, unit: '件', icon: 'AlarmClock', color: '#e11d48', foot: '超阈值自动升级状态' },
    { label: '平均滞留', value: avg, unit: '小时', icon: 'Timer', color: '#f59e0b', foot: '自入库时刻起算' },
    { label: '最长滞留', value: max, unit: '小时', icon: 'Warning', color: '#fb7185', foot: '建议优先退回处理' },
    { label: '7天以上', value: stayCount('168'), unit: '件', icon: 'CircleClose', color: '#a1abbf', foot: '多次催领无响应' }
  ]
})

async function load() {
  loading.value = true
  try {
    const data = await overdueParcels({ page: 1, size: 100 })
    all.value = data.records
    selected.value = new Set()
    page.value = 1
  } finally { loading.value = false }
}

function onSelect(list) { selected.value = new Set(list.map(r => r.id)) }
function toggle(row, v) {
  const next = new Set(selected.value)
  v ? next.add(row.id) : next.delete(row.id)
  selected.value = next
}

function openDetail(row) { detailId.value = row.id; detailVisible.value = true }

async function doResend(row) {
  await resendNotification(row.id)
  ElMessage.success('已补发催领提醒')
}

async function bulkRemind() {
  const ids = [...selected.value]
  if (!ids.length) return
  bulkLoading.value = true
  let ok = 0
  try {
    for (const id of ids) {
      try { await resendNotification(id); ok++ } catch { /* 单件失败不中断 */ }
    }
    ElMessage.success(`已催领 ${ok} / ${ids.length} 件`)
    selected.value = new Set()
  } finally { bulkLoading.value = false }
}

function openReturn(row) { returnRow.value = row; bulkReturn.value = false; returnReason.value = ''; returnVisible.value = true }
function openBulkReturn() { bulkReturn.value = true; returnReason.value = ''; returnVisible.value = true }

async function doReturn() {
  if (!returnReason.value.trim()) return ElMessage.warning('请填写退回原因')
  returnLoading.value = true
  try {
    const ids = bulkReturn.value ? [...selected.value] : [returnRow.value.id]
    let ok = 0
    for (const id of ids) {
      try { await returnParcel(id, returnReason.value.trim()); ok++ } catch { /* 单件失败不中断 */ }
    }
    ElMessage.success(`已登记退回 ${ok} / ${ids.length} 件`)
    returnVisible.value = false
    selected.value = new Set()
    load()
  } finally { returnLoading.value = false }
}

onMounted(async () => {
  stations.value = await listStations()
  load()
})
</script>

<style scoped>
.status-seg { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.stay-chip {
  display: inline-block; font-size: 11.5px; font-weight: 800; font-variant-numeric: tabular-nums;
  color: #b45309; background: rgba(245, 158, 11, 0.14); border: 1px solid rgba(245, 158, 11, 0.4);
  border-radius: 999px; padding: 2px 9px; white-space: nowrap;
}
.stay-chip.hot { color: #e11d48; background: rgba(225, 29, 72, 0.12); border-color: rgba(225, 29, 72, 0.4); }
.stay-chip.critical { color: #fff; background: linear-gradient(120deg, #9f1239, #e11d48); border-color: transparent; }
.ap-card-item.picked { border-color: #e11d48; box-shadow: 0 0 0 2px rgba(225, 29, 72, 0.22); }
.quick-reasons { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.quick-reasons button {
  border: 1px solid var(--default-border); background: transparent; color: var(--art-gray-600);
  border-radius: 999px; padding: 4px 11px; font-size: 11.5px; cursor: pointer; transition: all 0.18s var(--flair-ease);
}
.quick-reasons button:hover { border-color: #e11d48; color: #e11d48; transform: translateY(-1px); }
</style>