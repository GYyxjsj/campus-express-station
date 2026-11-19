-- ============================================================
-- 校园快递驿站 · 功能扩展迁移（幂等，可重复执行）
-- 新增：钱包充值 / 身份条码 / 代取服务 / 包裹去向轨迹 / 政策协议 / 公告弹窗
-- 执行: psql -U artex -d artex -v ON_ERROR_STOP=1 -f 04_features.sql
--       （库内 schema 为 campus_express 时见下方 search_path）
-- ============================================================

BEGIN;
SET search_path TO campus_express, public;

-- ---------- 1. 钱包账户 / 资金流水 / 充值订单 ----------
CREATE TABLE IF NOT EXISTS wallet_account (
    user_id        BIGINT PRIMARY KEY REFERENCES sys_user(id),
    balance        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_recharge NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_spent    NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_earned   NUMERIC(12,2) NOT NULL DEFAULT 0,
    status         SMALLINT      NOT NULL DEFAULT 1,          -- 1正常 0冻结
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0)
);
COMMENT ON TABLE wallet_account IS '用户钱包（余额/累计充值/累计消费/累计收入）';

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES sys_user(id),
    type          VARCHAR(16)   NOT NULL,                     -- RECHARGE/CONSUME/REFUND/EARN/ADJUST
    amount        NUMERIC(12,2) NOT NULL,                     -- 正=入账 负=出账
    balance_after NUMERIC(12,2) NOT NULL,
    channel       VARCHAR(16),                                -- CAMPUS_CARD/ALIPAY/WECHAT/BALANCE/SYSTEM
    title         VARCHAR(128)  NOT NULL,
    ref_type      VARCHAR(24),                                -- RECHARGE_ORDER/PROXY_ORDER/PARCEL/ADMIN
    ref_id        BIGINT,
    remark        VARCHAR(255),
    operator_id   BIGINT,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_wallet_tx_type CHECK (type IN ('RECHARGE','CONSUME','REFUND','EARN','ADJUST'))
);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_user ON wallet_transaction(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_type ON wallet_transaction(type, created_at DESC);

CREATE TABLE IF NOT EXISTS recharge_order (
    id         BIGSERIAL PRIMARY KEY,
    order_no   VARCHAR(32)   NOT NULL,
    user_id    BIGINT        NOT NULL REFERENCES sys_user(id),
    amount     NUMERIC(12,2) NOT NULL,
    bonus      NUMERIC(12,2) NOT NULL DEFAULT 0,
    channel    VARCHAR(16)   NOT NULL,                        -- CAMPUS_CARD/ALIPAY/WECHAT
    status     VARCHAR(16)   NOT NULL DEFAULT 'PENDING',      -- PENDING/PAID/CANCELLED/EXPIRED
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    expire_at  TIMESTAMPTZ   NOT NULL,
    paid_at    TIMESTAMPTZ,
    CONSTRAINT uk_recharge_no UNIQUE (order_no),
    CONSTRAINT ck_recharge_status CHECK (status IN ('PENDING','PAID','CANCELLED','EXPIRED')),
    CONSTRAINT ck_recharge_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_recharge_user ON recharge_order(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recharge_status ON recharge_order(status, expire_at);

-- ---------- 2. 身份条码（Code128 条纹码，柜台核验用） ----------
CREATE TABLE IF NOT EXISTS identity_code (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES sys_user(id),
    code         VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',       -- ACTIVE/REVOKED/EXPIRED
    expire_at    TIMESTAMPTZ NOT NULL,
    use_count    INT         NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at   TIMESTAMPTZ,
    CONSTRAINT uk_identity_code UNIQUE (code),
    CONSTRAINT ck_identity_status CHECK (status IN ('ACTIVE','REVOKED','EXPIRED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_identity_active ON identity_code(user_id) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_identity_expire ON identity_code(status, expire_at);

-- ---------- 3. 代取服务（有偿跑腿单，托管结算） ----------
CREATE TABLE IF NOT EXISTS proxy_order (
    id            BIGSERIAL PRIMARY KEY,
    order_no      VARCHAR(32)   NOT NULL,
    parcel_id     BIGINT        NOT NULL REFERENCES parcel(id),
    requester_id  BIGINT        NOT NULL REFERENCES sys_user(id),
    runner_id     BIGINT        REFERENCES sys_user(id),
    auth_id       BIGINT        REFERENCES proxy_auth(id),
    fee           NUMERIC(10,2) NOT NULL,
    tip           NUMERIC(10,2) NOT NULL DEFAULT 0,
    commission    NUMERIC(10,2) NOT NULL DEFAULT 0,
    status        VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    deliver_to    VARCHAR(255)  NOT NULL,
    note          VARCHAR(255),
    expire_at     TIMESTAMPTZ   NOT NULL,
    rating        SMALLINT,
    review        VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    accepted_at   TIMESTAMPTZ,
    picked_at     TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ,
    done_at       TIMESTAMPTZ,
    cancelled_at  TIMESTAMPTZ,
    cancel_reason VARCHAR(255),
    CONSTRAINT uk_proxy_order_no UNIQUE (order_no),
    CONSTRAINT ck_proxy_order_status CHECK (status IN ('OPEN','ACCEPTED','PICKED','DELIVERED','DONE','CANCELLED','EXPIRED')),
    CONSTRAINT ck_proxy_order_fee CHECK (fee >= 0 AND tip >= 0),
    CONSTRAINT ck_proxy_order_not_self CHECK (runner_id IS NULL OR runner_id <> requester_id)
);
CREATE INDEX IF NOT EXISTS idx_proxy_order_status ON proxy_order(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_proxy_order_requester ON proxy_order(requester_id, status);
CREATE INDEX IF NOT EXISTS idx_proxy_order_runner ON proxy_order(runner_id, status);
CREATE INDEX IF NOT EXISTS idx_proxy_order_parcel ON proxy_order(parcel_id);
COMMENT ON TABLE proxy_order IS '代取服务单 OPEN→ACCEPTED→PICKED→DELIVERED→DONE（托管扣费/完成结算/取消退款）';

-- ---------- 4. 包裹去向轨迹（时间 + 地点 + 检查员） ----------
CREATE TABLE IF NOT EXISTS parcel_trace (
    id            BIGSERIAL PRIMARY KEY,
    parcel_id     BIGINT       NOT NULL REFERENCES parcel(id),
    node          VARCHAR(24)  NOT NULL,
    title         VARCHAR(128) NOT NULL,
    location      VARCHAR(255),
    operator_id   BIGINT,
    operator_name VARCHAR(64),
    operator_role VARCHAR(16),                                -- COURIER/STAFF/ADMIN/STUDENT/SYSTEM
    detail        VARCHAR(512),
    happened_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_trace_parcel ON parcel_trace(parcel_id, happened_at);
CREATE INDEX IF NOT EXISTS idx_trace_time ON parcel_trace(happened_at DESC);
CREATE INDEX IF NOT EXISTS idx_trace_operator ON parcel_trace(operator_id, happened_at DESC);
CREATE INDEX IF NOT EXISTS idx_trace_node ON parcel_trace(node, happened_at DESC);
COMMENT ON TABLE parcel_trace IS '包裹去向台账：每个节点记录时间/地点/检查员（经办人）';

-- ---------- 5. 政策协议 / 签署记录 ----------
CREATE TABLE IF NOT EXISTS policy (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL,
    title        VARCHAR(128) NOT NULL,
    version      VARCHAR(16)  NOT NULL DEFAULT '1.0',
    category     VARCHAR(24)  NOT NULL DEFAULT 'SERVICE',     -- SERVICE/PRIVACY/PROXY/WALLET/CAMPUS
    summary      VARCHAR(255),
    content      TEXT         NOT NULL,
    required     SMALLINT     NOT NULL DEFAULT 0,             -- 1=必须签署后方可使用
    status       SMALLINT     NOT NULL DEFAULT 1,
    effective_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    publisher_id BIGINT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_policy_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS policy_acceptance (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES sys_user(id),
    policy_id   BIGINT      NOT NULL REFERENCES policy(id),
    version     VARCHAR(16) NOT NULL,
    ip          VARCHAR(64),
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_policy_accept UNIQUE (user_id, policy_id, version)
);
CREATE INDEX IF NOT EXISTS idx_policy_accept_user ON policy_acceptance(user_id, accepted_at DESC);

-- ---------- 6. 公告弹窗 ----------
ALTER TABLE notice ADD COLUMN IF NOT EXISTS is_popup SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE notice ADD COLUMN IF NOT EXISTS priority SMALLINT NOT NULL DEFAULT 0;   -- 0普通 1重要 2紧急
ALTER TABLE notice ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'NOTICE';

CREATE TABLE IF NOT EXISTS notice_read (
    user_id   BIGINT      NOT NULL REFERENCES sys_user(id),
    notice_id BIGINT      NOT NULL REFERENCES notice(id),
    read_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, notice_id)
);

-- ---------- 7. 权限点 / 角色授权 ----------
INSERT INTO sys_permission (perm_code, perm_name, module) VALUES
    ('parcel:inspect',  '包裹检查登记', 'parcel'),
    ('trace:manage',    '去向台账管理', 'parcel'),
    ('identity:verify', '身份条码核验', 'auth'),
    ('wallet:manage',   '资金账务管理', 'admin'),
    ('proxy:manage',    '代取服务监管', 'admin'),
    ('policy:manage',   '政策协议管理', 'admin')
ON CONFLICT (perm_code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON
    (r.role_code = 'ADMIN' AND p.perm_code IN ('parcel:inspect','trace:manage','identity:verify','wallet:manage','proxy:manage','policy:manage'))
 OR (r.role_code = 'STAFF' AND p.perm_code IN ('parcel:inspect','trace:manage','identity:verify'))
ON CONFLICT (role_id, perm_id) DO NOTHING;

-- ---------- 8. 业务配置 ----------
INSERT INTO sys_config (config_key, config_value, remark) VALUES
    ('proxy.service.fee.min',          '2',  '代取服务最低服务费(元)'),
    ('proxy.service.fee.max',          '50', '代取服务最高服务费(元)'),
    ('proxy.service.commission.percent','10','平台服务费抽成比例(%)'),
    ('proxy.service.max.hours',        '48', '代取服务单最长有效期(小时)'),
    ('wallet.recharge.min',            '1',  '单笔充值下限(元)'),
    ('wallet.recharge.max',            '500','单笔充值上限(元)'),
    ('identity.code.valid.hours',      '168','身份条码有效期(小时)'),
    ('notice.popup.enabled',           '1',  '公告弹窗总开关')
ON CONFLICT (config_key) DO NOTHING;

-- ---------- 9. 钱包开户 +  believable 历史流水 ----------
INSERT INTO wallet_account (user_id, balance, total_recharge, total_spent, total_earned)
SELECT u.id, 0, 0, 0, 0 FROM sys_user u
ON CONFLICT (user_id) DO NOTHING;

-- 学生充值历史（确定性伪随机，保证可重复执行结果一致）
WITH students AS (
    SELECT u.id, ur.role_id FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.id
    JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'STUDENT'
), seed AS (
    SELECT s.id,
           (abs(hashtext('recharge' || s.id)) % 4 + 1) AS times,
           abs(hashtext('chan' || s.id)) % 3 AS chan
    FROM students s
)
INSERT INTO recharge_order (order_no, user_id, amount, bonus, channel, status, created_at, expire_at, paid_at)
SELECT 'RC' || to_char(now() - (g.n * 7 || ' day')::interval, 'YYYYMMDD') || lpad(seed.id::text, 4, '0') || g.n,
       seed.id,
       amt.amount,
       CASE WHEN amt.amount >= 200 THEN 20 WHEN amt.amount >= 100 THEN 8 WHEN amt.amount >= 50 THEN 3 ELSE 0 END,
       (ARRAY['CAMPUS_CARD','ALIPAY','WECHAT'])[seed.chan + 1],
       'PAID',
       now() - (g.n * 7 || ' day')::interval,
       now() - (g.n * 7 || ' day')::interval + interval '30 minutes',
       now() - (g.n * 7 || ' day')::interval + interval '2 minutes'
FROM seed
CROSS JOIN generate_series(1, 4) AS g(n)
CROSS JOIN LATERAL (SELECT (ARRAY[20,50,100,200])[abs(hashtext('amt' || seed.id || g.n)) % 4 + 1]::numeric AS amount) amt
WHERE g.n <= seed.times
ON CONFLICT (order_no) DO NOTHING;

-- 充值入账流水
INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, created_at)
SELECT o.user_id, 'RECHARGE', o.amount + o.bonus, 0, o.channel,
       '账户充值' || CASE WHEN o.bonus > 0 THEN '（含赠送 ' || to_char(o.bonus, 'FM999990.00') || ' 元）' ELSE '' END,
       'RECHARGE_ORDER', o.id, o.paid_at
FROM recharge_order o
WHERE o.status = 'PAID'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'RECHARGE_ORDER' AND t.ref_id = o.id AND t.type = 'RECHARGE');

-- 历史消费（保管费 / 代取服务费）
INSERT INTO wallet_transaction (user_id, type, amount, balance_after, channel, title, ref_type, ref_id, remark, created_at)
SELECT p.owner_id, 'CONSUME', -fee.amount, 0, 'BALANCE', fee.title, 'PARCEL', p.id, fee.remark,
       p.outbound_time + interval '1 minute'
FROM parcel p
CROSS JOIN LATERAL (
    SELECT CASE WHEN p.status = 'RETURNED' THEN 3.00 ELSE 2.00 END::numeric AS amount,
           CASE WHEN p.status = 'RETURNED' THEN '超期保管费' ELSE '驿站服务费' END AS title,
           CASE WHEN p.status = 'RETURNED' THEN '滞留超 72 小时按驿站公约计收' ELSE '入库通知与货架保管服务' END AS remark
) fee
WHERE p.outbound_time IS NOT NULL
  AND abs(hashtext('fee' || p.id)) % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.ref_type = 'PARCEL' AND t.ref_id = p.id AND t.type = 'CONSUME');

-- 汇总余额（流水按时间顺序累加，余额不足时由系统赠送兜底，保证 CHECK(balance>=0)）
WITH ordered AS (
    SELECT t.id, t.user_id, t.amount,
           sum(t.amount) OVER (PARTITION BY t.user_id ORDER BY t.created_at, t.id) AS running
    FROM wallet_transaction t
), fixed AS (
    SELECT o.id, o.user_id, o.amount,
           o.running + GREATEST(0, COALESCE((SELECT min(running) FROM ordered x WHERE x.user_id = o.user_id), 0) * -1) + 30 AS balance_after
    FROM ordered o
)
UPDATE wallet_transaction t SET balance_after = f.balance_after FROM fixed f WHERE t.id = f.id;

UPDATE wallet_account w SET
    balance = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id), 0) + 30,
    total_recharge = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.type = 'RECHARGE'), 0),
    total_spent = COALESCE((SELECT -sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.amount < 0), 0),
    total_earned = COALESCE((SELECT sum(t.amount) FROM wallet_transaction t WHERE t.user_id = w.user_id AND t.type = 'EARN'), 0),
    updated_at = now();

-- 演示账号待支付订单（便于直接体验支付流程）
INSERT INTO recharge_order (order_no, user_id, amount, bonus, channel, status, created_at, expire_at)
SELECT 'RC' || to_char(now(), 'YYYYMMDD') || '0003DEMO', 3, 50, 3, 'ALIPAY', 'PENDING', now(), now() + interval '30 minutes'
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 3)
ON CONFLICT (order_no) DO NOTHING;

-- ---------- 10. 政策协议正文 ----------
INSERT INTO policy (code, title, version, category, summary, content, required, status, effective_at, publisher_id)
VALUES
('SERVICE_TERMS', '校园快递驿站服务协议', '2.1', 'SERVICE',
 '约定驿站代收、保管、取件核销与责任边界，是使用本系统的前提条款。',
 E'# 一、协议的接受与生效\n1.1 本协议由校园快递驿站（以下简称"驿站"）与使用本系统的师生用户（以下简称"用户"）共同缔结。用户完成注册、登录或实际使用代收、代取、钱包等任一服务，即视为已阅读并同意本协议全部条款。\n1.2 驿站有权根据运营需要修订本协议，修订后将在系统内公告并更新版本号。涉及用户重大权益的变更，需用户重新签署确认后方可继续使用相关功能。\n\n# 二、服务内容\n2.1 包裹代收与入库登记：驿站接收承运商送达的包裹，登记运单号、承运商、收件手机号，分配货架并生成 6 位取件码。\n2.2 取件提醒：包裹入库后通过站内信、短信、邮件三渠道发送到站通知；在库超过首次催领阈值自动催领，超过滞留阈值升级为滞留件并通知物主与管理员。\n2.3 取件核销：本人取件凭包裹编号与取件码；代取凭授权码与取件码双因子，由驿站员经办并全程留痕。\n2.4 代取服务：用户可发布有偿代取需求，由其他在校学生接单完成，费用经钱包托管并在确认收货后结算。\n2.5 去向追溯：包裹自到站至出库的每个节点均记录时间、地点与检查员（经办人），用户可随时查询。\n\n# 三、用户义务\n3.1 用户须以真实学号/工号与本人手机号注册，手机号须与包裹收件手机号一致，否则无法匹配入库。\n3.2 取件码、授权码、身份条码属于用户身份凭证，不得转借、截图外传或用于非本人包裹的核销。\n3.3 用户应在包裹到站后及时领取。在库超过 72 小时的包裹按滞留件处理，驿站有权按公约计收保管费或登记退回。\n3.4 禁止利用本系统寄递法律法规禁止流通的物品，禁止虚假发布代取需求、恶意接单不履约或刷单套取补贴。\n\n# 四、驿站义务与责任边界\n4.1 驿站应妥善保管在库包裹，保持货架容量与占用数据真实，不得无故丢弃、拆封用户包裹。\n4.2 因驿站保管不善造成包裹损毁、丢失的，驿站按实际损失协商赔偿；因承运商原因、不可抗力或用户自身泄露凭证造成的损失，驿站不承担赔偿责任。\n4.3 外包装完好但内件短少、破损的，由用户与承运商依运输合同处理，驿站协助提供入库检查记录与去向轨迹作为证据。\n\n# 五、违约与处置\n5.1 用户违反本协议的，驿站可视情节采取警告、限制代取服务、冻结钱包、停用账号等措施，并保留追究法律责任的权利。\n5.2 用户对处置有异议的，可在 7 日内通过驿站联系方式申诉，驿站应在 3 个工作日内答复。\n\n# 六、其他\n6.1 本协议适用中华人民共和国法律。因本协议产生的争议，双方应友好协商；协商不成的，提交驿站所在地有管辖权的人民法院解决。\n6.2 本协议部分条款无效不影响其余条款效力。协议最终解释权归驿站运营方所有。',
 1, 1, now() - interval '20 days', 1),

('PRIVACY', '个人信息与隐私保护政策', '1.4', 'PRIVACY',
 '说明收集哪些信息、用于何种目的、如何脱敏与留存，以及用户的查询更正权利。',
 E'# 一、我们收集的信息\n1.1 注册信息：学号/工号、姓名、手机号、邮箱（选填）、登录密码（BCrypt 不可逆加密存储）。\n1.2 业务信息：包裹运单号、承运商、收件手机号、货架位置、取件码、授权码、身份条码、代取服务单与收货地址。\n1.3 资金信息：充值金额、支付渠道、余额与资金流水。本系统不采集银行卡号、支付密码等敏感金融信息，充值为校园场景模拟支付。\n1.4 日志信息：操作时间、操作人、模块、动作、对象 ID、IP 地址与操作结果，用于安全审计。\n\n# 二、信息的使用\n2.1 用于包裹匹配入库、发送取件提醒、核销身份校验、代取服务撮合与结算、去向追溯与争议处理。\n2.2 用于运营统计（出入库趋势、状态分布、承运商占比），统计结果以聚合形式呈现，不指向特定个人。\n2.3 未经用户同意，不将个人信息用于商业营销，不向第三方出售个人信息。\n\n# 三、信息的共享与披露\n3.1 代取场景下，向被授权人/接单人展示物主姓名、学号、包裹运单号与收货地址，此为完成代取所必需的最小范围。\n3.2 依法律法规、司法或行政机关强制性要求披露的，我们将在合规范围内提供，并尽可能事先告知用户。\n\n# 四、脱敏与留存\n4.1 操作日志对密码、取件码、授权码等敏感字段自动脱敏后留存。\n4.2 学生端仅可查看本人包裹与本人资金流水；驿站员与管理员按权限点访问，越权访问将被拦截并记录。\n4.3 业务数据留存期为在校期间及毕业后 2 年；操作日志留存 180 天；到期后匿名化或删除。\n\n# 五、用户权利\n5.1 用户可在个人中心查询、更正本人姓名、手机号、邮箱，并可修改登录密码。\n5.2 用户可查询本人全部资金流水、代取记录与协议签署记录，并可撤销生效中的代取授权、吊销身份条码。\n5.3 用户可申请注销账号；注销后钱包余额按原路退回，未出库包裹须先完成领取或退回。\n\n# 六、安全措施\n6.1 传输层使用 HTTPS，认证使用 JWT（HS256，2 小时有效期），权限采用注解式 RBAC 校验。\n6.2 身份条码与授权码均设有效期与一次性使用约束，核销采用带前置状态的条件更新，防止并发重复核销。\n6.3 如发生个人信息安全事件，我们将立即启动应急处置，并按要求告知用户与主管部门。',
 1, 1, now() - interval '20 days', 1),

('PROXY_RULES', '代取服务与授权规则', '1.2', 'PROXY',
 '免费代取授权与有偿代取服务的凭证规则、费用托管、抽成、履约与争议处理。',
 E'# 一、两种代取方式\n1.1 免费代取授权：物主指定一名在校学生，系统生成 8 位一次性授权码，被授权人凭授权码 + 取件码到柜台核销，不产生费用。\n1.2 有偿代取服务：物主发布代取需求并设定服务费与小费，任意符合条件的在校学生可接单，完成后由平台结算给接单人。\n\n# 二、发布与接单\n2.1 发布条件：包裹处于在库待取或滞留超期状态、属于本人、且无生效中的代取授权；钱包余额足以支付服务费与小费。\n2.2 服务费区间由驿站配置（默认 2–50 元），有效期最长 48 小时。发布时费用即从钱包扣除并托管。\n2.3 接单人须为启用状态的学生账号，不得接自己发布的单。接单后系统自动生成代取授权，接单人凭授权码与取件码取件。\n2.4 取件码由物主在订单详情中查看并线下告知接单人，平台不向接单人直接展示取件码。\n\n# 三、履约与结算\n3.1 订单状态流转：待接单 → 已接单 → 已取件 → 已送达 → 已完成。\n3.2 接单人送达后点击"确认送达"，物主确认收货后订单完成，平台按配置比例（默认 10%）抽取服务费，余额结算至接单人钱包。\n3.3 物主可对本次服务评分（1–5 星）并填写评价，评分公开展示于接单人服务档案。\n\n# 四、取消、放弃与退款\n4.1 待接单状态下物主可随时取消，托管费用全额退回钱包。\n4.2 已接单但未取件时，接单人可放弃订单，订单回到待接单；物主取消的，托管费用全额退回。\n4.3 已取件后不支持自助取消，如有争议由驿站管理员介入处理，可强制取消并按责任判定退款。\n4.4 超过有效期仍无人接单的订单自动过期，托管费用全额退回。\n\n# 五、违规处理\n5.1 接单后无故不履约、虚假送达、索要额外费用、私自拆封包裹的，平台可冻结其接单资格并扣减信用分。\n5.2 恶意发布虚假需求套取补贴的，取消订单、追回资金并停用账号。\n5.3 因代取产生的包裹纠纷，以去向轨迹中的时间、地点与检查员记录为处理依据。',
 1, 1, now() - interval '12 days', 1),

('WALLET_RULES', '钱包充值与资金结算规则', '1.1', 'WALLET',
 '充值渠道、赠送规则、余额用途、退款路径与账务对账说明。',
 E'# 一、账户与余额\n1.1 每位注册用户自动开立钱包账户，余额以人民币元计价，精确到分。\n1.2 余额可用于支付代取服务费、小费、超期保管费与驿站增值服务，不可提现至他人账户。\n\n# 二、充值\n2.1 支持校园卡、支付宝、微信三种渠道（本系统为校园场景模拟支付，不产生真实资金划转）。\n2.2 单笔充值下限 1 元、上限 500 元。充值订单 30 分钟内未支付自动过期。\n2.3 赠送规则：充 50 送 3 元、充 100 送 8 元、充 200 送 20 元、充 500 送 60 元。赠送金额与本金一并入账，消费时优先扣减赠送部分。\n2.4 充值成功后即时入账并生成资金流水，可在钱包页查询与导出。\n\n# 三、消费与托管\n3.1 发布代取服务时，服务费与小费从余额扣除并托管；订单完成结算给接单人，取消或过期则全额退回。\n3.2 滞留超期包裹按驿站公约计收保管费，退回登记时一并结算。\n3.3 余额不足时相关操作将被拒绝，请先充值。\n\n# 四、收入与结算\n4.1 接单人完成代取服务后获得服务费与小费，扣除平台服务费（默认 10%）后入账。\n4.2 结算记录在资金流水中标记为"代取服务收入"，可关联查看对应订单。\n\n# 五、退款与对账\n5.1 充值退款按原渠道退回，模拟支付场景下即时冲正并生成 ADJUST 流水。\n5.2 管理员可在资金账务页查看全量流水、充值订单与平台抽成汇总，并对异常账户执行冻结/解冻与人工调账（须填写原因，全程审计）。\n5.3 用户对流水有异议的，可在 7 日内申诉，平台以订单与去向轨迹记录为准核查。',
 0, 1, now() - interval '12 days', 1),

('CAMPUS_CODE', '驿站取件公约与保管规范', '1.0', 'CAMPUS',
 '取件时限、货架秩序、保管费标准、文明取件与安全检查要求。',
 E'# 一、取件时限\n1.1 包裹到站后请在 24 小时内领取；超过 24 小时系统将自动催领，超过 72 小时升级为滞留件。\n1.2 滞留件每日催领不超过 3 次；连续滞留超过 7 日的包裹，驿站有权登记退回承运商。\n\n# 二、保管费\n2.1 在库 72 小时内免费保管。\n2.2 超过 72 小时按每日 1 元计收保管费，单件封顶 3 元，退回或取件时结算。\n2.3 因驿站原因（如货架错放、通知失败）导致超期的，免收保管费。\n\n# 三、取件秩序\n3.1 请凭取件码或身份条码在柜台核销，不得自行翻找货架。\n3.2 大件、易碎件请当场验视外包装，如有破损立即向经办驿站员提出，由其在去向轨迹中登记检查记录。\n3.3 高峰时段（11:30–13:00、17:00–19:00）请排队取件，建议使用代取服务错峰。\n\n# 四、安全检查\n4.1 驿站员每日对在库包裹执行外观检查与货架盘点，检查结果记入去向轨迹，包含时间、地点与检查员。\n4.2 发现包装破损、渗漏、异味的包裹，立即隔离并通知物主与管理员。\n4.3 严禁在驿站内拆封、试吃、试用他人包裹。\n\n# 五、文明与责任\n5.1 请爱护货架与扫码设备，取件后随手关闭柜门。\n5.2 冒领他人包裹的，移交学校保卫部门处理并永久停用账号。\n5.3 本公约自发布之日起施行，由驿站运营方负责解释与修订。',
 0, 1, now() - interval '30 days', 1)
ON CONFLICT (code) DO NOTHING;

-- 全员签署历史协议（老用户视为已接受当前必备版本）
INSERT INTO policy_acceptance (user_id, policy_id, version, ip, accepted_at)
SELECT u.id, p.id, p.version, '127.0.0.1', u.created_at + interval '1 minute'
FROM sys_user u CROSS JOIN policy p
WHERE p.required = 1 AND p.status = 1
ON CONFLICT (user_id, policy_id, version) DO NOTHING;

-- ---------- 11. 公告（含弹窗） ----------
UPDATE notice SET is_popup = 1, priority = 2, category = 'OPERATION'
WHERE title = '双十一期间驿站营业时间调整';

UPDATE notice SET is_popup = 0, priority = 1, category = 'RULE'
WHERE title = '关于规范代取授权的通知';

INSERT INTO notice (title, content, status, publisher_id, is_popup, priority, category, created_at)
SELECT '代取服务与钱包充值正式上线',
       E'同学们好：\n\n驿站即日起上线「有偿代取服务」与「钱包充值」两项能力。\n\n1. 代取服务：在「代取服务」页发布需求并设定服务费，其他同学接单后凭授权码到柜台取件并送达，确认收货后自动结算。\n2. 钱包充值：支持校园卡 / 支付宝 / 微信（校园模拟支付），充 50 送 3、充 100 送 8、充 200 送 20。\n3. 身份条码：在「我的身份码」生成 Code128 条纹码，柜台扫码即可核验身份，无需口述学号。\n4. 去向追溯：每件包裹的到站、上架、检查、出库节点均记录时间、地点与检查员，可在「包裹去向」查询。\n\n请在使用前阅读《代取服务与授权规则》《钱包充值与资金结算规则》。',
       1, 1, 1, 1, 'FEATURE', now() - interval '2 hours'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '代取服务与钱包充值正式上线');

