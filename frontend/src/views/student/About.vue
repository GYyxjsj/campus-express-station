<template>
  <div class="ap">
    <PageHero tone="admin" icon="OfficeBuilding" chip="ABOUT US" live title="关于我们"
      sub="校园快递驿站运营信息与系统版本；如页面显示异常，可尝试清理本地缓存后刷新。" />

    <div class="ap-row wide-left">
      <PanelCard title="运营信息" note="以公示信息为准" icon="Postcard" accent="slate" :index="1">
        <div v-loading="loading" class="ap-kv">
          <div><span>运营主体</span><b>{{ info.company || '—' }}</b></div>
          <div><span>负责人</span><b>{{ info.owner || '—' }}</b></div>
          <div><span>服务热线</span><b class="ap-mono">{{ info.phone || '—' }}</b></div>
          <div><span>联系邮箱</span><b class="ap-mono">{{ info.email || '—' }}</b></div>
          <div><span>办公地址</span><b>{{ info.address || '—' }}</b></div>
          <div><span>备案号</span><b class="ap-mono">{{ info.icp || '—' }}</b></div>
          <div><span>系统版本</span><b class="ap-mono">{{ info.version || '—' }}</b></div>
        </div>
        <div class="ap-note" style="--nc:#5b7cfa">
          <el-icon><InfoFilled /></el-icon>
          <div>取件纠纷、赔付争议请优先通过「联系客服」提交工单留痕；热线仅作补充沟通渠道。</div>
        </div>
      </PanelCard>

      <PanelCard title="本地缓存" note="不影响账号数据" icon="DeleteFilled" accent="amber" :index="2">
        <p class="cache-desc">
          清理浏览器本地缓存可解决页面样式错乱、旧数据残留等问题。
          <b>登录状态与账号数据不受影响</b>，服务端数据不会被删除。
        </p>
        <ul class="cache-list">
          <li>保留：登录令牌、账号信息</li>
          <li>清理：其余本地存储项与浏览器 Cache Storage</li>
          <li>清理后建议刷新页面以加载最新资源</li>
        </ul>
        <el-button type="warning" plain :icon="Brush" :loading="clearing" @click="clearCache">清理本地缓存</el-button>
        <div v-if="cleared" class="ap-note" style="--nc:#10d9b5">
          <el-icon><CircleCheckFilled /></el-icon>
          <div>已清理 {{ cleared }} 项本地缓存，刷新页面后生效。</div>
        </div>
      </PanelCard>
    </div>

    <PanelCard title="服务承诺" icon="Medal" accent="mint" :index="3">
      <div class="about-grid">
        <div class="ab-item"><el-icon :size="20"><Box /></el-icon><b>规范保管</b><p>分区上架、易碎重点看护，入库即推送取件提醒。</p></div>
        <div class="ab-item"><el-icon :size="20"><Lock /></el-icon><b>隐私保护</b><p>手机号脱敏展示，取件凭码核验，授权代取全程留痕。</p></div>
        <div class="ab-item"><el-icon :size="20"><Timer /></el-icon><b>超时保障</b><p>滞留自动催领，可购超时保险，责任超时按保额赔付。</p></div>
        <div class="ab-item"><el-icon :size="20"><Service /></el-icon><b>响应及时</b><p>工单与反馈专人跟进，处理进展站内实时通知。</p></div>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Brush, CircleCheckFilled, Box, Lock, Timer, Service } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { aboutInfo } from '../../api'

const info = ref({})
const loading = ref(false)
const clearing = ref(false)
const cleared = ref(0)

async function clearCache() {
  try {
    await ElMessageBox.confirm('清理本地缓存（保留登录状态）？清理后请刷新页面。', '清理缓存', { type: 'warning', confirmButtonText: '清理' })
  } catch { return }
  clearing.value = true
  try {
    let count = 0
    const keep = new Set(['ce_token', 'ce_user'])
    for (const key of Object.keys(localStorage)) {
      if (!keep.has(key)) { localStorage.removeItem(key); count++ }
    }
    if (window.caches) {
      const keys = await caches.keys()
      for (const k of keys) { await caches.delete(k); count++ }
    }
    cleared.value = count
    ElMessage.success(`已清理 ${count} 项本地缓存，建议刷新页面`)
  } finally { clearing.value = false }
}

onMounted(async () => {
  loading.value = true
  try { info.value = await aboutInfo() } finally { loading.value = false }
})
</script>

<style scoped>
.cache-desc { margin: 0 0 10px; font-size: 12.5px; line-height: 1.75; color: var(--art-gray-700); }
.cache-desc b { color: var(--art-gray-900); }
.cache-list { margin: 0 0 14px; padding-left: 18px; display: flex; flex-direction: column; gap: 6px; }
.cache-list li { font-size: 12px; color: var(--art-gray-600); line-height: 1.6; }
.about-grid { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(min(100%, 210px), 1fr)); }
.ab-item {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 15px;
  display: flex; flex-direction: column; gap: 7px;
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s;
}
.ab-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.ab-item .el-icon { color: var(--el-color-primary); }
.ab-item b { font-size: 13.5px; color: var(--art-gray-900); }
.ab-item p { margin: 0; font-size: 12px; line-height: 1.7; color: var(--art-gray-600); }
</style>