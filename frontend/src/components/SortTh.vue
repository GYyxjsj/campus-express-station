<template>
  <th :class="sort.thClass(prop)" :style="width ? { width } : null" @click.stop="onClick">
    {{ label }}
    <span class="sort-caret"><i class="up" /><i class="down" /></span>
  </th>
</template>

<script setup>
/**
 * 原生 table.ap-table 的可排序表头。
 * sort 传入 useServerSort(defaultProp) 的返回值；点击循环 升序→降序→默认时间倒序。
 * 排序变化后 emit('sort')，父组件在其中重置页码并重新加载。
 */
const props = defineProps({
  prop: { type: String, required: true },
  label: { type: String, required: true },
  width: { type: String, default: '' },
  sort: { type: Object, required: true }
})
const emit = defineEmits(['sort'])

function onClick() {
  props.sort.toggle(props.prop)
  emit('sort')
}
</script>