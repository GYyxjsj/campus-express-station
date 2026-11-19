<template>
  <div class="ap">
    <PageHero tone="staff" icon="Download" chip="INBOUND DESK" live title="入库工作台"
      sub="扫描运单号、匹配物主手机号、定位货架即可完成入库。系统自动生成取件码并向物主推送短信 / 站内 / 邮件三渠道到站提醒。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Upload" @click="$router.push('/staff/pickup')">取件核销台</el-button>
        <el-button type="default" :icon="Box" @click="$router.push('/staff/parcels')">包裹管理</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="inbound-grid">
      <PanelCard title="包裹入库" note="回车可快速提交" icon="Postcard" accent="staff" :index="4">
        <el-form ref="formRef" :model="form" :rules="rules" class="ap-form" label-position="top" @submit.prevent>
          <div class="ap-field-row">
            <label>运单号<em>*</em></label>
            <el-input v-model="form.trackingNo" class="scan-input" placeholder="扫描或输入运单号" clearable
                      :prefix-icon="Postcard" @keyup.enter="submit" />
          </div>
          <div class="ap-field-row">
            <label>快递公司<em>*</em></label>
            <el-select v-model="form.courierCompany" filterable allow-create default-first-option placeholder="选择或输入">
              <el-option v-for="c in COURIERS" :key="c" :label="c" :value="c" />
            </el-select>
            <div class="courier-chips">
              <button v-for="c in COURIERS.slice(0, 6)" :key="c" type="button"
                      :class="{ on: form.courierCompany === c }" @click="form.courierCompany = c">{{ c }}</button>
            </div>
          </div>
          <div class="ap-field-row">
            <label>物主手机号<em>*</em></label>
            <el-input v-model="form.ownerPhone" maxlength="11" placeholder="须与学生注册手机号一致" :prefix-icon="Iphone" />
            <div class="ap-form-tip">入库按手机号自动匹配学生账号；匹配失败会拒绝入库。</div>
          </div>
          <div class="form-duo">
            <div class="ap-field-row">
              <label>驿站<em>*</em></label>
              <el-select v-model="form.stationId" placeholder="选择驿站" @change="onStationChange">
                <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </div>
            <div class="ap-field-row">
              <label>货架<em>*</em></label>
              <el-select v-model="form.shelfId" placeholder="先选择驿站" :disabled="!form.stationId" :loading="shelfLoading">
                <el-option v-for="sh in shelves" :key="sh.id"
                           :label="`${sh.code}（${sh.occupied}/${sh.capacity}）`" :value="sh.id"
                           :disabled="sh.status !== 1 || sh.occupied >= sh.capacity" />
              </el-select>
            </div>
          </div>
          <div v-if="selectedShelf" class="shelf-preview">
            <div class="sp-head">
              <b class="ap-mono">{{ selectedShelf.code }}</b>
              <span>{{ selectedShelf.occupied }} / {{ selectedShelf.capacity }} 占用</span>
            </div>
            <div class="cap-bar"><i :style="{ width: pct(selectedShelf) + '%' }" :class="capClass(selectedShelf)" /></div>
            <span class="sp-tip">入库后占用 {{ selectedShelf.occupied + 1 }} / {{ selectedShelf.capacity }}，剩余 {{ selectedShelf.capacity - selectedShelf.occupied - 1 }} 位</span>
          </div>
          <div class="ap-field-row">
            <label>备注</label>
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit
                      placeholder="选填，如：大件 / 易碎 / 需冷藏" />
          </div>
          <el-button type="primary" size="large" style="width:100%" :icon="Download" :loading="loading" @click="submit">
            确认入库并发送通知
          </el-button>
        </el-form>
      </PanelCard>

      <div class="inbound-side">
        <PanelCard v-if="last" title="入库凭证" note="取件码可口头告知物主" icon="Ticket" accent="mint" :index="5">
          <div class="slip">
            <div class="slip-top">
              <b class="ap-mono">{{ last.trackingNo }}</b>
              <el-tag type="success" effect="dark" size="small">入库成功</el-tag>
            </div>
            <div class="slip-code">
              <label>取件码</label>
              <b class="ap-mono">{{ last.pickupCode }}</b>
              <el-button size="small" type="primary" plain :icon="DocumentCopy" @click="copyCode(last.pickupCode)">复制</el-button>
            </div>
            <div class="ap-kv">
              <div><label>物主</label><span>{{ last.ownerName }}（{{ last.ownerUsername }}）</span></div>
              <div><label>货架</label><span class="ap-mono">{{ last.stationName }} · {{ last.shelfCode }}</span></div>
              <div><label>入库时间</label><span class="ap-mono">{{ fmtTime(last.inboundTime) }}</span></div>
              <div><label>通知</label><span class="muted">短信 / 站内 / 邮件已推送</span></div>
            </div>
          </div>
        </PanelCard>

        <PanelCard title="最近入库" :note="`本次会话 ${recent.length} 件`" icon="Clock" accent="parcel" :index="6">
          <template #extra>
            <el-button v-if="recent.length" size="small" text type="danger" :icon="Delete" @click="recent = []">清空</el-button>
          </template>
          <div v-if="recent.length" class="ap-list">
            <div v-for="(r, i) in recent" :key="r.id" class="ap-list-item flair-in" :style="{ '--i': Math.min(i, 10) }">
              <div class="ap-list-avatar" :style="{ background: 'linear-gradient(140deg,#3b82f6,#38bdf8)' }">
                <el-icon :size="16"><Box /></el-icon>
              </div>
              <div class="ap-list-body">
                <div class="ap-list-l1">
                  <b class="ap-mono">{{ r.trackingNo }}</b>
                  <el-tag size="small" effect="plain" type="primary">{{ r.shelfCode }}</el-tag>
                </div>
                <div class="ap-list-l2">{{ r.ownerName }} · {{ fmtTime(r.inboundTime) }}</div>
              </div>
              <div class="ap-list-right">
                <button class="code-chip" title="点击复制取件码" @click="copyCode(r.pickupCode)">{{ r.pickupCode }}</button>
              </div>
            </div>
          </div>
          <div v-else class="ap-empty">
            <el-empty description="暂无入库记录" :image-size="76" />
            <p>完成第一笔入库后，凭证与取件码会显示在这里。</p>
          </div>
        </PanelCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Clock, Delete, DocumentCopy, Download, Iphone, Postcard, Ticket, Upload } from '@element-plus/icons-vue'
