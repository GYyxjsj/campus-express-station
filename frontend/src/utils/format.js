// 通用格式化与字典
export function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

export const PARCEL_STATUS = {
  IN_STOCK: { label: '在库待取', type: 'primary' },
  OVERDUE: { label: '滞留超期', type: 'danger' },
  PICKED_UP: { label: '已取件', type: 'success' },
  RETURNED: { label: '已退回', type: 'info' }
}

export const AUTH_STATUS = {
  ACTIVE: { label: '生效中', type: 'success' },
  USED: { label: '已使用', type: 'primary' },
  REVOKED: { label: '已撤销', type: 'info' },
  EXPIRED: { label: '已过期', type: 'warning' }
}

export const NOTIFY_TYPE = {
  ARRIVE: { label: '到站通知', type: 'primary' },
  REMIND: { label: '取件催领', type: 'warning' },
  OVERDUE: { label: '滞留告警', type: 'danger' },
  PROXY_GRANTED: { label: '代取授权', type: 'success' },
  PROXY_USED: { label: '代取完成', type: 'success' },
  NOTICE: { label: '系统通知', type: 'info' },
  WALLET_RECHARGE: { label: '钱包充值', type: 'warning' },
  PROXY_ORDER: { label: '代取服务', type: 'primary' },
  INSPECT: { label: '开箱查验', type: 'warning' },
  POLICY_UPDATE: { label: '协议更新', type: 'info' }
}

/* ---- 去向台账节点 ---- */
export const TRACE_NODE = {
  ARRIVE: { label: '到站入库', icon: 'Download', color: '#5b7cfa' },
  SHELVE: { label: '上架定位', icon: 'Grid', color: '#38bdf8' },
  INSPECT: { label: '开箱查验', icon: 'Search', color: '#ffb020' },
  TRANSFER: { label: '移架调拨', icon: 'Sort', color: '#8b5cf6' },
  REMIND: { label: '催领提醒', icon: 'Bell', color: '#f59e0b' },
  OVERDUE: { label: '滞留标记', icon: 'AlarmClock', color: '#fb7185' },
  AUTH_GRANTED: { label: '代取授权', icon: 'Promotion', color: '#10d9b5' },
  AUTH_REVOKED: { label: '授权撤销', icon: 'CircleClose', color: '#7987a1' },
  SERVICE_OPEN: { label: '发布代取', icon: 'ShoppingBag', color: '#0ea5e9' },
  SERVICE_ACCEPT: { label: '跑腿接单', icon: 'Bicycle', color: '#22d3ee' },
  SERVICE_PICKED: { label: '柜台取件', icon: 'Box', color: '#0d9488' },
  SERVICE_DELIVERED: { label: '送达交接', icon: 'Van', color: '#34d399' },
  SERVICE_DONE: { label: '完成结算', icon: 'Money', color: '#10d9b5' },
  SERVICE_CANCEL: { label: '服务取消', icon: 'RefreshLeft', color: '#7987a1' },
  PICKUP: { label: '取件出库', icon: 'Upload', color: '#10d9b5' },
  PICKUP_IDENTITY: { label: '条码核销', icon: 'Postcard', color: '#8b5cf6' },
  RETURN: { label: '退回登记', icon: 'Back', color: '#7987a1' },
  NOTE: { label: '备注留痕', icon: 'EditPen', color: '#6366f1' }
}

export const TRACE_MANUAL = [
  { value: 'INSPECT', label: '开箱查验' },
  { value: 'TRANSFER', label: '移架调拨' },
  { value: 'SHELVE', label: '重新上架' },
  { value: 'NOTE', label: '备注留痕' }
]

export const OPERATOR_ROLE = {
  COURIER: '快递员',
  STAFF: '驿站员工',
  ADMIN: '管理员',
  STUDENT: '学生',
  SYSTEM: '系统'
}

/* ---- 代取服务单 ---- */
export const ORDER_STATUS = {
  OPEN: { label: '待接单', type: 'warning', color: '#f59e0b' },
  ACCEPTED: { label: '已接单', type: 'primary', color: '#0ea5e9' },
  PICKED: { label: '已取件', type: 'primary', color: '#8b5cf6' },
  DELIVERED: { label: '已送达', type: 'success', color: '#10d9b5' },
  DONE: { label: '已完成', type: 'success', color: '#0d9488' },
  CANCELLED: { label: '已取消', type: 'info', color: '#7987a1' },
  EXPIRED: { label: '已超时', type: 'danger', color: '#fb7185' }
}

