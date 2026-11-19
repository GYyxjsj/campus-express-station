<template>
  <div class="ap">
    <PageHero tone="admin" icon="Document" chip="AUDIT TRAIL" live title="操作日志"
      sub="全站敏感操作的审计留痕：谁在何时对哪个对象做了什么，入参出参自动脱敏。点击任意记录可展开完整可读详情。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
        <el-button type="default" :icon="User" @click="$router.push('/admin/users')">用户管理</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="审计台账" :note="`${total} 条记录`" icon="Tickets" accent="slate" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field">
          <label>模块</label>
          <el-select v-model="query.module" clearable placeholder="全部模块" @change="load(1)">
            <el-option v-for="(label, key) in MODULES" :key="key" :label="label" :value="key" />
          </el-select>
        </div>
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" :prefix-icon="Search" clearable placeholder="操作人 / 动作关键词"
                    @keyup.enter="load(1)" @clear="load(1)" />
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="rows" stripe :default-sort="defaultSort" @sort-change="e => { onSortChange(e); load(1) }" @row-click="openDetail">
            <el-table-column label="时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" sortable="custom" width="100" />
            <el-table-column label="模块" prop="module" sortable="custom" width="106">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="moduleTag(row.module)">{{ moduleLabel(row.module) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="动作" sortable="custom" width="160" show-overflow-tooltip />
            <el-table-column label="详情（已脱敏）" min-width="260">
              <template #default="{ row }"><span class="ap-ellipsis detail-cell">{{ prettyDetail(row) }}</span></template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="126">
              <template #default="{ row }"><span class="ap-mono">{{ row.ip }}</span></template>
            </el-table-column>
            <el-table-column label="结果" prop="success" sortable="custom" width="80" fixed="right">
              <template #default="{ row }">
                <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small" effect="light">
                  {{ row.success === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in rows" :key="row.id" class="ap-card-item" @click="openDetail(row)">
            <div class="ap-card-top">
              <el-tag size="small" effect="plain" :type="moduleTag(row.module)">{{ moduleLabel(row.module) }}</el-tag>
              <b>{{ row.action }}</b>
              <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small" effect="light">
                {{ row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>操作人</label><span>{{ row.operatorName }}</span></div>
              <div><label>时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
              <div><label>IP</label><span class="ap-mono">{{ row.ip }}</span></div>
              <div><label>对象 ID</label><span class="ap-mono">{{ row.targetId ?? '—' }}</span></div>
            </div>
            <p class="card-detail">{{ prettyDetail(row) }}</p>
          </article>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="没有匹配的日志" :image-size="86" />
          <p>调整模块或关键词筛选后重试。</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <el-drawer v-model="detailVisible" title="日志详情" size="460px" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-head">
          <el-tag effect="plain" :type="moduleTag(current.module)">{{ moduleLabel(current.module) }}</el-tag>
          <b>{{ current.action }}</b>
          <el-tag :type="current.success === 1 ? 'success' : 'danger'" effect="light" size="small">
            {{ current.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </div>
        <div class="ap-kv">
          <div><label>操作人</label><span>{{ current.operatorName }}</span></div>
          <div><label>时间</label><span class="ap-mono">{{ fmtTime(current.createdAt) }}</span></div>
          <div><label>对象 ID</label><span class="ap-mono">{{ current.targetId ?? '—' }}</span></div>
          <div><label>来源 IP</label><span class="ap-mono">{{ current.ip }}</span></div>
        </div>
        <h4 class="d-h">可读摘要</h4>
        <div class="ap-note" style="--nc:#5b7cfa">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ prettyDetail(current) }}</span>
        </div>
        <h4 class="d-h">原始详情（已脱敏）</h4>
        <pre class="raw">{{ current.detail || '—' }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Document, InfoFilled, Refresh, RefreshLeft, Search, Tickets, User } from '@element-plus/icons-vue'
import { pageLogs } from '../../api'
import { fmtTime } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { prettyDetail, moduleLabel } from '../../utils/logDetail'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, module: '', keyword: '' })
const { defaultSort, onSortChange, sortParams } = useServerSort('createdAt')
const detailVisible = ref(false)
const current = ref(null)

const MODULES = {
  auth: '认证授权', parcel: '包裹', reminder: '提醒', admin: '系统管理',
  proxy: '代取服务', wallet: '钱包', policy: '协议', identity: '身份条码'
}
const MODULE_TAG = {
  auth: 'primary', parcel: 'warning', reminder: 'info', admin: 'danger',
  proxy: 'success', wallet: 'warning', policy: 'primary', identity: 'success'
}
const moduleTag = m => MODULE_TAG[m] || 'info'

const okCount = computed(() => rows.value.filter(r => r.success === 1).length)
const failCount = computed(() => rows.value.length - okCount.value)
const operators = computed(() => new Set(rows.value.map(r => r.operatorName)).size)

const heroStats = computed(() => [
  { label: '命中记录', value: total.value, unit: '条' },
  { label: '本页成功', value: okCount.value, unit: '条' },
  { label: '本页失败', value: failCount.value, unit: '条' }
])
const metrics = computed(() => [
  { label: '命中记录', value: total.value, unit: '条', icon: 'Tickets', color: '#5b7cfa', foot: '当前筛选条件命中' },
  { label: '本页成功', value: okCount.value, unit: '条', icon: 'CircleCheck', color: '#10d9b5', foot: '操作正常完成' },
  { label: '本页失败', value: failCount.value, unit: '条', icon: 'CircleClose', color: '#e11d48', foot: '含权限拦截与校验失败' },
  { label: '涉及操作人', value: operators.value, unit: '人', icon: 'User', color: '#f59e0b', foot: '本页去重统计' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await pageLogs({
      page: query.page, size: query.size,
      module: query.module || undefined, keyword: query.keyword || undefined,
      ...sortParams()
    })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function resetQuery() {
  query.module = ''
  query.keyword = ''
  load(1)
}

function openDetail(row) { current.value = row; detailVisible.value = true }

onMounted(() => load())
</script>

<style scoped>
.detail-cell { display: block; max-width: 420px; font-size: 12px; color: var(--art-gray-600); }
.card-detail { margin: 8px 0 0; font-size: 12px; color: var(--art-gray-600); line-height: 1.6; }
.detail { display: flex; flex-direction: column; gap: 12px; }
.d-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.d-head b { font-size: 15px; color: var(--art-gray-900); }
.d-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 4px; }
.raw {
  margin: 0; padding: 12px; border-radius: 10px; border: 1px solid var(--default-border);
  background: var(--art-gray-100); color: var(--art-gray-700);
  font-family: 'JetBrains Mono', Consolas, monospace; font-size: 11.5px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-all; max-height: 320px; overflow: auto;
}
html.dark .raw { background: #1c1c22; }
</style>