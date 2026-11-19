<template>
  <div class="ap">
    <PageHero tone="notice" icon="Service" chip="SERVICE DESK" live title="客服工单台"
      sub="学生提交的咨询与投诉统一在此受理：先「开始处理」认领工单，回复沟通后「标记完成」闭环。未读工单优先处理。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="工单队列" :note="`${total} 条工单`" icon="Tickets" accent="notice" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="query.status" clearable placeholder="全部状态" @change="load(1)">
            <el-option v-for="(d, key) in TICKET_STATUS" :key="key" :label="d.label" :value="key" />
          </el-select>
        </div>
        <div class="ap-field">
          <label>未读</label>
          <el-select v-model="query.unread" clearable placeholder="全部" @change="load(1)">
            <el-option label="仅未读" :value="true" />
            <el-option label="仅已读" :value="false" />
          </el-select>
        </div>
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" :prefix-icon="Search" clearable placeholder="工单号 / 标题 / 提交人"
                    @keyup.enter="load(1)" @clear="load(1)" />
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="sorted(rows)" stripe :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
            <el-table-column label="工单号" prop="ticketNo" sortable="custom" width="168">
              <template #default="{ row }">
                <span class="ap-mono">{{ row.ticketNo }}</span>
                <el-badge v-if="row.isRead === 0" is-dot class="unread-dot" />
              </template>
            </el-table-column>
            <el-table-column label="分类" prop="category" sortable="custom" width="110">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ catLabel(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" sortable="custom" min-width="180" show-overflow-tooltip />
            <el-table-column prop="userName" label="提交人" sortable="custom" width="100" />
            <el-table-column label="状态" prop="status" sortable="custom" width="96">
              <template #default="{ row }">
                <el-tag :type="dict(TICKET_STATUS, row.status).type" size="small" effect="light">
                  {{ dict(TICKET_STATUS, row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="176" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click.stop="openDetail(row)">处理</el-button>
                <el-button v-if="row.status === 'PENDING'" size="small" text type="warning" @click.stop="startProcessing(row)">受理</el-button>
                <el-button v-if="row.status === 'PROCESSING'" size="small" text type="success" @click.stop="markDone(row)">完成</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sorted(rows)" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <el-tag size="small" effect="plain">{{ catLabel(row.category) }}</el-tag>
              <b>{{ row.title }}</b>
              <el-tag :type="dict(TICKET_STATUS, row.status).type" size="small" effect="light">
                {{ dict(TICKET_STATUS, row.status).label }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>工单号</label><span class="ap-mono">{{ row.ticketNo }}</span></div>
              <div><label>提交人</label><span>{{ row.userName }}</span></div>
              <div><label>时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
              <div><label>未读</label><span>{{ row.isRead === 0 ? '未读' : '已读' }}</span></div>
            </div>
          </article>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="没有匹配的工单" :image-size="86" />
          <p>调整状态或关键词筛选后重试。</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <el-drawer v-model="detailVisible" title="工单处理" size="500px" append-to-body @closed="current = null">
      <div v-if="current" v-loading="detailLoading" class="detail">
        <div class="d-head">
          <el-tag effect="plain">{{ catLabel(current.category) }}</el-tag>
          <b>{{ current.title }}</b>
          <el-tag :type="dict(TICKET_STATUS, current.status).type" effect="light" size="small">
            {{ dict(TICKET_STATUS, current.status).label }}
          </el-tag>
        </div>
        <div class="ap-kv">
          <div><label>工单号</label><span class="ap-mono">{{ current.ticketNo }}</span></div>
          <div><label>提交人</label><span>{{ current.userName }}（{{ current.userPhone || '—' }}）</span></div>
          <div><label>联系方式</label><span>{{ current.contact || '—' }}</span></div>
          <div><label>提交时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>处理人</label><span>{{ current.handlerName || '未认领' }}</span></div>
          <div><label>完成时间</label><span class="ap-mono">{{ fmtTime(current.handledAt) }}</span></div>
        </div>
        <div class="ap-note" style="--nc:#5b7cfa">
          <el-icon><ChatDotSquare /></el-icon>
          <span>{{ current.content }}</span>
        </div>

        <h4 class="d-h">沟通记录（{{ replies.length }}）</h4>
        <div v-if="replies.length" class="reply-flow">
          <div v-for="r in replies" :key="r.id" class="reply-item" :class="{ staff: r.authorRole !== 'USER' }">
            <div class="reply-meta">
              <b>{{ r.authorName }}</b>
              <el-tag size="small" :type="r.authorRole === 'USER' ? 'info' : 'primary'" effect="plain">
                {{ r.authorRole === 'USER' ? '学生' : '客服' }}
              </el-tag>
              <span class="ap-mono">{{ fmtTime(r.createdAt) }}</span>
            </div>
            <p>{{ r.content }}</p>
          </div>
        </div>
        <div v-else class="ap-empty"><p>暂无沟通记录，回复后学生将收到站内提醒。</p></div>

        <template v-if="current.status !== 'DONE'">
          <el-input v-model="replyText" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="回复学生，说明处理进展或解决方案…" />
          <div class="d-actions">
            <el-button type="primary" :icon="Promotion" :loading="replying" @click="sendReply">发送回复</el-button>
            <el-button v-if="current.status === 'PENDING'" type="warning" :icon="VideoPlay" @click="startProcessing(current)">开始处理</el-button>
            <el-button v-if="current.status === 'PROCESSING'" type="success" :icon="CircleCheck" @click="markDone(current)">标记完成</el-button>
          </div>
        </template>
        <div v-else class="ap-note" style="--nc:#0d9488">
          <el-icon><CircleCheck /></el-icon>
          <span>工单已闭环。学生仍可查看完整沟通记录。</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotSquare, CircleCheck, Promotion, Refresh, RefreshLeft, Search, VideoPlay } from '@element-plus/icons-vue'
import { handleTickets, ticketStats, ticketDetail, replyTicket, ticketProcessing, ticketDone } from '../../api'
import { fmtTime, dict, TICKET_STATUS, TICKET_CATEGORY } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const stats = ref({ pending: 0, processing: 0, done: 0, unread: 0 })
const query = reactive({ page: 1, size: 20, status: '', unread: null, keyword: '' })
const { defaultSort, onSortChange, sorted } = useTableSort('createdAt')

const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const replies = ref([])
const replyText = ref('')
const replying = ref(false)

const catLabel = c => (TICKET_CATEGORY[c] || {}).label || c || '—'

const heroStats = computed(() => [
  { label: '待处理', value: stats.value.pending, unit: '单' },
  { label: '处理中', value: stats.value.processing, unit: '单' },
  { label: '未读', value: stats.value.unread, unit: '单' }
])
const metrics = computed(() => [
  { label: '待处理', value: stats.value.pending, unit: '单', icon: 'AlarmClock', color: '#f59e0b', foot: '等待客服认领' },
  { label: '处理中', value: stats.value.processing, unit: '单', icon: 'Loading', color: '#5b7cfa', foot: '已认领跟进中' },
  { label: '已处理', value: stats.value.done, unit: '单', icon: 'CircleCheck', color: '#10d9b5', foot: '已闭环归档' },
  { label: '未读工单', value: stats.value.unread, unit: '单', icon: 'Bell', color: '#fb7185', foot: '打开详情即标记已读' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await handleTickets({
      page: query.page, size: query.size,
      status: query.status || undefined,
      unread: query.unread === null || query.unread === '' ? undefined : query.unread,
      keyword: query.keyword || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function loadStats() {
  stats.value = await ticketStats()
}

function refresh() { load(); loadStats() }

function resetQuery() {
  query.status = ''
  query.unread = null
  query.keyword = ''
  load(1)
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  replyText.value = ''
  try {
    const data = await ticketDetail(row.id)
    current.value = data.ticket
    replies.value = data.replies || []
    if (row.isRead === 0) { row.isRead = 1; loadStats() }
  } finally { detailLoading.value = false }
}

async function reloadCurrent() {
  if (!current.value) return
  const data = await ticketDetail(current.value.id)
  current.value = data.ticket
  replies.value = data.replies || []
}

async function sendReply() {
  if (!replyText.value.trim()) return ElMessage.warning('请输入回复内容')
  replying.value = true
  try {
    await replyTicket(current.value.id, replyText.value.trim())
    ElMessage.success('回复已发送，学生将收到站内提醒')
    replyText.value = ''
    await reloadCurrent()
    refresh()
  } finally { replying.value = false }
}

async function startProcessing(row) {
  await ticketProcessing(row.id)
  ElMessage.success('已认领工单，状态更新为处理中')
  if (current.value && current.value.id === row.id) await reloadCurrent()
  refresh()
}

async function markDone(row) {
  const { value } = await ElMessageBox.prompt('填写处理结论（将随完成状态归档）', '标记完成', {
    inputPlaceholder: '例如：已联系驿站核实并补发取件码',
    inputType: 'textarea',
    confirmButtonText: '完成工单',
    inputValidator: v => (v && v.trim() ? true : '请填写处理结论')
  })
  await ticketDone(row.id, value.trim())
  ElMessage.success('工单已完成闭环')
  if (current.value && current.value.id === row.id) await reloadCurrent()
  refresh()
}

onMounted(refresh)
</script>

<style scoped>
.unread-dot { margin-left: 6px; vertical-align: 2px; }
.detail { display: flex; flex-direction: column; gap: 12px; }
.d-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.d-head b { font-size: 15px; color: var(--art-gray-900); }
.d-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 4px; }
.reply-flow { display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow: auto; }
.reply-item {
  padding: 10px 12px; border-radius: 10px; border: 1px solid var(--default-border);
  background: var(--art-gray-100);
}
.reply-item.staff { background: rgba(91, 124, 250, 0.07); border-color: rgba(91, 124, 250, 0.25); }
html.dark .reply-item { background: #1c1c22; }
html.dark .reply-item.staff { background: rgba(91, 124, 250, 0.12); }
.reply-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--art-gray-600); }
.reply-meta b { color: var(--art-gray-900); }
.reply-item p { margin: 6px 0 0; font-size: 13px; line-height: 1.7; color: var(--art-gray-700); white-space: pre-wrap; }
.d-actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>