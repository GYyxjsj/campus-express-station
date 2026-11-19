<template>
  <div v-if="nodes && nodes.length" class="ap-trace">
    <div v-for="(n, i) in nodes" :key="n.id || i" class="ap-trace-item flair-in"
         :style="{ '--tc': meta(n.node).color, '--i': Math.min(i, 12) }">
      <span class="ap-trace-dot"><el-icon><component :is="meta(n.node).icon" /></el-icon></span>
      <div class="ap-trace-head">
        <span class="ap-trace-title">{{ n.title || meta(n.node).label }}</span>
        <el-tag size="small" effect="plain" round>{{ meta(n.node).label }}</el-tag>
        <span class="ap-trace-time">{{ fmtTime(n.happenedAt) }}</span>
      </div>
      <div class="ap-trace-meta">
        <i><el-icon><LocationInformation /></el-icon>{{ n.location || '地点未登记' }}</i>
        <i><el-icon><UserFilled /></el-icon>{{ n.operatorName || '系统' }}
          <em v-if="n.operatorRole">· {{ OPERATOR_ROLE[n.operatorRole] || n.operatorRole }}</em>
        </i>
        <i v-if="showParcel && n.trackingNo"><el-icon><Box /></el-icon><span class="ap-mono">{{ n.trackingNo }}</span></i>
      </div>
      <div v-if="n.detail" class="ap-trace-detail">{{ n.detail }}</div>
    </div>
  </div>
  <div v-else class="ap-empty">
    <el-empty description="暂无去向记录" :image-size="70" />
    <p>包裹入库后，每一次上架、查验、移架与出库都会在此留痕。</p>
  </div>
</template>

<script setup>
import { TRACE_NODE, OPERATOR_ROLE, fmtTime } from '../utils/format'

defineProps({
  nodes: { type: Array, default: () => [] },
  showParcel: { type: Boolean, default: false }
})

function meta(node) {
  return TRACE_NODE[node] || { label: node || '记录', icon: 'InfoFilled', color: '#7987a1' }
}
</script>

<style scoped>
.ap-trace-meta em { font-style: normal; color: var(--art-gray-500); }
</style>