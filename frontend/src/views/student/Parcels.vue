<template>
  <div class="ap">
    <PageHero tone="parcel" icon="Box" chip="MY PARCELS" live title="我的包裹"
      sub="到站入库、上架定位、催领与出库全程留痕。在库包裹凭取件码或身份条码到柜台领取，也可授权同学免费代取。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Postcard" @click="$router.push('/student/identity')">身份条码</el-button>
        <el-button type="default" :icon="Share" @click="$router.push('/student/received')">受托代取</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <AdBanner slot="HOME_BANNER" hero style="margin-bottom:12px" />
    <AdBanner slot="PICKUP_SIDE" style="margin-bottom:16px" />

    <PanelCard title="包裹清单" :note="`${filtered.length} / ${all.length} 件`" icon="Files" accent="parcel" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索运单号 / 快递公司 / 驿站</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 SF6031、顺丰、东区菜鸟" />
        </div>
      </div>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="s in statusTabs" :key="s.key" :class="{ on: status === s.key }" @click="status = s.key; page = 1">
            {{ s.label }}<span class="n">{{ countOf(s.key) }}</span>
          </button>
        </div>
      </div>

      <div v-if="canPickCount" class="ap-note" style="--nc:#f59e0b">
        <el-icon><AlarmClock /></el-icon>
        <span><b>{{ canPickCount }} 件待取。</b>柜台出示取件码，或在「身份条码」页出示 Code 128 条纹码免码出库；超期未取会被标记滞留并推送催领。</span>
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
            <el-table-column label="状态" prop="status" sortable="custom" width="104">
              <template #default="{ row }">
                <el-tag size="small" :type="dict(PARCEL_STATUS, row.status).type" effect="light">
                  {{ dict(PARCEL_STATUS, row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="驿站 / 货架" prop="stationName" sortable="custom" min-width="160">
              <template #default="{ row }">
                {{ row.stationName }}
                <div class="ap-sub ap-mono">{{ row.shelfCode ? `货架 ${row.shelfCode}` : '未上架' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="取件码" prop="pickupCode" sortable="custom" width="110">
              <template #default="{ row }">
                <button v-if="canPick(row)" class="code-chip" title="点击复制" @click.stop="copyCode(row)">
                  {{ row.pickupCode }}<el-icon :size="12"><DocumentCopy /></el-icon>
                </button>
                <span v-else class="ap-sub">—</span>
              </template>
            </el-table-column>
            <el-table-column label="入库时间" prop="inboundTime" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></template>
            </el-table-column>
            <el-table-column label="代取授权" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.activeAuthId" type="success" size="small" effect="plain">{{ row.activeAuthGrantee }} 代取中</el-tag>
                <span v-else class="ap-sub">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="252" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
                  <el-button v-if="canPick(row) && !row.activeAuthId" link type="success" :icon="Share" @click.stop="openAuth(row)">代取</el-button>
                  <el-button v-if="canPick(row)" link type="warning" :icon="Bell" @click.stop="remind(row)">催领</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in paged" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <b class="ap-mono">{{ row.trackingNo }}</b>
              <el-tag size="small" :type="dict(PARCEL_STATUS, row.status).type" effect="light">
                {{ dict(PARCEL_STATUS, row.status).label }}
              </el-tag>
              <span v-if="row.activeAuthId" class="card-auth">{{ row.activeAuthGrantee }} 代取中</span>
            </div>
            <div class="ap-card-grid">
              <div><label>快递公司</label><span>{{ courierName(row.courierCompany) }}</span></div>
              <div><label>驿站 / 货架</label><span>{{ row.stationName }}{{ row.shelfCode ? ` · ${row.shelfCode}` : '' }}</span></div>
              <div><label>入库时间</label><span class="ap-mono">{{ fmtTime(row.inboundTime) }}</span></div>
              <div v-if="canPick(row)"><label>取件码</label><span class="ap-mono code-big">{{ row.pickupCode }}</span></div>
            </div>
            <div class="ap-card-actions">
              <el-button size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="canPick(row) && !row.activeAuthId" size="small" type="success" plain :icon="Share" @click.stop="openAuth(row)">找人代取</el-button>
              <el-button v-if="canPick(row)" size="small" type="warning" plain :icon="Bell" @click.stop="remind(row)">催领提醒</el-button>
            </div>
          </article>
        </div>

        <div v-if="!filtered.length && !loading" class="ap-empty">
          <el-empty :description="all.length ? '没有匹配的包裹' : '还没有包裹到站'" :image-size="86" />
          <p>{{ all.length ? '换个关键词或状态筛选试试。' : '包裹入库后会第一时间推送取件提醒，也可让同学发布代取服务帮你送上门。' }}</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="filtered.length"
                       v-model:current-page="page" :page-size="size" />
      </div>
      <template #foot>
        <span class="foot-tip"><el-icon :size="13"><InfoFilled /></el-icon>点击行可查看完整去向轨迹；取件码仅本人可见，请勿泄露给陌生人。</span>
      </template>
    </PanelCard>

    <ParcelDetail v-model="detailVisible" :parcel-id="detailId" />
    <ProxyAuthDialog v-model="authVisible" :parcel="authParcel" @created="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AlarmClock, Bell, DocumentCopy, InfoFilled, Postcard, Refresh, Search, Share, View } from '@element-plus/icons-vue'
import { myParcels, resendNotification } from '../../api'
import { fmtTime, dict, courierName, PARCEL_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import ParcelDetail from '../../components/ParcelDetail.vue'
import ProxyAuthDialog from '../../components/ProxyAuthDialog.vue'
import AdBanner from '../../components/AdBanner.vue'

const loading = ref(false)
const all = ref([])
const truncated = ref(false)
const keyword = ref('')
const status = ref('')
const { defaultSort, onSortChange, sorted } = useTableSort('inboundTime')
const page = ref(1)
const size = 10
const detailVisible = ref(false)
const detailId = ref(null)
const authVisible = ref(false)
const authParcel = ref(null)

const canPick = row => ['IN_STOCK', 'OVERDUE'].includes(row.status)
const canPickCount = computed(() => all.value.filter(canPick).length)

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'IN_STOCK', label: '在库待取' },
  { key: 'OVERDUE', label: '滞留超期' },
  { key: 'PICKED_UP', label: '已取件' },
  { key: 'RETURNED', label: '已退回' }
]
const countOf = key => (key ? all.value.filter(p => p.status === key).length : all.value.length)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const rows = all.value.filter(p => (!status.value || p.status === status.value) &&
    (!kw || [p.trackingNo, p.courierCompany, p.stationName, p.shelfCode].some(v => (v || '').toLowerCase().includes(kw))))
  return sorted(rows)
})
const paged = computed(() => filtered.value.slice((page.value - 1) * size, page.value * size))

const heroStats = computed(() => [
  { label: '在库待取', value: countOf('IN_STOCK'), unit: '件' },
  { label: '滞留超期', value: countOf('OVERDUE'), unit: '件' },
  { label: '代取授权中', value: all.value.filter(p => p.activeAuthId).length, unit: '件' }
])
const metrics = computed(() => {
  const month = new Date().toISOString().slice(0, 7)
  return [
    { label: '累计包裹', value: all.value.length, unit: '件', icon: 'Box', color: '#5b7cfa', foot: truncated.value ? '仅加载最近 100 件' : '含已取件与退回' },
    { label: '待取合计', value: canPickCount.value, unit: '件', icon: 'AlarmClock', color: '#f59e0b', foot: '在库 + 滞留超期' },
    { label: '本月入库', value: all.value.filter(p => (p.inboundTime || '').slice(0, 7) === month).length, unit: '件', icon: 'Calendar', color: '#0ea5e9', foot: '自然月统计' },
    { label: '已完成取件', value: countOf('PICKED_UP'), unit: '件', icon: 'CircleCheck', color: '#10d9b5', foot: '柜台核销出库' }
  ]
})

async function load() {
  loading.value = true
  try {
    const data = await myParcels({ page: 1, size: 100 })
    all.value = data.records
    truncated.value = data.total > data.records.length
    page.value = 1
  } finally { loading.value = false }
}

function openDetail(row) { detailId.value = row.id; detailVisible.value = true }
function openAuth(row) { authParcel.value = row; authVisible.value = true }

async function copyCode(row) {
  try {
    await navigator.clipboard.writeText(row.pickupCode)
    ElMessage.success(`取件码 ${row.pickupCode} 已复制`)
  } catch { ElMessage.warning('复制失败，请手动记录') }
}

async function remind(row) {
  await resendNotification(row.id)
  ElMessage.success('已重新推送取件提醒')
}

onMounted(load)
</script>

<style scoped>
.status-seg { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.code-chip {
  display: inline-flex; align-items: center; gap: 6px;
  font-family: 'JetBrains Mono', Consolas, monospace; font-size: 15px; font-weight: 800; letter-spacing: 2px;
  color: #b45309; background: linear-gradient(120deg, #fff7e6, #ffedd5);
  border: 1px dashed #f59e0b; border-radius: 8px; padding: 3px 9px; cursor: pointer;
  transition: transform 0.2s var(--flair-ease), box-shadow 0.2s;
}
.code-chip:hover { transform: translateY(-1px); box-shadow: 0 6px 16px -8px rgba(245, 158, 11, 0.7); }
html.dark .code-chip { color: #fbbf24; background: rgba(245, 158, 11, 0.12); }
.code-big { font-size: 16px; font-weight: 800; letter-spacing: 2px; color: #b45309; }
html.dark .code-big { color: #fbbf24; }
.card-auth { margin-left: auto; font-size: 11px; font-weight: 700; color: #0d9488; }
html.dark .card-auth { color: #35e0c2; }
.foot-tip { display: inline-flex; align-items: center; gap: 6px; font-size: 11.5px; color: var(--art-gray-500); }
.ap-note { margin-bottom: 12px; }
</style>