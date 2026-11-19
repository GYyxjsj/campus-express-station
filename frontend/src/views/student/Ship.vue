<template>
  <div class="ap">
    <PageHero tone="trace" icon="Van" chip="SELF SHIPPING" live title="服务站自寄"
      sub="在线下单、实时运费预算，钱包余额自动扣费；到站交件后由驿站统一寄出并回填运单号。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openForm">新建自寄单</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load(1)">刷新</el-button>
      </template>
    </PageHero>

    <div class="ap-row wide-left">
      <PanelCard title="我的自寄单" :note="`${total} 单`" icon="Tickets" accent="trace" :index="1">
        <div class="ap-toolbar">
          <div class="ap-seg">
            <button v-for="s in segs" :key="s.value" :class="{ on: status === s.value }" @click="status = s.value; load(1)">{{ s.label }}</button>
          </div>
          <div class="ap-field ap-grow">
            <el-input v-model="query.keyword" placeholder="搜索单号 / 运单号 / 收件人" :prefix-icon="Search" clearable @keyup.enter="load(1)" @clear="load(1)" />
          </div>
        </div>

        <div class="ap-table-wrap">
          <el-table :data="sortedRows" v-loading="loading" :default-sort="defaultSort" @sort-change="onSortChange" size="default">
            <el-table-column prop="orderNo" label="自寄单号" min-width="168" sortable="custom">
              <template #default="{ row }"><span class="ap-mono">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column prop="courierCompany" label="承运商" min-width="100">
              <template #default="{ row }">{{ courierName(row.courierCompany) }}</template>
            </el-table-column>
            <el-table-column label="收件人" min-width="150">
              <template #default="{ row }">
                <div class="ap-strong">{{ row.receiverName }}</div>
                <div class="cell-sub ap-mono">{{ row.receiverPhone }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="weightKg" label="重量" min-width="86" sortable="custom">
              <template #default="{ row }"><span class="ap-mono">{{ Number(row.weightKg).toFixed(2) }} kg</span></template>
            </el-table-column>
            <el-table-column prop="freight" label="运费" min-width="100" sortable="custom" align="right">
              <template #default="{ row }"><span class="ap-mono">{{ money(row.freight) }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" min-width="92">
              <template #default="{ row }"><el-tag size="small" :type="dict(SHIP_STATUS, row.status).type" effect="light">{{ dict(SHIP_STATUS, row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="trackingNo" label="运单号" min-width="140">
              <template #default="{ row }"><span class="ap-mono">{{ row.trackingNo || '待寄出' }}</span></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="下单时间" min-width="140" sortable="custom">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="196" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'CREATED'" size="small" text type="success" @click="doDrop(row)">已交件</el-button>
                <el-button v-if="row.status === 'CREATED'" size="small" text type="danger" @click="doCancel(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="暂无自寄单" :image-size="86" />
          <p>点击「新建自寄单」填写收寄信息，运费实时预算、下单即扣钱包余额。</p>
        </div>

        <div class="ap-pager">
          <el-pagination background layout="total, prev, pager, next" :total="total"
                         v-model:current-page="query.page" :page-size="query.size" @current-change="load()" />
        </div>
      </PanelCard>

      <PanelCard title="运费速算" note="与下单同规则" icon="Odometer" accent="mint" :index="2">
        <div class="ap-form">
          <div class="ap-field-row">
            <label>包裹重量（kg）</label>
            <el-input-number v-model="calcWeight" :min="0.1" :max="50" :step="0.5" :precision="2" style="width:100%" @change="quoteNow" />
          </div>
          <div class="ap-field-row">
            <el-checkbox v-model="calcRemote" @change="quoteNow">偏远地区（新疆 / 西藏等）</el-checkbox>
          </div>
        </div>
        <div v-if="calc" class="quote-box">
          <div class="qb-row"><span>首重 {{ Number(calc.firstKg).toFixed(1) }} kg</span><b class="ap-mono">{{ money(calc.firstPrice) }}</b></div>
          <div class="qb-row"><span>续重 {{ calc.extraUnits }} kg × {{ money(calc.extraPrice) }}</span><b class="ap-mono">{{ money(calc.extraFee) }}</b></div>
          <div class="qb-row"><span>偏远附加</span><b class="ap-mono">{{ money(calc.remoteFee) }}</b></div>
          <div class="qb-total"><span>预估运费</span><b class="ap-mono">{{ money(calc.total) }}</b></div>
        </div>
        <div class="ap-note" style="--nc:#0ea5e9">
          <el-icon><InfoFilled /></el-icon>
          <div>续重不足 1 kg 按 1 kg 计；实际以下单时预算为准，取消未交件订单运费原路退回钱包。</div>
        </div>
      </PanelCard>
    </div>

    <!-- 新建自寄单 -->
    <el-dialog v-model="formVisible" title="新建自寄单" width="640px" append-to-body top="6vh">
      <div class="ap-form">
        <div class="ap-field-row two">
          <div>
            <label>承运商<em>*</em></label>
            <el-select v-model="form.courierCompany" placeholder="选择快递公司" style="width:100%">
              <el-option v-for="c in shipCouriers" :key="c.code" :label="c.name" :value="c.code" />
            </el-select>
          </div>
          <div>
            <label>交件服务站<em>*</em></label>
            <el-select v-model="form.stationId" placeholder="选择营业中的服务站" style="width:100%">
              <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
        </div>
        <div class="ap-field-row two">
          <div>
            <label>寄件人<em>*</em></label>
            <el-input v-model="form.senderName" placeholder="你的姓名" maxlength="32" />
          </div>
          <div>
            <label>寄件人电话<em>*</em></label>
            <el-input v-model="form.senderPhone" placeholder="11 位手机号" maxlength="11" class="ap-mono" />
          </div>
        </div>
        <div class="ap-field-row two">
          <div>
            <label>收件人<em>*</em></label>
            <el-input v-model="form.receiverName" placeholder="收件人姓名" maxlength="32" />
          </div>
          <div>
            <label>收件人电话<em>*</em></label>
            <el-input v-model="form.receiverPhone" placeholder="11 位手机号" maxlength="11" class="ap-mono" />
          </div>
        </div>
        <div class="ap-field-row">
          <label>收件地址<em>*</em>
            <el-button v-if="addresses.length" size="small" text type="primary" style="margin-left:8px" @click="addrPickVisible = true">从地址簿带入</el-button>
          </label>
          <el-input v-model="form.receiverAddress" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="省市区 + 详细地址" />
        </div>
        <div class="ap-field-row two">
          <div>
            <label>物品类型</label>
            <el-select v-model="form.goodsType" style="width:100%">
              <el-option v-for="g in goodsTypes" :key="g" :label="g" :value="g" />
            </el-select>
          </div>
          <div>
            <label>重量（kg）<em>*</em></label>
            <el-input-number v-model="form.weightKg" :min="0.1" :max="50" :step="0.5" :precision="2" style="width:100%" @change="quoteForm" />
          </div>
        </div>
        <div class="ap-field-row">
          <div class="ship-flags">
            <el-checkbox v-model="form.remote" @change="quoteForm">偏远地区收件</el-checkbox>
            <el-checkbox v-model="form.insured">保价服务（驿站按声明价值看护）</el-checkbox>
          </div>
        </div>
        <div class="ap-field-row">
          <label>备注</label>
          <el-input v-model="form.note" placeholder="如：易碎品，请轻拿轻放" maxlength="200" />
        </div>
        <div v-if="formQuote" class="quote-box inline">
          <div class="qb-row"><span>首重 + 续重 {{ formQuote.extraUnits }} kg + 偏远附加</span><b class="ap-mono">{{ money(formQuote.firstPrice) }} + {{ money(formQuote.extraFee) }} + {{ money(formQuote.remoteFee) }}</b></div>
          <div class="qb-total"><span>本单运费（下单即从钱包扣除）</span><b class="ap-mono">{{ money(formQuote.total) }}</b></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :icon="Van" :loading="saving" @click="doCreate">确认下单</el-button>
      </template>
    </el-dialog>

    <!-- 地址簿带入 -->
    <el-dialog v-model="addrPickVisible" title="选择地址带入" width="480px" append-to-body>
      <div class="addr-pick">
        <button v-for="a in addresses" :key="a.id" class="addr-pick-item" @click="pickAddress(a)">
          <div><b>{{ a.contact }}</b><span class="ap-mono">{{ a.phone }}</span><el-tag v-if="a.isDefault === 1" size="small" type="success" effect="light">默认</el-tag></div>
          <p>{{ a.region ? a.region + ' ' : '' }}{{ a.detail }}</p>
        </button>
      </div>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="自寄单详情" width="520px" append-to-body>
      <template v-if="detail">
        <div class="ap-kv">
          <div><span>自寄单号</span><b class="ap-mono">{{ detail.orderNo }}</b></div>
          <div><span>状态</span><el-tag size="small" :type="dict(SHIP_STATUS, detail.status).type" effect="light">{{ dict(SHIP_STATUS, detail.status).label }}</el-tag></div>
          <div><span>承运商</span><b>{{ courierName(detail.courierCompany) }}</b></div>
          <div><span>服务站</span><b>{{ detail.stationName || stationName(detail.stationId) }}</b></div>
          <div><span>寄件人</span><b>{{ detail.senderName }} · <span class="ap-mono">{{ detail.senderPhone }}</span></b></div>
          <div><span>收件人</span><b>{{ detail.receiverName }} · <span class="ap-mono">{{ detail.receiverPhone }}</span></b></div>
          <div><span>收件地址</span><b>{{ detail.receiverAddress }}</b></div>
          <div><span>物品 / 重量</span><b>{{ detail.goodsType }} · <span class="ap-mono">{{ Number(detail.weightKg).toFixed(2) }} kg</span></b></div>
          <div><span>运费</span><b class="ap-mono">{{ money(detail.freight) }}</b></div>
          <div><span>保价</span><b>{{ detail.insured === 1 ? '已保价' : '未保价' }}</b></div>
          <div><span>运单号</span><b class="ap-mono">{{ detail.trackingNo || '待寄出回填' }}</b></div>
          <div><span>下单时间</span><b class="ap-mono">{{ fmtTime(detail.createdAt) }}</b></div>
          <div v-if="detail.droppedAt"><span>交件时间</span><b class="ap-mono">{{ fmtTime(detail.droppedAt) }}</b></div>
          <div v-if="detail.shippedAt"><span>寄出时间</span><b class="ap-mono">{{ fmtTime(detail.shippedAt) }}</b></div>
          <div v-if="detail.cancelledAt"><span>取消时间</span><b class="ap-mono">{{ fmtTime(detail.cancelledAt) }}</b></div>
          <div v-if="detail.note"><span>备注</span><b>{{ detail.note }}</b></div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Van, InfoFilled } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { createShipOrder, myShipOrders, shipOrderDetail, dropShipOrder, cancelShipOrder, freightQuote, listStations, myAddresses } from '../../api'
