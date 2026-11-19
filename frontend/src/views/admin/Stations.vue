<template>
  <div class="ap">
    <PageHero tone="admin" icon="OfficeBuilding" chip="STATION & SHELF" live title="驿站与货架"
      sub="驿站网点与货架容量的治理台：新建 / 编辑驿站信息，按编码规则开设货架，停用货架即刻阻断入库，容量占用实时可见。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openStation()">新建驿站</el-button>
        <el-button type="default" :icon="Grid" @click="tab = 'shelf'; shelfVisible = true">新建货架</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard :title="tab === 'station' ? '驿站网点' : '货架台账'"
               :note="tab === 'station' ? `${stations.length} 个网点` : `${shelves.length} 个货架`"
               icon="OfficeBuilding" accent="trace" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="stLoading || shLoading" @click="reload">刷新</el-button>
      </template>

      <div class="ap-seg tab-seg">
        <button :class="{ on: tab === 'station' }" @click="tab = 'station'">
          <el-icon :size="13"><OfficeBuilding /></el-icon>驿站管理<span class="n">{{ stations.length }}</span>
        </button>
        <button :class="{ on: tab === 'shelf' }" @click="tab = 'shelf'">
          <el-icon :size="13"><Grid /></el-icon>货架管理<span class="n">{{ shelves.length }}</span>
        </button>
      </div>

      <template v-if="tab === 'station'">
        <div v-loading="stLoading" class="station-grid">
          <article v-for="(s, i) in stations" :key="s.id" class="station-card flair-in" :style="{ '--i': Math.min(i, 10) }"
                   :class="{ off: s.status !== 1 }">
            <div class="st-head">
              <b>{{ s.name }}</b>
              <el-tag size="small" :type="s.status === 1 ? 'success' : 'info'" effect="light">
                {{ s.status === 1 ? '营业中' : '停用' }}
              </el-tag>
            </div>
            <div class="st-loc"><el-icon :size="13"><Location /></el-icon>{{ s.location }}</div>
            <div class="ap-kv">
              <div><label>营业时间</label><span class="ap-mono">{{ s.openHours || '—' }}</span></div>
              <div><label>联系方式</label><span class="ap-mono">{{ s.contact || '—' }}</span></div>
              <div><label>货架数</label><span>{{ shelfCountOf(s.id) }} 个</span></div>
              <div><label>空闲位</label><span>{{ freeOf(s.id) }} 位</span></div>
            </div>
            <div class="st-foot">
              <el-button size="small" type="primary" plain :icon="Edit" @click="openStation(s)">编辑驿站</el-button>
            </div>
          </article>
        </div>
        <div v-if="!stations.length && !stLoading" class="ap-empty">
          <el-empty description="还没有驿站网点" :image-size="86" />
          <p>点击右上角「新建驿站」开设第一个网点。</p>
        </div>
      </template>

      <template v-else>
        <div class="ap-toolbar">
          <div class="ap-field">
            <label>所属驿站</label>
            <el-select v-model="shelfStationId" clearable placeholder="全部驿站" @change="loadShelves">
              <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
          <div class="ap-field ap-grow">
            <label>货架编码</label>
            <el-input v-model="shelfKeyword" :prefix-icon="Search" clearable placeholder="如 A-01" />
          </div>
          <div class="ap-actions">
            <el-button type="success" :icon="Plus" @click="openShelf">新建货架</el-button>
          </div>
        </div>

        <div v-loading="shLoading">
          <div class="ap-table-wrap">
            <el-table :data="sortedShelves" stripe :default-sort="defaultSort" @sort-change="onSortChange">
              <el-table-column label="货架编码" prop="code" sortable="custom" width="130">
                <template #default="{ row }"><b class="ap-mono ap-strong">{{ row.code }}</b></template>
              </el-table-column>
              <el-table-column label="所属驿站" prop="stationName" sortable="custom" min-width="150">
                <template #default="{ row }">{{ stationName(row.stationId) }}</template>
              </el-table-column>
              <el-table-column label="占用 / 容量" prop="occupancy" sortable="custom" min-width="200">
                <template #default="{ row }">
                  <div class="cap-cell">
                    <div class="cap-bar"><i :style="{ width: pct(row) + '%' }" :class="capClass(row)" /></div>
                    <span class="ap-mono">{{ row.occupied }} / {{ row.capacity }}（{{ pct(row) }}%）</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" prop="status" sortable="custom" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-popconfirm :title="row.status === 1 ? '停用后不可再入库到该货架，确定？' : '确定重新启用该货架？'"
                                 width="240" @confirm="toggleShelf(row)">
                    <template #reference>
                      <el-button link :type="row.status === 1 ? 'danger' : 'success'" :icon="row.status === 1 ? CircleClose : CircleCheck">
                        {{ row.status === 1 ? '停用' : '启用' }}
                      </el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="ap-cards">
            <article v-for="row in filteredShelves" :key="row.id" class="ap-card-item" :class="{ off: row.status !== 1 }">
              <div class="ap-card-top">
                <b class="ap-mono">{{ row.code }}</b>
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="cap-cell">
                <div class="cap-bar"><i :style="{ width: pct(row) + '%' }" :class="capClass(row)" /></div>
                <span class="ap-mono">{{ row.occupied }} / {{ row.capacity }}（{{ pct(row) }}%）</span>
              </div>
              <div class="ap-card-grid">
                <div><label>所属驿站</label><span>{{ stationName(row.stationId) }}</span></div>
                <div><label>空闲位</label><span>{{ Math.max(0, row.capacity - row.occupied) }} 位</span></div>
              </div>
              <div class="ap-card-actions">
                <el-popconfirm :title="row.status === 1 ? '停用后不可再入库到该货架，确定？' : '确定重新启用该货架？'"
                               width="240" @confirm="toggleShelf(row)">
                  <template #reference>
                    <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain :icon="row.status === 1 ? CircleClose : CircleCheck">
                      {{ row.status === 1 ? '停用货架' : '启用货架' }}
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </article>
          </div>

          <div v-if="!filteredShelves.length && !shLoading" class="ap-empty">
            <el-empty :description="shelves.length ? '没有匹配的货架' : '暂无货架'" :image-size="86" />
            <p>{{ shelves.length ? '调整驿站或编码筛选。' : '点击「新建货架」按 A-01-03 规则开设。' }}</p>
          </div>
        </div>
      </template>
    </PanelCard>

    <el-dialog v-model="stVisible" :title="stForm.id ? '编辑驿站' : '新建驿站'" width="480px" append-to-body>
      <el-form ref="stRef" :model="stForm" :rules="stRules" label-position="top" class="ap-form">
        <el-form-item label="驿站名称" prop="name">
          <el-input v-model="stForm.name" placeholder="如 东区驿站" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="stForm.location" placeholder="如 学生公寓 3 号楼架空层" />
        </el-form-item>
        <div class="form-duo">
          <el-form-item label="营业时间">
            <el-input v-model="stForm.openHours" placeholder="如 08:00-21:00" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="stForm.contact" placeholder="电话 / 短号" />
          </el-form-item>
        </div>
        <el-form-item label="营业状态">
          <el-switch v-model="stForm.status" :active-value="1" :inactive-value="0"
                     active-text="营业" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shelfVisible" title="新建货架" width="440px" append-to-body>
      <el-form ref="shRef" :model="shForm" :rules="shRules" label-position="top" class="ap-form">
        <el-form-item label="所属驿站" prop="stationId">
          <el-select v-model="shForm.stationId" style="width: 100%" placeholder="选择驿站">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <div class="form-duo">
          <el-form-item label="货架编码" prop="code">
            <el-input v-model="shForm.code" placeholder="如 A-01-03" />
          </el-form-item>
          <el-form-item label="容量（位）" prop="capacity">
            <el-input-number v-model="shForm.capacity" :min="1" :max="500" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="ap-note" style="--nc:#0d9488">
          <el-icon><InfoFilled /></el-icon>
          <span>编码规则：区字母-排号-位号（如 A-01-03）。创建后即在入库工作台可选，占用率随上架实时变化。</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="shelfVisible = false">取消</el-button>
        <el-button type="primary" :icon="Plus" :loading="saving" @click="saveShelf">创建货架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Grid, InfoFilled, Location, OfficeBuilding, Plus, Refresh, Search, CircleClose, CircleCheck } from '@element-plus/icons-vue'
