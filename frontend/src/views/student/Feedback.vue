<template>
  <div class="ap">
    <PageHero tone="notice" icon="ChatDotSquare" chip="FEEDBACK" live title="意见反馈"
      sub="功能建议、问题上报、服务投诉与表扬都欢迎；管理员回复后可在此查看，你的每条反馈都会被认真对待。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="EditPen" @click="openForm">写反馈</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load(1)">刷新</el-button>
      </template>
    </PageHero>

    <PanelCard title="我的反馈" :note="`${total} 条`" icon="Comment" accent="notice" :index="1">
      <div v-loading="loading" class="fb-list">
        <article v-for="(f, i) in rows" :key="f.id" class="fb-item flair-in" :style="{ '--i': Math.min(i, 10) }">
          <div class="fb-l1">
            <el-tag size="small" :type="dict(FEEDBACK_TYPE, f.type).type" effect="light">{{ dict(FEEDBACK_TYPE, f.type).label }}</el-tag>
            <el-tag size="small" :type="dict(FEEDBACK_STATUS, f.status).type" effect="plain">{{ dict(FEEDBACK_STATUS, f.status).label }}</el-tag>
            <span class="fb-time ap-mono">{{ fmtTime(f.createdAt) }}</span>
          </div>
          <p class="fb-content">{{ f.content }}</p>
          <div v-if="f.reply" class="fb-reply">
            <div class="fb-reply-head">
              <el-icon :size="13"><Service /></el-icon>
              <b>官方回复{{ f.replierName ? ' · ' + f.replierName : '' }}</b>
              <span class="ap-mono">{{ fmtTime(f.repliedAt) }}</span>
            </div>
            <p>{{ f.reply }}</p>
          </div>
        </article>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="还没有反馈记录" :image-size="86" />
        <p>对驿站服务或系统功能有想法？点击「写反馈」告诉我们。</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>

    <el-dialog v-model="formVisible" title="写反馈" width="520px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>反馈类型<em>*</em></label>
          <div class="fb-type">
            <button v-for="(v, k) in FEEDBACK_TYPE" :key="k" :class="{ on: form.type === k }" @click="form.type = k">{{ v.label }}</button>
          </div>
        </div>
        <div class="ap-field-row">
          <label>反馈内容<em>*</em></label>
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit
                    placeholder="请具体描述你的建议或遇到的问题，如涉及包裹请附单号" />
        </div>
        <div class="ap-field-row">
          <label>联系方式</label>
          <el-input v-model="form.contact" placeholder="手机号 / 邮箱（选填，便于回访）" maxlength="32" />
          <div class="ap-form-tip">紧急取件问题建议使用「联系客服」提交工单，处理更快。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :icon="Promotion" :loading="saving" @click="doSubmit">提交反馈</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Refresh, Promotion, Service } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { submitFeedback, myFeedback } from '../../api'
import { FEEDBACK_TYPE, FEEDBACK_STATUS, dict, fmtTime } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ type: 'SUGGEST', content: '', contact: '' })

const heroStats = computed(() => [
  { label: '反馈总数', value: total.value, unit: '条' },
  { label: '本页已回复', value: rows.value.filter(f => f.status === 'REPLIED').length, unit: '条' },
  { label: '本页待回复', value: rows.value.filter(f => f.status === 'PENDING').length, unit: '条' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myFeedback({ ...query })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function openForm() {
  Object.assign(form, { type: 'SUGGEST', content: '', contact: '' })
  formVisible.value = true
}

async function doSubmit() {
  if (form.content.trim().length < 5) {
    ElMessage.warning('请至少填写 5 个字的反馈内容')
    return
  }
  saving.value = true
  try {
    await submitFeedback({ ...form, content: form.content.trim() })
    ElMessage.success('反馈已提交，感谢你的声音')
    formVisible.value = false
    await load(1)
  } finally { saving.value = false }
}

onMounted(() => load())
</script>

<style scoped>
.fb-list { display: flex; flex-direction: column; gap: 10px; }
.fb-item {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 13px 15px;
  background: var(--art-card-bg, #fff);
}
html.dark .fb-item { background: #17171c; }
.fb-l1 { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.fb-time { margin-left: auto; font-size: 11px; color: var(--art-gray-500); }
.fb-content { margin: 9px 0 0; font-size: 13px; line-height: 1.75; color: var(--art-gray-800); white-space: pre-wrap; word-break: break-word; }
.fb-reply {
  margin-top: 10px; border-left: 3px solid #10d9b5; border-radius: 0 10px 10px 0;
  background: rgba(16, 217, 181, 0.06); padding: 9px 12px;
}
.fb-reply-head { display: flex; align-items: center; gap: 7px; color: #0d9488; }
.fb-reply-head b { font-size: 12px; }
.fb-reply-head span { font-size: 10.5px; color: var(--art-gray-500); margin-left: auto; }
.fb-reply p { margin: 6px 0 0; font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); white-space: pre-wrap; }
.fb-type { display: flex; gap: 8px; flex-wrap: wrap; }
.fb-type button {
  border: 1px solid var(--default-border); border-radius: 10px; padding: 7px 13px;
  font-size: 12.5px; color: var(--art-gray-700); background: transparent; cursor: pointer; transition: all 0.2s;
}
.fb-type button.on { border-color: var(--el-color-primary); color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 600; }
</style>