<template>
  <div class="auth-page">
    <button class="theme-btn" @click="theme.toggle()" aria-label="切换主题">
      <el-icon :size="17"><Sunny v-if="theme.mode === 'dark'" /><Moon v-else /></el-icon>
    </button>

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
            <el-icon color="#8b5cf6" :size="20"><Postcard /></el-icon>
            <div><b>身份条码</b><span>注册即生成 Code 128 条纹码</span></div>
            <span class="pill pill-violet">免码取件</span>
          </div>
          <div class="float-card c2 art-card">
            <el-icon color="#f59e0b" :size="20"><Bell /></el-icon>
            <div><b>到站提醒</b><span>站内信 + 短信 + 邮件三渠道</span></div>
            <span class="pill pill-amber">即时推送</span>
          </div>
        </div>
        <h1 class="hero-title flair-in" style="--i:1">
          注册学生账号<br /><span class="flair-grad-text">开启智能取件体验</span>
        </h1>
        <p class="hero-type flair-in" style="--i:3">
          <TypeWriter :texts="slogans" loop :speed="90" :hold="2400" />
        </p>
        <p class="hero-sub flair-in" style="--i:5">学号开户 · 手机号匹配包裹 · 钱包充值 · 代取服务大厅 · 全程去向留痕</p>
      </div>
      <div class="left-foot">Spring Boot · MyBatis · PostgreSQL · Vue 3 · Art Design</div>
    </div>

    <div class="auth-right">
      <div class="auth-right-wrap">
        <div class="form">
          <h2 class="title">创建账号</h2>
          <p class="sub-title">使用学号注册，注册后即可接收取件提醒</p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" class="reg-form" label-position="top" @keyup.enter="submit">
            <div class="duo">
              <el-form-item label="学号" prop="username">
                <el-input v-model="form.username" class="custom-height" placeholder="4-20位字母或数字" :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" class="custom-height" placeholder="真实姓名" :prefix-icon="Postcard" />
              </el-form-item>
            </div>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" class="custom-height" maxlength="11" placeholder="用于包裹匹配与短信提醒" :prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item label="邮箱（选填）" prop="email">
              <el-input v-model="form.email" class="custom-height" placeholder="用于邮件提醒" :prefix-icon="Message" />
            </el-form-item>
            <div class="duo">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" class="custom-height" type="password" show-password placeholder="6-32位" :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirm">
                <el-input v-model="form.confirm" class="custom-height" type="password" show-password placeholder="再输入一次" :prefix-icon="Key" />
              </el-form-item>
            </div>
            <div class="strength" v-if="form.password">
              <div class="s-bar"><i :style="{ width: strength.pct + '%' }" :class="strength.cls" /></div>
              <span :class="strength.cls">密码强度：{{ strength.label }}</span>
            </div>
            <el-button v-ripple type="primary" size="large" class="submit custom-height" :loading="loading" @click="submit">
              <span class="submit-inner">注 册<el-icon class="go"><Right /></el-icon></span>
            </el-button>
            <div class="links">已有账号？<router-link to="/login">返回登录</router-link></div>
            <p class="agree">注册即代表同意驿站服务协议与隐私政策，包裹信息仅用于取件通知与去向留痕。</p>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Box, Iphone, Key, Lock, Message, Postcard, Right, User } from '@element-plus/icons-vue'
import { register } from '../api'
import { useThemeStore } from '../stores/theme'
import TypeWriter from '../components/TypeWriter.vue'
import heroImg from '../assets/login-hero.png'

const router = useRouter()
const theme = useThemeStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', realName: '', phone: '', email: '', password: '', confirm: '' })
const rules = {
  username: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{4,20}$/, message: '4-20位字母或数字', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '6-32位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')),
    trigger: 'blur'
  }]
}
const slogans = [
  '手机号是包裹归属匹配的唯一凭据。',
  '注册即生成专属身份条码，柜台免码取件。',
  '钱包充值满额赠送，代取服务悬赏下单。',
  '每一次出入库都记录时间、地点与检查员。'
]

