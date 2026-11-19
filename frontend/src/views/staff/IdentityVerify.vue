<template>
  <div class="ap">
    <PageHero tone="identity" icon="Aim" chip="IDENTITY VERIFY" live
      title="身份条码核验台"
      sub="扫描学生出示的 Code 128 身份条纹码，系统核验身份后返回其名下在库包裹，逐件确认即可免取件码出库，全程记入去向台账。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" @click="resetAll">清空台面</el-button>
        <el-button type="default" :icon="Postcard" @click="$router.push('/staff/traces')">去向台账</el-button>
      </template>
    </PageHero>

    <div class="ap-row wide-left">
      <!-- 扫码区 -->
      <PanelCard title="条码扫描" note="支持扫码枪 / 手工输入" icon="Aim" accent="identity" :index="1">
        <div class="scan-box" :class="{ scanning: verifying, ok: !!result, bad: !!error }">
          <div class="scan-laser" />
          <el-icon class="scan-glyph" :size="30"><Postcard /></el-icon>
          <label class="scan-label">身份条码编号</label>
          <el-input ref="inputRef" v-model="code" size="large" class="scan-input"
                    placeholder="CE 开头 12 位，如 CE5510176182" clearable
                    :prefix-icon="Search" @keyup.enter="doVerify" @clear="clearResult">
            <template #append>
              <el-button type="primary" :loading="verifying" :icon="Aim" @click="doVerify">核验</el-button>
            </template>
          </el-input>
          <p class="scan-tip">
            扫码枪读取后自动回车提交；手工输入请点击「核验」。条码不区分大小写，系统自动转大写并去除空格。
          </p>
          <div class="scan-actions">
            <el-button size="small" :icon="Delete" @click="code = ''; clearResult()">清空输入</el-button>
            <el-button size="small" :icon="Clock" @click="showHistory = !showHistory">
              {{ showHistory ? '隐藏' : '显示' }}本次会话记录（{{ session.length }}）
            </el-button>
          </div>
        </div>

        <div v-if="error" class="ap-note" style="--nc:#fb7185;margin-top:14px">
          <el-icon><CircleCloseFilled /></el-icon>
          <div><b>核验失败：</b>{{ error }}</div>
        </div>

        <div v-if="showHistory" class="session">
          <h4 class="s-h">本次会话核验记录</h4>
          <div v-if="session.length" class="ap-list">
            <div v-for="(s, i) in session" :key="i" class="ap-list-item" :style="{ '--la': s.ok ? '#10d9b5' : '#fb7185' }">
              <div class="ap-list-avatar"><el-icon :size="16"><component :is="s.ok ? 'CircleCheck' : 'CircleClose'" /></el-icon></div>
              <div class="ap-list-body">
                <div class="ap-list-l1"><b class="ap-mono">{{ s.code }}</b><span class="s-name">{{ s.name || '未知身份' }}</span></div>
                <div class="ap-list-l2">{{ s.msg }} · {{ s.at }}</div>
              </div>
            </div>
          </div>
          <div v-else class="ap-empty"><el-empty description="本次会话尚无核验记录" :image-size="60" /></div>
        </div>
      </PanelCard>

      <!-- 核验结果 -->
      <div class="side-col">
        <PanelCard title="身份核验结果" :note="result ? '已通过' : '等待扫码'" icon="UserFilled" accent="identity" :index="2">
          <div v-if="result" class="verify-result">
            <div class="vr-person">
              <div class="vr-avatar">{{ (result.user.realName || 'U').slice(0, 1) }}</div>
              <div class="vr-info">
                <b>{{ result.user.realName }}</b>
                <span class="ap-mono">{{ result.user.username }}</span>
                <span>{{ result.user.phone || '未绑定手机号' }}</span>
              </div>
              <div class="vr-badge"><el-icon :size="15"><CircleCheckFilled /></el-icon>身份一致</div>
            </div>
            <div class="ap-kv vr-kv">
              <div><label>条码编号</label><span class="ap-mono">{{ result.identity.code }}</span></div>
              <div><label>条码状态</label>
                <span><el-tag size="small" :type="dict(IDENTITY_STATUS, result.identity.status).type" effect="light">{{ dict(IDENTITY_STATUS, result.identity.status).label }}</el-tag></span>
              </div>
              <div><label>有效期至</label><span class="muted">{{ fmtTime(result.identity.expireAt) }}</span></div>
              <div><label>核验人</label><span>{{ result.verifiedBy }}</span></div>
              <div><label>核验时间</label><span class="muted">{{ verifiedAt }}</span></div>
              <div><label>累计核销</label><span>{{ result.identity.useCount || 0 }} 次</span></div>
            </div>
            <div class="ap-note" style="--nc:#8b5cf6;margin-top:13px">
              <el-icon><Lock /></el-icon>
              <div>出于隐私保护，核验结果<b>不展示取件码</b>。确认学生身份后直接点击出库，系统自动记录核验人、时间与货架地点。</div>
            </div>
          </div>
          <div v-else class="ap-empty">
            <el-empty description="尚未核验身份" :image-size="80" />
            <p>请在左侧扫描或输入学生出示的身份条码。</p>
          </div>
        </PanelCard>

        <PanelCard v-if="lastPickup" title="最近出库凭证" icon="Ticket" accent="mint" :index="3">
          <div class="pickup-slip">
            <div class="ps-row"><label>运单号</label><span class="ap-mono">{{ lastPickup.trackingNo }}</span></div>
            <div class="ps-row"><label>物主</label><span>{{ lastPickup.ownerName }}（{{ lastPickup.ownerUsername }}）</span></div>
            <div class="ps-row"><label>核销方式</label><span><el-tag size="small" type="primary" effect="light">身份条码免码出库</el-tag></span></div>
            <div class="ps-row"><label>检查员</label><span>{{ lastPickup.inspector }}</span></div>
            <div class="ps-row"><label>地点</label><span>{{ lastPickup.location }}</span></div>
            <div class="ps-row"><label>时间</label><span>{{ pickedAt }}</span></div>
            <div class="ps-tear" />
          </div>
        </PanelCard>
      </div>
    </div>

    <!-- 待取包裹 -->
    <PanelCard v-if="result" :title="`${result.user.realName} 的在库包裹`" :note="`${restParcels.length} / ${parcels.length} 件待取`"
               icon="Box" accent="parcel" :index="4">
      <template #extra>
        <el-button v-if="restParcels.length" type="primary" size="small" :icon="Upload" :loading="bulk" @click="doBulkPickup">
          全部出库（{{ restParcels.length }}）
        </el-button>
      </template>

      <div v-if="parcels.length" class="pick-grid">
        <article v-for="p in parcels" :key="p.id" class="pick-card" :class="{ gone: picked.has(p.id) }">
          <div class="pk-top">
            <span class="pk-courier">{{ courierName(p.courierCompany) }}</span>
            <el-tag size="small" :type="dict(PARCEL_STATUS, p.status).type" effect="light">{{ dict(PARCEL_STATUS, p.status).label }}</el-tag>
          </div>
          <b class="pk-no ap-mono">{{ p.trackingNo }}</b>
          <div class="pk-meta">
            <span><el-icon :size="13"><OfficeBuilding /></el-icon>{{ p.stationName || '—' }}</span>
            <span><el-icon :size="13"><Grid /></el-icon>货架 {{ p.shelfCode || '—' }}</span>
            <span><el-icon :size="13"><Clock /></el-icon>{{ fmtTime(p.inboundTime) }}</span>
          </div>
          <div v-if="p.activeAuthId" class="pk-auth">
            <el-icon :size="13"><Promotion /></el-icon>存在生效代取授权（{{ p.activeAuthGrantee }}），条码出库将同步撤销
          </div>
          <el-button v-if="!picked.has(p.id)" type="success" :icon="Upload" :loading="acting === p.id"
                     style="width:100%" @click="doPickup(p)">确认出库</el-button>
          <div v-else class="pk-done"><el-icon><CircleCheckFilled /></el-icon>已出库 · 台账已留痕</div>
        </article>
      </div>
      <div v-else class="ap-empty">
        <el-empty description="该学生名下没有在库包裹" :image-size="80" />
        <p>可能已全部领取，或包裹收件手机号与账号手机号不一致。</p>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Postcard, Aim, Search, Delete, Clock, Upload } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { verifyIdentity, identityPickup } from '../../api'
