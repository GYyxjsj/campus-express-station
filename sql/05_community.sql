-- ============================================================
-- 校园快递驿站 · 社区与增值服务迁移（幂等，可重复执行）
-- 新增：好友 / 邀请返利 / 红包卡券+兑换码 / 地址簿 / 收货偏好 / 电话簿
--       自寄订单 / 超时保险 / 客服工单 / 意见反馈 / 广告位 / AI 与关于我们配置
-- 执行: PGPASSWORD=... psql -U <用户名> -d <库名> -v ON_ERROR_STOP=1 -f 05_community.sql
-- ============================================================

BEGIN;
SET search_path TO campus_express, public;

-- ---------- 1. 好友关系 ----------
CREATE TABLE IF NOT EXISTS friendship (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES sys_user(id),   -- 发起人
    friend_id  BIGINT      NOT NULL REFERENCES sys_user(id),   -- 接收人
    status     VARCHAR(16) NOT NULL DEFAULT 'PENDING',         -- PENDING/ACCEPTED
    remark     VARCHAR(64),                                    -- 好友备注
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_friendship_pair UNIQUE (user_id, friend_id),
    CONSTRAINT ck_friendship_status CHECK (status IN ('PENDING','ACCEPTED')),
    CONSTRAINT ck_friendship_not_self CHECK (user_id <> friend_id)
);
CREATE INDEX IF NOT EXISTS idx_friendship_friend ON friendship(friend_id, status);
COMMENT ON TABLE friendship IS '好友关系（PENDING=待同意，双向查询由后端合并）';

-- ---------- 2. 邀请有礼（邀请码 + 绑定 + 充值返利 10%） ----------
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS invite_code VARCHAR(12);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS invited_by  BIGINT REFERENCES sys_user(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_invite_code ON sys_user(invite_code) WHERE invite_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS invite_rebate (
    id                BIGSERIAL PRIMARY KEY,
    inviter_id        BIGINT        NOT NULL REFERENCES sys_user(id),
    invitee_id        BIGINT        NOT NULL REFERENCES sys_user(id),
    recharge_order_id BIGINT        NOT NULL REFERENCES recharge_order(id),
    recharge_amount   NUMERIC(12,2) NOT NULL,
    rate_percent      NUMERIC(5,2)  NOT NULL DEFAULT 10,
    amount            NUMERIC(12,2) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_rebate_order UNIQUE (recharge_order_id),
    CONSTRAINT ck_rebate_amount CHECK (amount >= 0)
);
CREATE INDEX IF NOT EXISTS idx_rebate_inviter ON invite_rebate(inviter_id, created_at DESC);
COMMENT ON TABLE invite_rebate IS '邀请返利记录：被邀请人充值成功后按本金 10% 返给邀请人';

-- ---------- 3. 红包卡券 + 兑换码 ----------
CREATE TABLE IF NOT EXISTS coupon_template (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)   NOT NULL,
    type        VARCHAR(16)   NOT NULL DEFAULT 'COUPON',       -- COUPON 优惠券 / RED_PACKET 红包
    amount      NUMERIC(10,2) NOT NULL,
    min_spend   NUMERIC(10,2) NOT NULL DEFAULT 0,              -- 满多少可用，0=无门槛
    scope       VARCHAR(32)   NOT NULL DEFAULT 'ALL',          -- ALL/SHIP/PROXY/INSURANCE
    total_qty   INT           NOT NULL DEFAULT 100,
    claimed_qty INT           NOT NULL DEFAULT 0,
    valid_days  INT           NOT NULL DEFAULT 30,
    description VARCHAR(255),
    status      SMALLINT      NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_coupon_type CHECK (type IN ('COUPON','RED_PACKET')),
    CONSTRAINT ck_coupon_amount CHECK (amount > 0)
);
COMMENT ON TABLE coupon_template IS '卡券模板（优惠券/红包）';

CREATE TABLE IF NOT EXISTS user_coupon (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT      NOT NULL REFERENCES coupon_template(id),
    user_id     BIGINT      NOT NULL REFERENCES sys_user(id),
    code        VARCHAR(24) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'UNUSED',         -- UNUSED/USED/EXPIRED
    source      VARCHAR(16) NOT NULL DEFAULT 'CLAIM',          -- CLAIM 领取 / REDEEM 兑换码 / SYSTEM 发放
    expire_at   TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_coupon_code UNIQUE (code),
    CONSTRAINT uk_user_coupon_once UNIQUE (template_id, user_id),
    CONSTRAINT ck_user_coupon_status CHECK (status IN ('UNUSED','USED','EXPIRED'))
);
CREATE INDEX IF NOT EXISTS idx_user_coupon_user ON user_coupon(user_id, status, expire_at);