/* ---- 钱包 ---- */
export const WALLET_TX = {
  RECHARGE: { label: '充值入账', type: 'warning', sign: 1 },
  CONSUME: { label: '服务支出', type: 'primary', sign: -1 },
  REFUND: { label: '退款回账', type: 'success', sign: 1 },
  EARN: { label: '跑腿收入', type: 'success', sign: 1 },
  ADJUST: { label: '人工调整', type: 'info', sign: 0 }
}

export const RECHARGE_STATUS = {
  PENDING: { label: '待支付', type: 'warning' },
  PAID: { label: '已到账', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  EXPIRED: { label: '已过期', type: 'danger' }
}

export const PAY_CHANNEL = {
  CAMPUS_CARD: { label: '校园一卡通', icon: 'CreditCard', color: '#5b7cfa' },
  ALIPAY: { label: '支付宝', icon: 'Wallet', color: '#0ea5e9' },
  WECHAT: { label: '微信支付', icon: 'ChatDotRound', color: '#10d9b5' },
  BALANCE: { label: '余额支付', icon: 'Money', color: '#f59e0b' },
  SYSTEM: { label: '系统入账', icon: 'Cpu', color: '#8b5cf6' }
}

/* ---- 身份条码 ---- */
export const IDENTITY_STATUS = {
  ACTIVE: { label: '生效中', type: 'success' },
  REVOKED: { label: '已注销', type: 'info' },
  EXPIRED: { label: '已过期', type: 'warning' }
}

/* ---- 政策协议 ---- */
export const POLICY_CATEGORY = {
  SERVICE: { label: '服务条款', color: '#5b7cfa' },
  PRIVACY: { label: '隐私保护', color: '#8b5cf6' },
  PROXY: { label: '代取规则', color: '#0ea5e9' },
  WALLET: { label: '钱包规则', color: '#f59e0b' },
  CAMPUS: { label: '校园公约', color: '#10d9b5' }
}

export const NOTICE_CATEGORY = {
  NOTICE: { label: '通知', color: '#5b7cfa', icon: 'Bell' },
  OPERATION: { label: '运营', color: '#0ea5e9', icon: 'OfficeBuilding' },
  RULE: { label: '规章', color: '#8b5cf6', icon: 'Document' },
  FEATURE: { label: '新功能', color: '#10d9b5', icon: 'MagicStick' },
  ACTIVITY: { label: '活动', color: '#fb7185', icon: 'Present' }
}

export const NOTICE_PRIORITY = {
  0: { label: '普通', type: 'info', color: '#7987a1' },
  1: { label: '重要', type: 'warning', color: '#f59e0b' },
  2: { label: '紧急', type: 'danger', color: '#fb7185' }
}

/* ---- 自寄 / 保险 / 工单 / 反馈 / 卡券 / 广告 ---- */
export const SHIP_STATUS = {
  CREATED: { label: '待交件', type: 'warning' },
  DROPPED: { label: '已交件', type: 'primary' },
  SHIPPED: { label: '已寄出', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

export const INSURANCE_STATUS = {
  ACTIVE: { label: '保障中', type: 'success' },
  EXPIRED: { label: '已失效', type: 'info' },
  CLAIMED: { label: '理赔中', type: 'warning' },
  CLAIM_PAID: { label: '已理赔', type: 'primary' }
}

export const TICKET_STATUS = {
  PENDING: { label: '待处理', type: 'warning' },
  PROCESSING: { label: '处理中', type: 'primary' },
  DONE: { label: '已处理', type: 'success' }
}

export const TICKET_CATEGORY = {
  PARCEL: { label: '包裹问题', icon: 'Box', color: '#5b7cfa' },
  PROXY: { label: '代取服务', icon: 'Bicycle', color: '#0ea5e9' },
  WALLET: { label: '钱包账务', icon: 'Wallet', color: '#f59e0b' },
  SHIP: { label: '自寄寄件', icon: 'Van', color: '#8b5cf6' },
  OTHER: { label: '其他咨询', icon: 'ChatDotSquare', color: '#7987a1' }
}

export const FEEDBACK_TYPE = {
  SUGGEST: { label: '功能建议', type: 'primary' },
  BUG: { label: '问题报错', type: 'danger' },
  COMPLAINT: { label: '服务投诉', type: 'warning' },
  PRAISE: { label: '表扬点赞', type: 'success' }
}

export const FEEDBACK_STATUS = {
  PENDING: { label: '待回复', type: 'warning' },
  REPLIED: { label: '已回复', type: 'success' }
}

export const COUPON_STATUS = {
  UNUSED: { label: '待使用', type: 'success' },
  USED: { label: '已使用', type: 'info' },
  EXPIRED: { label: '已过期', type: 'warning' }
}

export const COUPON_TYPE = {
  COUPON: { label: '优惠券', color: '#5b7cfa' },
  RED_PACKET: { label: '红包', color: '#fb7185' }
}

export const COUPON_SCOPE = {
  ALL: '全场通用', SHIP: '自寄运费', PROXY: '代取服务', INSURANCE: '超时保险'
}

export const REDEEM_STATUS = {
  UNUSED: { label: '未兑换', type: 'success' },
  USED: { label: '已兑换', type: 'info' },
  DISABLED: { label: '已停用', type: 'warning' }
}

export const COUPON_SOURCE = { CLAIM: '活动领取', REDEEM: '兑换码兑换', SYSTEM: '系统发放' }

export const AD_SLOT = {
  HOME_BANNER: '首页横幅', PICKUP_SIDE: '取件页侧栏', WALLET_FOOT: '钱包页底部'
}

export const AD_THEME = {
  neutral: { label: '中性蓝', color: '#5b7cfa' },
  warm: { label: '暖橙', color: '#f59e0b' },
  cool: { label: '青碧', color: '#0d9488' }
}

export const PREF_TIME_SLOT = { ANY: '不限时段', MORNING: '上午 08:00-12:00', AFTERNOON: '下午 12:00-18:00', EVENING: '晚间 18:00-21:00' }
export const PREF_METHOD = { SELF: '本人自取', PROXY: '委托代取', DOOR: '送件上门' }
export const PREF_CHANNEL = { IN_APP: '站内提醒', SMS: '短信通知', EMAIL: '邮件通知', ALL: '全部渠道' }

/* 新业务提醒类型并入通知字典 */
Object.assign(NOTIFY_TYPE, {
  FRIEND: { label: '好友动态', type: 'primary' },
  INVITE: { label: '邀请有礼', type: 'success' },
  INVITE_REBATE: { label: '邀请返利', type: 'success' },
  SHIP: { label: '自寄动态', type: 'primary' },
  INSURANCE: { label: '超时保险', type: 'warning' },
  TICKET: { label: '客服工单', type: 'info' },
  COUPON: { label: '卡券到账', type: 'success' },
  FEEDBACK: { label: '反馈回复', type: 'info' }
})

export const COURIERS = ['顺丰速运', '中通快递', '圆通速递', '韵达快递', '申通快递', '极兔速递', '京东物流', '邮政EMS', '德邦快递']

export const COURIER_NAMES = {
  SF: '顺丰速运', ZT: '中通快递', YT: '圆通速递', YD: '韵达快递',
  ST: '申通快递', STO: '申通快递', JT: '极兔速递', JD: '京东物流',
  EMS: '邮政EMS', DB: '德邦快递'
}

export function courierName(code) {
  return COURIER_NAMES[code] || code || '未知承运商'
}

export function dict(map, key) {
  return map[key] || { label: key || '—', type: 'info' }
}

/* 金额：保留两位，千分位 */
export function money(v, decimals = 2) {
  const n = Number(v)
  const s = Number.isFinite(n)
    ? n.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
    : '0.00'
  return `${s} 元`
}

/* 金额（带单位）：12.00 元 —— money() 已统一带单位，此别名保持兼容 */
export function moneyY(v, decimals = 2) {
  return money(v, decimals)
}

/* 时间统一格式：yyyy-MM-dd HH:mm（兼容 ISO / 带秒 / 时间戳） */
export function fmtTimeFull(v) {
  return fmtTime(v)
}

/* 相对时间：3 分钟前 / 2 小时前 / 昨天 */
export function fromNow(v) {
  if (!v) return '—'
  const t = new Date(String(v).replace(' ', 'T')).getTime()
  if (!Number.isFinite(t)) return fmtTime(v)
  const diff = Date.now() - t
  if (diff < 0) return fmtTime(v)
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d} 天前`
  return fmtTime(v)
}

/* 倒计时：剩余 x 小时 y 分 */
export function countdown(v) {
  if (!v) return '—'
  const t = new Date(String(v).replace(' ', 'T')).getTime()
  if (!Number.isFinite(t)) return '—'
  const diff = t - Date.now()
  if (diff <= 0) return '已到期'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (h >= 24) return `${Math.floor(h / 24)} 天 ${h % 24} 小时`
  if (h > 0) return `${h} 小时 ${m} 分`
  return `${m} 分钟`
}