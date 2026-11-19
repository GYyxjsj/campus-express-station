<template>
  <div class="ap">
    <PageHero tone="trace" icon="Grid" chip="SHELF MAP" live title="货架查看"
      sub="按驿站查看货架启用状态与占用率，点击货架可展开当前在架包裹清单，便于盘点、移架与清理滞留位。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Download" @click="$router.push('/staff/inbound')">入库上架</el-button>
        <el-button type="default" :icon="Guide" @click="$router.push('/staff/traces')">移架留痕台账</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard :title="stationName ? `${stationName} · 货架分布` : '货架分布'" :note="`${filtered.length} / ${shelves.length} 个`"
               icon="Grid" accent="trace" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-seg station-seg">
        <button v-for="s in stations" :key="s.id" :class="{ on: stationId === s.id }" @click="stationId = s.id; load()">
          <el-icon :size="13"><OfficeBuilding /></el-icon>{{ s.name }}
        </button>
      </div>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>货架编号</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 A-02" />
        </div>
        <div class="ap-field">
          <label>排序</label>
          <el-select v-model="sort">
            <el-option label="占用率最高" value="pct" />
            <el-option label="空闲位最多" value="free" />
            <el-option label="编号 A→Z" value="code" />
          </el-select>
        </div>
      </div>

      <div class="status-seg">
        <div class="ap-seg">
          <button v-for="f in filterTabs" :key="f.key" :class="{ on: filter === f.key }" @click="filter = f.key">
            {{ f.label }}<span class="n">{{ filterCount(f.key) }}</span>
          </button>
        </div>
      </div>

      <div v-loading="loading" class="shelf-grid">
        <article v-for="(sh, i) in filtered" :key="sh.id" class="shelf-card flair-in" :style="{ '--i': Math.min(i, 12) }"
                 :class="{ off: sh.status !== 1 }" @click="openShelf(sh)">
          <div class="sh-head">
            <b class="ap-mono">{{ sh.code }}</b>
            <el-tag size="small" :type="sh.status === 1 ? 'success' : 'info'" effect="light">
              {{ sh.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <div class="sh-pct">
            <b>{{ pct(sh) }}<small>%</small></b>
            <span>{{ sh.occupied }} / {{ sh.capacity }} 占用</span>
          </div>
          <div class="cap-bar"><i :style="{ width: pct(sh) + '%' }" :class="capClass(sh)" /></div>
          <div class="sh-foot">
            <span :class="{ warn: sh.capacity - sh.occupied <= 2 && sh.status === 1 }">
              空闲 {{ Math.max(0, sh.capacity - sh.occupied) }} 位
            </span>
            <span class="sh-link">在架包裹<el-icon :size="12"><ArrowRight /></el-icon></span>
          </div>
        </article>
      </div>

      <div v-if="!filtered.length && !loading" class="ap-empty">
        <el-empty :description="shelves.length ? '没有匹配的货架' : '该驿站暂无货架'" :image-size="86" />
        <p>{{ shelves.length ? '调整编号关键词或筛选条件。' : '请联系管理员在「驿站与货架」中创建货架。' }}</p>
      </div>
    </PanelCard>

    <el-drawer v-model="shelfOpen" :title="currentShelf ? `货架 ${currentShelf.code} · 在架包裹` : '货架详情'" size="480px" append-to-body>
      <div v-if="currentShelf" class="detail">
        <div class="ap-kv">
          <div><label>所属驿站</label><span>{{ stationName }}</span></div>
          <div><label>状态</label><span>{{ currentShelf.status === 1 ? '启用中' : '已停用' }}</span></div>
          <div><label>占用 / 容量</label><span class="ap-mono">{{ currentShelf.occupied }} / {{ currentShelf.capacity }}</span></div>
          <div><label>占用率</label><span>{{ pct(currentShelf) }}%</span></div>
        </div>
        <div class="cap-bar big"><i :style="{ width: pct(currentShelf) + '%' }" :class="capClass(currentShelf)" /></div>
        <h4 class="d-h">在架包裹（{{ shelfParcels.length }}）</h4>
        <div v-if="shelfParcels.length" class="ap-list">
          <div v-for="p in shelfParcels" :key="p.id" class="ap-list-item">
            <div class="ap-list-avatar" :style="{ background: 'linear-gradient(140deg,#0d9488,#34d399)' }">
              <el-icon :size="16"><Box /></el-icon>
            </div>
            <div class="ap-list-body">
              <div class="ap-list-l1">
                <b class="ap-mono">{{ p.trackingNo }}</b>
                <el-tag size="small" :type="p.status === 'OVERDUE' ? 'danger' : 'primary'" effect="plain">
                  {{ p.status === 'OVERDUE' ? '滞留' : '在库' }}
                </el-tag>
              </div>
              <div class="ap-list-l2">{{ p.ownerName }} · 入库 {{ fmtTime(p.inboundTime) }}</div>
            </div>
          </div>
        </div>
        <div v-else class="ap-empty">
          <el-empty description="该货架当前空闲" :image-size="70" />
          <p>可在入库工作台将新包裹上架到此货架。</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Box, Download, Grid, Guide, OfficeBuilding, Refresh, Search } from '@element-plus/icons-vue'
import { listStations, listShelves, pageParcels } from '../../api'
import { fmtTime } from '../../utils/format'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const stations = ref([])
const stationId = ref(null)
const shelves = ref([])
const loading = ref(false)
const keyword = ref('')
const sort = ref('pct')
const filter = ref('')
const shelfOpen = ref(false)
const currentShelf = ref(null)
const shelfParcels = ref([])

const stationName = computed(() => stations.value.find(s => s.id === stationId.value)?.name || '')
const pct = sh => (sh.capacity ? Math.min(100, Math.round((sh.occupied / sh.capacity) * 100)) : 0)
const capClass = sh => (pct(sh) >= 90 ? 'hot' : pct(sh) >= 70 ? 'warm' : '')

const filterTabs = [
  { key: '', label: '全部' },
  { key: 'on', label: '启用' },
  { key: 'off', label: '停用' },
  { key: 'full', label: '将满 ≥70%' }
]
const filterCount = key => shelves.value.filter(sh =>
  key === 'on' ? sh.status === 1 : key === 'off' ? sh.status !== 1 : key === 'full' ? pct(sh) >= 70 : true).length

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let rows = shelves.value.filter(sh =>
    (!filter.value || (filter.value === 'on' ? sh.status === 1 : filter.value === 'off' ? sh.status !== 1 : pct(sh) >= 70)) &&
    (!kw || sh.code.toLowerCase().includes(kw)))
  rows = [...rows]
  if (sort.value === 'pct') rows.sort((a, b) => pct(b) - pct(a))
  else if (sort.value === 'free') rows.sort((a, b) => (b.capacity - b.occupied) - (a.capacity - a.occupied))
  else rows.sort((a, b) => a.code.localeCompare(b.code))
  return rows
})

