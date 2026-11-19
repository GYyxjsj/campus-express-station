<template>
  <div class="ap">
    <PageHero tone="parcel" icon="Location" chip="ADDRESS BOOK" live title="收货地址簿"
      sub="维护宿舍、教学楼等常用收货地址，支持浏览器定位自动记录经纬度；默认地址将用于自寄下单与送件上门。"
      :stats="heroStats">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增地址</el-button>
        <el-button type="default" :icon="Aim" :loading="locating" @click="locate">获取当前定位</el-button>
      </template>
    </PageHero>

    <div v-if="located" class="ap-note" style="--nc:#0ea5e9">
      <el-icon><Position /></el-icon>
      <div><b>定位成功：</b>经度 {{ located.longitude.toFixed(6) }}，纬度 {{ located.latitude.toFixed(6) }}（精度约 {{ Math.round(located.accuracy) }} 米）。新增或编辑地址时可一键带入。</div>
    </div>

    <PanelCard title="地址列表" :note="`${rows.length} 条`" icon="MapLocation" accent="parcel" :index="1">
      <div v-loading="loading" class="addr-grid">
        <article v-for="(a, i) in rows" :key="a.id" class="addr-item flair-in" :class="{ def: a.isDefault === 1 }"
                 :style="{ '--i': Math.min(i, 10) }">
          <div class="ad-l1">
            <el-tag size="small" effect="plain">{{ a.label || '收货地址' }}</el-tag>
            <el-tag v-if="a.isDefault === 1" size="small" type="success" effect="light">默认</el-tag>
            <span class="ad-ops">
              <el-button size="small" text type="primary" :icon="EditPen" @click="openForm(a)">编辑</el-button>
              <el-button v-if="a.isDefault !== 1" size="small" text type="success" @click="doDefault(a)">设默认</el-button>
              <el-button size="small" text type="danger" :icon="Delete" @click="doDelete(a)">删除</el-button>
            </span>
          </div>
          <div class="ad-person">
            <b>{{ a.contact }}</b>
            <span class="ap-mono">{{ a.phone }}</span>
          </div>
          <p class="ad-detail">{{ a.region ? a.region + ' ' : '' }}{{ a.detail }}</p>
          <div class="ad-meta ap-mono">
            <span v-if="a.longitude != null && a.latitude != null">
              <el-icon :size="12"><Position /></el-icon>
              {{ Number(a.longitude).toFixed(5) }}, {{ Number(a.latitude).toFixed(5) }}
            </span>
            <span v-else>未记录坐标</span>
            <span>更新于 {{ fmtTime(a.updatedAt || a.createdAt) }}</span>
          </div>
        </article>
      </div>
      <div v-if="!rows.length && !loading" class="ap-empty">
        <el-empty description="还没有收货地址" :image-size="86" />
        <p>新增常用地址后，自寄与送件上门可一键带入，无需重复填写。</p>
      </div>
    </PanelCard>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑地址' : '新增地址'" width="520px" append-to-body>
      <div class="ap-form">
        <div class="ap-field-row">
          <label>地址标签</label>
          <el-input v-model="form.label" placeholder="如：宿舍 / 教学楼 / 家中" maxlength="32" />
        </div>
        <div class="ap-field-row">
          <label>联系人<em>*</em></label>
          <el-input v-model="form.contact" placeholder="收件人姓名" maxlength="32" />
        </div>
        <div class="ap-field-row">
          <label>手机号<em>*</em></label>
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" class="ap-mono" />
        </div>
        <div class="ap-field-row">
          <label>所在地区</label>
          <el-input v-model="form.region" placeholder="如：XX大学 东区" maxlength="120" />
        </div>
        <div class="ap-field-row">
          <label>详细地址<em>*</em></label>
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="楼栋 / 门牌 / 房间号" maxlength="200" show-word-limit />
        </div>
        <div class="ap-field-row">
          <label>坐标（可选）</label>
          <div class="coord-row">
            <el-input v-model="form.longitude" placeholder="经度" class="ap-mono" />
            <el-input v-model="form.latitude" placeholder="纬度" class="ap-mono" />
            <el-button :icon="Aim" :loading="locating" @click="locate(true)">定位带入</el-button>
          </div>
          <div class="ap-form-tip">使用浏览器 Geolocation 获取，需授权定位权限；坐标仅用于校内送件导航。</div>
        </div>
        <div class="ap-field-row">
          <el-checkbox v-model="form.isDefault">设为默认收货地址</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存地址</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Aim, EditPen, Delete, Position } from '@element-plus/icons-vue'
import PageHero from '../../components/PageHero.vue'
import PanelCard from '../../components/PanelCard.vue'
import { myAddresses, saveAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api'
import { fmtTime } from '../../utils/format'

const rows = ref([])
const loading = ref(false)
const locating = ref(false)
const located = ref(null)
const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, label: '', contact: '', phone: '', region: '', detail: '', longitude: '', latitude: '', isDefault: false })

