const B = 'http://127.0.0.1:8080/campus-express/api';
const out = [];
function log(...a) { const s = a.map(x => typeof x === 'string' ? x : JSON.stringify(x)).join(' '); out.push(s); console.log(s); }

async function login(username) {
  const r = await fetch(B + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password: '123456' }) });
  const j = await r.json();
  if (j.code !== 0) throw new Error('login failed ' + username + ' ' + JSON.stringify(j));
  return j.data.token;
}
async function call(token, method, path, body) {
  const r = await fetch(B + path, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const j = await r.json();
  return j;
}
const ok = (j, label) => { if (j.code !== 0) { log('FAIL', label, j); throw new Error(label + ': ' + j.msg); } log('OK  ', label); return j.data; };

const A = await login('20230101');   // 陈嘉睿 id3
const R = await login('20230102');   // 林晚晴 id4 (runner)
const S = await login('staff01');    // 周立群 id2
const AD = await login('admin');

// 1. 充值
const before = ok(await call(A, 'GET', '/wallet'), 'wallet overview').account.balance;
const order = ok(await call(A, 'POST', '/wallet/recharge', { amount: 50, channel: 'ALIPAY' }), 'create recharge order');
log('     orderNo', order.orderNo, 'bonus', order.bonus);
const paid = ok(await call(A, 'POST', `/wallet/recharge/${order.id}/pay`), 'pay recharge');
log('     balance', before, '->', paid.account.balance);
const dup = await call(A, 'POST', `/wallet/recharge/${order.id}/pay`);
log(dup.code === 81006 ? 'OK   double-pay rejected' : 'FAIL double-pay ' + JSON.stringify(dup));

// 2. 身份条码
const idc = ok(await call(A, 'POST', '/identity/generate'), 'generate identity code');
log('     code', idc.code, 'expire', idc.expireAt);
const ver = ok(await call(S, 'POST', '/identity/verify', { code: idc.code }), 'staff verify identity');
log('     owner', ver.user.realName, 'parcels', ver.parcels.length, 'pickupCode hidden:', ver.parcels.every(p => !p.pickupCode));

// 3. 代取服务全流程（每次运行新入库测试包裹，脚本可重复执行）
const stamp = Date.now().toString().slice(-8);
const inbound = async (suffix) => ok(await call(S, 'POST', '/parcels/inbound', {
  trackingNo: 'TEST' + stamp + suffix, courierCompany: 'SF', ownerPhone: '15868231905',
  stationId: 1, shelfId: 3, remark: '自动化验证包裹'
}), 'inbound test parcel ' + suffix);
const p1 = await inbound('M');
const p2 = await inbound('A');
const p3 = await inbound('B');
const target = p1;
const rules = ok(await call(A, 'GET', '/proxy-orders/rules'), 'proxy rules');
log('     rules', rules);
const created = ok(await call(A, 'POST', '/proxy-orders', { parcelId: target.id, fee: 5, tip: 1, deliverTo: '东区学生公寓3号楼 512 室', note: '门口即可', hours: 12 }), 'create proxy order');
log('     orderNo', created.orderNo, 'total', created.total, 'commission', created.commission);
const balAfterEscrow = ok(await call(A, 'GET', '/wallet'), 'wallet after escrow').account.balance;
log('     escrow balance', balAfterEscrow);
const hall = ok(await call(R, 'GET', '/proxy-orders/hall?page=1&size=50'), 'runner hall');
log('     hall contains order:', hall.records.some(o => o.id === created.id), 'secrets hidden:', hall.records.every(o => !o.pickupCode && !o.authCode));
const accepted = ok(await call(R, 'POST', `/proxy-orders/${created.id}/accept`), 'runner accept');
log('     authCode', accepted.authCode, 'runner', accepted.runnerName);
const selfAccept = await call(A, 'POST', `/proxy-orders/${created.id}/accept`);
log(selfAccept.code !== 0 ? 'OK   re-accept rejected (' + selfAccept.msg + ')' : 'FAIL re-accept allowed');
// 柜台核销联动
const detailA = ok(await call(A, 'GET', `/parcels/${target.id}`), 'parcel detail for pickup code');
const pp = ok(await call(S, 'POST', '/parcels/proxy-pickup', { authCode: accepted.authCode, pickupCode: detailA.pickupCode }), 'staff proxy pickup');
log('     pickup result', pp);
const afterPickup = ok(await call(R, 'GET', `/proxy-orders/${created.id}`), 'order after counter pickup');
log('     status', afterPickup.status, '(expect PICKED)');
const delivered = ok(await call(R, 'POST', `/proxy-orders/${created.id}/delivered`), 'runner delivered');
log('     status', delivered.status);
const runnerBefore = ok(await call(R, 'GET', '/wallet'), 'runner wallet before settle').account.balance;
const done = ok(await call(A, 'POST', `/proxy-orders/${created.id}/confirm`), 'requester confirm');
const runnerAfter = ok(await call(R, 'GET', '/wallet'), 'runner wallet after settle').account.balance;
log('     status', done.status, 'runner income', runnerBefore, '->', runnerAfter);
ok(await call(A, 'POST', `/proxy-orders/${created.id}/rate`, { rating: 5, review: '很快，包装完好' }), 'rate order');
const rated = ok(await call(A, 'GET', `/proxy-orders/${created.id}`), 'order after rate');
log('     rating', rated.rating, rated.review);

// 4. 取消退款流程
const c2 = ok(await call(A, 'POST', '/proxy-orders', { parcelId: p2.id, fee: 3, tip: 0, deliverTo: '西区食堂北侧', hours: 6 }), 'create second order');
const b2 = ok(await call(A, 'GET', '/wallet'), 'balance before cancel').account.balance;
ok(await call(A, 'POST', `/proxy-orders/${c2.id}/cancel`, { reason: '我自己去取' }), 'cancel order');
const b3 = ok(await call(A, 'GET', '/wallet'), 'balance after cancel').account.balance;
log('     refund', b2, '->', b3);

// 5. 轨迹台账
const tr = ok(await call(A, 'GET', `/traces/parcel/${target.id}`), 'parcel trace (student)');
log('     nodes', tr.map(t => t.node).join(','));
log('     sample', tr[tr.length - 1]);
const insp = ok(await call(S, 'POST', '/traces', { parcelId: p2.id, node: 'INSPECT', location: '东区菜鸟驿站 · 货架 A-01-02', detail: '外包装完好，无破损' }), 'staff inspect登记');
log('     inspector', insp.operatorName, insp.operatorRole, insp.location);
const trPage = ok(await call(S, 'GET', '/traces?page=1&size=3'), 'trace ledger page');
log('     total', trPage.total);
const trOv = ok(await call(S, 'GET', '/traces/overview'), 'trace overview');
log('     stats', trOv.stats);
const forbidden = await call(R, 'GET', '/traces?page=1&size=3');
log(forbidden.code === 40300 ? 'OK   student blocked from ledger' : 'FAIL ledger perm ' + JSON.stringify(forbidden).slice(0, 120));

// 6. 身份条码免码出库
const ip = ok(await call(S, 'POST', '/identity/pickup', { identityCode: idc.code, parcelId: p3.id }), 'identity pickup');
log('     result', ip);
const wrongOwner = await call(S, 'POST', '/identity/pickup', { identityCode: idc.code, parcelId: target.id });
log(wrongOwner.code !== 0 ? 'OK   already-out parcel rejected (' + wrongOwner.msg + ')' : 'FAIL repeated identity pickup');

// 7. 协议
const pol = ok(await call(A, 'GET', '/policies'), 'policy list');
log('     policies', pol.map(p => `${p.code} v${p.version} accepted=${p.accepted}`).join(' | '));
const pd = ok(await call(A, 'GET', '/policies/SERVICE_TERMS'), 'policy detail');
log('     content length', pd.content.length);
ok(await call(A, 'POST', '/policies/accept', { policyIds: pol.map(p => p.id) }), 'accept policies (idempotent)');

// 8. 公告弹窗
const pop = ok(await call(A, 'GET', '/notices/popup'), 'popup notices');
log('     count', pop.count, pop.notices.map(n => n.title));
ok(await call(A, 'POST', `/notices/${pop.notices[0].id}/dismiss`), 'dismiss notice');
const pop2 = ok(await call(A, 'GET', '/notices/popup'), 'popup after dismiss');
log('     count after', pop2.count);

// 9. 管理端
const wov = ok(await call(AD, 'GET', '/admin/wallet/overview'), 'admin wallet overview');
log('     summary', wov.summary);
const acc = ok(await call(AD, 'GET', '/admin/wallet/accounts?page=1&size=3'), 'admin accounts');
log('     total', acc.total, acc.records[0]);
ok(await call(AD, 'POST', '/admin/wallet/adjust', { userId: 4, amount: 10, remark: '活动奖励' }), 'admin adjust +10');
const pov = ok(await call(AD, 'GET', '/admin/proxy-orders/overview'), 'admin proxy overview');
log('     stats', pov.stats);
log('     topRunners', pov.topRunners.slice(0, 3));
const apol = ok(await call(AD, 'GET', '/admin/policies?page=1&size=10'), 'admin policies');
log('     total', apol.total);
const asta = ok(await call(AD, 'GET', '/admin/policies/stats'), 'policy stats');
log('     stats', asta);
const cfg = ok(await call(AD, 'GET', '/admin/configs'), 'configs');
log('     config count', cfg.length);
const badCfg = await call(AD, 'PUT', '/admin/configs', { 'proxy.service.commission.percent': '90' });
log(badCfg.code === 40001 ? 'OK   config range guard' : 'FAIL config guard ' + JSON.stringify(badCfg));
ok(await call(AD, 'PUT', '/admin/configs', { 'proxy.service.commission.percent': '10' }), 'config update ok');

log('ALL DONE');