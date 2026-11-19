<template>
  <div class="ap">
    <PageHero tone="wallet" icon="Coin" chip="PLATFORM TREASURY" live
      title="资金账务中心"
      sub="平台全部钱包账户、充值订单与收支流水的总账视图。支持人工补入 / 扣减、账户冻结解冻，所有调整都会写入流水并记录操作管理员。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="reload">刷新总账</el-button>
        <el-button type="default" :icon="Money" @click="openAdjust()">账务调整</el-button>
      </template>
    </PageHero>

    <div class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </div>

    <div class="ap-row wide-left">
      <PanelCard title="近 14 日资金流向" note="充值 / 消费 / 收入与退款" icon="TrendCharts" accent="wallet" :index="1">
        <div ref="flowRef" class="ap-chart" />
      </PanelCard>
      <PanelCard title="流水类型分布" note="按笔数与金额" icon="Histogram" accent="amber" :index="2">
        <div ref="typeRef" class="ap-chart sm" />
        <div class="type-sum">
          <div v-for="t in typeRows" :key="t.label" class="ts-row" :style="{ '--nc': t.color }">
            <i /><span>{{ t.label }}</span><b>{{ t.value }} 笔</b><em>{{ money(t.amount) }}</em>
          </div>
        </div>
      </PanelCard>
    </div>

    <!-- 钱包账户 -->
    <PanelCard title="钱包账户" :note="`共 ${accTotal} 个账户`" icon="Wallet" accent="wallet" :index="3">
      <template #extra>
        <el-tag v-if="summary.frozen" type="danger" effect="light" round>冻结 {{ summary.frozen }} 个</el-tag>
      </template>
      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>账户检索</label>
          <el-input v-model="accQuery.keyword" placeholder="学号 / 姓名 / 手机号" clearable :prefix-icon="Search"
                    @keyup.enter="searchAcc" @clear="searchAcc" />
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="searchAcc">查询</el-button>
          <el-button :icon="RefreshLeft" @click="accQuery.keyword = ''; searchAcc()">重置</el-button>
        </div>
      </div>

      <div v-loading="accLoading" class="ap-table-wrap">
        <table class="ap-table">
          <thead>
            <tr>
              <SortTh prop="realName" label="账户" width="190px" :sort="accSorter" @sort="searchAcc" />
              <th style="width:120px">角色</th>
              <SortTh prop="balance" label="可用余额" width="130px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="totalRecharge" label="累计充值" width="120px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="totalSpent" label="累计支出" width="120px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="totalEarned" label="跑腿收入" width="120px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="status" label="钱包状态" width="100px" :sort="accSorter" @sort="searchAcc" />
              <th style="width:190px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in accounts" :key="a.userId">
              <td>
                <b class="ap-strong">{{ a.realName }}</b>
                <div class="ap-sub ap-mono">{{ a.username }} · {{ a.phone || '—' }}</div>
              </td>
              <td><span class="role-txt">{{ roleText(a.roles) }}</span></td>
              <td><span class="ap-money bare" :class="{ zero: Number(a.balance) === 0 }">{{ money(a.balance) }}</span></td>
              <td class="ap-mono">{{ money(a.totalRecharge) }}</td>
              <td class="ap-mono">{{ money(a.totalSpent) }}</td>
              <td class="ap-mono">{{ money(a.totalEarned) }}</td>
              <td>
                <el-tag size="small" :type="a.status === 1 ? 'success' : 'danger'" effect="light">
                  {{ a.status === 1 ? '正常' : '已冻结' }}
                </el-tag>
              </td>
              <td>
                <div class="ap-ops">
                  <el-button text type="primary" size="small" :icon="Tickets" @click="viewTx(a)">流水</el-button>
                  <el-button text type="warning" size="small" :icon="EditPen" @click="openAdjust(a)">调整</el-button>
                  <el-button text :type="a.status === 1 ? 'danger' : 'success'" size="small" :icon="a.status === 1 ? Lock : Unlock" @click="toggleFreeze(a)">
                    {{ a.status === 1 ? '冻结' : '解冻' }}
                  </el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!accounts.length && !accLoading">
              <td colspan="8"><div class="ap-empty"><el-empty description="没有匹配的钱包账户" :image-size="80" /></div></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-loading="accLoading" class="ap-cards">
        <article v-for="a in accounts" :key="a.userId" class="ap-card-item" :style="{ '--ci': a.status === 1 ? '#f59e0b' : '#fb7185' }">
          <div class="ap-card-top">
            <b class="ap-strong">{{ a.realName }}<span class="ap-sub ap-mono"> {{ a.username }}</span></b>
            <el-tag size="small" :type="a.status === 1 ? 'success' : 'danger'" effect="light">{{ a.status === 1 ? '正常' : '已冻结' }}</el-tag>
          </div>
          <div class="card-balance"><span class="ap-money bare">{{ money(a.balance) }}</span><em>可用余额</em></div>
          <div class="ap-card-grid">
            <div><label>累计充值</label><span class="ap-mono">{{ money(a.totalRecharge) }}</span></div>
            <div><label>累计支出</label><span class="ap-mono">{{ money(a.totalSpent) }}</span></div>
            <div><label>跑腿收入</label><span class="ap-mono">{{ money(a.totalEarned) }}</span></div>
            <div><label>角色</label><span>{{ roleText(a.roles) }}</span></div>
          </div>
          <div class="ap-card-actions">
            <el-button size="small" type="primary" plain :icon="Tickets" @click="viewTx(a)">流水</el-button>
            <el-button size="small" type="warning" plain :icon="EditPen" @click="openAdjust(a)">调整</el-button>
            <el-button size="small" :type="a.status === 1 ? 'danger' : 'success'" plain :icon="a.status === 1 ? Lock : Unlock" @click="toggleFreeze(a)">
              {{ a.status === 1 ? '冻结' : '解冻' }}
            </el-button>
          </div>
        </article>
        <div v-if="!accounts.length && !accLoading" class="ap-empty"><el-empty description="没有匹配的钱包账户" :image-size="80" /></div>
      </div>

      <template #foot>
        <div class="ap-pager">
          <el-pagination background layout="total, sizes, prev, pager, next" :total="accTotal"
                         v-model:current-page="accQuery.page" v-model:page-size="accQuery.size"
                         :page-sizes="[10, 20, 50]" @current-change="loadAccounts" @size-change="searchAcc" />
        </div>
      </template>
    </PanelCard>

    <!-- 收支流水 -->
    <div ref="txRef">
      <PanelCard title="平台收支流水" :note="`共 ${txTotal} 笔`" icon="Tickets" accent="mint" :index="4">
        <template #extra>
          <el-tag v-if="txQuery.userId" closable type="primary" effect="light" @close="clearUserFilter">
            仅看 {{ userFilterName }}
          </el-tag>
        </template>
        <div class="ap-toolbar">
          <div class="ap-field ap-grow">
            <label>关键词</label>
            <el-input v-model="txQuery.keyword" placeholder="标题 / 备注 / 学号 / 姓名" clearable :prefix-icon="Search"
                      @keyup.enter="searchTx" @clear="searchTx" />
          </div>
          <div class="ap-field">
            <label>流水类型</label>
            <el-select v-model="txQuery.type" placeholder="全部类型" clearable style="width:150px" @change="searchTx">
              <el-option v-for="(v, k) in WALLET_TX" :key="k" :label="v.label" :value="k" />
            </el-select>
          </div>
          <div class="ap-actions">
            <el-button type="primary" :icon="Search" @click="searchTx">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetTx">重置</el-button>
          </div>
        </div>

        <div v-loading="txLoading" class="ap-table-wrap">
          <table class="ap-table">
            <thead>
              <tr>
                <SortTh prop="createdAt" label="时间" width="150px" :sort="txSorter" @sort="searchTx" />
                <SortTh prop="type" label="类型" width="110px" :sort="txSorter" @sort="searchTx" />
                <SortTh prop="amount" label="金额" width="130px" :sort="txSorter" @sort="searchTx" />
                <SortTh prop="balanceAfter" label="变动后余额" width="120px" :sort="txSorter" @sort="searchTx" />
                <SortTh prop="title" label="事项" :sort="txSorter" @sort="searchTx" />
                <SortTh prop="channel" label="渠道" width="120px" :sort="txSorter" @sort="searchTx" />
                <th style="width:150px">账户</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in txs" :key="t.id">
                <td class="ap-mono">{{ fmtTime(t.createdAt) }}</td>
                <td><el-tag size="small" :type="dict(WALLET_TX, t.type).type" effect="light">{{ dict(WALLET_TX, t.type).label }}</el-tag></td>
                <td><span class="ap-money" :class="txClass(t)">{{ money(Math.abs(Number(t.amount))) }}</span></td>
                <td class="ap-mono">{{ money(t.balanceAfter) }}</td>
                <td>
                  <b class="ap-strong">{{ t.title }}</b>
                  <div v-if="t.remark" class="ap-sub ap-ellipsis">{{ t.remark }}</div>
                </td>
                <td><span class="ch-cell" :style="{ '--nc': channelColor(t.channel) }">{{ channelLabel(t.channel) }}</span></td>
                <td class="ap-mono">{{ t.userId }}</td>
              </tr>
              <tr v-if="!txs.length && !txLoading">
                <td colspan="7"><div class="ap-empty"><el-empty description="没有匹配的流水" :image-size="80" /></div></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-loading="txLoading" class="ap-cards">
          <article v-for="t in txs" :key="t.id" class="ap-card-item" :style="{ '--ci': channelColor(t.channel) }">
            <div class="ap-card-top">
              <el-tag size="small" :type="dict(WALLET_TX, t.type).type" effect="light">{{ dict(WALLET_TX, t.type).label }}</el-tag>
              <span class="ap-money" :class="txClass(t)">{{ money(Math.abs(Number(t.amount))) }}</span>
            </div>
            <b class="ap-strong">{{ t.title }}</b>
            <div class="ap-card-grid">
              <div><label>时间</label><span class="ap-mono">{{ fmtTime(t.createdAt) }}</span></div>
              <div><label>变动后余额</label><span class="ap-mono">{{ money(t.balanceAfter) }}</span></div>
              <div><label>渠道</label><span>{{ channelLabel(t.channel) }}</span></div>
              <div><label>账户 ID</label><span class="ap-mono">{{ t.userId }}</span></div>
            </div>
            <p v-if="t.remark" class="card-remark">{{ t.remark }}</p>
          </article>
          <div v-if="!txs.length && !txLoading" class="ap-empty"><el-empty description="没有匹配的流水" :image-size="80" /></div>
        </div>

        <template #foot>
          <div class="ap-pager">
            <el-pagination background layout="total, sizes, prev, pager, next" :total="txTotal"
                           v-model:current-page="txQuery.page" v-model:page-size="txQuery.size"
                           :page-sizes="[10, 20, 50]" @current-change="loadTx" @size-change="searchTx" />
          </div>
        </template>
      </PanelCard>
    </div>

    <!-- 充值订单 -->
    <PanelCard title="充值订单" :note="`共 ${ordTotal} 单`" icon="ShoppingCart" accent="amber" :index="5">
      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="ordQuery.keyword" placeholder="订单号 / 学号 / 姓名" clearable :prefix-icon="Search"
                    @keyup.enter="searchOrd" @clear="searchOrd" />
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="searchOrd">查询</el-button>
        </div>
      </div>
      <div class="ap-seg">
        <button :class="{ on: !ordQuery.status }" @click="pickOrdStatus('')">全部<span class="n">{{ ordTotal }}</span></button>
        <button v-for="(v, k) in RECHARGE_STATUS" :key="k" :class="{ on: ordQuery.status === k }" @click="pickOrdStatus(k)">{{ v.label }}</button>
      </div>

      <div v-loading="ordLoading" class="ap-table-wrap">
        <table class="ap-table">
          <thead>
            <tr>
              <SortTh prop="orderNo" label="订单号" width="190px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="realName" label="用户" width="160px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="amount" label="充值金额" width="110px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="bonus" label="赠送" width="100px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="channel" label="渠道" width="130px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="status" label="状态" width="100px" :sort="ordSorter" @sort="searchOrd" />
              <SortTh prop="createdAt" label="创建时间" width="150px" :sort="ordSorter" @sort="searchOrd" />
              <th style="width:150px">到账 / 失效</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td class="ap-mono">{{ o.orderNo }}</td>
              <td><b class="ap-strong">{{ o.realName }}</b><div class="ap-sub ap-mono">{{ o.username }}</div></td>
              <td><span class="ap-money bare">{{ money(o.amount) }}</span></td>
              <td class="ap-mono" :class="{ bonus: Number(o.bonus) > 0 }">{{ Number(o.bonus) > 0 ? '+' + money(o.bonus) : '—' }}</td>
              <td><span class="ch-cell" :style="{ '--nc': channelColor(o.channel) }">{{ channelLabel(o.channel) }}</span></td>
              <td><el-tag size="small" :type="dict(RECHARGE_STATUS, o.status).type" effect="light">{{ dict(RECHARGE_STATUS, o.status).label }}</el-tag></td>
              <td class="ap-mono">{{ fmtTime(o.createdAt) }}</td>
              <td class="ap-mono">{{ o.status === 'PAID' ? fmtTime(o.paidAt) : fmtTime(o.expireAt) }}</td>
            </tr>
            <tr v-if="!orders.length && !ordLoading">
              <td colspan="8"><div class="ap-empty"><el-empty description="没有匹配的充值订单" :image-size="80" /></div></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-loading="ordLoading" class="ap-cards">
        <article v-for="o in orders" :key="o.id" class="ap-card-item" :style="{ '--ci': channelColor(o.channel) }">
          <div class="ap-card-top">
            <span class="ap-mono ap-sub">{{ o.orderNo }}</span>
            <el-tag size="small" :type="dict(RECHARGE_STATUS, o.status).type" effect="light">{{ dict(RECHARGE_STATUS, o.status).label }}</el-tag>
          </div>
          <div class="card-balance">
            <span class="ap-money bare">{{ money(o.amount) }}</span>
            <em v-if="Number(o.bonus) > 0">赠 {{ money(o.bonus) }}</em>
          </div>
          <div class="ap-card-grid">
            <div><label>用户</label><span>{{ o.realName }}（{{ o.username }}）</span></div>
            <div><label>渠道</label><span>{{ channelLabel(o.channel) }}</span></div>
            <div><label>创建</label><span class="ap-mono">{{ fmtTime(o.createdAt) }}</span></div>
            <div><label>{{ o.status === 'PAID' ? '到账' : '失效' }}</label><span class="ap-mono">{{ fmtTime(o.status === 'PAID' ? o.paidAt : o.expireAt) }}</span></div>
          </div>
        </article>
        <div v-if="!orders.length && !ordLoading" class="ap-empty"><el-empty description="没有匹配的充值订单" :image-size="80" /></div>
      </div>

      <template #foot>
        <div class="ap-pager">
          <el-pagination background layout="total, sizes, prev, pager, next" :total="ordTotal"
                         v-model:current-page="ordQuery.page" v-model:page-size="ordQuery.size"
                         :page-sizes="[10, 20, 50]" @current-change="loadOrders" @size-change="searchOrd" />
        </div>
      </template>
    </PanelCard>

    <!-- 账务调整 -->
    <el-dialog v-model="adjustOpen" title="人工账务调整" width="520px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>调整账户<em>*</em></label>
          <el-select v-model="adjust.userId" filterable remote reserve-keyword placeholder="搜索学号 / 姓名 / 手机号"
                     :remote-method="searchAccounts" :loading="accSearchLoading" style="width:100%" @change="onAdjustUser">
            <el-option v-for="a in accOptions" :key="a.userId" :label="`${a.realName}（${a.username}）`" :value="a.userId">
              <div class="ao-line"><b>{{ a.realName }}</b><span>{{ a.username }} · 余额 {{ money(a.balance) }}</span></div>
            </el-option>
          </el-select>
        </div>
        <div v-if="adjustAccount" class="ap-kv adj-kv">
          <div><label>当前余额</label><span class="ap-money bare">{{ money(adjustAccount.balance) }}</span></div>
          <div><label>钱包状态</label><span>{{ adjustAccount.status === 1 ? '正常' : '已冻结' }}</span></div>
          <div><label>累计充值</label><span>{{ money(adjustAccount.totalRecharge) }}</span></div>
          <div><label>累计支出</label><span>{{ money(adjustAccount.totalSpent) }}</span></div>
        </div>
        <div class="ap-field-row">
          <label>调整方向<em>*</em></label>
          <div class="dir-pick">
            <button type="button" :class="{ on: adjust.dir === 1 }" style="--nc:#10d9b5" @click="adjust.dir = 1">
              <el-icon :size="15"><Top /></el-icon>补入余额
            </button>
            <button type="button" :class="{ on: adjust.dir === -1 }" style="--nc:#fb7185" @click="adjust.dir = -1">
              <el-icon :size="15"><Bottom /></el-icon>扣减余额
            </button>
          </div>
        </div>
        <div class="ap-field-row">
          <label>调整金额<em>*</em></label>
          <el-input-number v-model="adjust.amount" :min="0.01" :max="10000" :precision="2" :step="10"
                           controls-position="right" style="width:100%" />
          <p class="ap-form-tip">单次调整上限 10000 元；扣减不得超过账户当前余额。调整不计入充值 / 消费统计。</p>
        </div>
        <div class="ap-field-row">
          <label>调整事由<em>*</em></label>
          <el-input v-model="adjust.remark" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：活动奖励补发 / 误充值退回 / 违规扣减，将永久写入流水备注" />
        </div>
        <div class="adj-preview" :class="adjust.dir === 1 ? 'in' : 'out'">
          <span>调整后余额</span>
          <b>{{ money(previewBalance) }}</b>
        </div>
      </div>
      <template #footer>
        <el-button @click="adjustOpen = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" :icon="Check" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search, Money, Check, Top, Bottom, Tickets, EditPen, Lock, Unlock } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import SortTh from '../../components/SortTh.vue'