INSERT INTO notice (title, content, status, publisher_id, is_popup, priority, category, created_at)
SELECT '关于包裹去向记录与检查员留痕的说明',
       E'为保障包裹安全、便于纠纷追溯，系统已启用全链路去向留痕：\n\n· 到站签收、上架、日常检查、催领、滞留升级、授权、核销出库、退回登记均生成轨迹节点；\n· 每个节点记录发生时间、所在地点（驿站 + 货架）与检查员（经办人）姓名及角色；\n· 学生端仅可查看本人包裹轨迹，驿站端可登记检查记录并查询去向台账。\n\n如对某条记录有异议，请携带运单号到柜台申诉，我们将调取原始留痕核查。',
       1, 1, 0, 0, 'RULE', now() - interval '1 day'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '关于包裹去向记录与检查员留痕的说明');

INSERT INTO notice (title, content, status, publisher_id, is_popup, priority, category, created_at)
SELECT '期末季驿站高峰取件指引',
       E'期末季包裹量上升，为避免拥堵：\n\n1. 建议错峰取件，避开 11:30–13:00 与 17:00–19:00；\n2. 可使用代取服务委托同学错峰领取；\n3. 大件包裹请提前联系驿站预留取件通道；\n4. 滞留超 72 小时将按公约计收保管费，请及时领取。',
       1, 1, 1, 1, 'OPERATION', now() - interval '3 hours'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '期末季驿站高峰取件指引');