const heroStats = computed(() => [
  { label: '地址总数', value: rows.value.length, unit: '条' },
  { label: '已记坐标', value: rows.value.filter(a => a.longitude != null).length, unit: '条' },
  { label: '默认地址', value: rows.value.filter(a => a.isDefault === 1).length, unit: '条' }
])

async function load() {
  loading.value = true
  try { rows.value = await myAddresses() } finally { loading.value = false }
}

function openForm(a) {
  Object.assign(form, a
    ? { id: a.id, label: a.label || '', contact: a.contact, phone: a.phone, region: a.region || '', detail: a.detail,
        longitude: a.longitude != null ? String(a.longitude) : '', latitude: a.latitude != null ? String(a.latitude) : '', isDefault: a.isDefault === 1 }
    : { id: null, label: '', contact: '', phone: '', region: '', detail: '', longitude: '', latitude: '', isDefault: rows.value.length === 0 })
  formVisible.value = true
}

function locate(fillForm = false) {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位，请手动填写坐标')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    pos => {
      locating.value = false
      located.value = { longitude: pos.coords.longitude, latitude: pos.coords.latitude, accuracy: pos.coords.accuracy || 0 }
      if (fillForm) {
        form.longitude = pos.coords.longitude.toFixed(6)
        form.latitude = pos.coords.latitude.toFixed(6)
      }
      ElMessage.success('定位成功')
    },
    err => {
      locating.value = false
      const msg = err.code === 1 ? '定位权限被拒绝，请在浏览器设置中允许本站定位' : '定位失败，请检查设备定位服务或手动填写坐标'
      ElMessage.warning(msg)
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
  )
}

async function doSave() {
  if (!form.contact.trim() || !/^1[3-9]\d{9}$/.test(form.phone.trim()) || !form.detail.trim()) {
    ElMessage.warning('请完整填写联系人、11 位手机号与详细地址')
    return
  }
  const payload = {
    label: form.label.trim(), contact: form.contact.trim(), phone: form.phone.trim(),
    region: form.region.trim(), detail: form.detail.trim(),
    longitude: form.longitude === '' ? null : Number(form.longitude),
    latitude: form.latitude === '' ? null : Number(form.latitude),
    isDefault: form.isDefault
  }
  if ((payload.longitude != null && !Number.isFinite(payload.longitude)) || (payload.latitude != null && !Number.isFinite(payload.latitude))) {
    ElMessage.warning('坐标需为数字，可点击「定位带入」自动获取')
    return
  }
  saving.value = true
  try {
    if (form.id) await updateAddress(form.id, payload)
    else await saveAddress(payload)
    ElMessage.success('地址已保存')
    formVisible.value = false
    await load()
  } finally { saving.value = false }
}

async function doDefault(a) {
  await setDefaultAddress(a.id)
  ElMessage.success(`已将「${a.label || a.detail}」设为默认地址`)
  await load()
}

async function doDelete(a) {
  try {
    await ElMessageBox.confirm(`确定删除地址「${a.label || a.detail}」吗？`, '删除地址', { type: 'warning' })
  } catch { return }
  await deleteAddress(a.id)
  ElMessage.success('地址已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.addr-grid { display: grid; gap: 10px; grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr)); }
.addr-item {
  border: 1px solid var(--default-border); border-radius: 14px; padding: 13px 15px;
  background: var(--art-card-bg, #fff);
  transition: transform 0.22s var(--flair-ease), box-shadow 0.22s, border-color 0.22s;
}
html.dark .addr-item { background: #17171c; }
.addr-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow-hover); }
.addr-item.def { border-color: rgba(16, 217, 181, 0.55); background: linear-gradient(130deg, rgba(16, 217, 181, 0.06), transparent 60%); }
html.dark .addr-item.def { background: linear-gradient(130deg, rgba(16, 217, 181, 0.1), #17171c 60%); }
.ad-l1 { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.ad-ops { margin-left: auto; display: inline-flex; gap: 0; }
.ad-ops .el-button { padding: 4px 6px; }
.ad-person { display: flex; align-items: baseline; gap: 10px; margin-top: 9px; }
.ad-person b { font-size: 14.5px; color: var(--art-gray-900); }
.ad-person span { font-size: 12px; color: var(--art-gray-600); }
.ad-detail { margin: 5px 0 8px; font-size: 12.5px; color: var(--art-gray-700); line-height: 1.65; word-break: break-all; }
.ad-meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 11px; color: var(--art-gray-500); }
.ad-meta span { display: inline-flex; align-items: center; gap: 4px; }
.coord-row { display: flex; gap: 8px; }
.coord-row .el-input { flex: 1; }
</style>