import { IDENTITY_STATUS, PARCEL_STATUS, dict, fmtTime, courierName } from '../../utils/format'

const code = ref('')
const verifying = ref(false)
const result = ref(null)
const error = ref('')
const inputRef = ref(null)
const verifiedAt = ref('')
const pickedAt = ref('')
const lastPickup = ref(null)
const acting = ref(null)
const bulk = ref(false)
const picked = ref(new Set())
const session = ref([])
const showHistory = ref(false)

const parcels = computed(() => result.value?.parcels || [])
const restParcels = computed(() => parcels.value.filter(p => !picked.value.has(p.id)))
const sessionOut = computed(() => session.value.filter(s => s.out).reduce((n, s) => n + s.out, 0))
const heroStats = computed(() => [
  { label: '本次核验', value: session.value.filter(s => s.ok).length, unit: '次' },
  { label: '本次出库', value: sessionOut.value, unit: '件' },
  { label: '台面待取', value: restParcels.value.length, unit: '件' }
])

function clearResult() {
  result.value = null
  error.value = ''
  picked.value = new Set()
}
function resetAll() {
  code.value = ''
  clearResult()
  lastPickup.value = null
  session.value = []
  ElMessage.success('台面已清空')
  focusInput()
}
function focusInput() { nextTick(() => inputRef.value?.focus()) }
function stamp() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