-- ---------- 12. 代取服务单种子（覆盖各状态，关联真实授权） ----------
-- 待接单：在库且无生效授权的包裹
INSERT INTO proxy_order (order_no, parcel_id, requester_id, fee, tip, status, deliver_to, note, expire_at, created_at)
SELECT 'PO' || to_char(p.inbound_time, 'YYYYMMDD') || lpad(p.id::text, 5, '0'),
       p.id, p.owner_id,
       (ARRAY[3,4,5,6,8])[abs(hashtext('fee' || p.id)) % 5 + 1]::numeric,
       (ARRAY[0,0,1,2])[abs(hashtext('tip' || p.id)) % 4 + 1]::numeric,
       'OPEN',
       (ARRAY['东区学生公寓3号楼 512 室','西区食堂北侧平房 驿站前台','图书馆二楼自习区','东区学生公寓7号楼 308 室'])[abs(hashtext('addr' || p.id)) % 4 + 1],
       (ARRAY['麻烦轻拿轻放，易碎品','放到宿舍门口即可，谢谢','下午六点前送到','可以顺路带一杯奶茶吗'])[abs(hashtext('note' || p.id)) % 4 + 1],
       now() + interval '20 hours',
       p.inbound_time + interval '2 hours'
FROM parcel p
WHERE p.status IN ('IN_STOCK','OVERDUE')
  AND NOT EXISTS (SELECT 1 FROM proxy_auth a WHERE a.parcel_id = p.id AND a.status = 'ACTIVE')
  AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.parcel_id = p.id)
  AND abs(hashtext('open' || p.id)) % 3 = 0
