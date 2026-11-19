<template>
  <div class="ap">
    <PageHero tone="identity" icon="Postcard" chip="IDENTITY BARCODE"
      title="身份条纹码"
      sub="Code 128 标准条纹码，柜台扫码即可核验身份并免取件码出库。条码与学号绑定，注销后立即失效。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="generating" @click="doGenerate">
          {{ active ? '重新生成' : '生成条码' }}
        </el-button>
        <el-button v-if="active" type="default" :icon="FullScreen" @click="openShow">出示模式</el-button>
      </template>
    </PageHero>

    <div class="ap-row wide-left">
      <!-- 电子身份卡 -->
      <PanelCard title="电子身份凭证" :note="active ? '生效中' : '未生成'" icon="Postcard" accent="identity" :index="1">
        <template #extra>
          <el-button v-if="active" size="small" text type="primary" :icon="Download" @click="downloadSvg">下载 SVG</el-button>
          <el-button v-if="active" size="small" text type="primary" :icon="DocumentCopy" @click="copyCode">复制条码</el-button>
        </template>

        <div v-if="active" class="idcard-wrap">
          <div class="idcard" :class="{ expired: isExpired }">
            <div class="idcard-shine" />
            <header class="idcard-head">
              <div class="idc-brand">
                <el-icon :size="17"><Box /></el-icon>
                <span>校园快递驿站</span>
              </div>
              <span class="idc-kind">ELECTRONIC IDENTITY</span>
            </header>
            <div class="idcard-person">
              <div class="idc-avatar">{{ (user.realName || 'U').slice(0, 1) }}</div>
              <div class="idc-info">
                <b>{{ user.realName || '—' }}</b>
                <span class="ap-mono">{{ user.username || '—' }}</span>
                <span class="idc-phone">{{ user.phone || '未绑定手机号' }}</span>
              </div>
              <div class="idc-stamp" :class="isExpired ? 'bad' : 'ok'">
                {{ isExpired ? '已过期' : '有效' }}
              </div>
            </div>
            <div class="idc-barcode">
              <BarcodeSvg ref="barcodeRef" :value="active.code" :scale="2" :bar-height="86" />
            </div>
            <footer class="idcard-foot">
              <div><label>条码编号</label><span class="ap-mono">{{ active.code }}</span></div>
              <div><label>有效期至</label><span>{{ fmtTime(active.expireAt) }}</span></div>
              <div><label>剩余</label><span>{{ countdown(active.expireAt) }}</span></div>
              <div><label>已核销</label><span>{{ active.useCount || 0 }} 次</span></div>
            </footer>
          </div>
          <p class="idcard-tip">
            <el-icon><InfoFilled /></el-icon>
            请在驿站柜台出示此条码，员工扫码核验后即可免取件码领取本人名下全部在库包裹。
          </p>
        </div>

        <div v-else class="ap-empty">
          <el-empty description="尚未生成身份条码" :image-size="86" />
          <p>生成后可在柜台扫码取件，无需逐个报取件码。有效期 {{ ov.validHours || 168 }} 小时。</p>
          <el-button type="primary" :icon="Refresh" :loading="generating" style="margin-top:12px" @click="doGenerate">
            立即生成条码
          </el-button>
        </div>
      </PanelCard>

      <div class="side-col">
        <PanelCard title="条码状态" icon="Odometer" accent="identity" :index="2">
          <div class="ap-kv">
            <div><label>当前状态</label>
              <span>
                <el-tag v-if="active" size="small" :type="isExpired ? 'warning' : 'success'" effect="light">
                  {{ isExpired ? '已过期' : '生效中' }}
                </el-tag>
                <el-tag v-else size="small" type="info" effect="light">未生成</el-tag>
              </span>
            </div>
            <div><label>在库待取</label><span class="ap-strong">{{ ov.inStock || 0 }} 件</span></div>
            <div><label>有效时长</label><span>{{ ov.validHours || 168 }} 小时</span></div>
            <div><label>最近核销</label><span class="muted">{{ active?.lastUsedAt ? fmtTime(active.lastUsedAt) : '暂无' }}</span></div>
          </div>
          <div v-if="active" class="state-actions">
            <el-button type="primary" plain :icon="Refresh" :loading="generating" @click="doGenerate">重新生成</el-button>
            <el-button type="danger" plain :icon="CircleClose" @click="doRevoke">注销条码</el-button>
          </div>
          <div class="ap-note" style="--nc:#8b5cf6;margin-top:14px">
            <el-icon><Lock /></el-icon>
            <div><b>安全提示：</b>重新生成会立即注销旧条码；条码等同取件凭证，请勿截图转发他人。</div>
          </div>
        </PanelCard>

        <PanelCard title="使用说明" icon="Reading" accent="slate" :index="3">
          <ol class="step-list">
            <li><b>生成条码</b>点击生成后获得 12 位 Code 128 条码，与本人学号唯一绑定。</li>
            <li><b>柜台出示</b>在「出示模式」下全屏展示，或使用扫码枪直接读取屏幕条纹。</li>
            <li><b>身份核验</b>员工扫码后系统返回姓名、学号与在库包裹清单，不泄露取件码。</li>
            <li><b>免码出库</b>核验通过后逐件确认出库，去向台账自动记录核验人与地点。</li>
            <li><b>到期失效</b>超过有效期自动失效，需重新生成；也可随时手动注销。</li>
          </ol>
        </PanelCard>
      </div>
    </div>

    <!-- 历史条码 -->
    <PanelCard title="历史条码" :note="`近 ${history.length} 条`" icon="Clock" accent="slate" :index="4">
      <div class="ap-table-wrap">
        <el-table :data="sortedHistory" stripe :default-sort="defaultSort" @sort-change="onSortChange">
          <el-table-column label="条码编号" prop="code" sortable="custom" min-width="180">
            <template #default="{ row }"><span class="ap-mono ap-strong">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column label="状态" prop="status" sortable="custom" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="dict(IDENTITY_STATUS, row.status).type" effect="light">{{ dict(IDENTITY_STATUS, row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生成时间" prop="createdAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="有效期至" prop="expireAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.expireAt) }}</span></template>
          </el-table-column>
          <el-table-column label="核销次数" prop="useCount" sortable="custom" width="100" align="right">
            <template #default="{ row }"><span class="ap-strong">{{ row.useCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="最近核销" prop="lastUsedAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ row.lastUsedAt ? fmtTime(row.lastUsedAt) : '—' }}</span></template>
          </el-table-column>
          <el-table-column label="注销时间" prop="revokedAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ row.revokedAt ? fmtTime(row.revokedAt) : '—' }}</span></template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in history" :key="row.id" class="ap-card-item"
             :style="{ '--ci': row.status === 'ACTIVE' ? '#10d9b5' : '#7987a1' }">
          <div class="ap-card-top">
            <b class="ap-mono">{{ row.code }}</b>
            <el-tag size="small" :type="dict(IDENTITY_STATUS, row.status).type" effect="light">{{ dict(IDENTITY_STATUS, row.status).label }}</el-tag>
          </div>
          <div class="ap-card-grid">
            <div><label>生成</label><span>{{ fmtTime(row.createdAt) }}</span></div>
            <div><label>有效期至</label><span>{{ fmtTime(row.expireAt) }}</span></div>
            <div><label>核销次数</label><span>{{ row.useCount || 0 }}</span></div>
            <div><label>最近核销</label><span>{{ row.lastUsedAt ? fmtTime(row.lastUsedAt) : '—' }}</span></div>
          </div>
        </div>
        <div v-if="!history.length" class="ap-empty"><el-empty description="暂无历史条码" :image-size="64" /></div>
      </div>
    </PanelCard>

    <!-- 出示模式：全屏铺满整个视口 -->
    <el-dialog v-model="showMode" fullscreen append-to-body class="show-dialog"
               :show-close="false" @opened="enterFs" @closed="exitFs">
      <template #header>
        <div class="show-head">
          <span class="show-head-title">柜台出示模式</span>
          <div class="show-head-ops">
            <el-button size="small" :icon="Download" @click="downloadSvg">下载 SVG</el-button>
            <el-button size="small" type="primary" @click="showMode = false">退出示模式</el-button>
          </div>
        </div>
      </template>
      <div v-if="active" class="show-mode" :class="{ portrait: isPortrait }">
        <div class="show-person">
          <b>{{ user.realName }}</b>
          <span class="ap-mono">{{ user.username }}</span>
        </div>
        <div class="show-stage" :style="stageStyle">
          <div class="show-barcode" :class="{ rot: isPortrait }" :style="barcodeStyle">
            <BarcodeSvg :value="active.code" :scale="3" :bar-height="210" />
          </div>
        </div>
        <div class="show-meta">
          <span>有效期至 {{ fmtTime(active.expireAt) }}</span>
          <span>剩余 {{ countdown(active.expireAt) }}</span>
        </div>
        <p class="show-tip">请将屏幕亮度调高，扫码枪对准条纹区域读取。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, FullScreen, Download, DocumentCopy, CircleClose } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import BarcodeSvg from '../../components/BarcodeSvg.vue'
