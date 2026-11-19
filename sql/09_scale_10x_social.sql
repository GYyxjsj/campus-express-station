-- ============================================================
-- 09_scale_10x_social.sql 数据扩容第三段：社区/钱包/服务链路（幂等）
-- 依赖 07/08 已执行；末尾统一重算钱包余额，保证 CHECK(balance>=0)
-- ============================================================
BEGIN;

-- ---------- 9. 充值订单 + 入账流水（新学生每人 2~3 单，含少量未支付/过期单） ----------
INSERT INTO recharge_order (order_no, user_id, amount, bonus, channel, status, created_at, expire_at, paid_at)
SELECT 'RC' || to_char(u.created_at, 'YYYYMMDD') || lpad(u.id::text, 5, '0') || g.n,
       u.id, amt.amount,
       CASE WHEN amt.amount >= 200 THEN 20 WHEN amt.amount >= 100 THEN 8 WHEN amt.amount >= 50 THEN 3 ELSE 0 END,
       (ARRAY['CAMPUS_CARD','ALIPAY','WECHAT'])[1 + abs(hashtext('rcch' || u.id || g.n)) % 3],
       CASE WHEN g.n = 3 AND abs(hashtext('rcst' || u.id)) % 2 = 0 THEN 'EXPIRED' ELSE 'PAID' END,
       u.created_at + ((g.n * 9) || ' days')::interval,
       u.created_at + ((g.n * 9) || ' days')::interval + interval '30 minutes',
       CASE WHEN g.n = 3 AND abs(hashtext('rcst' || u.id)) % 2 = 0 THEN NULL
            ELSE u.created_at + ((g.n * 9) || ' days')::interval + interval '2 minutes' END
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
CROSS JOIN generate_series(1, 3) g(n)
CROSS JOIN LATERAL (SELECT (ARRAY[20,50,100,200])[1 + abs(hashtext('rcamt' || u.id || g.n)) % 4]::numeric AS amount) amt
WHERE u.username LIKE '2024%'
  AND g.n <= 2 + abs(hashtext('rcn' || u.id)) % 2
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, created_at)
SELECT o.user_id, 'RECHARGE', o.amount + o.bonus, 0, o.channel,
       '账户充值' || CASE WHEN o.bonus > 0 THEN '（含赠送 ' || to_char(o.bonus, 'FM999990.00') || ' 元）' ELSE '' END,
       'RECHARGE_ORDER', o.id, o.paid_at
FROM recharge_order o
WHERE o.status = 'PAID'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'RECHARGE_ORDER' AND t.ref_id = o.id AND t.type = 'RECHARGE');

-- ---------- 10. 邀请返利（被邀请人已支付充值 → 邀请人 EARN 10%） ----------
INSERT INTO invite_rebate (inviter_id, invitee_id, recharge_order_id, recharge_amount, rate_percent, amount, created_at)
SELECT u.invited_by, o.user_id, o.id, o.amount, 10, round(o.amount * 0.10, 2), o.paid_at + interval '5 minutes'
FROM recharge_order o JOIN sys_user u ON u.id = o.user_id
WHERE o.status = 'PAID' AND u.invited_by IS NOT NULL
ON CONFLICT (recharge_order_id) DO NOTHING;

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT r.inviter_id, 'EARN', r.amount, 0, 'SYSTEM', '邀请充值返利', 'INVITE_REBATE', r.id,
       '好友 ' || u.real_name || ' 充值 ' || to_char(r.recharge_amount, 'FM999990.00') || ' 元，返利 ' || to_char(r.rate_percent, 'FM990') || '%',
       r.created_at
FROM invite_rebate r JOIN sys_user u ON u.id = r.invitee_id
WHERE NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'INVITE_REBATE' AND t.ref_id = r.id);

-- ---------- 11. 好友关系（新学生之间确定性配对，约 10 倍） ----------
INSERT INTO friendship (user_id, friend_id, status, remark, created_at, updated_at)
SELECT a.id, b.id,
       CASE WHEN abs(hashtext('fst2' || a.id || b.id)) % 4 = 0 THEN 'PENDING' ELSE 'ACCEPTED' END,
       CASE WHEN abs(hashtext('frm2' || a.id || b.id)) % 2 = 0 THEN '同班同学' ELSE NULL END,
       now() - ((abs(hashtext('fct2' || a.id || b.id)) % 60 + 1) || ' day')::interval,
       now() - ((abs(hashtext('fut2' || a.id || b.id)) % 10) || ' day')::interval
FROM sys_user a
JOIN sys_user_role ra ON ra.user_id = a.id AND ra.role_id = 3
JOIN sys_user b ON b.id > a.id AND b.id <= a.id + 8
JOIN sys_user_role rb ON rb.user_id = b.id AND rb.role_id = 3
WHERE a.username LIKE '2024%' AND b.username LIKE '2024%'
  AND abs(hashtext('fpair2' || a.id || b.id)) % 5 <> 0
ON CONFLICT (user_id, friend_id) DO NOTHING;

-- ---------- 12. 好友私聊（仅 ACCEPTED 好友对，每对 2 条往返） ----------
INSERT INTO chat_message (sender_id, receiver_id, content, read_at, created_at)
SELECT f.user_id, f.friend_id,
       (ARRAY['同学你好，明天中午顺路帮我代取一下快递可以吗？','你的包裹好像滞留了，记得去取哦','驿站今天入库高峰，建议错峰取件','收到啦，谢谢你帮忙代取！'])[1 + abs(hashtext('cm1' || f.id)) % 4],
       now() - ((abs(hashtext('cmr' || f.id)) % 20 + 2) || ' hour')::interval,
       now() - ((abs(hashtext('cmr' || f.id)) % 20 + 2) || ' hour')::interval
