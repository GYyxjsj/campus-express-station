-- ============================================================
-- 08_scale_10x_biz.sql 数据扩容第二段：围绕新包裹的业务链路（幂等）
-- 依赖 07_scale_10x.sql 已执行
-- ============================================================
BEGIN;

-- ---------- 5. 去向轨迹：每个新包裹 ARRIVE/SHELVE，出库的加 PICKUP/RETURN，滞留加 REMIND/OVERDUE ----------
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'ARRIVE', '包裹到站', s.name, NULL, NULL, 'SYSTEM',
       '承运商 ' || p.courier_company || ' 送达，运单 ' || p.tracking_no, p.inbound_time, p.inbound_time
FROM parcel p JOIN station s ON s.id = p.station_id
WHERE p.id > 462
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'ARRIVE');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'SHELVE', '上架入库', f.code, st.id, st.real_name, 'STAFF',
       '取件码已生成并发送通知', p.inbound_time + interval '15 minutes', p.inbound_time + interval '15 minutes'
FROM parcel p
JOIN shelf f ON f.id = p.shelf_id
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.shelf_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'SHELVE');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'PICKUP', '本人取件出库', s.name, st.id, st.real_name, 'STAFF',
       '取件码核验通过', p.outbound_time, p.outbound_time
FROM parcel p JOIN station s ON s.id = p.station_id
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'PICKED_UP'
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'PICKUP');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'RETURN', '包裹退回', s.name, st.id, st.real_name, 'STAFF', p.return_reason, p.outbound_time, p.outbound_time
FROM parcel p JOIN station s ON s.id = p.station_id
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'RETURNED'
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'RETURN');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'REMIND', '催领提醒', s.name, NULL, NULL, 'SYSTEM', '在库超 24 小时，自动催领', p.inbound_time + interval '26 hours', p.inbound_time + interval '26 hours'
FROM parcel p JOIN station s ON s.id = p.station_id
WHERE p.id > 462 AND p.status IN ('IN_STOCK','OVERDUE') AND p.inbound_time < now() - interval '30 hours'
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'REMIND');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at, created_at)
SELECT p.id, 'OVERDUE', '升级滞留件', s.name, NULL, NULL, 'SYSTEM', '在库超 72 小时，按公约进入滞留管理', p.inbound_time + interval '74 hours', p.inbound_time + interval '74 hours'
FROM parcel p JOIN station s ON s.id = p.station_id
WHERE p.id > 462 AND p.status = 'OVERDUE'
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'OVERDUE');

-- ---------- 6. 取件流水：新 PICKED_UP 包裹（92% SELF / 8% PROXY 走授权链） ----------
-- 6a. 先给 8% 的新已取包裹建 USED 授权
INSERT INTO proxy_auth (parcel_id, grantor_id, grantee_id, auth_code, status, expire_at, remark, created_at, used_at)
SELECT p.id, p.owner_id, gr.id,
       upper(substr(md5('auth' || p.id), 1, 8)),
       'USED', p.outbound_time + interval '2 hours', '同学顺路代取',
       p.outbound_time - interval '3 hours', p.outbound_time
FROM parcel p
JOIN LATERAL (SELECT id FROM sys_user WHERE username LIKE '202%' AND status = 1 AND id <> p.owner_id
              ORDER BY id OFFSET (p.id % 400) LIMIT 1) gr ON true
WHERE p.id > 462 AND p.status = 'PICKED_UP' AND p.id % 12 = 0
  AND NOT EXISTS (SELECT 1 FROM proxy_auth a WHERE a.parcel_id = p.id);

