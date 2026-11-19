<template>
  <div class="ap">
    <PageHero tone="staff" icon="Upload" chip="PICKUP DESK" live title="取件核销台"
      sub="本人取件凭包裹 + 取件码核销；代取核销需授权码与取件码双因子匹配。核销即出库，检查员、时间与货架地点自动写入去向台账。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Aim" @click="$router.push('/staff/identity')">身份条码核验</el-button>
        <el-button type="default" :icon="Guide" @click="$router.push('/staff/traces')">去向台账</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="pickup-grid">
      <PanelCard title="核销操作" note="回车提交" icon="Aim" accent="staff" :index="4">
        <div class="ap-seg mode-seg">
          <button :class="{ on: tab === 'self' }" @click="tab = 'self'"><el-icon :size="13"><User /></el-icon>本人取件</button>
          <button :class="{ on: tab === 'proxy' }" @click="tab = 'proxy'"><el-icon :size="13"><Share /></el-icon>代取核销</button>
        </div>

        <el-form v-if="tab === 'self'" class="ap-form" label-position="top" @submit.prevent>
          <div class="ap-field-row">
            <label>包裹<em>*</em></label>
            <el-select v-model="selfForm.parcelId" filterable remote clearable allow-create default-first-option
                       :remote-method="searchParcel"
                       :loading="searching" placeholder="输入运单号搜索 / 扫码枪扫描 / 粘贴包裹 ID">
              <el-option v-for="p in parcelOptions" :key="p.id" :label="`${p.trackingNo} · ${p.ownerName}`" :value="p.id">
                <div class="opt-row">
                  <b class="ap-mono">{{ p.trackingNo }}</b>
                  <span>{{ p.ownerName }} · {{ p.shelfCode || '未上架' }}</span>
                </div>
              </el-option>
            </el-select>
            <div class="ap-form-tip">支持扫码枪直接扫描输入框；也可点击下方「摄像头扫码」对准面单条码自动识别。</div>
          </div>

          <div class="scan-block">
            <el-button v-if="!scanning" type="primary" plain :icon="Camera" style="width:100%" @click="startScan">
              摄像头扫码识别面单
            </el-button>
            <template v-else>
              <div class="scan-view">
                <video ref="videoEl" autoplay playsinline muted></video>
                <div class="scan-line"></div>
                <span class="scan-tip">对准快递面单条码，识别后自动填入</span>
              </div>
              <el-button type="danger" plain :icon="VideoPause" style="width:100%" @click="stopScan">停止扫码</el-button>
            </template>
            <div v-if="scanHint" class="ap-form-tip">{{ scanHint }}</div>
          </div>

          <div class="ap-field-row">
            <label>取件码<em>*</em></label>
            <el-input v-model="selfForm.pickupCode" maxlength="6" class="code-input" placeholder="6 位数字取件码"
                      :prefix-icon="Key" @keyup.enter="doPickup" />
          </div>
          <el-button type="primary" size="large" style="width:100%" :icon="Upload" :loading="loading" @click="doPickup">
            核销出库（本人）
          </el-button>
        </el-form>

        <el-form v-else class="ap-form" label-position="top" @submit.prevent>
          <div class="ap-field-row">
            <label>授权码<em>*</em></label>
            <el-input v-model="proxyForm.authCode" class="code-input" placeholder="代取人出示的授权码"
                      :prefix-icon="Share" @keyup.enter="doProxyPickup" />
          </div>
          <div class="ap-field-row">
            <label>取件码<em>*</em></label>
            <el-input v-model="proxyForm.pickupCode" maxlength="6" class="code-input" placeholder="物主提供的 6 位取件码"
                      :prefix-icon="Key" @keyup.enter="doProxyPickup" />
          </div>
          <div class="ap-note" style="--nc:#f59e0b">
            <el-icon><Warning /></el-icon>
            <span>双因子校验：授权须处于生效期，且授权码与取件码同时匹配；核销成功后授权自动置为已使用。</span>
          </div>
          <el-button type="success" size="large" style="width:100%" :icon="CircleCheck" :loading="loading" @click="doProxyPickup">
            核销出库（代取）
          </el-button>
        </el-form>
      </PanelCard>

      <div class="pickup-side">
        <PanelCard v-if="last" title="出库凭证" note="已写入去向台账" icon="Ticket" accent="mint" :index="5">
          <div class="slip">
            <div class="slip-top">
              <b class="ap-mono">{{ last.trackingNo }}</b>
              <el-tag :type="last.pickupType === 'PROXY' ? 'success' : 'primary'" effect="dark" size="small">
                {{ last.pickupType === 'PROXY' ? '代取核销' : '本人取件' }}
              </el-tag>
            </div>
            <div class="ap-kv">
              <div><label>物主</label><span>{{ last.ownerName }}（{{ last.ownerUsername }}）</span></div>
              <div><label>取件人</label><span>{{ last.pickerName }}</span></div>
              <div><label>检查员</label><span>{{ last.inspector || store.user?.realName }}</span></div>
              <div><label>核销时间</label><span class="ap-mono">{{ last.time }}</span></div>
            </div>
          </div>
        </PanelCard>

        <PanelCard title="本次会话核销" :note="`${recent.length} 笔`" icon="Clock" accent="parcel" :index="6">
          <template #extra>
            <el-button v-if="recent.length" size="small" text type="danger" :icon="Delete" @click="recent = []">清空</el-button>
          </template>
          <div v-if="recent.length" class="ap-list">
            <div v-for="(r, i) in recent" :key="i" class="ap-list-item flair-in" :style="{ '--i': Math.min(i, 10) }">
              <div class="ap-list-avatar" :style="{ background: r.pickupType === 'PROXY' ? 'linear-gradient(140deg,#0d9488,#34d399)' : 'linear-gradient(140deg,#3b82f6,#38bdf8)' }">
                <el-icon :size="16"><component :is="r.pickupType === 'PROXY' ? 'Share' : 'User'" /></el-icon>
              </div>
              <div class="ap-list-body">
                <div class="ap-list-l1">
                  <b class="ap-mono">{{ r.trackingNo }}</b>
                  <el-tag size="small" :type="r.pickupType === 'PROXY' ? 'success' : 'primary'" effect="plain">
                    {{ r.pickupType === 'PROXY' ? '代取' : '本人' }}
                  </el-tag>
                </div>
                <div class="ap-list-l2">物主 {{ r.ownerName }} · 取件人 {{ r.pickerName }} · {{ r.time }}</div>
              </div>
            </div>
          </div>
          <div v-else class="ap-empty">
            <el-empty description="暂无核销记录" :image-size="76" />
            <p>核销成功后凭证与台账留痕会显示在这里。</p>
          </div>
        </PanelCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Camera, CircleCheck, Clock, Delete, Guide, Key, Share, Ticket, Upload, User, VideoPause, Warning } from '@element-plus/icons-vue'