ON CONFLICT (order_no) DO NOTHING;

-- 已完成：已出库且存在 USED 授权的包裹
INSERT INTO proxy_order (order_no, parcel_id, requester_id, runner_id, auth_id, fee, tip, commission, status,
                         deliver_to, note, expire_at, created_at, accepted_at, picked_at, delivered_at, done_at, rating, review)
SELECT 'PO' || to_char(a.created_at, 'YYYYMMDD') || lpad(a.id::text, 5, '0') || 'D',
       a.parcel_id, a.grantor_id, a.grantee_id, a.id,
       (ARRAY[3,5,6,8])[abs(hashtext('dfee' || a.id)) % 4 + 1]::numeric,
       (ARRAY[0,1,2])[abs(hashtext('dtip' || a.id)) % 3 + 1]::numeric,
       round((ARRAY[3,5,6,8])[abs(hashtext('dfee' || a.id)) % 4 + 1] * 0.1, 2),
       'DONE',
       (ARRAY['东区学生公寓3号楼 512 室','西区食堂北侧平房 驿站前台','图书馆二楼自习区'])[abs(hashtext('daddr' || a.id)) % 3 + 1],
       '已完成的历史代取服务',
       a.expire_at, a.created_at, a.created_at + interval '20 minutes',
       pr.picked_at, pr.picked_at + interval '40 minutes', pr.picked_at + interval '55 minutes',
       (ARRAY[5,5,4,5,3])[abs(hashtext('rate' || a.id)) % 5 + 1],
       (ARRAY['速度很快，包装完好，感谢！','准时送到，态度很好','稍微晚了一点，但整体不错','非常靠谱，下次还找你'])[abs(hashtext('rev' || a.id)) % 4 + 1]
