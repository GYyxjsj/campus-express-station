<template>
  <div class="ap">
    <PageHero tone="wallet" icon="Ticket" chip="COUPON & REDEEM" live title="红包卡券"
      sub="活动优惠券与现金红包统一收纳，兑换码在此核销入账。到期自动失效，使用范围以券面说明为准。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="MagicStick" @click="redeemVisible = true">兑换码核销</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load(1)">刷新</el-button>
      </template>
    </PageHero>

    <AdBanner slot="WALLET_FOOT" />

    <PanelCard title="我的卡券" :note="`${total} 张`" icon="Ticket" accent="wallet" :index="1">
      <div class="ap-seg">
        <button v-for="s in segs" :key="s.value" :class="{ on: status === s.value }" @click="status = s.value; load(1)">
          {{ s.label }}
        </button>
      </div>

      <div v-loading="loading" class="coupon-grid">
        <article v-for="(c, i) in rows" :key="c.id" class="coupon flair-in" :class="[c.type === 'RED_PACKET' ? 'red' : 'blue', c.status.toLowerCase()]"
                 :style="{ '--i': Math.min(i, 10) }">
          <div class="cp-left">
            <div class="cp-amount ap-mono">{{ Number(c.amount || 0).toFixed(2) }}<small>元</small></div>
            <div class="cp-min">{{ Number(c.minSpend || 0) > 0 ? `满 ${money(c.minSpend)} 可用` : '无门槛' }}</div>
          </div>
          <div class="cp-divider" />
          <div class="cp-body">
            <div class="cp-l1">
              <el-tag size="small" :type="c.type === 'RED_PACKET' ? 'danger' : 'primary'" effect="light">
                {{ dict(COUPON_TYPE, c.type).label }}
              </el-tag>
              <b>{{ c.name }}</b>
            </div>
            <p class="cp-desc">{{ c.description || '按券面范围抵扣对应服务费用。' }}</p>
            <div class="cp-meta ap-mono">
              <span>{{ COUPON_SCOPE[c.scope] || '全场通用' }}</span>
              <span>{{ COUPON_SOURCE[c.source] || c.source }}</span>
              <span>有效期至 {{ fmtTime(c.expireAt) }}</span>
            </div>
          </div>
          <div class="cp-state">
            <el-tag size="small" :type="dict(COUPON_STATUS, c.status).type" effect="light">{{ dict(COUPON_STATUS, c.status).label }}</el-tag>
            <span v-if="c.status === 'UNUSED'" class="cp-left-days">{{ countdown(c.expireAt) }}</span>
            <span v-else-if="c.usedAt" class="cp-left-days">{{ fmtTime(c.usedAt) }} 使用</span>
          </div>
        </article>
      </div>

      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="暂无卡券" :image-size="86" />
        <p>参与驿站活动或核销兑换码后，优惠券与红包会出现在这里。</p>
      </div>

      <div class="ap-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
      </div>
    </PanelCard>

    <PanelCard title="使用说明" icon="Reading" accent="slate" :index="2">
      <ol class="step-list">
        <li><b>获取卡券</b>运营活动发放、兑换码核销两种途径；到账时提醒中心同步通知。</li>
        <li><b>使用范围</b>券面标注适用服务：自寄运费、代取服务、超时保险或全场通用。</li>
        <li><b>门槛与有效期</b>满足最低消费方可抵扣；超过有效期自动失效，不可恢复。</li>
        <li><b>兑换码</b>由驿站活动发放，一码一券，核销后立即入包；请勿泄露给他人。</li>
      </ol>
    </PanelCard>

    <el-dialog v-model="redeemVisible" title="兑换码核销" width="440px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>兑换码<em>*</em></label>
          <el-input v-model="code" placeholder="请输入活动兑换码" maxlength="32" clearable
                    class="ap-mono" @keyup.enter="doRedeem" />
          <div class="ap-form-tip">不区分大小写；每个兑换码仅可核销一次。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="redeemVisible = false">取消</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="redeeming" :disabled="!code.trim()" @click="doRedeem">立即核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import AdBanner from '../../components/AdBanner.vue'