import { myIdentity, generateIdentity, revokeIdentity } from '../../api'
import { IDENTITY_STATUS, dict, fmtTime, countdown } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const ov = ref({})
const active = computed(() => ov.value.active || null)
const history = computed(() => ov.value.history || [])
const { defaultSort, onSortChange, sorted } = useTableSort('createdAt', { useCount: r => r.useCount || 0 })
const sortedHistory = computed(() => sorted(history.value))
const user = computed(() => ov.value.user || {})
const generating = ref(false)
const showMode = ref(false)
const barcodeRef = ref(null)
const now = ref(Date.now())

const isExpired = computed(() => {
  if (!active.value) return false
  return new Date(String(active.value.expireAt).replace(' ', 'T')).getTime() <= now.value
})

const heroStats = computed(() => [
  { label: '在库待取', value: Number(ov.value.inStock || 0), unit: '件' },
  { label: '有效时长', value: Number(ov.value.validHours || 168), unit: '小时' },
  { label: '累计核销', value: history.value.reduce((s, r) => s + Number(r.useCount || 0), 0), unit: '次' }
])

async function load() { ov.value = await myIdentity() }

async function doGenerate() {
  if (active.value && !isExpired.value) {
    try {
      await ElMessageBox.confirm('重新生成会立即注销当前条码，旧条码将无法用于柜台核验。确定继续吗？', '重新生成条码', { type: 'warning' })
    } catch { return }
  }
  generating.value = true
  try {
    const code = await generateIdentity()
    ElMessage.success(`条码 ${code.code} 已生成`)
    await load()
  } finally { generating.value = false }
}

