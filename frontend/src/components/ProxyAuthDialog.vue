<template>
  <el-dialog v-model="visible" title="发起代取授权" width="520px" append-to-body @closed="reset">
    <div class="ap-form">
      <div class="ap-note" style="--nc:#8b5cf6">
        <el-icon><Share /></el-icon>
        <span>为「<b class="ap-mono">{{ parcel?.trackingNo }}</b>」授权他人免费代取。系统生成一次性授权码，请线下告知代取人；代取人凭授权码 + 取件码到驿站核销。</span>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="代取人" prop="granteeUsername">
          <el-select v-model="form.granteeUsername" filterable remote clearable
                     :remote-method="searchGrantee" :loading="searching"
                     placeholder="输入学号 / 姓名搜索" style="width: 100%">
            <el-option v-for="u in candidates" :key="u.id"
                       :label="`${u.realName}（${u.username}）`" :value="u.username" />
          </el-select>
          <div v-if="friends.length" class="friend-chips">
            <span class="fc-label"><el-icon :size="13"><UserFilled /></el-icon>好友快选</span>
            <button v-for="f in friends" :key="f.id" type="button" class="fc-item"
                    :class="{ on: form.granteeUsername === f.username }"
                    @click="form.granteeUsername = f.username">
              {{ f.realName || f.username }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="有效时长（小时）" prop="validHours">
          <div class="hours-row">
            <el-input-number v-model="form.validHours" :min="1" :max="72" controls-position="right" />
            <div class="hour-chips">
              <button v-for="h in [6, 12, 24, 48, 72]" :key="h" type="button"
                      :class="{ on: form.validHours === h }" @click="form.validHours = h">{{ h }}h</button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注（选填）">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="如：帮忙送到 3 号宿舍楼" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :icon="Share" :loading="loading" @click="submit">生成授权</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="resultVisible" title="授权已生成" width="440px" append-to-body>
    <div class="auth-result">
      <p>请将下方授权码告知代取人 <b>{{ result?.granteeName }}</b>：</p>
      <div class="auth-box">
        <b class="ap-mono">{{ result?.authCode }}</b>
        <el-button size="small" type="primary" plain :icon="CopyDocument" @click="copyCode">复制授权码</el-button>
      </div>
      <p class="expire">有效期至 {{ fmtTime(result?.expireAt) }} · 过期自动失效，可随时撤销</p>
      <div class="ap-note" style="--nc:#f59e0b">
        <el-icon><Warning /></el-icon>
        <span>代取人还需向你索取该包裹的 6 位取件码，双码同时匹配才能核销出库。</span>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="resultVisible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Share, UserFilled, Warning } from '@element-plus/icons-vue'
import { createProxyAuth, searchUsers, myFriends } from '../api'
import { fmtTime } from '../utils/format'

const props = defineProps({ modelValue: Boolean, parcel: Object })
const emit = defineEmits(['update:modelValue', 'created'])
const visible = ref(false)
const resultVisible = ref(false)
const loading = ref(false)
const searching = ref(false)
const candidates = ref([])
const result = ref(null)
const formRef = ref()
const form = reactive({ granteeUsername: '', validHours: 24, remark: '' })
const rules = {
  granteeUsername: [{ required: true, message: '请选择代取人', trigger: 'change' }],
  validHours: [{ required: true, message: '请填写有效时长', trigger: 'blur' }]
}

watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => emit('update:modelValue', v))

/* 好友快选：打开弹窗时拉取好友列表，一键填入代取人 */
const friends = ref([])
watch(visible, async v => {
  if (v && !friends.value.length) {
    try { friends.value = (await myFriends()) || [] } catch { friends.value = [] }
  }
})

async function searchGrantee(kw) {
  if (!kw || kw.length < 2) { candidates.value = []; return }
  searching.value = true
  try { candidates.value = await searchUsers(kw) } finally { searching.value = false }
}

function reset() {
  form.granteeUsername = ''
  form.validHours = 24
  form.remark = ''
  candidates.value = []
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(result.value?.authCode || '')
    ElMessage.success('授权码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动抄录')
  }
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    result.value = await createProxyAuth({ parcelId: props.parcel.id, ...form })
    visible.value = false
    resultVisible.value = true
    ElMessage.success('授权创建成功')
    emit('created')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.hours-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; width: 100%; }
.hour-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.hour-chips button {
  border: 1px solid var(--default-border); background: transparent; color: var(--art-gray-600);
  border-radius: 999px; padding: 4px 11px; font-size: 11.5px; font-weight: 700; cursor: pointer;
  transition: all 0.18s var(--flair-ease);
}
.hour-chips button:hover { border-color: #8b5cf6; color: #8b5cf6; transform: translateY(-1px); }
.hour-chips button.on { border-color: #8b5cf6; color: #fff; background: linear-gradient(120deg, #7c3aed, #8b5cf6); }
.friend-chips { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; width: 100%; margin-top: 8px; }
.fc-label { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--art-gray-500); }
.fc-item {
  border: 1px solid var(--default-border); background: transparent; color: var(--art-gray-600);
  border-radius: 999px; padding: 3px 10px; font-size: 11.5px; font-weight: 700; cursor: pointer;
  transition: all 0.18s var(--flair-ease);
}
.fc-item:hover { border-color: #0d9488; color: #0d9488; transform: translateY(-1px); }
.fc-item.on { border-color: #0d9488; color: #fff; background: linear-gradient(120deg, #0d9488, #34d399); }
.auth-result { text-align: center; display: flex; flex-direction: column; gap: 10px; }
.auth-result p { margin: 0; font-size: 13px; color: var(--art-gray-700); }
.auth-box {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  border: 1.5px dashed rgba(139, 92, 246, 0.6); border-radius: 14px; padding: 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(167, 139, 250, 0.03));
}
.auth-box b { font-size: 30px; font-weight: 800; letter-spacing: 6px; color: #7c3aed; }
html.dark .auth-box b { color: #c4b5fd; }
.expire { color: var(--art-gray-500); font-size: 12px; }
.auth-result .ap-note { text-align: left; }
</style>