import { myCoupons, redeemCode } from '../../api'
import { COUPON_STATUS, COUPON_TYPE, COUPON_SCOPE, COUPON_SOURCE, dict, fmtTime, money, countdown } from '../../utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const status = ref('')
const query = reactive({ page: 1, size: 10 })
const redeemVisible = ref(false)
const redeeming = ref(false)
const code = ref('')

const segs = [
  { value: '', label: '全部' },
  { value: 'UNUSED', label: '待使用' },
  { value: 'USED', label: '已使用' },
  { value: 'EXPIRED', label: '已过期' }
]

const heroStats = computed(() => [
  { label: '卡券总数', value: total.value, unit: '张' },
  { label: '本页可用', value: rows.value.filter(c => c.status === 'UNUSED').length, unit: '张' },
  { label: '本页面额', value: rows.value.reduce((s, c) => s + Number(c.amount || 0), 0), unit: '元', decimals: 2 }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myCoupons({ ...query, status: status.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function doRedeem() {
  if (!code.value.trim()) return
  redeeming.value = true
  try {
    const c = await redeemCode(code.value.trim())
    ElMessage.success(`核销成功：${c.name}（${money(c.amount)}）已入包`)
    redeemVisible.value = false
    code.value = ''
    status.value = 'UNUSED'
    await load(1)
  } finally { redeeming.value = false }
}

onMounted(() => load())
</script>

<style scoped>
.coupon-grid { display: flex; flex-direction: column; gap: 10px; }
.coupon {
  display: flex; align-items: stretch; gap: 0; overflow: hidden;
  border: 1px solid var(--default-border); border-radius: 14px;
  background: var(--art-card-bg, #fff);
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s;
}
html.dark .coupon { background: #17171c; }
.coupon:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.coupon.used, .coupon.expired { filter: saturate(0.35); opacity: 0.75; }

.cp-left {
  flex: none; width: 132px; padding: 16px 12px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  color: #fff;
}
.coupon.blue .cp-left { background: linear-gradient(140deg, #4c6ef5, #8b5cf6); }
.coupon.red .cp-left { background: linear-gradient(140deg, #e11d48, #fb7185); }
.cp-amount { font-size: 26px; font-weight: 800; line-height: 1.1; }
.cp-amount small { font-size: 12px; font-weight: 600; margin-left: 3px; opacity: 0.9; }
.cp-min { font-size: 10.5px; opacity: 0.85; letter-spacing: 0.4px; }

.cp-divider { width: 0; border-left: 2px dashed var(--default-border); margin: 10px 0; }
.cp-body { flex: 1; min-width: 0; padding: 13px 15px; }
.cp-l1 { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.cp-l1 b { font-size: 13.5px; color: var(--art-gray-900); }
.cp-desc { margin: 5px 0 7px; font-size: 12px; color: var(--art-gray-600); line-height: 1.6; }
.cp-meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 11px; color: var(--art-gray-500); }

.cp-state {
  flex: none; width: 108px; padding: 13px 12px;
  display: flex; flex-direction: column; align-items: flex-end; justify-content: center; gap: 6px;
}
.cp-left-days { font-size: 10.5px; color: var(--art-gray-500); text-align: right; }

.step-list { margin: 0; padding-left: 18px; display: flex; flex-direction: column; gap: 9px; }
.step-list li { font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
.step-list b { color: var(--art-gray-900); margin-right: 5px; }

@media (max-width: 640px) {
  .coupon { flex-wrap: wrap; }
  .cp-left { width: 100%; flex-direction: row; justify-content: flex-start; gap: 10px; padding: 12px 15px; }
  .cp-divider { display: none; }
  .cp-state { width: 100%; flex-direction: row; align-items: center; justify-content: space-between; padding: 0 15px 12px; }
}
</style>