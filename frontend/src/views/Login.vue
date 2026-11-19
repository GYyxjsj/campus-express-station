<template>
  <div class="auth-page" :class="channel">
    <button class="theme-btn" @click="theme.toggle()" aria-label="切换主题">
      <el-icon :size="17"><Sunny v-if="theme.mode === 'dark'" /><Moon v-else /></el-icon>
    </button>

    <!-- 左侧品牌区：插画主视觉 + 极光 + 半调网点 + 打字机 -->
    <div class="auth-left grain">
      <div class="aurora"><i /><i /><i /></div>
      <div class="dot-layer" />
      <div class="brand-logo">
        <div class="brand-icon"><el-icon :size="22"><Box /></el-icon></div>
        <span>校园快递驿站</span>
        <span class="flair-stamp">Campus Express</span>
      </div>
      <div class="hero">
        <div class="hero-art flair-in" style="--i:2">
          <img :src="heroImg" alt="校园快递驿站插画：学生扫码取件与货架" />
          <div class="float-card c1 art-card">
            <el-icon color="#5b7cfa" :size="20"><Box /></el-icon>
            <div><b>包裹到站</b><span>SF1391… 已到 A 区货架</span></div>
            <span class="pill pill-blue">待取件</span>
          </div>
          <div class="float-card c2 art-card">
            <el-icon color="#10d9b5" :size="20"><Promotion /></el-icon>
            <div><b>代取授权</b><span>已授权 林同学 代取</span></div>
            <span class="pill pill-green">已生效</span>
          </div>
        </div>
        <h1 class="hero-title flair-in" style="--i:1">
          取件提醒 · 代取授权<br /><span class="flair-grad-text">驿站运营管理系统</span>
        </h1>
        <p class="hero-type flair-in" style="--i:3">
          <TypeWriter :texts="slogans" loop :speed="90" :hold="2400" />
        </p>
        <p class="hero-sub flair-in" style="--i:5">覆盖包裹入库、取件提醒、代取授权、核销出库与操作审计的完整业务闭环</p>
      </div>
      <div class="left-foot">Spring Boot · MyBatis · PostgreSQL · Vue 3 · Art Design</div>
    </div>

    <!-- 右侧登录表单：双通道 -->
    <div class="auth-right">
      <div class="auth-right-wrap">
        <div class="form">
          <h2 class="title">欢迎回来</h2>
          <p class="sub-title">选择身份通道，凭{{ channel === 'student' ? '学号' : '工号' }}与验证码登录</p>

          <!-- 通道切换：学生 / 教职工 -->
          <div class="channel-switch" role="tablist" aria-label="登录通道">
            <span class="switch-thumb" :class="{ right: channel === 'staff' }" />
            <button role="tab" :aria-selected="channel === 'student'" :class="{ active: channel === 'student' }"
                    @click="switchChannel('student')">
              <el-icon :size="16"><Reading /></el-icon>学生通道
            </button>
            <button role="tab" :aria-selected="channel === 'staff'" :class="{ active: channel === 'staff' }"
                    @click="switchChannel('staff')">
              <el-icon :size="16"><OfficeBuilding /></el-icon>教职工通道
            </button>
          </div>
          <p class="channel-note">
            {{ channel === 'student' ? '在校学生使用学号登录，接收取件提醒、发起代取授权。' : '驿站员工使用工号登录；系统管理员请在本通道使用 admin 账号。' }}
          </p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form" @keyup.enter="submit">
            <el-form-item prop="username">
              <el-input v-model="form.username" class="custom-height"
                        :placeholder="channel === 'student' ? '学号（如 20230101）' : '工号（如 staff01）/ admin'" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" class="custom-height" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="form.captcha" class="custom-height captcha-input" maxlength="4"
                          placeholder="验证码" :prefix-icon="Key" autocomplete="off" />
                <button type="button" class="captcha-box" title="看不清？点击刷新" aria-label="刷新验证码" @click="refreshCaptcha">
                  <canvas ref="captchaRef" class="captcha-canvas" />
                </button>
              </div>
            </el-form-item>
            <el-button v-ripple type="primary" size="large" class="submit custom-height"
                       :loading="loading" @click="submit">
              <span class="submit-inner">{{ channel === 'student' ? '学生登录' : '教职工登录' }}<el-icon class="go"><Right /></el-icon></span>
            </el-button>
            <div class="links" v-if="channel === 'student'">
              还没有账号？<router-link to="/register">注册学生账号</router-link>
            </div>
            <div class="links" v-else>教职工账号由管理员统一开通</div>
          </el-form>

          <div class="demo-tip art-card art-card-sm halftone-corner">
            <div class="demo-title">演示账号（密码均为 123456，验证码自动填入）</div>
            <div class="demo-accounts">
              <button v-for="a in channelDemos" :key="a.u" class="demo-chip" @click="fill(a.u)">
                <b>{{ a.u }}</b><span>{{ a.r }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useThemeStore } from '../stores/theme'