async function doVerify() {
  const raw = code.value.trim().toUpperCase().replace(/\s+/g, '')
  if (!raw) { ElMessage.warning('请扫描或输入身份条码'); focusInput(); return }
  verifying.value = true
  error.value = ''
  try {
    const r = await verifyIdentity(raw)
    result.value = r
    picked.value = new Set()
    verifiedAt.value = `${fmtTime(new Date().toISOString())} ${stamp()}`
    session.value.unshift({ code: raw, name: r.user?.realName, ok: true, msg: `核验通过，${(r.parcels || []).length} 件在库`, at: stamp(), out: 0 })
    ElMessage.success(`身份核验通过：${r.user?.realName}（${r.user?.username}）`)
  } catch (e) {
    result.value = null
    error.value = e?.message || '条码无效或已失效'
    session.value.unshift({ code: raw, ok: false, msg: error.value, at: stamp(), out: 0 })
  } finally {
    verifying.value = false
  }
}

async function doPickup(p, silent = false) {
  acting.value = p.id
  try {
    const r = await identityPickup({ identityCode: result.value.identity.code, parcelId: p.id })
    p.status = 'PICKED_UP'
    picked.value = new Set([...picked.value, p.id])
    lastPickup.value = r
    pickedAt.value = `${fmtTime(new Date().toISOString())} ${stamp()}`
    const s = session.value.find(x => x.code === result.value.identity.code && x.ok)
    if (s) s.out = (s.out || 0) + 1
    if (!silent) ElMessage.success(`${p.trackingNo} 已出库，检查员 ${r.inspector}`)
    return true
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '出库失败')
    return false
  } finally { acting.value = null }
}

async function doBulkPickup() {
  const rest = restParcels.value
  if (!rest.length) return
  try {
    await ElMessageBox.confirm(`确定为 ${result.value.user.realName} 一次性出库 ${rest.length} 件包裹吗？每件都会独立记入去向台账。`, '批量出库', { type: 'warning' })
  } catch { return }
  bulk.value = true
  let ok = 0
  try {
    for (const p of rest) { if (await doPickup(p, true)) ok++ }
    ElMessage.success(`批量出库完成：成功 ${ok} 件${ok < rest.length ? `，失败 ${rest.length - ok} 件` : ''}`)
  } finally { bulk.value = false }
}

onMounted(focusInput)
</script>

<style scoped>
.side-col { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }

