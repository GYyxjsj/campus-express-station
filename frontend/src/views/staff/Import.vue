<template>
  <div class="ap">
    <PageHero tone="staff" icon="Files" chip="BATCH INBOUND" live title="批量导入包裹"
      sub="快递员批量交接时使用：粘贴 CSV 或选择文本文件，每行「运单号,快递公司,收件手机号[,备注]」，单次最多 200 行。逐行独立入库，失败行不影响其他行，成功行自动生成取件码并通知学生。"
      :stats="heroStats">
      <template #actions>
        <el-button type="default" :icon="Download" @click="$router.push('/staff/inbound')">单件入库</el-button>
        <el-button type="default" :icon="Box" @click="$router.push('/staff/parcels')">包裹管理</el-button>
      </template>
    </PageHero>

    <div class="import-grid">
      <PanelCard title="导入清单" note="CSV 文本 / .csv / .txt 文件" icon="EditPen" accent="staff" :index="0">
        <el-form class="ap-form" label-position="top" @submit.prevent>
          <div class="ap-field-row">
            <label>目标驿站<em>*</em></label>
            <el-select v-model="stationId" placeholder="选择包裹入库驿站" style="width:100%" @change="onStationChange">
              <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
          <div class="ap-field-row">
            <label>默认货架</label>
            <el-select v-model="shelfId" clearable placeholder="不指定则入库后待上架" style="width:100%" :loading="shelfLoading">
              <el-option v-for="sh in shelves" :key="sh.id" :label="`${sh.code}（容量 ${sh.capacity}）`" :value="sh.id" :disabled="sh.status === 0" />
            </el-select>
            <div class="ap-form-tip">行内不提供货架时统一使用此货架；两处都留空则包裹入库后待上架。</div>
          </div>
          <div class="ap-field-row">
            <label>CSV 内容<em>*</em></label>
            <el-input v-model="csvText" type="textarea" :rows="10" class="csv-area"
                      placeholder="SF1234567890,SF,13800138000,大件请轻放&#10;ZT9876543210,ZT,13900139000&#10;YT5555666677,YT,13700137000,备注可省略" />
            <div class="ap-form-tip">
              首行若含「运单」或 trackingNo 表头会自动跳过；快递公司支持代码（SF/ZT/YT/YD/ST/JT/JD/EMS/DB）或中文名。
            </div>
          </div>
          <div class="import-ops">
            <el-button :icon="Upload" @click="fileInput && fileInput.click()">读取文件</el-button>
            <el-button :icon="MagicStick" @click="fillSample">填入示例</el-button>
            <el-button text type="danger" :icon="Delete" :disabled="!csvText" @click="csvText = ''">清空</el-button>
            <input ref="fileInput" type="file" accept=".csv,.txt" hidden @change="onFile" />
          </div>
          <div class="ap-note" style="--nc:#f59e0b">
            <el-icon><Warning /></el-icon>
            <span>当前解析 {{ lineCount }} 行（不含表头），单次上限 200 行。手机号须为 11 位大陆号码，收件人须是已注册学生。</span>
          </div>
          <el-button type="primary" size="large" style="width:100%" :icon="Files" :loading="importing"
                     :disabled="!stationId || !lineCount" @click="doImport">
            批量入库（{{ lineCount }} 行）
          </el-button>
        </el-form>
      </PanelCard>

      <PanelCard title="导入结果" :note="result ? `成功 ${result.success} / 失败 ${result.failed}` : '等待导入'"
                 icon="DataAnalysis" accent="mint" :index="1">
        <template #extra>
          <el-button v-if="result" size="small" text type="primary" :icon="DocumentCopy" @click="copyResult">复制结果</el-button>
        </template>

        <template v-if="result">
          <div class="result-sum">
            <div class="rs-item ok"><b>{{ result.success }}</b><span>入库成功</span></div>
            <div class="rs-item bad"><b>{{ result.failed }}</b><span>失败行</span></div>
            <div class="rs-item"><b>{{ result.total }}</b><span>总行数</span></div>
          </div>
          <div class="ap-table-wrap">
            <el-table :data="result.rows" stripe max-height="480">
              <el-table-column prop="line" label="行号" width="66" align="center">
                <template #default="{ row }"><span class="ap-mono">{{ row.line }}</span></template>
              </el-table-column>
              <el-table-column label="运单号" prop="trackingNo" min-width="150">
                <template #default="{ row }"><span class="ap-mono">{{ row.trackingNo || '—' }}</span></template>
              </el-table-column>
              <el-table-column label="结果" width="88">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="light">
                    {{ row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="取件码 / 失败原因" min-width="180">
                <template #default="{ row }">
                  <span v-if="row.success" class="ap-mono pick-code">{{ row.pickupCode }}</span>
                  <span v-else class="fail-msg">{{ row.message }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="ap-note" style="--nc:#0d9488">
            <el-icon><CircleCheck /></el-icon>
            <span>成功包裹已自动向收件学生发送到站提醒；失败行修正后可单独重新导入，不会重复入库。</span>
          </div>
        </template>
        <div v-else class="ap-empty">
          <el-empty description="尚未执行导入" :image-size="86" />
          <p>左侧填写清单后点击「批量入库」，每行的取件码或失败原因会逐行展示在这里。</p>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, CircleCheck, DataAnalysis, Delete, DocumentCopy, Download, Files, MagicStick, Upload, Warning } from '@element-plus/icons-vue'
import { importParcels, listStations, listShelves } from '../../api'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'

const stations = ref([])
const shelves = ref([])
const stationId = ref(null)
const shelfId = ref(null)
const shelfLoading = ref(false)
const csvText = ref('')
const importing = ref(false)
const result = ref(null)
const fileInput = ref(null)

const lineCount = computed(() => {
  const lines = csvText.value.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  if (lines.length && /运单|trackingNo/i.test(lines[0])) return lines.length - 1
  return lines.length
})

const heroStats = computed(() => [
  { label: '待导入行', value: lineCount.value, unit: '行' },
  { label: '上次成功', value: result.value ? result.value.success : 0, unit: '行' },
  { label: '上次失败', value: result.value ? result.value.failed : 0, unit: '行' }
])

async function onStationChange() {
  shelfId.value = null
  shelves.value = []
  if (!stationId.value) return
  shelfLoading.value = true
  try { shelves.value = await listShelves(stationId.value) } finally { shelfLoading.value = false }
}

function onFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 512 * 1024) return ElMessage.warning('文件过大，请拆分后导入（单次最多 200 行）')
  const reader = new FileReader()
  reader.onload = () => {
    csvText.value = String(reader.result || '')
    ElMessage.success(`已读取 ${file.name}，解析 ${lineCount.value} 行`)
  }
  reader.onerror = () => ElMessage.error('文件读取失败，请改用粘贴方式')
  reader.readAsText(file, 'UTF-8')
}

