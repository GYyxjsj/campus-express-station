-- ============================================================
-- 06_chat.sql 好友私聊消息
-- 依赖：01_schema.sql / 05_community.sql（friendship / sys_user）
-- 幂等：可重复执行
-- ============================================================
SET search_path TO campus_express, public;

CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT       NOT NULL REFERENCES sys_user(id),
    receiver_id BIGINT       NOT NULL REFERENCES sys_user(id),
    content     VARCHAR(500) NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_chat_not_self CHECK (sender_id <> receiver_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_pair_time ON chat_message(sender_id, receiver_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_receiver_read ON chat_message(receiver_id, read_at);

COMMENT ON TABLE chat_message IS '好友私聊消息（仅 ACCEPTED 好友之间可发送）';

-- ---------- 种子：演示账号 20230101(3) 与好友 20240333(7)、20230241(6) 的对话 ----------
INSERT INTO chat_message (sender_id, receiver_id, content, read_at, created_at)
SELECT * FROM (VALUES
    (7::bigint, 3::bigint, '同学你好，我看到你有件滞留快递，明天中午我顺路帮你代取？', now() - interval '2 days 3 hours', now() - interval '2 days 3 hours'),
    (3::bigint, 7::bigint, '好呀，取件码我发在授权里了，麻烦啦', now() - interval '2 days 2 hours', now() - interval '2 days 2 hours'),
    (7::bigint, 3::bigint, '已经取到啦，包裹在 3 号货架下层，晚点给你送宿舍楼下', now() - interval '1 day 5 hours', now() - interval '1 day 5 hours'),
    (3::bigint, 7::bigint, '收到，晚上 7 点楼下见，请你喝奶茶', NULL, now() - interval '1 day 4 hours'),
    (6::bigint, 3::bigint, '驿站今天 18:00 后入库高峰，取件建议错峰哦', NULL, now() - interval '6 hours'),
    (3::bigint, 6::bigint, '了解，我下午没课就先去取了', NULL, now() - interval '5 hours')
) AS v(sender_id, receiver_id, content, read_at, created_at)
WHERE NOT EXISTS (SELECT 1 FROM chat_message);