import {
  adminWalletOverview, adminWalletAccounts, adminWalletTransactions,
  adminRechargeOrders, adminWalletAdjust, adminWalletStatus
} from '../../api'
import { WALLET_TX, RECHARGE_STATUS, PAY_CHANNEL, dict, fmtTime, money } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { useChart, useChartStyle } from '../../composables/useChart'

const { isDark, chartText, headText, axisStyle, tipStyle, tipRow, area } = useChartStyle()

const ov = ref({ summary: {}, daily: [], types: [] })
const loading = ref(false)
const flowRef = ref(null)
const typeRef = ref(null)
const txRef = ref(null)

const summary = computed(() => ov.value.summary || {})
const daily = computed(() => ov.value.daily || [])
const TYPE_COLOR = { RECHARGE: '#f59e0b', CONSUME: '#5b7cfa', REFUND: '#10d9b5', EARN: '#34d399', ADJUST: '#8b5cf6' }
const typeRows = computed(() => (ov.value.types || []).map(t => ({
  name: t.name, value: Number(t.value), amount: Number(t.amount),
  label: dict(WALLET_TX, t.name).label, color: TYPE_COLOR[t.name] || '#7987a1'
})))

const heroStats = computed(() => [
  { label: '平台沉淀余额', value: Number(summary.value.balance || 0), decimals: 2, unit: '元' },
  { label: '累计充值', value: Number(summary.value.recharge || 0), decimals: 2, unit: '元' },
  { label: '平台佣金', value: Number(summary.value.commission || 0), decimals: 2, unit: '元' },
  { label: '钱包账户', value: Number(summary.value.accounts || 0), unit: '个' }
])
const metrics = computed(() => [
  { label: '钱包账户总数', value: Number(summary.value.accounts || 0), foot: `冻结 ${summary.value.frozen || 0} 个`, color: '#f59e0b', icon: 'Wallet' },
  { label: '平台沉淀余额', value: Number(summary.value.balance || 0), decimals: 2, unit: '元', foot: '全部账户可用余额合计', color: '#5b7cfa', icon: 'Coin' },
  { label: '累计充值入账', value: Number(summary.value.recharge || 0), decimals: 2, unit: '元', foot: `今日 ${money(summary.value.todayRecharge)}`, color: '#10d9b5', icon: 'Top' },
  { label: '累计服务支出', value: Number(summary.value.spent || 0), decimals: 2, unit: '元', foot: `今日 ${money(summary.value.todayConsume)}`, color: '#8b5cf6', icon: 'Bottom' },
  { label: '跑腿收入合计', value: Number(summary.value.earned || 0), decimals: 2, unit: '元', foot: '含退款回账', color: '#0ea5e9', icon: 'Bicycle' },
  { label: '待支付充值单', value: Number(summary.value.pendingOrders || 0), foot: '15 分钟未支付自动过期', color: '#fb7185', icon: 'AlarmClock' }
])

