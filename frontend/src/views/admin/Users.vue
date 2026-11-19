<template>
  <div class="ap">
    <PageHero tone="admin" icon="User" chip="USER ADMIN" live title="用户管理"
      sub="全校账号的开户、角色与状态治理：学号 / 工号唯一开户，角色决定权限面，停用 / 注销即刻阻断登录，注销保留审计可恢复，重置密码留痕。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
        <el-button type="default" :icon="Document" @click="$router.push('/admin/logs')">操作日志</el-button>
      </template>
    </PageHero>

    <section class="ap-stats">
      <MetricTile v-for="(m, i) in metrics" :key="m.label" v-bind="m" :index="i" />
    </section>

    <PanelCard title="账号台账" :note="`${total} 个账号`" icon="UserFilled" accent="admin" :index="4">
      <template #extra>
        <el-button size="small" text type="primary" :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>

      <div class="ap-toolbar">
        <div class="ap-field ap-grow">
          <label>关键词</label>
          <el-input v-model="query.keyword" :prefix-icon="Search" clearable placeholder="用户名 / 姓名 / 手机号"
                    @keyup.enter="load(1)" @clear="load(1)" />
        </div>
        <div class="ap-field status-field">
          <label>状态</label>
          <el-select v-model="query.status" clearable placeholder="全部" @change="load(1)">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
            <el-option label="已注销" :value="2" />
          </el-select>
        </div>
        <div class="ap-actions">
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="ap-seg role-seg">
        <button v-for="r in roleTabs" :key="r.key" :class="{ on: query.roleCode === r.key }" @click="query.roleCode = r.key; load(1)">
          {{ r.label }}<span class="n">{{ r.key ? (counts[r.key] || 0) : totalAll }}</span>
        </button>
      </div>

      <div v-loading="loading">
        <div class="ap-table-wrap">
          <el-table :data="rows" stripe :default-sort="defaultSort" @sort-change="e => { onSortChange(e); load(1) }">
            <el-table-column label="账号" prop="realName" sortable="custom" min-width="190">
              <template #default="{ row }">
                <div class="u-cell">
                  <span class="u-avatar" :style="{ background: avatarBg(row) }">{{ (row.realName || row.username || '?').slice(0, 1) }}</span>
                  <div>
                    <b>{{ row.realName }}</b>
                    <div class="ap-sub ap-mono">{{ row.username }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="联系方式" min-width="170">
              <template #default="{ row }">
                <span class="ap-mono">{{ row.phone }}</span>
                <div class="ap-sub ap-ellipsis">{{ row.email || '未填邮箱' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="160">
              <template #default="{ row }">
                <el-tag v-for="r in row.roles" :key="r" size="small" class="role-tag" effect="light"
                        :type="r === 'ADMIN' ? 'danger' : r === 'STAFF' ? 'warning' : 'primary'">
                  {{ roleLabel(r) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" sortable="custom" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small" effect="light">
                  {{ row.status === 1 ? '正常' : row.status === 2 ? '已注销' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" prop="createdAt" sortable="custom" width="150">
              <template #default="{ row }"><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <div class="ap-ops">
                  <el-button v-if="row.status === 2" link type="success" :icon="RefreshLeft" @click="doRestore(row)">恢复</el-button>
                  <template v-else>
                    <el-button link type="primary" :icon="Key" @click="openRoles(row)">角色</el-button>
                    <el-button link type="warning" :icon="Lock" @click="openReset(row)">重置密码</el-button>
                    <el-popconfirm :title="row.status === 1 ? '停用后该账号将无法登录，确定？' : '确定恢复该账号登录？'"
                                   width="230" @confirm="toggleStatus(row)">
                      <template #reference>
                        <el-button link :type="row.status === 1 ? 'danger' : 'success'" :icon="row.status === 1 ? CircleClose : CircleCheck">
                          {{ row.status === 1 ? '停用' : '启用' }}
                        </el-button>
                      </template>
                    </el-popconfirm>
                    <el-button link type="danger" :icon="Delete" @click="doCancel(row)">注销</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ap-cards">
          <article v-for="row in rows" :key="row.id" class="ap-card-item" :class="{ off: row.status !== 1 }">
            <div class="ap-card-top">
              <span class="u-avatar" :style="{ background: avatarBg(row) }">{{ (row.realName || row.username || '?').slice(0, 1) }}</span>
              <b>{{ row.realName }}</b>
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small" effect="light">
                {{ row.status === 1 ? '正常' : row.status === 2 ? '已注销' : '停用' }}
              </el-tag>
            </div>
            <div class="ap-card-grid">
              <div><label>用户名</label><span class="ap-mono">{{ row.username }}</span></div>
              <div><label>手机号</label><span class="ap-mono">{{ row.phone }}</span></div>
              <div><label>角色</label><span>{{ (row.roles || []).map(roleLabel).join(' / ') }}</span></div>
              <div><label>注册时间</label><span class="ap-mono">{{ fmtTime(row.createdAt) }}</span></div>
            </div>
            <div class="ap-card-actions">
              <el-button v-if="row.status === 2" size="small" type="success" plain :icon="RefreshLeft" @click="doRestore(row)">恢复账号</el-button>
              <template v-else>
                <el-button size="small" :icon="Key" @click="openRoles(row)">角色</el-button>
                <el-button size="small" type="warning" plain :icon="Lock" @click="openReset(row)">重置密码</el-button>
                <el-popconfirm :title="row.status === 1 ? '停用后该账号将无法登录，确定？' : '确定恢复该账号登录？'"
                               width="230" @confirm="toggleStatus(row)">
                  <template #reference>
                    <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain :icon="row.status === 1 ? CircleClose : CircleCheck">
                      {{ row.status === 1 ? '停用' : '启用' }}
                    </el-button>
                  </template>
                </el-popconfirm>
                <el-button size="small" type="danger" plain :icon="Delete" @click="doCancel(row)">注销</el-button>
              </template>
            </div>
          </article>
        </div>

        <div v-if="!rows.length && !loading" class="ap-empty">
          <el-empty description="没有匹配的账号" :image-size="86" />
          <p>调整关键词或角色筛选，或点击右上角「新建用户」开户。</p>
        </div>
      </div>

      <div class="ap-pager">
        <el-pagination background :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next, sizes'"
                       :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </PanelCard>

    <el-dialog v-model="createVisible" title="新建用户" width="500px" append-to-body>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-position="top" class="ap-form">
        <div class="form-duo">
          <el-form-item label="用户名 / 学号" prop="username">
            <el-input v-model="createForm.username" placeholder="4-20位字母或数字" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="createForm.realName" placeholder="真实姓名" />
          </el-form-item>
        </div>
        <div class="form-duo">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="createForm.phone" maxlength="11" placeholder="用于包裹匹配" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="createForm.email" placeholder="选填" />
          </el-form-item>
        </div>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" show-password placeholder="6-32位，建议告知本人后尽快修改" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-checkbox-group v-model="createForm.roles" class="role-checks">
            <el-checkbox value="STUDENT">学生</el-checkbox>
            <el-checkbox value="STAFF">驿站员工</el-checkbox>
            <el-checkbox value="ADMIN">管理员</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :icon="Plus" :loading="saving" @click="doCreate">创建账号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rolesVisible" :title="`分配角色 · ${current?.realName || ''}`" width="420px" append-to-body>
      <div class="ap-form">
        <div class="ap-note" style="--nc:#5b7cfa">
          <el-icon><InfoFilled /></el-icon>
          <span>角色决定权限面：学生可取件 / 授权 / 充值；员工可入库核销；管理员拥有全部治理权限。保存后下次登录生效。</span>
        </div>
        <el-checkbox-group v-model="rolesForm" class="role-checks big">
          <el-checkbox value="STUDENT">学生</el-checkbox>
          <el-checkbox value="STAFF">驿站员工</el-checkbox>
          <el-checkbox value="ADMIN">管理员</el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRoles">保存角色</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" :title="`重置密码 · ${current?.realName || ''}`" width="420px" append-to-body>
      <div class="ap-form">
        <div class="ap-note" style="--nc:#f59e0b">
          <el-icon><Warning /></el-icon>
          <span>重置后旧密码立即失效，请将新密码安全告知本人。操作会写入审计日志。</span>
        </div>
        <div class="ap-field-row">
          <label>新密码<em>*</em></label>
          <el-input v-model="newPassword" show-password placeholder="6-32位" />
        </div>
      </div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="warning" :icon="Lock" :loading="saving" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Delete, Document, InfoFilled, Key, Lock, Plus, Refresh, RefreshLeft, Search, User, UserFilled, Warning } from '@element-plus/icons-vue'
import { pageUsers, createUser, setUserStatus, resetUserPassword, assignUserRoles, cancelUser, restoreUser } from '../../api'
import { fmtTime } from '../../utils/format'
import { useServerSort } from '../../utils/tableSort'
import { useIsMobile } from '../../utils/useIsMobile'
import PageHero from '../../components/PageHero.vue'
import MetricTile from '../../components/MetricTile.vue'
import PanelCard from '../../components/PanelCard.vue'

const isMobile = useIsMobile()
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const totalAll = ref(0)
const counts = ref({})
const query = reactive({ page: 1, size: 10, keyword: '', roleCode: '', status: '' })
const { defaultSort, onSortChange, sortParams } = useServerSort('createdAt')
const createVisible = ref(false)
const rolesVisible = ref(false)
const resetVisible = ref(false)
const current = ref(null)
const rolesForm = ref([])
const newPassword = ref('')
const createRef = ref()
const createForm = reactive({ username: '', realName: '', phone: '', email: '', password: '', roles: ['STUDENT'] })
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{4,20}$/, message: '4-20位字母或数字', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '6-32位', trigger: 'blur' }
  ],
  roles: [{ required: true, type: 'array', min: 1, message: '至少选择一个角色', trigger: 'change' }]
}

const roleTabs = [
  { key: '', label: '全部' },
  { key: 'STUDENT', label: '学生' },
  { key: 'STAFF', label: '员工' },
  { key: 'ADMIN', label: '管理员' }
]
const roleLabel = r => ({ ADMIN: '管理员', STAFF: '员工', STUDENT: '学生' }[r] || r)
const AVATAR_BG = {
  ADMIN: 'linear-gradient(140deg,#e11d48,#fb7185)',
  STAFF: 'linear-gradient(140deg,#f59e0b,#fbbf24)',
  STUDENT: 'linear-gradient(140deg,#3b82f6,#38bdf8)'
}
const avatarBg = row => AVATAR_BG[(row.roles || [])[0]] || 'linear-gradient(140deg,#64748b,#94a3b8)'

const heroStats = computed(() => [
  { label: '账号总数', value: totalAll.value, unit: '个' },
  { label: '学生', value: counts.value.STUDENT || 0, unit: '人' },
  { label: '员工', value: counts.value.STAFF || 0, unit: '人' }
])
const metrics = computed(() => [
  { label: '学生账号', value: counts.value.STUDENT || 0, unit: '人', icon: 'User', color: '#3b82f6', foot: '取件 / 授权 / 钱包' },
  { label: '驿站员工', value: counts.value.STAFF || 0, unit: '人', icon: 'Avatar', color: '#f59e0b', foot: '入库 / 核销 / 台账' },
  { label: '管理员', value: counts.value.ADMIN || 0, unit: '人', icon: 'Key', color: '#e11d48', foot: '全部治理权限' },
  { label: '已注销', value: counts.value.CANCELLED || 0, unit: '个', icon: 'Delete', color: '#64748b', foot: '软删除 · 可恢复' }
])

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await pageUsers({
      page: query.page, size: query.size,
      keyword: query.keyword || undefined, roleCode: query.roleCode || undefined,
      status: (query.status === '' || query.status == null) ? undefined : query.status,
      ...sortParams()
    })
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
  loadCounts()
}

async function loadCounts() {
  const keys = ['STUDENT', 'STAFF', 'ADMIN']
  const res = await Promise.all([
    pageUsers({ page: 1, size: 1 }),
    ...keys.map(k => pageUsers({ page: 1, size: 1, roleCode: k })),
    pageUsers({ page: 1, size: 1, status: 2 })
  ])
  totalAll.value = res[0].total
  counts.value = Object.fromEntries(keys.map((k, i) => [k, res[i + 1].total]))
  counts.value.CANCELLED = res[4].total
}

function resetQuery() {
  query.keyword = ''
  query.roleCode = ''
  query.status = ''
  load(1)
}

function openCreate() {
  Object.assign(createForm, { username: '', realName: '', phone: '', email: '', password: '', roles: ['STUDENT'] })
  createVisible.value = true
}

async function doCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await createUser({ ...createForm, email: createForm.email || undefined })
    ElMessage.success('用户已创建')
    createVisible.value = false
    load(1)
  } finally { saving.value = false }
}

