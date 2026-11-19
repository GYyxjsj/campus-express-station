-- ============================================================
-- 07_scale_10x.sql  数据扩容：业务表约 10 倍（幂等，重复执行不叠加）
-- 原则：保持外键/唯一约束/状态机/账务一致性；配置与 RBAC 字典表不扩容
-- 执行: psql -U <用户名> -d <库名> -f 07_scale_10x.sql (search_path=campus_express)
-- ============================================================
BEGIN;

-- ---------- 0. 基线快照（首次执行时记录，用于幂等判断） ----------
CREATE TABLE IF NOT EXISTS scale_baseline (
    table_name VARCHAR(64) PRIMARY KEY,
    base_count BIGINT NOT NULL,
    taken_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO scale_baseline(table_name, base_count)
SELECT relname, n_live_tup FROM pg_stat_user_tables
WHERE schemaname = 'campus_express' AND relname <> 'scale_baseline'
ON CONFLICT (table_name) DO NOTHING;

-- ---------- 1. 用户：49 -> ~490（新增学生 20240001..20240414 + 员工 staff04..staff30） ----------
INSERT INTO sys_user (username, real_name, phone, email, password, status, created_at, updated_at, invite_code)
SELECT '2024' || lpad(g::text, 4, '0'),
       (ARRAY['王','李','张','刘','陈','杨','赵','黄','周','吴','徐','孙','马','朱','胡','郭','何','林','罗','郑'])[1 + g % 20]
       || (ARRAY['子涵','浩然','欣怡','梓萱','宇轩','雨桐','思远','嘉懿','晨曦','若曦','明轩','静怡','博文','诗涵','俊杰','梦琪','天佑','语嫣','志强','雅雯'])[1 + (g / 20) % 20],
       '13' || lpad((800000000 + g * 7919)::text, 9, '0'),
       'stu2024' || lpad(g::text, 4, '0') || '@campus.edu.cn',
       (SELECT password FROM sys_user WHERE username = '20230101'),
       CASE WHEN g % 37 = 0 THEN 0 ELSE 1 END,
       now() - ((g % 300) || ' days')::interval - ((g % 24) || ' hours')::interval,
       now(),
       upper(substr(md5('invite2024' || g), 1, 8))
FROM generate_series(1, 414) g
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = '2024' || lpad(g::text, 4, '0'));

INSERT INTO sys_user (username, real_name, phone, email, password, status, created_at, updated_at, invite_code)
SELECT 'staff' || lpad(g::text, 2, '0'),
       (ARRAY['钱','孙','李','周','吴','郑','王','冯','陈','褚'])[1 + g % 10] || (ARRAY['建国','丽华','文博','秀英','德福','桂兰','铁军','淑珍','向东','春梅'])[1 + (g / 10) % 10],
       '15' || lpad((900000000 + g * 104729)::text, 9, '0'),
       'staff' || lpad(g::text, 2, '0') || '@campus.edu.cn',
       (SELECT password FROM sys_user WHERE username = 'staff01'),
       1, now() - ((100 + g * 3) || ' days')::interval, now(),
       upper(substr(md5('invitestaff' || g), 1, 8))
FROM generate_series(4, 30) g
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = 'staff' || lpad(g::text, 2, '0'));

-- 角色绑定
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.role_code = 'STUDENT'
WHERE u.username LIKE '2024%'
ON CONFLICT (user_id, role_id) DO NOTHING;
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.role_code = 'STAFF'
WHERE u.username ~ '^staff[0-9]{2}$' AND u.username <> 'staff01'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 邀请关系：新学生按序绑定到老学生（含演示号），保持 invited_by 有效
UPDATE sys_user u SET invited_by = inv.id
FROM (
    SELECT nu.id, (SELECT ou.id FROM sys_user ou JOIN sys_user_role our ON our.user_id = ou.id
                   JOIN sys_role orr ON orr.id = our.role_id AND orr.role_code = 'STUDENT'
                   WHERE ou.username LIKE '2023%' ORDER BY ou.id
                   OFFSET (abs(hashtext(nu.username)) % 40) LIMIT 1) AS inv_id
    FROM sys_user nu WHERE nu.username LIKE '2024%' AND nu.invited_by IS NULL
) m JOIN sys_user inv ON inv.id = m.inv_id
WHERE u.id = m.id;