import { SHIP_STATUS, dict, fmtTime, money, courierName } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'

const shipCouriers = [
  { code: 'SF', name: '顺丰速运' }, { code: 'ZT', name: '中通快递' }, { code: 'YT', name: '圆通速递' },
  { code: 'YD', name: '韵达快递' }, { code: 'STO', name: '申通快递' }, { code: 'JT', name: '极兔速递' },
  { code: 'JD', name: '京东物流' }, { code: 'EMS', name: '邮政EMS' }
]
const goodsTypes = ['日常用品', '服饰鞋包', '书籍文件', '数码产品', '食品特产', '其他']

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const status = ref('')
const query = reactive({ page: 1, size: 10, keyword: '' })
const segs = [
  { value: '', label: '全部' }, { value: 'CREATED', label: '待交件' },
  { value: 'DROPPED', label: '已交件' }, { value: 'SHIPPED', label: '已寄出' }, { value: 'CANCELLED', label: '已取消' }
]

const { sortState, defaultSort, onSortChange, sorted } = useTableSort('createdAt', { createdAt: r => r.createdAt, freight: r => Number(r.freight || 0), weightKg: r => Number(r.weightKg || 0) })
const sortedRows = computed(() => sorted(rows.value))

const stations = ref([])
const addresses = ref([])
const formVisible = ref(false)
const addrPickVisible = ref(false)
const saving = ref(false)
const form = reactive({
  courierCompany: 'ZT', stationId: null, senderName: '', senderPhone: '',
  receiverName: '', receiverPhone: '', receiverAddress: '', goodsType: '日常用品',
  weightKg: 1, remote: false, insured: false, note: ''
})
const formQuote = ref(null)
const calcWeight = ref(1)
const calcRemote = ref(false)
const calc = ref(null)
const detailVisible = ref(false)
const detail = ref(null)