FROM friendship f
WHERE f.status = 'ACCEPTED' AND f.user_id > 49
  AND abs(hashtext('cmpick' || f.id)) % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM chat_message c WHERE c.sender_id = f.user_id AND c.receiver_id = f.friend_id);

INSERT INTO chat_message (sender_id, receiver_id, content, read_at, created_at)
SELECT f.friend_id, f.user_id,
       (ARRAY['好呀，取件码发我一下','没问题，下午就帮你取','了解，我晚上没课就去','不客气，举手之劳～'])[1 + abs(hashtext('cm2' || f.id)) % 4],
       CASE WHEN abs(hashtext('cm2r' || f.id)) % 2 = 0 THEN now() - interval '1 hour' ELSE NULL END,
       now() - ((abs(hashtext('cmr' || f.id)) % 20 + 1) || ' hour')::interval
FROM friendship f
WHERE f.status = 'ACCEPTED' AND f.user_id > 49
  AND abs(hashtext('cmpick' || f.id)) % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM chat_message c WHERE c.sender_id = f.friend_id AND c.receiver_id = f.user_id);

-- ---------- 13. 身份条码（新用户各一条 ACTIVE，部分历史 REVOKED） ----------
INSERT INTO identity_code (user_id, code, status, expire_at, use_count, last_used_at, created_at)
SELECT u.id, 'CE' || upper(substr(md5('idc' || u.id), 1, 10)), 'ACTIVE',
       now() + interval '30 days',
       abs(hashtext('iduc' || u.id)) % 6,
       CASE WHEN abs(hashtext('iduc' || u.id)) % 6 > 0 THEN now() - ((abs(hashtext('idlu' || u.id)) % 10 + 1) || ' day')::interval ELSE NULL END,
       u.created_at + interval '2 days'
FROM sys_user u
WHERE u.username LIKE '2024%'
  AND NOT EXISTS (SELECT 1 FROM identity_code i WHERE i.user_id = u.id AND i.status = 'ACTIVE');

INSERT INTO identity_code (user_id, code, status, expire_at, use_count, created_at, revoked_at)
SELECT u.id, 'CE' || upper(substr(md5('idcold' || u.id), 1, 10)), 'REVOKED',
       u.created_at + interval '30 days', 3,
       u.created_at + interval '1 day', u.created_at + interval '20 days'
FROM sys_user u
WHERE u.username LIKE '2024%' AND abs(hashtext('idrev' || u.id)) % 4 = 0
  AND NOT EXISTS (SELECT 1 FROM identity_code i WHERE i.user_id = u.id AND i.status = 'REVOKED');

-- ---------- 14. 地址簿 / 收货偏好 / 电话簿（新学生） ----------
INSERT INTO user_address (user_id, label, contact, phone, region, detail, longitude, latitude, is_default, created_at)
SELECT u.id, '宿舍', u.real_name, u.phone,
       (ARRAY['校本部·东区','校本部·西区','校本部·南区','校本部·北区'])[1 + abs(hashtext('areg' || u.id)) % 4],
       (abs(hashtext('dorm2' || u.id)) % 12 + 1) || '号学生公寓 ' || (abs(hashtext('room2' || u.id)) % 6 + 1) || '0' || (abs(hashtext('r22' || u.id)) % 4 + 1) || ' 室',
       120.123456 + (abs(hashtext('lng2' || u.id)) % 100) / 10000.0,
       30.256789 + (abs(hashtext('lat2' || u.id)) % 100) / 10000.0,
       1, u.created_at + interval '1 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE u.username LIKE '2024%'
  AND NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.id AND a.label = '宿舍');

INSERT INTO user_address (user_id, label, contact, phone, region, detail, longitude, latitude, is_default, created_at)
SELECT u.id, '教学楼', u.real_name, u.phone, '校本部·教学区',
       '第' || (abs(hashtext('tb2' || u.id)) % 5 + 1) || '教学楼 ' || (abs(hashtext('tbr2' || u.id)) % 4 + 1) || '楼自习区（课间自取）',
       NULL, NULL, 0, u.created_at + interval '2 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE u.username LIKE '2024%' AND abs(hashtext('addr22' || u.id)) % 2 = 0
  AND NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.id AND a.label = '教学楼');

INSERT INTO receive_preference (user_id, time_slot, deliver_method, notify_channel, quiet_hours, no_call, fragile_care, auto_proxy, note, updated_at)
SELECT u.id,
       (ARRAY['ANY','MORNING','AFTERNOON','EVENING'])[1 + abs(hashtext('slot2' || u.id)) % 4],
       (ARRAY['SELF','SELF','PROXY','DOOR'])[1 + abs(hashtext('mth2' || u.id)) % 4],
       (ARRAY['IN_APP','IN_APP','SMS','ALL'])[1 + abs(hashtext('chn2' || u.id)) % 4],
       '23:00-08:00',
       (abs(hashtext('nc2' || u.id)) % 4 = 0)::int, 1,
       (abs(hashtext('ap2' || u.id)) % 5 = 0)::int,
       CASE abs(hashtext('pn2' || u.id)) % 4
            WHEN 0 THEN '大件请提前短信告知，方便推车来取'
            WHEN 1 THEN '生鲜冷链件请电话叫醒，勿放货架常温区'
            WHEN 2 THEN '工作日白天在实验室，晚间集中取件'
            ELSE NULL END,
       now() - ((abs(hashtext('put2' || u.id)) % 20) || ' day')::interval
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE u.username LIKE '2024%'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO phone_contact (user_id, name, phone, relation, is_starred, remark, created_at)
SELECT fs.user_id, f.real_name, f.phone, '同学', 1, '常帮我代取', now() - interval '15 day'
FROM friendship fs JOIN sys_user f ON f.id = fs.friend_id
WHERE fs.status = 'ACCEPTED' AND fs.user_id > 49
  AND NOT EXISTS (SELECT 1 FROM phone_contact c WHERE c.user_id = fs.user_id AND c.phone = f.phone);