FROM proxy_auth a
JOIN pickup_record pr ON pr.auth_id = a.id
WHERE a.status = 'USED'
  AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.auth_id = a.id)
ON CONFLICT (order_no) DO NOTHING;

-- 已接单 / 已取件 / 已送达：为在库包裹生成真实生效授权后挂单
DO $$
DECLARE
    r RECORD;
    v_auth_id BIGINT;
    v_order_id BIGINT;
    v_code VARCHAR(8);
    v_status TEXT;
    v_fee NUMERIC;
BEGIN
    FOR r IN
        SELECT p.id AS parcel_id, p.owner_id, p.station_id, p.inbound_time,
               (SELECT u.id FROM sys_user u
                 JOIN sys_user_role ur ON ur.user_id = u.id
                 JOIN sys_role ro ON ro.id = ur.role_id AND ro.role_code = 'STUDENT'
                WHERE u.status = 1 AND u.id <> p.owner_id
                ORDER BY abs(hashtext('runner' || p.id || u.id)) LIMIT 1) AS runner_id,
               (ARRAY['ACCEPTED','PICKED','DELIVERED'])[abs(hashtext('st' || p.id)) % 3 + 1] AS st
        FROM parcel p
        WHERE p.status IN ('IN_STOCK','OVERDUE')
          AND NOT EXISTS (SELECT 1 FROM proxy_auth a WHERE a.parcel_id = p.id AND a.status = 'ACTIVE')
          AND NOT EXISTS (SELECT 1 FROM proxy_order o WHERE o.parcel_id = p.id)
          AND abs(hashtext('run' || p.id)) % 4 = 0
        LIMIT 6
    LOOP
        v_fee := (ARRAY[4,5,6,8])[abs(hashtext('rfee' || r.parcel_id)) % 4 + 1];
        v_code := upper(substr(md5(random()::text || r.parcel_id), 1, 8));
        INSERT INTO proxy_auth (parcel_id, grantor_id, grantee_id, auth_code, status, expire_at, remark, created_at)
        VALUES (r.parcel_id, r.owner_id, r.runner_id, v_code, 'ACTIVE',
                now() + interval '24 hours', '代取服务自动授权', now() - interval '3 hours')
        RETURNING id INTO v_auth_id;

        INSERT INTO proxy_order (order_no, parcel_id, requester_id, runner_id, auth_id, fee, tip, status,
                                 deliver_to, note, expire_at, created_at, accepted_at, picked_at, delivered_at)
        VALUES ('PO' || to_char(now(), 'YYYYMMDD') || lpad(r.parcel_id::text, 5, '0') || 'R',
                r.parcel_id, r.owner_id, r.runner_id, v_auth_id, v_fee, 1, r.st,
                '东区学生公寓' || (abs(hashtext('bld' || r.parcel_id)) % 8 + 1) || '号楼 ' || (abs(hashtext('room' || r.parcel_id)) % 600 + 101) || ' 室',
                '接单后请尽快到柜台取件', now() + interval '20 hours',
                now() - interval '3 hours', now() - interval '2 hours',
                CASE WHEN r.st IN ('PICKED','DELIVERED') THEN now() - interval '1 hour' ELSE NULL END,
                CASE WHEN r.st = 'DELIVERED' THEN now() - interval '20 minutes' ELSE NULL END)
        RETURNING id INTO v_order_id;

        IF r.st IN ('PICKED','DELIVERED') THEN
            UPDATE parcel SET status = 'PICKED_UP', outbound_time = now() - interval '1 hour', updated_at = now()
            WHERE id = r.parcel_id;
            UPDATE proxy_auth SET status = 'USED', used_at = now() - interval '1 hour' WHERE id = v_auth_id;
            INSERT INTO pickup_record (parcel_id, pickup_type, owner_id, picker_id, auth_id, staff_id, station_id, picked_at)
            VALUES (r.parcel_id, 'PROXY', r.owner_id, r.runner_id, v_auth_id, 2, r.station_id, now() - interval '1 hour')
            ON CONFLICT (parcel_id) DO NOTHING;
        END IF;
    END LOOP;
