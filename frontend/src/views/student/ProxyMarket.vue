<template>
  <div class="ap">
    <PageHero tone="proxy" icon="Bicycle" chip="PROXY MARKET" live
      title="代取服务大厅"
      sub="同学发布付费代取需求，跑腿员接单后凭系统自动生成的授权码到柜台取件，送达确认后余额自动结算。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Plus" @click="openPublish">发布代取</el-button>
        <el-button type="default" :icon="Tickets" @click="$router.push('/student/proxy-orders')">我的服务单</el-button>
      </template>
    </PageHero>

    <div class="ap-note" style="--nc:#0ea5e9">
      <el-icon><InfoFilled /></el-icon>
      <div>
        <b>计费规则：</b>服务费 {{ money(rules.feeMin, 0) }}–{{ money(rules.feeMax, 0) }} 元，小费上限 {{ money(rules.maxTip, 0) }} 元；
        平台按总额 {{ rules.commissionPercent ?? 10 }}% 收取服务费，其余归跑腿员；接单时限最长 {{ rules.maxHours ?? 48 }} 小时，超时未接单自动全额退款。
      </div>
    </div>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="ap-toolbar">
      <div class="ap-field">
        <label>驿站</label>
        <el-select v-model="stationId" placeholder="全部驿站" clearable style="width:190px" @change="reload">
          <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </div>
      <div class="ap-field ap-grow">
        <label>搜索</label>
        <el-input v-model="keyword" placeholder="运单号 / 送达地址 / 备注" clearable :prefix-icon="Search" @keyup.enter="reload" @clear="reload" />
      </div>
      <div class="ap-actions">
        <el-button :icon="Search" type="primary" @click="reload">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <PanelCard title="待接单需求" :note="`${total} 单`" icon="ShoppingBag" accent="proxy" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="loadHall">刷新</el-button>
      </template>

      <div v-loading="loading" class="market-grid">
        <article v-for="(o, i) in rows" :key="o.id" class="market-card flair-in" :style="{ '--i': Math.min(i, 10) }">
          <div class="mc-top">
            <span class="mc-courier">{{ courierName(o.courierCompany) }}</span>
            <el-tag size="small" :type="dict(PARCEL_STATUS, o.parcelStatus).type" effect="light">
              {{ dict(PARCEL_STATUS, o.parcelStatus).label }}
            </el-tag>
            <span class="mc-no ap-mono">{{ o.trackingNo }}</span>
          </div>

          <div class="mc-route">
            <div class="mc-point from">
              <el-icon :size="14"><OfficeBuilding /></el-icon>
              <div>
                <b>{{ o.stationName || '驿站' }}</b>
                <span>{{ o.shelfCode ? `货架 ${o.shelfCode}` : (o.stationLocation || '柜台领取') }}</span>
              </div>
            </div>
            <div class="mc-arrow"><el-icon :size="15"><Right /></el-icon></div>
            <div class="mc-point to">
              <el-icon :size="14"><LocationInformation /></el-icon>
              <div>
                <b>{{ o.deliverTo || '未填写送达地址' }}</b>
                <span>委托人 {{ o.requesterName || '匿名同学' }}</span>
              </div>
            </div>
          </div>

          <p v-if="o.note" class="mc-note">{{ o.note }}</p>

          <div class="mc-money">
            <div class="mc-total">
              <span>跑腿实得</span>
              <b class="ap-money bare">{{ money(o.runnerIncome) }}</b>
            </div>
            <div class="mc-split">
              <span>服务费 {{ money(o.fee) }}</span>
              <span v-if="Number(o.tip)">+ 小费 {{ money(o.tip) }}</span>
              <span class="mc-comm">平台 {{ rules.commissionPercent ?? 10 }}%</span>
            </div>
          </div>

          <div class="mc-foot">
            <span class="mc-expire" :class="{ urgent: urgent(o.expireAt) }">
              <el-icon :size="13"><AlarmClock /></el-icon>{{ countdown(o.expireAt) }}
            </span>
            <el-button type="primary" size="small" :icon="Bicycle" :loading="acting === o.id"
                       :disabled="o.requesterId === myId" @click="doAccept(o)">
              {{ o.requesterId === myId ? '我发布的' : '立即接单' }}
            </el-button>
          </div>
        </article>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="暂时没有待接单的代取需求" :image-size="86" />
        <p>可以稍后刷新，或自己发布一单让同学帮忙取件。</p>
        <el-button type="primary" :icon="Plus" style="margin-top:12px" @click="openPublish">发布代取需求</el-button>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="page" :page-size="size" @current-change="loadHall" />
      </div>
    </PanelCard>

    <!-- 发布代取 -->
    <el-dialog v-model="publish" title="发布代取需求" width="620px" :close-on-click-modal="false" append-to-body>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="98px" class="ap-form">
        <el-form-item label="选择包裹" prop="parcelId">
          <el-select v-model="form.parcelId" placeholder="仅可选择本人在库 / 滞留包裹" filterable style="width:100%"
                     :loading="parcelLoading" @change="onParcelChange">
            <el-option v-for="p in myStock" :key="p.id" :label="`${p.trackingNo} · ${courierName(p.courierCompany)}`"
                       :value="p.id" :disabled="busy.has(p.id)">
              <span class="ap-mono">{{ p.trackingNo }}</span>
              <span style="margin-left:8px;color:var(--art-gray-500);font-size:12px">
                {{ courierName(p.courierCompany) }} · {{ p.stationName || '' }}{{ p.shelfCode ? ' · ' + p.shelfCode : '' }}
              </span>
              <el-tag v-if="busy.has(p.id)" size="small" type="info" style="margin-left:8px">已有进行中服务</el-tag>
            </el-option>
          </el-select>
          <div class="ap-form-tip">已存在生效授权或进行中服务单的包裹不可重复发布。</div>
        </el-form-item>

        <div v-if="selectedParcel" class="parcel-preview">
          <div><label>所在驿站</label><span>{{ selectedParcel.stationName || '—' }}</span></div>
          <div><label>货架</label><span>{{ selectedParcel.shelfCode || '—' }}</span></div>
          <div><label>入库时间</label><span>{{ fmtTime(selectedParcel.inboundTime) }}</span></div>
          <div><label>包裹状态</label><span>{{ dict(PARCEL_STATUS, selectedParcel.status).label }}</span></div>
        </div>

        <el-form-item label="服务费" prop="fee">
          <div class="fee-row">
            <el-slider v-model="form.fee" :min="Number(rules.feeMin || 2)" :max="Number(rules.feeMax || 50)"
                       :step="0.5" :marks="feeMarks" style="flex:1;min-width:150px" />
            <el-input-number v-model="form.fee" :min="Number(rules.feeMin || 2)" :max="Number(rules.feeMax || 50)"
                             :precision="2" :step="0.5" controls-position="right" style="width:132px" />
          </div>
        </el-form-item>

        <el-form-item label="额外小费">
          <el-input-number v-model="form.tip" :min="0" :max="Number(rules.maxTip || 50)" :precision="2" :step="1"
                           controls-position="right" style="width:132px" />
          <span class="ap-form-tip" style="margin-left:10px">小费全额归跑腿员，可提升接单速度。</span>
        </el-form-item>

        <el-form-item label="送达地址" prop="deliverTo">
          <el-input v-model="form.deliverTo" placeholder="如：东区学生公寓 3 号楼 512 室" maxlength="120" show-word-limit />
        </el-form-item>

        <el-form-item label="接单时限">
          <el-select v-model="form.hours" style="width:170px">
            <el-option v-for="h in hourOptions" :key="h" :label="`${h} 小时`" :value="h" />
          </el-select>
          <span class="ap-form-tip" style="margin-left:10px">超时无人接单自动取消并全额退款。</span>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="如：包裹较重 / 请放宿舍门口 / 易碎品轻拿轻放" />
        </el-form-item>

        <div class="escrow-box">
          <div class="eb-line"><span>服务费</span><b>{{ money(form.fee) }}</b></div>
          <div class="eb-line"><span>小费</span><b>{{ money(form.tip) }}</b></div>
          <div class="eb-line total"><span>托管扣款</span><b>{{ money(totalFee) }}</b></div>
          <div class="eb-split">
            <span>平台服务费 {{ rules.commissionPercent ?? 10 }}%：{{ money(commission) }}</span>
            <span>跑腿员实得：{{ money(runnerGets) }}</span>
          </div>
          <div class="eb-balance" :class="{ lack: balance < totalFee }">
            <el-icon><Wallet /></el-icon>
            当前余额 {{ money(balance) }}
            <template v-if="balance < totalFee">
              · 余额不足，请先<router-link class="ap-link" to="/student/wallet">充值</router-link>
            </template>
            <template v-else>· 发布后立即托管，完成后结算</template>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="publish = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="balance < totalFee" @click="doPublish">
          确认发布并托管 {{ money(totalFee) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Tickets, Search, Refresh, Bicycle } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import MetricTile from '../../components/MetricTile.vue'
import {
  proxyHall, proxyRules, acceptProxyOrder, createProxyOrder,
  myParcels, myProxyRequests, listStations, walletOverview
} from '../../api'
import { useUserStore } from '../../stores/user'
import { PARCEL_STATUS, ORDER_STATUS, dict, fmtTime, money, countdown, courierName } from '../../utils/format'

const router = useRouter()
const store = useUserStore()
const myId = computed(() => store.user?.id)

const rules = ref({ feeMin: 2, feeMax: 50, commissionPercent: 10, maxHours: 48, maxTip: 50 })
const stations = ref([])
const stationId = ref(null)
const keyword = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 9
const loading = ref(false)
const acting = ref(null)
const balance = ref(0)

const heroStats = computed(() => [
  { label: '待接单', value: total.value, unit: '单' },
  { label: '平台累计完成', value: Number(rules.value.doneCount || 0), unit: '单' },
  { label: '我的余额', value: balance.value, decimals: 2, unit: '元' }
])
const metrics = computed(() => [
  { label: '服务费区间', value: Number(rules.value.feeMax || 50), unit: '元上限', color: '#0ea5e9', icon: 'Money', foot: `下限 ${money(rules.value.feeMin, 0)} · 可叠加小费` },
  { label: '平台抽成', value: Number(rules.value.commissionPercent || 10), unit: '%', color: '#8b5cf6', icon: 'PieChart', foot: '按托管总额计算' },
  { label: '最长接单时限', value: Number(rules.value.maxHours || 48), unit: '小时', color: '#f59e0b', icon: 'AlarmClock', foot: '超时自动全额退款' },
  { label: '小费上限', value: Number(rules.value.maxTip || 50), unit: '元', color: '#10d9b5', icon: 'Present', foot: '小费全额归跑腿员' }
])

const feeMarks = computed(() => {
  const min = Number(rules.value.feeMin || 2), max = Number(rules.value.feeMax || 50)
  return { [min]: `${min} 元`, [max]: `${max} 元` }
})
const hourOptions = computed(() => {
  const max = Number(rules.value.maxHours || 48)
  return [2, 6, 12, 24, 36, 48, 72].filter(h => h <= max)
})

function urgent(v) {
  if (!v) return false
  return new Date(String(v).replace(' ', 'T')).getTime() - Date.now() < 3600000
}

async function loadHall() {
  loading.value = true
  try {
    const r = await proxyHall({ page: page.value, size, stationId: stationId.value || undefined, keyword: keyword.value || undefined })
    rows.value = r.records || []
    total.value = Number(r.total || 0)
  } finally { loading.value = false }
}
function reload() { page.value = 1; loadHall() }
function reset() { stationId.value = null; keyword.value = ''; reload() }

async function doAccept(o) {
  try {
    await ElMessageBox.confirm(
      `接单后系统将自动生成代取授权码，请凭码到「${o.stationName || '驿站'}」柜台取件，并送至「${o.deliverTo || '委托人指定地址'}」。实得 ${money(o.runnerIncome)}。`,
      `确认接单 ${o.orderNo}`, { type: 'info', confirmButtonText: '确认接单' }
    )
  } catch { return }
  acting.value = o.id
  try {
    const res = await acceptProxyOrder(o.id)
    ElMessage.success(`接单成功，授权码 ${res.authCode || ''}，请前往「我的服务单」查看`)
    await Promise.all([loadHall(), loadBalance()])
    router.push('/student/proxy-orders?tab=runs')
  } finally { acting.value = null }
}

/* ---- 发布 ---- */
const publish = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const myStock = ref([])
const parcelLoading = ref(false)
const busy = ref(new Set())
const form = ref({ parcelId: null, fee: 5, tip: 0, deliverTo: '', note: '', hours: 24 })
const formRules = {
  parcelId: [{ required: true, message: '请选择包裹', trigger: 'change' }],
  fee: [{ required: true, message: '请填写服务费', trigger: 'blur' }],
  deliverTo: [{ required: true, message: '请填写送达地址', trigger: 'blur' }]
}
const selectedParcel = computed(() => myStock.value.find(p => p.id === form.value.parcelId) || null)
const totalFee = computed(() => Number(form.value.fee || 0) + Number(form.value.tip || 0))
const commission = computed(() => Math.round(totalFee.value * Number(rules.value.commissionPercent || 10)) / 100)
const runnerGets = computed(() => Math.max(0, totalFee.value - commission.value))

function onParcelChange() {
  const p = selectedParcel.value
  if (p && !form.value.deliverTo) form.value.deliverTo = ''
}

async function openPublish() {
  publish.value = true
  parcelLoading.value = true
  try {
    const [pr, req] = await Promise.all([
      myParcels({ page: 1, size: 100 }),
      myProxyRequests({ page: 1, size: 100 })
    ])
    myStock.value = (pr.records || []).filter(p => ['IN_STOCK', 'OVERDUE'].includes(p.status))
    busy.value = new Set((req.records || [])
      .filter(o => ['OPEN', 'ACCEPTED', 'PICKED', 'DELIVERED'].includes(o.status))
      .map(o => o.parcelId))
    if (!hourOptions.value.includes(form.value.hours)) form.value.hours = hourOptions.value[hourOptions.value.length - 1] || 24
  } finally { parcelLoading.value = false }
}

async function doPublish() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const res = await createProxyOrder({ ...form.value })
    ElMessage.success(`服务单 ${res.orderNo} 已发布，托管 ${money(totalFee.value)}`)
    publish.value = false
    form.value = { parcelId: null, fee: 5, tip: 0, deliverTo: '', note: '', hours: 24 }
    await Promise.all([loadHall(), loadBalance()])
    router.push('/student/proxy-orders?tab=requests')
  } finally { submitting.value = false }
}