INSERT INTO phone_contact (user_id, name, phone, relation, is_starred, remark, created_at)
SELECT u.id,
       (ARRAY['陈辅导员','刘班主任','宿管阿姨'])[1 + abs(hashtext('rel2' || u.id)) % 3],
       '139' || lpad((abs(hashtext('tel2' || u.id)) % 100000000)::text, 8, '0'),
       (ARRAY['辅导员','辅导员','宿管'])[1 + abs(hashtext('rel2' || u.id)) % 3],
       0, NULL, now() - interval '25 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE u.username LIKE '2024%' AND abs(hashtext('pc32' || u.id)) % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM phone_contact c WHERE c.user_id = u.id AND c.relation IN ('辅导员','宿管'));

-- ---------- 15. 卡券：新增模板 + 新学生领券（每人每模板至多 1 张） ----------
INSERT INTO coupon_template (id, name, type, amount, min_spend, scope, total_qty, claimed_qty, valid_days, description) VALUES
 (6, '开学季自寄满减券', 'COUPON',     5.00, 12.00, 'SHIP',      800, 0, 30, '自寄运费满 12 元可用'),
 (7, '代取服务加油券',   'COUPON',     3.00,  3.00, 'PROXY',     800, 0, 20, '发布有偿代取时抵扣服务费'),
 (8, '保险体验周卡',     'COUPON',     1.00,  1.00, 'INSURANCE', 600, 0, 14, '兑换一次超时保险保费'),
 (9, '暖冬现金红包',     'RED_PACKET', 6.60,  0.00, 'ALL',       500, 0, 15, '无门槛红包，可抵扣任意驿站服务'),
 (10,'期末冲刺红包',     'RED_PACKET', 3.30,  0.00, 'ALL',       500, 0, 10, '考试周专属无门槛红包')
ON CONFLICT (id) DO NOTHING;
SELECT setval('coupon_template_id_seq', GREATEST((SELECT max(id) FROM coupon_template), 10), true);

INSERT INTO user_coupon (template_id, user_id, code, status, source, expire_at, used_at, created_at)
SELECT t.id, s.id, 'CP' || upper(substr(md5('uc24' || s.id || t.id), 1, 14)),
       CASE WHEN abs(hashtext('ucst24' || s.id || t.id)) % 5 = 0 THEN 'USED' ELSE 'UNUSED' END,
       CASE WHEN abs(hashtext('ucsrc24' || s.id || t.id)) % 4 = 0 THEN 'REDEEM' ELSE 'CLAIM' END,
       now() + (t.valid_days || ' day')::interval,
       CASE WHEN abs(hashtext('ucst24' || s.id || t.id)) % 5 = 0 THEN now() - interval '1 day' ELSE NULL END,
       now() - ((abs(hashtext('ucct24' || s.id || t.id)) % 10 + 1) || ' day')::interval
FROM sys_user s
JOIN sys_user_role ur ON ur.user_id = s.id AND ur.role_id = 3
JOIN coupon_template t ON t.id = abs(hashtext('uct24' || s.id)) % 10 + 1
WHERE s.username LIKE '2024%' AND abs(hashtext('ucown24' || s.id)) % 3 <> 0
ON CONFLICT (template_id, user_id) DO NOTHING;

UPDATE coupon_template t SET claimed_qty = (SELECT count(*) FROM user_coupon c WHERE c.template_id = t.id);

-- 兑换码批量生成（80 个，部分已用/停用）
INSERT INTO redeem_code (code, template_id, status, used_by, used_at, created_at)
SELECT 'RC24' || upper(substr(md5('rdc' || g), 1, 8)),
       abs(hashtext('rdct' || g)) % 10 + 1,
       CASE WHEN g % 7 = 0 THEN 'DISABLED' WHEN g % 3 = 0 THEN 'USED' ELSE 'UNUSED' END,
       CASE WHEN g % 3 = 0 AND g % 7 <> 0 THEN
            (SELECT id FROM sys_user WHERE username LIKE '2024%' ORDER BY id OFFSET (g % 400) LIMIT 1)
            ELSE NULL END,
       CASE WHEN g % 3 = 0 AND g % 7 <> 0 THEN now() - ((g % 20) || ' day')::interval ELSE NULL END,
       now() - interval '30 day'
FROM generate_series(1, 80) g
ON CONFLICT (code) DO NOTHING;

-- ---------- 16. 自寄订单（新学生约 250 单）+ 运费扣款流水 ----------
INSERT INTO ship_order (order_no, user_id, station_id, courier_company, sender_name, sender_phone,
                        receiver_name, receiver_phone, receiver_address, goods_type, weight_kg, freight,
                        insured, tracking_no, status, note, created_at, dropped_at, shipped_at)
