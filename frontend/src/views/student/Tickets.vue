<template>
  <div class="ap">
    <PageHero tone="notice" icon="Service" chip="SERVICE TICKET" live title="联系客服"
      sub="包裹、代取、钱包、自寄问题一键提交工单，客服在线回复跟进；处理进展实时通知，可随时补充说明。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openForm">提交工单</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load(1)">刷新</el-button>
      </template>
    </PageHero>

    <PanelCard title="我的工单" :note="`${total} 单`" icon="ChatLineSquare" accent="notice" :index="1">
      <div class="ap-seg">
        <button v-for="s in segs" :key="s.value" :class="{ on: status === s.value }" @click="status = s.value; load(1)">{{ s.label }}</button>
      </div>

      <div class="ap-table-wrap">
        <el-table :data="sortedRows" v-loading="loading" :default-sort="defaultSort" @sort-change="onSortChange">
          <el-table-column prop="ticketNo" label="工单号" min-width="168" sortable="custom">
            <template #default="{ row }"><span class="ap-mono">{{ row.ticketNo }}</span></template>
          </el-table-column>
          <el-table-column prop="category" label="类别" min-width="104">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ dict(TICKET_CATEGORY, row.category).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }"><span class="ap-strong">{{ row.title }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="92">
            <template #default="{ row }"><el-tag size="small" :type="dict(TICKET_STATUS, row.status).type" effect="light">{{ dict(TICKET_STATUS, row.status).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" min-width="140" sortable="custom">
            <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="最近更新" min-width="140" sortable="custom">
            <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.updatedAt || row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openDetail(row)">查看 / 回复</el-button>
              <el-button v-if="row.status !== 'DONE'" size="small" text type="danger" @click="doClose(row)">关闭</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="暂无工单" :image-size="86" />
        <p>遇到取件、扣费或派送问题，提交工单后客服会尽快跟进处理。</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>

    <!-- 提交工单 -->
    <el-dialog v-model="formVisible" title="提交客服工单" width="540px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>问题类别<em>*</em></label>
          <div class="tk-cat">
            <button v-for="(v, k) in TICKET_CATEGORY" :key="k" :class="{ on: form.category === k }" @click="form.category = k">
              {{ v.label }}
            </button>
          </div>
        </div>
        <div class="ap-field-row">
          <label>标题<em>*</em></label>
          <el-input v-model="form.title" placeholder="一句话概括问题，如：包裹显示已取但未收到" maxlength="64" show-word-limit />
        </div>
        <div class="ap-field-row">
          <label>问题描述<em>*</em></label>
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="1000" show-word-limit
                    placeholder="请描述发生时间、涉及单号与具体现象，便于客服快速定位" />
        </div>
        <div class="ap-field-row">
          <label>联系方式</label>
          <el-input v-model="form.contact" placeholder="手机号 / 微信（选填，便于回访）" maxlength="32" />
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">提交工单</el-button>
      </template>
    </el-dialog>

    <!-- 详情 + 回复流 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="620px" append-to-body top="6vh">
      <template v-if="detail">
        <div class="ap-kv">
          <div><span>工单号</span><b class="ap-mono">{{ detail.ticket.ticketNo }}</b></div>
          <div><span>类别 / 状态</span><b>{{ dict(TICKET_CATEGORY, detail.ticket.category).label }} ·
            <el-tag size="small" :type="dict(TICKET_STATUS, detail.ticket.status).type" effect="light">{{ dict(TICKET_STATUS, detail.ticket.status).label }}</el-tag></b></div>
          <div><span>标题</span><b>{{ detail.ticket.title }}</b></div>
          <div><span>提交时间</span><b class="ap-mono">{{ fmtTime(detail.ticket.createdAt) }}</b></div>
          <div v-if="detail.ticket.handlerName"><span>处理人</span><b>{{ detail.ticket.handlerName }}</b></div>
        </div>
        <div class="tk-thread">
          <div class="tk-msg mine">
            <div class="tk-meta"><b>我</b><span class="ap-mono">{{ fmtTime(detail.ticket.createdAt) }}</span></div>
            <p>{{ detail.ticket.content }}</p>
          </div>
          <div v-for="r in detail.replies" :key="r.id" class="tk-msg" :class="r.authorRole === 'USER' ? 'mine' : 'staff'">
            <div class="tk-meta">
              <b>{{ r.authorRole === 'USER' ? (r.authorName || '我') : `客服 · ${r.authorName || '驿站'}` }}</b>
              <span class="ap-mono">{{ fmtTime(r.createdAt) }}</span>
            </div>
            <p>{{ r.content }}</p>
          </div>
          <div v-if="!detail.replies.length" class="tk-waiting">客服尚未回复，处理进展会通过提醒中心通知你。</div>
        </div>
        <div v-if="detail.ticket.status !== 'DONE'" class="tk-reply">
          <el-input v-model="replyContent" type="textarea" :rows="2" maxlength="500" placeholder="补充说明或追问…" />
          <el-button type="primary" :icon="Promotion" :loading="replying" :disabled="!replyContent.trim()" @click="doReply">发送回复</el-button>
        </div>
        <div v-else class="ap-note" style="--nc:#10d9b5">
          <el-icon><CircleCheckFilled /></el-icon>
          <div>工单已处理完成。如问题仍未解决，可重新提交新工单并注明原工单号。</div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Promotion, CircleCheckFilled } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { createTicket, myTickets, ticketDetail, closeTicket, replyTicket } from '../../api'