-- ---------- 2. 驿站/货架：station 2->6, shelf 12->~120 ----------
INSERT INTO station (name, location, open_hours, contact, status)
SELECT v.name, v.location, v.open_hours, v.contact, 1
FROM (VALUES
 ('南区综合驿站', '南区宿舍9号楼西侧', '07:30-22:00', '0571-88001003'),
 ('北区智能柜站点', '北区图书馆负一层', '00:00-24:00', '0571-88001004'),
 ('研究生公寓驿站', '研究生公寓2号楼门厅', '08:00-21:30', '0571-88001005'),
 ('国际交流中心站点', '国际交流中心一层大堂', '08:30-20:30', '0571-88001006')
) AS v(name, location, open_hours, contact)
WHERE NOT EXISTS (SELECT 1 FROM station s WHERE s.name = v.name);

INSERT INTO shelf (station_id, code, capacity, status)
SELECT s.id,
       chr(65 + (g % 4)) || '-' || lpad((1 + (g / 4) % 6)::text, 2, '0') || '-' || lpad((1 + g % 3)::text, 2, '0'),
       (ARRAY[40, 60, 80])[1 + g % 3],
       CASE WHEN g % 23 = 0 THEN 0 ELSE 1 END
FROM station s CROSS JOIN generate_series(0, 23) g
WHERE NOT EXISTS (
    SELECT 1 FROM shelf f WHERE f.station_id = s.id
      AND f.code = chr(65 + (g % 4)) || '-' || lpad((1 + (g / 4) % 6)::text, 2, '0') || '-' || lpad((1 + g % 3)::text, 2, '0')
);

-- ---------- 3. 钱包开户（每用户一户） ----------
INSERT INTO wallet_account (user_id, balance, total_recharge, total_spent, total_earned, status)
SELECT u.id, 30, 0, 0, 0, 1 FROM sys_user u
WHERE NOT EXISTS (SELECT 1 FROM wallet_account w WHERE w.user_id = u.id);

-- ---------- 4. 包裹：362 -> ~3620（状态分布贴近真实：79% 已取 / 8% 在库 / 8% 滞留 / 5% 退回） ----------
INSERT INTO parcel (tracking_no, courier_company, owner_id, station_id, shelf_id, pickup_code, status,
                    remark, extra, inbound_time, outbound_time, return_reason, created_at, updated_at)
SELECT
    cc.code || lpad((1000000000000 + g * 104729 % 899999999999)::text, 13, '0'),
    cc.code,
    stu.id,
    st.id,
    CASE WHEN g % 10 < 8 THEN sh.id ELSE NULL END,
    lpad((abs(hashtext('pk' || g)) % 1000000)::text, 6, '0'),
    CASE WHEN g % 100 < 79 THEN 'PICKED_UP'
         WHEN g % 100 < 87 THEN 'IN_STOCK'
         WHEN g % 100 < 95 THEN 'OVERDUE'
         ELSE 'RETURNED' END,
    CASE WHEN g % 17 = 0 THEN '易碎品，轻拿轻放' WHEN g % 23 = 0 THEN '大件，需推车' ELSE NULL END,
    '{}'::jsonb,
    ib.t,
    CASE WHEN g % 100 < 79 THEN ib.t + ((2 + g % 40) || ' hours')::interval
         WHEN g % 100 >= 95 THEN ib.t + ((80 + g % 30) || ' hours')::interval
         ELSE NULL END,
    CASE WHEN g % 100 >= 95 THEN (ARRAY['超期未取，承运商召回','物主申请退回','地址异常退回'])[1 + g % 3] ELSE NULL END,
    ib.t, ib.t
FROM generate_series(1, 3258) g
CROSS JOIN LATERAL (SELECT (ARRAY['SF','ZT','YT','JD','STO','YD','EMS','JT'])[1 + g % 8] AS code) cc
CROSS JOIN LATERAL (
    SELECT now() - CASE WHEN g % 100 < 79 OR g % 100 >= 95
                        THEN ((g % 120) || ' days')::interval + ((g % 24) || ' hours')::interval
                        WHEN g % 100 < 87 THEN ((g % 60) || ' hours')::interval
                        ELSE ((4 + g % 10) || ' days')::interval END AS t
) ib
JOIN LATERAL (SELECT id FROM sys_user WHERE username LIKE '202%' AND status = 1
              ORDER BY id OFFSET (g % 450) LIMIT 1) stu ON true
JOIN LATERAL (SELECT id FROM station WHERE status = 1 ORDER BY id OFFSET (g % 6) LIMIT 1) st ON true
JOIN LATERAL (SELECT id FROM shelf WHERE station_id = st.id AND status = 1
              ORDER BY id OFFSET (g % 20) LIMIT 1) sh ON true
WHERE NOT EXISTS (
    SELECT 1 FROM parcel p WHERE p.tracking_no = cc.code || lpad((1000000000000 + g * 104729 % 899999999999)::text, 13, '0')
);

COMMIT;