function roleText(roles) {
  const map = { ADMIN: '管理员', STAFF: '驿站员工', STUDENT: '学生', COURIER: '快递员' }
  return String(roles || '').split(',').filter(Boolean).map(r => map[r] || r).join(' / ') || '—'
}
function channelLabel(c) { return PAY_CHANNEL[c]?.label || c || '—' }
function channelColor(c) { return PAY_CHANNEL[c]?.color || '#7987a1' }
function txClass(t) {
  const sign = WALLET_TX[t.type]?.sign ?? (Number(t.amount) >= 0 ? 1 : -1)
  return sign > 0 ? 'in' : sign < 0 ? 'out' : 'bare'
}

/* ---- 图表 ---- */
useChart(flowRef, chart => {
  const d = daily.value
  const dark = isDark()
  const ax = axisStyle()
  const series = [
    { key: 'recharge', name: '充值', color: '#f59e0b', rgb: '245,158,11' },
    { key: 'consume', name: '消费', color: '#5b7cfa', rgb: '91,124,250' },
    { key: 'earn', name: '收入/退款', color: '#10d9b5', rgb: '16,217,181' }
  ]
  chart.setOption({
    tooltip: {
      trigger: 'axis', ...tipStyle(),
      axisPointer: { type: 'line', lineStyle: { color: dark ? '#4a4a58' : '#c9d2e0', type: [3, 3] } },
      formatter: ps => `<div style="font-size:11.5px;opacity:0.6;">${ps[0]?.axisValue ?? ''}</div>` +
        ps.map(p => tipRow(p.color, p.seriesName, `${money(p.value)}`)).join('')
    },
    legend: { data: series.map(s => s.name), top: 0, right: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, itemGap: 16, textStyle: { color: chartText(), fontSize: 12 } },
    grid: { left: 8, right: 20, top: 34, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category', boundaryGap: false, data: d.map(x => x.day),
      axisLabel: { ...ax.axisLabel, hideOverlap: true },
      axisLine: { lineStyle: { color: dark ? '#2c2c36' : '#eceff3' } }, axisTick: ax.axisTick
    },
    yAxis: { type: 'value', min: 0, splitNumber: 4, ...ax, axisLabel: { ...ax.axisLabel, formatter: v => v >= 1000 ? (v / 1000) + 'k' : v } },
    animationDuration: 1100, animationEasing: 'cubicOut',
    series: series.map(s => ({
      name: s.name, type: 'line', smooth: 0.4, symbol: 'circle', symbolSize: 7, showSymbol: false,
      data: d.map(x => Number(x[s.key] || 0)),
      itemStyle: { color: s.color, borderColor: dark ? '#161618' : '#fff', borderWidth: 2 },
      lineStyle: { width: 2.5 }, areaStyle: { color: area(s.rgb) }, emphasis: { focus: 'series' }
    }))
  }, true)
})