END $$;

-- 代取服务资金流水（托管扣费 / 取消退款 / 完成结算）
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

-- 重新汇总余额（含代取流水）
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

-- ---------- 13. 去向轨迹回填（时间 / 地点 / 检查员） ----------
-- 到站签收
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT p.id, 'ARRIVE', '承运商送达驿站', s.name || ' 收货台', NULL,
       CASE p.courier_company WHEN 'SF' THEN '顺丰速运' WHEN 'ZT' THEN '中通快递' WHEN 'YT' THEN '圆通速递'
            WHEN 'YD' THEN '韵达快递' WHEN 'STO' THEN '申通快递' WHEN 'JT' THEN '极兔速递'
            WHEN 'JD' THEN '京东物流' WHEN 'EMS' THEN '邮政EMS' ELSE p.courier_company END || ' 配送员',
       'COURIER', '运单 ' || p.tracking_no || ' 到达驿站，外包装完好，签收待入库', p.inbound_time - interval '20 minutes'
FROM parcel p JOIN station s ON s.id = p.station_id
WHERE NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'ARRIVE');

-- 入库上架（检查员 = 经办驿站员）
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT p.id, 'SHELVE', '入库登记并上架', s.name || ' · ' || COALESCE(sh.code, '待分配货架'),
       st.id, st.real_name, 'STAFF',
       '核对收件手机号 ' || left(u.phone, 3) || '****' || right(u.phone, 4) || '，生成取件码并发送三渠道到站通知',
       p.inbound_time