async function doRevoke() {
  if (!active.value) return
  try {
    await ElMessageBox.confirm(`确定注销条码 ${active.value.code} 吗？注销后需重新生成才能扫码取件。`, '注销条码', { type: 'warning' })
  } catch { return }
  await revokeIdentity(active.value.id)
  ElMessage.success('条码已注销')
  showMode.value = false
  await load()
}

function downloadSvg() {
  if (!active.value) return
  barcodeRef.value?.download(`identity-${active.value.code}.svg`)
  ElMessage.success('条码 SVG 已开始下载')
}
async function copyCode() {
  if (!active.value) return
  try {
    await navigator.clipboard.writeText(active.value.code)
    ElMessage.success('条码编号已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持自动复制，请手动选择条码编号')
  }
}

function openShow() { showMode.value = true }

/* 视口尺寸与横竖屏：竖屏时条码旋转 90° 沿屏幕长边最大化 */
const vw = ref(window.innerWidth)
const vh = ref(window.innerHeight)
const isPortrait = computed(() => vh.value > vw.value)
function onResize() { vw.value = window.innerWidth; vh.value = window.innerHeight }

/* 最大化几何：Code128 模块数 = 11×(字符数+3) + 13 + 20（起始/校验/终止/静区），scale=3；
   高 = barHeight 210 + 文本区 22 = 232。按实际条码长度算原始宽高比。
   横屏：宽 = 视口宽 - 边距，高按比例但受可用高度约束；
   竖屏：旋转 90° 后「视觉宽 = 原始高 × k、视觉高 = 原始宽 × k」，沿长边取最大 k。 */
const RAW_W = computed(() => (11 * (String(active.value?.code || '').length + 3) + 33) * 3)
const RAW_H = 232
const showBox = computed(() => {
  const rawW = RAW_W.value
  const pad = Math.max(16, Math.min(vw.value, vh.value) * 0.04)
  const headroom = isPortrait.value ? 150 : 300 // 顶栏 + 姓名/有效期/提示占位
  if (!isPortrait.value) {
    // 横向铺满：宽度吃满视口（扣边距与卡片内衬），高度按条码原始比例，但不超过可用高度
    let w = vw.value - pad * 2 - 24
    let h = w * (RAW_H / rawW)
    const maxH = Math.max(160, vh.value - headroom)
    if (h > maxH) { h = maxH; w = h * (rawW / RAW_H) }
    return { w: Math.round(w), h: Math.round(h), rotW: 0, rotH: 0 }
  }
  // 竖屏旋转：旋转后占位宽 = RAW_H*k（受屏宽约束），占位高 = RAW_W*k（受屏高约束）
  const kW = (vw.value - pad * 2 - 24) / RAW_H
  const kH = (vh.value - headroom) / rawW
  const k = Math.max(0.4, Math.min(kW, kH))
  const w = rawW * k
  const h = RAW_H * k
  return { w: Math.round(w), h: Math.round(h), rotW: Math.round(h), rotH: Math.round(w) }
})
const barcodeStyle = computed(() => {
  const b = showBox.value
  if (!isPortrait.value) return { width: b.w + 'px', height: b.h + 'px' }
  // 旋转后视觉占位为 rotW × rotH；SVG 本体仍 w × h，用绝对定位居中再转 90°
  return { width: b.w + 'px', height: b.h + 'px' }
})
const stageStyle = computed(() => {
  const b = showBox.value
  if (!isPortrait.value) return { width: b.w + 'px', height: b.h + 'px' }
  return { width: b.rotW + 'px', height: b.rotH + 'px' }
})