import TypeWriter from '../components/TypeWriter.vue'
import { drawCaptcha } from '../utils/captcha'
import heroImg from '../assets/login-hero.png'

const store = useUserStore()
const theme = useThemeStore()
const router = useRouter()
const route = useRoute()
const formRef = ref()
const captchaRef = ref()
const loading = ref(false)
const channel = ref('student')
let captchaCode = ''

const form = reactive({ username: '', password: '', captcha: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位字符', trigger: 'blur' }
  ]
}
const slogans = [
  '包裹入库后，自动向收件人发送取件提醒。',
  '代取授权限时生效，凭授权码与取件码核销。',
  '滞留包裹自动标记超期，支持批量催领。',
  '出入库操作全程留痕，可按模块审计追溯。'
]
const channelDemos = computed(() => channel.value === 'student'
  ? [{ u: '20230101', r: '学生 · 陈嘉睿' }, { u: '20230102', r: '学生 · 林晚晴' }]
  : [{ u: 'staff01', r: '驿站员工' }, { u: 'admin', r: '系统管理员' }])

function refreshCaptcha() {
  if (!captchaRef.value) return
  captchaCode = drawCaptcha(captchaRef.value, theme.mode === 'dark')
  form.captcha = ''
}
function switchChannel(c) {
  if (channel.value === c) return
  channel.value = c
  formRef.value?.clearValidate()
  refreshCaptcha()
}
function fill(u) {
  form.username = u
  form.password = '123456'
  form.captcha = captchaCode
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value.validate()
  if (form.captcha.trim().toUpperCase() !== captchaCode) {
    ElMessage.error('验证码不正确，已刷新')
    refreshCaptcha()
    return
  }
  loading.value = true
  try {
    await store.login({ username: form.username, password: form.password })
    const roles = store.user?.roles || []
    const isStudentOnly = roles.includes('STUDENT') && !roles.includes('STAFF') && !roles.includes('ADMIN')
    if (channel.value === 'student' && !isStudentOnly) {
      store.logout()
      ElMessage.warning('该账号属于教职工，请切换到「教职工通道」登录')
      refreshCaptcha()
      return
    }
    if (channel.value === 'staff' && isStudentOnly) {
      store.logout()
      ElMessage.warning('学生账号请使用「学生通道」登录')
      refreshCaptcha()
      return
    }
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

watch(() => theme.mode, () => nextTick(refreshCaptcha))
onMounted(refreshCaptcha)
</script><style scoped>
.auth-page {
  height: 100vh; height: 100dvh;
  display: flex;
  background: var(--default-box-color);
  overflow: hidden;
  position: relative;
}
.theme-btn {
  position: absolute; top: 20px; right: 20px; z-index: 10;
  width: 38px; height: 38px;
  border: 1px solid var(--default-border);
  background: var(--default-box-color);
  border-radius: 10px;
  color: var(--art-gray-700);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: all 0.25s var(--flair-ease);
}
.theme-btn:hover { background: var(--art-hover-color); border-color: var(--flair-dot-strong); }

/* ---- 左栏品牌区 ---- */
.auth-left {
  flex: 1;
  background: linear-gradient(160deg, #eef2ff 0%, #f7f6ff 45%, #e9f9f4 100%);
  display: flex; flex-direction: column;
  padding: 32px 48px;
  position: relative;
  overflow: hidden;
}
html.dark .auth-left {
  background: linear-gradient(160deg, #0e1220 0%, #120f22 45%, #0a1614 100%);
}
.dot-layer {
  position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(var(--flair-dot-strong) 1.4px, transparent 1.4px);
  background-size: 18px 18px;
  -webkit-mask-image: linear-gradient(115deg, #000 0%, transparent 60%);
  mask-image: linear-gradient(115deg, #000 0%, transparent 60%);
}
.brand-logo { display: flex; align-items: center; gap: 10px; font-size: 17px; font-weight: 600; color: var(--art-gray-900); position: relative; z-index: 1; }
.brand-logo .flair-stamp { margin-left: 6px; }
.brand-icon {
  width: 38px; height: 38px; border-radius: 11px;
  background: var(--flair-primary);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 3px 0 0 #3d55c8;
}
html.dark .brand-icon { box-shadow: 0 3px 0 0 #2c3f9e; }
.hero { margin: auto 0; max-width: 560px; position: relative; z-index: 1; }
.hero-title { font-size: 36px; font-weight: 800; line-height: 1.32; color: var(--art-gray-900); margin: 30px 0 14px; letter-spacing: 0.5px; }
.hero-type { min-height: 30px; font-size: 19px; font-weight: 600; color: var(--flair-primary); margin-bottom: 10px; }
.hero-sub { font-size: 13.5px; color: var(--art-gray-600); line-height: 1.7; }
.left-foot { position: relative; z-index: 1; font-size: 11px; letter-spacing: 2px; color: var(--art-gray-600); text-transform: uppercase; }

/* ---- 插画主视觉：mask 融底 + 浮动信息卡 ---- */
.hero-art { position: relative; height: 300px; }
.hero-art img {
  width: 100%; height: 100%; object-fit: cover; object-position: center 42%;
  border-radius: 20px;
  -webkit-mask-image: radial-gradient(115% 105% at 50% 45%, #000 58%, transparent 96%);
  mask-image: radial-gradient(115% 105% at 50% 45%, #000 58%, transparent 96%);
  mix-blend-mode: multiply;
}
html.dark .hero-art img {
  mix-blend-mode: normal;
  filter: brightness(0.5) saturate(0.8) hue-rotate(-6deg);
  opacity: 0.85;
  -webkit-mask-image: radial-gradient(105% 95% at 50% 45%, #000 40%, transparent 88%);
  mask-image: radial-gradient(105% 95% at 50% 45%, #000 40%, transparent 88%);
}
.float-card {
  position: absolute;
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  width: 300px;
  animation: floatY 5s ease-in-out infinite;
  backdrop-filter: blur(6px);
  z-index: 2;
}
.float-card b { display: block; font-size: 14px; color: var(--art-gray-900); }
.float-card span { font-size: 12px; color: var(--art-gray-600); }
.float-card .pill { margin-left: auto; flex-shrink: 0; }
.c1 { left: -14px; top: 24px; }
.c2 { right: -10px; bottom: 18px; animation-delay: 1.6s; }
@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.pill { font-size: 11px; padding: 3px 10px; border-radius: 20px; font-weight: 600; }
.pill-blue { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.pill-green { background: var(--el-color-success-light-9); color: var(--el-color-success-dark-2); }

/* ---- 右栏表单 ---- */
.auth-right { width: 520px; flex-shrink: 0; position: relative; background: var(--default-box-color); }
.auth-right::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(420px 260px at 100% 0%, rgba(139, 92, 246, 0.07), transparent 70%),
    radial-gradient(420px 300px at 0% 100%, rgba(91, 124, 250, 0.07), transparent 70%);
}
.auth-right-wrap {
  position: absolute; inset: 0;
  width: 440px; max-height: 100%;
  padding: 5px 0; margin: auto;
  height: fit-content;
  overflow-y: auto; overflow-x: hidden;
  animation: slideInRight 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
.form { padding: 34px 0; position: relative; }
.title { font-size: 30px; font-weight: 700; color: var(--art-gray-900); }
.sub-title { margin-top: 8px; font-size: 14px; color: var(--art-gray-600); }

/* ---- 通道切换：滑块 segmented ---- */
.channel-switch {
  position: relative;
  margin-top: 22px;
  display: grid; grid-template-columns: 1fr 1fr;
  background: var(--art-hover-color);
  border: 1px solid var(--default-border);
  border-radius: 13px;
  padding: 4px;
}
.switch-thumb {
  position: absolute; top: 4px; bottom: 4px; left: 4px;
  width: calc(50% - 4px);
  border-radius: 10px;
  background: var(--flair-grad-hero);
  box-shadow: 0 6px 14px -6px rgba(91, 124, 250, 0.6);
  transition: transform 0.35s var(--flair-ease);
}
.switch-thumb.right { transform: translateX(100%); }
.channel-switch button {
  position: relative; z-index: 1;
  display: inline-flex; align-items: center; justify-content: center; gap: 7px;
  height: 40px;
  border: none; background: transparent;
  border-radius: 10px;
  font-size: 14px; font-weight: 600;
  color: var(--art-gray-600);
  cursor: pointer;
  transition: color 0.3s;
}
.channel-switch button.active { color: #fff; }
.channel-note { margin-top: 10px; font-size: 12px; color: var(--art-gray-600); line-height: 1.6; min-height: 20px; }

.login-form { margin-top: 18px; }
.custom-height { height: 42px; }

/* ---- 验证码 ---- */
.captcha-row { display: flex; gap: 10px; width: 100%; align-items: center; }
.captcha-input { flex: 1; }
.captcha-box {
  flex-shrink: 0;
  width: 116px; height: 42px;
  padding: 0;
  border: 1.5px solid var(--default-border);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: transparent;
  transition: border-color 0.25s, transform 0.2s var(--flair-ease);
}
.captcha-box:hover { border-color: var(--flair-primary); transform: translateY(-1px); }
.captcha-box:active { transform: translateY(0); }
.captcha-canvas { display: block; width: 100%; height: 100%; }

.submit { width: 100%; margin-top: 6px; height: 44px; font-size: 15px; letter-spacing: 2px; }
.submit-inner { display: inline-flex; align-items: center; gap: 6px; }
.submit .go { transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1); }
.submit:hover .go { transform: translateX(4px); }
.links { margin-top: 16px; text-align: center; font-size: 13px; color: var(--art-gray-600); }
.links a { color: var(--el-color-primary); text-decoration: none; font-weight: 500; }

.demo-tip { margin-top: 24px; padding: 14px 16px; }
.demo-title { font-size: 12px; color: var(--art-gray-600); margin-bottom: 10px; position: relative; z-index: 1; }
.demo-accounts { display: flex; gap: 8px; position: relative; z-index: 1; }
.demo-chip {
  flex: 1;
  border: 1.5px solid var(--default-border);
  background: transparent;
  border-radius: var(--art-radius-card-xs);
  padding: 8px 6px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.23, 1, 0.32, 1);
}
.demo-chip:hover {
  border-color: var(--flair-ink);
  background: var(--flair-ink);
  transform: translateY(-2px);
  box-shadow: 0 8px 16px -8px rgba(20, 20, 40, 0.45);
}
.demo-chip:hover b, .demo-chip:hover span { color: var(--flair-paper); }
.demo-chip:active { transform: translateY(0); box-shadow: none; }
.demo-chip b { display: block; font-size: 13px; color: var(--art-gray-800); transition: color 0.22s; }
.demo-chip span { font-size: 11px; color: var(--art-gray-600); transition: color 0.22s; }

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ---- 响应式 ---- */
@media (max-width: 1024px) {
  .auth-left { display: none; }
  .auth-right { width: 100%; }
}
@media (max-width: 640px) {
  .auth-right-wrap { width: 100%; position: static; padding: 0 28px; box-sizing: border-box; animation: none; max-height: none; }
  .auth-right { overflow-y: auto; overflow-x: hidden; }
  .form { padding: 56px 0 40px; }
  .title { font-size: 26px; }
}
@media (max-height: 760px) and (min-width: 1025px) {
  .hero-art { height: 220px; }
  .hero-title { font-size: 30px; margin-top: 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .float-card { animation: none !important; }
  .switch-thumb { transition: none !important; }
}
</style>