function openRoles(row) {
  current.value = row
  rolesForm.value = [...(row.roles || [])]
  rolesVisible.value = true
}

async function doRoles() {
  if (!rolesForm.value.length) return ElMessage.warning('至少选择一个角色')
  saving.value = true
  try {
    await assignUserRoles(current.value.id, rolesForm.value)
    ElMessage.success('角色已更新')
    rolesVisible.value = false
    load()
  } finally { saving.value = false }
}

function openReset(row) { current.value = row; newPassword.value = ''; resetVisible.value = true }

async function doReset() {
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    return ElMessage.warning('密码长度须为6-32位')
  }
  saving.value = true
  try {
    await resetUserPassword(current.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally { saving.value = false }
}

async function toggleStatus(row) {
  await setUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '账号已停用' : '账号已启用')
  load()
}

async function doCancel(row) {
  try {
    await ElMessageBox.confirm(
      `注销后「${row.realName || row.username}」将无法登录，名下身份条码同步吊销。若仍有在库包裹、进行中代取单或钱包余额，系统会拦截并提示原因。确定注销？`,
      '注销账号',
      { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }
  try {
    await cancelUser(row.id)
    ElMessage.success('账号已注销')
    load()
  } catch { /* 护栏原因已由拦截器提示 */ }
}

async function doRestore(row) {
  try {
    await restoreUser(row.id)
    ElMessage.success('账号已恢复，可正常登录')
    load()
  } catch { /* 拦截器已提示 */ }
}

onMounted(() => load())
</script>

<style scoped>
.role-seg { margin-bottom: 12px; }
.status-field { min-width: 132px; }
.u-cell { display: flex; align-items: center; gap: 10px; }
.u-avatar {
  width: 34px; height: 34px; border-radius: 10px; flex: none;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 800; font-size: 15px;
}
.role-tag { margin-right: 4px; }
.ap-card-item.off { opacity: 0.66; }
.form-duo { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.role-checks { display: flex; gap: 18px; flex-wrap: wrap; }
.role-checks.big :deep(.el-checkbox) {
  border: 1px solid var(--default-border); border-radius: 10px; padding: 10px 16px; margin-right: 0;
  transition: border-color 0.2s;
}
.role-checks.big :deep(.el-checkbox.is-checked) { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
@media (max-width: 560px) { .form-duo { grid-template-columns: 1fr; gap: 0; } }
</style>