const heroStats = computed(() => [
  { label: '货架数', value: shelves.value.length, unit: '个' },
  { label: '空闲位', value: shelves.value.reduce((n, s) => n + Math.max(0, s.capacity - s.occupied), 0), unit: '位' },
  { label: '将满货架', value: filterCount('full'), unit: '个' }
])
const metrics = computed(() => {
  const cap = shelves.value.reduce((n, s) => n + s.capacity, 0)
  const occ = shelves.value.reduce((n, s) => n + s.occupied, 0)
  return [
    { label: '货架总数', value: shelves.value.length, unit: '个', icon: 'Grid', color: '#0d9488', foot: `启用 ${filterCount('on')} 个` },
    { label: '总容量', value: cap, unit: '位', icon: 'Box', color: '#0ea5e9', foot: '全部货架容量合计' },
    { label: '已占用', value: occ, unit: '位', icon: 'Files', color: '#f59e0b', foot: '在架包裹数' },
    { label: '整体占用率', value: cap ? Math.round((occ / cap) * 100) : 0, unit: '%', icon: 'Odometer', color: '#8b5cf6', foot: '≥70% 建议扩容或清理' }
  ]
})

async function load() {
  if (!stationId.value) return
  loading.value = true
  try { shelves.value = await listShelves(stationId.value) } finally { loading.value = false }
}

async function openShelf(sh) {
  currentShelf.value = sh
  shelfParcels.value = []
  shelfOpen.value = true
  const data = await pageParcels({ page: 1, size: 100, stationId: stationId.value, status: 'IN_STOCK' })
  const overdue = await pageParcels({ page: 1, size: 100, stationId: stationId.value, status: 'OVERDUE' })
  shelfParcels.value = [...data.records, ...overdue.records].filter(p => p.shelfCode === sh.code)
}

onMounted(async () => {
  stations.value = await listStations()
  if (stations.value.length) {
    stationId.value = stations.value[0].id
    load()
  }
})
</script>

<style scoped>
.station-seg { margin-bottom: 12px; }
.station-seg button { display: inline-flex; align-items: center; gap: 6px; }
.status-seg { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.shelf-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 215px), 1fr)); gap: 12px; }
.shelf-card {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 14px;
  background: var(--art-card-bg, #fff); cursor: pointer; position: relative; overflow: hidden;
  transition: transform 0.25s var(--flair-ease), box-shadow 0.25s, border-color 0.25s;
}
html.dark .shelf-card { background: #17171c; }
.shelf-card:hover { transform: translateY(-3px); box-shadow: var(--art-card-shadow-hover); border-color: rgba(13, 148, 136, 0.5); }
.shelf-card.off { opacity: 0.62; }
.sh-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.sh-head b { font-size: 16px; color: var(--art-gray-900); letter-spacing: 1px; }
.sh-pct { display: flex; align-items: baseline; justify-content: space-between; margin: 10px 0 7px; }
.sh-pct b { font-size: 24px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.sh-pct b small { font-size: 0.55em; margin-left: 1px; color: var(--art-gray-500); }
.sh-pct span { font-size: 11.5px; color: var(--art-gray-500); font-variant-numeric: tabular-nums; }
.cap-bar { height: 7px; border-radius: 999px; background: var(--art-gray-100); overflow: hidden; }
html.dark .cap-bar { background: #26262e; }
.cap-bar.big { height: 10px; margin: 4px 0 6px; }
.cap-bar i { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #10d9b5, #38bdf8); transition: width 0.6s var(--flair-ease); }
.cap-bar i.warm { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.cap-bar i.hot { background: linear-gradient(90deg, #e11d48, #fb7185); }
.sh-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 9px; font-size: 11.5px; color: var(--art-gray-600); }
.sh-foot .warn { color: #e11d48; font-weight: 700; }
.sh-link { display: inline-flex; align-items: center; gap: 3px; color: #0d9488; font-weight: 700; }
html.dark .sh-link { color: #34d399; }
.detail { display: flex; flex-direction: column; gap: 12px; }
.d-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 4px; }
</style>