import { TICKET_STATUS, TICKET_CATEGORY, dict, fmtTime } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const status = ref('')
const query = reactive({ page: 1, size: 10 })
const segs = [
  { value: '', label: '全部' }, { value: 'PENDING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' }, { value: 'DONE', label: '已完成' }
]

const { defaultSort, onSortChange, sorted } = useTableSort('createdAt', { updatedAt: r => r.updatedAt || r.createdAt })
const sortedRows = computed(() => sorted(rows.value))

const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ category: 'PARCEL', title: '', content: '', contact: '' })
const detailVisible = ref(false)
const detail = ref(null)
const replyContent = ref('')
const replying = ref(false)

const heroStats = computed(() => [
  { label: '工单总数', value: total.value, unit: '单' },
  { label: '本页跟进中', value: rows.value.filter(r => r.status !== 'DONE').length, unit: '单' },
  { label: '本页已完成', value: rows.value.filter(r => r.status === 'DONE').length, unit: '单' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myTickets({ ...query, status: status.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function openForm() {
  Object.assign(form, { category: 'PARCEL', title: '', content: '', contact: '' })
  formVisible.value = true
}

async function doCreate() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题与问题描述')
    return
  }
  saving.value = true
  try {
    const t = await createTicket({ ...form, title: form.title.trim(), content: form.content.trim() })
    ElMessage.success(`工单 ${t.ticketNo} 已提交，客服将尽快处理`)
    formVisible.value = false
    status.value = ''
    await load(1)
  } finally { saving.value = false }
}

async function openDetail(row) {
  detail.value = await ticketDetail(row.id)
  replyContent.value = ''
  detailVisible.value = true
}

async function doReply() {
  replying.value = true
  try {
    await replyTicket(detail.value.ticket.id, replyContent.value.trim())
    replyContent.value = ''
    detail.value = await ticketDetail(detail.value.ticket.id)
    ElMessage.success('回复已发送')
    await load()
  } finally { replying.value = false }
}

async function doClose(row) {
  try {
    await ElMessageBox.confirm(`确定关闭工单 ${row.ticketNo}？关闭后不可再回复。`, '关闭工单', { type: 'warning' })
  } catch { return }
  await closeTicket(row.id)
  ElMessage.success('工单已关闭')
  await load()
}

onMounted(() => load())
</script>

<style scoped>
.tk-cat { display: flex; gap: 8px; flex-wrap: wrap; }
.tk-cat button {
  border: 1px solid var(--default-border); border-radius: 10px; padding: 7px 13px;
  font-size: 12.5px; color: var(--art-gray-700); background: transparent; cursor: pointer; transition: all 0.2s;
}
.tk-cat button.on { border-color: var(--el-color-primary); color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 600; }
.tk-thread {
  margin-top: 14px; padding: 12px; border: 1px solid var(--default-border); border-radius: 12px;
  max-height: 40vh; overflow: auto; display: flex; flex-direction: column; gap: 10px;
}
.tk-msg { border-radius: 10px; padding: 9px 12px; max-width: 92%; }
.tk-msg.mine { align-self: flex-end; background: var(--el-color-primary-light-9); }
html.dark .tk-msg.mine { background: rgba(91, 124, 250, 0.12); }
.tk-msg.staff { align-self: flex-start; background: rgba(16, 217, 181, 0.08); border: 1px solid rgba(16, 217, 181, 0.25); }
.tk-meta { display: flex; align-items: baseline; gap: 9px; }
.tk-meta b { font-size: 12px; color: var(--art-gray-900); }
.tk-meta span { font-size: 10.5px; color: var(--art-gray-500); }
.tk-msg p { margin: 5px 0 0; font-size: 12.5px; line-height: 1.7; color: var(--art-gray-800); white-space: pre-wrap; word-break: break-word; }
.tk-waiting { font-size: 12px; color: var(--art-gray-500); text-align: center; padding: 8px 0; }
.tk-reply { display: flex; gap: 10px; align-items: flex-end; margin-top: 12px; }
.tk-reply .el-input { flex: 1; }
</style>