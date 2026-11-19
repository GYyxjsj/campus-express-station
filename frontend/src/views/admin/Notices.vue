<template>
  <div class="ap">
    <PageHero tone="notice" icon="Bell" chip="NOTICE DESK" live title="公告管理"
      sub="面向全校的驿站公告：发布后学生端公告栏与登录弹窗同步可见，下线即刻隐藏。支持编辑修订与重新发布，全程留痕。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="open()">发布公告</el-button>
        <el-button type="default" :icon="View" @click="$router.push('/notices')">学生视角预览</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="公告台账" :note="`${filtered.length} / ${all.length} 条`" icon="ChatDotSquare" accent="notice" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索标题 / 内容</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 营业时间、双十一" />
        </div>
        <div class="ap-field">
          <label>排序</label>
          <el-select v-model="sort">
            <el-option label="最新发布" value="new" />
            <el-option label="最早发布" value="old" />
            <el-option label="最近更新" value="upd" />
          </el-select>
        </div>
      </div>

      <div class="ap-seg status-seg">
        <button v-for="s in statusTabs" :key="s.key" :class="{ on: status === s.key }" @click="status = s.key">
          {{ s.label }}<span class="n">{{ statusCount(s.key) }}</span>
        </button>
      </div>

      <div v-loading="loading" class="notice-list">
        <article v-for="(n, i) in filtered" :key="n.id" class="notice-item flair-in" :style="{ '--i': Math.min(i, 10) }"
                 :class="{ off: n.status !== 1 }">
          <div class="n-head">
            <b>{{ n.title }}</b>
            <el-tag size="small" :type="n.status === 1 ? 'success' : 'info'" effect="light">
              {{ n.status === 1 ? '已发布' : '已下线' }}
            </el-tag>
          </div>
          <p class="n-content">{{ n.content }}</p>
          <div class="n-foot">
            <span class="ap-mono">发布 {{ fmtTime(n.createdAt) }}</span>
            <span class="ap-mono">更新 {{ fmtTime(n.updatedAt) }}</span>
            <span class="n-actions">
              <el-button size="small" type="primary" plain :icon="Edit" @click="open(n)">编辑</el-button>
              <el-popconfirm :title="n.status === 1 ? '下线后学生端不可见，确定？' : '确定重新发布该公告？'"
                             width="220" @confirm="toggle(n)">
                <template #reference>
                  <el-button size="small" :type="n.status === 1 ? 'danger' : 'success'" plain>
                    {{ n.status === 1 ? '下线' : '发布' }}
                  </el-button>
                </template>
              </el-popconfirm>
            </span>
          </div>
        </article>
      </div>

      <div v-if="!filtered.length && !loading" class="ap-empty">
        <el-empty :description="all.length ? '没有匹配的公告' : '还没有公告'" :image-size="86" />
        <p>{{ all.length ? '调整关键词或状态筛选。' : '点击「发布公告」发布第一条驿站公告。' }}</p>
      </div>
    </PanelCard>

    <el-dialog v-model="visible" :title="form.id ? '编辑公告' : '发布公告'" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="ap-form">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="如 双十一期间营业时间调整" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="7" maxlength="2000" show-word-limit
                    placeholder="公告正文，支持换行。发布后学生端公告栏可见，并触发登录弹窗提醒。" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="发布" inactive-text="下线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, ChatDotSquare, Check, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { pageNotices, createNotice, updateNotice } from '../../api'
import { fmtTime } from '../../utils/format'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const loading = ref(false)
const saving = ref(false)
const all = ref([])
const keyword = ref('')
const sort = ref('new')
const status = ref('')
const visible = ref(false)
const formRef = ref()
const form = reactive({ id: null, title: '', content: '', status: 1 })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'on', label: '已发布' },
  { key: 'off', label: '已下线' }
]
const statusCount = key => all.value.filter(n =>
  key === 'on' ? n.status === 1 : key === 'off' ? n.status !== 1 : true).length

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let rows = all.value.filter(n =>
    (!status.value || (status.value === 'on' ? n.status === 1 : n.status !== 1)) &&
    (!kw || [n.title, n.content].some(v => (v || '').toLowerCase().includes(kw))))
  rows = [...rows]
  if (sort.value === 'new') rows.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
  else if (sort.value === 'old') rows.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
  else rows.sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
  return rows
})

const monthCount = computed(() => {
  const prefix = new Date().toISOString().slice(0, 7)
  return all.value.filter(n => (n.createdAt || '').startsWith(prefix)).length
})
const heroStats = computed(() => [
  { label: '公告总数', value: all.value.length, unit: '条' },
  { label: '已发布', value: statusCount('on'), unit: '条' },
  { label: '本月发布', value: monthCount.value, unit: '条' }
])
const metrics = computed(() => [
  { label: '公告总数', value: all.value.length, unit: '条', icon: 'ChatDotSquare', color: '#f59e0b', foot: '含已下线历史' },
  { label: '已发布', value: statusCount('on'), unit: '条', icon: 'Bell', color: '#10d9b5', foot: '学生端可见' },
  { label: '已下线', value: statusCount('off'), unit: '条', icon: 'Remove', color: '#a1abbf', foot: '仅管理端留档' },
  { label: '本月发布', value: monthCount.value, unit: '条', icon: 'Calendar', color: '#5b7cfa', foot: '按创建时间统计' }
])

async function load() {
  loading.value = true
  try {
    const data = await pageNotices({ page: 1, size: 100 })
    all.value = data.records
  } finally { loading.value = false }
}

function open(row) {
  Object.assign(form, row
    ? { id: row.id, title: row.title, content: row.content, status: row.status }
    : { id: null, title: '', content: '', status: 1 })
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, ...data } = form
    if (id) await updateNotice(id, data)
    else await createNotice(data)
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally { saving.value = false }
}

async function toggle(row) {
  await updateNotice(row.id, { title: row.title, content: row.content, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(row.status === 1 ? '公告已下线' : '公告已发布')
  load()
}

onMounted(load)
</script>

<style scoped>
.status-seg { margin-bottom: 14px; }
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-item {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 15px 16px;
  background: var(--art-card-bg, #fff);
  transition: transform 0.25s var(--flair-ease), box-shadow 0.25s, border-color 0.25s;
}
html.dark .notice-item { background: #17171c; }
.notice-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); border-color: rgba(245, 158, 11, 0.5); }
.notice-item.off { opacity: 0.66; }
.n-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.n-head b { font-size: 14.5px; color: var(--art-gray-900); }
.n-content {
  margin: 8px 0 10px; font-size: 13px; color: var(--art-gray-600); line-height: 1.7;
  white-space: pre-wrap; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.n-foot { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; font-size: 11.5px; color: var(--art-gray-500); }
.n-actions { margin-left: auto; display: inline-flex; gap: 6px; }
@media (max-width: 560px) {
  .n-actions { margin-left: 0; width: 100%; }
}
</style>