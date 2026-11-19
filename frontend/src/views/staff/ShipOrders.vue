<template>
  <div class="ap">
    <PageHero tone="staff" icon="Van" chip="SHIP DESK" live title="自寄处理台"
      sub="学生服务站自寄订单在此流转：待交件 → 已交件 → 已寄出。包裹交至柜台后录入承运运单号即完成寄出，运费从学生钱包扣除的订单取消时自动退款。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="自寄订单" :note="`${total} 笔订单`" icon="List" accent="staff" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="query.status" clearable placeholder="全部状态" @change="load(1)">
            <el-option v-for="(d, key) in SHIP_STATUS" :key="key" :label="d.label" :value="key" />
          </el-select>
        </div>
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" :prefix-icon="Search" clearable placeholder="订单号 / 运单号 / 寄件人 / 收件人"
                    @keyup.enter="load(1)" @clear="load(1)" />
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="sorted(rows)" stripe :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
            <el-table-column label="订单号" prop="orderNo" sortable="custom" width="170">
              <template #default="{ row }"><span class="ap-mono">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column prop="userName" label="下单学生" sortable="custom" width="100" />
            <el-table-column label="承运商" prop="courierCompany" sortable="custom" width="110">
              <template #default="{ row }">{{ courierName(row.courierCompany) }}</template>
            </el-table-column>
            <el-table-column label="收件人" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row.receiverName }} · {{ row.receiverAddress }}</template>
            </el-table-column>
            <el-table-column label="运费" prop="freight" sortable="custom" width="104" align="right">
              <template #default="{ row }"><span class="ap-mono">{{ money(row.freight) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" prop="status" sortable="custom" width="94">
              <template #default="{ row }">
                <el-tag :type="dict(SHIP_STATUS, row.status).type" size="small" effect="light">
                  {{ dict(SHIP_STATUS, row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click.stop="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'DROPPED'" size="small" text type="success" @click.stop="openShip(row)">寄出</el-button>
                <el-button v-if="row.status === 'CREATED' || row.status === 'DROPPED'" size="small" text type="danger"
                           @click.stop="doCancel(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sorted(rows)" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <span class="ap-mono card-no">{{ row.orderNo }}</span>
              <el-tag :type="dict(SHIP_STATUS, row.status).type" size="small" effect="light">
                {{ dict(SHIP_STATUS, row.status).label }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>学生</label><span>{{ row.userName }}</span></div>
              <div><label>承运商</label><span>{{ courierName(row.courierCompany) }}</span></div>
              <div><label>运费</label><span class="ap-mono">{{ money(row.freight) }}</span></div>
              <div><label>下单时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
            </div>
            <div class="card-ops" @click.stop>
              <el-button v-if="row.status === 'DROPPED'" size="small" type="success" plain @click="openShip(row)">录入运单寄出</el-button>
              <el-button v-if="row.status === 'CREATED' || row.status === 'DROPPED'" size="small" type="danger" plain @click="doCancel(row)">取消</el-button>
            </div>
          </article>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="没有匹配的自寄订单" :image-size="86" />
          <p>学生在「寄件与运费」页下单后会出现在这里。</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <el-drawer v-model="detailVisible" title="自寄订单详情" size="460px" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-head">
          <b class="ap-mono">{{ current.orderNo }}</b>
          <el-tag :type="dict(SHIP_STATUS, current.status).type" effect="light" size="small">
            {{ dict(SHIP_STATUS, current.status).label }}
          </el-tag>
        </div>
        <div class="ap-kv">
          <div><label>下单学生</label><span>{{ current.userName }}</span></div>
          <div><label>驿站</label><span>{{ current.stationName || '—' }}</span></div>
          <div><label>承运商</label><span>{{ courierName(current.courierCompany) }}</span></div>
          <div><label>物品 / 重量</label><span>{{ current.goodsType || '—' }} / {{ current.weightKg }} kg</span></div>
          <div><label>运费</label><span class="ap-mono">{{ money(current.freight) }}</span></div>
          <div><label>保价</label><span>{{ current.insured === 1 ? '已保价' : '未保价' }}</span></div>
          <div><label>寄件人</label><span>{{ current.senderName }}（{{ current.senderPhone }}）</span></div>
          <div><label>收件人</label><span>{{ current.receiverName }}（{{ current.receiverPhone }}）</span></div>
          <div><label>收件地址</label><span>{{ current.receiverAddress }}</span></div>
          <div><label>承运运单号</label><span class="ap-mono">{{ current.trackingNo || '待寄出录入' }}</span></div>
          <div><label>下单时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>交件时间</label><span class="ap-mono">{{ fmtTime(current.droppedAt) }}</span></div>
          <div><label>寄出时间</label><span class="ap-mono">{{ fmtTime(current.shippedAt) }}</span></div>
        </div>
        <div v-if="current.note" class="ap-note" style="--nc:#f59e0b">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ current.note }}</span>
        </div>
        <div class="d-actions">
          <el-button v-if="current.status === 'DROPPED'" type="success" :icon="Van" @click="openShip(current)">录入运单寄出</el-button>
          <el-button v-if="current.status === 'CREATED' || current.status === 'DROPPED'" type="danger" plain
                     :icon="CircleClose" @click="doCancel(current)">代客取消</el-button>
        </div>
        <div v-if="current.status === 'CREATED'" class="ap-note" style="--nc:#5b7cfa">
          <el-icon><InfoFilled /></el-icon>
          <span>学生尚未到柜台交件。收到实物后请学生在其订单页点「确认交件」，再录入运单号寄出。</span>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="shipVisible" title="录入运单号寄出" width="440px" append-to-body>
      <div v-if="shipTarget" class="ap-form">
        <div class="ap-note" style="--nc:#0d9488">
          <el-icon><Van /></el-icon>
          <span>{{ shipTarget.orderNo }} · {{ courierName(shipTarget.courierCompany) }} · 收件 {{ shipTarget.receiverName }}</span>
        </div>
        <div class="ap-field-row">
          <label>承运运单号<em>*</em></label>
          <el-input v-model="shipTracking" maxlength="64" class="mono-input" placeholder="快递面单上的运单号"
                    @keyup.enter="doShip" />
          <div class="ap-form-tip">寄出后学生将收到站内提醒，可凭此单号查询物流。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="success" :loading="shipping" :icon="Van" @click="doShip">确认寄出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleClose, InfoFilled, Refresh, RefreshLeft, Search, Van } from '@element-plus/icons-vue'
import { adminShipOrders, adminShipDeliver, adminShipCancel } from '../../api'
import { fmtTime, money, dict, courierName, SHIP_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, status: '', keyword: '' })
const { defaultSort, onSortChange, sorted } = useTableSort('createdAt')

