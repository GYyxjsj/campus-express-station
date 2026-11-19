<template>
  <div class="ap">
    <PageHero tone="wallet" icon="Present" chip="COUPON FACTORY" live title="红包卡券工厂"
      sub="创建优惠券 / 红包模板并批量生成兑换码：学生在「红包卡券」页输入兑换码领取，一人一码限领一次。金额抵扣自寄运费、代取服务费或保险保费。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">创建模板发码</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="卡券模板" :note="`${tplTotal} 个模板`" icon="Ticket" accent="wallet" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="tplQuery.status" clearable placeholder="全部状态" @change="loadTemplates(1)">
            <el-option label="启用中" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </div>
      </div>
      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="sortedTemplates(templates)" stripe :default-sort="tplSort.defaultSort" @sort-change="tplSort.onSortChange">
            <el-table-column prop="name" label="模板名称" sortable="custom" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" prop="type" sortable="custom" width="92">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :style="{ color: typeColor(row.type), borderColor: typeColor(row.type) }">
                  {{ (COUPON_TYPE[row.type] || {}).label || row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="面额" prop="amount" sortable="custom" width="104" align="right">
              <template #default="{ row }"><span class="ap-mono amount">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="门槛" prop="minSpend" sortable="custom" width="110" align="right">
              <template #default="{ row }">
                <span class="ap-mono">{{ Number(row.minSpend) > 0 ? `满 ${money(row.minSpend)}` : '无门槛' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="适用范围" prop="scope" sortable="custom" width="106">
              <template #default="{ row }">{{ COUPON_SCOPE[row.scope] || row.scope }}</template>
            </el-table-column>
            <el-table-column label="领取进度" min-width="130">
              <template #default="{ row }">
                <div class="claim-cell">
                  <el-progress :percentage="claimPct(row)" :stroke-width="6" :show-text="false"
                               :color="claimPct(row) >= 100 ? '#7987a1' : '#f59e0b'" />
                  <span class="ap-mono">{{ row.claimedQty }} / {{ row.totalQty }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="有效期" prop="validDays" sortable="custom" width="92" align="center">
              <template #default="{ row }">{{ row.validDays }} 天</template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sortedTemplates(templates)" :key="row.id" class="ap-card-item">
            <div class="ap-card-top">
              <b>{{ row.name }}</b>
              <el-tag size="small" effect="plain">{{ (COUPON_TYPE[row.type] || {}).label || row.type }}</el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>面额</label><span class="ap-mono">{{ money(row.amount) }}</span></div>
              <div><label>门槛</label><span class="ap-mono">{{ Number(row.minSpend) > 0 ? `满 ${money(row.minSpend)}` : '无门槛' }}</span></div>
              <div><label>范围</label><span>{{ COUPON_SCOPE[row.scope] || row.scope }}</span></div>
              <div><label>领取</label><span class="ap-mono">{{ row.claimedQty }} / {{ row.totalQty }}</span></div>
              <div><label>有效期</label><span>{{ row.validDays }} 天</span></div>
              <div><label>创建</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
            </div>
          </article>
        </div>

        <div v-if="!templates.length && !loading" class="ap-empty">
          <el-empty description="暂无卡券模板" :image-size="86" />
          <p>点击右上角「创建模板发码」生成第一批兑换码。</p>
        </div>
      </div>
      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="tplTotal" v-model:current-page="tplQuery.page" v-model:page-size="tplQuery.size"
                       :page-sizes="[10, 20, 50]" @current-change="loadTemplates()" @size-change="loadTemplates(1)" />
      </div>
    </PanelCard>

    <PanelCard title="兑换码台账" :note="`${codeTotal} 个兑换码`" icon="Key" accent="amber" :index="5">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>状态</label>
          <el-select v-model="codeQuery.status" clearable placeholder="全部状态" @change="loadCodes(1)">
            <el-option v-for="(d, key) in REDEEM_STATUS" :key="key" :label="d.label" :value="key" />
          </el-select>
        </div>
      </div>
      <div v-loading="codeLoading">
        <div class="ap-table-wrap">
          <el-table :data="sortedCodes(codes)" stripe :default-sort="codeSort.defaultSort" @sort-change="codeSort.onSortChange">
            <el-table-column label="兑换码" prop="code" sortable="custom" width="180">
              <template #default="{ row }">
                <span class="ap-mono code-cell">{{ row.code }}</span>
                <el-button size="small" text :icon="DocumentCopy" @click="copyText(row.code)" />
              </template>
            </el-table-column>
            <el-table-column prop="templateName" label="所属模板" sortable="custom" min-width="170" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" sortable="custom" width="96">
              <template #default="{ row }">
                <el-tag :type="dict(REDEEM_STATUS, row.status).type" size="small" effect="light">
                  {{ dict(REDEEM_STATUS, row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="usedByName" label="兑换人" sortable="custom" width="100">
              <template #default="{ row }">{{ row.usedByName || '—' }}</template>
            </el-table-column>
            <el-table-column label="兑换时间" prop="usedAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.usedAt) }}</span></template>
            </el-table-column>
            <el-table-column label="生成时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in sortedCodes(codes)" :key="row.id" class="ap-card-item">
            <div class="ap-card-top">
              <b class="ap-mono">{{ row.code }}</b>
              <el-tag :type="dict(REDEEM_STATUS, row.status).type" size="small" effect="light">
                {{ dict(REDEEM_STATUS, row.status).label }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>模板</label><span>{{ row.templateName }}</span></div>
              <div><label>兑换人</label><span>{{ row.usedByName || '—' }}</span></div>
              <div><label>兑换时间</label><span class="ap-mono">{{ fmtTime(row.usedAt) }}</span></div>
              <div><label>生成时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
            </div>
          </article>
        </div>

        <div v-if="!codes.length && !codeLoading" class="ap-empty">
          <el-empty description="暂无兑换码" :image-size="86" />
        </div>
      </div>
      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="codeTotal" v-model:current-page="codeQuery.page" v-model:page-size="codeQuery.size"
                       :page-sizes="[10, 20, 50]" @current-change="loadCodes()" @size-change="loadCodes(1)" />
      </div>
    </PanelCard>

    <el-dialog v-model="createVisible" title="创建卡券模板并批量发码" width="560px" append-to-body :close-on-click-modal="false">
      <el-form class="ap-form" label-position="top" @submit.prevent>
        <div class="ap-field-row">
          <label>模板名称<em>*</em></label>
          <el-input v-model="batch.name" maxlength="64" show-word-limit placeholder="例如：迎新季自寄立减券" />
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>类型</label>
            <el-radio-group v-model="batch.type">
              <el-radio-button value="COUPON">优惠券</el-radio-button>
              <el-radio-button value="RED_PACKET">红包</el-radio-button>
            </el-radio-group>
          </div>
          <div class="ap-field-row">
            <label>适用范围</label>
            <el-select v-model="batch.scope" style="width:100%">
              <el-option v-for="(label, key) in COUPON_SCOPE" :key="key" :label="label" :value="key" />
            </el-select>
          </div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>面额（元）<em>*</em></label>
            <el-input-number v-model="batch.amount" :min="0.01" :max="200" :precision="2" :step="1" style="width:100%" />
          </div>
          <div class="ap-field-row">
            <label>使用门槛（元）</label>
            <el-input-number v-model="batch.minSpend" :min="0" :max="1000" :precision="2" :step="5" style="width:100%" />
            <div class="ap-form-tip">0 表示无门槛。</div>
          </div>
        </div>
        <div class="form-2col">
          <div class="ap-field-row">
            <label>发码数量</label>
            <el-input-number v-model="batch.quantity" :min="1" :max="500" style="width:100%" />
            <div class="ap-form-tip">1 ~ 500 个，一码一人。</div>
          </div>
          <div class="ap-field-row">
            <label>领取后有效期（天）</label>
            <el-input-number v-model="batch.validDays" :min="1" :max="365" style="width:100%" />
          </div>
        </div>
        <div class="ap-field-row">
          <label>使用说明</label>
          <el-input v-model="batch.description" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="展示在学生卡券详情中（可留空）" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建并生成兑换码</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="codesVisible" title="兑换码已生成" width="520px" append-to-body>
      <div v-if="generated" class="gen-box">
        <div class="ap-note" style="--nc:#0d9488">
          <el-icon><CircleCheck /></el-icon>
          <span>「{{ generated.template.name }}」已生成 {{ generated.codes.length }} 个兑换码，复制后线下发放或随公告推送。</span>
        </div>
        <div class="gen-codes">
          <span v-for="c in generated.codes" :key="c" class="ap-mono gen-code" @click="copyText(c)">{{ c }}</span>
        </div>
        <div class="ap-form-tip">点击任意兑换码单独复制，或一键复制全部。</div>
      </div>
      <template #footer>
        <el-button :icon="DocumentCopy" type="primary" @click="copyText((generated?.codes || []).join('\n'))">复制全部</el-button>
        <el-button @click="codesVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, DocumentCopy, Plus, Refresh } from '@element-plus/icons-vue'
import { adminCoupons, adminCreateCouponBatch, adminRedeemCodes } from '../../api'
import { fmtTime, money, dict, COUPON_TYPE, COUPON_SCOPE, REDEEM_STATUS } from '../../utils/format'
import { useTableSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const codeLoading = ref(false)
const templates = ref([])
const codes = ref([])
const tplTotal = ref(0)
const codeTotal = ref(0)
const tplQuery = reactive({ page: 1, size: 10, status: null })
const codeQuery = reactive({ page: 1, size: 10, status: '' })
const tplSort = useTableSort('createdAt')
const codeSort = useTableSort('createdAt')
const sortedTemplates = tplSort.sorted
const sortedCodes = codeSort.sorted

const createVisible = ref(false)
const creating = ref(false)
const codesVisible = ref(false)
const generated = ref(null)
const batch = reactive({ name: '', type: 'COUPON', scope: 'ALL', amount: 5, minSpend: 0, quantity: 10, validDays: 30, description: '' })

const typeColor = t => (COUPON_TYPE[t] || {}).color || '#5b7cfa'
const claimPct = row => row.totalQty ? Math.min(100, Math.round(row.claimedQty / row.totalQty * 100)) : 0

const heroStats = computed(() => [
  { label: '模板总数', value: tplTotal.value, unit: '个' },
  { label: '兑换码总数', value: codeTotal.value, unit: '个' },
  { label: '本页已兑换', value: codes.value.filter(c => c.status === 'USED').length, unit: '个' }
])
const metrics = computed(() => [
  { label: '卡券模板', value: tplTotal.value, unit: '个', icon: 'Ticket', color: '#5b7cfa', foot: '优惠券与红包模板' },
  { label: '本页已领取', value: templates.value.reduce((a, t) => a + (t.claimedQty || 0), 0), unit: '张', icon: 'Present', color: '#f59e0b', foot: '当前页模板累计领取' },
  { label: '兑换码台账', value: codeTotal.value, unit: '个', icon: 'Key', color: '#8b5cf6', foot: '一码一人限领' },
  { label: '本页待兑换', value: codes.value.filter(c => c.status === 'UNUSED').length, unit: '个', icon: 'MagicStick', color: '#10d9b5', foot: '可继续发放' }
])

async function loadTemplates(page) {
  if (page) tplQuery.page = page
  loading.value = true
  try {
    const data = await adminCoupons({
      page: tplQuery.page, size: tplQuery.size,
      status: tplQuery.status === null || tplQuery.status === '' ? undefined : tplQuery.status
    })
    templates.value = data.records
    tplTotal.value = data.total
  } finally { loading.value = false }
}

async function loadCodes(page) {
  if (page) codeQuery.page = page
  codeLoading.value = true
  try {
    const data = await adminRedeemCodes({
      page: codeQuery.page, size: codeQuery.size,
      status: codeQuery.status || undefined
    })
    codes.value = data.records
    codeTotal.value = data.total
  } finally { codeLoading.value = false }
}

function refresh() { loadTemplates(); loadCodes() }

function openCreate() {
  batch.name = ''
  batch.type = 'COUPON'
  batch.scope = 'ALL'
  batch.amount = 5
  batch.minSpend = 0
  batch.quantity = 10
  batch.validDays = 30
  batch.description = ''
  createVisible.value = true
}

async function doCreate() {
  if (!batch.name.trim()) return ElMessage.warning('请填写模板名称')
  if (!batch.amount || batch.amount <= 0) return ElMessage.warning('面额须大于 0')
  creating.value = true
  try {
    const data = await adminCreateCouponBatch({ ...batch, name: batch.name.trim() })
    generated.value = data
    createVisible.value = false
    codesVisible.value = true
    ElMessage.success(`模板已创建，生成 ${data.codes.length} 个兑换码`)
    refresh()
  } finally { creating.value = false }
}

async function copyText(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch { ElMessage.warning('当前环境不支持剪贴板，请手动选择复制') }
}

onMounted(refresh)
</script>

<style scoped>
.amount { font-weight: 700; color: #f59e0b; }
.claim-cell { display: flex; align-items: center; gap: 8px; }
.claim-cell .el-progress { flex: 1; min-width: 60px; }
.claim-cell span { font-size: 11.5px; color: var(--art-gray-600); white-space: nowrap; }
.code-cell { font-weight: 700; letter-spacing: 1px; }
.form-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.gen-box { display: flex; flex-direction: column; gap: 12px; }
.gen-codes {
  display: flex; flex-wrap: wrap; gap: 8px; max-height: 300px; overflow: auto;
  padding: 12px; border-radius: 10px; border: 1px solid var(--default-border); background: var(--art-gray-100);
}
html.dark .gen-codes { background: #1c1c22; }
.gen-code {
  padding: 5px 10px; border-radius: 8px; font-size: 12.5px; font-weight: 700; cursor: pointer;
  border: 1px dashed rgba(245, 158, 11, 0.5); color: #b45309; background: rgba(245, 158, 11, 0.08);
  transition: transform 0.15s ease, background 0.15s ease;
}
.gen-code:hover { transform: translateY(-1px); background: rgba(245, 158, 11, 0.16); }
html.dark .gen-code { color: #fbbf24; }
@media (max-width: 640px) { .form-2col { grid-template-columns: 1fr; } }
</style>