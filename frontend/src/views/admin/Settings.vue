<template>
  <div class="ap">
    <PageHero tone="admin" icon="Setting" chip="SYSTEM SETTINGS" live title="系统设置"
      sub="配置 AI 助手接入（OpenAI 兼容接口）与「关于我们」站点信息。API Key 仅回显脱敏片段，留空或保持脱敏值提交则不修改；未配置 Key 时学生端 AI 助手自动降级为规则应答。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHero>

    <PanelCard title="AI 助手接入" note="OpenAI 兼容接口 · 仅回答快递域问题" icon="MagicStick" accent="staff" :index="0">
      <template #extra>
        <el-tag :type="aiReady ? 'success' : 'info'" effect="light" size="small">
          {{ aiReady ? 'AI 已就绪' : '降级模式（未启用或未配 Key）' }}
        </el-tag>
      </template>
      <div v-loading="loading" class="ap-form">
        <div class="ap-field-row switch-row">
          <div>
            <label>启用 AI 助手</label>
            <div class="ap-form-tip">关闭后学生端 AI 入口保留，但全部走本地规则应答。</div>
          </div>
          <el-switch v-model="aiEnabled" active-text="启用" inactive-text="关闭" inline-prompt />
        </div>
        <div class="ap-field-row">
          <label>API 地址</label>
          <el-input v-model="form['ai.api.url']" maxlength="300" clearable
                    placeholder="https://api.openai.com/v1/chat/completions" />
          <div class="ap-form-tip">须为 http(s) 完整地址，指向 OpenAI 兼容的 chat/completions 端点。</div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>模型名称</label>
            <el-input v-model="form['ai.api.model']" maxlength="300" clearable placeholder="gpt-4o-mini" />
          </div>
          <div class="ap-field-row">
            <label>API Key</label>
            <el-input v-model="form['ai.api.key']" maxlength="200" clearable show-password
                      placeholder="留空或保持脱敏值则不修改" />
            <div class="ap-form-tip">已保存的 Key 只显示首尾 4 位，重新输入完整 Key 才会覆盖。</div>
          </div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>携带历史轮数（0 ~ 20）</label>
            <el-input-number v-model="maxHistory" :min="0" :max="20" style="width:100%" />
            <div class="ap-form-tip">0 表示每次仅发送当前问题，不带上下文。</div>
          </div>
          <div class="ap-field-row">
            <label>请求超时（毫秒，3000 ~ 60000）</label>
            <el-input-number v-model="timeoutMs" :min="3000" :max="60000" :step="1000" style="width:100%" />
          </div>
        </div>
        <div class="ap-note" style="--nc:#5b7cfa">
          <el-icon><InfoFilled /></el-icon>
          <span>内置提示词注入防护：系统提示词固定为快递域助手，检测到「忽略指令 / 扮演其他角色 / 泄露提示词」等注入话术会直接拒绝。</span>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Check" :loading="savingAi" @click="saveAi">保存 AI 设置</el-button>
        </div>
      </div>
    </PanelCard>

    <PanelCard title="关于我们（站点信息）" note="展示在学生端「关于我们」页" icon="OfficeBuilding" accent="slate" :index="1">
      <div v-loading="loading" class="ap-form">
        <div class="form-2col">
          <div class="ap-field-row">
            <label>运营主体</label>
            <el-input v-model="form['about.company']" maxlength="300" placeholder="例如：校园快递驿站运营中心" />
          </div>
          <div class="ap-field-row">
            <label>负责人</label>
            <el-input v-model="form['about.owner']" maxlength="300" placeholder="负责人姓名" />
          </div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>联系邮箱</label>
            <el-input v-model="form['about.email']" maxlength="300" placeholder="service@example.edu.cn" />
          </div>
          <div class="ap-field-row">
            <label>服务热线</label>
            <el-input v-model="form['about.phone']" maxlength="300" placeholder="400-000-0000" />
          </div>
        </div>
        <div class="ap-field-row">
          <label>办公地址</label>
          <el-input v-model="form['about.address']" maxlength="300" placeholder="校区 / 楼栋 / 房间号" />
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>备案号</label>
            <el-input v-model="form['about.icp']" maxlength="300" placeholder="可留空" />
          </div>
          <div class="ap-field-row">
            <label>系统版本</label>
            <el-input v-model="form['about.version']" maxlength="300" placeholder="例如：v2.0.0" />
          </div>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Check" :loading="savingAbout" @click="saveAbout">保存站点信息</el-button>
        </div>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, InfoFilled, Refresh } from '@element-plus/icons-vue'
import { adminSettings, adminSaveSettings, aiStatus } from '../../api'

const loading = ref(false)
const savingAi = ref(false)
const savingAbout = ref(false)
const aiReady = ref(false)

const AI_KEYS = ['ai.enabled', 'ai.api.url', 'ai.api.key', 'ai.api.model', 'ai.max.history', 'ai.timeout.ms']
const ABOUT_KEYS = ['about.company', 'about.owner', 'about.email', 'about.phone', 'about.address', 'about.icp', 'about.version']

const form = reactive({})
const aiEnabled = ref(true)
const maxHistory = ref(0)
const timeoutMs = ref(3000)

const heroStats = computed(() => [
  { label: 'AI 状态', value: aiReady.value ? '就绪' : '降级', unit: '' },
  { label: '历史轮数', value: maxHistory.value, unit: '轮' },
  { label: '超时上限', value: Math.round(timeoutMs.value / 1000), unit: '秒' }
])

async function load() {
  loading.value = true
  try {
    const [data, status] = await Promise.all([adminSettings(), aiStatus().catch(() => ({ enabled: false }))])
    for (const k of [...AI_KEYS, ...ABOUT_KEYS]) form[k] = data[k] ?? ''
    aiEnabled.value = form['ai.enabled'] !== '0'
    maxHistory.value = Number(form['ai.max.history']) || 0
    timeoutMs.value = Number(form['ai.timeout.ms']) || 3000
    aiReady.value = !!status.enabled
  } finally { loading.value = false }
}

async function saveAi() {
  const url = (form['ai.api.url'] || '').trim()
  if (url && !/^https?:\/\//.test(url)) return ElMessage.warning('API 地址须为 http(s) URL')
  if (aiEnabled.value && (!url || !(form['ai.api.model'] || '').trim())) {
    ElMessage.warning('启用 AI 前请先填写 API 地址与模型名称')
    return
  }
  savingAi.value = true
  try {
    await adminSaveSettings({
      'ai.enabled': aiEnabled.value ? '1' : '0',
      'ai.api.url': url,
      'ai.api.key': form['ai.api.key'] || '',
      'ai.api.model': (form['ai.api.model'] || '').trim(),
      'ai.max.history': String(maxHistory.value),
      'ai.timeout.ms': String(timeoutMs.value)
    })
    ElMessage.success('AI 设置已保存')
    await load()
  } finally { savingAi.value = false }
}

async function saveAbout() {
  savingAbout.value = true
  try {
    const payload = {}
    for (const k of ABOUT_KEYS) payload[k] = (form[k] || '').trim()
    await adminSaveSettings(payload)
    ElMessage.success('站点信息已保存，学生端「关于我们」即时生效')
  } finally { savingAbout.value = false }
}

onMounted(load)
</script>

<style scoped>
.form-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.ap-actions { display: flex; justify-content: flex-end; padding-top: 4px; }
@media (max-width: 640px) { .form-2col { grid-template-columns: 1fr; } }
</style>