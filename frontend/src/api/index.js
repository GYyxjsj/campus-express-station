import http from './http'

// ---- 认证 ----
export const login = data => http.post('/api/auth/login', data)
export const register = data => http.post('/api/auth/register', data)
export const getMe = () => http.get('/api/auth/me')
export const changePassword = data => http.put('/api/auth/password', data)

// ---- 包裹 ----
export const inboundParcel = data => http.post('/api/parcels/inbound', data)
export const pageParcels = params => http.get('/api/parcels', { params })
export const myParcels = params => http.get('/api/parcels/my', { params })
export const overdueParcels = params => http.get('/api/parcels/overdue', { params })
export const parcelDetail = id => http.get(`/api/parcels/${id}`)
export const pickup = data => http.post('/api/parcels/pickup', data)
export const proxyPickup = data => http.post('/api/parcels/proxy-pickup', data)
export const returnParcel = (id, reason) => http.post(`/api/parcels/${id}/return`, { reason })

// ---- 提醒 ----
export const myNotifications = params => http.get('/api/notifications/my', { params })
export const unreadCount = () => http.get('/api/notifications/unread-count')
export const readNotification = id => http.put(`/api/notifications/${id}/read`)
export const readAllNotifications = () => http.put('/api/notifications/read-all')
export const resendNotification = parcelId => http.post('/api/notifications/resend', { parcelId })

// ---- 代取授权 ----
export const createProxyAuth = data => http.post('/api/proxy-auth', data)
export const grantedAuths = params => http.get('/api/proxy-auth/granted', { params })
export const receivedAuths = params => http.get('/api/proxy-auth/received', { params })
export const revokeAuth = id => http.put(`/api/proxy-auth/${id}/revoke`)
export const authDetail = id => http.get(`/api/proxy-auth/${id}`)

// ---- 管理 ----
export const pageUsers = params => http.get('/api/admin/users', { params })
export const searchUsers = keyword => http.get('/api/admin/users/search', { params: { keyword } })
export const createUser = data => http.post('/api/admin/users', data)
export const setUserStatus = (id, status) => http.put(`/api/admin/users/${id}/status`, { status })
export const resetUserPassword = (id, newPassword) => http.put(`/api/admin/users/${id}/password`, { newPassword })
export const assignUserRoles = (id, roles) => http.put(`/api/admin/users/${id}/roles`, { roles })
export const cancelUser = id => http.delete(`/api/admin/users/${id}`)
export const restoreUser = id => http.put(`/api/admin/users/${id}/restore`)

export const listStations = () => http.get('/api/admin/stations')
export const createStation = data => http.post('/api/admin/stations', data)
export const updateStation = (id, data) => http.put(`/api/admin/stations/${id}`, data)
export const listShelves = stationId => http.get('/api/admin/shelves', { params: { stationId } })
export const createShelf = data => http.post('/api/admin/shelves', data)
export const setShelfStatus = (id, status) => http.put(`/api/admin/shelves/${id}/status`, { status })

export const listConfigs = () => http.get('/api/admin/configs')
export const updateConfigs = data => http.put('/api/admin/configs', data)

export const pageNotices = params => http.get('/api/admin/notices', { params })
export const createNotice = data => http.post('/api/admin/notices', data)
export const updateNotice = (id, data) => http.put(`/api/admin/notices/${id}`, data)

export const pageLogs = params => http.get('/api/admin/logs', { params })

// ---- 统计 ----
export const statsOverview = () => http.get('/api/stats/overview')
export const statsTrend = days => http.get('/api/stats/trend', { params: { days } })
export const statsDistribution = () => http.get('/api/stats/distribution')
export const statsHeatmap = weeks => http.get('/api/stats/heatmap', { params: { weeks } })

// ---- 好友私聊 ----
export const chatConversations = () => http.get('/api/chat/conversations')
export const chatMessages = peerId => http.get(`/api/chat/messages/${peerId}`)
export const chatSend = data => http.post('/api/chat/send', data)
export const chatUnread = () => http.get('/api/chat/unread')

// ---- 钱包与充值 ----
export const walletOverview = () => http.get('/api/wallet')
export const walletTiers = () => http.get('/api/wallet/tiers')
export const walletTransactions = params => http.get('/api/wallet/transactions', { params })
export const myRechargeOrders = params => http.get('/api/wallet/recharge-orders', { params })
export const createRecharge = data => http.post('/api/wallet/recharge', data)
export const payRecharge = id => http.post(`/api/wallet/recharge/${id}/pay`)
export const cancelRecharge = id => http.post(`/api/wallet/recharge/${id}/cancel`)

