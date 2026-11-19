<template>
  <el-drawer v-model="visible" :size="drawerSize" direction="rtl" :with-header="false"
             class="ai-drawer" :append-to-body="true">
    <div class="ai-shell">
      <header class="ai-head">
        <div class="ai-head-l">
          <span class="ai-avatar"><el-icon :size="20"><ChatDotRound /></el-icon></span>
          <div>
            <div class="ai-title">驿站 AI 助手</div>
            <div class="ai-sub">{{ statusText }}</div>
          </div>
        </div>
        <div class="ai-head-r">
          <el-tooltip content="清空对话" placement="bottom">
            <button class="ai-icon-btn" @click="clearChat" aria-label="清空对话"><el-icon><Delete /></el-icon></button>
          </el-tooltip>
          <button class="ai-icon-btn" @click="visible = false" aria-label="关闭"><el-icon><Close /></el-icon></button>
        </div>
      </header>

      <div class="ai-body" ref="bodyRef">
        <div v-if="!messages.length" class="ai-empty blinds">
          <div class="ai-empty-icon"><el-icon :size="34"><MagicStick /></el-icon></div>
          <p class="ai-empty-title">你好，我是驿站智能助手</p>
          <p class="ai-empty-sub">包裹、代取、运费、保险等驿站问题都可以问我，试试这些：</p>
          <button v-for="q in presets" :key="q" class="ai-preset" @click="send(q)">{{ q }}</button>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role">
          <span class="ai-msg-avatar">
            <el-icon v-if="m.role === 'assistant'" :size="15"><ChatDotRound /></el-icon>
            <template v-else>{{ meInitial }}</template>
          </span>
          <div class="ai-bubble">
            <span v-if="m.pending" class="ai-typing"><i /><i /><i /></span>
            <template v-else>{{ m.content }}</template>
          </div>
        </div>
      </div>

      <footer class="ai-foot">
        <el-input v-model="draft" :disabled="sending" placeholder="输入快递相关问题，回车发送"
                  maxlength="300" @keyup.enter="send()" clearable>
          <template #append>
            <el-button :loading="sending" @click="send()" aria-label="发送">
              <el-icon><Promotion /></el-icon>
            </el-button>
          </template>
        </el-input>
        <p class="ai-note">仅限快递 / 驿站业务咨询 · 回复由 AI 生成，仅供参考</p>
      </footer>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { aiChat, aiStatus } from '../api'
import { useUserStore } from '../stores/user'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const store = useUserStore()
const meInitial = computed(() => (store.user?.realName || '我').slice(0, 1))
const drawerSize = computed(() => {
  const w = window.innerWidth
  if (w <= 560) return '100%'
  return Math.min(620, Math.round(w * 0.46)) + 'px'
})

const presets = [
  '我的包裹到站多久了？',
  '取件码在哪里查看？',
  '怎么授权同学帮我代取？',
  '包裹超时了会怎么样？',
  '驿站自寄怎么收费？'
]

const messages = ref([])
const draft = ref('')
const sending = ref(false)
const bodyRef = ref(null)
const online = ref(null)

const statusText = computed(() => {
  if (online.value === null) return '仅限快递业务咨询'
  return online.value ? 'AI 在线 · 仅限快递业务咨询' : '规则应答模式 · 仅限快递业务咨询'
})

watch(visible, async v => {
  if (v && online.value === null) {
    try { online.value = !!(await aiStatus())?.enabled } catch (e) { online.value = false }
  }
})

function scrollBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function clearChat() { messages.value = [] }

async function send(text) {
  const q = (text || draft.value).trim()
  if (!q || sending.value) return
  draft.value = ''
  const history = messages.value
    .filter(m => !m.pending)
    .slice(-8)
    .map(m => ({ role: m.role, content: m.content }))
  messages.value.push({ role: 'user', content: q })
  const pendingMsg = { role: 'assistant', content: '', pending: true }
  messages.value.push(pendingMsg)
  sending.value = true
  scrollBottom()
  try {
    const res = await aiChat({ message: q, history })
    pendingMsg.content = res?.reply || '暂时没有得到有效回复，请稍后再试。'
    if (typeof res?.enabled === 'boolean') online.value = res.enabled
  } catch (e) {
    pendingMsg.content = e?.message || '网络异常，请稍后再试。'
  } finally {
    pendingMsg.pending = false
    sending.value = false
    scrollBottom()
  }
}
</script>