import { inboundParcel, listStations, listShelves } from '../../api'
import { COURIERS, fmtTime } from '../../utils/format'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const formRef = ref()
const loading = ref(false)
const shelfLoading = ref(false)
const stations = ref([])
const shelves = ref([])
const recent = ref([])
const last = ref(null)
const form = reactive({ trackingNo: '', courierCompany: '', ownerPhone: '', stationId: null, shelfId: null, remark: '' })
const rules = {
  trackingNo: [{ required: true, message: '请输入运单号', trigger: 'blur' }],
  courierCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  ownerPhone: [
    { required: true, message: '请输入物主手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  stationId: [{ required: true, message: '请选择驿站', trigger: 'change' }],
  shelfId: [{ required: true, message: '请选择货架', trigger: 'change' }]
}

const selectedShelf = computed(() => shelves.value.find(s => s.id === form.shelfId) || null)
const freeSlots = computed(() => shelves.value.filter(s => s.status === 1 && s.occupied < s.capacity).length)
const pct = sh => (sh.capacity ? Math.min(100, Math.round((sh.occupied / sh.capacity) * 100)) : 0)
const capClass = sh => (pct(sh) >= 90 ? 'hot' : pct(sh) >= 70 ? 'warm' : '')

const heroStats = computed(() => [
  { label: '本次入库', value: recent.value.length, unit: '件' },
  { label: '可选货架', value: freeSlots.value, unit: '个' },
  { label: '驿站数', value: stations.value.length, unit: '个' }
])
const metrics = computed(() => {
  const cap = shelves.value.reduce((n, s) => n + s.capacity, 0)
  const occ = shelves.value.reduce((n, s) => n + s.occupied, 0)
  return [
    { label: '本次会话入库', value: recent.value.length, unit: '件', icon: 'Download', color: '#3b82f6', foot: '含已推送通知' },
    { label: '当前驿站货架', value: shelves.value.length, unit: '个', icon: 'Grid', color: '#0ea5e9', foot: form.stationId ? stations.value.find(s => s.id === form.stationId)?.name : '未选择驿站' },
    { label: '货架空闲位', value: Math.max(0, cap - occ), unit: '位', icon: 'Box', color: '#10d9b5', foot: `总容量 ${cap} 位` },
    { label: '占用率', value: cap ? Math.round((occ / cap) * 100) : 0, unit: '%', icon: 'Odometer', color: '#f59e0b', foot: '当前驿站全部货架' }
  ]
})

async function onStationChange() {
  form.shelfId = null
  shelves.value = []
  if (!form.stationId) return
  shelfLoading.value = true
  try { shelves.value = await listShelves(form.stationId) } finally { shelfLoading.value = false }
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const vo = await inboundParcel({ ...form, remark: form.remark || undefined })
    recent.value = [vo, ...recent.value].slice(0, 20)
    last.value = vo
    ElMessage.success(`入库成功，取件码 ${vo.pickupCode}，已通知 ${vo.ownerName}`)
    form.trackingNo = ''
    form.ownerPhone = ''
    form.remark = ''
    await onStationChange()
  } finally { loading.value = false }
}

async function copyCode(code) {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success(`取件码 ${code} 已复制`)
  } catch { ElMessage.warning('复制失败，请手动记录') }
}