useChart(typeRef, chart => {
  const rows = [...typeRows.value].reverse()
  const dark = isDark()
  chart.setOption({
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' }, ...tipStyle(),
      formatter: ps => {
        const r = typeRows.value.find(x => x.label === ps[0]?.name)
        return tipRow(r?.color || '#7987a1', ps[0]?.name, `${ps[0]?.value} 笔`) +
          `<div style="font-size:11.5px;opacity:0.6;margin-top:4px;text-align:right;">金额 ${money(r?.amount || 0)}</div>`
      }
    },
    grid: { left: 4, right: 46, top: 8, bottom: 4, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, ...axisStyle(), axisLabel: { show: false }, splitLine: { show: false } },
    yAxis: {
      type: 'category', data: rows.map(r => r.label),
      axisLabel: { color: chartText(), fontSize: 12 }, axisLine: { show: false }, axisTick: { show: false }
    },
    animationDuration: 900, animationEasing: 'cubicOut',
    series: [{
      type: 'bar', barWidth: 15, data: rows.map(r => ({ value: r.value, itemStyle: { color: r.color, borderRadius: [0, 7, 7, 0] } })),
      label: { show: true, position: 'right', color: headText(), fontSize: 12, fontWeight: 700, formatter: '{c}' },
      itemStyle: { shadowBlur: dark ? 10 : 0, shadowColor: 'rgba(0,0,0,0.3)' }
    }]
  }, true)
})

