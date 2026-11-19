<template>
  <div class="ap">
    <PageHero tone="policy" icon="Setting" chip="POLICY ENGINE" live title="策略配置"
      sub="提醒与授权的运行参数：保存后定时任务下一轮扫描即生效，无需重启。阈值联动校验，防止催领与滞留策略互相矛盾。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存策略</el-button>
        <el-button type="default" :icon="RefreshLeft" :loading="loading" @click="load">还原</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="ap-row wide-left">
      <PanelCard title="提醒与授权策略" note="保存即生效" icon="Odometer" accent="policy" :index="4">
        <div v-loading="loading" class="cfg-list">
          <div v-for="item in items" :key="item.key" class="cfg-row">
            <div class="cfg-info">
              <b>{{ item.label }}</b>
              <p>{{ item.desc }}</p>
            </div>
            <div class="cfg-ctrl">
              <el-input-number v-model="values[item.key]" :min="1" :max="item.max" controls-position="right" />
              <span class="unit">{{ item.unit }}</span>
            </div>
          </div>
          <div class="ap-note" style="--nc:#f59e0b">
            <el-icon><Warning /></el-icon>
            <span>滞留升级阈值必须大于首次催领阈值，否则包裹会在被催领前直接进入滞留流程。</span>
          </div>
          <div class="cfg-actions">
            <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存策略</el-button>
            <el-button :icon="RefreshLeft" @click="load">还原为已保存值</el-button>
          </div>
        </div>
      </PanelCard>

      <PanelCard title="机制说明" note="定时任务节奏" icon="Clock" accent="notice" :index="5">
        <el-timeline class="mech">
          <el-timeline-item type="primary" title="入库即时通知" timestamp="触发式">
            包裹入库后立刻通过 站内信 + 短信 + 邮件 三渠道发送到站通知（短信/邮件为模拟实现，接口可替换真实服务商）。
          </el-timeline-item>
          <el-timeline-item type="warning" title="定时催领" timestamp="每 30 分钟">
            在库超过「首次催领阈值」的包裹自动催领；24 小时内不重复发送，每包裹每日不超过「每日催领上限」。
          </el-timeline-item>
          <el-timeline-item type="danger" title="滞留升级" timestamp="每 30 分钟">
            在库超过「滞留阈值」自动置为滞留超期，并通知物主与全部管理员，可在滞留件管理中退回。
          </el-timeline-item>
          <el-timeline-item type="success" title="授权自动过期" timestamp="每 10 分钟">
            代取授权超过有效期或「授权最长有效期」自动置为过期，并通知被授权人；授权码为一次性，核销即失效。
          </el-timeline-item>
        </el-timeline>
      </PanelCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Clock, RefreshLeft, Setting, Warning } from '@element-plus/icons-vue'
import { listConfigs, updateConfigs } from '../../api'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const loading = ref(false)
const saving = ref(false)
const values = reactive({})
const items = [
  { key: 'remind.first.delay.hours', label: '首次催领阈值', unit: '小时', max: 720,
    desc: '入库后超过该时长未取，开始定时催领（默认 24）' },
  { key: 'remind.overdue.hours', label: '滞留升级阈值', unit: '小时', max: 720,
    desc: '超过该时长未取升级为滞留件，须大于首次催领阈值（默认 72）' },
  { key: 'remind.daily.max', label: '每日催领上限', unit: '次/包裹', max: 24,
    desc: '同一包裹每日最多催领次数，防打扰（默认 3）' },
  { key: 'auth.max.valid.hours', label: '授权最长有效期', unit: '小时', max: 720,
    desc: '代取授权允许的最长有效时长上限（默认 72）' }
]

const heroStats = computed(() => [
  { label: '首次催领', value: values['remind.first.delay.hours'] || 0, unit: '小时' },
  { label: '滞留阈值', value: values['remind.overdue.hours'] || 0, unit: '小时' },
  { label: '每日上限', value: values['remind.daily.max'] || 0, unit: '次' }
])
const metrics = computed(() => [
  { label: '首次催领阈值', value: values['remind.first.delay.hours'] || 0, unit: '小时', icon: 'Bell', color: '#5b7cfa', foot: '超过即开始定时催领' },
  { label: '滞留升级阈值', value: values['remind.overdue.hours'] || 0, unit: '小时', icon: 'AlarmClock', color: '#e11d48', foot: '超过自动升级滞留' },
  { label: '每日催领上限', value: values['remind.daily.max'] || 0, unit: '次', icon: 'Message', color: '#f59e0b', foot: '每包裹每日防打扰' },
  { label: '授权最长有效期', value: values['auth.max.valid.hours'] || 0, unit: '小时', icon: 'Share', color: '#10d9b5', foot: '代取授权时长上限' }
])

async function load() {
  loading.value = true
  try {
    const list = await listConfigs()
    for (const item of items) {
      const cfg = list.find(c => c.configKey === item.key)
      values[item.key] = cfg ? Number(cfg.configValue) : 24
    }
  } finally { loading.value = false }
}

async function save() {
  if (values['remind.overdue.hours'] <= values['remind.first.delay.hours']) {
    return ElMessage.warning('滞留阈值必须大于首次催领阈值')
  }
  saving.value = true
  try {
    const payload = {}
    for (const item of items) payload[item.key] = String(values[item.key])
    await updateConfigs(payload)
    ElMessage.success('策略已保存，下一轮扫描生效')
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.cfg-list { display: flex; flex-direction: column; gap: 4px; }
.cfg-row {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 13px 4px; border-bottom: 1px dashed var(--default-border);
}
.cfg-row:last-of-type { border-bottom: none; }
.cfg-info b { font-size: 13.5px; color: var(--art-gray-900); }
.cfg-info p { margin: 3px 0 0; font-size: 12px; color: var(--art-gray-500); line-height: 1.55; }
.cfg-ctrl { display: flex; align-items: center; gap: 8px; flex: none; }
.cfg-ctrl .unit { font-size: 12px; color: var(--art-gray-600); white-space: nowrap; }
.cfg-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.mech { padding-left: 4px; }
.mech :deep(.el-timeline-item__content) { font-size: 12.5px; color: var(--art-gray-600); line-height: 1.65; }
.mech :deep(.el-timeline-item__timestamp) { font-size: 11px; }
@media (max-width: 560px) {
  .cfg-row { flex-direction: column; align-items: flex-start; gap: 8px; }
  .cfg-ctrl { width: 100%; }
  .cfg-ctrl :deep(.el-input-number) { flex: 1; }
}
</style>