onMounted(async () => { stations.value = await listStations() })
</script>

<style scoped>
.inbound-grid { display: grid; grid-template-columns: minmax(340px, 430px) minmax(0, 1fr); gap: var(--ap-gap); align-items: start; }
.inbound-side { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.scan-input :deep(input) { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 16px; letter-spacing: 1.5px; font-weight: 700; }
.form-duo { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.courier-chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 7px; }
.courier-chips button {
  border: 1px solid var(--default-border); background: var(--art-card-bg, #fff); color: var(--art-gray-600);
  border-radius: 999px; padding: 3px 10px; font-size: 11.5px; cursor: pointer;
  transition: all 0.18s var(--flair-ease);
}
.courier-chips button:hover { border-color: #3b82f6; color: #3b82f6; transform: translateY(-1px); }
.courier-chips button.on { background: linear-gradient(120deg, #1d4ed8, #3b82f6); border-color: transparent; color: #fff; font-weight: 700; }
html.dark .courier-chips button { background: #1c1c22; }
.shelf-preview { border: 1px dashed var(--default-border); border-radius: 10px; padding: 10px 12px; margin-bottom: 15px; }
.sp-head { display: flex; justify-content: space-between; font-size: 12px; color: var(--art-gray-600); margin-bottom: 7px; }
.sp-head b { color: var(--art-gray-900); font-size: 13px; }
.cap-bar { height: 7px; border-radius: 999px; background: var(--art-gray-100); overflow: hidden; }
html.dark .cap-bar { background: #26262e; }
.cap-bar i { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #10d9b5, #38bdf8); transition: width 0.5s var(--flair-ease); }
.cap-bar i.warm { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.cap-bar i.hot { background: linear-gradient(90deg, #e11d48, #fb7185); }
.sp-tip { display: block; margin-top: 6px; font-size: 11px; color: var(--art-gray-500); }
.slip { display: flex; flex-direction: column; gap: 12px; }
.slip-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.slip-top b { font-size: 15px; color: var(--art-gray-900); }
.slip-code {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  border: 1px dashed #f59e0b; border-radius: 12px; padding: 12px 14px;
  background: linear-gradient(120deg, rgba(245, 158, 11, 0.12), rgba(251, 191, 36, 0.05));
}
.slip-code label { font-size: 11px; font-weight: 700; letter-spacing: 1px; color: var(--art-gray-600); }
.slip-code b { font-size: 30px; letter-spacing: 6px; color: #b45309; }
html.dark .slip-code b { color: #fbbf24; }
.code-chip {
  font-family: 'JetBrains Mono', Consolas, monospace; font-size: 14px; font-weight: 800; letter-spacing: 2px;
  color: #b45309; background: linear-gradient(120deg, #fff7e6, #ffedd5);
  border: 1px dashed #f59e0b; border-radius: 8px; padding: 3px 9px; cursor: pointer;
  transition: transform 0.2s var(--flair-ease);
}
.code-chip:hover { transform: translateY(-1px); }
html.dark .code-chip { color: #fbbf24; background: rgba(245, 158, 11, 0.12); }
@media (max-width: 1024px) { .inbound-grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .form-duo { grid-template-columns: 1fr; gap: 0; } }
</style>