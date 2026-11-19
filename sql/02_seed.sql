-- ============================================================
-- 种子数据（演示/测试用）
-- 所有账号初始密码: 123456 （BCrypt cost=10, $2a$）
-- 执行: psql -U postgres -d campus_express -f 02_seed.sql
-- ============================================================
BEGIN;

-- ---------- 角色 ----------
INSERT INTO sys_role (id, role_code, role_name, remark) VALUES
 (1, 'ADMIN',   '系统管理员', '全部权限'),
 (2, 'STAFF',   '驿站员',     '入库/核销/滞留处理'),
 (3, 'STUDENT', '学生',       '包裹/提醒/代取授权')
ON CONFLICT (id) DO NOTHING;
SELECT setval('sys_role_id_seq', 10, true);

-- ---------- 权限点 ----------
INSERT INTO sys_permission (id, perm_code, perm_name, module) VALUES
 (1,  'parcel:inbound',      '包裹入库',     'parcel'),
 (2,  'parcel:pickup',       '本人取件核销', 'parcel'),
 (3,  'parcel:proxy-pickup', '代取核销',     'parcel'),
 (4,  'auth:create',         '发起代取授权', 'auth'),
 (5,  'auth:revoke',         '撤销代取授权', 'auth'),
 (6,  'reminder:send',       '提醒补发',     'reminder'),
 (7,  'user:manage',         '用户管理',     'admin'),
 (8,  'station:manage',      '驿站货架管理', 'admin'),
 (9,  'stats:view',          '统计看板',     'admin'),
 (10, 'log:view',            '操作日志',     'admin'),
 (11, 'notice:manage',       '公告管理',     'admin')
ON CONFLICT (id) DO NOTHING;
SELECT setval('sys_permission_id_seq', 20, true);

-- ADMIN: 全部; STAFF: 1,2,3,6; STUDENT: 4,5
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission ON CONFLICT DO NOTHING;
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
 (2,1),(2,2),(2,3),(2,6),(3,4),(3,5) ON CONFLICT DO NOTHING;

-- ---------- 用户（密码均为 123456） ----------
INSERT INTO sys_user (id, username, real_name, phone, email, password, status) VALUES
 (1, 'admin',    '系统管理员', '13800000001', 'admin@campus.edu.cn',
     '$2a$10$g2aw/jO8RHlKvLPIS8zxQux7RPNHvFK7HSBGr90hYgZbL5SJ1JbXi', 1),
 (2, 'staff01',  '王驿站',     '13800000002', 'staff01@campus.edu.cn',
     '$2a$10$g2aw/jO8RHlKvLPIS8zxQux7RPNHvFK7HSBGr90hYgZbL5SJ1JbXi', 1),
 (3, '20230101', '张小明',     '13900000101', 'zxm@stu.campus.edu.cn',
     '$2a$10$g2aw/jO8RHlKvLPIS8zxQux7RPNHvFK7HSBGr90hYgZbL5SJ1JbXi', 1),
 (4, '20230102', '李小红',     '13900000102', 'lxh@stu.campus.edu.cn',
     '$2a$10$g2aw/jO8RHlKvLPIS8zxQux7RPNHvFK7HSBGr90hYgZbL5SJ1JbXi', 1),
 (5, '20230103', '王小刚',     '13900000103', 'wxg@stu.campus.edu.cn',
     '$2a$10$g2aw/jO8RHlKvLPIS8zxQux7RPNHvFK7HSBGr90hYgZbL5SJ1JbXi', 1)
ON CONFLICT (id) DO NOTHING;
SELECT setval('sys_user_id_seq', 100, true);

INSERT INTO sys_user_role (user_id, role_id) VALUES
 (1,1),(2,2),(3,3),(4,3),(5,3) ON CONFLICT DO NOTHING;

-- ---------- 驿站 / 货架 ----------
INSERT INTO station (id, name, location, open_hours, contact) VALUES
 (1, '东区菜鸟驿站', '东区学生公寓3号楼架空层', '08:00-21:00', '13800000002'),
 (2, '西区快递服务中心', '西区食堂北侧平房', '08:30-20:30', '13800000002')
ON CONFLICT (id) DO NOTHING;
SELECT setval('station_id_seq', 10, true);

