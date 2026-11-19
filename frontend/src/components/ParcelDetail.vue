<template>
  <el-drawer v-model="visible" title="包裹详情" size="460px" append-to-body>
    <div v-loading="loading" class="pd">
      <template v-if="data">
        <div class="pd-head">
          <div>
            <b class="ap-mono">{{ data.trackingNo }}</b>
            <div class="pd-sub">{{ courierName(data.courierCompany) }} · {{ data.stationName }}</div>
          </div>
          <el-tag :type="dict(PARCEL_STATUS, data.status).type" effect="dark">{{ dict(PARCEL_STATUS, data.status).label }}</el-tag>
        </div>

        <div v-if="data.pickupCode && ['IN_STOCK', 'OVERDUE'].includes(data.status)" class="pd-code">
          <label>取件码</label>
          <b class="ap-mono">{{ data.pickupCode }}</b>
          <span>柜台出示此码或身份条码即可取件</span>
        </div>

        <div class="ap-kv">
          <div><label>物主</label><span>{{ data.ownerName }}（{{ data.ownerUsername }}）</span></div>
          <div><label>手机号</label><span class="ap-mono">{{ data.ownerPhone }}</span></div>
          <div><label>货架</label><span class="ap-mono">{{ data.shelfCode || '未上架' }}</span></div>
          <div><label>入库时间</label><span class="ap-mono">{{ fmtTime(data.inboundTime) }}</span></div>
          <div><label>出库时间</label><span class="ap-mono">{{ fmtTime(data.outboundTime) }}</span></div>
          <div v-if="data.returnReason"><label>退回原因</label><span>{{ data.returnReason }}</span></div>
          <div v-if="data.remark"><label>备注</label><span>{{ data.remark }}</span></div>
          <div><label>代取授权</label>
            <span v-if="data.activeAuthId">
              <el-tag type="success" size="small" effect="plain">生效中</el-tag>
              被授权人 {{ data.activeAuthGrantee }}
            </span>
            <span v-else class="muted">无</span>
          </div>
        </div>

        <h4 class="pd-h">去向轨迹（{{ traces.length }}）</h4>
        <TraceTimeline :nodes="traces" />
      </template>
      <el-empty v-else-if="!loading" description="未找到包裹" :image-size="80" />
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { parcelDetail, parcelTraces } from '../api'
import { fmtTime, dict, courierName, PARCEL_STATUS } from '../utils/format'
import TraceTimeline from './TraceTimeline.vue'

const props = defineProps({ modelValue: Boolean, parcelId: [Number, String] })
const emit = defineEmits(['update:modelValue'])
const visible = ref(false)
const loading = ref(false)
const data = ref(null)
const traces = ref([])

watch(() => props.modelValue, async v => {
  visible.value = v
  if (v && props.parcelId) {
    loading.value = true
    data.value = null
    traces.value = []
    try {
      data.value = await parcelDetail(props.parcelId)
      traces.value = await parcelTraces(props.parcelId)
    } finally { loading.value = false }
  }
})
watch(visible, v => emit('update:modelValue', v))
</script>

<style scoped>
.pd { display: flex; flex-direction: column; gap: 14px; min-height: 200px; }
.pd-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.pd-head b { font-size: 16px; color: var(--art-gray-900); letter-spacing: 0.5px; }
.pd-sub { margin-top: 3px; font-size: 12px; color: var(--art-gray-500); }
.pd-code {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  border: 1.5px dashed rgba(245, 158, 11, 0.6); border-radius: 14px; padding: 12px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(251, 191, 36, 0.03));
}
.pd-code label { font-size: 11px; color: var(--art-gray-500); letter-spacing: 2px; }
.pd-code b { font-size: 30px; font-weight: 800; letter-spacing: 7px; color: #b45309; }
html.dark .pd-code b { color: #fbbf24; }
.pd-code span { font-size: 11px; color: var(--art-gray-500); }
.pd-h { font-size: 13px; font-weight: 700; color: var(--art-gray-900); margin-top: 2px; }
</style>