/* ---- 账户 ---- */
const accounts = ref([])
const accTotal = ref(0)
const accLoading = ref(false)
const accQuery = reactive({ page: 1, size: 10, keyword: '' })
const accSorter = useServerSort('updatedAt')
async function loadAccounts() {
  accLoading.value = true
  try {
    const r = await adminWalletAccounts({ page: accQuery.page, size: accQuery.size, keyword: accQuery.keyword?.trim() || undefined, ...accSorter.sortParams() })
    accounts.value = r.records || []
    accTotal.value = Number(r.total || 0)
  } finally { accLoading.value = false }
}
function searchAcc() { accQuery.page = 1; loadAccounts() }

/* ---- 流水 ---- */
const txs = ref([])
const txTotal = ref(0)
const txLoading = ref(false)
const txQuery = reactive({ page: 1, size: 10, userId: null, type: '', keyword: '' })
const txSorter = useServerSort('createdAt')
const userFilterName = ref('')
async function loadTx() {
  txLoading.value = true
  try {
    const r = await adminWalletTransactions({
      page: txQuery.page, size: txQuery.size,
      userId: txQuery.userId || undefined, type: txQuery.type || undefined,
      keyword: txQuery.keyword?.trim() || undefined,
      ...txSorter.sortParams()
    })
    txs.value = r.records || []
    txTotal.value = Number(r.total || 0)
  } finally { txLoading.value = false }
}
function searchTx() { txQuery.page = 1; loadTx() }
function resetTx() { txQuery.userId = null; txQuery.type = ''; txQuery.keyword = ''; userFilterName.value = ''; searchTx() }
function clearUserFilter() { txQuery.userId = null; userFilterName.value = ''; searchTx() }
function viewTx(a) {
  txQuery.userId = a.userId
  userFilterName.value = `${a.realName}（${a.username}）`
  searchTx()
  txRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* ---- 充值订单 ---- */
const orders = ref([])
const ordTotal = ref(0)
const ordLoading = ref(false)
const ordQuery = reactive({ page: 1, size: 10, status: '', keyword: '' })
const ordSorter = useServerSort('createdAt')
async function loadOrders() {
  ordLoading.value = true
  try {
    const r = await adminRechargeOrders({
      page: ordQuery.page, size: ordQuery.size,
      status: ordQuery.status || undefined, keyword: ordQuery.keyword?.trim() || undefined,
      ...ordSorter.sortParams()
    })
    orders.value = r.records || []
    ordTotal.value = Number(r.total || 0)
  } finally { ordLoading.value = false }
}
function searchOrd() { ordQuery.page = 1; loadOrders() }
function pickOrdStatus(s) { ordQuery.status = ordQuery.status === s ? '' : s; searchOrd() }

/* ---- 冻结 ---- */
async function toggleFreeze(a) {
  const freeze = a.status === 1
  try {
    await ElMessageBox.confirm(
      freeze
        ? `冻结后 ${a.realName} 将无法充值、发布代取或支付，已有余额保留。确定冻结该钱包吗？`
        : `解冻后 ${a.realName} 可恢复正常充值与消费。确定解冻吗？`,
      freeze ? '冻结钱包' : '解冻钱包', { type: 'warning', confirmButtonText: freeze ? '确认冻结' : '确认解冻' }
    )
  } catch { return }
  try {
    await adminWalletStatus(a.userId, freeze ? 0 : 1)
    ElMessage.success(freeze ? '钱包已冻结' : '钱包已解冻')
    await Promise.all([loadAccounts(), loadOverview()])
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

/* ---- 账务调整 ---- */
const adjustOpen = ref(false)
const adjusting = ref(false)
const accSearchLoading = ref(false)
const accOptions = ref([])
const adjust = reactive({ userId: null, dir: 1, amount: 10, remark: '' })
const adjustAccount = computed(() => accOptions.value.find(a => a.userId === adjust.userId) || null)
const previewBalance = computed(() => {
  const base = Number(adjustAccount.value?.balance || 0)
  return Math.max(0, base + adjust.dir * Number(adjust.amount || 0))
})
async function searchAccounts(kw) {
  accSearchLoading.value = true
  try {
    const r = await adminWalletAccounts({ page: 1, size: 20, keyword: kw?.trim() || undefined })
    accOptions.value = r.records || []
  } finally { accSearchLoading.value = false }
}
async function openAdjust(a) {
  adjustOpen.value = true
  adjust.dir = 1; adjust.amount = 10; adjust.remark = ''
  if (a) {
    adjust.userId = a.userId
    accOptions.value = [a]
  } else {
    adjust.userId = null
    await searchAccounts('')
  }
}
function onAdjustUser() { adjust.remark = '' }
async function submitAdjust() {
  if (!adjust.userId) return ElMessage.warning('请选择调整账户')
  if (!adjust.amount || adjust.amount <= 0) return ElMessage.warning('请输入调整金额')
  if (!adjust.remark.trim()) return ElMessage.warning('请填写调整事由')
  if (adjust.dir === -1 && adjust.amount > Number(adjustAccount.value?.balance || 0)) {
    return ElMessage.warning('扣减金额不得超过当前余额')
  }
  adjusting.value = true
  try {
    await adminWalletAdjust({
      userId: adjust.userId,
      amount: adjust.dir * Number(adjust.amount),
      remark: adjust.remark.trim()
    })
    ElMessage.success('账务调整已入账')
    adjustOpen.value = false
    await Promise.all([loadOverview(), loadAccounts(), loadTx()])
  } catch (e) { ElMessage.error(e?.message || '调整失败') } finally { adjusting.value = false }
}

async function loadOverview() { ov.value = await adminWalletOverview() }
async function reload() {
  loading.value = true
  try { await Promise.all([loadOverview(), loadAccounts(), loadTx(), loadOrders()]); ElMessage.success('总账已刷新') }
  finally { loading.value = false }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadAccounts(), loadTx(), loadOrders(), searchAccounts('')])
})
</script>

