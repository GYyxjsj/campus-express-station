/**
 * 审计日志详情可读化 —— 多层嵌套转义 JSON → 人类可读文案
 * detail 形如 {"args":{"arg0":"{\"parcelId\":1,...}"},"result":"截断的json串"}
 * 看板时间线与日志管理页共用。
 */

const MODULE_LABELS = {
  auth: '认证授权', parcel: '包裹', reminder: '提醒', admin: '系统管理',
  proxy: '代取服务', wallet: '钱包', policy: '协议', identity: '身份条码'
}
export const moduleLabel = m => MODULE_LABELS[m] || m || '系统操作'

function tryParse(v) {
  if (typeof v !== 'string') return v
  const s = v.trim()
  if (!s.startsWith('{') && !s.startsWith('[')) return v
  try { return JSON.parse(s) } catch { return v }
}

/** 递归解包：字符串→对象→内层字符串，最多 depth 层 */
function deepParse(v, depth = 4) {
  let cur = tryParse(v)
  let n = 0
  while (typeof cur === 'string' && n < depth) {
    const next = tryParse(cur)
    if (next === cur) break
    cur = next
    n++
  }
  return cur
}

/** 从（可能被截断的）原始串里正则兜底提取字段 */
function regexPick(raw, key, pattern = '[A-Za-z0-9_\\-]+') {
  const m = String(raw).match(new RegExp(key + '[\\\\"]*\\s*:\\s*[\\\\"]*(' + pattern + ')'))
  return m ? m[1] : null
}

/** 关键字段（入参/结果/正则兜底共用） */
const KEY_FIELDS = ['trackingNo', 'authCode', 'pickupCode', 'granteeUsername', 'username', 'stationName', 'shelfCode']

/**
 * 把一条日志的 detail 转成可读摘要。
 * @param {object} l 日志记录 {detail, module, action}
 * @returns {string} 如「运单 SF1390001111222 · 代取人 20230102 · 授权码 XFQTDZ6E · 有效期 24 小时」
 */
export function prettyDetail(l) {
  const raw = String(l?.detail || '')
  const o = deepParse(raw)
  const flat = {}
  if (typeof o === 'string') {
    // 整串非法 JSON（审计截断）：若是纯文本直接返回，否则走下方正则兜底
    if (o && !o.trim().startsWith('{') && !o.trim().startsWith('[')) return o.trim()
  } else {
    // 解包 args（{arg0:"json串"} / 数组 / 直接对象）
    let args = o.args
    if (args && !Array.isArray(args) && typeof args === 'object') args = Object.values(args)
    if (Array.isArray(args)) args = args.map(a => deepParse(a))
    for (const a of [].concat(args || [])) {
      if (a && typeof a === 'object') Object.assign(flat, a)
    }
    // 顶层直接就是业务对象的情况
    if (!Object.keys(flat).length && !o.args && !o.result) Object.assign(flat, o)

    // result 侧补充关键字段（不覆盖入参）
    const res = deepParse(o.result)
    const resData = res && typeof res === 'object' ? deepParse(res.data) : null
    if (resData && typeof resData === 'object') {
      for (const k of KEY_FIELDS) if (!flat[k] && resData[k]) flat[k] = resData[k]
    }
  }
  // 审计详情可能被截断为非法 JSON：对缺失字段正则兜底（驿站/货架允许中文）
  const TEXT_FIELDS = ['stationName', 'shelfCode']
  for (const k of KEY_FIELDS) {
    if (!flat[k]) {
      const pat = TEXT_FIELDS.includes(k) ? '[^"\\\\,}]{1,30}' : '[A-Za-z0-9_\\-]+'
      const v = regexPick(raw, k, pat)
      if (v && v.trim()) flat[k] = v.trim()
    }
  }
  for (const k of ['parcelId', 'validHours', 'userId', 'noticeId']) {
    if (flat[k] == null) { const v = regexPick(raw, k, '\\d+'); if (v) flat[k] = Number(v) }
  }
  if (!flat.remark) { const v = regexPick(raw, 'remark', '[^"\\\\,}]{1,40}'); if (v && v.trim()) flat.remark = v.trim() }

  const parts = []
  if (flat.trackingNo) parts.push(`运单 ${flat.trackingNo}`)
  if (flat.stationName) parts.push(`驿站 ${flat.stationName}`)
  if (flat.shelfCode) parts.push(`货架 ${flat.shelfCode}`)
  if (flat.granteeUsername) parts.push(`代取人 ${flat.granteeUsername}`)
  if (flat.authCode) parts.push(`授权码 ${flat.authCode}`)
  if (flat.pickupCode) parts.push(`取件码 ${flat.pickupCode}`)
  if (flat.validHours) parts.push(`有效期 ${flat.validHours} 小时`)
  if (flat.username && flat.username !== flat.granteeUsername) parts.push(`账号 ${flat.username}`)
  if (flat.remark) parts.push(`备注 ${flat.remark}`)
  if (!parts.length && flat.parcelId) parts.push(`包裹 #${flat.parcelId}`)
  if (!parts.length && flat.noticeId) parts.push(`公告 #${flat.noticeId}`)
  if (!parts.length && flat.userId) parts.push(`用户 #${flat.userId}`)
  return parts.join(' · ') || moduleLabel(l?.module) + '操作'
}