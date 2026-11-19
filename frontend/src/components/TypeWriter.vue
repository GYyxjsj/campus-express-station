<template>
  <span class="tw">{{ shown }}<i v-if="caret" class="typewriter-caret" /></span>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  // 字符串或字符串数组（数组则循环打字-删除）
  texts: { type: [String, Array], default: '' },
  speed: { type: Number, default: 85 },      // 打字间隔 ms
  eraseSpeed: { type: Number, default: 38 }, // 删除间隔 ms
  hold: { type: Number, default: 2200 },     // 打完后停留 ms
  loop: { type: Boolean, default: false },
  caret: { type: Boolean, default: true },
  startDelay: { type: Number, default: 0 }
})

const shown = ref('')
let timer = null
let cancelled = false
const list = () => (Array.isArray(props.texts) ? props.texts : [props.texts]).filter(Boolean)

function schedule(fn, ms) { timer = setTimeout(fn, ms) }

function typeText(str, i, done) {
  if (cancelled) return
  shown.value = str.slice(0, i)
  if (i <= str.length) schedule(() => typeText(str, i + 1, done), props.speed + Math.random() * 40)
  else done()
}
function eraseText(done) {
  if (cancelled) return
  const cur = shown.value
  if (cur.length > 0) { shown.value = cur.slice(0, -1); schedule(() => eraseText(done), props.eraseSpeed) }
  else done()
}
function run(idx) {
  const arr = list()
  if (!arr.length || cancelled) return
  const str = arr[idx % arr.length]
  typeText(str, 0, () => {
    if (props.loop && arr.length > 0) schedule(() => eraseText(() => run(idx + 1)), props.hold)
  })
}
function start() {
  cancelled = false
  schedule(() => run(0), props.startDelay)
}
onMounted(start)
watch(() => props.texts, () => {
  clearTimeout(timer); shown.value = ''; start()
})
onBeforeUnmount(() => { cancelled = true; clearTimeout(timer) })
</script>

<style scoped>
.tw { white-space: pre-wrap; }
</style>