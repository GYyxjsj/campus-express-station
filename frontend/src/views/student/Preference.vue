<template>
  <div class="ap">
    <PageHero tone="policy" icon="Setting" chip="RECEIVE PREFERENCE" live title="收货偏好"
      sub="设置取件时段、交付方式与通知渠道，驿站将按你的偏好安排提醒与派送；偏好可随时调整，保存后立即生效。"
      :stats="heroStats" />

    <div class="ap-row wide-left">
      <PanelCard title="偏好设置" note="保存后立即生效" icon="SetUp" accent="policy" :index="1">
        <div v-loading="loading" class="ap-form">
          <div class="ap-field-row">
            <label>期望取件时段</label>
            <div class="pref-seg">
              <button v-for="(v, k) in PREF_TIME_SLOT" :key="k" :class="{ on: form.timeSlot === k }" @click="form.timeSlot = k">
                {{ v }}
              </button>
            </div>
          </div>
          <div class="ap-field-row">
            <label>默认交付方式</label>
            <div class="pref-seg">
              <button v-for="(v, k) in PREF_METHOD" :key="k" :class="{ on: form.deliverMethod === k }" @click="form.deliverMethod = k">
                {{ v }}
              </button>
            </div>
            <div class="ap-form-tip">「送件上门」按默认收货地址派送；「委托代取」需在授权管理中指定代取人。</div>
          </div>
          <div class="ap-field-row">
            <label>通知渠道</label>
            <div class="pref-seg">
              <button v-for="(v, k) in PREF_CHANNEL" :key="k" :class="{ on: form.notifyChannel === k }" @click="form.notifyChannel = k">
                {{ v }}
              </button>
            </div>
          </div>
          <div class="ap-field-row">
            <label>免打扰时段</label>
            <el-input v-model="form.quietHours" placeholder="如 22:00-08:00，留空表示不设免打扰" maxlength="20" />
            <div class="ap-form-tip">该时段内不推送短信/电话提醒，站内消息仍正常记录。</div>
          </div>
          <div class="ap-field-row">
            <label>服务开关</label>
            <div class="pref-switches">
              <div class="ps-item">
                <div><b>拒绝来电</b><p>驿站仅通过短信/站内信联系，不打电话</p></div>
                <el-switch v-model="form.noCall" />
              </div>
              <div class="ps-item">
                <div><b>易碎品重点看护</b><p>易碎标签包裹单独存放并优先提醒</p></div>
                <el-switch v-model="form.fragileCare" />
              </div>
              <div class="ps-item">
                <div><b>自动委托代取</b><p>超时未取时按有效授权自动转代取</p></div>
                <el-switch v-model="form.autoProxy" />
              </div>
            </div>
          </div>
          <div class="ap-field-row">
            <label>备注给驿站</label>
            <el-input v-model="form.note" type="textarea" :rows="3" maxlength="200" show-word-limit
                      placeholder="如：大件请放低层货架；生鲜请优先通知" />
          </div>
        </div>
        <template #foot>
          <div class="pref-foot">
            <span v-if="updatedAt" class="ap-mono pf-time">上次保存：{{ fmtTime(updatedAt) }}</span>
            <el-button type="primary" :icon="Check" :loading="saving" @click="doSave">保存偏好</el-button>
          </div>
        </template>
      </PanelCard>

      <PanelCard title="偏好如何生效" icon="Reading" accent="slate" :index="2">
        <ol class="step-list">
          <li><b>提醒安排</b>催领提醒按期望时段与免打扰设置调度。</li>
          <li><b>渠道分发</b>按所选渠道推送：站内必达，短信/邮件按配置。</li>
          <li><b>交付方式</b>入库时展示给驿站员工，作为派送参考。</li>
          <li><b>自动代取</b>仅在存在有效代取授权时生效，可随时关闭。</li>
        </ol>
        <div class="ap-note" style="--nc:#8b5cf6">
          <el-icon><InfoFilled /></el-icon>
          <div>偏好为服务参考项，遇特殊天气或站点高峰，驿站可能电话确认，请以实际通知为准。</div>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, InfoFilled } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { myPreference, savePreference } from '../../api'
import { PREF_TIME_SLOT, PREF_METHOD, PREF_CHANNEL, fmtTime } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const updatedAt = ref('')
const form = reactive({
  timeSlot: 'ANY', deliverMethod: 'SELF', notifyChannel: 'IN_APP',
  quietHours: '', noCall: false, fragileCare: false, autoProxy: false, note: ''
})

const heroStats = computed(() => [
  { label: '交付方式', value: PREF_METHOD[form.deliverMethod] || '—' },
  { label: '通知渠道', value: PREF_CHANNEL[form.notifyChannel] || '—' },
  { label: '已开开关', value: [form.noCall, form.fragileCare, form.autoProxy].filter(Boolean).length, unit: '项' }
])

async function load() {
  loading.value = true
  try {
    const p = await myPreference()
    if (p) {
      Object.assign(form, {
        timeSlot: p.timeSlot || 'ANY',
        deliverMethod: p.deliverMethod || 'SELF',
        notifyChannel: p.notifyChannel || 'IN_APP',
        quietHours: p.quietHours || '',
        noCall: p.noCall === 1, fragileCare: p.fragileCare === 1, autoProxy: p.autoProxy === 1,
        note: p.note || ''
      })
      updatedAt.value = p.updatedAt || ''
    }
  } finally { loading.value = false }
}

async function doSave() {
  if (form.quietHours && !/^\d{2}:\d{2}-\d{2}:\d{2}$/.test(form.quietHours.trim())) {
    ElMessage.warning('免打扰时段格式应为 HH:mm-HH:mm，如 22:00-08:00')
    return
  }
  saving.value = true
  try {
    const p = await savePreference({
      timeSlot: form.timeSlot, deliverMethod: form.deliverMethod, notifyChannel: form.notifyChannel,
      quietHours: form.quietHours.trim(),
      noCall: form.noCall ? 1 : 0, fragileCare: form.fragileCare ? 1 : 0, autoProxy: form.autoProxy ? 1 : 0,
      note: form.note
    })
    updatedAt.value = (p && p.updatedAt) || new Date().toISOString()
    ElMessage.success('收货偏好已保存')
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.pref-seg { display: flex; gap: 8px; flex-wrap: wrap; }
.pref-seg button {
  border: 1px solid var(--default-border); border-radius: 10px; padding: 8px 14px;
  font-size: 12.5px; color: var(--art-gray-700); background: transparent; cursor: pointer;
  transition: all 0.2s var(--flair-ease);
}
.pref-seg button:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
.pref-seg button.on {
  border-color: var(--el-color-primary); color: var(--el-color-primary);
  background: var(--el-color-primary-light-9); font-weight: 600;
}
.pref-switches { display: flex; flex-direction: column; gap: 8px; }
.ps-item {
  display: flex; align-items: center; justify-content: space-between; gap: 14px;
  border: 1px solid var(--default-border); border-radius: 12px; padding: 11px 14px;
}
.ps-item b { font-size: 13px; color: var(--art-gray-900); }
.ps-item p { margin: 3px 0 0; font-size: 11.5px; color: var(--art-gray-500); }
.pref-foot { display: flex; align-items: center; justify-content: flex-end; gap: 14px; }
.pf-time { font-size: 11.5px; color: var(--art-gray-500); }
.step-list { margin: 0 0 12px; padding-left: 18px; display: flex; flex-direction: column; gap: 9px; }
.step-list li { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
.step-list b { color: var(--art-gray-900); margin-right: 5px; }
</style>