async function loadBalance() {
  const w = await walletOverview().catch(() => null)
  balance.value = Number(w?.account?.balance || 0)
}

onMounted(async () => {
  const [r, s] = await Promise.all([proxyRules(), listStations().catch(() => [])])
  rules.value = r
  stations.value = s
  await Promise.all([loadHall(), loadBalance()])
})
</script>

<style scoped>
.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 310px), 1fr));
  gap: 14px;
  min-height: 120px;
}
.market-card {
  position: relative; overflow: hidden;
  display: flex; flex-direction: column;
  border: 1px solid var(--default-border);
  border-radius: var(--art-radius-card-sm);
  background: var(--default-box-color);
  padding: 14px 15px 12px;
  transition: transform 0.3s var(--flair-ease), box-shadow 0.3s, border-color 0.3s;
}
.market-card::before {
  content: ''; position: absolute; left: 0; right: 0; top: 0; height: 3px;
  background: linear-gradient(90deg, #0ea5e9, #22d3ee 55%, transparent);
  opacity: 0.75;
}
.market-card:hover { transform: translateY(-4px); box-shadow: 0 18px 34px -20px rgba(14, 165, 233, 0.6); border-color: color-mix(in srgb, #0ea5e9 45%, transparent); }
.mc-top { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.mc-courier { font-size: 12px; font-weight: 700; color: var(--ap-proxy); }
.mc-no { margin-left: auto; font-size: 11.5px; color: var(--art-gray-500); }

.mc-route { display: flex; align-items: center; gap: 8px; margin: 12px 0 10px; }
.mc-point { display: flex; gap: 7px; align-items: flex-start; min-width: 0; flex: 1; }
.mc-point .el-icon { margin-top: 2px; flex-shrink: 0; }
.mc-point.from .el-icon { color: var(--ap-proxy); }
.mc-point.to .el-icon { color: #10d9b5; }
.mc-point b { display: block; font-size: 12.5px; color: var(--art-gray-900); line-height: 1.4; word-break: break-all; }
.mc-point span { display: block; font-size: 11px; color: var(--art-gray-500); margin-top: 2px; line-height: 1.4; }
.mc-arrow { color: var(--art-gray-400); flex-shrink: 0; }

.mc-note {
  margin: 0 0 10px; font-size: 11.5px; line-height: 1.6; color: var(--art-gray-600);
  background: var(--art-gray-100); border-radius: 8px; padding: 7px 10px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
html.dark .mc-note { background: #1c1c22; }

.mc-money {
  margin-top: auto; padding: 10px 12px; border-radius: 11px;
  background: linear-gradient(140deg, rgba(14, 165, 233, 0.1), rgba(16, 217, 181, 0.06));
  border: 1px solid color-mix(in srgb, #0ea5e9 24%, transparent);
}
.mc-total { display: flex; align-items: baseline; justify-content: space-between; }
.mc-total span { font-size: 11.5px; color: var(--art-gray-600); font-weight: 600; }
.mc-total b { font-size: 23px; font-weight: 800; color: #0369a1; }
html.dark .mc-total b { color: #38bdf8; }
.mc-split { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; font-size: 10.5px; color: var(--art-gray-500); }
.mc-comm { margin-left: auto; }

.mc-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 11px; }
.mc-expire { display: inline-flex; align-items: center; gap: 4px; font-size: 11.5px; color: var(--art-gray-500); }
.mc-expire.urgent { color: #e0576f; font-weight: 700; }

/* 发布表单 */
.fee-row { display: flex; align-items: center; gap: 16px; width: 100%; }
.parcel-preview {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 130px), 1fr));
  gap: 8px 14px; margin: -4px 0 16px;
  padding: 11px 13px; border-radius: 10px;
  background: color-mix(in srgb, #0ea5e9 7%, transparent);
  border: 1px solid color-mix(in srgb, #0ea5e9 22%, transparent);
}
.parcel-preview label { display: block; font-size: 10.5px; color: var(--art-gray-500); letter-spacing: 0.5px; }
.parcel-preview span { display: block; font-size: 12.5px; color: var(--art-gray-900); font-weight: 600; margin-top: 2px; }

.escrow-box {
  padding: 13px 15px; border-radius: 12px;
  border: 1px dashed color-mix(in srgb, #0ea5e9 42%, transparent);
  background: linear-gradient(150deg, rgba(14, 165, 233, 0.07), rgba(16, 217, 181, 0.03));
}
.eb-line { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--art-gray-600); padding: 2px 0; }
.eb-line b { color: var(--art-gray-900); }
.eb-line.total { margin-top: 7px; padding-top: 8px; border-top: 1px solid color-mix(in srgb, #0ea5e9 25%, transparent); font-weight: 700; }
.eb-line.total b { font-size: 21px; color: #0369a1; font-variant-numeric: tabular-nums; }
html.dark .eb-line.total b { color: #38bdf8; }
.eb-split { display: flex; gap: 14px; flex-wrap: wrap; margin-top: 6px; font-size: 11px; color: var(--art-gray-500); }
.eb-balance { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-top: 9px; font-size: 12px; color: #0d9488; font-weight: 600; }
.eb-balance.lack { color: #e0576f; }
html.dark .eb-balance { color: #35e0c2; }
html.dark .eb-balance.lack { color: #ff8fa3; }

@media (max-width: 560px) {
  .fee-row { flex-direction: column; align-items: stretch; gap: 4px; }
  .fee-row .el-input-number { width: 100% !important; }
  .mc-no { margin-left: 0; width: 100%; }
}
</style>