/* 对话框铺满视口的基础上，再尝试浏览器级全屏；竖屏时尝试锁定横向（仅部分移动浏览器支持，失败则用 CSS 旋转兜底） */
async function enterFs() {
  const el = document.documentElement
  try {
    if (!document.fullscreenElement && el.requestFullscreen) await el.requestFullscreen()
  } catch { /* 忽略：保持视口全屏 */ }
  try {
    if (screen.orientation && screen.orientation.lock) await screen.orientation.lock('landscape')
  } catch { /* 桌面浏览器/不支持时忽略，由 CSS 旋转兜底 */ }
  onResize()
}
function exitFs() {
  try { screen.orientation && screen.orientation.unlock && screen.orientation.unlock() } catch { /* 忽略 */ }
  if (document.fullscreenElement && document.exitFullscreen) {
    document.exitFullscreen().catch(() => {})
  }
}
/* 用户按 Esc 退出浏览器全屏时，同步关闭出示模式 */
function onFsChange() {
  onResize()
  if (!document.fullscreenElement && showMode.value) showMode.value = false
}

let tick = null
onMounted(async () => {
  await load()
  tick = setInterval(() => { now.value = Date.now() }, 30000)
  document.addEventListener('fullscreenchange', onFsChange)
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  clearInterval(tick)
  document.removeEventListener('fullscreenchange', onFsChange)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.side-col { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.ap-sub2 { font-size: 12px; color: var(--art-gray-600); }

/* ---- 电子身份卡 ---- */
.idcard-wrap { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.idcard {
  position: relative; overflow: hidden;
  width: 100%; max-width: 460px;
  border-radius: 18px;
  padding: 18px 20px 16px;
  color: #fff;
  background: linear-gradient(135deg, #3b1d8f 0%, #6d28d9 34%, #8b5cf6 66%, #a78bfa 100%);
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: 0 24px 50px -22px rgba(109, 40, 217, 0.75), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: transform 0.4s var(--flair-ease), box-shadow 0.4s;
}
.idcard:hover { transform: translateY(-4px) rotate(-0.4deg); box-shadow: 0 30px 60px -22px rgba(109, 40, 217, 0.85); }
.idcard::before {
  content: ''; position: absolute; width: 260px; height: 260px;
  right: -70px; top: -90px; pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.3) 1.5px, transparent 1.5px);
  background-size: 11px 11px;
  -webkit-mask-image: radial-gradient(closest-side, #000 22%, transparent 100%);
  mask-image: radial-gradient(closest-side, #000 22%, transparent 100%);
  transform: rotate(10deg);
}
.idcard.expired { filter: saturate(0.45); }
.idcard-shine {
  position: absolute; top: 0; bottom: 0; width: 60%;
  left: -70%; pointer-events: none;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  transform: skewX(-18deg);
  animation: idc-shine 5.5s ease-in-out infinite;
}
@keyframes idc-shine { 0%, 62% { left: -70%; } 100% { left: 130%; } }

.idcard-head { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.idc-brand { display: inline-flex; align-items: center; gap: 7px; font-size: 13.5px; font-weight: 700; letter-spacing: 0.6px; }
.idc-kind { font-size: 9.5px; font-weight: 700; letter-spacing: 1.6px; opacity: 0.72; }

.idcard-person { position: relative; display: flex; align-items: center; gap: 12px; margin: 15px 0 14px; }
.idc-avatar {
  width: 46px; height: 46px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.36);
  backdrop-filter: blur(6px);
}
.idc-info { min-width: 0; flex: 1; }
.idc-info b { display: block; font-size: 18px; font-weight: 800; letter-spacing: 0.5px; }
.idc-info span { display: block; font-size: 12px; opacity: 0.86; margin-top: 2px; }
.idc-phone { font-size: 11.5px !important; opacity: 0.7 !important; }
.idc-stamp {
  flex-shrink: 0;
  font-size: 11px; font-weight: 800; letter-spacing: 1.4px;
  padding: 5px 11px; border-radius: 999px;
  border: 1.5px dashed rgba(255, 255, 255, 0.6);
  transform: rotate(-6deg);
}
.idc-stamp.ok { background: rgba(16, 217, 181, 0.28); border-color: rgba(126, 247, 212, 0.85); }
.idc-stamp.bad { background: rgba(251, 113, 133, 0.3); border-color: rgba(255, 180, 195, 0.85); }

.idc-barcode {
  position: relative;
  background: #fff; border-radius: 12px;
  padding: 12px 12px 8px;
  box-shadow: 0 10px 24px -14px rgba(20, 10, 50, 0.7);
}
.idcard-foot {
  position: relative;
  display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 96px), 1fr));
  gap: 8px 12px; margin-top: 14px;
}
.idcard-foot label { display: block; font-size: 9.5px; letter-spacing: 1px; opacity: 0.66; text-transform: uppercase; }
.idcard-foot span { display: block; font-size: 11.5px; font-weight: 600; margin-top: 2px; word-break: break-all; }
.idcard-tip {
  display: flex; gap: 7px; align-items: flex-start;
  font-size: 12px; line-height: 1.65; color: var(--art-gray-600);
  max-width: 460px; margin: 0;
}
.idcard-tip .el-icon { color: var(--ap-identity); margin-top: 2px; flex-shrink: 0; }

.state-actions { display: flex; gap: 8px; margin-top: 15px; flex-wrap: wrap; }
.state-actions .el-button { flex: 1; }

.step-list { margin: 0; padding-left: 18px; display: flex; flex-direction: column; gap: 9px; }
.step-list li { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
.step-list b { color: var(--art-gray-900); margin-right: 5px; }

/* ---- 出示模式（全屏最大化） ---- */
.show-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.show-head-title { font-size: 15px; font-weight: 800; letter-spacing: 1px; color: var(--art-gray-900); }
.show-head-ops { display: flex; gap: 8px; }
.show-mode {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: clamp(10px, 2vh, 20px);
  min-height: calc(100vh - 120px);
  padding: 8px 12px;
}
.show-person { text-align: center; }
.show-person b { display: block; font-size: clamp(20px, 3vw, 30px); font-weight: 800; color: var(--art-gray-900); }
.show-person span { display: block; font-size: clamp(13px, 1.5vw, 17px); color: var(--art-gray-500); margin-top: 4px; letter-spacing: 2px; }

/* 舞台：JS 按视口算出的最大占位尺寸；竖屏时为旋转后的宽×高 */
.show-stage { position: relative; flex-shrink: 0; }
.show-barcode {
  width: 100%; height: 100%;
  background: #fff; border-radius: 14px;
  padding: clamp(8px, 1.5vh, 18px);
  border: 1px solid var(--default-border);
  box-shadow: 0 24px 60px -30px rgba(10, 10, 30, 0.45);
  display: flex; align-items: center; justify-content: center;
}
.show-barcode :deep(svg) { width: 100%; height: 100%; }
/* 竖屏：条码整体旋转 90°，沿屏幕长边最大化；本体宽高由 JS 给定（未旋转尺寸），
   绝对定位到舞台中心后旋转，占位尺寸即舞台的 rotW × rotH */
.show-barcode.rot {
  position: absolute; left: 50%; top: 50%;
  transform: translate(-50%, -50%) rotate(90deg);
  transform-origin: center center;
}
.show-mode.portrait { gap: 8px; min-height: calc(100vh - 110px); }
.show-mode.portrait .show-person b { font-size: 17px; }
.show-mode.portrait .show-person span { font-size: 12px; margin-top: 2px; }
.show-mode.portrait .show-meta { font-size: 11.5px; gap: 12px; }
.show-mode.portrait .show-tip { font-size: 11px; }

.show-meta { display: flex; gap: 22px; flex-wrap: wrap; justify-content: center; font-size: clamp(12px, 1.3vw, 15px); color: var(--art-gray-600); }
.show-tip { font-size: clamp(11px, 1.2vw, 14px); color: var(--art-gray-500); margin: 0; text-align: center; }

@media (max-width: 560px) {
  .idcard { padding: 15px 14px 13px; }
  .idc-info b { font-size: 16px; }
  .idc-kind { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .idcard-shine { animation: none; display: none; }
  .idcard { transition: none; }
}
</style>