import { pickup, proxyPickup, pageParcels } from '../../api'
import { fmtTime } from '../../utils/format'
import { useUserStore } from '../../stores/user'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const store = useUserStore()
const tab = ref('self')
const loading = ref(false)
const searching = ref(false)
const parcelOptions = ref([])
const recent = ref([])
const last = ref(null)
const selfForm = reactive({ parcelId: null, pickupCode: '' })
const proxyForm = reactive({ authCode: '', pickupCode: '' })

const heroStats = computed(() => [
  { label: '本次核销', value: recent.value.length, unit: '笔' },
  { label: '其中代取', value: recent.value.filter(r => r.pickupType === 'PROXY').length, unit: '笔' },
  { label: '本人取件', value: recent.value.filter(r => r.pickupType !== 'PROXY').length, unit: '笔' }
])
const metrics = computed(() => [
  { label: '会话核销总量', value: recent.value.length, unit: '笔', icon: 'Upload', color: '#3b82f6', foot: '核销即出库留痕' },
  { label: '本人取件', value: recent.value.filter(r => r.pickupType !== 'PROXY').length, unit: '笔', icon: 'User', color: '#0ea5e9', foot: '包裹 + 取件码' },
  { label: '代取核销', value: recent.value.filter(r => r.pickupType === 'PROXY').length, unit: '笔', icon: 'Share', color: '#10d9b5', foot: '授权码 + 取件码双因子' },
  { label: '代取占比', value: recent.value.length ? Math.round(recent.value.filter(r => r.pickupType === 'PROXY').length / recent.value.length * 100) : 0, unit: '%', icon: 'Aim', color: '#8b5cf6', foot: '免码出库请走身份核验台' }
])

async function searchParcel(kw) {
  if (!kw || kw.trim().length < 3) { parcelOptions.value = []; return }
  searching.value = true
  try {
    const data = await pageParcels({ page: 1, size: 12, keyword: kw.trim(), status: 'IN_STOCK' })
    parcelOptions.value = data.records
  } finally { searching.value = false }
}

function addRecent(r) {
  const item = { ...r, time: fmtTime(new Date()) }
  recent.value = [item, ...recent.value].slice(0, 30)
  last.value = item
}

async function doPickup() {
  if (!selfForm.parcelId || !selfForm.pickupCode) return ElMessage.warning('请选择包裹并填写取件码')
  loading.value = true
  try {
    const r = await pickup({ ...selfForm })
    addRecent(r)
    ElMessage.success(`核销成功：${r.trackingNo}（${r.ownerName}）`)
    selfForm.parcelId = null
    selfForm.pickupCode = ''
    parcelOptions.value = []
  } finally { loading.value = false }
}

async function doProxyPickup() {
  if (!proxyForm.authCode || !proxyForm.pickupCode) return ElMessage.warning('请填写授权码和取件码')
  await ElMessageBox.confirm('确认代取人身份与授权码无误后核销？核销即出库并留痕。', '代取核销确认', { type: 'warning' })
  loading.value = true
  try {
    const r = await proxyPickup({ ...proxyForm })
    addRecent(r)
    ElMessage.success(`代取核销成功：${r.trackingNo}，取件人 ${r.pickerName}`)
    proxyForm.authCode = ''
    proxyForm.pickupCode = ''
  } finally { loading.value = false }
}

