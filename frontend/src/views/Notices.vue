<template>
  <div class="ap">
    <PageHero tone="notice" icon="Bell" chip="CAMPUS NOTICE" live title="驿站公告"
      sub="营业时间调整、高峰期安排与取件须知都在这里。重要公告也会在你登录时弹窗提醒，确保不错过。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Message" @click="$router.push('/notifications')">提醒中心</el-button>
        <el-button type="default" :icon="Document" @click="$router.push('/policies')">政策协议</el-button>
      </template>
    </PageHero>

    <PanelCard title="公告栏" :note="`${total} 条`" icon="ChatDotSquare" accent="notice" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>搜索公告</label>
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="如 营业时间、假期安排" />
        </div>
      </div>

      <div v-loading="loading" class="notice-list">
        <article v-for="(n, i) in filtered" :key="n.id" class="notice-card flair-in" :style="{ '--i': Math.min(i, 8) }"
                 :class="{ latest: i === 0 && !keyword }">
          <div class="n-head">
            <span class="n-badge"><el-icon :size="14"><Bell /></el-icon></span>
            <b>{{ n.title }}</b>
            <el-tag v-if="i === 0 && !keyword" size="small" type="warning" effect="dark">最新</el-tag>
            <span class="n-time ap-mono">{{ fmtTime(n.createdAt) }}</span>
          </div>
          <p class="n-content">{{ n.content }}</p>
        </article>
      </div>

      <div v-if="!filtered.length && !loading" class="ap-empty">
        <el-empty :description="total ? '没有匹配的公告' : '暂无公告'" :image-size="86" />
        <p>{{ total ? '换个关键词试试。' : '驿站有新安排时会第一时间发布在这里。' }}</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bell, Document, Message, Refresh, Search } from '@element-plus/icons-vue'
import { pageNotices } from '../api'
import { fmtTime } from '../utils/format'
import PageHero from '../components/PageHero.vue'
import PanelCard from '../components/PanelCard.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const keyword = ref('')
const query = reactive({ page: 1, size: 10 })

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter(n => [n.title, n.content].some(v => (v || '').toLowerCase().includes(kw)))
})
const heroStats = computed(() => [
  { label: '公告总数', value: total.value, unit: '条' },
  { label: '本页展示', value: filtered.value.length, unit: '条' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await pageNotices(query)
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

onMounted(() => load())
</script>

<style scoped>
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-card {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 16px 18px;
  background: var(--art-card-bg, #fff); position: relative; overflow: hidden;
  transition: transform 0.25s var(--flair-ease), box-shadow 0.25s, border-color 0.25s;
}
html.dark .notice-card { background: #17171c; }
.notice-card:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); border-color: rgba(245, 158, 11, 0.5); }
.notice-card.latest { border-color: rgba(245, 158, 11, 0.55); background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), transparent 50%); }
html.dark .notice-card.latest { background: linear-gradient(135deg, rgba(245, 158, 11, 0.14), #17171c 55%); }
.n-head { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.n-badge {
  width: 28px; height: 28px; border-radius: 9px; flex: none;
  display: inline-flex; align-items: center; justify-content: center; color: #fff;
  background: linear-gradient(140deg, #f59e0b, #fbbf24);
}
.n-head b { font-size: 14.5px; color: var(--art-gray-900); }
.n-time { margin-left: auto; font-size: 11.5px; color: var(--art-gray-500); }
.n-content { margin: 10px 0 0; font-size: 13px; color: var(--art-gray-600); line-height: 1.75; white-space: pre-wrap; }
@media (max-width: 560px) { .n-time { margin-left: 0; width: 100%; } }
</style>