SELECT 'SP24' || to_char(now() - (g.n * 7 || ' day')::interval, 'YYYYMMDD') || lpad(u.id::text, 4, '0') || g.n,
       u.id,
       (SELECT id FROM station WHERE status = 1 ORDER BY id OFFSET (abs(hashtext('spst24' || u.id || g.n)) % 6) LIMIT 1),
       (ARRAY['SF','ZT','YT','JD','STO'])[abs(hashtext('spcc24' || u.id || g.n)) % 5 + 1],
       u.real_name, u.phone,
       (ARRAY['王建国','李秀兰','张伟','陈丽华'])[abs(hashtext('sprn24' || u.id || g.n)) % 4 + 1],
       '138' || lpad((abs(hashtext('sprp24' || u.id || g.n)) % 100000000)::text, 8, '0'),
       (ARRAY['浙江省杭州市西湖区文三路 100 号','江苏省南京市鼓楼区中山北路 32 号','上海市杨浦区四平路 1239 号','安徽省合肥市蜀山区黄山路 443 号'])[abs(hashtext('spra24' || u.id || g.n)) % 4 + 1],
       (ARRAY['衣物','书籍','日用品','特产食品','数码配件'])[abs(hashtext('spg24' || u.id || g.n)) % 5 + 1],
       w.kg,
       8 + 2 * GREATEST(0, ceil(w.kg - 1)),
       (abs(hashtext('spin24' || u.id || g.n)) % 3 = 0)::int,
       CASE WHEN st.s = 'SHIPPED' THEN 'SF' || lpad((abs(hashtext('sptn24' || u.id || g.n)) % 1000000000000)::text, 12, '0') ELSE NULL END,
       st.s,
       CASE WHEN abs(hashtext('spnt24' || u.id || g.n)) % 3 = 0 THEN '易碎，请贴防压标签' ELSE NULL END,
       now() - (g.n * 7 || ' day')::interval,
       CASE WHEN st.s <> 'CREATED' THEN now() - (g.n * 7 || ' day')::interval + interval '3 hour' ELSE NULL END,
       CASE WHEN st.s = 'SHIPPED' THEN now() - (g.n * 7 || ' day')::interval + interval '6 hour' ELSE NULL END
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
CROSS JOIN generate_series(1, 2) AS g(n)
CROSS JOIN LATERAL (SELECT (0.5 + (abs(hashtext('spw24' || u.id || g.n)) % 10) * 0.5)::numeric AS kg) w
CROSS JOIN LATERAL (SELECT (ARRAY['CREATED','DROPPED','SHIPPED','SHIPPED'])[abs(hashtext('sps24' || u.id || g.n)) % 4 + 1] AS s) st
WHERE u.username LIKE '2024%' AND abs(hashtext('spown24' || u.id)) % 2 = 0
  AND g.n <= abs(hashtext('spn24' || u.id)) % 2 + 1
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT o.user_id, 'CONSUME', -o.freight, 0, 'BALANCE', '自寄运费', 'SHIP_ORDER', o.id,
       '订单 ' || o.order_no || ' · ' || to_char(o.weight_kg, 'FM990.0') || ' kg', o.shipped_at
FROM ship_order o
WHERE o.status = 'SHIPPED'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'SHIP_ORDER' AND t.ref_id = o.id AND t.type = 'CONSUME');

-- ---------- 17. 超时保险（新包裹 1/4 投保）+ 保费/理赔流水 ----------
INSERT INTO insurance_policy (policy_no, user_id, parcel_id, premium, coverage, status, expire_at, claimed_at, settled_at, created_at)
SELECT 'IN' || to_char(p.inbound_time, 'YYYYMMDD') || lpad(p.id::text, 6, '0'),
       p.owner_id, p.id, 1.00, 5.00,
       CASE WHEN p.status = 'OVERDUE' THEN 'CLAIMED'
            WHEN p.status IN ('PICKED_UP','RETURNED') AND p.outbound_time > p.inbound_time + interval '72 hour' THEN 'CLAIM_PAID'
            WHEN p.status IN ('PICKED_UP','RETURNED') THEN 'EXPIRED'
            ELSE 'ACTIVE' END,
       p.inbound_time + interval '168 hour',
       CASE WHEN p.status = 'OVERDUE' THEN now() - interval '5 hour'
            WHEN p.status IN ('PICKED_UP','RETURNED') AND p.outbound_time > p.inbound_time + interval '72 hour' THEN p.outbound_time + interval '10 minute'
            ELSE NULL END,
       CASE WHEN p.status IN ('PICKED_UP','RETURNED') AND p.outbound_time > p.inbound_time + interval '72 hour' THEN p.outbound_time + interval '30 minute' ELSE NULL END,
       p.inbound_time + interval '2 hour'
FROM parcel p
WHERE p.id > 462 AND abs(hashtext('ins24' || p.id)) % 4 = 0
ON CONFLICT (parcel_id) DO NOTHING;

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT i.user_id, 'CONSUME', -i.premium, 0, 'BALANCE', '超时保险保费', 'INSURANCE', i.id,
       '保单 ' || i.policy_no || '，理赔额度 ' || to_char(i.coverage, 'FM990.00') || ' 元', i.created_at
FROM insurance_policy i
WHERE NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'INSURANCE' AND t.ref_id = i.id AND t.type = 'CONSUME');

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT i.user_id, 'EARN', i.coverage, 0, 'SYSTEM', '超时保险理赔', 'INSURANCE_CLAIM', i.id,
       '保单 ' || i.policy_no || ' 包裹超时理赔到账', i.settled_at
