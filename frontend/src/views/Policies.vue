<template>
  <div class="ap">
    <PageHero tone="policy" icon="Document" chip="POLICY CENTER"
      title="政策与协议"
      sub="服务条款、隐私政策、代取规则、钱包规则与取件公约全文在此。必备协议须签署后方可使用对应功能，版本更新会通知重新签署。"
      :stats="heroStats">
      <template #actions>
        <el-button v-if="pending.length" type="default" :icon="EditPen" @click="selectFirstPending">
          签署待办（{{ pending.length }}）
        </el-button>
        <el-button type="default" :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHero>

    <div v-if="pending.length" class="ap-note" style="--nc:#f59e0b">
      <el-icon><WarningFilled /></el-icon>
      <div>
        <b>有 {{ pending.length }} 份必备协议待签署：</b>
        <span v-for="(p, i) in pending" :key="p.id">
          <a class="pending-link" @click="select(p)">{{ p.title }} v{{ p.version }}</a><template v-if="i < pending.length - 1">、</template>
        </span>
        。签署后立即生效，签署记录含版本号、时间与来源 IP。
      </div>
    </div>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <div class="ap-row policy-row">
      <!-- 目录 -->
      <PanelCard title="协议目录" :note="`${list.length} 份`" icon="Notebook" accent="policy" :index="4">
        <template #extra>
          <el-select v-model="category" placeholder="全部分类" clearable size="small" style="width:130px" @change="load">
            <el-option v-for="(v, k) in POLICY_CATEGORY" :key="k" :label="v.label" :value="k" />
          </el-select>
        </template>
        <div class="cat-list">
          <button v-for="p in list" :key="p.id" class="cat-item" :class="{ on: current?.id === p.id }"
                  :style="{ '--pc': catColor(p.category) }" @click="select(p)">
            <span class="cat-bar" />
            <div class="cat-body">
              <div class="cat-l1">
                <b>{{ p.title }}</b>
                <el-tag v-if="p.required === 1" size="small" type="danger" effect="plain">必备</el-tag>
              </div>
              <div class="cat-l2">
                <span class="cat-code ap-mono">{{ p.code }}</span>
                <span>v{{ p.version }}</span>
                <span>{{ POLICY_CATEGORY[p.category]?.label || p.category }}</span>
              </div>
              <p class="cat-sum">{{ p.summary }}</p>
              <div class="cat-state">
                <template v-if="p.accepted">
                  <el-icon :size="13"><CircleCheckFilled /></el-icon>
                  已签署 v{{ p.acceptedVersion }} · {{ fmtTime(p.acceptedAt) }}
                </template>
                <template v-else>
                  <el-icon :size="13"><WarningFilled /></el-icon>未签署
                </template>
                <span class="cat-count">{{ p.acceptCount || 0 }} 人签署</span>
              </div>
            </div>
          </button>
          <div v-if="!list.length" class="ap-empty"><el-empty description="暂无协议" :image-size="64" /></div>
        </div>
      </PanelCard>

      <!-- 阅读器 -->
      <PanelCard :title="current ? current.title : '协议全文'" :note="current ? `v${current.version}` : ''"
                 icon="Reading" accent="policy" :index="5">
        <template #extra>
          <el-tag v-if="current" size="small" :type="current.status === 1 ? 'success' : 'info'" effect="light">
            {{ current.status === 1 ? '已发布' : '已下线' }}
          </el-tag>
          <el-button v-if="current" size="small" text type="primary" :icon="Download" @click="downloadTxt">导出文本</el-button>
        </template>

        <div v-if="current" class="reader">
          <div class="reader-progress"><i :style="{ width: progress + '%' }" /></div>
          <div class="reader-meta">
            <span :style="{ color: catColor(current.category) }"><el-icon :size="13"><CollectionTag /></el-icon>{{ POLICY_CATEGORY[current.category]?.label || current.category }}</span>
            <span><el-icon :size="13"><Clock /></el-icon>生效 {{ fmtTime(current.effectiveAt) }}</span>
            <span><el-icon :size="13"><User /></el-icon>{{ current.publisherName || '驿站运营方' }}</span>
            <span><el-icon :size="13"><Stamp /></el-icon>{{ current.acceptCount || 0 }} 人已签署</span>
          </div>
          <p class="reader-summary">{{ current.summary }}</p>

          <div ref="bodyRef" class="reader-body" @scroll="onScroll">
            <section v-for="(s, i) in sections" :key="i" class="reader-sec">
              <h3>{{ s.title }}</h3>
              <p v-for="(c, j) in s.clauses" :key="j">
                <b v-if="c.no">{{ c.no }}</b>{{ c.text }}
              </p>
            </section>
            <div v-if="!sections.length" class="ap-empty"><el-empty description="暂无正文" :image-size="64" /></div>
          </div>

          <div class="reader-foot">
            <div class="rf-state">
              <template v-if="current.accepted && current.acceptedVersion === current.version">
                <el-icon color="#10d9b5"><CircleCheckFilled /></el-icon>
                <span>你已于 {{ fmtTime(current.acceptedAt) }} 签署 v{{ current.acceptedVersion }}</span>
              </template>
              <template v-else-if="current.accepted">
                <el-icon color="#f59e0b"><WarningFilled /></el-icon>
                <span>已签署旧版 v{{ current.acceptedVersion }}，当前 v{{ current.version }} 需重新签署</span>
              </template>
              <template v-else>
                <el-icon color="#fb7185"><CircleCloseFilled /></el-icon>
                <span>{{ current.required === 1 ? '必备协议，尚未签署' : '可选协议，尚未签署' }}</span>
              </template>
            </div>
            <el-button v-if="needAccept" type="primary" :icon="EditPen" :loading="accepting" @click="doAccept">
              我已阅读并同意签署
            </el-button>
            <el-button v-else type="success" plain :icon="CircleCheck" disabled>已签署当前版本</el-button>
          </div>
        </div>
        <div v-else class="ap-empty">
          <el-empty description="请从左侧选择一份协议" :image-size="86" />
          <p>共 {{ list.length }} 份协议，其中 {{ requiredCount }} 份为必备条款。</p>
        </div>
      </PanelCard>
    </div>

    <!-- 签署记录 -->
    <PanelCard title="我的签署记录" :note="`${acceptedList.length} 条`" icon="Stamp" accent="mint" :index="6">
      <div class="ap-table-wrap">
        <el-table :data="sortedAccepted" stripe :default-sort="accSort.defaultSort" @sort-change="accSort.onSortChange">
          <el-table-column label="协议" prop="title" sortable="custom" min-width="220">
            <template #default="{ row }">
              <span class="ap-strong">{{ row.title }}</span>
              <div class="ap-sub ap-mono">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column label="分类" prop="category" sortable="custom" width="120">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :style="{ color: catColor(row.category), borderColor: catColor(row.category) }">
                {{ POLICY_CATEGORY[row.category]?.label || row.category }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签署版本" prop="acceptedVersion" sortable="custom" width="110">
            <template #default="{ row }"><span class="ap-mono ap-strong">v{{ row.acceptedVersion }}</span></template>
          </el-table-column>
          <el-table-column label="当前版本" prop="version" sortable="custom" width="110">
            <template #default="{ row }">
              <span class="ap-mono" :class="{ 'ver-old': row.acceptedVersion !== row.version }">v{{ row.version }}</span>
            </template>
          </el-table-column>
          <el-table-column label="签署时间" prop="acceptedAt" sortable="custom" width="160">
            <template #default="{ row }"><span class="ap-mono ap-sub2">{{ fmtTime(row.acceptedAt) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.acceptedVersion === row.version ? 'success' : 'warning'" effect="light">
                {{ row.acceptedVersion === row.version ? '最新已签' : '需重新签署' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ap-cards">
        <div v-for="row in sortedAccepted" :key="row.id" class="ap-card-item"
             :style="{ '--ci': row.acceptedVersion === row.version ? '#10d9b5' : '#f59e0b' }">
          <div class="ap-card-top">
            <b>{{ row.title }}</b>
            <el-tag size="small" :type="row.acceptedVersion === row.version ? 'success' : 'warning'" effect="light">
              {{ row.acceptedVersion === row.version ? '最新已签' : '需重签' }}
            </el-tag>
          </div>
          <div class="ap-card-grid">
            <div><label>编码</label><span class="ap-mono">{{ row.code }}</span></div>
            <div><label>签署版本</label><span>v{{ row.acceptedVersion }}</span></div>
            <div><label>当前版本</label><span>v{{ row.version }}</span></div>
            <div><label>签署时间</label><span>{{ fmtTime(row.acceptedAt) }}</span></div>
          </div>
        </div>
        <div v-if="!acceptedList.length" class="ap-empty"><el-empty description="暂无签署记录" :image-size="64" /></div>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, EditPen, Download, CircleCheck } from '@element-plus/icons-vue'
import PageHero from '../components/PageHero.vue'
import PanelCard from '../components/PanelCard.vue'
import MetricTile from '../components/MetricTile.vue'
import { policyList, policyDetail, acceptPolicies, pendingPolicies } from '../api'
import { POLICY_CATEGORY, fmtTime } from '../utils/format'
import { parsePolicy } from '../utils/policyParse'
import { useTableSort } from '../utils/tableSort'

const list = ref([])
const pending = ref([])
const category = ref('')
const current = ref(null)
const accepting = ref(false)
const bodyRef = ref(null)
const progress = ref(0)

const requiredCount = computed(() => list.value.filter(p => p.required === 1).length)
const acceptedList = computed(() => list.value.filter(p => p.accepted))
const accSort = useTableSort('acceptedAt')
const sortedAccepted = computed(() => accSort.sorted(acceptedList.value))
const needAccept = computed(() => current.value && (!current.value.accepted || current.value.acceptedVersion !== current.value.version))

const heroStats = computed(() => [
  { label: '协议总数', value: list.value.length, unit: '份' },
  { label: '我已签署', value: acceptedList.value.length, unit: '份' },
  { label: '待签署', value: pending.value.length, unit: '份' }
])
const metrics = computed(() => [
  { label: '必备协议', value: requiredCount.value, unit: '份', color: '#6366f1', icon: 'Document', foot: '未签署将限制相关功能' },
  { label: '已签最新版本', value: acceptedList.value.filter(p => p.acceptedVersion === p.version).length, unit: '份', color: '#10d9b5', icon: 'CircleCheck', foot: '版本号一致视为有效' },
  { label: '需重新签署', value: acceptedList.value.filter(p => p.acceptedVersion !== p.version).length + pending.value.length, unit: '份', color: '#f59e0b', icon: 'Warning', foot: '协议修订后需重新确认' },
  { label: '累计签署人次', value: list.value.reduce((s, p) => s + Number(p.acceptCount || 0), 0), unit: '次', color: '#8b5cf6', icon: 'Stamp', foot: '全平台签署统计' }
])

const sections = computed(() => parsePolicy(current.value?.content))

function catColor(c) { return POLICY_CATEGORY[c]?.color || '#6366f1' }

async function load() {
  const [l, p] = await Promise.all([
    policyList({ category: category.value || undefined }),
    pendingPolicies().catch(() => [])
  ])
  list.value = l || []
  pending.value = p || []
  if (current.value) {
    const found = list.value.find(x => x.id === current.value.id)
    if (found) current.value = found
  } else if (list.value.length) {
    await select(pending.value[0] || list.value[0])
  }
}

async function select(p) {
  progress.value = 0
  const d = await policyDetail(p.code).catch(() => p)
  current.value = d
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = 0
}
function selectFirstPending() { if (pending.value[0]) select(pending.value[0]) }

function onScroll() {
  const el = bodyRef.value
  if (!el) return
  const max = el.scrollHeight - el.clientHeight
  progress.value = max > 0 ? Math.min(100, Math.round((el.scrollTop / max) * 100)) : 100
}

async function doAccept() {
  accepting.value = true
  try {
    await acceptPolicies([current.value.id])
    ElMessage.success(`已签署《${current.value.title}》v${current.value.version}`)
    const code = current.value.code
    await load()
    const found = list.value.find(x => x.code === code)
    if (found) current.value = await policyDetail(code)
  } finally { accepting.value = false }
}

function downloadTxt() {
  if (!current.value) return
  const text = `${current.value.title}（v${current.value.version}）\n编码：${current.value.code}\n生效：${fmtTime(current.value.effectiveAt)}\n\n${current.value.content}`
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${current.value.code}-v${current.value.version}.txt`
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  ElMessage.success('协议文本已导出')
}

onMounted(load)
</script>

<style scoped>
.policy-row { grid-template-columns: minmax(0, 1fr) minmax(0, 1.7fr); }
.ap-sub2 { font-size: 12px; color: var(--art-gray-600); }
.pending-link { color: var(--el-color-primary); font-weight: 700; cursor: pointer; text-decoration: underline; text-underline-offset: 2px; }
.ver-old { color: #d97706; font-weight: 700; }

/* 目录 */
.cat-list { display: flex; flex-direction: column; gap: 9px; }
.cat-item {
  position: relative; display: flex; gap: 0; text-align: left;
  border: 1px solid var(--default-border);
  border-radius: var(--art-radius-card-sm);
  background: var(--default-box-color);
  padding: 0; cursor: pointer; overflow: hidden;
  transition: all 0.26s var(--flair-ease);
}
.cat-item:hover { border-color: var(--pc); transform: translateX(3px); box-shadow: 0 10px 22px -16px var(--pc); }
.cat-item.on { border-color: var(--pc); background: color-mix(in srgb, var(--pc) 7%, transparent); box-shadow: 0 10px 24px -16px var(--pc); }
.cat-bar { width: 3px; flex-shrink: 0; background: var(--pc); opacity: 0.35; transition: opacity 0.25s; }
.cat-item.on .cat-bar, .cat-item:hover .cat-bar { opacity: 1; }
.cat-body { padding: 11px 13px; min-width: 0; flex: 1; }
.cat-l1 { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.cat-l1 b { font-size: 13.5px; color: var(--art-gray-900); line-height: 1.4; }
.cat-l2 { display: flex; gap: 9px; flex-wrap: wrap; margin-top: 4px; font-size: 10.5px; color: var(--art-gray-500); }
.cat-code { letter-spacing: 0.4px; }
.cat-sum {
  margin: 6px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--art-gray-600);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.cat-state { display: flex; align-items: center; gap: 5px; margin-top: 7px; font-size: 11px; color: var(--art-gray-600); flex-wrap: wrap; }
.cat-state .el-icon { color: var(--pc); }
.cat-count { margin-left: auto; color: var(--art-gray-500); }

/* 阅读器 */
.reader { display: flex; flex-direction: column; }
.reader-progress { height: 3px; border-radius: 3px; background: var(--art-gray-200); overflow: hidden; margin-bottom: 12px; }
.reader-progress i { display: block; height: 100%; width: 0; background: linear-gradient(90deg, #6366f1, #8b5cf6); transition: width 0.15s linear; }
html.dark .reader-progress { background: #23232e; }
.reader-meta { display: flex; gap: 14px; flex-wrap: wrap; font-size: 11.5px; color: var(--art-gray-500); }
.reader-meta span { display: inline-flex; align-items: center; gap: 4px; }
.reader-summary {
  margin: 11px 0 13px; padding: 11px 13px;
  font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700);
  background: color-mix(in srgb, #6366f1 7%, transparent);
  border-left: 3px solid #6366f1; border-radius: 0 10px 10px 0;
}
.reader-body {
  max-height: 460px; overflow-y: auto;
  padding: 4px 14px 4px 2px;
  border-top: 1px dashed var(--default-border);
  border-bottom: 1px dashed var(--default-border);
}
.reader-sec { padding: 14px 0 4px; }
.reader-sec h3 {
  margin: 0 0 9px; font-size: 14px; font-weight: 800; color: var(--art-gray-900);
  display: flex; align-items: center; gap: 8px;
}
.reader-sec h3::before { content: ''; width: 4px; height: 15px; border-radius: 3px; background: linear-gradient(180deg, #6366f1, #8b5cf6); }
.reader-sec p { margin: 0 0 8px; font-size: 12.8px; line-height: 1.85; color: var(--art-gray-700); text-align: justify; }
.reader-sec p b { color: var(--ap-policy); font-weight: 800; margin-right: 6px; font-variant-numeric: tabular-nums; }
.reader-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; padding-top: 13px; }
.rf-state { display: flex; align-items: center; gap: 7px; font-size: 12px; color: var(--art-gray-600); min-width: 0; }
.rf-state .el-icon { font-size: 15px; flex-shrink: 0; }

@media (max-width: 1024px) {
  .policy-row { grid-template-columns: minmax(0, 1fr); }
  .reader-body { max-height: 420px; }
}
@media (max-width: 560px) {
  .reader-body { max-height: 340px; }
  .reader-foot .el-button { width: 100%; }
}
</style>