function fillSample() {
  csvText.value = [
    '运单号,快递公司,收件手机号,备注',
    'SF2026111700001,SF,13800138001,示例行请替换',
    'ZT2026111700002,ZT,13800138002',
    'YT2026111700003,YT,13800138003,易碎品'
  ].join('\n')
  ElMessage.info('已填入示例，请将手机号替换为真实注册学生号码')
}

async function doImport() {
  if (!stationId.value) return ElMessage.warning('请选择目标驿站')
  if (!lineCount.value) return ElMessage.warning('请先粘贴或读取 CSV 内容')
  if (lineCount.value > 200) return ElMessage.warning(`当前 ${lineCount.value} 行，超过单次 200 行上限，请拆分导入`)
  importing.value = true
  try {
    const data = await importParcels({
      stationId: stationId.value,
      shelfId: shelfId.value || undefined,
      csv: csvText.value
    })
    result.value = data
    if (data.failed === 0) ElMessage.success(`全部 ${data.success} 行入库成功，取件通知已发送`)
    else ElMessage.warning(`成功 ${data.success} 行，失败 ${data.failed} 行，请查看逐行结果`)
  } finally { importing.value = false }
}

async function copyResult() {
  if (!result.value) return
  const text = result.value.rows.map(r =>
    `${r.line}\t${r.trackingNo || '-'}\t${r.success ? '成功' : '失败'}\t${r.success ? r.pickupCode : r.message}`
  ).join('\n')
  try {
    await navigator.clipboard.writeText(`行号\t运单号\t结果\t取件码/原因\n${text}`)
    ElMessage.success('逐行结果已复制到剪贴板')
  } catch { ElMessage.warning('当前环境不支持剪贴板，请手动选择复制') }
}

onMounted(async () => {
  stations.value = await listStations()
  if (stations.value.length === 1) {
    stationId.value = stations.value[0].id
    onStationChange()
  }
})
</script>

<style scoped>
.import-grid { display: grid; grid-template-columns: minmax(360px, 480px) minmax(0, 1fr); gap: var(--ap-gap); align-items: start; }
.csv-area :deep(textarea) { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12.5px; line-height: 1.7; }
.import-ops { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.ap-note { margin-bottom: 14px; }
.result-sum { display: flex; gap: 10px; margin-bottom: 14px; }
.rs-item {
  flex: 1; padding: 12px 14px; border-radius: 12px; border: 1px solid var(--default-border);
  background: var(--art-gray-100); display: flex; flex-direction: column; gap: 2px;
}
html.dark .rs-item { background: #1c1c22; }
.rs-item b { font-size: 22px; font-weight: 800; color: var(--art-gray-900); font-variant-numeric: tabular-nums; }
.rs-item span { font-size: 12px; color: var(--art-gray-500); }
.rs-item.ok b { color: #0d9488; }
.rs-item.bad b { color: #e11d48; }
.pick-code { font-weight: 700; letter-spacing: 2px; color: #0d9488; }
.fail-msg { font-size: 12px; color: #e11d48; }
@media (max-width: 1024px) { .import-grid { grid-template-columns: 1fr; } }
</style>