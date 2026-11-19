<template>
  <div class="ap">
    <PageHero tone="parcel" icon="Box" chip="PARCEL OPS" live title="包裹管理"
      sub="全站包裹的检索与处置台：按运单号 / 物主 / 手机号搜索，按状态与驿站过滤，可补发到站提醒、登记退回，或查看完整去向轨迹。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Download" @click="$router.push('/staff/inbound')">入库工作台</el-button>
        <el-button type="default" :icon="AlarmClock" @click="$router.push('/staff/overdue')">滞留件管理</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="包裹台账" :note="`${total} 件`" icon="Files" accent="parcel" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" :prefix-icon="Search" clearable placeholder="运单号 / 物主姓名 / 手机号"
                    @keyup.enter="load(1)" @clear="load(1)" />
        </div>
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="query.status" clearable placeholder="全部状态" @change="load(1)">
            <el-option v-for="(v, k) in PARCEL_STATUS" :key="k" :label="v.label" :value="k" />
          </el-select>
        </div>
        <div class="ap-field">
          <label>驿站</label>
          <el-select v-model="query.stationId" clearable placeholder="全部驿站" @change="load(1)">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div v-if="selected.size" class="bulk-bar flair-in">
        <span><b>{{ selected.size }}</b> 件已选</span>
        <el-button size="small" type="warning" :icon="Bell" :loading="bulkLoading" @click="bulkRemind">批量补发提醒</el-button>
        <el-button size="small" text type="primary" @click="selected = new Set()">取消选择</el-button>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="rows" stripe :default-sort="defaultSort" @sort-change="e => { onSortChange(e); load(1) }" @row-click="openDetail" @selection-change="onSelect">
            <el-table-column type="selection" width="42" :selectable="canPick" />
            <el-table-column label="运单号" prop="trackingNo" sortable="custom" min-width="165">
              <template #default="{ row }">
                <b class="ap-mono ap-strong">{{ row.trackingNo }}</b>
                <div class="ap-sub">{{ courierName(row.courierCompany) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="物主" prop="ownerName" sortable="custom" width="140">
              <template #default="{ row }">
                {{ row.ownerName }}
                <div class="ap-sub ap-mono">{{ row.ownerUsername }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" sortable="custom" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="dict(PARCEL_STATUS, row.status).type" effect="light">{{ dict(PARCEL_STATUS, row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="驿站 / 货架" prop="stationName" sortable="custom" min-width="150">
              <template #default="{ row }">
                {{ row.stationName }}
                <div class="ap-sub ap-mono">{{ row.shelfCode || '未上架' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="取件码" prop="pickupCode" sortable="custom" width="96">
              <template #default="{ row }"><span class="ap-mono ap-strong">{{ row.pickupCode || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="入库时间" prop="inboundTime" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></template>
            </el-table-column>
            <el-table-column label="代取" width="104">
              <template #default="{ row }">
                <el-tag v-if="row.activeAuthId" type="success" size="small" effect="plain">{{ row.activeAuthGrantee }}</el-tag>
                <span v-else class="ap-sub">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="188" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
                  <el-button v-if="canPick(row)" v-perm="'reminder:send'" link type="warning" :icon="Bell" @click.stop="doResend(row)">补发</el-button>
                  <el-button v-if="canPick(row)" link type="danger" :icon="RefreshLeft" @click.stop="openReturn(row)">退回</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in rows" :key="row.id" class="ap-card-item" :class="{ picked: selected.has(row.id) }" @click="openDetail(row)">
            <div class="ap-card-top">
              <el-checkbox v-if="canPick(row)" :model-value="selected.has(row.id)" @click.stop @change="v => toggle(row, v)" />
              <b class="ap-mono">{{ row.trackingNo }}</b>
              <el-tag size="small" :type="dict(PARCEL_STATUS, row.status).type" effect="light">{{ dict(PARCEL_STATUS, row.status).label }}</el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>物主</label><span>{{ row.ownerName }}（{{ row.ownerUsername }}）</span></div>
              <div><label>驿站 / 货架</label><span>{{ row.stationName }}{{ row.shelfCode ? ` · ${row.shelfCode}` : '' }}</span></div>
              <div><label>取件码</label><span class="ap-mono">{{ row.pickupCode || '—' }}</span></div>
              <div><label>入库时间</label><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></div>
            </div>
            <div class="ap-card-actions">
              <el-button size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="canPick(row)" v-perm="'reminder:send'" size="small" type="warning" plain :icon="Bell" @click.stop="doResend(row)">补发提醒</el-button>
              <el-button v-if="canPick(row)" size="small" type="danger" plain :icon="RefreshLeft" @click.stop="openReturn(row)">退回</el-button>
            </div>
          </article>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="没有匹配的包裹" :image-size="86" />
          <p>调整关键词、状态或驿站筛选后重试。</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <ParcelDetail v-model="detailVisible" :parcel-id="detailId" />

    <el-dialog v-model="returnVisible" title="退回登记" width="460px" append-to-body>
      <div class="ap-form">
        <div class="ap-note" style="--nc:#e11d48">
          <el-icon><Warning /></el-icon>
          <span>退回后包裹状态置为已退回并写入去向台账，物主会收到通知。运单号 <b class="ap-mono">{{ returnRow?.trackingNo }}</b></span>
        </div>
        <div class="ap-field-row">
          <label>退回原因<em>*</em></label>
          <el-input v-model="returnReason" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：超期未取退回快递公司 / 物主拒收" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AlarmClock, Bell, Box, Download, Refresh, RefreshLeft, Search, View, Warning } from '@element-plus/icons-vue'
import { pageParcels, listStations, resendNotification, returnParcel } from '../../api'
import { fmtTime, dict, courierName, PARCEL_STATUS } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import ParcelDetail from '../../components/ParcelDetail.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const bulkLoading = ref(false)
const rows = ref([])
const total = ref(0)
const counts = ref({})
const stations = ref([])
const selected = ref(new Set())
const query = reactive({ page: 1, size: 10, status: '', keyword: '', stationId: null })
const { defaultSort, onSortChange, sortParams } = useServerSort('inboundTime')
const detailVisible = ref(false)
const detailId = ref(null)
const returnVisible = ref(false)
const returnLoading = ref(false)
const returnRow = ref(null)
const returnReason = ref('')
const QUICK_REASONS = ['超期未取退回快递公司', '物主拒收', '包裹破损退回', '地址信息错误']

const canPick = row => ['IN_STOCK', 'OVERDUE'].includes(row.status)

const heroStats = computed(() => [
  { label: '在库待取', value: counts.value.IN_STOCK || 0, unit: '件' },
  { label: '滞留超期', value: counts.value.OVERDUE || 0, unit: '件' },
  { label: '全部包裹', value: total.value, unit: '件' }
])
const metrics = computed(() => [
  { label: '在库待取', value: counts.value.IN_STOCK || 0, unit: '件', icon: 'Box', color: '#5b7cfa', foot: '含已上架与待上架' },
  { label: '滞留超期', value: counts.value.OVERDUE || 0, unit: '件', icon: 'AlarmClock', color: '#e11d48', foot: '超阈值自动升级' },
  { label: '已取件', value: counts.value.PICKED_UP || 0, unit: '件', icon: 'CircleCheck', color: '#10d9b5', foot: '柜台核销出库' },
  { label: '已退回', value: counts.value.RETURNED || 0, unit: '件', icon: 'RefreshLeft', color: '#a1abbf', foot: '退回快递公司留痕' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await pageParcels({
      page: query.page, size: query.size,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      stationId: query.stationId || undefined,
      ...sortParams()
    })
    rows.value = data.records
    total.value = data.total
    selected.value = new Set()
  } finally { loading.value = false }
  loadCounts()
}

async function loadCounts() {
  const keys = Object.keys(PARCEL_STATUS)
  const res = await Promise.all(keys.map(k => pageParcels({ page: 1, size: 1, status: k })))
  counts.value = Object.fromEntries(keys.map((k, i) => [k, res[i].total]))
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.stationId = null
  load(1)
}

function onSelect(list) { selected.value = new Set(list.map(r => r.id)) }
function toggle(row, v) {
  const next = new Set(selected.value)
  v ? next.add(row.id) : next.delete(row.id)
  selected.value = next
}

async function bulkRemind() {
  const ids = [...selected.value]
  if (!ids.length) return
  bulkLoading.value = true
  let ok = 0
  try {
    for (const id of ids) {
      try { await resendNotification(id); ok++ } catch { /* 单件失败不中断批量 */ }
    }
    ElMessage.success(`已补发 ${ok} / ${ids.length} 件到站提醒`)
    selected.value = new Set()
  } finally { bulkLoading.value = false }
}

function openDetail(row) { detailId.value = row.id; detailVisible.value = true }

async function doResend(row) {
  await resendNotification(row.id)
  ElMessage.success('已补发到站提醒')
}

function openReturn(row) { returnRow.value = row; returnReason.value = ''; returnVisible.value = true }

async function doReturn() {
  if (!returnReason.value.trim()) return ElMessage.warning('请填写退回原因')
  returnLoading.value = true
  try {
    await returnParcel(returnRow.value.id, returnReason.value.trim())
    ElMessage.success('已登记退回')
    returnVisible.value = false
    load()
  } finally { returnLoading.value = false }
}

onMounted(async () => {
  stations.value = await listStations()
  load()
})
</script>

<style scoped>
.bulk-bar {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  border: 1px dashed #f59e0b; border-radius: 10px; padding: 8px 12px; margin-bottom: 12px;
  background: linear-gradient(120deg, rgba(245, 158, 11, 0.1), rgba(251, 191, 36, 0.04));
  font-size: 12.5px; color: var(--art-gray-700);
}
.bulk-bar b { color: #b45309; font-size: 14px; }
html.dark .bulk-bar b { color: #fbbf24; }
.ap-card-item.picked { border-color: #f59e0b; box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.25); }
.quick-reasons { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.quick-reasons button {
  border: 1px solid var(--default-border); background: transparent; color: var(--art-gray-600);
  border-radius: 999px; padding: 4px 11px; font-size: 11.5px; cursor: pointer; transition: all 0.18s var(--flair-ease);
}
.quick-reasons button:hover { border-color: #e11d48; color: #e11d48; transform: translateY(-1px); }
</style>