-- 6b. SELF 取件流水
INSERT INTO pickup_record (parcel_id, pickup_type, owner_id, picker_id, auth_id, staff_id, station_id, picked_at)
SELECT p.id, 'SELF', p.owner_id, p.owner_id, NULL, st.id, p.station_id, p.outbound_time
FROM parcel p
JOIN LATERAL (SELECT id FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'PICKED_UP' AND p.id % 12 <> 0
  AND NOT EXISTS (SELECT 1 FROM pickup_record r WHERE r.parcel_id = p.id);

-- 6c. PROXY 取件流水（挂授权）
INSERT INTO pickup_record (parcel_id, pickup_type, owner_id, picker_id, auth_id, staff_id, station_id, picked_at)
SELECT p.id, 'PROXY', p.owner_id, a.grantee_id, a.id, st.id, p.station_id, p.outbound_time
FROM parcel p
JOIN proxy_auth a ON a.parcel_id = p.id AND a.status = 'USED'
JOIN LATERAL (SELECT id FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'PICKED_UP'
  AND NOT EXISTS (SELECT 1 FROM pickup_record r WHERE r.parcel_id = p.id);

-- ---------- 7. 生效中授权：给部分新在库包裹建 ACTIVE 授权（每包裹至多一条） ----------
INSERT INTO proxy_auth (parcel_id, grantor_id, grantee_id, auth_code, status, expire_at, remark, created_at)
SELECT p.id, p.owner_id, gr.id,
       upper(substr(md5('act' || p.id), 1, 8)),
       'ACTIVE', now() + interval '20 hours', '考试周托同学代取', now() - interval '4 hours'
FROM parcel p
JOIN LATERAL (SELECT id FROM sys_user WHERE username LIKE '202%' AND status = 1 AND id <> p.owner_id
              ORDER BY id OFFSET ((p.id + 7) % 400) LIMIT 1) gr ON true
WHERE p.id > 462 AND p.status IN ('IN_STOCK','OVERDUE') AND p.id % 5 = 0
  AND NOT EXISTS (SELECT 1 FROM proxy_auth a WHERE a.parcel_id = p.id);

-- ---------- 8. 通知：到站/催领/滞留/授权（对齐既有类型枚举） ----------
INSERT INTO notification (user_id, parcel_id, type, channel, title, content, is_read, sent_at, read_at)
SELECT p.owner_id, p.id, 'ARRIVE', 'IN_APP', '包裹到站通知',
       '您的 ' || p.courier_company || ' 包裹（' || p.tracking_no || '）已到站，取件码 ' || p.pickup_code || '，请及时领取。',
       CASE WHEN p.status = 'PICKED_UP' THEN 1 ELSE (p.id % 2)::smallint END,
       p.inbound_time + interval '16 minutes',
       CASE WHEN p.status = 'PICKED_UP' THEN p.outbound_time ELSE NULL END
FROM parcel p
WHERE p.id > 462
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.parcel_id = p.id AND n.type = 'ARRIVE');

INSERT INTO notification (user_id, parcel_id, type, channel, title, content, is_read, sent_at)
SELECT p.owner_id, p.id, 'REMIND', (ARRAY['IN_APP','SMS'])[1 + p.id % 2], '取件催领提醒',
       '您的包裹（' || p.tracking_no || '）已在库超过 24 小时，请尽快凭取件码 ' || p.pickup_code || ' 领取。',
       0, p.inbound_time + interval '26 hours'
FROM parcel p
WHERE p.id > 462 AND p.status IN ('IN_STOCK','OVERDUE') AND p.inbound_time < now() - interval '30 hours'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.parcel_id = p.id AND n.type = 'REMIND');

INSERT INTO notification (user_id, parcel_id, type, channel, title, content, is_read, sent_at)
SELECT p.owner_id, p.id, 'OVERDUE', 'IN_APP', '包裹滞留告警',
       '您的包裹（' || p.tracking_no || '）在库已超 72 小时进入滞留管理，超期将按公约计收保管费或退回。',
       0, p.inbound_time + interval '74 hours'
FROM parcel p
WHERE p.id > 462 AND p.status = 'OVERDUE'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.parcel_id = p.id AND n.type = 'OVERDUE');

INSERT INTO notification (user_id, parcel_id, type, channel, title, content, is_read, sent_at)
SELECT a.grantee_id, a.parcel_id, 'PROXY_GRANTED', 'IN_APP', '收到代取授权',
       '同学向您授权代取包裹，授权码 ' || a.auth_code || '，有效期至 ' || to_char(a.expire_at, 'MM-DD HH24:MI') || '。',
       CASE WHEN a.status = 'USED' THEN 1 ELSE 0 END, a.created_at
FROM proxy_auth a
WHERE a.parcel_id > 462
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.parcel_id = a.parcel_id AND n.type = 'PROXY_GRANTED');

COMMIT;