<template>
  <div class="ap">
    <PageHero tone="wallet" icon="Present" chip="INVITE & REBATE" live title="邀请有礼"
      sub="把专属邀请码分享给同学，对方注册绑定后，其每笔充值都会按比例返利到您的钱包余额，实时到账、流水可查。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="DocumentCopy" @click="copyCode">复制邀请码</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </template>
    </PageHero>

    <div class="ap-row wide-left">
      <PanelCard title="我的邀请码" note="注册时填写即可绑定" icon="Postcard" accent="wallet" :index="1">
        <div class="invite-card">
          <div class="iv-shine" />
          <label>专属邀请码</label>
          <div class="iv-code ap-mono">{{ info.inviteCode || '—' }}</div>
          <p>同学在注册页「邀请码」一栏填写此码，即与您建立邀请关系。</p>
          <div class="iv-rate">充值返利 <b>{{ info.ratePercent ?? 10 }}%</b> · 实时入账</div>
        </div>
        <div class="ap-note" style="--nc:#f59e0b;margin-top:14px">
          <el-icon><InfoFilled /></el-icon>
          <div><b>返利规则：</b>被邀请人每完成一笔充值，系统按 {{ info.ratePercent ?? 10 }}% 自动折算返利入您的钱包；返利为站内虚拟余额，可用于代取、自寄与保险。</div>
        </div>
      </PanelCard>

      <div class="side-col">
        <PanelCard title="绑定邀请人" note="仅可绑定一次" icon="Link" accent="mint" :index="2">
          <template v-if="info.boundInviter">
            <div class="ap-kv">
              <div><label>我的邀请人</label><span class="ap-strong">{{ info.inviterName || '—' }}</span></div>
              <div><label>绑定状态</label><span><el-tag size="small" type="success" effect="light">已绑定</el-tag></span></div>
            </div>
            <div class="ap-note" style="--nc:#10d9b5;margin-top:12px">
              <el-icon><CircleCheck /></el-icon>
              <div>您已通过邀请注册，邀请关系不可更换。</div>
            </div>
          </template>
          <template v-else>
            <div class="ap-form">
              <div class="ap-field-row">
                <label>对方的邀请码<em>*</em></label>
                <el-input v-model="bindCode" placeholder="填写同学的专属邀请码" maxlength="16" clearable />
                <div class="ap-form-tip">绑定后不可更改；不能填写自己的邀请码。</div>
              </div>
              <el-button type="primary" :icon="Connection" :loading="binding" :disabled="!bindCode.trim()" @click="doBind">
                立即绑定
              </el-button>
            </div>
          </template>
        </PanelCard>

        <PanelCard title="邀请概览" icon="Odometer" accent="amber" :index="3">
          <div class="ap-kv">
            <div><label>已邀请同学</label><span class="ap-strong">{{ info.inviteeCount || 0 }} 人</span></div>
            <div><label>累计返利</label><span class="ap-strong">{{ money(info.totalRebate) }}</span></div>
            <div><label>返利比例</label><span>{{ info.ratePercent ?? 10 }}%</span></div>
          </div>
        </PanelCard>
      </div>
    </div>

    <PanelCard title="我邀请的同学" :note="`${invitees.length} 人`" icon="User" accent="parcel" :index="4">
      <div class="ap-table-wrap">
        <el-table :data="sortedInvitees" stripe :default-sort="ivSort.defaultSort" @sort-change="ivSort.onSortChange">
          <el-table-column label="姓名" prop="realName" sortable="custom" min-width="120">
            <template #default="{ row }"><span class="ap-strong">{{ row.realName }}</span></template>
          </el-table-column>
          <el-table-column label="学号" prop="username" sortable="custom" min-width="140">
            <template #default="{ row }"><span class="ap-mono">{{ row.username }}</span></template>
          </el-table-column>
          <el-table-column label="手机号" prop="phone" sortable="custom" min-width="140">
            <template #default="{ row }"><span class="ap-mono">{{ row.phone || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="状态" prop="status" sortable="custom" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" prop="createdAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in invitees" :key="row.id" class="ap-card-item" style="--ci:#5b7cfa">
          <div class="ap-card-top"><b>{{ row.realName }}</b><span class="ap-mono">{{ row.username }}</span></div>
          <div class="ap-card-grid">
            <div><label>手机号</label><span>{{ row.phone || '—' }}</span></div>
            <div><label>注册时间</label><span>{{ fmtTime(row.createdAt) }}</span></div>
          </div>
        </div>
        <div v-if="!invitees.length" class="ap-empty"><el-empty description="还没有同学通过您的邀请注册" :image-size="64" /></div>
      </div>
    </PanelCard>

    <PanelCard title="返利流水" :note="`${rebateTotal} 笔`" icon="Coin" accent="wallet" :index="5">
      <div class="ap-table-wrap">
        <el-table v-loading="rebateLoading" :data="rebates" stripe>
          <el-table-column label="被邀请人" min-width="130">
            <template #default="{ row }"><span class="ap-strong">{{ row.inviteeName }}</span><span class="ap-mono ap-sub2">（{{ row.inviteeUsername }}）</span></template>
          </el-table-column>
          <el-table-column label="充值单号" prop="orderNo" min-width="180">
            <template #default="{ row }"><span class="ap-mono">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column label="充值金额" min-width="120" align="right">
            <template #default="{ row }"><span class="ap-mono">{{ money(row.rechargeAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="返利比例" width="100" align="right">
            <template #default="{ row }"><span>{{ row.ratePercent }}%</span></template>
          </el-table-column>
          <el-table-column label="返利到账" min-width="120" align="right">
            <template #default="{ row }"><span class="ap-mono ap-strong rebate">+{{ money(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="到账时间" prop="createdAt" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.createdAt) }}</span></template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in rebates" :key="row.id" class="ap-card-item" style="--ci:#f59e0b">
          <div class="ap-card-top"><b>{{ row.inviteeName }}</b><span class="rebate ap-mono">+{{ money(row.amount) }}</span></div>
          <div class="ap-card-grid">
            <div><label>充值金额</label><span>{{ money(row.rechargeAmount) }}</span></div>
            <div><label>比例</label><span>{{ row.ratePercent }}%</span></div>
            <div><label>到账时间</label><span>{{ fmtTime(row.createdAt) }}</span></div>
          </div>
        </div>
        <div v-if="!rebates.length && !rebateLoading" class="ap-empty"><el-empty description="暂无返利记录" :image-size="64" /></div>
      </div>
      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="rebateTotal"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="loadRebates()" />
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Refresh, Connection } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { inviteInfo, inviteRecords, bindInviteCode } from '../../api'
import { fmtTime, money } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const info = ref({})
const loading = ref(false)
const binding = ref(false)
const bindCode = ref('')
const rebates = ref([])
const rebateTotal = ref(0)
const rebateLoading = ref(false)
const query = reactive({ page: 1, size: 10 })

