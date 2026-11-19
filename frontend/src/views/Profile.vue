<template>
  <div class="ap">
    <PageHero tone="identity" icon="User" chip="MY ACCOUNT" live title="个人中心"
      sub="账号资料与安全设置：手机号用于包裹匹配与短信提醒，密码修改后需重新登录。学号 / 工号由管理员开户，不可自行变更。"
      :stats="heroStats">
      <template #actions>
        <el-button v-if="store.isStudent" type="default" :icon="Postcard" @click="$router.push('/student/identity')">我的身份条码</el-button>
        <el-button v-if="store.isStudent" type="default" :icon="Wallet" @click="$router.push('/student/wallet')">我的钱包</el-button>
        <el-button type="default" :icon="Message" @click="$router.push('/notifications')">提醒中心</el-button>
      </template>
    </PageHero>

    <div class="ap-row">
      <PanelCard title="个人信息" note="开户资料" icon="Postcard" accent="identity" :index="4">
        <div class="me-head">
          <span class="me-avatar" :style="{ background: avatarBg }">{{ (u?.realName || u?.username || '?').slice(0, 1) }}</span>
          <div class="me-id">
            <b>{{ u?.realName }}</b>
            <span class="ap-mono">{{ u?.username }}</span>
            <div class="me-roles">
              <el-tag v-for="r in u?.roles || []" :key="r" size="small" effect="light"
                      :type="r === 'ADMIN' ? 'danger' : r === 'STAFF' ? 'warning' : 'primary'">
                {{ roleLabel(r) }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="ap-kv">
          <div><label>手机号</label><span class="ap-mono">{{ u?.phone || '—' }}</span></div>
          <div><label>邮箱</label><span>{{ u?.email || '未填写' }}</span></div>
          <div><label>注册时间</label><span class="ap-mono">{{ fmtTime(u?.createdAt) }}</span></div>
          <div><label>账号状态</label><span>正常在用</span></div>
        </div>
        <div class="ap-note" style="--nc:#8b5cf6">
          <el-icon><InfoFilled /></el-icon>
          <span>手机号是包裹归属匹配的唯一凭据，换号请联系管理员修改，否则新包裹将无法自动关联到你。</span>
        </div>
      </PanelCard>

      <PanelCard title="修改密码" note="修改后需重新登录" icon="Lock" accent="amber" :index="5">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="ap-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password placeholder="当前登录密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32位" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirm">
            <el-input v-model="form.confirm" type="password" show-password placeholder="再输入一次新密码"
                      @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :icon="Check" :loading="loading" @click="submit">保存修改</el-button>
        </el-form>
        <div class="ap-note" style="--nc:#f59e0b">
          <el-icon><Warning /></el-icon>
          <span>修改成功后当前会话立即失效，需要使用新密码重新登录。</span>
        </div>
      </PanelCard>
    </div>

    <PanelCard title="本地缓存" note="仅清理本机浏览器数据" icon="Delete" accent="slate" :index="6">
      <div class="cache-row">
        <div class="cache-info">
          <b>清理缓存与偏好记录</b>
          <p>清除本机保存的公告「不再弹出」记录、页面偏好等本地数据（约 {{ cacheSize }}）。登录状态与服务器数据不受影响。</p>
        </div>
        <el-button type="danger" plain :icon="Delete" @click="clearCache">一键清理</el-button>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, InfoFilled, Lock, Message, Postcard, User, Wallet, Warning } from '@element-plus/icons-vue'
import { changePassword } from '../api'
import { useUserStore } from '../stores/user'
import { fmtTime } from '../utils/format'
import PageHero from '../components/PageHero.vue'
import PanelCard from '../components/PanelCard.vue'

const store = useUserStore()
const router = useRouter()
const u = computed(() => store.user)
const formRef = ref()
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '6-32位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => v === form.newPassword ? cb() : cb(new Error('两次密码不一致')),
    trigger: 'blur'
  }]
}

const roleLabel = r => ({ ADMIN: '管理员', STAFF: '驿站员工', STUDENT: '学生' }[r] || r)
const avatarBg = computed(() => {
  const r = (u.value?.roles || [])[0]
  return r === 'ADMIN' ? 'linear-gradient(140deg,#e11d48,#fb7185)'
    : r === 'STAFF' ? 'linear-gradient(140deg,#f59e0b,#fbbf24)'
    : 'linear-gradient(140deg,#8b5cf6,#a78bfa)'
})
const heroStats = computed(() => [
  { label: '角色', value: (u.value?.roles || []).length, unit: '个' },
  { label: '未读提醒', value: store.unread, unit: '条' }
])

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    store.logout()
    router.push('/login')
  } finally { loading.value = false }
}

/* ---- 本地缓存清理：保留登录态，清掉偏好/公告不再弹出等本地记录 ---- */
const KEEP_KEYS = ['ce_token', 'ce_user']
function localKeys() {
  const keys = []
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i)
    if (k && !KEEP_KEYS.includes(k)) keys.push(k)
  }
  return keys
}
const cacheSize = computed(() => {
  const bytes = localKeys().reduce((sum, k) => sum + (localStorage.getItem(k) || '').length * 2, 0)
  return bytes > 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${bytes} B`
})
async function clearCache() {
  const keys = localKeys()
  if (!keys.length) { ElMessage.info('没有可清理的本地缓存'); return }
  await ElMessageBox.confirm(
    `将清除 ${keys.length} 项本地记录（公告「不再弹出」、页面偏好等）。登录状态保留，服务器数据不受影响。`,
    '清理缓存', { type: 'warning', confirmButtonText: '清理', cancelButtonText: '取消' }
  )
  keys.forEach(k => localStorage.removeItem(k))
  ElMessage.success('本地缓存已清理')
}
</script>

<style scoped>
.me-head { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.me-avatar {
  width: 62px; height: 62px; border-radius: 18px; flex: none;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 27px; font-weight: 800;
  box-shadow: 0 8px 20px rgba(139, 92, 246, 0.3);
}
.me-id { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.me-id b { font-size: 18px; color: var(--art-gray-900); }
.me-id > span { font-size: 12.5px; color: var(--art-gray-500); }
.me-roles { display: flex; gap: 5px; flex-wrap: wrap; margin-top: 3px; }
.ap-note { margin-top: 14px; }
.cache-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.cache-info { flex: 1; min-width: 220px; }
.cache-info b { font-size: 13.5px; color: var(--art-gray-900); }
.cache-info p { margin: 5px 0 0; font-size: 12.5px; line-height: 1.7; color: var(--art-gray-600); }
</style>