/* ---- 摄像头扫码：BarcodeDetector 识别面单条码，失败/不支持降级手动输入 ---- */
const scanning = ref(false)
const scanHint = ref('')
const videoEl = ref(null)
let mediaStream = null
let scanTimer = null
let detector = null

async function startScan() {
  scanHint.value = ''
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    scanHint.value = '当前环境不支持摄像头（需 HTTPS 或 localhost），请使用扫码枪或手动输入运单号。'
    return ElMessage.warning(scanHint.value)
  }
  if (!('BarcodeDetector' in window)) {
    scanHint.value = '当前浏览器不支持条码识别（推荐 Chrome / Edge），请使用扫码枪或手动输入运单号。'
    return ElMessage.warning(scanHint.value)
  }
  try {
    const formats = await window.BarcodeDetector.getSupportedFormats()
    const want = ['code_128', 'code_39', 'ean_13', 'ean_8', 'itf', 'qr_code'].filter(f => formats.includes(f))
    if (!want.length) throw new Error('no-format')
    detector = new window.BarcodeDetector({ formats: want })
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false
    })
    scanning.value = true
    await nextTick()
    if (videoEl.value) {
      videoEl.value.srcObject = mediaStream
      await videoEl.value.play().catch(() => {})
    }
    scanTimer = setInterval(detectFrame, 350)
    ElMessage.success('摄像头已开启，对准面单条码')
  } catch (e) {
    stopScan()
    scanHint.value = e && e.name === 'NotAllowedError'
      ? '摄像头权限被拒绝，请在浏览器地址栏允许摄像头后重试，或改用扫码枪 / 手动输入。'
      : '摄像头开启失败，请检查设备占用情况，或改用扫码枪 / 手动输入运单号。'
    ElMessage.warning(scanHint.value)
  }
}

async function detectFrame() {
  if (!detector || !videoEl.value || videoEl.value.readyState < 2) return
  try {
    const codes = await detector.detect(videoEl.value)
    if (codes.length && codes[0].rawValue) onScanned(String(codes[0].rawValue).trim())
  } catch { /* 单帧识别失败忽略，下一帧继续 */ }
}

async function onScanned(no) {
  stopScan()
  if (!no) return
  ElMessage.success(`识别到条码：${no}`)
  searching.value = true
  try {
    const data = await pageParcels({ page: 1, size: 12, keyword: no, status: 'IN_STOCK' })
    parcelOptions.value = data.records
    const hit = data.records.find(p => p.trackingNo === no)
    if (hit) {
      selfForm.parcelId = hit.id
      ElMessage.success(`已选中包裹：${hit.trackingNo}（${hit.ownerName}），请输入取件码核销`)
    } else if (data.records.length === 1) {
      selfForm.parcelId = data.records[0].id
    } else {
      scanHint.value = data.records.length
        ? `「${no}」匹配到 ${data.records.length} 个在库包裹，请在下拉中确认选择。`
        : `未找到运单「${no}」的在库包裹，请核对条码或手动搜索。`
    }
  } finally { searching.value = false }
}

function stopScan() {
  if (scanTimer) { clearInterval(scanTimer); scanTimer = null }
  if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
  detector = null
  scanning.value = false
}

watch(tab, v => { if (v !== 'self') stopScan() })
onBeforeUnmount(stopScan)
</script>

<style scoped>
.pickup-grid { display: grid; grid-template-columns: minmax(340px, 430px) minmax(0, 1fr); gap: var(--ap-gap); align-items: start; }
.pickup-side { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.mode-seg { margin-bottom: 14px; }
.mode-seg button { display: inline-flex; align-items: center; gap: 6px; }
.code-input :deep(input) { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 18px; letter-spacing: 3px; font-weight: 700; }
.opt-row { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.opt-row span { font-size: 11.5px; color: var(--art-gray-500); }
.slip { display: flex; flex-direction: column; gap: 12px; }
.slip-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.slip-top b { font-size: 15px; color: var(--art-gray-900); }
.ap-note { margin-bottom: 14px; }
.scan-block { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
.scan-view {
  position: relative; border-radius: 12px; overflow: hidden; background: #0b0d12;
  border: 1px solid var(--default-border); aspect-ratio: 4 / 3;
}
.scan-view video { width: 100%; height: 100%; object-fit: cover; display: block; }
.scan-line {
  position: absolute; left: 8%; right: 8%; height: 2px; top: 20%;
  background: linear-gradient(90deg, transparent, #34d399, transparent);
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.8);
  animation: scan-sweep 2.2s ease-in-out infinite;
}
@keyframes scan-sweep { 0%, 100% { top: 18%; } 50% { top: 78%; } }
.scan-tip {
  position: absolute; left: 0; right: 0; bottom: 8px; text-align: center;
  font-size: 11.5px; color: rgba(255, 255, 255, 0.85); letter-spacing: 0.4px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}
@media (max-width: 1024px) { .pickup-grid { grid-template-columns: 1fr; } }
</style>