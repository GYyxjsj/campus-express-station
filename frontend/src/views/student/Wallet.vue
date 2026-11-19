<template>
  <div class="ap">
    <PageHero tone="wallet" icon="Wallet" chip="CAMPUS WALLET" live
      title="钱包与充值"
      sub="校园一卡通 / 支付宝 / 微信三通道即时到账，余额用于代取服务托管扣款，跑腿收入自动结算回钱包。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Plus" @click="focusComposer">立即充值</el-button>
        <el-button type="default" :icon="Tickets" @click="scrollTo('tx')">收支流水</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="ap-row wide-left">
      <!-- 充值台 -->
      <PanelCard ref="composerRef" title="充值台" note="即时到账" icon="CreditCard" accent="wallet" :index="4">
        <template #extra>
          <span class="range-hint">单笔 {{ money(ov.min, 0) }} – {{ money(ov.max, 0) }}</span>
        </template>

        <div v-if="account.status === 0" class="ap-note" style="--nc:#fb7185;margin-bottom:14px">
          <el-icon><WarningFilled /></el-icon>
          <div><b>钱包已冻结。</b>请联系驿站管理员解除冻结后再充值，冻结期间余额不可用于代取服务。</div>
        </div>

        <div class="tier-grid">
          <button v-for="t in tiers" :key="t.amount" class="tier" :class="{ on: pick === Number(t.amount) }"
                  @click="chooseTier(t)">
            <span class="tier-amt">{{ Number(t.amount) }}<i>元</i></span>
            <span class="tier-bonus" :class="{ zero: !Number(t.bonus) }">
              {{ Number(t.bonus) ? `赠 ${Number(t.bonus)} 元` : '无赠送' }}
            </span>
            <span class="tier-get">到账 {{ money(Number(t.amount) + Number(t.bonus)) }}</span>
            <el-icon v-if="pick === Number(t.amount)" class="tier-check"><CircleCheckFilled /></el-icon>
          </button>
        </div>

        <div class="custom-row">
          <label class="ap-field-label">自定义金额</label>
          <el-input-number v-model="custom" :min="Number(ov.min || 1)" :max="Number(ov.max || 500)"
                           :precision="2" :step="10" controls-position="right" placeholder="输入金额"
                           @change="pick = null" />
          <span class="custom-bonus" v-if="customBonus > 0">命中档位赠送 {{ money(customBonus) }}</span>
          <span class="custom-bonus muted" v-else>满 50 赠 3 · 满 100 赠 8 · 满 200 赠 20</span>
        </div>

        <label class="ap-field-label">支付通道</label>
        <div class="channel-grid">
          <button v-for="c in channels" :key="c.key" class="channel" :class="{ on: channel === c.key }"
                  :style="{ '--cc': c.color }" @click="channel = c.key">
            <el-icon :size="19"><component :is="c.icon" /></el-icon>
            <span>{{ c.label }}</span>
            <i class="ch-dot" />
          </button>
        </div>

        <div class="checkout">
          <div class="co-line"><span>实付金额</span><b class="ap-money bare">{{ money(amount) }}</b></div>
          <div class="co-line bonus"><span>活动赠送</span><b>+ {{ money(bonus) }}</b></div>
          <div class="co-total"><span>预计到账</span><b>{{ money(amount + bonus) }}</b></div>
          <el-button type="primary" size="large" :icon="Money" :loading="creating"
                     :disabled="!validAmount || account.status === 0" @click="submitRecharge">
            生成充值订单
          </el-button>
          <p class="co-tip">演示环境：订单生成后点击「确认支付」即时入账，15 分钟未支付自动过期。</p>
        </div>
      </PanelCard>

      <!-- 待支付 + 规则 -->
      <div class="side-col">
        <PanelCard title="待支付订单" :note="`${pending.length} 笔`" icon="AlarmClock" accent="amber" :index="5">
          <div v-if="pending.length" class="ap-list">
            <div v-for="o in pending" :key="o.id" class="ap-list-item" style="--la:#f59e0b">
              <div class="ap-list-avatar"><el-icon :size="17"><Ticket /></el-icon></div>
              <div class="ap-list-body">
                <div class="ap-list-l1">
                  <b class="ap-money bare">{{ money(o.amount) }}</b>
                  <el-tag v-if="Number(o.bonus)" size="small" type="warning" effect="light">赠 {{ money(o.bonus) }}</el-tag>
                </div>
                <div class="ap-list-l2 ap-mono">{{ o.orderNo }}</div>
                <div class="ap-list-l2">
                  {{ channelLabel(o.channel) }} · 剩余 {{ countdown(o.expireAt) }}
                </div>
              </div>
              <div class="ap-list-right">
                <el-button type="primary" size="small" @click="openCashier(o)">支付</el-button>
                <el-button size="small" text type="danger" @click="doCancel(o)">取消</el-button>
              </div>
            </div>
          </div>
          <div v-else class="ap-empty">
            <el-empty description="没有待支付订单" :image-size="64" />
            <p>在左侧充值台选择档位即可生成订单。</p>
          </div>
        </PanelCard>

        <PanelCard title="钱包规则" icon="InfoFilled" accent="slate" :index="6">
          <ul class="rule-list">
            <li><b>托管扣款</b>发布代取服务时，服务费与小费从余额托管冻结，完成后结算给跑腿员。</li>
            <li><b>取消退款</b>服务单在接单前取消或超时未接单，托管金额全额退回钱包。</li>
            <li><b>平台服务费</b>每笔完成订单按 {{ rules.commissionPercent ?? 10 }}% 收取，跑腿员实得为总额扣除服务费。</li>
            <li><b>充值赠送</b>满 50 赠 3、满 100 赠 8、满 200 赠 20，赠送金额与本金一并入账。</li>
            <li><b>流水留痕</b>每笔变动记录变动后余额、通道与关联单号，可在下方逐笔核对。</li>
          </ul>
          <template #foot>
            <router-link class="ap-link" to="/policies">查看《钱包与充值规则》全文 →</router-link>
          </template>
        </PanelCard>
      </div>
    </div>

    <!-- 收支流水 -->
    <PanelCard id="tx" title="收支流水" :note="`共 ${txTotal} 笔`" icon="List" accent="mint" :index="7">
      <template #extra>
        <div class="ap-seg">
          <button v-for="t in txTabs" :key="t.key" :class="{ on: txType === t.key }" @click="txType = t.key; txPage = 1; loadTx()">
            {{ t.label }}
          </button>
        </div>
      </template>
      <div class="ap-table-wrap">
        <el-table :data="sortedTx" v-loading="txLoading" stripe :default-sort="txSort.defaultSort" @sort-change="txSort.onSortChange">
          <el-table-column label="时间" prop="createdAt" sortable="custom" width="150">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="类型" prop="type" sortable="custom" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="dict(WALLET_TX, row.type).type" effect="light">{{ dict(WALLET_TX, row.type).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="200">
            <template #default="{ row }">
              <span class="ap-strong">{{ row.title }}</span>
              <div v-if="row.remark" class="ap-sub">{{ row.remark }}</div>
            </template>
          </el-table-column>
          <el-table-column label="通道" width="110">
            <template #default="{ row }">{{ channelLabel(row.channel) }}</template>
          </el-table-column>
          <el-table-column label="金额" prop="amount" sortable="custom" width="120" align="right">
            <template #default="{ row }">
              <span class="ap-money" :class="Number(row.amount) >= 0 ? 'in' : 'out'">{{ money(Math.abs(Number(row.amount))) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="变动后余额" prop="balanceAfter" sortable="custom" width="130" align="right">
            <template #default="{ row }"><span class="ap-mono">{{ money(row.balanceAfter) }}</span></template>
          </el-table-column>
          <el-table-column label="关联单号" width="180">
            <template #default="{ row }">
              <span v-if="row.refId" class="ap-mono ap-sub2">{{ refLabel(row) }}</span>
              <span v-else class="ap-sub">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in txRows" :key="row.id" class="ap-card-item"
             :style="{ '--ci': Number(row.amount) >= 0 ? '#10d9b5' : '#fb7185' }">
          <div class="ap-card-top">
            <b>{{ row.title }}</b>
            <el-tag size="small" :type="dict(WALLET_TX, row.type).type" effect="light">{{ dict(WALLET_TX, row.type).label }}</el-tag>
            <span class="ap-money card-amt" :class="Number(row.amount) >= 0 ? 'in' : 'out'">{{ money(Math.abs(Number(row.amount))) }}</span>
          </div>
          <div class="ap-card-grid">
            <div><label>时间</label><span>{{ fmtTime(row.createdAt) }}</span></div>
            <div><label>通道</label><span>{{ channelLabel(row.channel) }}</span></div>
            <div><label>变动后余额</label><span>{{ money(row.balanceAfter) }}</span></div>
            <div v-if="row.refId"><label>关联单号</label><span class="ap-mono">{{ refLabel(row) }}</span></div>
          </div>
          <div v-if="row.remark" class="ap-sub" style="margin-top:8px">{{ row.remark }}</div>
        </div>
        <div v-if="!txRows.length && !txLoading" class="ap-empty"><el-empty description="暂无流水" :image-size="64" /></div>
      </div>
      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="txTotal"
                       v-model:current-page="txPage" :page-size="txSize" @current-change="loadTx" />
      </div>
    </PanelCard>

    <!-- 充值订单 -->
    <PanelCard title="充值订单" :note="`共 ${orderTotal} 笔`" icon="Tickets" accent="wallet" :index="8">
      <template #extra>
        <div class="ap-seg">
          <button v-for="s in orderTabs" :key="s.key" :class="{ on: orderStatus === s.key }"
                  @click="orderStatus = s.key; orderPage = 1; loadOrders()">{{ s.label }}</button>
        </div>
      </template>
      <div class="ap-table-wrap">
        <el-table :data="sortedOrders" v-loading="orderLoading" stripe :default-sort="orderSort.defaultSort" @sort-change="orderSort.onSortChange">
          <el-table-column label="订单号" prop="orderNo" sortable="custom" min-width="190">
            <template #default="{ row }"><span class="ap-mono ap-strong">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column label="金额" prop="amount" sortable="custom" width="110" align="right">
            <template #default="{ row }"><span class="ap-money bare in">{{ money(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="赠送" prop="bonus" sortable="custom" width="90" align="right">
            <template #default="{ row }">
              <span v-if="Number(row.bonus)" class="ap-money bare in">{{ money(row.bonus) }}</span>
              <span v-else class="ap-sub">—</span>
            </template>
          </el-table-column>
          <el-table-column label="通道" width="120">
            <template #default="{ row }">{{ channelLabel(row.channel) }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="status" sortable="custom" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="dict(RECHARGE_STATUS, row.status).type" effect="light">{{ dict(RECHARGE_STATUS, row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createdAt" sortable="custom" width="150">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="到账时间" prop="paidAt" sortable="custom" width="150">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ row.paidAt ? fmtTime(row.paidAt) : '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <div class="ap-ops">
                  <el-button size="small" type="primary" link :icon="Money" @click="openCashier(row)">支付</el-button>
                  <el-button size="small" type="danger" link :icon="CircleClose" @click="doCancel(row)">取消</el-button>
                </div>
              </template>
              <span v-else class="ap-sub">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in orderRows" :key="row.id" class="ap-card-item" style="--ci:#f59e0b">
          <div class="ap-card-top">
            <b class="ap-money bare">{{ money(row.amount) }}</b>
            <el-tag size="small" :type="dict(RECHARGE_STATUS, row.status).type" effect="light">{{ dict(RECHARGE_STATUS, row.status).label }}</el-tag>
            <el-tag v-if="Number(row.bonus)" size="small" type="warning" effect="plain">赠 {{ money(row.bonus) }}</el-tag>
          </div>
          <div class="ap-card-grid">
            <div><label>订单号</label><span class="ap-mono">{{ row.orderNo }}</span></div>
            <div><label>通道</label><span>{{ channelLabel(row.channel) }}</span></div>
            <div><label>创建</label><span>{{ fmtTime(row.createdAt) }}</span></div>
            <div><label>到账</label><span>{{ row.paidAt ? fmtTime(row.paidAt) : '—' }}</span></div>
          </div>
          <div v-if="row.status === 'PENDING'" class="ap-card-actions">
            <el-button type="primary" size="small" :icon="Money" @click="openCashier(row)">立即支付</el-button>
            <el-button size="small" type="danger" plain :icon="CircleClose" @click="doCancel(row)">取消订单</el-button>
          </div>
        </div>
        <div v-if="!orderRows.length && !orderLoading" class="ap-empty"><el-empty description="暂无充值订单" :image-size="64" /></div>
      </div>
      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="orderTotal"
                       v-model:current-page="orderPage" :page-size="orderSize" @current-change="loadOrders" />
      </div>
    </PanelCard>

    <AdBanner slot="WALLET_FOOT" foot />

    <!-- 收银台 -->
    <el-dialog v-model="cashier" title="收银台" width="440px" :close-on-click-modal="false" append-to-body>
      <div v-if="current" class="cashier">
        <div class="cash-head" :style="{ '--cc': channelColor(current.channel) }">
          <el-icon :size="26"><component :is="channelIcon(current.channel)" /></el-icon>
          <div>
            <b>{{ channelLabel(current.channel) }}</b>
            <span class="ap-mono">{{ current.orderNo }}</span>
          </div>
          <el-tag size="small" type="warning" effect="dark">待支付</el-tag>
        </div>
        <div class="cash-amount">
          <span>支付金额</span>
          <b>{{ money(current.amount) }}</b>
        </div>
        <div class="cash-kv">
          <div><label>活动赠送</label><span class="ap-money in bare">{{ money(current.bonus) }}</span></div>
          <div><label>实际到账</label><span class="ap-money in bare">{{ money(Number(current.amount) + Number(current.bonus)) }}</span></div>
          <div><label>支付剩余</label><span>{{ countdown(current.expireAt) }}</span></div>
          <div><label>当前余额</label><span class="ap-money bare">{{ money(account.balance) }}</span></div>
        </div>
        <div class="ap-note" style="--nc:#f59e0b">
          <el-icon><InfoFilled /></el-icon>
          <div>演示环境不接入真实支付网关，点击「确认支付」即模拟通道回调并即时入账。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cashier = false">稍后支付</el-button>
        <el-button type="primary" :loading="paying" :icon="CircleCheck" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Tickets, Money, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import MetricTile from '../../components/MetricTile.vue'
import AdBanner from '../../components/AdBanner.vue'
import {
  walletOverview, createRecharge, payRecharge, cancelRecharge,
  walletTransactions, myRechargeOrders, proxyRules
} from '../../api'
import { WALLET_TX, RECHARGE_STATUS, PAY_CHANNEL, dict, fmtTime, money, countdown } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const ov = ref({ min: 1, max: 500 })
const account = computed(() => ov.value.account || { balance: 0, status: 1, totalRecharge: 0, totalSpent: 0, totalEarned: 0 })
const tiers = computed(() => ov.value.tiers || [])
const pending = computed(() => ov.value.pending || [])
const rules = ref({})

const pick = ref(50)
const custom = ref(null)
const channel = ref('CAMPUS_CARD')
const creating = ref(false)
const composerRef = ref(null)

const channels = [
  { key: 'CAMPUS_CARD', label: '校园一卡通', icon: 'CreditCard', color: '#5b7cfa' },
  { key: 'ALIPAY', label: '支付宝', icon: 'Wallet', color: '#0ea5e9' },
  { key: 'WECHAT', label: '微信支付', icon: 'ChatDotRound', color: '#10d9b5' }
]

const amount = computed(() => Number(pick.value ?? custom.value ?? 0))
const bonus = computed(() => {
  const a = amount.value
  let b = 0
  for (const t of tiers.value) if (a >= Number(t.amount)) b = Number(t.bonus)
  return b
})
const customBonus = computed(() => {
  const a = Number(custom.value || 0)
  let b = 0
  for (const t of tiers.value) if (a >= Number(t.amount)) b = Number(t.bonus)
  return b
})
const validAmount = computed(() => {
  const a = amount.value
  return Number.isFinite(a) && a >= Number(ov.value.min || 1) && a <= Number(ov.value.max || 500)
})

const heroStats = computed(() => [
  { label: '可用余额', value: Number(account.value.balance || 0), decimals: 2 },
  { label: '本月充值', value: Number(ov.value.monthRecharge || 0), decimals: 2 },
  { label: '本月跑腿收入', value: Number(ov.value.monthEarn || 0), decimals: 2 }
])
const metrics = computed(() => [
  { label: '累计充值', value: Number(account.value.totalRecharge || 0), decimals: 2, unit: '元', color: '#f59e0b', icon: 'Download', foot: '含活动赠送金额' },
  { label: '累计支出', value: Number(account.value.totalSpent || 0), decimals: 2, unit: '元', color: '#fb7185', icon: 'Upload', foot: '代取服务托管扣款' },
  { label: '累计收入', value: Number(account.value.totalEarned || 0), decimals: 2, unit: '元', color: '#10d9b5', icon: 'Bicycle', foot: '跑腿结算实得' },
  { label: '本月消费', value: Number(ov.value.monthConsume || 0), decimals: 2, unit: '元', color: '#8b5cf6', icon: 'ShoppingBag', foot: '自然月统计' }
])

function channelLabel(k) { return PAY_CHANNEL[k]?.label || k || '—' }
function channelIcon(k) { return PAY_CHANNEL[k]?.icon || 'Money' }
function channelColor(k) { return PAY_CHANNEL[k]?.color || '#5b7cfa' }
function refLabel(row) {
  const map = { RECHARGE_ORDER: '充值', PROXY_ORDER: '代取', PARCEL: '包裹', ADMIN: '人工' }
  return `${map[row.refType] || row.refType || ''}#${row.refId}`
}

function chooseTier(t) {
  pick.value = Number(t.amount)
  custom.value = null
}
function focusComposer() {
  composerRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadOverview() {
  ov.value = await walletOverview()
  if (!tiers.value.some(t => Number(t.amount) === pick.value)) pick.value = Number(tiers.value[1]?.amount || 50)
}

async function submitRecharge() {
  if (!validAmount.value) return ElMessage.warning(`请输入 ${ov.value.min} – ${ov.value.max} 元之间的金额`)
  creating.value = true
  try {
    const order = await createRecharge({ amount: amount.value, channel: channel.value })
    ElMessage.success('充值订单已生成，请在收银台完成支付')
    await loadOverview()
    await loadOrders()
    openCashier(order)
  } finally {
    creating.value = false
  }
}

const cashier = ref(false)
const current = ref(null)
const paying = ref(false)
function openCashier(o) { current.value = o; cashier.value = true }

async function doPay() {
  if (!current.value) return
  paying.value = true
  try {
    const res = await payRecharge(current.value.id)
    cashier.value = false
    ElMessage.success(`充值成功，到账 ${money(Number(res.order.amount) + Number(res.order.bonus))}`)
    await Promise.all([loadOverview(), loadOrders(), loadTx()])
  } finally {
    paying.value = false
  }
}

async function doCancel(o) {
  try {
    await ElMessageBox.confirm(`确定取消充值订单 ${o.orderNo} 吗？`, '取消订单', { type: 'warning' })
  } catch { return }
  await cancelRecharge(o.id)
  ElMessage.success('订单已取消')
  await Promise.all([loadOverview(), loadOrders()])
}

/* ---- 流水 ---- */
const txTabs = [
  { key: '', label: '全部' }, { key: 'RECHARGE', label: '充值' }, { key: 'CONSUME', label: '支出' },
  { key: 'EARN', label: '收入' }, { key: 'REFUND', label: '退款' }, { key: 'ADJUST', label: '调整' }
]
const txType = ref('')
const txRows = ref([])
const txTotal = ref(0)
const txPage = ref(1)
const txSize = 8
const txLoading = ref(false)
const txSort = useTableSort('createdAt', { amount: r => Number(r.amount), balanceAfter: r => Number(r.balanceAfter) })
const sortedTx = computed(() => txSort.sorted(txRows.value))
async function loadTx() {
  txLoading.value = true
  try {
    const r = await walletTransactions({ page: txPage.value, size: txSize, type: txType.value || undefined })
    txRows.value = r.records || []
    txTotal.value = Number(r.total || 0)
  } finally { txLoading.value = false }
}

/* ---- 充值订单 ---- */
const orderTabs = [
  { key: '', label: '全部' }, { key: 'PENDING', label: '待支付' },
  { key: 'PAID', label: '已到账' }, { key: 'CANCELLED', label: '已取消' }, { key: 'EXPIRED', label: '已过期' }
]
const orderStatus = ref('')
const orderRows = ref([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderSize = 8
const orderLoading = ref(false)
const orderSort = useTableSort('createdAt', { amount: r => Number(r.amount), bonus: r => Number(r.bonus || 0) })
const sortedOrders = computed(() => orderSort.sorted(orderRows.value))
async function loadOrders() {
  orderLoading.value = true
  try {
    const r = await myRechargeOrders({ page: orderPage.value, size: orderSize, status: orderStatus.value || undefined })
    orderRows.value = r.records || []
    orderTotal.value = Number(r.total || 0)
  } finally { orderLoading.value = false }
}

let tick = null
onMounted(async () => {
  await Promise.all([loadOverview(), loadTx(), loadOrders()])
  rules.value = await proxyRules().catch(() => ({}))
  tick = setInterval(() => { ov.value = { ...ov.value } }, 30000)
})
onUnmounted(() => clearInterval(tick))
</script>

<style scoped>
.side-col { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.range-hint { font-size: 11.5px; color: var(--art-gray-500); }
.ap-field-label {
  display: block; font-size: 11px; font-weight: 700; letter-spacing: 0.8px;
  color: var(--art-gray-500); text-transform: uppercase; margin: 16px 0 8px;
}
.ap-sub2 { font-size: 12px; color: var(--art-gray-600); }

/* 档位 */
.tier-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 120px), 1fr)); gap: 10px; }
.tier {
  position: relative; overflow: hidden;
  border: 1.5px solid var(--default-border);
  background: var(--default-box-color);
  border-radius: 14px;
  padding: 14px 10px 12px;
  cursor: pointer; text-align: center;
  display: flex; flex-direction: column; gap: 3px; align-items: center;
  transition: all 0.25s var(--flair-ease);
}
.tier:hover { border-color: #f59e0b; transform: translateY(-2px); box-shadow: 0 10px 20px -12px rgba(245, 158, 11, 0.6); }
.tier.on {
  border-color: #f59e0b;
  background: linear-gradient(160deg, rgba(245, 158, 11, 0.12), rgba(251, 191, 36, 0.05));
  box-shadow: 0 10px 22px -12px rgba(245, 158, 11, 0.7);
}
.tier.on::before {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(rgba(245, 158, 11, 0.22) 1px, transparent 1px);
  background-size: 8px 8px;
  -webkit-mask-image: radial-gradient(closest-side at 80% 10%, #000, transparent);
  mask-image: radial-gradient(closest-side at 80% 10%, #000, transparent);
}
.tier-amt { font-size: 24px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; line-height: 1.1; }
.tier-amt i { font-style: normal; font-size: 0.5em; margin-left: 2px; color: #f59e0b; }
.tier-bonus { font-size: 11.5px; font-weight: 700; color: #d97706; }
.tier-bonus.zero { color: var(--art-gray-500); font-weight: 500; }
.tier-get { font-size: 11px; color: var(--art-gray-500); }
.tier-check { position: absolute; right: 7px; top: 7px; color: #f59e0b; font-size: 16px; }
html.dark .tier-bonus { color: #ffc14d; }

.custom-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 4px; }
.custom-row .ap-field-label { margin: 0; }
.custom-row .el-input-number { width: 170px; }
.custom-bonus { font-size: 11.5px; color: #d97706; font-weight: 600; }
.custom-bonus.muted { color: var(--art-gray-500); font-weight: 400; }
html.dark .custom-bonus { color: #ffc14d; }

/* 通道 */
.channel-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 120px), 1fr)); gap: 10px; }
.channel {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  border: 1.5px solid var(--default-border);
  background: var(--default-box-color);
  border-radius: 12px; padding: 11px 12px;
  cursor: pointer; font-size: 13px; font-weight: 600;
  color: var(--art-gray-700);
  transition: all 0.24s var(--flair-ease);
}
.channel .el-icon { color: var(--cc); }
.channel:hover { border-color: var(--cc); transform: translateY(-2px); }
.channel.on {
  border-color: var(--cc); color: var(--art-gray-900);
  background: color-mix(in srgb, var(--cc) 9%, transparent);
  box-shadow: 0 8px 18px -12px var(--cc);
}
.ch-dot {
  margin-left: auto; width: 14px; height: 14px; border-radius: 50%;
  border: 2px solid var(--default-border); flex-shrink: 0;
  transition: all 0.2s;
}
.channel.on .ch-dot { border-color: var(--cc); background: var(--cc); box-shadow: inset 0 0 0 2.5px var(--default-box-color); }

/* 结算 */
.checkout {
  margin-top: 18px; padding: 15px 16px;
  border-radius: 14px;
  border: 1px dashed color-mix(in srgb, #f59e0b 45%, transparent);
  background: linear-gradient(150deg, rgba(245, 158, 11, 0.07), rgba(251, 191, 36, 0.02));
}
.co-line { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: var(--art-gray-600); padding: 3px 0; }
.co-line b { color: var(--art-gray-900); font-size: 14px; }
.co-line.bonus b { color: #d97706; }
html.dark .co-line.bonus b { color: #ffc14d; }
.co-total {
  display: flex; justify-content: space-between; align-items: baseline;
  margin: 9px 0 13px; padding-top: 10px;
  border-top: 1px solid color-mix(in srgb, #f59e0b 28%, transparent);
  font-size: 13px; color: var(--art-gray-700); font-weight: 600;
}
.co-total b { font-size: 27px; font-weight: 800; color: #b45309; font-variant-numeric: tabular-nums; }
html.dark .co-total b { color: #fbbf24; }
.checkout .el-button { width: 100%; }
.co-tip { margin-top: 9px; font-size: 11px; color: var(--art-gray-500); line-height: 1.6; text-align: center; }

/* 规则 */
.rule-list { margin: 0; padding-left: 18px; display: flex; flex-direction: column; gap: 9px; }
.rule-list li { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
.rule-list b { color: var(--art-gray-900); margin-right: 4px; }

.card-amt { margin-left: auto; font-size: 15px; }

/* 收银台 */
.cashier { display: flex; flex-direction: column; gap: 14px; }
.cash-head {
  display: flex; align-items: center; gap: 11px;
  padding: 13px 14px; border-radius: 12px;
  background: color-mix(in srgb, var(--cc) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--cc) 32%, transparent);
}
.cash-head .el-icon { color: var(--cc); }
.cash-head b { display: block; font-size: 14px; color: var(--art-gray-900); }
.cash-head span { display: block; font-size: 11.5px; color: var(--art-gray-500); margin-top: 2px; }
.cash-head .el-tag { margin-left: auto; }
.cash-amount { text-align: center; padding: 6px 0 2px; }
.cash-amount span { display: block; font-size: 12px; color: var(--art-gray-500); letter-spacing: 1px; }
.cash-amount b { display: block; font-size: 40px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; line-height: 1.2; }
.cash-amount i { font-style: normal; font-size: 0.45em; margin-right: 3px; color: #f59e0b; }
.cash-kv { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 14px; }
.cash-kv label { display: block; font-size: 11px; color: var(--art-gray-500); letter-spacing: 0.5px; }
.cash-kv span { display: block; font-size: 14px; font-weight: 700; color: var(--art-gray-900); margin-top: 2px; }

@media (max-width: 560px) {
  .custom-row .el-input-number { width: 100%; }
  .cash-amount b { font-size: 32px; }
}
</style>