import { listStations, createStation, updateStation, listShelves, createShelf, setShelfStatus } from '../../api'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const tab = ref('station')
const stLoading = ref(false)
const shLoading = ref(false)
const saving = ref(false)
const stations = ref([])
const shelves = ref([])
const shelfStationId = ref(null)
const shelfKeyword = ref('')
const stVisible = ref(false)
const shelfVisible = ref(false)
const stRef = ref()
const shRef = ref()
const stForm = reactive({ id: null, name: '', location: '', openHours: '08:00-21:00', contact: '', status: 1 })
const shForm = reactive({ stationId: null, code: '', capacity: 40 })
const stRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }]
}
const shRules = {
  stationId: [{ required: true, message: '请选择驿站', trigger: 'change' }],
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[A-Z]-\d{2}-\d{2}$/, message: '格式如 A-01-03', trigger: 'blur' }
  ]
}

const stationName = id => stations.value.find(s => s.id === id)?.name || id
const pct = row => (row.capacity ? Math.min(100, Math.round((row.occupied / row.capacity) * 100)) : 0)
const capClass = row => (pct(row) >= 90 ? 'hot' : pct(row) >= 70 ? 'warm' : '')
const shelfCountOf = id => shelves.value.filter(s => s.stationId === id).length
const freeOf = id => shelves.value.filter(s => s.stationId === id && s.status === 1)
  .reduce((n, s) => n + Math.max(0, s.capacity - s.occupied), 0)