FROM parcel p
JOIN station s ON s.id = p.station_id
LEFT JOIN shelf sh ON sh.id = p.shelf_id
JOIN sys_user u ON u.id = p.owner_id
JOIN sys_user st ON st.id = CASE WHEN p.station_id = 1 THEN 2 ELSE 48 END
WHERE NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'SHELVE');

-- 日常检查（部分在库/滞留包裹）
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT p.id, 'INSPECT', '在库外观检查与货架盘点', s.name || ' · ' || COALESCE(sh.code, '货架'),
       st.id, st.real_name, 'STAFF',
       (ARRAY['外包装完好，无破损渗漏，标签清晰','外箱轻微压痕，已拍照留存并通知物主','胶带松动，已加固并复核重量','货架盘点核对一致，占用数已更新'])[abs(hashtext('ins' || p.id)) % 4 + 1],
       p.inbound_time + interval '18 hours'
FROM parcel p
JOIN station s ON s.id = p.station_id
LEFT JOIN shelf sh ON sh.id = p.shelf_id
JOIN sys_user st ON st.id = CASE WHEN p.station_id = 1 THEN 2 ELSE 48 END
WHERE p.status IN ('IN_STOCK','OVERDUE') AND abs(hashtext('insp' || p.id)) % 2 = 0
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'INSPECT');

-- 催领 / 滞留升级（来自真实提醒记录）
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT n.parcel_id, 'REMIND', '系统自动催领', s.name, NULL, '系统定时任务', 'SYSTEM',
       '在库超过首次催领阈值，已通过站内信/短信/邮件提醒物主', n.sent_at
FROM notification n JOIN parcel p ON p.id = n.parcel_id JOIN station s ON s.id = p.station_id
WHERE n.type = 'REMIND' AND n.channel = 'IN_APP'
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = n.parcel_id AND t.node = 'REMIND' AND t.happened_at = n.sent_at);

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT p.id, 'OVERDUE', '滞留超期升级', s.name || ' · ' || COALESCE(sh.code, '货架'), NULL, '系统定时任务', 'SYSTEM',
       '在库超过滞留阈值，状态升级为滞留超期，已通知物主与全部管理员', p.updated_at
FROM parcel p JOIN station s ON s.id = p.station_id LEFT JOIN shelf sh ON sh.id = p.shelf_id
WHERE p.status IN ('OVERDUE','RETURNED')
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'OVERDUE');

