<template>
  <div class="ap">
    <PageHero tone="notice" icon="Picture" chip="AD SLOTS" live title="广告位管理"
      sub="维护三个站内推广位：首页横幅、取件页侧栏、钱包页底部。设置投放时段与权重后自动轮播展示，停用即时下线，不投放外链也可作纯公告位使用。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit()">新增广告</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="广告清单" :note="`${rows.length} 条广告`" icon="Film" accent="notice" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>广告位</label>
          <el-select v-model="slotFilter" clearable placeholder="全部广告位">
            <el-option v-for="(label, key) in AD_SLOT" :key="key" :label="label" :value="key" />
          </el-select>
        </div>
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="statusFilter" clearable placeholder="全部状态">
            <el-option label="投放中" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </div>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="sorted(filtered)" stripe :default-sort="defaultSort" @sort-change="onSortChange">
            <el-table-column label="广告位" prop="slotKey" sortable="custom" width="128">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ AD_SLOT[row.slotKey] || row.slotKey }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标题 / 文案" min-width="230">
              <template #default="{ row }">
                <div class="ad-cell">
                  <b :style="{ color: themeColor(row.theme) }">{{ row.title }}</b>
                  <span v-if="row.content">{{ row.content }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="主题" prop="theme" sortable="custom" width="92">
              <template #default="{ row }">
                <span class="theme-dot" :style="{ background: themeColor(row.theme) }"></span>{{ themeLabel(row.theme) }}
              </template>
            </el-table-column>
            <el-table-column label="权重" prop="weight" sortable="custom" width="80" align="center">
              <template #default="{ row }"><span class="ap-mono">{{ row.weight }}</span></template>
            </el-table-column>
            <el-table-column label="投放时段" min-width="220">
              <template #default="{ row }">
                <span class="ap-mono time-range">{{ fmtTime(row.startAt) }} ~ {{ fmtTime(row.endAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" sortable="custom" width="92">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                  {{ row.status === 1 ? '投放中' : '已停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="196" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-button size="small" text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button size="small" text type="danger" @click="doDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sorted(filtered)" :key="row.id" class="ap-card-item">
            <div class="ap-card-top">
              <el-tag size="small" effect="plain">{{ AD_SLOT[row.slotKey] || row.slotKey }}</el-tag>
              <b :style="{ color: themeColor(row.theme) }">{{ row.title }}</b>
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ row.status === 1 ? '投放中' : '已停用' }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>权重</label><span class="ap-mono">{{ row.weight }}</span></div>
              <div><label>主题</label><span>{{ themeLabel(row.theme) }}</span></div>
              <div><label>开始</label><span class="ap-mono">{{ fmtTime(row.startAt) }}</span></div>
              <div><label>结束</label><span class="ap-mono">{{ fmtTime(row.endAt) }}</span></div>
            </div>
            <div class="card-ops">
              <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
              <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" plain @click="doDelete(row)">删除</el-button>
            </div>
          </article>
        </div>

        <div v-if="!filtered.length && !loading" class="ap-empty">
          <el-empty description="暂无广告" :image-size="86" />
          <p>点击右上角「新增广告」创建第一条站内推广。</p>
        </div>
      </div>
    </PanelCard>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑广告' : '新增广告'" width="560px" append-to-body>
      <el-form class="ap-form" label-position="top" @submit.prevent>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>广告位<em>*</em></label>
            <el-select v-model="form.slotKey" style="width:100%">
              <el-option v-for="(label, key) in AD_SLOT" :key="key" :label="label" :value="key" />
            </el-select>
          </div>
          <div class="ap-field-row">
            <label>视觉主题</label>
            <el-select v-model="form.theme" style="width:100%">
              <el-option v-for="(t, key) in AD_THEME" :key="key" :label="t.label" :value="key">
                <span class="theme-dot" :style="{ background: t.color }"></span>{{ t.label }}
              </el-option>
            </el-select>
          </div>
        </div>
        <div class="ap-field-row">
          <label>标题<em>*</em></label>
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="例如：双十一寄件立减，兑换码领不停" />
        </div>
        <div class="ap-field-row">
          <label>副文案</label>
          <el-input v-model="form.content" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="补充说明，展示在标题下方（可留空）" />
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>跳转链接</label>
            <el-input v-model="form.linkUrl" maxlength="200" placeholder="https://… 或 /student/coupons" />
            <div class="ap-form-tip">http(s) 外链或站内路径，留空为纯展示。</div>
          </div>
          <div class="ap-field-row">
            <label>展示权重</label>
            <el-input-number v-model="form.weight" :min="0" :max="999" style="width:100%" />
            <div class="ap-form-tip">数值越大越靠前展示。</div>
          </div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>开始时间</label>
            <el-date-picker v-model="form.startAt" type="datetime" style="width:100%"
                            placeholder="留空立即生效" value-format="YYYY-MM-DDTHH:mm:ss" />
          </div>
          <div class="ap-field-row">
            <label>结束时间</label>
            <el-date-picker v-model="form.endAt" type="datetime" style="width:100%"
                            placeholder="留空长期投放" value-format="YYYY-MM-DDTHH:mm:ss" />
          </div>
        </div>
        <div class="ap-field-row switch-row">
          <label>投放状态</label>
          <el-switch v-model="form.enabled" active-text="投放中" inactive-text="停用" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ form.id ? '保存修改' : '创建广告' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { adminAds, adminCreateAd, adminUpdateAd, adminDeleteAd } from '../../api'
import { fmtTime, AD_SLOT, AD_THEME } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const loading = ref(false)
const rows = ref([])
const slotFilter = ref('')
const statusFilter = ref(null)
const { defaultSort, onSortChange, sorted } = useTableSort('createdAt')

const editVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, slotKey: 'HOME_BANNER', theme: 'neutral', title: '', content: '', linkUrl: '', weight: 0, startAt: null, endAt: null, enabled: true })

const themeColor = t => (AD_THEME[t] || {}).color || '#5b7cfa'
const themeLabel = t => (AD_THEME[t] || {}).label || t

const filtered = computed(() => rows.value.filter(r =>
  (!slotFilter.value || r.slotKey === slotFilter.value) &&
  (statusFilter.value === null || statusFilter.value === '' || r.status === statusFilter.value)
))
const liveCount = computed(() => rows.value.filter(r => r.status === 1).length)

const heroStats = computed(() => [
  { label: '广告总数', value: rows.value.length, unit: '条' },
  { label: '投放中', value: liveCount.value, unit: '条' },
  { label: '已停用', value: rows.value.length - liveCount.value, unit: '条' }
])
const metrics = computed(() => [
  ...Object.entries(AD_SLOT).map(([key, label], i) => ({
    label, value: rows.value.filter(r => r.slotKey === key && r.status === 1).length, unit: '条',
    icon: ['Postcard', 'Aim', 'Wallet'][i], color: ['#5b7cfa', '#f59e0b', '#0d9488'][i], foot: '该位投放中的广告数'
  })),
  { label: '停用广告', value: rows.value.length - liveCount.value, unit: '条', icon: 'VideoPause', color: '#7987a1', foot: '可随时重新启用' }
])

async function load() {
  loading.value = true
  try { rows.value = await adminAds() } finally { loading.value = false }
}

function openEdit(row) {
  form.id = row ? row.id : null
  form.slotKey = row ? row.slotKey : 'HOME_BANNER'
  form.theme = row ? (row.theme || 'neutral') : 'neutral'
  form.title = row ? row.title : ''
  form.content = row ? (row.content || '') : ''
  form.linkUrl = row ? (row.linkUrl || '') : ''
  form.weight = row ? (row.weight || 0) : 0
  form.startAt = row && row.startAt ? String(row.startAt).slice(0, 19) : null
  form.endAt = row && row.endAt ? String(row.endAt).slice(0, 19) : null
  form.enabled = row ? row.status === 1 : true
  editVisible.value = true
}

/* 后端要求 ISO 带时区（OffsetDateTime.parse），补本地时区偏移 */
function toIso(v) {
  if (!v) return undefined
  const d = new Date(v)
  if (!Number.isFinite(d.getTime())) return undefined
  const pad = n => String(Math.abs(n)).padStart(2, '0')
  const off = -d.getTimezoneOffset()
  const sign = off >= 0 ? '+' : '-'
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 19)
  return `${local}${sign}${pad(Math.floor(off / 60))}:${pad(off % 60)}`
}