const invitees = computed(() => info.value.invitees || [])
const ivSort = useTableSort('createdAt')
const sortedInvitees = computed(() => ivSort.sorted(invitees.value))

const heroStats = computed(() => [
  { label: '已邀请', value: Number(info.value.inviteeCount || 0), unit: '人' },
  { label: '累计返利', value: Number(info.value.totalRebate || 0), unit: '元', decimals: 2 },
  { label: '返利比例', value: Number(info.value.ratePercent ?? 10), unit: '%' }
])

async function loadAll() {
  loading.value = true
  try { info.value = await inviteInfo() } finally { loading.value = false }
  loadRebates()
}
async function loadRebates() {
  rebateLoading.value = true
  try {
    const data = await inviteRecords({ ...query })
    rebates.value = data.records
    rebateTotal.value = data.total
  } finally { rebateLoading.value = false }
}
async function doBind() {
  binding.value = true
  try {
    await bindInviteCode(bindCode.value.trim())
    ElMessage.success('邀请关系绑定成功')
    bindCode.value = ''
    await loadAll()
  } finally { binding.value = false }
}
async function copyCode() {
  if (!info.value.inviteCode) return
  try {
    await navigator.clipboard.writeText(info.value.inviteCode)
    ElMessage.success('邀请码已复制，快去分享给同学吧')
  } catch { ElMessage.warning('当前浏览器不支持自动复制，请手动选择邀请码') }
}
onMounted(loadAll)
</script>

<style scoped>
.side-col { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.ap-sub2 { font-size: 12px; color: var(--art-gray-600); }
.rebate { color: #0d9488; font-weight: 700; }
html.dark .rebate { color: #34d399; }

.invite-card {
  position: relative; overflow: hidden;
  border-radius: 18px; padding: 22px 24px;
  color: #fff; text-align: center;
  background: linear-gradient(135deg, #b45309 0%, #f59e0b 45%, #fbbf24 100%);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: 0 24px 50px -22px rgba(180, 83, 9, 0.7), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.invite-card::before {
  content: ''; position: absolute; width: 240px; height: 240px;
  left: -70px; bottom: -110px; pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.32) 1.5px, transparent 1.5px);
  background-size: 11px 11px;
  -webkit-mask-image: radial-gradient(closest-side, #000 20%, transparent 100%);
  mask-image: radial-gradient(closest-side, #000 20%, transparent 100%);
}
.iv-shine {
  position: absolute; top: 0; bottom: 0; width: 55%; left: -70%; pointer-events: none;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  transform: skewX(-18deg); animation: iv-shine 5.5s ease-in-out infinite;
}
@keyframes iv-shine { 0%, 60% { left: -70%; } 100% { left: 135%; } }
.invite-card label { position: relative; display: block; font-size: 10.5px; letter-spacing: 2px; opacity: 0.8; }
.iv-code { position: relative; font-size: 34px; font-weight: 800; letter-spacing: 6px; margin: 8px 0 6px; }
.invite-card p { position: relative; margin: 0; font-size: 12px; opacity: 0.88; line-height: 1.7; }
.iv-rate {
  position: relative; display: inline-block; margin-top: 12px;
  font-size: 12.5px; padding: 6px 14px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.2); border: 1px solid rgba(255, 255, 255, 0.35);
}
.iv-rate b { font-size: 15px; margin: 0 2px; }
@media (prefers-reduced-motion: reduce) { .iv-shine { animation: none; display: none; } }
</style>