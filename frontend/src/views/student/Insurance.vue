<template>
  <div class="ap">
    <PageHero tone="policy" icon="Umbrella" chip="OVERTIME INSURANCE" live title="超时保险"
      sub="为在库/滞留包裹投保：保障期内因驿站原因超时未取，按保额赔付至钱包余额。每件包裹限投一次。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Umbrella" @click="openBuy">为包裹投保</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load(1)">刷新</el-button>
      </template>
    </PageHero>

    <div class="ap-stats">
      <MetricTile label="保费 / 单" :value="Number(plan.premium || 0)" unit="元" :decimals="2" icon="Coin" color="#5b7cfa" :index="0" />
      <MetricTile label="赔付保额" :value="Number(plan.coverage || 0)" unit="元" :decimals="2" icon="Money" color="#10d9b5" :index="1" />
      <MetricTile label="保障时效" :value="Number(plan.validHours || 0)" unit="小时" icon="Timer" color="#f59e0b" :index="2" />
      <MetricTile label="我的保单" :value="total" unit="份" icon="Document" color="#8b5cf6" :index="3" />
    </div>

    <PanelCard title="我的保单" :note="`${total} 份`" icon="DocumentChecked" accent="policy" :index="1">
      <div class="ap-seg">
        <button v-for="s in segs" :key="s.value" :class="{ on: status === s.value }" @click="status = s.value; load(1)">{{ s.label }}</button>
      </div>

      <div class="ap-table-wrap">
        <el-table :data="sortedRows" v-loading="loading" :default-sort="defaultSort" @sort-change="onSortChange">
          <el-table-column prop="policyNo" label="保单号" min-width="170" sortable="custom">
            <template #default="{ row }"><span class="ap-mono">{{ row.policyNo }}</span></template>
          </el-table-column>
          <el-table-column prop="trackingNo" label="关联包裹" min-width="160">
            <template #default="{ row }">
              <div class="ap-mono">{{ row.trackingNo || '—' }}</div>
              <div class="cell-sub">{{ courierName(row.courierCompany) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="premium" label="保费" min-width="92" sortable="custom" align="right">
            <template #default="{ row }"><span class="ap-mono">{{ money(row.premium) }}</span></template>
          </el-table-column>
          <el-table-column prop="coverage" label="保额" min-width="92" align="right">
            <template #default="{ row }"><span class="ap-mono">{{ money(row.coverage) }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="96">
            <template #default="{ row }"><el-tag size="small" :type="dict(INSURANCE_STATUS, row.status).type" effect="light">{{ dict(INSURANCE_STATUS, row.status).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="expireAt" label="保障截止" min-width="150" sortable="custom">
            <template #default="{ row }">
              <div class="ap-mono">{{ fmtTime(row.expireAt) }}</div>
              <div v-if="row.status === 'ACTIVE'" class="cell-sub">{{ countdown(row.expireAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="投保时间" min-width="140" sortable="custom">
            <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="理赔进展" min-width="150">
            <template #default="{ row }">
              <span v-if="row.settledAt" class="ap-mono cell-sub">{{ fmtTime(row.settledAt) }} 已赔付</span>
              <span v-else-if="row.claimedAt" class="ap-mono cell-sub">{{ fmtTime(row.claimedAt) }} 已受理</span>
              <span v-else class="cell-sub">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="还没有保单" :image-size="86" />
        <p>为在库或滞留包裹投保后，保单与理赔进展将在这里跟踪。</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>

    <PanelCard title="保障规则" icon="Reading" accent="slate" :index="2">
      <ol class="step-list">
        <li><b>投保范围</b>仅限本人名下「在库 / 滞留」状态包裹，每件限投一次。</li>
        <li><b>保费扣款</b>投保成功即从钱包余额扣除保费 {{ money(plan.premium || 0) }}。</li>
        <li><b>触发理赔</b>保障期内包裹超时且责任在驿站，系统自动受理理赔。</li>
        <li><b>赔付到账</b>核实后按保额 {{ money(plan.coverage || 0) }} 赔付至钱包，可在钱包流水中核对。</li>
        <li><b>保障失效</b>包裹取走或超过保障时效（{{ plan.validHours || 168 }} 小时）后保单到期。</li>
      </ol>
    </PanelCard>

    <el-dialog v-model="buyVisible" title="选择包裹投保" width="560px" append-to-body>
      <div class="ap-note" style="--nc:#5b7cfa">
        <el-icon><Umbrella /></el-icon>
        <div>保费 <b class="ap-mono">{{ money(plan.premium || 0) }}</b> · 保额 <b class="ap-mono">{{ money(plan.coverage || 0) }}</b> · 保障 {{ plan.validHours || 168 }} 小时，投保即从钱包扣费。</div>
      </div>
      <div v-loading="parcelLoading" class="ins-pick">
        <button v-for="p in candidates" :key="p.id" class="ins-pick-item" :disabled="buying" @click="doBuy(p)">
          <div class="ip-l1">
            <span class="ap-mono">{{ p.trackingNo }}</span>
            <el-tag size="small" :type="dict(PARCEL_STATUS, p.status).type" effect="light">{{ dict(PARCEL_STATUS, p.status).label }}</el-tag>
          </div>
          <div class="ip-l2">{{ courierName(p.courierCompany) }} · 入库 {{ fmtTime(p.inboundAt || p.createdAt) }}</div>
        </button>
      </div>
      <div v-if="!candidates.length && !parcelLoading" class="ap-empty">
        <el-empty description="暂无可投保包裹" :image-size="80" />
        <p>只有在库或滞留、且未投保的包裹可以购买超时保险。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Umbrella, Refresh } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import MetricTile from '../../components/MetricTile.vue'
import { insurancePlan, buyInsurance, myInsurance, myParcels } from '../../api'
import { INSURANCE_STATUS, PARCEL_STATUS, dict, fmtTime, money, countdown, courierName } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const plan = ref({})
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const status = ref('')
const query = reactive({ page: 1, size: 10 })
const segs = [
  { value: '', label: '全部' }, { value: 'ACTIVE', label: '保障中' },
  { value: 'CLAIMED', label: '理赔中' }, { value: 'CLAIM_PAID', label: '已赔付' }, { value: 'EXPIRED', label: '已到期' }
]

const { defaultSort, onSortChange, sorted } = useTableSort('createdAt', { premium: r => Number(r.premium || 0) })
const sortedRows = computed(() => sorted(rows.value))

const buyVisible = ref(false)
const parcelLoading = ref(false)
const buying = ref(false)
const candidates = ref([])

const heroStats = computed(() => [
  { label: '保单总数', value: total.value, unit: '份' },
  { label: '本页保障中', value: rows.value.filter(r => r.status === 'ACTIVE').length, unit: '份' },
  { label: '本页已赔付', value: rows.value.filter(r => r.status === 'CLAIM_PAID').length, unit: '份' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myInsurance({ ...query, status: status.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function openBuy() {
  buyVisible.value = true
  parcelLoading.value = true
  try {
    const [inStock, overdue] = await Promise.all([
      myParcels({ page: 1, size: 50, status: 'IN_STOCK' }),
      myParcels({ page: 1, size: 50, status: 'OVERDUE' })
    ])
    candidates.value = [...(inStock.records || []), ...(overdue.records || [])]
  } finally { parcelLoading.value = false }
}

async function doBuy(p) {
  try {
    await ElMessageBox.confirm(`为包裹 ${p.trackingNo} 投保？保费 ${money(plan.value.premium || 0)} 将从钱包扣除。`, '确认投保', { type: 'info', confirmButtonText: '确认投保' })
  } catch { return }
  buying.value = true
  try {
    const policy = await buyInsurance(p.id)
    ElMessage.success(`投保成功：保单 ${policy.policyNo}`)
    buyVisible.value = false
    status.value = ''
    await load(1)
  } finally { buying.value = false }
}

onMounted(async () => {
  load()
  try { plan.value = await insurancePlan() } catch { /* 展示默认 */ }
})
</script>

<style scoped>
.cell-sub { font-size: 11px; color: var(--art-gray-500); margin-top: 2px; }
.step-list { margin: 0; padding-left: 18px; display: flex; flex-direction: column; gap: 9px; }
.step-list li { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
.step-list b { color: var(--art-gray-900); margin-right: 5px; }
.ins-pick { display: flex; flex-direction: column; gap: 8px; max-height: 46vh; overflow: auto; margin-top: 10px; }
.ins-pick-item {
  text-align: left; border: 1px solid var(--default-border); border-radius: 12px; padding: 11px 13px;
  background: transparent; cursor: pointer; transition: all 0.2s;
}
.ins-pick-item:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.ip-l1 { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.ip-l1 .ap-mono { font-size: 13px; color: var(--art-gray-900); font-weight: 600; }
.ip-l2 { margin-top: 5px; font-size: 11.5px; color: var(--art-gray-500); }
</style>