async function save() {
  if (!form.title.trim()) return ElMessage.warning('请填写广告标题')
  if (form.linkUrl && !/^(https?:\/\/|\/)/.test(form.linkUrl.trim())) {
    return ElMessage.warning('跳转链接须为 http(s) 外链或以 / 开头的站内路径')
  }
  if (form.startAt && form.endAt && new Date(form.endAt) < new Date(form.startAt)) {
    return ElMessage.warning('结束时间不能早于开始时间')
  }
  const body = {
    slotKey: form.slotKey, theme: form.theme, title: form.title.trim(),
    content: form.content, linkUrl: form.linkUrl.trim(), weight: form.weight,
    status: form.enabled ? 1 : 0,
    startAt: toIso(form.startAt), endAt: toIso(form.endAt)
  }
  saving.value = true
  try {
    if (form.id) await adminUpdateAd(form.id, body)
    else await adminCreateAd(body)
    ElMessage.success(form.id ? '广告已更新' : '广告已创建')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}

async function toggleStatus(row) {
  const body = {
    slotKey: row.slotKey, theme: row.theme || 'neutral', title: row.title,
    content: row.content || '', linkUrl: row.linkUrl || '', weight: row.weight || 0,
    status: row.status === 1 ? 0 : 1,
    startAt: row.startAt ? toIso(String(row.startAt).slice(0, 19)) : undefined,
    endAt: row.endAt ? toIso(String(row.endAt).slice(0, 19)) : undefined
  }
  await adminUpdateAd(row.id, body)
  ElMessage.success(row.status === 1 ? '广告已停用，前台即时下线' : '广告已启用')
  load()
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除广告「${row.title}」？删除后不可恢复。`, '删除广告', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '再想想'
  })
  await adminDeleteAd(row.id)
  ElMessage.success('广告已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.ad-cell { display: flex; flex-direction: column; gap: 2px; }
.ad-cell b { font-size: 13px; }
.ad-cell span { font-size: 11.5px; color: var(--art-gray-500); }
.theme-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; vertical-align: 1px; }
.time-range { font-size: 11.5px; }
.card-ops { display: flex; gap: 8px; margin-top: 10px; }
.form-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.switch-row { display: flex; align-items: center; gap: 12px; }
@media (max-width: 640px) { .form-2col { grid-template-columns: 1fr; } }
</style>