const heroStats = computed(() => [
  { label: '自寄总数', value: total.value, unit: '单' },
  { label: '本页待交件', value: rows.value.filter(r => r.status === 'CREATED').length, unit: '单' },
  { label: '本页运费', value: rows.value.reduce((s, r) => s + Number(r.freight || 0), 0), unit: '元', decimals: 2 }
])

function stationName(id) {
  const s = stations.value.find(x => x.id === id)
  return s ? s.name : '—'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myShipOrders({ ...query, status: status.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function quoteNow() {
  try { calc.value = await freightQuote({ weightKg: calcWeight.value, remote: calcRemote.value }) } catch { /* 忽略预算失败 */ }
}
async function quoteForm() {
  try { formQuote.value = await freightQuote({ weightKg: form.weightKg, remote: form.remote }) } catch { /* 忽略 */ }
}

function openForm() {
  const me = JSON.parse(localStorage.getItem('ce_user') || '{}')
  Object.assign(form, {
    courierCompany: 'ZT', stationId: stations.value.length ? stations.value[0].id : null,
    senderName: me.realName || me.username || '', senderPhone: me.phone || '',
    receiverName: '', receiverPhone: '', receiverAddress: '', goodsType: '日常用品',
    weightKg: 1, remote: false, insured: false, note: ''
  })
  formVisible.value = true
  quoteForm()
}

function pickAddress(a) {
  form.receiverName = a.contact
  form.receiverPhone = a.phone
  form.receiverAddress = (a.region ? a.region + ' ' : '') + a.detail
  addrPickVisible.value = false
  ElMessage.success('已带入地址簿信息')
}

async function doCreate() {
  if (!form.stationId) { ElMessage.warning('请选择交件服务站'); return }
  if (!form.senderName.trim() || !/^1[3-9]\d{9}$/.test(form.senderPhone.trim())) { ElMessage.warning('请完整填写寄件人姓名与手机号'); return }
  if (!form.receiverName.trim() || !/^1[3-9]\d{9}$/.test(form.receiverPhone.trim()) || !form.receiverAddress.trim()) {
    ElMessage.warning('请完整填写收件人姓名、手机号与收件地址'); return
  }
  saving.value = true
  try {
    const order = await createShipOrder({ ...form, senderName: form.senderName.trim(), receiverName: form.receiverName.trim(), receiverAddress: form.receiverAddress.trim() })
    ElMessage.success(`下单成功：${order.orderNo}，运费 ${money(order.freight)} 已从钱包扣除`)
    formVisible.value = false
    status.value = ''
    await load(1)
  } finally { saving.value = false }
}

async function doDrop(row) {
  try {
    await ElMessageBox.confirm(`确认已将包裹交至服务站？交件后不可取消。`, '交件确认', { type: 'info', confirmButtonText: '已交件' })
  } catch { return }
  await dropShipOrder(row.id)
  ElMessage.success('已标记交件，等待驿站寄出')
  await load()
}

async function doCancel(row) {
  try {
    await ElMessageBox.confirm(`取消自寄单 ${row.orderNo}？运费将退回钱包余额。`, '取消自寄单', { type: 'warning' })
  } catch { return }
  await cancelShipOrder(row.id)
  ElMessage.success('已取消，运费退回钱包')
  await load()
}

async function openDetail(row) {
  detail.value = await shipOrderDetail(row.id)
  detailVisible.value = true
}

onMounted(async () => {
  load()
  quoteNow()
  try {
    const all = await listStations()
    stations.value = (all || []).filter(s => s.status === 1)
  } catch { /* 无权限或失败时下拉为空 */ }
  try { addresses.value = await myAddresses() } catch { /* 忽略 */ }
})
</script>

<style scoped>
.cell-sub { font-size: 11px; color: var(--art-gray-500); margin-top: 2px; }
.quote-box { border: 1px solid var(--default-border); border-radius: 12px; padding: 12px 14px; margin-top: 6px; }
.quote-box.inline { margin-top: 2px; background: var(--el-color-primary-light-9); border-color: var(--el-color-primary-light-7); }
html.dark .quote-box.inline { background: rgba(91, 124, 250, 0.08); }
.qb-row { display: flex; justify-content: space-between; gap: 12px; font-size: 12px; color: var(--art-gray-600); padding: 3px 0; }
.qb-row b { color: var(--art-gray-800); font-weight: 600; }
.qb-total {
  display: flex; justify-content: space-between; align-items: baseline; gap: 12px;
  margin-top: 7px; padding-top: 9px; border-top: 1px dashed var(--default-border);
  font-size: 12.5px; color: var(--art-gray-700);
}
.qb-total b { font-size: 19px; font-weight: 800; color: var(--el-color-primary); }
.ship-flags { display: flex; gap: 20px; flex-wrap: wrap; }
.addr-pick { display: flex; flex-direction: column; gap: 8px; max-height: 50vh; overflow: auto; }
.addr-pick-item {
  text-align: left; border: 1px solid var(--default-border); border-radius: 12px; padding: 11px 13px;
  background: transparent; cursor: pointer; transition: all 0.2s;
}
.addr-pick-item:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.addr-pick-item div { display: flex; align-items: center; gap: 9px; }
.addr-pick-item b { font-size: 13.5px; color: var(--art-gray-900); }
.addr-pick-item span { font-size: 12px; color: var(--art-gray-600); }
.addr-pick-item p { margin: 5px 0 0; font-size: 12px; color: var(--art-gray-600); line-height: 1.6; }
</style>