FROM insurance_policy i
WHERE i.status = 'CLAIM_PAID'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'INSURANCE_CLAIM' AND t.ref_id = i.id);

-- ---------- 18. 代取服务单（OPEN 挂无授权在库包裹；DONE 挂 USED 授权链） ----------
INSERT INTO proxy_order (order_no, parcel_id, requester_id, fee, tip, status, deliver_to, note, expire_at, created_at)
SELECT 'PO24' || to_char(p.inbound_time, 'YYYYMMDD') || lpad(p.id::text, 5, '0'),
       p.id, p.owner_id,
       (ARRAY[3,4,5,6,8])[abs(hashtext('fee24' || p.id)) % 5 + 1]::numeric,
       (ARRAY[0,0,1,2])[abs(hashtext('tip24' || p.id)) % 4 + 1]::numeric,
       'OPEN',
       (ARRAY['东区学生公寓3号楼 512 室','西区食堂北侧平房 驿站前台','图书馆二楼自习区','南区宿舍9号楼 208 室'])[abs(hashtext('addr24' || p.id)) % 4 + 1],
       (ARRAY['麻烦轻拿轻放，易碎品','放到宿舍门口即可，谢谢','下午六点前送到','可以顺路带一杯奶茶吗'])[abs(hashtext('note24' || p.id)) % 4 + 1],
       now() + interval '20 hours',
       greatest(p.inbound_time + interval '2 hours', now() - interval '10 hours')
FROM parcel p
WHERE p.id > 462 AND p.status IN ('IN_STOCK','OVERDUE')
  AND NOT EXISTS (SELECT 1 FROM proxy_auth a WHERE a.parcel_id = p.id AND a.status = 'ACTIVE')
  AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.parcel_id = p.id)
  AND abs(hashtext('open24' || p.id)) % 3 = 0
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO proxy_order (order_no, parcel_id, requester_id, runner_id, auth_id, fee, tip, commission, status,
                         deliver_to, note, expire_at, created_at, accepted_at, picked_at, delivered_at, done_at, rating, review)
SELECT 'PO24' || to_char(a.created_at, 'YYYYMMDD') || lpad(a.id::text, 5, '0') || 'D',
       a.parcel_id, a.grantor_id, a.grantee_id, a.id,
       (ARRAY[3,5,6,8])[abs(hashtext('dfee24' || a.id)) % 4 + 1]::numeric,
       (ARRAY[0,1,2])[abs(hashtext('dtip24' || a.id)) % 3 + 1]::numeric,
       round((ARRAY[3,5,6,8])[abs(hashtext('dfee24' || a.id)) % 4 + 1] * 0.1, 2),
       'DONE',
       (ARRAY['东区学生公寓3号楼 512 室','西区食堂北侧平房 驿站前台','图书馆二楼自习区'])[abs(hashtext('daddr24' || a.id)) % 3 + 1],
       '已完成的历史代取服务',
       a.expire_at, a.created_at, a.created_at + interval '20 minutes',
       pr.picked_at, pr.picked_at + interval '40 minutes', pr.picked_at + interval '55 minutes',
       (ARRAY[5,5,4,5,3])[abs(hashtext('rate24' || a.id)) % 5 + 1],
       (ARRAY['速度很快，包装完好，感谢！','准时送到，态度很好','稍微晚了一点，但整体不错','非常靠谱，下次还找你'])[abs(hashtext('rev24' || a.id)) % 4 + 1]
FROM proxy_auth a
JOIN pickup_record pr ON pr.auth_id = a.id
WHERE a.status = 'USED' AND a.parcel_id > 462
  AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.auth_id = a.id)
ON CONFLICT (order_no) DO NOTHING;

-- 少量进行中订单（ACCEPTED），复用 ACTIVE 授权且不改包裹状态
INSERT INTO proxy_order (order_no, parcel_id, requester_id, runner_id, auth_id, fee, tip, status,
                         deliver_to, note, expire_at, created_at, accepted_at)
SELECT 'PO24' || to_char(a.created_at, 'YYYYMMDD') || lpad(a.id::text, 5, '0') || 'R',
       a.parcel_id, a.grantor_id, a.grantee_id, a.id,
       (ARRAY[4,5,6,8])[abs(hashtext('rfee24' || a.id)) % 4 + 1]::numeric, 1,
       'ACCEPTED',
       '东区学生公寓' || (abs(hashtext('bld24' || a.id)) % 8 + 1) || '号楼 ' || (abs(hashtext('rm24' || a.id)) % 600 + 101) || ' 室',
       '接单后请尽快到柜台取件', a.expire_at,
       a.created_at, a.created_at + interval '30 minutes'
FROM proxy_auth a
WHERE a.status = 'ACTIVE' AND a.parcel_id > 462 AND abs(hashtext('run24' || a.id)) % 6 = 0
  AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.auth_id = a.id)
ON CONFLICT (order_no) DO NOTHING;

-- 托管扣费 / 完成结算流水
INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT o.requester_id, 'CONSUME', -(o.fee + o.tip), 0, 'BALANCE', '代取服务费托管', 'PROXY_ORDER', o.id,
       '订单 ' || o.order_no, o.created_at
FROM proxy_order o
WHERE NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type='PROXY_ORDER' AND t.ref_id=o.id AND t.type='CONSUME');

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT o.runner_id, 'EARN', o.fee + o.tip - o.commission, 0, 'BALANCE', '代取服务收入', 'PROXY_ORDER', o.id,
       '订单 ' || o.order_no || '（已扣平台服务费 ' || to_char(o.commission, 'FM999990.00') || ' 元）', o.done_at