export const adminWalletOverview = () => http.get('/api/admin/wallet/overview')
export const adminWalletAccounts = params => http.get('/api/admin/wallet/accounts', { params })
export const adminWalletTransactions = params => http.get('/api/admin/wallet/transactions', { params })
export const adminRechargeOrders = params => http.get('/api/admin/wallet/recharge-orders', { params })
export const adminWalletAdjust = data => http.post('/api/admin/wallet/adjust', data)
export const adminWalletStatus = (userId, status) => http.post(`/api/admin/wallet/accounts/${userId}/status`, null, { params: { status } })

// ---- 身份条码 ----
export const myIdentity = () => http.get('/api/identity')
export const generateIdentity = () => http.post('/api/identity/generate')
export const revokeIdentity = id => http.post(`/api/identity/${id}/revoke`)
export const verifyIdentity = code => http.post('/api/identity/verify', { code })
export const identityPickup = data => http.post('/api/identity/pickup', data)

// ---- 代取服务单 ----
export const proxyRules = () => http.get('/api/proxy-orders/rules')
export const proxyHall = params => http.get('/api/proxy-orders/hall', { params })
export const myProxyRequests = params => http.get('/api/proxy-orders/my-requests', { params })
export const myProxyRuns = params => http.get('/api/proxy-orders/my-runs', { params })
export const proxyOrderDetail = id => http.get(`/api/proxy-orders/${id}`)
export const createProxyOrder = data => http.post('/api/proxy-orders', data)
export const acceptProxyOrder = id => http.post(`/api/proxy-orders/${id}/accept`)
export const pickedProxyOrder = id => http.post(`/api/proxy-orders/${id}/picked`)
export const deliveredProxyOrder = id => http.post(`/api/proxy-orders/${id}/delivered`)
export const confirmProxyOrder = id => http.post(`/api/proxy-orders/${id}/confirm`)
export const cancelProxyOrder = (id, reason) => http.post(`/api/proxy-orders/${id}/cancel`, { reason })
export const rateProxyOrder = (id, data) => http.post(`/api/proxy-orders/${id}/rate`, data)

export const adminProxyOverview = () => http.get('/api/admin/proxy-orders/overview')
export const adminProxyOrders = params => http.get('/api/admin/proxy-orders', { params })
export const adminCancelProxyOrder = (id, reason) => http.post(`/api/admin/proxy-orders/${id}/cancel`, { reason })
export const adminConfirmProxyOrder = id => http.post(`/api/admin/proxy-orders/${id}/confirm`)

// ---- 去向台账 ----
export const parcelTraces = parcelId => http.get(`/api/traces/parcel/${parcelId}`)
export const tracePage = params => http.get('/api/traces', { params })
export const traceOverview = () => http.get('/api/traces/overview')
export const addTrace = data => http.post('/api/traces', data)

// ---- 政策协议 ----
export const policyList = params => http.get('/api/policies', { params })
export const pendingPolicies = () => http.get('/api/policies/pending')
export const myPolicies = () => http.get('/api/policies/my')
export const policyDetail = code => http.get(`/api/policies/${code}`)
export const acceptPolicies = policyIds => http.post('/api/policies/accept', { policyIds })

export const adminPolicyStats = () => http.get('/api/admin/policies/stats')
export const adminPolicyPage = params => http.get('/api/admin/policies', { params })
export const adminPolicyAcceptances = params => http.get('/api/admin/policies/acceptances', { params })
export const adminCreatePolicy = data => http.post('/api/admin/policies', data)
export const adminUpdatePolicy = (id, data) => http.put(`/api/admin/policies/${id}`, data)
export const adminPolicyStatus = (id, status) => http.post(`/api/admin/policies/${id}/status`, null, { params: { status } })

// ---- 公告弹窗 ----
export const popupNotices = () => http.get('/api/notices/popup')
export const noticeDetail = id => http.get(`/api/notices/${id}`)
export const dismissNotice = id => http.post(`/api/notices/${id}/dismiss`)
export const dismissAllNotices = () => http.post('/api/notices/dismiss-all')
// ---- AI 助手 ----
export const aiChat = data => http.post('/api/ai/chat', data, { timeout: 60000 })
export const aiStatus = () => http.get('/api/ai/status')

