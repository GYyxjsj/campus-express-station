<template>
  <div class="ap">
    <PageHero tone="identity" icon="Notebook" chip="PHONE BOOK" live title="电话簿"
      sub="记录家人、室友与常用联系人，代取授权与自寄下单时可快速带入手机号；星标联系人置顶展示。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openForm">添加联系人</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHero>

    <PanelCard title="联系人" :note="`${rows.length} 位`" icon="UserFilled" accent="identity" :index="1">
      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <el-input v-model="keyword" placeholder="搜索姓名 / 手机号 / 关系" :prefix-icon="Search" clearable />
        </div>
        <div class="ap-seg">
          <button :class="{ on: onlyStarred }" @click="onlyStarred = !onlyStarred">仅看星标</button>
        </div>
      </div>

      <div v-loading="loading" class="ct-grid">
        <article v-for="(c, i) in filtered" :key="c.id" class="ct-item flair-in" :style="{ '--i': Math.min(i, 10) }">
          <div class="ct-avatar" :class="{ star: c.isStarred === 1 }">{{ (c.name || '?').slice(0, 1) }}</div>
          <div class="ct-main">
            <div class="ct-l1">
              <b>{{ c.name }}</b>
              <el-tag v-if="c.relation" size="small" effect="plain">{{ c.relation }}</el-tag>
              <el-icon v-if="c.isStarred === 1" class="ct-star" :size="14"><StarFilled /></el-icon>
            </div>
            <div class="ct-phone ap-mono">{{ c.phone }}</div>
            <p v-if="c.remark" class="ct-remark">{{ c.remark }}</p>
            <div class="ct-time ap-mono">添加于 {{ fmtTime(c.createdAt) }}</div>
          </div>
          <div class="ct-ops">
            <el-tooltip content="复制手机号" placement="top">
              <el-button size="small" circle :icon="CopyDocument" @click="copyPhone(c)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button size="small" circle type="danger" plain :icon="Delete" @click="doDelete(c)" />
            </el-tooltip>
          </div>
        </article>
      </div>

      <div v-if="!filtered.length && !loading" class="ap-empty">
        <el-empty :description="rows.length ? '没有匹配的联系人' : '电话簿还是空的'" :image-size="86" />
        <p v-if="!rows.length">添加常用联系人后，代取授权、自寄下单可一键带入，避免手输出错。</p>
      </div>
    </PanelCard>

    <el-dialog v-model="formVisible" title="添加联系人" width="460px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>姓名<em>*</em></label>
          <el-input v-model="form.name" placeholder="联系人姓名" maxlength="32" />
        </div>
        <div class="ap-field-row">
          <label>手机号<em>*</em></label>
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" class="ap-mono" />
        </div>
        <div class="ap-field-row">
          <label>关系</label>
          <div class="rel-seg">
            <button v-for="r in relations" :key="r" :class="{ on: form.relation === r }" @click="form.relation = form.relation === r ? '' : r">{{ r }}</button>
          </div>
        </div>
        <div class="ap-field-row">
          <label>备注</label>
          <el-input v-model="form.remark" placeholder="如：同宿舍，可代取大件" maxlength="100" />
        </div>
        <div class="ap-field-row">
          <el-checkbox v-model="form.isStarred">设为星标联系人（置顶展示）</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存联系人</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Delete, CopyDocument, StarFilled } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { myContacts, saveContact, deleteContact } from '../../api'
import { fmtTime } from '../../utils/format'

const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const onlyStarred = ref(false)
const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ name: '', phone: '', relation: '', remark: '', isStarred: false })
const relations = ['家人', '室友', '同学', '朋友', '老师', '其他']

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rows.value
    .filter(c => !onlyStarred.value || c.isStarred === 1)
    .filter(c => !kw || [c.name, c.phone, c.relation, c.remark].some(v => (v || '').toLowerCase().includes(kw)))
    .slice()
    .sort((a, b) => (b.isStarred || 0) - (a.isStarred || 0) || String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
})

const heroStats = computed(() => [
  { label: '联系人', value: rows.value.length, unit: '位' },
  { label: '星标', value: rows.value.filter(c => c.isStarred === 1).length, unit: '位' },
  { label: '家人室友', value: rows.value.filter(c => ['家人', '室友'].includes(c.relation)).length, unit: '位' }
])

async function load() {
  loading.value = true
  try { rows.value = await myContacts() } finally { loading.value = false }
}

function openForm() {
  Object.assign(form, { name: '', phone: '', relation: '', remark: '', isStarred: false })
  formVisible.value = true
}

async function doSave() {
  if (!form.name.trim() || !/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    ElMessage.warning('请填写姓名与有效的 11 位手机号')
    return
  }
  saving.value = true
  try {
    await saveContact({
      name: form.name.trim(), phone: form.phone.trim(),
      relation: form.relation, remark: form.remark.trim(), isStarred: form.isStarred ? 1 : 0
    })
    ElMessage.success('联系人已添加')
    formVisible.value = false
    await load()
  } finally { saving.value = false }
}

async function copyPhone(c) {
  try {
    await navigator.clipboard.writeText(c.phone)
    ElMessage.success(`已复制 ${c.name} 的手机号`)
  } catch {
    ElMessage.info(`手机号：${c.phone}`)
  }
}

async function doDelete(c) {
  try {
    await ElMessageBox.confirm(`确定删除联系人「${c.name}」吗？`, '删除联系人', { type: 'warning' })
  } catch { return }
  await deleteContact(c.id)
  ElMessage.success('联系人已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.ct-grid { display: grid; gap: 10px; grid-template-columns: repeat(auto-fill, minmax(min(100%, 330px), 1fr)); }
.ct-item {
  display: flex; gap: 12px; align-items: flex-start;
  border: 1px solid var(--default-border); border-radius: 14px; padding: 13px 14px;
  background: var(--art-card-bg, #fff);
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s;
}
html.dark .ct-item { background: #17171c; }
.ct-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.ct-avatar {
  flex: none; width: 40px; height: 40px; border-radius: 12px;
  display: grid; place-items: center; font-size: 16px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #5b7cfa, #8b5cf6);
}
.ct-avatar.star { background: linear-gradient(135deg, #f59e0b, #f97316); }
.ct-main { flex: 1; min-width: 0; }
.ct-l1 { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.ct-l1 b { font-size: 14px; color: var(--art-gray-900); }
.ct-star { color: #f59e0b; }
.ct-phone { margin-top: 4px; font-size: 13px; color: var(--art-gray-800); letter-spacing: 0.5px; }
.ct-remark { margin: 4px 0 0; font-size: 11.5px; color: var(--art-gray-600); line-height: 1.6; }
.ct-time { margin-top: 6px; font-size: 10.5px; color: var(--art-gray-500); }
.ct-ops { display: flex; flex-direction: column; gap: 6px; }
.rel-seg { display: flex; gap: 7px; flex-wrap: wrap; }
.rel-seg button {
  border: 1px solid var(--default-border); border-radius: 999px; padding: 5px 13px;
  font-size: 12px; color: var(--art-gray-700); background: transparent; cursor: pointer; transition: all 0.2s;
}
.rel-seg button.on { border-color: var(--el-color-primary); color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 600; }
</style>