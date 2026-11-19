<template>
  <div class="ap">
    <PageHero tone="policy" icon="Document" chip="POLICY GOVERNANCE" live
      title="政策协议治理"
      sub="维护服务条款、隐私保护、代取规则、钱包规则与校园公约。必签协议升版后会自动通知全部已签署用户重新确认，未签署者将被限制相关服务。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Plus" @click="openEdit()">新建协议</el-button>
        <el-button type="default" :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
      </template>
    </PageHero>

    <div class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </div>

    <div class="ap-row wide-left">
      <!-- 协议目录 -->
      <PanelCard title="协议目录" :note="`共 ${total} 份`" icon="Collection" accent="policy" :index="1">
        <template #extra>
          <el-button type="primary" size="small" :icon="Plus" @click="openEdit()">新建</el-button>
        </template>
        <div class="ap-toolbar">
          <div class="ap-field ap-grow">
            <label>关键词</label>
            <el-input v-model="query.keyword" placeholder="编码 / 标题 / 摘要" clearable :prefix-icon="Search"
                      @keyup.enter="search" @clear="search" />
          </div>
          <div class="ap-field">
            <label>分类</label>
            <el-select v-model="query.category" placeholder="全部分类" clearable style="width:150px" @change="search">
              <el-option v-for="(v, k) in POLICY_CATEGORY" :key="k" :label="v.label" :value="k" />
            </el-select>
          </div>
          <div class="ap-actions">
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
          </div>
        </div>
        <div class="ap-seg">
          <button :class="{ on: query.status === null }" @click="pickStatus(null)">全部<span class="n">{{ total }}</span></button>
          <button :class="{ on: query.status === 1 }" @click="pickStatus(1)">已发布</button>
          <button :class="{ on: query.status === 0 }" @click="pickStatus(0)">已下线</button>
        </div>

        <div v-loading="listLoading" class="ap-table-wrap">
          <table class="ap-table">
            <thead>
              <tr>
                <SortTh prop="code" label="编码 / 版本" width="150px" :sort="sorter" @sort="search" />
                <SortTh prop="title" label="标题与摘要" :sort="sorter" @sort="search" />
                <SortTh prop="category" label="分类" width="110px" :sort="sorter" @sort="search" />
                <SortTh prop="required" label="必签" width="90px" :sort="sorter" @sort="search" />
                <SortTh prop="acceptCount" label="签署人次" width="100px" :sort="sorter" @sort="search" />
                <SortTh prop="status" label="状态" width="90px" :sort="sorter" @sort="search" />
                <th style="width:180px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in rows" :key="p.id" @click="openPreview(p)">
                <td>
                  <span class="ap-mono">{{ p.code }}</span>
                  <div class="ap-sub">v{{ p.version }}</div>
                </td>
                <td>
                  <b class="ap-strong">{{ p.title }}</b>
                  <div class="ap-sub ap-ellipsis">{{ p.summary || '暂无摘要' }}</div>
                </td>
                <td><span class="cat-chip" :style="{ '--nc': catColor(p.category) }">{{ catLabel(p.category) }}</span></td>
                <td>
                  <el-tag size="small" :type="p.required === 1 ? 'danger' : 'info'" effect="light">
                    {{ p.required === 1 ? '必签' : '可选' }}
                  </el-tag>
                </td>
                <td><b class="sign-num">{{ p.acceptCount || 0 }}</b></td>
                <td>
                  <el-tag size="small" :type="p.status === 1 ? 'success' : 'info'" effect="light">
                    {{ p.status === 1 ? '已发布' : '已下线' }}
                  </el-tag>
                </td>
                <td @click.stop>
                  <div class="ap-ops">
                    <el-button text type="primary" size="small" :icon="View" @click="openPreview(p)">预览</el-button>
                    <el-button text type="warning" size="small" :icon="EditPen" @click="openEdit(p)">修订</el-button>
                    <el-button text :type="p.status === 1 ? 'danger' : 'success'" size="small" :icon="p.status === 1 ? CircleClose : Stamp" @click="toggleStatus(p)">
                      {{ p.status === 1 ? '下线' : '发布' }}
                    </el-button>
                  </div>
                </td>
              </tr>
              <tr v-if="!rows.length && !listLoading">
                <td colspan="7"><div class="ap-empty"><el-empty description="没有匹配的协议" :image-size="80" /></div></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-loading="listLoading" class="ap-cards">
          <article v-for="p in rows" :key="p.id" class="ap-card-item" :style="{ '--ci': catColor(p.category) }" @click="openPreview(p)">
            <div class="ap-card-top">
              <span class="cat-chip" :style="{ '--nc': catColor(p.category) }">{{ catLabel(p.category) }}</span>
              <el-tag size="small" :type="p.status === 1 ? 'success' : 'info'" effect="light">{{ p.status === 1 ? '已发布' : '已下线' }}</el-tag>
            </div>
            <b class="ap-strong">{{ p.title }}</b>
            <div class="ap-card-grid">
              <div><label>编码</label><span class="ap-mono">{{ p.code }}</span></div>
              <div><label>版本</label><span>v{{ p.version }}</span></div>
              <div><label>签署人次</label><span>{{ p.acceptCount || 0 }}</span></div>
              <div><label>约束力</label><span>{{ p.required === 1 ? '必签' : '可选' }}</span></div>
            </div>
            <p v-if="p.summary" class="card-summary">{{ p.summary }}</p>
            <div class="ap-card-actions" @click.stop>
              <el-button size="small" type="primary" plain :icon="View" @click="openPreview(p)">预览</el-button>
              <el-button size="small" type="warning" plain :icon="EditPen" @click="openEdit(p)">修订</el-button>
              <el-button size="small" :type="p.status === 1 ? 'danger' : 'success'" plain :icon="p.status === 1 ? CircleClose : Stamp" @click="toggleStatus(p)">
                {{ p.status === 1 ? '下线' : '发布' }}
              </el-button>
            </div>
          </article>
          <div v-if="!rows.length && !listLoading" class="ap-empty"><el-empty description="没有匹配的协议" :image-size="80" /></div>
        </div>

        <template #foot>
          <div class="ap-pager">
            <el-pagination background layout="total, prev, pager, next" :total="total"
                           v-model:current-page="query.page" v-model:page-size="query.size"
                           @current-change="loadRows" />
          </div>
        </template>
      </PanelCard>

      <!-- 分类与热度 -->
      <div class="side-col">
        <PanelCard title="分类构成" note="按协议份数" icon="Files" accent="policy" :index="2">
          <div class="cat-bars">
            <div v-for="c in catRows" :key="c.key" class="cb-row" :style="{ '--nc': c.color }">
              <div class="cb-head"><span>{{ c.label }}</span><b>{{ c.count }} 份</b></div>
              <div class="cb-track"><i :style="{ width: c.pct + '%' }" /></div>
              <div class="cb-foot">必签 {{ c.required }} · 签署 {{ c.signs }} 人次</div>
            </div>
            <div v-if="!catRows.length" class="ap-empty"><el-empty description="暂无协议" :image-size="60" /></div>
          </div>
        </PanelCard>

        <PanelCard title="签署热度 Top 5" note="按累计签署人次" icon="TrendCharts" accent="mint" :index="3">
          <div v-if="heatRows.length" class="ap-list">
            <div v-for="(p, i) in heatRows" :key="p.id" class="ap-list-item" :style="{ '--la': catColor(p.category) }">
              <div class="ap-list-avatar">{{ i + 1 }}</div>
              <div class="ap-list-body">
                <div class="ap-list-l1"><b class="heat-title">{{ p.title }}</b></div>
                <div class="ap-list-l2">{{ p.code }} · v{{ p.version }} · {{ catLabel(p.category) }}</div>
              </div>
              <div class="ap-list-right"><b class="sign-num">{{ p.acceptCount || 0 }}</b></div>
            </div>
          </div>
          <div v-else class="ap-empty"><el-empty description="暂无签署数据" :image-size="60" /></div>
        </PanelCard>
      </div>
    </div>

    <!-- 签署台账 -->
    <PanelCard title="用户签署台账" :note="`共 ${accTotal} 条签署记录`" icon="Stamp" accent="mint" :index="4">
      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="accQuery.keyword" placeholder="学号 / 姓名 / 协议标题" clearable :prefix-icon="Search"
                    @keyup.enter="searchAcc" @clear="searchAcc" />
        </div>
        <div class="ap-field">
          <label>协议</label>
          <el-select v-model="accQuery.policyId" placeholder="全部协议" clearable filterable style="width:230px" @change="searchAcc">
            <el-option v-for="p in allPolicies" :key="p.id" :label="`${p.title}（v${p.version}）`" :value="p.id" />
          </el-select>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="searchAcc">查询</el-button>
          <el-button :icon="RefreshLeft" @click="accQuery.keyword = ''; accQuery.policyId = null; searchAcc()">重置</el-button>
        </div>
      </div>

      <div v-loading="accLoading" class="ap-table-wrap">
        <table class="ap-table">
          <thead>
            <tr>
              <SortTh prop="acceptedAt" label="签署时间" width="160px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="realName" label="用户" width="180px" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="policyTitle" label="协议" :sort="accSorter" @sort="searchAcc" />
              <SortTh prop="version" label="签署版本" width="100px" :sort="accSorter" @sort="searchAcc" />
              <th style="width:110px">当前版本</th>
              <SortTh prop="ip" label="来源 IP" width="140px" :sort="accSorter" @sort="searchAcc" />
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in acceptances" :key="a.id">
              <td class="ap-mono">{{ fmtTime(a.acceptedAt) }}</td>
              <td><b class="ap-strong">{{ a.realName }}</b><div class="ap-sub ap-mono">{{ a.username }}</div></td>
              <td>
                <b class="ap-strong">{{ a.policyTitle }}</b>
                <div class="ap-sub ap-mono">{{ a.policyCode }}</div>
              </td>
              <td><span class="ver-chip">v{{ a.version }}</span></td>
              <td>
                <span v-if="latestVersion(a) === a.version" class="ver-ok"><el-icon :size="12"><CircleCheckFilled /></el-icon>v{{ a.version }}</span>
                <span v-else class="ver-old"><el-icon :size="12"><WarningFilled /></el-icon>v{{ latestVersion(a) }}</span>
              </td>
              <td class="ap-mono ap-sub">{{ a.ip || '—' }}</td>
            </tr>
            <tr v-if="!acceptances.length && !accLoading">
              <td colspan="6"><div class="ap-empty"><el-empty description="没有匹配的签署记录" :image-size="80" /></div></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-loading="accLoading" class="ap-cards">
        <article v-for="a in acceptances" :key="a.id" class="ap-card-item"
                 :style="{ '--ci': latestVersion(a) === a.version ? '#10d9b5' : '#f59e0b' }">
          <div class="ap-card-top">
            <b class="ap-strong">{{ a.realName }}<span class="ap-sub ap-mono"> {{ a.username }}</span></b>
            <span class="ver-chip">v{{ a.version }}</span>
          </div>
          <b class="ap-strong">{{ a.policyTitle }}</b>
          <div class="ap-card-grid">
            <div><label>协议编码</label><span class="ap-mono">{{ a.policyCode }}</span></div>
            <div><label>签署时间</label><span class="ap-mono">{{ fmtTime(a.acceptedAt) }}</span></div>
            <div><label>当前版本</label><span>v{{ latestVersion(a) }}</span></div>
            <div><label>来源 IP</label><span class="ap-mono">{{ a.ip || '—' }}</span></div>
          </div>
        </article>
        <div v-if="!acceptances.length && !accLoading" class="ap-empty"><el-empty description="没有匹配的签署记录" :image-size="80" /></div>
      </div>

      <template #foot>
        <div class="ap-pager">
          <el-pagination background layout="total, sizes, prev, pager, next" :total="accTotal"
                         v-model:current-page="accQuery.page" v-model:page-size="accQuery.size"
                         :page-sizes="[10, 20, 50]" @current-change="loadAcceptances" @size-change="searchAcc" />
        </div>
      </template>
    </PanelCard>

    <!-- 预览抽屉 -->
    <el-drawer v-model="previewOpen" :title="preview?.title || '协议预览'" size="600px">
      <template v-if="preview">
        <div class="pv-head" :style="{ '--nc': catColor(preview.category) }">
          <span class="cat-chip" :style="{ '--nc': catColor(preview.category) }">{{ catLabel(preview.category) }}</span>
          <el-tag size="small" :type="preview.required === 1 ? 'danger' : 'info'" effect="light">{{ preview.required === 1 ? '必签协议' : '可选协议' }}</el-tag>
          <el-tag size="small" :type="preview.status === 1 ? 'success' : 'info'" effect="light">{{ preview.status === 1 ? '已发布' : '已下线' }}</el-tag>
        </div>
        <div class="ap-kv">
          <div><label>协议编码</label><span class="ap-mono">{{ preview.code }}</span></div>
          <div><label>当前版本</label><span>v{{ preview.version }}</span></div>
          <div><label>生效时间</label><span class="ap-mono">{{ fmtTime(preview.effectiveAt) }}</span></div>
          <div><label>发布人</label><span>{{ preview.publisherName || '—' }}</span></div>
          <div><label>签署人次</label><span>{{ preview.acceptCount || 0 }}</span></div>
          <div><label>最后修订</label><span class="ap-mono">{{ fmtTime(preview.updatedAt) }}</span></div>
        </div>
        <p v-if="preview.summary" class="pv-summary">{{ preview.summary }}</p>
        <div class="pv-body">
          <section v-for="(s, i) in previewSections" :key="i" class="pv-sec">
            <h3>{{ s.title }}</h3>
            <p v-for="(c, j) in s.clauses" :key="j"><b v-if="c.no">{{ c.no }}</b>{{ c.text }}</p>
          </section>
          <div v-if="!previewSections.length" class="ap-empty"><el-empty description="暂无正文" :image-size="64" /></div>
        </div>
        <div class="pv-actions">
          <el-button type="primary" :icon="EditPen" @click="openEdit(preview)">修订此协议</el-button>
          <el-button :icon="Download" @click="exportText">导出文本</el-button>
          <el-button :icon="Stamp" @click="viewSigners(preview)">查看签署人</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 编辑 / 新建 -->
    <el-dialog v-model="editOpen" :title="form.id ? '修订协议' : '新建协议'" width="880px" append-to-body top="6vh">
      <div class="ap-form">
        <div class="edit-grid">
          <div class="ap-field-row">
            <label>协议编码<em>*</em></label>
            <el-input v-model="form.code" :disabled="!!form.id" maxlength="32" placeholder="如 SERVICE_TERMS" />
            <p v-if="form.id" class="ap-form-tip">编码创建后不可修改，用于系统内部引用。</p>
          </div>
          <div class="ap-field-row">
            <label>版本号<em>*</em></label>
            <el-input v-model="form.version" maxlength="16" placeholder="如 1.0 / 2.1" />
            <p class="ap-form-tip">必签协议升版后，系统自动通知全部已签署用户重新签署。</p>
          </div>
        </div>
        <div class="ap-field-row">
          <label>协议标题<em>*</em></label>
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="如：校园快递驿站服务条款" />
        </div>
        <div class="edit-grid">
          <div class="ap-field-row">
            <label>分类</label>
            <el-select v-model="form.category" style="width:100%">
              <el-option v-for="(v, k) in POLICY_CATEGORY" :key="k" :label="v.label" :value="k" />
            </el-select>
          </div>
          <div class="ap-field-row">
            <label>约束与状态</label>
            <div class="switch-row">
              <span>必签协议</span>
              <el-switch v-model="form.required" :active-value="1" :inactive-value="0" />
              <span class="sr-gap">立即发布</span>
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </div>
          </div>
        </div>
        <div class="ap-field-row">
          <label>摘要</label>
          <el-input v-model="form.summary" maxlength="255" show-word-limit placeholder="一句话说明协议适用范围与核心约束" />
        </div>
        <div class="ap-field-row">
          <label>协议正文<em>*</em></label>
          <div class="editor-head">
            <span class="eh-tip">使用 <code># 章节标题</code> 分节，<code>1.1 条款内容</code> 自动编号</span>
            <div class="eh-actions">
              <el-button size="small" text :icon="MagicStick" @click="form.content = POLICY_TEMPLATE">插入模板</el-button>
              <el-button size="small" text :icon="View" @click="showPreview = !showPreview">
                {{ showPreview ? '隐藏预览' : '实时预览' }}
              </el-button>
            </div>
          </div>
          <div class="editor-body" :class="{ split: showPreview }">
            <el-input v-model="form.content" type="textarea" :rows="showPreview ? 16 : 12"
                      placeholder="# 一、总则&#10;1.1 ..." class="editor-input" />
            <div v-if="showPreview" class="editor-preview">
              <section v-for="(s, i) in formSections" :key="i" class="pv-sec">
                <h3>{{ s.title }}</h3>
                <p v-for="(c, j) in s.clauses" :key="j"><b v-if="c.no">{{ c.no }}</b>{{ c.text }}</p>
              </section>
              <div v-if="!formSections.length" class="ap-empty"><el-empty description="正文为空" :image-size="56" /></div>
            </div>
          </div>
          <p class="ap-form-tip">正文 {{ form.content.length }} 字 · {{ formSections.length }} 个章节 · {{ clauseCount }} 条条款</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" :icon="Check" @click="submit">
          {{ form.id ? '保存修订' : '创建协议' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshLeft, Search, Check, EditPen, Download, Stamp, MagicStick, View, CircleClose } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'
import SortTh from '../../components/SortTh.vue'
import {
  adminPolicyStats, adminPolicyPage, adminPolicyAcceptances,
  adminCreatePolicy, adminUpdatePolicy, adminPolicyStatus
} from '../../api'
import { POLICY_CATEGORY, fmtTime } from '../../utils/format'
import { parsePolicy, POLICY_TEMPLATE } from '../../utils/policyParse'
import { useServerSort } from '../../utils/tableSort'

const stats = ref({})
const allPolicies = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const listLoading = ref(false)
const query = reactive({ page: 1, size: 10, category: '', status: null, keyword: '' })
const sorter = useServerSort('updatedAt')

const heroStats = computed(() => [
  { label: '协议总数', value: Number(stats.value.policies || 0), unit: '份' },
  { label: '已发布生效', value: Number(stats.value.published || 0), unit: '份' },
  { label: '必签协议', value: Number(stats.value.required || 0), unit: '份' },
  { label: '累计签署', value: Number(stats.value.acceptances || 0), unit: '人次' }
])
const metrics = computed(() => [
  { label: '协议总数', value: Number(stats.value.policies || 0), foot: `已发布 ${stats.value.published || 0} 份`, color: '#6366f1', icon: 'Collection' },
  { label: '必签协议', value: Number(stats.value.required || 0), foot: '未签署将限制服务', color: '#fb7185', icon: 'Warning' },
  { label: '累计签署人次', value: Number(stats.value.acceptances || 0), foot: `今日新增 ${stats.value.today || 0} 次`, color: '#10d9b5', icon: 'Stamp' },
  { label: '签署用户数', value: Number(stats.value.signers || 0), foot: '去重后的独立用户', color: '#8b5cf6', icon: 'User' }
])

function catLabel(c) { return POLICY_CATEGORY[c]?.label || c || '—' }
function catColor(c) { return POLICY_CATEGORY[c]?.color || '#6366f1' }

const catRows = computed(() => {
  const keys = Object.keys(POLICY_CATEGORY)
  const list = keys.map(k => {
    const items = allPolicies.value.filter(p => p.category === k)
    return {
      key: k, label: POLICY_CATEGORY[k].label, color: POLICY_CATEGORY[k].color,
      count: items.length,
      required: items.filter(p => p.required === 1).length,
      signs: items.reduce((s, p) => s + Number(p.acceptCount || 0), 0)
    }
  }).filter(c => c.count > 0)
  const max = Math.max(1, ...list.map(c => c.count))
  return list.map(c => ({ ...c, pct: Math.round((c.count / max) * 100) }))
})
const heatRows = computed(() =>
  [...allPolicies.value].sort((a, b) => Number(b.acceptCount || 0) - Number(a.acceptCount || 0)).slice(0, 5)
)
function latestVersion(a) {
  return allPolicies.value.find(p => p.id === a.policyId)?.version || a.version
}

async function loadStats() { stats.value = await adminPolicyStats() }
async function loadAll() {
  const r = await adminPolicyPage({ page: 1, size: 100 })
  allPolicies.value = r.records || []
}
async function loadRows() {
  listLoading.value = true
  try {
    const r = await adminPolicyPage({
      page: query.page, size: query.size,
      category: query.category || undefined,
      status: query.status === null ? undefined : query.status,
      keyword: query.keyword?.trim() || undefined,
      ...sorter.sortParams()
    })
    rows.value = r.records || []
    total.value = Number(r.total || 0)
  } finally { listLoading.value = false }
}
function search() { query.page = 1; loadRows() }
function resetQuery() { query.category = ''; query.status = null; query.keyword = ''; search() }
function pickStatus(s) { query.status = query.status === s ? null : s; search() }

/* ---- 签署台账 ---- */
const acceptances = ref([])
const accTotal = ref(0)
const accLoading = ref(false)
const accQuery = reactive({ page: 1, size: 10, policyId: null, keyword: '' })
const accSorter = useServerSort('acceptedAt')
async function loadAcceptances() {
  accLoading.value = true
  try {
    const r = await adminPolicyAcceptances({
      page: accQuery.page, size: accQuery.size,
      policyId: accQuery.policyId || undefined, keyword: accQuery.keyword?.trim() || undefined,
      ...accSorter.sortParams()
    })
    acceptances.value = r.records || []
    accTotal.value = Number(r.total || 0)
  } finally { accLoading.value = false }
}
function searchAcc() { accQuery.page = 1; loadAcceptances() }
function viewSigners(p) {
  accQuery.policyId = p.id
  searchAcc()
  previewOpen.value = false
  ElMessage.success(`已筛选《${p.title}》的签署记录`)
}

/* ---- 预览 ---- */
const previewOpen = ref(false)
const preview = ref(null)
const previewSections = computed(() => parsePolicy(preview.value?.content))
function openPreview(p) { preview.value = p; previewOpen.value = true }
function exportText() {
  const p = preview.value
  const text = `${p.title}（${p.code} v${p.version}）\n分类：${catLabel(p.category)}　生效：${fmtTime(p.effectiveAt)}\n\n${p.content}\n`
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url; a.download = `${p.code}_v${p.version}.txt`; a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('协议文本已导出')
}

/* ---- 上下线 ---- */
async function toggleStatus(p) {
  const off = p.status === 1
  try {
    await ElMessageBox.confirm(
      off
        ? `下线后《${p.title}》将不再对学生展示，${p.required === 1 ? '且不再作为必签项拦截服务。' : ''}确定下线吗？`
        : `发布后《${p.title}》${p.required === 1 ? '将作为必签协议，未签署用户会被要求先签署。' : '将对学生可见。'}确定发布吗？`,
      off ? '下线协议' : '发布协议', { type: 'warning' }
    )
  } catch { return }
  try {
    await adminPolicyStatus(p.id, off ? 0 : 1)
    ElMessage.success(off ? '协议已下线' : '协议已发布')
    await refreshAll()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

/* ---- 编辑 ---- */
const editOpen = ref(false)
const saving = ref(false)
const showPreview = ref(true)
const form = reactive({ id: null, code: '', title: '', version: '1.0', category: 'SERVICE', summary: '', content: '', required: 0, status: 1 })
const formSections = computed(() => parsePolicy(form.content))
const clauseCount = computed(() => formSections.value.reduce((s, x) => s + x.clauses.length, 0))

function openEdit(p) {
  editOpen.value = true
  showPreview.value = true
  if (p) {
    Object.assign(form, {
      id: p.id, code: p.code, title: p.title, version: p.version, category: p.category,
      summary: p.summary || '', content: p.content || '', required: p.required ?? 0, status: p.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null, code: '', title: '', version: '1.0', category: 'SERVICE',
      summary: '', content: POLICY_TEMPLATE, required: 0, status: 1
    })
  }
}
async function submit() {
  if (!form.code.trim()) return ElMessage.warning('请填写协议编码')
  if (!form.title.trim()) return ElMessage.warning('请填写协议标题')
  if (!form.version.trim()) return ElMessage.warning('请填写版本号')
  if (!form.content.trim()) return ElMessage.warning('请填写协议正文')
  const payload = {
    code: form.code.trim().toUpperCase(), title: form.title.trim(), version: form.version.trim(),
    category: form.category, summary: form.summary.trim(), content: form.content,
    required: form.required, status: form.status
  }
  saving.value = true
  try {
    if (form.id) {
      await adminUpdatePolicy(form.id, payload)
      ElMessage.success('协议已修订' + (form.required === 1 ? '，必签升版将通知已签署用户' : ''))
    } else {
      await adminCreatePolicy(payload)
      ElMessage.success('协议已创建')
    }
    editOpen.value = false
    previewOpen.value = false
    await refreshAll()
  } catch (e) { ElMessage.error(e?.message || '保存失败') } finally { saving.value = false }
}

async function refreshAll() { await Promise.all([loadStats(), loadAll(), loadRows(), loadAcceptances()]) }
async function reload() {
  loading.value = true
  try { await refreshAll(); ElMessage.success('协议数据已刷新') } finally { loading.value = false }
}

onMounted(refreshAll)
</script>

<style scoped>
.side-col { display: flex; flex-direction: column; gap: var(--ap-gap); min-width: 0; }
.cat-chip {
  display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;
  font-size: 11.5px; font-weight: 700; color: var(--nc);
  background: color-mix(in srgb, var(--nc) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--nc) 32%, transparent);
  padding: 3px 9px; border-radius: 999px;
}
.sign-num { font-size: 15px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.card-summary { margin: 0; font-size: 12px; line-height: 1.6; color: var(--art-gray-600); }
.ap-table tbody tr { cursor: pointer; }

.cat-bars { display: flex; flex-direction: column; gap: 13px; }
.cb-head { display: flex; justify-content: space-between; align-items: baseline; font-size: 12.5px; color: var(--art-gray-700); }
.cb-head b { font-size: 14px; font-weight: 800; color: var(--nc); }
.cb-track { height: 7px; border-radius: 4px; background: var(--art-gray-200); overflow: hidden; margin: 5px 0 4px; }
html.dark .cb-track { background: #26262e; }
.cb-track i { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, var(--nc), color-mix(in srgb, var(--nc) 50%, #ffffff)); transition: width 0.8s var(--flair-ease); }
.cb-foot { font-size: 11px; color: var(--art-gray-500); }
.heat-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 190px; }
.ap-list-avatar { font-size: 14px; font-weight: 800; }

.ver-chip { font-size: 11.5px; font-weight: 700; color: var(--ap-policy); background: rgba(99, 102, 241, 0.12); padding: 3px 9px; border-radius: 999px; }
.ver-ok, .ver-old { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 700; }
.ver-ok { color: #0d9488; }
.ver-old { color: #f59e0b; }
html.dark .ver-ok { color: #35e0c2; }

/* 预览 */
.pv-head { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 15px; }
.pv-summary { margin: 14px 0 0; padding: 12px 14px; border-radius: 11px; background: var(--art-gray-100); font-size: 12.5px; line-height: 1.7; color: var(--art-gray-700); }
html.dark .pv-summary { background: #1a1a20; }
.pv-body { margin-top: 16px; }
.pv-sec { margin-bottom: 17px; }
.pv-sec h3 {
  margin: 0 0 9px; font-size: 14.5px; font-weight: 800; color: var(--art-gray-900);
  padding-left: 11px; border-left: 3px solid var(--ap-policy);
}
.pv-sec p { margin: 0 0 7px; font-size: 12.8px; line-height: 1.85; color: var(--art-gray-700); }
.pv-sec p b { color: var(--ap-policy); font-weight: 800; margin-right: 7px; font-variant-numeric: tabular-nums; }
.pv-actions { display: flex; gap: 9px; flex-wrap: wrap; margin-top: 20px; padding-top: 16px; border-top: 1px dashed var(--default-border); }

/* 编辑器 */
.edit-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.switch-row { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; font-size: 12.5px; color: var(--art-gray-700); padding: 5px 0; }
.sr-gap { margin-left: 12px; }
.editor-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-bottom: 7px; }
.eh-tip { font-size: 11.5px; color: var(--art-gray-500); }
.eh-tip code { font-family: 'JetBrains Mono', Consolas, monospace; background: var(--art-gray-200); padding: 1px 5px; border-radius: 5px; color: var(--ap-policy); }
html.dark .eh-tip code { background: #26262e; }
.eh-actions { display: flex; gap: 2px; }
.editor-body { display: block; }
.editor-body.split { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; align-items: start; }
.editor-input :deep(.el-textarea__inner) { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12.5px; line-height: 1.75; }
.editor-preview {
  max-height: 358px; overflow-y: auto;
  padding: 13px 15px; border-radius: 11px;
  border: 1px solid var(--default-border); background: var(--art-gray-100);
}
html.dark .editor-preview { background: #1a1a20; }

@media (max-width: 768px) {
  .edit-grid, .editor-body.split { grid-template-columns: 1fr; }
  .editor-preview { max-height: 260px; }
}
</style>