<style scoped>
.type-sum { display: flex; flex-direction: column; gap: 5px; margin-top: 10px; }
.ts-row { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--art-gray-600); }
.ts-row i { width: 8px; height: 8px; border-radius: 3px; background: var(--nc); flex-shrink: 0; }
.ts-row span { flex: 1; }
.ts-row b { color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.ts-row em { font-style: normal; color: var(--art-gray-500); font-variant-numeric: tabular-nums; min-width: 82px; text-align: right; }

.role-txt { font-size: 12px; color: var(--art-gray-600); }
.ch-cell { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 600; color: var(--nc); }
.ch-cell::before { content: ''; width: 7px; height: 7px; border-radius: 50%; background: var(--nc); }
.ap-money.zero { color: var(--art-gray-500); }
.bonus { color: #0d9488; font-weight: 700; }
html.dark .bonus { color: #35e0c2; }

.card-balance { display: flex; align-items: baseline; gap: 9px; }
.card-balance .ap-money { font-size: 22px; }
.card-balance em { font-style: normal; font-size: 11.5px; color: var(--art-gray-500); }
.card-remark { margin: 0; font-size: 12px; line-height: 1.6; color: var(--art-gray-600); }

.ao-line { display: flex; flex-direction: column; line-height: 1.45; padding: 2px 0; }
.ao-line span { font-size: 11.5px; color: var(--art-gray-500); }
.adj-kv { padding: 11px 13px; border-radius: 11px; background: var(--art-gray-100); }
html.dark .adj-kv { background: #1a1a20; }
.dir-pick { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.dir-pick button {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 11px 8px; border-radius: 11px; cursor: pointer;
  border: 1.5px solid var(--default-border); background: var(--default-box-color);
  font-size: 13px; font-weight: 700; color: var(--art-gray-700);
  transition: all 0.22s var(--flair-ease);
}
.dir-pick button:hover { border-color: var(--nc); color: var(--nc); }
.dir-pick button.on {
  border-color: var(--nc); color: #fff;
  background: linear-gradient(135deg, var(--nc), color-mix(in srgb, var(--nc) 60%, #ffffff));
  box-shadow: 0 8px 18px -10px var(--nc);
}
.adj-preview {
  display: flex; align-items: center; justify-content: space-between;
  padding: 13px 16px; border-radius: 12px;
  font-size: 12.5px; font-weight: 600;
}
.adj-preview span { color: var(--art-gray-600); }
.adj-preview b { font-size: 22px; font-weight: 800; font-variant-numeric: tabular-nums; }
.adj-preview.in { background: rgba(16, 217, 181, 0.12); border: 1px solid rgba(16, 217, 181, 0.35); }
.adj-preview.in b { color: #0d9488; }
.adj-preview.out { background: rgba(251, 113, 133, 0.12); border: 1px solid rgba(251, 113, 133, 0.35); }
.adj-preview.out b { color: #e11d48; }
html.dark .adj-preview.in b { color: #35e0c2; }
html.dark .adj-preview.out b { color: #ff8fa3; }
</style>