const strength = computed(() => {
  const p = form.password
  let score = 0
  if (p.length >= 8) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  if (p.length >= 12) score++
  if (score <= 1) return { pct: 25, cls: 'weak', label: '弱' }
  if (score <= 3) return { pct: 60, cls: 'mid', label: '中' }
  return { pct: 100, cls: 'strong', label: '强' }
})

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const { confirm, ...data } = form
    if (!data.email) delete data.email
    await register(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
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

.auth-left {
  flex: 1;
  background: linear-gradient(160deg, #f3eeff 0%, #f7f6ff 45%, #e9f9f4 100%);
  display: flex; flex-direction: column;
  padding: 32px 48px;
  position: relative;
  overflow: hidden;
}
html.dark .auth-left { background: linear-gradient(160deg, #141024 0%, #120f22 45%, #0a1614 100%); }
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
.hero-type { min-height: 30px; font-size: 19px; font-weight: 600; color: #8b5cf6; margin-bottom: 10px; }
.hero-sub { font-size: 13.5px; color: var(--art-gray-600); line-height: 1.7; }
.left-foot { position: relative; z-index: 1; font-size: 11px; letter-spacing: 2px; color: var(--art-gray-600); text-transform: uppercase; }

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
@keyframes floatY { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
.pill { font-size: 11px; padding: 3px 10px; border-radius: 20px; font-weight: 600; }
.pill-violet { background: rgba(139, 92, 246, 0.14); color: #7c3aed; }
.pill-amber { background: rgba(245, 158, 11, 0.16); color: #b45309; }
html.dark .pill-violet { color: #c4b5fd; }
html.dark .pill-amber { color: #fbbf24; }

.auth-right { width: 560px; flex-shrink: 0; position: relative; background: var(--default-box-color); }
.auth-right::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(420px 260px at 100% 0%, rgba(139, 92, 246, 0.07), transparent 70%),
    radial-gradient(420px 300px at 0% 100%, rgba(16, 217, 181, 0.06), transparent 70%);
}
.auth-right-wrap {
  position: absolute; inset: 0;
  width: 470px; max-height: 100%;
  padding: 5px 0; margin: auto;
  height: fit-content;
  overflow-y: auto; overflow-x: hidden;
  animation: slideInRight 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
.form { padding: 30px 0; position: relative; }
.title { font-size: 30px; font-weight: 700; color: var(--art-gray-900); }
.sub-title { margin-top: 8px; font-size: 14px; color: var(--art-gray-600); }
.reg-form { margin-top: 18px; }
.reg-form :deep(.el-form-item__label) { font-size: 12.5px; color: var(--art-gray-600); padding-bottom: 4px; }
.custom-height { height: 42px; }
.duo { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }

.strength { display: flex; align-items: center; gap: 10px; margin: -6px 0 12px; }
.s-bar { flex: 1; height: 5px; border-radius: 999px; background: var(--art-gray-100); overflow: hidden; }
html.dark .s-bar { background: #26262e; }
.s-bar i { display: block; height: 100%; border-radius: 999px; transition: width 0.35s var(--flair-ease); }
.s-bar i.weak { background: linear-gradient(90deg, #e11d48, #fb7185); }
.s-bar i.mid { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.s-bar i.strong { background: linear-gradient(90deg, #0d9488, #34d399); }
.strength span { font-size: 11.5px; font-weight: 700; white-space: nowrap; }
.strength span.weak { color: #e11d48; }
.strength span.mid { color: #b45309; }
.strength span.strong { color: #0d9488; }
html.dark .strength span.mid { color: #fbbf24; }
html.dark .strength span.strong { color: #34d399; }

.submit { width: 100%; margin-top: 6px; height: 44px; font-size: 15px; letter-spacing: 4px; }
.submit-inner { display: inline-flex; align-items: center; gap: 6px; }
.submit .go { transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1); }
.submit:hover .go { transform: translateX(4px); }
.links { margin-top: 16px; text-align: center; font-size: 13px; color: var(--art-gray-600); }
.links a { color: var(--el-color-primary); text-decoration: none; font-weight: 500; }
.agree { margin: 12px 0 0; font-size: 11.5px; color: var(--art-gray-500); line-height: 1.6; text-align: center; }

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

@media (max-width: 1024px) {
  .auth-left { display: none; }
  .auth-right { width: 100%; }
}
@media (max-width: 640px) {
  .auth-right-wrap { width: 100%; position: static; padding: 0 24px; box-sizing: border-box; animation: none; max-height: none; }
  .auth-right { overflow-y: auto; overflow-x: hidden; }
  .form { padding: 52px 0 36px; }
  .title { font-size: 26px; }
  .duo { grid-template-columns: 1fr; gap: 0; }
}
@media (max-height: 760px) and (min-width: 1025px) {
  .hero-art { height: 220px; }
  .hero-title { font-size: 30px; margin-top: 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .float-card { animation: none !important; }
}
</style>