FROM proxy_order o
WHERE o.status = 'DONE' AND o.runner_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type='PROXY_ORDER' AND t.ref_id=o.id AND t.type='EARN');

-- ---------- 19. 客服工单（~50）+ 回复 ----------
INSERT INTO service_ticket (ticket_no, user_id, category, title, content, contact, status, is_read, handler_id, handled_at, created_at, updated_at)
SELECT 'TK24' || lpad(g::text, 6, '0'),
       u.id,
       (ARRAY['PARCEL','PROXY','WALLET','SHIP','OTHER'])[1 + g % 5],
       (ARRAY['取件码短信未收到','代取订单无人接单','充值赠送金额疑问','自寄运费咨询','建议延长营业时间','包裹外包装破损申诉','身份条码无法核验','滞留件保管费咨询'])[1 + g % 8],
       (ARRAY['包裹显示已到站但一直没收到取件码，麻烦查一下通知发送记录。',
              '发布的代取单一直待接单，服务费已托管，能否催一下或告知取消流程？',
              '按规则充值应有赠送金额，但余额似乎只加了本金，请核对订单。',
              '想寄一箱书回家，大概 6 公斤，请问运费怎么算、支持哪些承运商？',
              '晚课下课后赶过去经常关门了，希望考试周延长营业到 22:00。',
              '取件时发现外箱破损，内件疑似受潮，希望调取出库前检查记录。',
              '柜台扫我的身份条码提示无效，重新生成后仍不行，请协助排查。',
              '包裹滞留了五天，想确认保管费的计费标准和封顶金额。'])[1 + g % 8],
       u.phone,
       CASE WHEN g % 10 < 4 THEN 'PENDING' WHEN g % 10 < 6 THEN 'PROCESSING' ELSE 'DONE' END,
       CASE WHEN g % 3 <> 0 THEN 1 ELSE 0 END,
       CASE WHEN g % 10 >= 4 THEN (SELECT id FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (g % 25) LIMIT 1) ELSE NULL END,
       CASE WHEN g % 10 >= 6 THEN now() - ((g % 48) || ' hours')::interval ELSE NULL END,
       now() - ((g % 72 + 1) || ' hours')::interval,
       now() - ((g % 24) || ' hours')::interval
FROM generate_series(1, 50) g
JOIN LATERAL (SELECT id, phone FROM sys_user WHERE username LIKE '202%' AND status = 1 ORDER BY id OFFSET (g * 7 % 450) LIMIT 1) u ON true
ON CONFLICT (ticket_no) DO NOTHING;

INSERT INTO ticket_reply (ticket_id, author_id, author_role, content, created_at)
SELECT t.id, t.handler_id, 'STAFF',
       (ARRAY['已核实并处理：相关记录已补发/更正，如仍有问题请携带运单号到柜台，我们会调取原始留痕核查。',
              '已在驿站群帮您转发提醒。若 12 小时内仍无人接单，可在订单页直接取消，托管费用全额退回钱包。',
              '经核对账务无误：赠送金额与本金一并入账，可在钱包流水中查看「账户充值（含赠送）」明细。',
              '运费按首重 8 元/公斤、续重 2 元/公斤计收，偏远地区每公斤另加 4 元；支持顺丰/中通/圆通/京东/申通。'])[1 + abs(hashtext('trp' || t.id)) % 4],
       t.handled_at
FROM service_ticket t
WHERE t.ticket_no LIKE 'TK24%' AND t.status = 'DONE' AND t.handler_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ticket_reply r WHERE r.ticket_id = t.id);

-- ---------- 20. 意见反馈（~40） ----------
INSERT INTO feedback (user_id, type, content, contact, status, reply, replier_id, replied_at, created_at)
SELECT u.id,
       (ARRAY['SUGGEST','BUG','COMPLAINT','PRAISE'])[1 + g % 4],
       (ARRAY['希望包裹列表能按承运商筛选，件多时翻起来费劲。',
              '暗色模式下部分页面金额对比度偏低，看起来吃力。',
              '高峰期取件排队时间较长，希望增加开放窗口。',
              '驿站员服务很耐心，大件主动帮忙搬到推车，点赞！',
              '建议通知里附上货架编号，找件更快。',
              '身份条码页在手机上偶尔加载慢，希望优化。',
              '希望自寄支持上门揽收预约时段选择。',
              '代取结算很及时，体验不错，继续保持。'])[1 + g % 8] || '（编号 ' || g || '）',
       CASE WHEN g % 3 = 0 THEN u.email ELSE NULL END,
       CASE WHEN g % 2 = 0 THEN 'REPLIED' ELSE 'PENDING' END,
       CASE WHEN g % 2 = 0 THEN '感谢您的反馈！我们已记录并纳入迭代评估，处理进展会通过站内信同步。' ELSE NULL END,
       CASE WHEN g % 2 = 0 THEN 1 ELSE NULL END,
       CASE WHEN g % 2 = 0 THEN now() - ((g % 24) || ' hours')::interval ELSE NULL END,
       now() - ((g % 96 + 1) || ' hours')::interval
FROM generate_series(1, 40) g
JOIN LATERAL (SELECT id, email FROM sys_user WHERE username LIKE '202%' AND status = 1 ORDER BY id OFFSET (g * 11 % 450) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM feedback f WHERE f.user_id = u.id AND f.content LIKE '%（编号 ' || g || '）');

