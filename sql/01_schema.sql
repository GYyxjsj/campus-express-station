-- ============================================================
-- 校园快递驿站取件提醒与代取授权管理系统
-- PostgreSQL 14+ 建库脚本
-- 执行: psql -U postgres -d campus_express -f 01_schema.sql
-- 建库: CREATE DATABASE campus_express ENCODING 'UTF8';
-- ============================================================

BEGIN;

-- ---------- 1. 用户 ----------
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,                -- 学号/工号
    real_name     VARCHAR(64)  NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(128),
    password      VARCHAR(100) NOT NULL,                -- BCrypt
    status        SMALLINT     NOT NULL DEFAULT 1,      -- 1启用 0停用
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_username UNIQUE (username)
);
CREATE INDEX IF NOT EXISTS idx_user_phone ON sys_user(phone);
COMMENT ON TABLE sys_user IS '用户(学生/驿站员/管理员)';

-- ---------- 2. 角色 / 权限 ----------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    role_code   VARCHAR(32) NOT NULL,
    role_name   VARCHAR(64) NOT NULL,
    remark      VARCHAR(255),
    CONSTRAINT uk_role_code UNIQUE (role_code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    perm_code   VARCHAR(64)  NOT NULL,
    perm_name   VARCHAR(64)  NOT NULL,
    module      VARCHAR(32)  NOT NULL,
    CONSTRAINT uk_perm_code UNIQUE (perm_code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL REFERENCES sys_user(id),
    role_id  BIGINT NOT NULL REFERENCES sys_role(id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id       BIGSERIAL PRIMARY KEY,
    role_id  BIGINT NOT NULL REFERENCES sys_role(id),
    perm_id  BIGINT NOT NULL REFERENCES sys_permission(id),
    CONSTRAINT uk_role_perm UNIQUE (role_id, perm_id)
);

-- ---------- 3. 驿站 / 货架 ----------
CREATE TABLE IF NOT EXISTS station (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    location     VARCHAR(255) NOT NULL,
    open_hours   VARCHAR(64)  NOT NULL DEFAULT '08:00-21:00',
    contact      VARCHAR(32),
    status       SMALLINT     NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shelf (
    id          BIGSERIAL PRIMARY KEY,
    station_id  BIGINT      NOT NULL REFERENCES station(id),
    code        VARCHAR(16) NOT NULL,                   -- 如 A-01-03 (区-列-层)
    capacity    INT         NOT NULL DEFAULT 40,
    status      SMALLINT    NOT NULL DEFAULT 1,
    CONSTRAINT uk_shelf_code UNIQUE (station_id, code)
);

-- ---------- 4. 包裹 ----------
CREATE TABLE IF NOT EXISTS parcel (
    id              BIGSERIAL PRIMARY KEY,
    tracking_no     VARCHAR(64)  NOT NULL,
    courier_company VARCHAR(32)  NOT NULL,              -- SF/ZT/YT/JD/STO/YD/EMS/JT
    owner_id        BIGINT       NOT NULL REFERENCES sys_user(id),
    station_id      BIGINT       NOT NULL REFERENCES station(id),
    shelf_id        BIGINT       REFERENCES shelf(id),
    pickup_code     VARCHAR(6)   NOT NULL,              -- 6位数字取件码
    status          VARCHAR(16)  NOT NULL DEFAULT 'IN_STOCK',  -- IN_STOCK/OVERDUE/PICKED_UP/RETURNED
    remark          VARCHAR(255),
    extra           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    inbound_time    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    outbound_time   TIMESTAMPTZ,
    return_reason   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_parcel_tracking UNIQUE (tracking_no),
    CONSTRAINT ck_parcel_status CHECK (status IN ('IN_STOCK','OVERDUE','PICKED_UP','RETURNED'))
);
CREATE INDEX IF NOT EXISTS idx_parcel_owner   ON parcel(owner_id, status);
CREATE INDEX IF NOT EXISTS idx_parcel_status  ON parcel(status, inbound_time);
CREATE INDEX IF NOT EXISTS idx_parcel_station ON parcel(station_id, status);
CREATE INDEX IF NOT EXISTS idx_parcel_code    ON parcel(pickup_code);
COMMENT ON TABLE parcel IS '包裹(状态机: IN_STOCK→OVERDUE→PICKED_UP/RETURNED)';

-- ---------- 5. 取件提醒 ----------
CREATE TABLE IF NOT EXISTS notification (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES sys_user(id),
    parcel_id   BIGINT      REFERENCES parcel(id),
    type        VARCHAR(16) NOT NULL,                   -- ARRIVE/REMIND/OVERDUE/PROXY_GRANTED/PROXY_USED/NOTICE
    channel     VARCHAR(16) NOT NULL DEFAULT 'IN_APP',  -- IN_APP/SMS/EMAIL
    title       VARCHAR(128) NOT NULL,
    content     VARCHAR(1024) NOT NULL,
    is_read     SMALLINT    NOT NULL DEFAULT 0,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_notif_user  ON notification(user_id, is_read, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_parcel ON notification(parcel_id, type, sent_at DESC);

-- ---------- 6. 代取授权 ----------
CREATE TABLE IF NOT EXISTS proxy_auth (
    id           BIGSERIAL PRIMARY KEY,
    parcel_id    BIGINT      NOT NULL REFERENCES parcel(id),
    grantor_id   BIGINT      NOT NULL REFERENCES sys_user(id),   -- 授权人(物主)
    grantee_id   BIGINT      NOT NULL REFERENCES sys_user(id),   -- 被授权人(代取人)
    auth_code    VARCHAR(8)  NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',          -- ACTIVE/USED/REVOKED/EXPIRED
    expire_at    TIMESTAMPTZ NOT NULL,
    remark       VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at      TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    CONSTRAINT uk_auth_code UNIQUE (auth_code),
    CONSTRAINT ck_auth_status CHECK (status IN ('ACTIVE','USED','REVOKED','EXPIRED')),
    CONSTRAINT ck_auth_not_self CHECK (grantor_id <> grantee_id)
);
-- 同一包裹同时最多一条生效授权（部分唯一索引，DB 层兜底并发）
CREATE UNIQUE INDEX IF NOT EXISTS uk_proxy_active ON proxy_auth(parcel_id) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_auth_grantor ON proxy_auth(grantor_id, status);
CREATE INDEX IF NOT EXISTS idx_auth_grantee ON proxy_auth(grantee_id, status);
CREATE INDEX IF NOT EXISTS idx_auth_expire  ON proxy_auth(status, expire_at);

-- ---------- 7. 取件流水 ----------
CREATE TABLE IF NOT EXISTS pickup_record (
    id          BIGSERIAL PRIMARY KEY,
    parcel_id   BIGINT      NOT NULL REFERENCES parcel(id),
    pickup_type VARCHAR(8)  NOT NULL,                   -- SELF/PROXY
    owner_id    BIGINT      NOT NULL REFERENCES sys_user(id),
    picker_id   BIGINT      NOT NULL REFERENCES sys_user(id),
    auth_id     BIGINT      REFERENCES proxy_auth(id),
    staff_id    BIGINT      NOT NULL REFERENCES sys_user(id),   -- 经办驿站员
    station_id  BIGINT      NOT NULL REFERENCES station(id),
    picked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_pickup_parcel UNIQUE (parcel_id),
    CONSTRAINT ck_pickup_type CHECK (pickup_type IN ('SELF','PROXY'))
);
CREATE INDEX IF NOT EXISTS idx_pickup_time ON pickup_record(picked_at);

-- ---------- 8. 配置 / 公告 / 操作日志 ----------
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(64)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    remark       VARCHAR(255),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_config_key UNIQUE (config_key)
);

CREATE TABLE IF NOT EXISTS notice (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(128)  NOT NULL,
    content     VARCHAR(2048) NOT NULL,
    status      SMALLINT      NOT NULL DEFAULT 1,       -- 1发布 0下线
    publisher_id BIGINT       NOT NULL REFERENCES sys_user(id),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS operation_log (
    id           BIGSERIAL PRIMARY KEY,
    operator_id  BIGINT,
    operator_name VARCHAR(64),
    module       VARCHAR(32)  NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    target_id    VARCHAR(64),
    detail       VARCHAR(1024),
    ip           VARCHAR(64),
    success      SMALLINT     NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oplog_time ON operation_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_oplog_module ON operation_log(module, created_at DESC);

COMMIT;