const filteredShelves = computed(() => {
  const kw = shelfKeyword.value.trim().toLowerCase()
  return shelves.value.filter(s => !kw || s.code.toLowerCase().includes(kw))
})
const { defaultSort, onSortChange, sorted } = useTableSort('code', {
  stationName: r => stationName(r.stationId),
  occupancy: r => pct(r)
})
const sortedShelves = computed(() => sorted(filteredShelves.value))

const heroStats = computed(() => [
  { label: '驿站', value: stations.value.length, unit: '个' },
  { label: '货架', value: shelves.value.length, unit: '个' },
  { label: '空闲位', value: shelves.value.reduce((n, s) => n + Math.max(0, s.capacity - s.occupied), 0), unit: '位' }
])
const metrics = computed(() => {
  const cap = shelves.value.reduce((n, s) => n + s.capacity, 0)
  const occ = shelves.value.reduce((n, s) => n + s.occupied, 0)
  const on = stations.value.filter(s => s.status === 1).length
  return [
    { label: '驿站网点', value: stations.value.length, unit: '个', icon: 'OfficeBuilding', color: '#0d9488', foot: `营业中 ${on} 个` },
    { label: '货架总数', value: shelves.value.length, unit: '个', icon: 'Grid', color: '#0ea5e9', foot: `启用 ${shelves.value.filter(s => s.status === 1).length} 个` },
    { label: '总容量', value: cap, unit: '位', icon: 'Box', color: '#f59e0b', foot: `已占用 ${occ} 位` },
    { label: '整体占用率', value: cap ? Math.round((occ / cap) * 100) : 0, unit: '%', icon: 'Odometer', color: '#8b5cf6', foot: '≥70% 建议扩容' }
  ]
})

async function loadStations() {
  stLoading.value = true
  try { stations.value = await listStations() } finally { stLoading.value = false }
}

async function loadShelves() {
  shLoading.value = true
  try { shelves.value = await listShelves(shelfStationId.value || undefined) } finally { shLoading.value = false }
}

function reload() { loadStations(); loadShelves() }

function openStation(row) {
  Object.assign(stForm, row
    ? { id: row.id, name: row.name, location: row.location, openHours: row.openHours, contact: row.contact, status: row.status }
    : { id: null, name: '', location: '', openHours: '08:00-21:00', contact: '', status: 1 })
  stVisible.value = true
}

function openShelf() {
  shForm.stationId = shelfStationId.value || (stations.value[0]?.id ?? null)
  shForm.code = ''
  shForm.capacity = 40
  shelfVisible.value = true
}

async function saveStation() {
  await stRef.value.validate()
  saving.value = true
  try {
    const { id, ...data } = stForm
    if (id) await updateStation(id, data)
    else await createStation(data)
    ElMessage.success('已保存')
    stVisible.value = false
    loadStations()
  } finally { saving.value = false }
}

async function saveShelf() {
  await shRef.value.validate()
  saving.value = true
  try {
    await createShelf({ ...shForm })
    ElMessage.success('货架已创建')
    shelfVisible.value = false
    shForm.code = ''
    loadShelves()
  } finally { saving.value = false }
}

async function toggleShelf(row) {
  await setShelfStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('状态已更新')
  loadShelves()
}

onMounted(async () => {
  await loadStations()
  loadShelves()
})
</script>

<style scoped>
.tab-seg { margin-bottom: 14px; }
.tab-seg button { display: inline-flex; align-items: center; gap: 6px; }
.station-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 290px), 1fr)); gap: 12px; }
.station-card {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 15px;
  background: var(--art-card-bg, #fff); display: flex; flex-direction: column; gap: 10px;
  transition: transform 0.25s var(--flair-ease), box-shadow 0.25s, border-color 0.25s;
}
html.dark .station-card { background: #17171c; }
.station-card:hover { transform: translateY(-3px); box-shadow: var(--art-card-shadow-hover); border-color: rgba(13, 148, 136, 0.5); }
.station-card.off { opacity: 0.62; }
.st-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.st-head b { font-size: 15.5px; color: var(--art-gray-900); }
.st-loc { display: flex; align-items: center; gap: 5px; font-size: 12.5px; color: var(--art-gray-600); }
.st-foot { display: flex; justify-content: flex-end; }
.cap-cell { display: flex; flex-direction: column; gap: 5px; padding: 2px 0; }
.cap-cell span { font-size: 11.5px; color: var(--art-gray-500); }
.cap-bar { height: 7px; border-radius: 999px; background: var(--art-gray-100); overflow: hidden; min-width: 120px; }
html.dark .cap-bar { background: #26262e; }
.cap-bar i { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #10d9b5, #38bdf8); transition: width 0.6s var(--flair-ease); }
.cap-bar i.warm { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.cap-bar i.hot { background: linear-gradient(90deg, #e11d48, #fb7185); }
.ap-card-item.off { opacity: 0.66; }
.form-duo { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
@media (max-width: 560px) { .form-duo { grid-template-columns: 1fr; gap: 0; } }
</style>