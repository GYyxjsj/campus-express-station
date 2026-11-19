import { ref } from 'vue'

/**
 * 表格表头点击排序（el-table sortable="custom" 配套）。
 * 默认按时间倒序；点击表头切换升/降序，取消排序回落默认时间倒序。
 * @param {string} defaultProp 默认排序字段（时间字段）
 * @param {object} getters 可选：prop -> (row) => 可比较值（拼接/换算列）
 */
export function useTableSort(defaultProp, getters = {}) {
  const sortState = ref({ prop: defaultProp, order: 'descending' })
  const defaultSort = { prop: defaultProp, order: 'descending' }

  function onSortChange({ prop, order }) {
    sortState.value = order ? { prop, order } : { prop: defaultProp, order: 'descending' }
  }

  function sorted(list) {
    const { prop, order } = sortState.value
    const get = getters[prop] || (r => r[prop])
    const dir = order === 'ascending' ? 1 : -1
    return [...(list || [])].sort((a, b) => {
      const va = get(a)
      const vb = get(b)
      if (va == null && vb == null) return 0
      if (va == null) return 1
      if (vb == null) return -1
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb), 'zh-CN') * dir
    })
  }

  return { sortState, defaultSort, onSortChange, sorted }
}

/**
 * 服务端分页表格的排序参数：返回 { sortKey, order } 直接并入查询参数。
 * 后端 Sorts 白名单解析，默认时间倒序。
 * toggle/thClass 供原生 table.ap-table 的 SortTh 表头组件使用。
 */
export function useServerSort(defaultProp) {
  const sortState = ref({ prop: defaultProp, order: 'descending' })
  const defaultSort = { prop: defaultProp, order: 'descending' }

  function onSortChange({ prop, order }) {
    sortState.value = order ? { prop, order } : { prop: defaultProp, order: 'descending' }
  }

  /** 原生表头点击循环：新列升序 → 降序 → 回落默认时间倒序 */
  function toggle(prop) {
    const s = sortState.value
    if (s.prop !== prop) sortState.value = { prop, order: 'ascending' }
    else if (s.order === 'ascending') sortState.value = { prop, order: 'descending' }
    else sortState.value = { prop: defaultProp, order: 'descending' }
  }

  function thClass(prop) {
    const s = sortState.value
    return {
      sortable: true,
      sorted: s.prop === prop,
      asc: s.prop === prop && s.order === 'ascending',
      desc: s.prop === prop && s.order === 'descending'
    }
  }

  function sortParams() {
    return { sortKey: sortState.value.prop, order: sortState.value.order }
  }

  return { sortState, defaultSort, onSortChange, toggle, thClass, sortParams }
}