-- ---------- 21. 操作日志（新包裹入库/核销/退回 + 授权，约 6500 条） ----------
INSERT INTO operation_log (operator_id, operator_name, module, action, target_id, detail, ip, success, created_at)
SELECT st.id, st.real_name, 'parcel', '包裹入库', p.id::text,
       '{"args":{"arg0":"{\"trackingNo\":\"' || p.tracking_no || '\",\"courierCompany\":\"' || p.courier_company
       || '\",\"stationId\":' || p.station_id || ',\"shelfId\":' || COALESCE(p.shelf_id::text, 'null')
       || '}"},"result":"{\"code\":0,\"msg\":\"ok\",\"data\":{\"id\":' || p.id || ',\"trackingNo\":\"' || p.tracking_no
       || '\",\"pickupCode\":\"***\",\"status\":\"IN_STOCK\"}}"}',
       '10.20.' || (p.id % 8) || '.' || (p.id % 250 + 1), 1, p.inbound_time
FROM parcel p
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462
  AND NOT EXISTS (SELECT 1 FROM operation_log l WHERE l.module = 'parcel' AND l.action = '包裹入库' AND l.target_id = p.id::text);

INSERT INTO operation_log (operator_id, operator_name, module, action, target_id, detail, ip, success, created_at)
SELECT st.id, st.real_name, 'parcel',
       CASE WHEN pr.pickup_type = 'PROXY' THEN '代取核销' ELSE '本人取件核销' END,
       p.id::text,
       '{"args":{"arg0":"{\"parcelId\":' || p.id || ',\"pickupType\":\"' || pr.pickup_type
       || '\"}"},"result":"{\"code\":0,\"msg\":\"ok\",\"data\":{\"id\":' || p.id || ',\"status\":\"PICKED_UP\"}}"}',
       '10.20.' || (p.id % 8) || '.' || (p.id % 250 + 1), 1, p.outbound_time
FROM parcel p
JOIN pickup_record pr ON pr.parcel_id = p.id
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'PICKED_UP'
  AND NOT EXISTS (SELECT 1 FROM operation_log l WHERE l.module = 'parcel' AND l.action IN ('本人取件核销','代取核销') AND l.target_id = p.id::text);

INSERT INTO operation_log (operator_id, operator_name, module, action, target_id, detail, ip, success, created_at)
SELECT st.id, st.real_name, 'parcel', '包裹退回登记', p.id::text,
       '{"args":{"arg0":"{\"parcelId\":' || p.id || ',\"reason\":\"' || COALESCE(p.return_reason, '')
       || '\"}"},"result":"{\"code\":0,\"msg\":\"ok\",\"data\":{\"id\":' || p.id || ',\"status\":\"RETURNED\"}}"}',
       '10.20.' || (p.id % 8) || '.' || (p.id % 250 + 1), 1, p.outbound_time
FROM parcel p
JOIN LATERAL (SELECT id, real_name FROM sys_user WHERE username ~ '^staff' ORDER BY id OFFSET (p.id % 25) LIMIT 1) st ON true
WHERE p.id > 462 AND p.status = 'RETURNED'
  AND NOT EXISTS (SELECT 1 FROM operation_log l WHERE l.module = 'parcel' AND l.action = '包裹退回登记' AND l.target_id = p.id::text);

INSERT INTO operation_log (operator_id, operator_name, module, action, target_id, detail, ip, success, created_at)
SELECT a.grantor_id, u.real_name, 'auth', '发起代取授权', a.id::text,
       '{"args":{"arg0":"{\"parcelId\":' || a.parcel_id || ',\"granteeId\":' || a.grantee_id
       || '}"},"result":"{\"code\":0,\"msg\":\"ok\",\"data\":{\"id\":' || a.id || ',\"authCode\":\"' || a.auth_code
       || '\",\"status\":\"' || a.status || '\"}}"}',
       '10.20.3.' || (a.id % 250 + 1), 1, a.created_at
FROM proxy_auth a JOIN sys_user u ON u.id = a.grantor_id
WHERE a.parcel_id > 462
  AND NOT EXISTS (SELECT 1 FROM operation_log l WHERE l.module = 'auth' AND l.action = '发起代取授权' AND l.target_id = a.id::text);

-- ---------- 22. 公告扩充 + 已读回填 ----------
INSERT INTO notice (title, content, status, publisher_id, is_popup, priority, category, created_at)
SELECT v.title, v.content, 1, 1, v.is_popup, v.priority, v.category, now() - (v.days || ' days')::interval
FROM (VALUES
 ('南区综合驿站等四个新站点投入运营', E'为缓解取件高峰压力，南区综合驿站、北区智能柜站点、研究生公寓驿站、国际交流中心站点已正式启用。\n\n各站点营业时间见「驿站信息」页，包裹将按收件地址就近分配，请留意到站通知中的站点名称。', 0, 1, 'OPERATION', 3),
 ('期末季错峰取件倡议', E'期末考试周临近，预计每日 17:00-19:00 为取件高峰。\n\n建议同学们：\n1. 收到到站通知后 24 小时内领取；\n2. 使用代取服务或身份条码快速核销；\n3. 大件包裹避开高峰时段。', 0, 0, 'NOTICE', 5),
 ('超时保险理赔规则说明', E'超时保险保费 1 元/件，保障期 168 小时。\n\n包裹在库超过 72 小时触发理赔条件，理赔金 5 元自动入账钱包，可在「资金流水」中查看「超时保险理赔」记录。', 0, 0, 'RULE', 8),
 ('寒假前包裹清库通知', E'寒假放假前一周将对连续滞留超过 7 日的包裹集中登记退回承运商。\n\n请同学们离校前务必清空在库包裹，如需保留请联系柜台办理延期保管。', 0, 1, 'OPERATION', 12),
 ('驿站服务满意度调查', E'为持续改进服务质量，诚邀同学们在「意见反馈」页提交使用体验与建议。\n\n有效建议将纳入迭代计划，被采纳的建议提出人可获赠代取服务体验券一张。', 0, 0, 'NOTICE', 15)
) AS v(title, content, is_popup, priority, category, days)
WHERE NOT EXISTS (SELECT 1 FROM notice n WHERE n.title = v.title);