// ---- 好友 ----
export const myFriends = () => http.get('/api/friends')
export const friendRequests = () => http.get('/api/friends/requests')
export const sendFriendRequest = data => http.post('/api/friends/request', data)
export const respondFriendRequest = (id, accept) => http.post(`/api/friends/request/${id}/respond`, { accept })
export const removeFriend = id => http.delete(`/api/friends/${id}`)

// ---- 邀请有礼 ----
export const inviteInfo = () => http.get('/api/invite/info')
export const inviteRecords = params => http.get('/api/invite/records', { params })
export const bindInviteCode = code => http.post('/api/invite/bind', { code })

// ---- 卡券 / 兑换码 ----
export const myCoupons = params => http.get('/api/coupons/my', { params })
export const redeemCode = code => http.post('/api/coupons/redeem', { code })
export const adminCoupons = params => http.get('/api/admin/coupons', { params })
export const adminCreateCouponBatch = data => http.post('/api/admin/coupons/batch', data)
export const adminRedeemCodes = params => http.get('/api/admin/coupons/codes', { params })

// ---- 地址簿 / 收货偏好 / 电话簿 ----
export const myAddresses = () => http.get('/api/profile/addresses')
export const saveAddress = data => http.post('/api/profile/addresses', data)
export const updateAddress = (id, data) => http.put(`/api/profile/addresses/${id}`, data)
export const deleteAddress = id => http.delete(`/api/profile/addresses/${id}`)
export const setDefaultAddress = id => http.post(`/api/profile/addresses/${id}/default`)
export const myPreference = () => http.get('/api/profile/preference')
export const savePreference = data => http.put('/api/profile/preference', data)
export const myContacts = () => http.get('/api/profile/contacts')
export const saveContact = data => http.post('/api/profile/contacts', data)
export const deleteContact = id => http.delete(`/api/profile/contacts/${id}`)

// ---- 自寄 / 保险 / 运费 ----
export const createShipOrder = data => http.post('/api/ship-orders', data)
export const myShipOrders = params => http.get('/api/ship-orders', { params })
export const shipOrderDetail = id => http.get(`/api/ship-orders/${id}`)
export const dropShipOrder = id => http.post(`/api/ship-orders/${id}/drop`)
export const cancelShipOrder = id => http.post(`/api/ship-orders/${id}/cancel`)
export const adminShipOrders = params => http.get('/api/manage/ship-orders', { params })
export const adminShipDeliver = (id, trackingNo) => http.post(`/api/manage/ship-orders/${id}/ship`, { trackingNo })
export const adminShipCancel = id => http.post(`/api/manage/ship-orders/${id}/cancel`)
export const insurancePlan = () => http.get('/api/insurance/plan')
export const buyInsurance = parcelId => http.post('/api/insurance/buy', { parcelId })
export const myInsurance = params => http.get('/api/insurance/my', { params })
export const freightQuote = params => http.get('/api/freight/quote', { params })
export const importParcels = data => http.post('/api/parcels/import', data)

// ---- 客服工单 / 反馈 ----
export const createTicket = data => http.post('/api/tickets', data)
export const myTickets = params => http.get('/api/tickets/my', { params })
export const ticketDetail = id => http.get(`/api/tickets/${id}`)
export const closeTicket = id => http.post(`/api/tickets/${id}/close`)
export const handleTickets = params => http.get('/api/manage/tickets', { params })
export const ticketStats = () => http.get('/api/manage/tickets/stats')
export const ticketProcessing = id => http.post(`/api/manage/tickets/${id}/processing`)
export const ticketDone = (id, remark) => http.post(`/api/manage/tickets/${id}/done`, { remark })
export const replyTicket = (id, content) => http.post(`/api/tickets/${id}/reply`, { content })
export const submitFeedback = data => http.post('/api/feedback', data)
export const myFeedback = params => http.get('/api/feedback/my', { params })
export const adminFeedback = params => http.get('/api/admin/feedback', { params })
export const adminReplyFeedback = (id, reply) => http.post(`/api/admin/feedback/${id}/reply`, { reply })

// ---- 广告位 / 关于我们 / 系统设置 ----
export const activeAds = () => http.get('/api/ads/active')
export const adminAds = () => http.get('/api/admin/ads')
export const adminCreateAd = data => http.post('/api/admin/ads', data)
export const adminUpdateAd = (id, data) => http.put(`/api/admin/ads/${id}`, data)
export const adminDeleteAd = id => http.delete(`/api/admin/ads/${id}`)
export const aboutInfo = () => http.get('/api/about')
export const adminSettings = () => http.get('/api/admin/settings')
export const adminSaveSettings = data => http.put('/api/admin/settings', data)