-- 授权发起 / 撤销 / 使用
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT a.parcel_id, 'AUTH_GRANTED', '物主发起代取授权', s.name, a.grantor_id, g.real_name, 'STUDENT',
       '被授权人 ' || ge.real_name || '（' || ge.username || '），授权码 ' || left(a.auth_code, 2) || '****' || right(a.auth_code, 2),
       a.created_at
FROM proxy_auth a JOIN parcel p ON p.id = a.parcel_id JOIN station s ON s.id = p.station_id
JOIN sys_user g ON g.id = a.grantor_id JOIN sys_user ge ON ge.id = a.grantee_id
WHERE NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = a.parcel_id AND t.node = 'AUTH_GRANTED' AND t.happened_at = a.created_at);

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT a.parcel_id, 'AUTH_REVOKED', '代取授权失效', s.name, a.grantor_id, g.real_name, 'STUDENT',
       CASE a.status WHEN 'REVOKED' THEN '物主主动撤销授权，授权码即时失效' ELSE '授权超过有效期自动过期' END,
       COALESCE(a.revoked_at, a.expire_at)
FROM proxy_auth a JOIN parcel p ON p.id = a.parcel_id JOIN station s ON s.id = p.station_id
JOIN sys_user g ON g.id = a.grantor_id
WHERE a.status IN ('REVOKED','EXPIRED')
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = a.parcel_id AND t.node = 'AUTH_REVOKED');

-- 核销出库（检查员 = 经办驿站员）
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT pr.parcel_id, 'PICKUP',
       CASE pr.pickup_type WHEN 'PROXY' THEN '代取核销出库' ELSE '本人取件核销出库' END,
       s.name || ' 核销台 · ' || COALESCE(sh.code, '货架'),
       pr.staff_id, st.real_name, 'STAFF',
       '取件人 ' || pk.real_name || '（' || pk.username || '）' ||
       CASE pr.pickup_type WHEN 'PROXY' THEN '，授权码 + 取件码双因子校验通过' ELSE '，取件码校验通过' END ||
       '，检查员 ' || st.real_name || ' 复核外包装后放行',
       pr.picked_at
FROM pickup_record pr
JOIN parcel p ON p.id = pr.parcel_id
JOIN station s ON s.id = pr.station_id
LEFT JOIN shelf sh ON sh.id = p.shelf_id
JOIN sys_user st ON st.id = pr.staff_id
JOIN sys_user pk ON pk.id = pr.picker_id
WHERE NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = pr.parcel_id AND t.node = 'PICKUP');

-- 退回登记
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT p.id, 'RETURN', '滞留件退回承运商', s.name || ' 退货区',
       CASE WHEN p.station_id = 1 THEN 2 ELSE 48 END,
       CASE WHEN p.station_id = 1 THEN '周立群' ELSE '驿站员' END, 'STAFF',
       COALESCE(p.return_reason, '超期未取退回快递公司'), p.outbound_time
FROM parcel p JOIN station s ON s.id = p.station_id
WHERE p.status = 'RETURNED' AND p.outbound_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = p.id AND t.node = 'RETURN');

-- 代取服务节点
INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT o.parcel_id, 'SERVICE_OPEN', '发布有偿代取服务', s.name, o.requester_id, u.real_name, 'STUDENT',
       '服务费 ' || to_char(o.fee, 'FM999990.00') || ' 元，送达 ' || o.deliver_to, o.created_at
FROM proxy_order o JOIN parcel p ON p.id = o.parcel_id JOIN station s ON s.id = p.station_id
JOIN sys_user u ON u.id = o.requester_id
WHERE NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = o.parcel_id AND t.node = 'SERVICE_OPEN');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT o.parcel_id, 'SERVICE_ACCEPT', '代取服务接单', s.name, o.runner_id, u.real_name, 'STUDENT',
       '接单人 ' || u.real_name || '（' || u.username || '），系统已生成一次性代取授权', o.accepted_at
FROM proxy_order o JOIN parcel p ON p.id = o.parcel_id JOIN station s ON s.id = p.station_id
JOIN sys_user u ON u.id = o.runner_id
WHERE o.accepted_at IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = o.parcel_id AND t.node = 'SERVICE_ACCEPT');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT o.parcel_id, 'SERVICE_DELIVERED', '代取送达待确认', o.deliver_to, o.runner_id, u.real_name, 'STUDENT',
       '接单人已送达指定地址，等待物主确认收货后结算', o.delivered_at
FROM proxy_order o JOIN sys_user u ON u.id = o.runner_id
WHERE o.delivered_at IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = o.parcel_id AND t.node = 'SERVICE_DELIVERED');

INSERT INTO parcel_trace (parcel_id, node, title, location, operator_id, operator_name, operator_role, detail, happened_at)
SELECT o.parcel_id, 'SERVICE_DONE', '代取服务完成结算', o.deliver_to, o.requester_id, u.real_name, 'STUDENT',
       '物主确认收货，结算 ' || to_char(o.fee + o.tip - o.commission, 'FM999990.00') || ' 元至接单人钱包，评分 ' || COALESCE(o.rating::text, '—'),
       o.done_at
FROM proxy_order o JOIN sys_user u ON u.id = o.requester_id
WHERE o.done_at IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM parcel_trace t WHERE t.parcel_id = o.parcel_id AND t.node = 'SERVICE_DONE');

COMMIT;

-- ---------- 14. 结果自检 ----------
SELECT 'wallet_account' AS t, count(*) FROM wallet_account
UNION ALL SELECT 'wallet_transaction', count(*) FROM wallet_transaction
UNION ALL SELECT 'recharge_order', count(*) FROM recharge_order
UNION ALL SELECT 'proxy_order', count(*) FROM proxy_order
UNION ALL SELECT 'parcel_trace', count(*) FROM parcel_trace
UNION ALL SELECT 'policy', count(*) FROM policy
UNION ALL SELECT 'policy_acceptance', count(*) FROM policy_acceptance
UNION ALL SELECT 'notice_popup', count(*) FROM notice WHERE is_popup = 1 AND status = 1;