const detailVisible = ref(false)
const current = ref(null)
const shipVisible = ref(false)
const shipTarget = ref(null)
const shipTracking = ref('')
const shipping = ref(false)

const count = s => rows.value.filter(r => r.status === s).length
const freightSum = computed(() => rows.value.filter(r => r.status !== 'CANCELLED')
  .reduce((a, r) => a + Number(r.freight || 0), 0))

const heroStats = computed(() => [
  { label: '命中订单', value: total.value, unit: '笔' },
  { label: '本页待交件', value: count('CREATED'), unit: '笔' },
  { label: '本页已交件', value: count('DROPPED'), unit: '笔' }
])
const metrics = computed(() => [
  { label: '待交件', value: count('CREATED'), unit: '笔', icon: 'Clock', color: '#f59e0b', foot: '学生未到柜台交件' },
  { label: '已交件待寄出', value: count('DROPPED'), unit: '笔', icon: 'Box', color: '#5b7cfa', foot: '录入运单号即寄出' },
  { label: '已寄出', value: count('SHIPPED'), unit: '笔', icon: 'Van', color: '#10d9b5', foot: '本页统计' },
  { label: '本页有效运费', value: freightSum.value.toFixed(2), unit: '元', icon: 'Money', color: '#8b5cf6', foot: '不含已取消订单' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await adminShipOrders({
      page: query.page, size: query.size,
      status: query.status || undefined, keyword: query.keyword || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function resetQuery() {
  query.status = ''
  query.keyword = ''
  load(1)
}

function openDetail(row) { current.value = row; detailVisible.value = true }

function openShip(row) {
  shipTarget.value = row
  shipTracking.value = ''
  shipVisible.value = true
}

async function doShip() {
  const no = shipTracking.value.trim()
  if (!no) return ElMessage.warning('请输入承运运单号')
  shipping.value = true
  try {
    const updated = await adminShipDeliver(shipTarget.value.id, no)
    ElMessage.success(`已寄出：${updated.orderNo}，运单号 ${no}`)
    shipVisible.value = false
    if (current.value && current.value.id === updated.id) current.value = updated
    load()
  } finally { shipping.value = false }
}

async function doCancel(row) {
  await ElMessageBox.confirm(
    `确认代客取消自寄单 ${row.orderNo}？已扣运费将原路退回学生钱包。`,
    '取消自寄单', { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
  )
  const updated = await adminShipCancel(row.id)
  ElMessage.success('订单已取消，运费已退回')
  if (current.value && current.value.id === updated.id) current.value = updated
  load()
}

onMounted(() => load())
</script>

<style scoped>
.card-no { font-size: 12.5px; color: var(--art-gray-900); font-weight: 700; }
.card-ops { display: flex; gap: 8px; margin-top: 10px; }
.detail { display: flex; flex-direction: column; gap: 12px; }
.d-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.d-head b { font-size: 15px; color: var(--art-gray-900); }
.d-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.mono-input :deep(input) { font-family: 'JetBrains Mono', Consolas, monospace; letter-spacing: 1px; }
</style>