CREATE TABLE IF NOT EXISTS redeem_code (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(24) NOT NULL,
    template_id BIGINT      NOT NULL REFERENCES coupon_template(id),
    status      VARCHAR(16) NOT NULL DEFAULT 'UNUSED',         -- UNUSED/USED/DISABLED
    used_by     BIGINT      REFERENCES sys_user(id),
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_redeem_code UNIQUE (code),
    CONSTRAINT ck_redeem_status CHECK (status IN ('UNUSED','USED','DISABLED'))
);
COMMENT ON TABLE redeem_code IS '一次性兑换码，兑换后生成对应卡券';-- ---------- 4. 地址簿（含浏览器定位坐标） ----------
CREATE TABLE IF NOT EXISTS user_address (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES sys_user(id),
    label      VARCHAR(16)  NOT NULL DEFAULT '宿舍',           -- 宿舍/教学楼/家/其他
    contact    VARCHAR(64)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    region     VARCHAR(128) NOT NULL DEFAULT '校本部',
    detail     VARCHAR(255) NOT NULL,
    longitude  NUMERIC(10,6),
    latitude   NUMERIC(10,6),
    is_default SMALLINT     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_address_user ON user_address(user_id, is_default DESC);
COMMENT ON TABLE user_address IS '收货地址簿，longitude/latitude 来自浏览器 Geolocation';

-- ---------- 5. 收货偏好 ----------
CREATE TABLE IF NOT EXISTS receive_preference (
    user_id         BIGINT      PRIMARY KEY REFERENCES sys_user(id),
    time_slot       VARCHAR(16) NOT NULL DEFAULT 'ANY',        -- ANY/MORNING/AFTERNOON/EVENING
    deliver_method  VARCHAR(16) NOT NULL DEFAULT 'SELF',       -- SELF 自取 / PROXY 优先代取 / DOOR 送楼
    notify_channel  VARCHAR(16) NOT NULL DEFAULT 'IN_APP',     -- IN_APP/SMS/EMAIL/ALL
    quiet_hours     VARCHAR(32) NOT NULL DEFAULT '23:00-08:00',
    no_call         SMALLINT    NOT NULL DEFAULT 0,            -- 1=请勿电话联系
    fragile_care    SMALLINT    NOT NULL DEFAULT 1,            -- 易碎件特别关照
    auto_proxy      SMALLINT    NOT NULL DEFAULT 0,            -- 超 24h 自动发代取
    note            VARCHAR(255),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_pref_slot CHECK (time_slot IN ('ANY','MORNING','AFTERNOON','EVENING')),
    CONSTRAINT ck_pref_method CHECK (deliver_method IN ('SELF','PROXY','DOOR')),
    CONSTRAINT ck_pref_channel CHECK (notify_channel IN ('IN_APP','SMS','EMAIL','ALL'))
);

-- ---------- 6. 电话簿（常用联系人） ----------
CREATE TABLE IF NOT EXISTS phone_contact (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES sys_user(id),
    name       VARCHAR(64) NOT NULL,
    phone      VARCHAR(20) NOT NULL,
    relation   VARCHAR(32) NOT NULL DEFAULT '同学',            -- 同学/室友/家人/辅导员/驿站
    is_starred SMALLINT    NOT NULL DEFAULT 0,
    remark     VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_contact_user ON phone_contact(user_id, is_starred DESC);

-- ---------- 7. 服务站自寄 ----------
CREATE TABLE IF NOT EXISTS ship_order (
    id             BIGSERIAL PRIMARY KEY,
    order_no       VARCHAR(32)   NOT NULL,
    user_id        BIGINT        NOT NULL REFERENCES sys_user(id),
    station_id     BIGINT        NOT NULL REFERENCES station(id),
    courier_company VARCHAR(32)  NOT NULL,
    sender_name    VARCHAR(64)   NOT NULL,
    sender_phone   VARCHAR(20)   NOT NULL,
    receiver_name  VARCHAR(64)   NOT NULL,
    receiver_phone VARCHAR(20)   NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    goods_type     VARCHAR(32)   NOT NULL DEFAULT '日用品',
    weight_kg      NUMERIC(8,2)  NOT NULL DEFAULT 1,
    freight        NUMERIC(10,2) NOT NULL,
    insured        SMALLINT      NOT NULL DEFAULT 0,
    tracking_no    VARCHAR(64),
    status         VARCHAR(16)   NOT NULL DEFAULT 'CREATED',   -- CREATED/DROPPED/SHIPPED/CANCELLED
    note           VARCHAR(255),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    dropped_at     TIMESTAMPTZ,
    shipped_at     TIMESTAMPTZ,
    cancelled_at   TIMESTAMPTZ,
    CONSTRAINT uk_ship_no UNIQUE (order_no),
    CONSTRAINT ck_ship_status CHECK (status IN ('CREATED','DROPPED','SHIPPED','CANCELLED')),
    CONSTRAINT ck_ship_weight CHECK (weight_kg > 0)
);
CREATE INDEX IF NOT EXISTS idx_ship_user ON ship_order(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ship_status ON ship_order(status, created_at DESC);
COMMENT ON TABLE ship_order IS '自寄订单：线上下单计费 → 到站投递 → 揽收发出';

-- ---------- 8. 超时保险 ----------
CREATE TABLE IF NOT EXISTS insurance_policy (
    id           BIGSERIAL PRIMARY KEY,
    policy_no    VARCHAR(32)   NOT NULL,
    user_id      BIGINT        NOT NULL REFERENCES sys_user(id),
    parcel_id    BIGINT        NOT NULL REFERENCES parcel(id),
    premium      NUMERIC(10,2) NOT NULL,
    coverage     NUMERIC(10,2) NOT NULL,                       -- 理赔额度
    status       VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',      -- ACTIVE/EXPIRED/CLAIMED/CLAIM_PAID
    expire_at    TIMESTAMPTZ   NOT NULL,
    claimed_at   TIMESTAMPTZ,
    settled_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_policy_no UNIQUE (policy_no),
    CONSTRAINT uk_policy_parcel UNIQUE (parcel_id),
    CONSTRAINT ck_policy_status CHECK (status IN ('ACTIVE','EXPIRED','CLAIMED','CLAIM_PAID'))
);
CREATE INDEX IF NOT EXISTS idx_policy_user ON insurance_policy(user_id, created_at DESC);
COMMENT ON TABLE insurance_policy IS '超时保险：包裹超 72h 未取可理赔，理赔金入钱包';-- ---------- 9. 客服工单（自动通知管理员+员工，待处理/已处理/未读） ----------
CREATE TABLE IF NOT EXISTS service_ticket (
    id          BIGSERIAL PRIMARY KEY,
    ticket_no   VARCHAR(32)   NOT NULL,
    user_id     BIGINT        NOT NULL REFERENCES sys_user(id),
    category    VARCHAR(16)   NOT NULL DEFAULT 'PARCEL',       -- PARCEL/PROXY/WALLET/SHIP/OTHER
    title       VARCHAR(128)  NOT NULL,
    content     VARCHAR(1024) NOT NULL,
    contact     VARCHAR(64),
    status      VARCHAR(16)   NOT NULL DEFAULT 'PENDING',      -- PENDING 待处理 / PROCESSING 处理中 / DONE 已处理
    is_read     SMALLINT      NOT NULL DEFAULT 0,              -- 管理侧是否已读
    handler_id  BIGINT        REFERENCES sys_user(id),
    handled_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_ticket_no UNIQUE (ticket_no),
    CONSTRAINT ck_ticket_status CHECK (status IN ('PENDING','PROCESSING','DONE'))
);
CREATE INDEX IF NOT EXISTS idx_ticket_status ON service_ticket(status, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ticket_user ON service_ticket(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ticket_reply (
    id         BIGSERIAL PRIMARY KEY,
    ticket_id  BIGINT        NOT NULL REFERENCES service_ticket(id),
    author_id  BIGINT        NOT NULL REFERENCES sys_user(id),
    author_role VARCHAR(16)  NOT NULL DEFAULT 'STAFF',         -- USER/STAFF/ADMIN
    content    VARCHAR(1024) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_reply_ticket ON ticket_reply(ticket_id, created_at);
COMMENT ON TABLE service_ticket IS '联系客服工单，提交即站内信通知全部管理员与驿站员';

-- ---------- 10. 意见反馈 ----------
CREATE TABLE IF NOT EXISTS feedback (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES sys_user(id),
    type       VARCHAR(16)   NOT NULL DEFAULT 'SUGGEST',       -- SUGGEST 建议 / BUG 问题 / COMPLAINT 投诉 / PRAISE 表扬
    content    VARCHAR(1024) NOT NULL,
    contact    VARCHAR(64),
    status     VARCHAR(16)   NOT NULL DEFAULT 'PENDING',       -- PENDING/REPLIED
    reply      VARCHAR(1024),
    replier_id BIGINT        REFERENCES sys_user(id),
    replied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_feedback_type CHECK (type IN ('SUGGEST','BUG','COMPLAINT','PRAISE')),
    CONSTRAINT ck_feedback_status CHECK (status IN ('PENDING','REPLIED'))
);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status, created_at DESC);

-- ---------- 11. 广告位 ----------
CREATE TABLE IF NOT EXISTS ad_slot (
    id         BIGSERIAL PRIMARY KEY,
    slot_key   VARCHAR(32)   NOT NULL,                         -- HOME_BANNER/PICKUP_SIDE/WALLET_FOOT
    title      VARCHAR(64)   NOT NULL,
    content    VARCHAR(255)  NOT NULL,
    link_url   VARCHAR(255),
    theme      VARCHAR(16)   NOT NULL DEFAULT 'neutral',       -- neutral/warm/cool（配色倾向）
    weight     INT           NOT NULL DEFAULT 1,               -- 越大越优先
    status     SMALLINT      NOT NULL DEFAULT 1,
    start_at   TIMESTAMPTZ,
    end_at     TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ad_slot ON ad_slot(slot_key, status, weight DESC);
COMMENT ON TABLE ad_slot IS '广告占位：管理员维护，学生端按 slot_key 拉取展示';-- ---------- 12. 权限点 ----------
INSERT INTO sys_permission (perm_code, perm_name, module) VALUES
    ('ticket:handle',  '客服工单处理', 'service'),
    ('feedback:reply', '意见反馈回复', 'service'),
    ('ad:manage',      '广告位管理',   'admin'),
    ('coupon:manage',  '卡券兑换码管理', 'admin'),
    ('ship:manage',    '自寄订单经办', 'ship'),
    ('ai:config',      'AI 接口配置',  'admin')
ON CONFLICT (perm_code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON
    (r.role_code = 'ADMIN' AND p.perm_code IN ('ticket:handle','feedback:reply','ad:manage','coupon:manage','ship:manage','ai:config'))
 OR (r.role_code = 'STAFF' AND p.perm_code IN ('ticket:handle','ship:manage'))
ON CONFLICT (role_id, perm_id) DO NOTHING;

-- ---------- 13. 业务配置（AI / 运费 / 保险 / 邀请 / 关于我们） ----------
INSERT INTO sys_config (config_key, config_value, remark) VALUES
    ('ai.enabled',            '1',   'AI 助手总开关'),
    ('ai.api.url',            'https://api.openai.com/v1/chat/completions', 'OpenAI 兼容 Chat 接口地址'),
    ('ai.api.key',            '',    'API Key（管理员在面板配置，留空则规则应答）'),
    ('ai.api.model',          'gpt-4o-mini', '模型名称'),
    ('ai.max.history',        '8',   '每次携带的历史消息条数'),
    ('ai.timeout.ms',         '15000','请求超时(毫秒)'),
    ('invite.rebate.percent', '10',  '邀请返利比例：被邀请人充值本金的百分比'),
    ('freight.first.kg',      '1',   '运费首重(公斤)'),
    ('freight.first.price',   '8',   '首重价格(元)'),
    ('freight.extra.price',   '2',   '续重每公斤价格(元)'),
    ('freight.remote.extra',  '4',   '偏远地区每公斤附加(元)'),
    ('insurance.premium',     '1',   '超时保险保费(元/件)'),
    ('insurance.coverage',    '5',   '超时保险理赔额度(元/件)'),
    ('insurance.valid.hours', '168', '保险有效期(小时)'),
    ('about.company',         '校园驿站运营服务中心', '关于我们·公司名称'),
    ('about.owner',           '周立群', '关于我们·负责人'),
    ('about.email',           'service@campus-express.cn', '关于我们·联系邮箱'),
    ('about.phone',           '0571-8888-6666', '关于我们·客服电话'),
    ('about.address',         '东区学生公寓3号楼架空层 驿站运营办公室', '关于我们·办公地址'),
    ('about.icp',             '浙ICP备2026001234号-1', '关于我们·备案号'),
    ('about.version',         'v5.0.0', '系统版本号')
ON CONFLICT (config_key) DO NOTHING;

-- ---------- 14. 邀请码回填 + 邀请绑定 ----------
UPDATE sys_user SET invite_code = upper(substr(md5('invite' || id), 1, 8))
WHERE invite_code IS NULL;

-- 确定性绑定：部分学生由更早注册的学生邀请（不成环：只绑 id 更小者）
UPDATE sys_user u SET invited_by = src.id
FROM (
    SELECT s.id, (SELECT min(o.id) FROM sys_user o
                  JOIN sys_user_role orl ON orl.user_id = o.id AND orl.role_id = 3
                  WHERE o.id < s.id AND abs(hashtext('invsrc' || s.id || o.id)) % 5 = 0) AS inviter
    FROM sys_user s JOIN sys_user_role ur ON ur.user_id = s.id AND ur.role_id = 3
) src
WHERE u.id = src.id AND src.inviter IS NOT NULL AND u.invited_by IS NULL;-- ---------- 15. 种子：好友关系 ----------
INSERT INTO friendship (user_id, friend_id, status, remark, created_at, updated_at)
SELECT a.id, b.id,
       CASE WHEN abs(hashtext('fst' || a.id || b.id)) % 3 = 0 THEN 'PENDING' ELSE 'ACCEPTED' END,
       CASE WHEN abs(hashtext('frm' || a.id || b.id)) % 2 = 0 THEN '同班同学' ELSE NULL END,
       now() - ((abs(hashtext('fct' || a.id || b.id)) % 40 + 1) || ' day')::interval,
       now() - ((abs(hashtext('fut' || a.id || b.id)) % 10) || ' day')::interval
FROM sys_user a
JOIN sys_user_role ra ON ra.user_id = a.id AND ra.role_id = 3
JOIN sys_user b ON b.id > a.id AND b.id <= a.id + 6
JOIN sys_user_role rb ON rb.user_id = b.id AND rb.role_id = 3
WHERE abs(hashtext('fpair' || a.id || b.id)) % 4 = 0
ON CONFLICT (user_id, friend_id) DO NOTHING;

-- 演示账号固定好友（保证 20230101 有可看的好友与待处理申请）
INSERT INTO friendship (user_id, friend_id, status, remark, created_at, updated_at) VALUES
 (3, 6,  'ACCEPTED', '室友·若涵', now() - interval '60 day', now() - interval '60 day'),
 (3, 7,  'ACCEPTED', '篮球队·子墨', now() - interval '45 day', now() - interval '45 day'),
 (8, 3,  'PENDING',  NULL, now() - interval '2 day', now() - interval '2 day'),
 (4, 10, 'ACCEPTED', '社团·雨桐', now() - interval '30 day', now() - interval '30 day')
ON CONFLICT (user_id, friend_id) DO UPDATE SET status = EXCLUDED.status, remark = EXCLUDED.remark;

-- ---------- 16. 种子：邀请返利（被邀请人已支付充值 → 邀请人 EARN 10%） ----------
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

-- ---------- 17. 种子：卡券模板 / 用户卡券 / 兑换码 ----------
INSERT INTO coupon_template (id, name, type, amount, min_spend, scope, total_qty, claimed_qty, valid_days, description) VALUES
 (1, '新人自寄立减券', 'COUPON',     3.00,  8.00, 'SHIP',      500, 0, 30, '自寄运费满 8 元可用，下单时自动抵扣'),
 (2, '代取服务体验券', 'COUPON',     2.00,  2.00, 'PROXY',     300, 0, 15, '发布有偿代取时抵扣服务费'),
 (3, '超时保险免费券', 'COUPON',     1.00,  1.00, 'INSURANCE', 200, 0, 30, '兑换一次超时保险保费'),
 (4, '双十一现金红包', 'RED_PACKET', 5.00,  0.00, 'ALL',       100, 0, 7,  '无门槛红包，可抵扣任意驿站服务'),
 (5, '邀新专享红包',   'RED_PACKET', 8.80,  0.00, 'ALL',       50,  0, 30, '成功邀请 3 位好友后可领取')
ON CONFLICT (id) DO NOTHING;
SELECT setval('coupon_template_id_seq', GREATEST((SELECT max(id) FROM coupon_template), 10), true);

INSERT INTO user_coupon (template_id, user_id, code, status, source, expire_at, used_at, created_at)
SELECT t.id, s.id, 'CP' || upper(substr(md5('uc' || s.id || t.id), 1, 14)),
       CASE WHEN abs(hashtext('ucst' || s.id || t.id)) % 5 = 0 THEN 'USED' ELSE 'UNUSED' END,
       'CLAIM',
       now() + (t.valid_days || ' day')::interval,
       CASE WHEN abs(hashtext('ucst' || s.id || t.id)) % 5 = 0 THEN now() - interval '1 day' ELSE NULL END,
       now() - ((abs(hashtext('ucct' || s.id || t.id)) % 10 + 1) || ' day')::interval
FROM sys_user s
JOIN sys_user_role ur ON ur.user_id = s.id AND ur.role_id = 3
JOIN coupon_template t ON t.id = abs(hashtext('uct' || s.id)) % 5 + 1
WHERE abs(hashtext('ucown' || s.id)) % 3 <> 0
ON CONFLICT (template_id, user_id) DO NOTHING;

UPDATE coupon_template t SET claimed_qty = (SELECT count(*) FROM user_coupon c WHERE c.template_id = t.id);

INSERT INTO redeem_code (code, template_id, status, used_by, used_at) VALUES
 ('WELCOME2026', 1, 'UNUSED', NULL, NULL),
 ('EXPRESS666',  2, 'UNUSED', NULL, NULL),
 ('SAFE7DAYS',   3, 'UNUSED', NULL, NULL),
 ('DOUBLE11',    4, 'UNUSED', NULL, NULL),
 ('FRIEND888',   5, 'UNUSED', NULL, NULL),
 ('NEWTERM2026', 1, 'UNUSED', NULL, NULL),
 ('CAMPUSLOVE',  4, 'USED',   3,    now() - interval '3 day'),
 ('STATIONVIP',  5, 'DISABLED', NULL, NULL)
ON CONFLICT (code) DO UPDATE SET status = EXCLUDED.status, used_by = EXCLUDED.used_by, used_at = EXCLUDED.used_at;-- ---------- 18. 种子：地址簿 / 收货偏好 / 电话簿 ----------
INSERT INTO user_address (user_id, label, contact, phone, region, detail, longitude, latitude, is_default, created_at)
SELECT u.id, '宿舍', u.real_name, u.phone, '校本部·东区',
       (abs(hashtext('dorm' || u.id)) % 12 + 1) || '号学生公寓 ' || (abs(hashtext('room' || u.id)) % 6 + 1) || '0' || (abs(hashtext('r2' || u.id)) % 4 + 1) || ' 室',
       120.123456 + (abs(hashtext('lng' || u.id)) % 100) / 10000.0,
       30.256789 + (abs(hashtext('lat' || u.id)) % 100) / 10000.0,
       1, u.created_at + interval '1 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.id AND a.label = '宿舍');

INSERT INTO user_address (user_id, label, contact, phone, region, detail, longitude, latitude, is_default, created_at)
SELECT u.id, '教学楼', u.real_name, u.phone, '校本部·教学区',
       '第' || (abs(hashtext('tb' || u.id)) % 5 + 1) || '教学楼 ' || (abs(hashtext('tbr' || u.id)) % 4 + 1) || '楼自习区（课间自取）',
       NULL, NULL, 0, u.created_at + interval '2 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE abs(hashtext('addr2' || u.id)) % 2 = 0
  AND NOT EXISTS (SELECT 1 FROM user_address a WHERE a.user_id = u.id AND a.label = '教学楼');

INSERT INTO receive_preference (user_id, time_slot, deliver_method, notify_channel, quiet_hours, no_call, fragile_care, auto_proxy, note, updated_at)
SELECT u.id,
       (ARRAY['ANY','MORNING','AFTERNOON','EVENING'])[abs(hashtext('slot' || u.id)) % 4 + 1],
       (ARRAY['SELF','SELF','PROXY','DOOR'])[abs(hashtext('mth' || u.id)) % 4 + 1],
       (ARRAY['IN_APP','IN_APP','SMS','ALL'])[abs(hashtext('chn' || u.id)) % 4 + 1],
       '23:00-08:00',
       (abs(hashtext('nc' || u.id)) % 4 = 0)::int,
       1,
       (abs(hashtext('ap' || u.id)) % 5 = 0)::int,
       CASE abs(hashtext('pn' || u.id)) % 4
            WHEN 0 THEN '大件请提前短信告知，方便推车来取'
            WHEN 1 THEN '生鲜冷链件请电话叫醒，勿放货架常温区'
            WHEN 2 THEN '工作日白天在实验室，晚间集中取件'
            ELSE NULL END,
       now() - ((abs(hashtext('put' || u.id)) % 20) || ' day')::interval
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO phone_contact (user_id, name, phone, relation, is_starred, remark, created_at)
SELECT u.id, f.real_name, f.phone, '同学', 1, '常帮我代取', now() - interval '15 day'
FROM friendship fs JOIN sys_user u ON u.id = fs.user_id JOIN sys_user f ON f.id = fs.friend_id
WHERE fs.status = 'ACCEPTED'
  AND NOT EXISTS (SELECT 1 FROM phone_contact c WHERE c.user_id = fs.user_id AND c.phone = f.phone);

INSERT INTO phone_contact (user_id, name, phone, relation, is_starred, remark, created_at)
SELECT u.id, s.name || ' 柜台', s.contact, '驿站', 1, s.open_hours || ' 营业', now() - interval '30 day'
FROM sys_user u CROSS JOIN station s
WHERE u.id IN (3, 4, 5)
  AND NOT EXISTS (SELECT 1 FROM phone_contact c WHERE c.user_id = u.id AND c.name = s.name || ' 柜台');

INSERT INTO phone_contact (user_id, name, phone, relation, is_starred, remark, created_at)
SELECT u.id,
       (ARRAY['陈辅导员','刘班主任','宿管阿姨'])[abs(hashtext('rel' || u.id)) % 3 + 1],
       '139' || lpad((abs(hashtext('tel' || u.id)) % 100000000)::text, 8, '0'),
       (ARRAY['辅导员','辅导员','宿管'])[abs(hashtext('rel' || u.id)) % 3 + 1],
       0, NULL, now() - interval '25 day'
FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
WHERE abs(hashtext('pc3' || u.id)) % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM phone_contact c WHERE c.user_id = u.id AND c.relation IN ('辅导员','宿管'));-- ---------- 19. 种子：自寄订单 ----------
INSERT INTO ship_order (order_no, user_id, station_id, courier_company, sender_name, sender_phone,
                        receiver_name, receiver_phone, receiver_address, goods_type, weight_kg, freight,
                        insured, tracking_no, status, note, created_at, dropped_at, shipped_at)
SELECT 'SP' || to_char(now() - (g.n * 6 || ' day')::interval, 'YYYYMMDD') || lpad(u.id::text, 4, '0') || g.n,
       u.id,
       abs(hashtext('spst' || u.id || g.n)) % 2 + 1,
       (ARRAY['SF','ZT','YT','JD','STO'])[abs(hashtext('spcc' || u.id || g.n)) % 5 + 1],
       u.real_name, u.phone,
       (ARRAY['王建国','李秀兰','张伟','陈丽华'])[abs(hashtext('sprn' || u.id || g.n)) % 4 + 1],
       '138' || lpad((abs(hashtext('sprp' || u.id || g.n)) % 100000000)::text, 8, '0'),
       (ARRAY['浙江省杭州市西湖区文三路 100 号','江苏省南京市鼓楼区中山北路 32 号','上海市杨浦区四平路 1239 号','安徽省合肥市蜀山区黄山路 443 号'])[abs(hashtext('spra' || u.id || g.n)) % 4 + 1],
       (ARRAY['衣物','书籍','日用品','特产食品','数码配件'])[abs(hashtext('spg' || u.id || g.n)) % 5 + 1],
       w.kg,
       8 + 2 * GREATEST(0, ceil(w.kg - 1)),
       (abs(hashtext('spin' || u.id || g.n)) % 3 = 0)::int,
       CASE WHEN st.s IN ('SHIPPED') THEN 'SF' || lpad((abs(hashtext('sptn' || u.id || g.n)) % 1000000000000)::text, 12, '0') ELSE NULL END,
       st.s,
       CASE WHEN abs(hashtext('spnt' || u.id || g.n)) % 3 = 0 THEN '易碎，请贴防压标签' ELSE NULL END,
       now() - (g.n * 6 || ' day')::interval,
       CASE WHEN st.s <> 'CREATED' THEN now() - (g.n * 6 || ' day')::interval + interval '3 hour' ELSE NULL END,
       CASE WHEN st.s = 'SHIPPED' THEN now() - (g.n * 6 || ' day')::interval + interval '6 hour' ELSE NULL END
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.role_id = 3
CROSS JOIN generate_series(1, 2) AS g(n)
CROSS JOIN LATERAL (SELECT (0.5 + (abs(hashtext('spw' || u.id || g.n)) % 10) * 0.5)::numeric AS kg) w
CROSS JOIN LATERAL (SELECT (ARRAY['CREATED','DROPPED','SHIPPED','SHIPPED'])[abs(hashtext('sps' || u.id || g.n)) % 4 + 1] AS s) st
WHERE abs(hashtext('spown' || u.id)) % 3 = 0 AND g.n <= abs(hashtext('spn' || u.id)) % 2 + 1
ON CONFLICT (order_no) DO NOTHING;

-- 已发出的自寄单运费扣款
INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT o.user_id, 'CONSUME', -o.freight, 0, 'BALANCE', '自寄运费', 'SHIP_ORDER', o.id,
       '订单 ' || o.order_no || ' · ' || to_char(o.weight_kg, 'FM990.0') || ' kg', o.shipped_at
FROM ship_order o
WHERE o.status = 'SHIPPED'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'SHIP_ORDER' AND t.ref_id = o.id AND t.type = 'CONSUME');

-- ---------- 20. 种子：超时保险 ----------
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
WHERE abs(hashtext('ins' || p.id)) % 4 = 0
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
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'INSURANCE_CLAIM' AND t.ref_id = i.id);-- ---------- 21. 种子：客服工单 / 意见反馈 / 广告位 ----------
INSERT INTO service_ticket (ticket_no, user_id, category, title, content, contact, status, is_read, handler_id, handled_at, created_at, updated_at) VALUES
 ('TK20261101001', 3, 'PARCEL', '取件码收不到短信', '包裹显示已到站两天，但一直没收到取件码短信，站内信也没有，麻烦帮忙查一下运单 SF1358820016688 的通知发送记录。', '13900000101', 'DONE', 1, 2, now() - interval '2 day', now() - interval '3 day', now() - interval '2 day'),
 ('TK20261105002', 6, 'PROXY',  '代取订单一直没人接单', '昨晚发布的代取单（送到东区 5 号楼）到现在还是待接单，服务费已经托管了，能不能帮忙催一下或者告诉我怎么取消退款？', '18883655675', 'PROCESSING', 1, 48, now() - interval '6 hour', now() - interval '1 day', now() - interval '6 hour'),
 ('TK20261110003', 10, 'WALLET', '充值赠送金额没到账', '前天用微信渠道充了 100 元，按规则应该送 8 元，但余额只加了 100，麻烦核对一下订单。', '18983748137', 'PENDING', 0, NULL, NULL, now() - interval '8 hour', now() - interval '8 hour'),
 ('TK20261112004', 4, 'SHIP',   '自寄件包装破损咨询', '上周从西区服务中心寄出的书籍包裹，家里收到时外箱破损少了两本，想问下驿站这边有没有出库前的检查记录可以作证。', '13900000102', 'PENDING', 0, NULL, NULL, now() - interval '4 hour', now() - interval '4 hour'),
 ('TK20261114005', 7, 'OTHER',  '建议延长晚间营业时间', '晚课下课后赶过去经常关门了，能不能考试周把东区驿站延长到 22:00？', NULL, 'PENDING', 1, NULL, NULL, now() - interval '30 hour', now() - interval '30 hour')
ON CONFLICT (ticket_no) DO UPDATE SET status = EXCLUDED.status, is_read = EXCLUDED.is_read,
    handler_id = EXCLUDED.handler_id, handled_at = EXCLUDED.handled_at, updated_at = EXCLUDED.updated_at;

INSERT INTO ticket_reply (ticket_id, author_id, author_role, content, created_at)
SELECT t.id, 2, 'STAFF', '已查到该运单短信通道当时运营商网关超时，已补发站内信与短信，取件码同步在包裹详情页可见。给您带来不便非常抱歉。', now() - interval '2 day'
FROM service_ticket t WHERE t.ticket_no = 'TK20261101001'
  AND NOT EXISTS (SELECT 1 FROM ticket_reply r WHERE r.ticket_id = t.id);

INSERT INTO ticket_reply (ticket_id, author_id, author_role, content, created_at)
SELECT t.id, 48, 'STAFF', '已在驿站群帮您转发提醒。若 12 小时内仍无人接单，可在订单页直接取消，托管费用全额退回钱包。', now() - interval '6 hour'
FROM service_ticket t WHERE t.ticket_no = 'TK20261105002'
  AND NOT EXISTS (SELECT 1 FROM ticket_reply r WHERE r.ticket_id = t.id);

-- 工单提交时的自动通知（管理员 + 驿站员）
INSERT INTO notification (user_id, type, channel, title, content, is_read, sent_at)
SELECT staff.id, 'NOTICE', 'IN_APP', '新客服工单待处理',
       '【' || t.ticket_no || '】' || t.title || ' —— 提交人 ' || u.real_name || '，请及时处理。',
       CASE WHEN t.is_read = 1 THEN 1 ELSE 0 END, t.created_at
FROM service_ticket t
JOIN sys_user u ON u.id = t.user_id
JOIN sys_user_role ur ON ur.role_id IN (1, 2)
JOIN sys_user staff ON staff.id = ur.user_id
WHERE NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = staff.id AND n.title = '新客服工单待处理' AND n.content LIKE '%' || t.ticket_no || '%');

INSERT INTO feedback (user_id, type, content, contact, status, reply, replier_id, replied_at, created_at)
SELECT v.user_id, v.type, v.content, v.contact, v.status, v.reply, v.replier_id, v.replied_at, v.created_at
FROM (VALUES
 (3, 'SUGGEST', '希望包裹列表能按承运商筛选，双十一件太多翻起来费劲。', NULL, 'REPLIED', '感谢建议！承运商筛选已排入下个迭代，预计两周内上线。', 1, now() - interval '5 day', now() - interval '6 day'),
 (6, 'PRAISE',  '西区吴阿姨服务特别好，大件主动帮忙搬到推车，点赞！', NULL, 'REPLIED', '已转达表扬并计入月度服务之星评选，谢谢您的认可。', 1, now() - interval '3 day', now() - interval '4 day'),
 (10, 'BUG',    '暗色模式下钱包页流水金额看不太清，对比度有点低。', 'wangyt@stu.campus.edu.cn', 'PENDING', NULL, NULL, NULL, now() - interval '10 hour'),
 (12, 'COMPLAINT', '高峰期取件排队超过 20 分钟，只有一个窗口开放，希望增加人手。', NULL, 'PENDING', NULL, NULL, NULL, now() - interval '2 hour')
) AS v(user_id, type, content, contact, status, reply, replier_id, replied_at, created_at)
WHERE NOT EXISTS (SELECT 1 FROM feedback f WHERE f.user_id = v.user_id AND f.content = v.content);

INSERT INTO ad_slot (slot_key, title, content, link_url, theme, weight, status, start_at, end_at)
SELECT v.slot_key, v.title, v.content, v.link_url, v.theme, v.weight, v.status, v.start_at, v.end_at
FROM (VALUES
 ('HOME_BANNER', '校园寄件节', '自寄首重立减 3 元，新人券叠加更划算', '/student/ship', 'warm', 10, 1, now() - interval '5 day', now() + interval '25 day'),
 ('HOME_BANNER', '超时保险上线', '1 元保 7 天，超时自动理赔 5 元入钱包', '/student/insurance', 'cool', 8, 1, now() - interval '3 day', now() + interval '27 day'),
 ('PICKUP_SIDE', '邀请有礼', '好友充值你拿 10% 返利，上不封顶', '/student/invite', 'warm', 6, 1, NULL::timestamptz, NULL::timestamptz),
 ('WALLET_FOOT', '充值加赠', '充 100 送 8 元 · 充 200 送 20 元', '/student/wallet', 'neutral', 5, 1, NULL::timestamptz, NULL::timestamptz)
) AS v(slot_key, title, content, link_url, theme, weight, status, start_at, end_at)
WHERE NOT EXISTS (SELECT 1 FROM ad_slot a WHERE a.slot_key = v.slot_key AND a.title = v.title);-- ---------- 22. 余额重算（纳入返利/自寄/保险流水，保证 CHECK(balance>=0)） ----------
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

-- ---------- 23. 结果自检 ----------
SELECT 'friendship' AS t, count(*) FROM friendship
UNION ALL SELECT 'invite_bound', count(*) FROM sys_user WHERE invited_by IS NOT NULL
UNION ALL SELECT 'invite_rebate', count(*) FROM invite_rebate
UNION ALL SELECT 'coupon_template', count(*) FROM coupon_template
UNION ALL SELECT 'user_coupon', count(*) FROM user_coupon
UNION ALL SELECT 'redeem_code', count(*) FROM redeem_code
UNION ALL SELECT 'user_address', count(*) FROM user_address
UNION ALL SELECT 'receive_preference', count(*) FROM receive_preference
UNION ALL SELECT 'phone_contact', count(*) FROM phone_contact
UNION ALL SELECT 'ship_order', count(*) FROM ship_order
UNION ALL SELECT 'insurance_policy', count(*) FROM insurance_policy
UNION ALL SELECT 'service_ticket', count(*) FROM service_ticket
UNION ALL SELECT 'feedback', count(*) FROM feedback
UNION ALL SELECT 'ad_slot', count(*) FROM ad_slot
UNION ALL SELECT 'ai_config', count(*) FROM sys_config WHERE config_key LIKE 'ai.%';