INSERT INTO shelf (id, station_id, code, capacity) VALUES
 (1, 1, 'A-01-01', 40), (2, 1, 'A-01-02', 40), (3, 1, 'A-02-01', 40),
 (4, 1, 'B-01-01', 60), (5, 2, 'A-01-01', 40), (6, 2, 'A-01-02', 40)
ON CONFLICT (id) DO NOTHING;
SELECT setval('shelf_id_seq', 20, true);

-- ---------- 演示包裹 ----------
-- 在库新件(张小明) / 在库超24h(李小红, 用于催领演示) / 滞留件(王小刚) / 已取件(张小明)
INSERT INTO parcel (id, tracking_no, courier_company, owner_id, station_id, shelf_id,
                    pickup_code, status, inbound_time, outbound_time) VALUES
 (1, 'SF1390001111222', 'SF', 3, 1, 1, '582914', 'IN_STOCK', now() - interval '2 hours',  NULL),
 (2, 'ZT7788001122334', 'ZT', 4, 1, 2, '193756', 'IN_STOCK', now() - interval '30 hours', NULL),
 (3, 'JD0099887766554', 'JD', 5, 1, 4, '826403', 'OVERDUE',  now() - interval '80 hours', NULL),
 (4, 'YT5566778899001', 'YT', 3, 1, 3, '675210', 'PICKED_UP', now() - interval '50 hours', now() - interval '26 hours')
ON CONFLICT (id) DO NOTHING;
SELECT setval('parcel_id_seq', 100, true);

-- ---------- 提醒 ----------
INSERT INTO notification (user_id, parcel_id, type, channel, title, content, is_read, sent_at) VALUES
 (3, 1, 'ARRIVE', 'IN_APP', '包裹到站提醒',
  '您的顺丰包裹 SF1390001111222 已到达东区菜鸟驿站，货架 A-01-01，取件码 582914，请及时领取。', 0, now() - interval '2 hours'),
 (4, 2, 'ARRIVE', 'IN_APP', '包裹到站提醒',
  '您的中通包裹 ZT7788001122334 已到达东区菜鸟驿站，货架 A-01-02，取件码 193756，请及时领取。', 1, now() - interval '30 hours'),
 (4, 2, 'REMIND', 'IN_APP', '取件催领提醒',
  '您的包裹 ZT7788001122334 已在库超过24小时，请尽快前往东区菜鸟驿站领取（取件码 193756）。', 0, now() - interval '5 hours'),
 (5, 3, 'OVERDUE', 'IN_APP', '包裹滞留告警',
  '您的京东包裹 JD0099887766554 已滞留超过72小时，请尽快领取，逾期将按驿站规定退回。', 0, now() - interval '6 hours'),
 (1, 3, 'OVERDUE', 'IN_APP', '滞留件升级告警',
  '包裹 JD0099887766554（王小刚）已滞留超72小时，请安排处理。', 0, now() - interval '6 hours');

-- ---------- 历史取件流水（包裹4，本人取件） ----------
INSERT INTO pickup_record (parcel_id, pickup_type, owner_id, picker_id, staff_id, station_id, picked_at)
VALUES (4, 'SELF', 3, 3, 2, 1, now() - interval '26 hours')
ON CONFLICT DO NOTHING;

-- ---------- 提醒策略配置 ----------
INSERT INTO sys_config (config_key, config_value, remark) VALUES
 ('remind.first.delay.hours', '24', '首次催领阈值(小时)'),
 ('remind.overdue.hours',     '72', '滞留判定阈值(小时)'),
 ('remind.daily.max',         '3',  '每人每日催领提醒上限'),
 ('auth.max.valid.hours',     '72', '代取授权最长有效期(小时)')
ON CONFLICT (config_key) DO NOTHING;

-- ---------- 公告 ----------
INSERT INTO notice (title, content, status, publisher_id) VALUES
 ('双十一期间驿站营业时间调整',
  '11月10日至11月20日快递高峰期，东区菜鸟驿站营业时间延长为 07:30-22:30，请同学们错峰取件，入库后48小时内领取可享货架优先整理服务。', 1, 1),
 ('关于规范代取授权的通知',
  '为保障包裹安全，代取请一律通过系统"代取授权"功能生成授权码，驿站不再接受口头委托。授权码有效期最长72小时，可随时撤销。', 1, 1);

COMMIT;