INSERT INTO notice_read (user_id, notice_id, read_at)
SELECT u.id, n.id, n.created_at + interval '1 day'
FROM sys_user u CROSS JOIN notice n
WHERE u.username LIKE '2024%' AND n.status = 1 AND abs(hashtext('nr' || u.id || n.id)) % 2 = 0
ON CONFLICT (user_id, notice_id) DO NOTHING;

-- ---------- 23. 协议签署（新用户签必备协议；每 20 人留 1 人未签用于测试协议门禁流程） ----------
INSERT INTO policy_acceptance (user_id, policy_id, version, ip, accepted_at)
SELECT u.id, p.id, p.version, '10.20.9.' || (u.id % 250 + 1), u.created_at + interval '1 minute'
FROM sys_user u CROSS JOIN policy p
WHERE (u.username LIKE '2024%' OR u.username ~ '^staff[0-9]{2}$')
  AND p.required = 1 AND p.status = 1
  AND abs(hashtext('pa' || u.id)) % 20 <> 0
ON CONFLICT (user_id, policy_id, version) DO NOTHING;

-- ---------- 24. 广告位补充 ----------
INSERT INTO ad_slot (slot_key, title, content, link_url, theme, weight, status, start_at, end_at)
SELECT v.slot_key, v.title, v.content, v.link_url, v.theme, v.weight, 1, now() - interval '2 day', now() + interval '28 day'
FROM (VALUES
 ('HOME_BANNER', '新站点启用', '南区/北区/研究生公寓/国际交流中心四站点上线', '/student/stations', 'cool', 9),
 ('PICKUP_SIDE', '期末错峰取件', '高峰 17:00-19:00，建议代取或身份码快速核销', '/student/proxy-market', 'neutral', 5),
 ('WALLET_FOOT', '暖冬红包', '领券中心上新：暖冬现金红包 6.6 元无门槛', '/student/coupons', 'warm', 4)
) AS v(slot_key, title, content, link_url, theme, weight)
WHERE NOT EXISTS (SELECT 1 FROM ad_slot a WHERE a.slot_key = v.slot_key AND a.title = v.title);

-- ---------- 25. 余额重算（全量流水滚动结余，保证 CHECK(balance>=0)） ----------
WITH ordered AS (
    SELECT t.id, t.user_id, t.amount,
           sum(t.amount) OVER (PARTITION BY t.user_id ORDER BY t.created_at, t.id) AS running
    FROM wallet_transaction t
), mn AS (
    SELECT user_id, min(running) AS m FROM ordered GROUP BY user_id
), fixed AS (
    SELECT o.id, o.running + GREATEST(0, -mn.m) + 30 AS bal FROM ordered o JOIN mn ON mn.user_id = o.user_id
)
UPDATE wallet_transaction t SET balance_after = f.bal FROM fixed f WHERE t.id = f.id;

UPDATE wallet_account w SET
    balance = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id), 0) + 30,
    total_recharge = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.type='RECHARGE'), 0),
    total_spent = COALESCE((SELECT -sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.amount < 0), 0),
    total_earned = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.type='EARN'), 0),
    updated_at = now();

COMMIT;

-- ---------- 26. 结果自检 ----------
SELECT 'recharge_order' AS t, count(*) FROM recharge_order
UNION ALL SELECT 'invite_rebate', count(*) FROM invite_rebate
UNION ALL SELECT 'friendship', count(*) FROM friendship
UNION ALL SELECT 'chat_message', count(*) FROM chat_message
UNION ALL SELECT 'identity_code', count(*) FROM identity_code
UNION ALL SELECT 'user_address', count(*) FROM user_address
UNION ALL SELECT 'receive_preference', count(*) FROM receive_preference
UNION ALL SELECT 'phone_contact', count(*) FROM phone_contact
UNION ALL SELECT 'coupon_template', count(*) FROM coupon_template
UNION ALL SELECT 'user_coupon', count(*) FROM user_coupon
UNION ALL SELECT 'redeem_code', count(*) FROM redeem_code
UNION ALL SELECT 'ship_order', count(*) FROM ship_order
UNION ALL SELECT 'insurance_policy', count(*) FROM insurance_policy
UNION ALL SELECT 'proxy_order', count(*) FROM proxy_order
UNION ALL SELECT 'service_ticket', count(*) FROM service_ticket
UNION ALL SELECT 'ticket_reply', count(*) FROM ticket_reply
UNION ALL SELECT 'feedback', count(*) FROM feedback
UNION ALL SELECT 'operation_log', count(*) FROM operation_log
UNION ALL SELECT 'notice', count(*) FROM notice
UNION ALL SELECT 'notice_read', count(*) FROM notice_read
UNION ALL SELECT 'policy_acceptance', count(*) FROM policy_acceptance
UNION ALL SELECT 'ad_slot', count(*) FROM ad_slot
UNION ALL SELECT 'wallet_transaction', count(*) FROM wallet_transaction;