<style scoped>
.ai-shell { display: flex; flex-direction: column; height: 100%; }
.ai-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px 14px;
  border-bottom: 1px solid color-mix(in srgb, var(--flair-primary) 12%, var(--default-border));
}
.ai-head-l { display: flex; align-items: center; gap: 11px; min-width: 0; }
.ai-avatar {
  width: 40px; height: 40px; border-radius: 13px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--flair-primary), #8b5cf6);
  color: #fff;
  box-shadow: 0 8px 18px -8px color-mix(in srgb, var(--flair-primary) 80%, transparent);
}
.ai-title { font-size: var(--t-title, 14.5px); font-weight: 700; letter-spacing: var(--ls-heading, 0.2px); }
.ai-sub { font-size: var(--t-caption, 12px); color: var(--default-text-color-secondary, #8a90a5); margin-top: 2px; }
.ai-head-r { display: flex; gap: 6px; }
.ai-icon-btn {
  width: 32px; height: 32px; border-radius: 9px; border: none; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; color: var(--default-text-color-secondary, #8a90a5);
  transition: background 0.18s, color 0.18s;
}
.ai-icon-btn:hover { background: color-mix(in srgb, var(--flair-primary) 10%, transparent); color: var(--flair-primary); }
.ai-body { flex: 1; overflow-y: auto; padding: 18px 16px; display: flex; flex-direction: column; gap: 14px; }
.ai-empty { text-align: center; padding: 26px 8px 8px; }
.ai-empty-icon {
  width: 62px; height: 62px; margin: 0 auto 14px; border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--flair-primary) 10%, transparent);
  color: var(--flair-primary);
}
.ai-empty-title { font-size: var(--t-title, 14.5px); font-weight: 700; }
.ai-empty-sub { font-size: var(--t-caption, 12px); color: var(--default-text-color-secondary, #8a90a5); margin: 6px 0 16px; line-height: var(--lh-body, 1.62); }
.ai-preset {
  display: block; width: 100%; text-align: left;
  margin-bottom: 8px; padding: 10px 14px;
  border-radius: 11px; cursor: pointer;
  border: 1px solid color-mix(in srgb, var(--flair-primary) 16%, var(--default-border));
  background: var(--default-box-color, #fff);
  color: var(--default-text-color, #333);
  font-size: var(--t-body, 13.5px);
  transition: border-color 0.18s, background 0.18s, transform 0.18s;
}
.ai-preset:hover {
  border-color: var(--flair-primary);
  background: color-mix(in srgb, var(--flair-primary) 6%, var(--default-box-color, #fff));
  transform: translateX(3px);
}
.ai-msg { display: flex; gap: 9px; align-items: flex-start; }
.ai-msg.user { flex-direction: row-reverse; }
.ai-msg-avatar {
  width: 28px; height: 28px; border-radius: 9px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
  background: color-mix(in srgb, var(--flair-primary) 12%, transparent);
  color: var(--flair-primary);
}
.ai-msg.user .ai-msg-avatar { background: color-mix(in srgb, #8b5cf6 14%, transparent); color: #8b5cf6; }
.ai-bubble {
  max-width: 86%;
  padding: 10px 13px;
  border-radius: 13px;
  font-size: var(--t-body, 13.5px);
  line-height: var(--lh-prose, 1.75);
  letter-spacing: var(--ls-body, 0.15px);
  white-space: pre-wrap; word-break: break-word;
  background: var(--default-box-color, #fff);
  border: 1px solid var(--default-border);
}
.ai-msg.user .ai-bubble {
  background: var(--flair-primary); border-color: transparent; color: #fff;
}
.ai-typing { display: inline-flex; gap: 4px; padding: 4px 2px; }
.ai-typing i {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--flair-primary); opacity: 0.4;
  animation: ai-dot 1.1s ease-in-out infinite;
}
.ai-typing i:nth-child(2) { animation-delay: 0.18s; }
.ai-typing i:nth-child(3) { animation-delay: 0.36s; }
@keyframes ai-dot { 40% { opacity: 1; transform: translateY(-3px); } }
.ai-foot { padding: 12px 16px 14px; border-top: 1px solid var(--default-border); }
.ai-note { margin-top: 8px; text-align: center; font-size: var(--t-overline, 11px); color: var(--default-text-color-secondary, #9aa1b5); letter-spacing: var(--ls-label, 0.6px); }
@media (prefers-reduced-motion: reduce) {
  .ai-typing i, .ai-preset { animation: none !important; transition: none !important; }
}
</style>