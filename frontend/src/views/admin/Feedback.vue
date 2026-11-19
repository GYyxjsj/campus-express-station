<template>
  <div class="ap">
    <PageHero tone="notice" icon="ChatLineSquare" chip="FEEDBACK DESK" live title="意见反馈"
      sub="集中处理学生提交的功能建议、问题报错、服务投诉与表扬点赞；回复后学生端「意见反馈」页即时可见，并写入消息通知。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="反馈列表" :note="`${total} 条反馈`" icon="ChatLineSquare" accent="notice" :index="4">
      <template #extra>
        <div class="ap-seg">
          <button v-for="seg in segments" :key="seg.value" :class="{ on: query.status === seg.value }"
                  @click="query.status = seg.value; load(1)">{{ seg.label }}</button>
        </div>
      </template>
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>反馈类型</label>
          <el-select v-model="query.type" clearable placeholder="全部类型" @change="load(1)">
            <el-option v-for="(d, key) in FEEDBACK_TYPE" :key="key" :label="d.label" :value="key" />
          </el-select>
        </div>
      </div>
      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="sorted(list)" stripe :default-sort="sort.defaultSort" @sort-change="sort.onSortChange">
            <el-table-column label="类型" prop="type" sortable="custom" width="106">
              <template #default="{ row }">
                <el-tag :type="dict(FEEDBACK_TYPE, row.type).type" size="small" effect="light">
                  {{ dict(FEEDBACK_TYPE, row.type).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="提交人" sortable="custom" width="100" show-overflow-tooltip />
            <el-table-column prop="content" label="反馈内容" min-width="240" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" sortable="custom" width="96">
              <template #default="{ row }">
                <el-tag :type="dict(FEEDBACK_STATUS, row.status).type" size="small" effect="light">
                  {{ dict(FEEDBACK_STATUS, row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="replierName" label="回复人" sortable="custom" width="100">
              <template #default="{ row }">{{ row.replierName || '—' }}</template>
            </el-table-column>
            <el-table-column label="提交时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'PENDING'" size="small" text type="warning" @click="openReply(row)">回复</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sorted(list)" :key="row.id" class="ap-card-item">
            <div class="ap-card-top">
              <b>{{ row.userName }}</b>
              <el-tag :type="dict(FEEDBACK_STATUS, row.status).type" size="small" effect="light">
                {{ dict(FEEDBACK_STATUS, row.status).label }}
              </el-tag>
            </div>
            <p class="card-content">{{ row.content }}</p>
            <div class="ap-card-grid">
              <div><label>类型</label><span>{{ dict(FEEDBACK_TYPE, row.type).label }}</span></div>
              <div><label>提交时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
            </div>
            <div class="card-actions">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" type="primary" @click="openReply(row)">回复</el-button>
            </div>
          </article>
        </div>

        <div v-if="!list.length && !loading" class="ap-empty">
          <el-empty description="暂无符合条件的反馈" :image-size="86" />
          <p>切换上方筛选，或等待学生提交新反馈。</p>
        </div>
      </div>
      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <el-dialog v-model="detailVisible" title="反馈详情" width="560px" append-to-body>
      <div v-if="current" class="ap-kv">
        <div><label>提交人</label><span>{{ current.userName }}</span></div>
        <div><label>类型</label><span>{{ dict(FEEDBACK_TYPE, current.type).label }}</span></div>
        <div><label>状态</label><span>{{ dict(FEEDBACK_STATUS, current.status).label }}</span></div>
        <div><label>联系方式</label><span>{{ current.contact || '未留' }}</span></div>
        <div><label>提交时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
        <div><label>回复时间</label><span class="ap-mono">{{ fmtTime(current.repliedAt) }}</span></div>
      </div>
      <div v-if="current" class="detail-block">
        <label>反馈内容</label>
        <pre>{{ current.content }}</pre>
      </div>
      <div v-if="current && current.reply" class="detail-block">
        <label>官方回复（{{ current.replierName || '管理员' }}）</label>
        <pre class="reply-pre">{{ current.reply }}</pre>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current && current.status === 'PENDING'" type="primary" @click="openReply(current)">回复反馈</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复反馈" width="520px" append-to-body :close-on-click-modal="false">
      <div v-if="current" class="ap-note" style="--nc:#5b7cfa">
        <el-icon><ChatLineSquare /></el-icon>
        <span>{{ current.userName }}：{{ current.content.slice(0, 80) }}{{ current.content.length > 80 ? '…' : '' }}</span>
      </div>
      <el-form class="ap-form" label-position="top" @submit.prevent>
        <div class="ap-field-row">
          <label>回复内容<em>*</em></label>
          <el-input v-model="replyText" type="textarea" :rows="4" maxlength="500" show-word-limit
                    placeholder="回复将展示在学生端反馈详情，并推送消息通知" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="doReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Refresh } from '@element-plus/icons-vue'
import { adminFeedback, adminReplyFeedback } from '../../api'
import { fmtTime, dict, FEEDBACK_TYPE, FEEDBACK_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', type: '' })
const sort = useTableSort('createdAt')
const sorted = sort.sorted

const segments = [
  { label: '全部', value: '' },
  { label: '待回复', value: 'PENDING' },
  { label: '已回复', value: 'REPLIED' }
]

const detailVisible = ref(false)
const replyVisible = ref(false)
const replying = ref(false)
const current = ref(null)
const replyText = ref('')

const heroStats = computed(() => [
  { label: '反馈总数', value: total.value, unit: '条' },
  { label: '本页待回复', value: list.value.filter(f => f.status === 'PENDING').length, unit: '条' },
  { label: '本页已回复', value: list.value.filter(f => f.status === 'REPLIED').length, unit: '条' }
])
const metrics = computed(() => [
  { label: '反馈总数', value: total.value, unit: '条', icon: 'ChatLineSquare', color: '#5b7cfa', foot: '当前筛选条件下' },
  { label: '本页待回复', value: list.value.filter(f => f.status === 'PENDING').length, unit: '条', icon: 'Bell', color: '#f59e0b', foot: '优先处理投诉与报错' },
  { label: '本页问题报错', value: list.value.filter(f => f.type === 'BUG').length, unit: '条', icon: 'Warning', color: '#fb7185', foot: 'BUG 类反馈' },
  { label: '本页表扬点赞', value: list.value.filter(f => f.type === 'PRAISE').length, unit: '条', icon: 'Sunny', color: '#10d9b5', foot: '服务口碑' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await adminFeedback({
      page: query.page, size: query.size,
      status: query.status || undefined,
      type: query.type || undefined
    })
    list.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function openDetail(row) { current.value = row; detailVisible.value = true }

function openReply(row) {
  current.value = row
  replyText.value = ''
  detailVisible.value = false
  replyVisible.value = true
}

async function doReply() {
  const text = replyText.value.trim()
  if (!text) return ElMessage.warning('请填写回复内容')
  replying.value = true
  try {
    await adminReplyFeedback(current.value.id, text)
    ElMessage.success('已回复，学生端即时可见')
    replyVisible.value = false
    load()
  } finally { replying.value = false }
}

onMounted(load)
</script>

<style scoped>
.card-content {
  margin: 4px 0 10px; font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700);
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.card-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.detail-block { margin-top: 14px; }
.detail-block label { display: block; font-size: 12px; font-weight: 600; color: var(--art-gray-600); margin-bottom: 6px; }
.detail-block pre {
  margin: 0; padding: 12px; border-radius: 10px; border: 1px solid var(--default-border);
  background: var(--art-gray-100); font-size: 12.5px; line-height: 1.8; white-space: pre-wrap;
  word-break: break-word; font-family: inherit; color: var(--art-gray-800);
}
html.dark .detail-block pre { background: #1c1c22; }
.reply-pre { border-color: rgba(91, 124, 250, 0.35); }
</style>