/* 扫码区 */
.scan-box {
  position: relative; overflow: hidden;
  padding: 22px 20px 18px;
  border-radius: 15px;
  border: 1.5px dashed color-mix(in srgb, #8b5cf6 42%, transparent);
  background:
    radial-gradient(600px 200px at 50% -20%, rgba(139, 92, 246, 0.12), transparent 70%),
    var(--art-gray-100);
  text-align: center;
  transition: border-color 0.3s, background 0.3s;
}
html.dark .scan-box { background: radial-gradient(600px 200px at 50% -20%, rgba(139, 92, 246, 0.14), transparent 70%), #1a1a20; }
.scan-box.ok { border-color: color-mix(in srgb, #10d9b5 55%, transparent); }
.scan-box.bad { border-color: color-mix(in srgb, #fb7185 55%, transparent); }
.scan-box::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(rgba(139, 92, 246, 0.16) 1px, transparent 1px);
  background-size: 10px 10px;
  -webkit-mask-image: radial-gradient(closest-side at 50% 0%, #000, transparent);
  mask-image: radial-gradient(closest-side at 50% 0%, #000, transparent);
}
.scan-laser {
  position: absolute; left: 8%; right: 8%; height: 2px; top: 20%;
  background: linear-gradient(90deg, transparent, #a78bfa, transparent);
  box-shadow: 0 0 12px #8b5cf6;
  opacity: 0; pointer-events: none;
}
.scan-box.scanning .scan-laser { opacity: 1; animation: laser 1.1s ease-in-out infinite; }
@keyframes laser { 0%, 100% { top: 18%; } 50% { top: 78%; } }
.scan-glyph { position: relative; color: var(--ap-identity); margin-bottom: 6px; }
.scan-label { position: relative; display: block; font-size: 11px; font-weight: 700; letter-spacing: 1.4px; color: var(--art-gray-500); text-transform: uppercase; margin-bottom: 9px; }
.scan-input { position: relative; max-width: 460px; margin: 0 auto; }
.scan-input :deep(.el-input__inner) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 19px; font-weight: 700; letter-spacing: 3px; text-align: center; height: 46px;
}
.scan-tip { position: relative; margin: 11px auto 0; max-width: 460px; font-size: 11.5px; line-height: 1.65; color: var(--art-gray-500); }
.scan-actions { position: relative; display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; margin-top: 13px; }

.session { margin-top: 16px; }
.s-h { margin: 0 0 8px; font-size: 11.5px; font-weight: 800; letter-spacing: 1.1px; color: var(--art-gray-500); text-transform: uppercase; }
.s-name { font-size: 12px; color: var(--art-gray-600); }

/* 核验结果 */
.vr-person { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.vr-avatar {
  width: 50px; height: 50px; border-radius: 15px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #6d28d9, #a78bfa);
  box-shadow: 0 10px 20px -10px rgba(139, 92, 246, 0.8);
}
.vr-info { min-width: 0; flex: 1; }
.vr-info b { display: block; font-size: 18px; font-weight: 800; color: var(--art-gray-900); }
.vr-info span { display: block; font-size: 12px; color: var(--art-gray-500); margin-top: 2px; }
.vr-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11.5px; font-weight: 700; color: #0d9488;
  background: rgba(16, 217, 181, 0.14);
  border: 1px solid rgba(16, 217, 181, 0.4);
  padding: 5px 11px; border-radius: 999px;
}
html.dark .vr-badge { color: #35e0c2; }
.vr-kv { margin-top: 15px; }

/* 出库凭证 */
.pickup-slip {
  position: relative;
  padding: 14px 16px 18px;
  border-radius: 12px;
  background: repeating-linear-gradient(180deg, var(--art-gray-100) 0 26px, color-mix(in srgb, var(--art-gray-100) 60%, var(--default-box-color)) 26px 27px);
  border: 1px dashed var(--default-border);
}
html.dark .pickup-slip { background: #1a1a20; }
.ps-row { display: flex; justify-content: space-between; gap: 12px; padding: 4px 0; font-size: 12.5px; }
.ps-row label { color: var(--art-gray-500); flex-shrink: 0; }
.ps-row span { color: var(--art-gray-900); font-weight: 600; text-align: right; word-break: break-all; }
.ps-tear {
  position: absolute; left: 0; right: 0; bottom: -1px; height: 8px;
  background: radial-gradient(circle at 6px -2px, transparent 6px, var(--default-box-color) 6px);
  background-size: 12px 8px;
}

/* 包裹卡片 */
.pick-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 13px; }
.pick-card {
  display: flex; flex-direction: column; gap: 8px;
  padding: 13px 14px;
  border: 1px solid var(--default-border);
  border-top: 3px solid var(--ap-parcel);
  border-radius: var(--art-radius-card-sm);
  background: var(--default-box-color);
  transition: all 0.3s var(--flair-ease);
}
.pick-card:hover { box-shadow: 0 14px 28px -20px rgba(30, 35, 80, 0.55); transform: translateY(-2px); }
.pick-card.gone { border-top-color: #10d9b5; opacity: 0.72; }
.pk-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.pk-courier { font-size: 12px; font-weight: 700; color: var(--ap-parcel); }
.pk-no { font-size: 14.5px; color: var(--art-gray-900); word-break: break-all; }
.pk-meta { display: flex; flex-direction: column; gap: 4px; font-size: 11.5px; color: var(--art-gray-600); }
.pk-meta span { display: inline-flex; align-items: center; gap: 5px; }
.pk-meta .el-icon { color: var(--art-gray-500); }
.pk-auth {
  display: flex; gap: 5px; align-items: flex-start;
  font-size: 11px; line-height: 1.55; color: #b45309;
  background: rgba(245, 158, 11, 0.11); border-radius: 8px; padding: 6px 9px;
}
html.dark .pk-auth { color: #ffc14d; }
.pk-done {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  font-size: 12.5px; font-weight: 700; color: #0d9488;
  background: rgba(16, 217, 181, 0.13); border-radius: 9px; padding: 9px;
}
html.dark .pk-done { color: #35e0c2; }

@media (max-width: 560px) {
  .scan-input :deep(.el-input__inner) { font-size: 16px; letter-spacing: 2px; }
  .scan-box { padding: 18px 13px 15px; }
}
@media (prefers-reduced-motion: reduce) {
  .scan-box.scanning .scan-laser { animation: none; opacity: 0.5; top: 48%; }
}
</style>