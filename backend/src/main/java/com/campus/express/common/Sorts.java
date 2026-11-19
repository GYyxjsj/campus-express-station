package com.campus.express.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 列表排序参数白名单解析：前端仅传语义化 sortKey + order，
 * SQL 片段全部来自这里的固定映射，杜绝 ORDER BY 注入。
 */
public final class Sorts {

    private Sorts() {}

    private static final Map<String, String> PARCEL = new HashMap<>();
    private static final Map<String, String> USER = new HashMap<>();
    private static final Map<String, String> LOG = new HashMap<>();
    private static final Map<String, String> POLICY = new HashMap<>();
    private static final Map<String, String> ACCEPTANCE = new HashMap<>();
    private static final Map<String, String> PROXY = new HashMap<>();
    private static final Map<String, String> WALLET_ACCOUNT = new HashMap<>();
    private static final Map<String, String> WALLET_TX = new HashMap<>();
    private static final Map<String, String> RECHARGE = new HashMap<>();
    private static final Map<String, String> TRACE = new HashMap<>();

    static {
        PARCEL.put("inboundTime", "p.inbound_time");
        PARCEL.put("trackingNo", "p.tracking_no");
        PARCEL.put("status", "p.status");
        PARCEL.put("ownerName", "u.real_name");
        PARCEL.put("stationName", "s.name");
        PARCEL.put("pickupCode", "p.pickup_code");

        USER.put("createdAt", "u.created_at");
        USER.put("username", "u.username");
        USER.put("realName", "u.real_name");
        USER.put("status", "u.status");

        LOG.put("createdAt", "created_at");
        LOG.put("operatorName", "operator_name");
        LOG.put("module", "module");
        LOG.put("action", "action");
        LOG.put("success", "success");

        POLICY.put("updatedAt", "p.updated_at");
        POLICY.put("code", "p.code");
        POLICY.put("title", "p.title");
        POLICY.put("category", "p.category");
        POLICY.put("required", "p.required");
        POLICY.put("acceptCount", "accept_count");
        POLICY.put("status", "p.status");

        ACCEPTANCE.put("acceptedAt", "a.accepted_at");
        ACCEPTANCE.put("realName", "u.real_name");
        ACCEPTANCE.put("policyTitle", "p.title");
        ACCEPTANCE.put("version", "a.version");
        ACCEPTANCE.put("ip", "a.ip");

        PROXY.put("createdAt", "o.created_at");
        PROXY.put("orderNo", "o.order_no");
        PROXY.put("status", "o.status");
        PROXY.put("trackingNo", "p.tracking_no");
        PROXY.put("requesterName", "ru.real_name");
        PROXY.put("total", "total");
        PROXY.put("commission", "o.commission");
        PROXY.put("rating", "o.rating");

        WALLET_ACCOUNT.put("updatedAt", "w.updated_at");
        WALLET_ACCOUNT.put("realName", "u.real_name");
        WALLET_ACCOUNT.put("balance", "w.balance");
        WALLET_ACCOUNT.put("totalRecharge", "w.total_recharge");
        WALLET_ACCOUNT.put("totalSpent", "w.total_spent");
        WALLET_ACCOUNT.put("totalEarned", "w.total_earned");
        WALLET_ACCOUNT.put("status", "w.status");

        WALLET_TX.put("createdAt", "created_at");
        WALLET_TX.put("type", "type");
        WALLET_TX.put("amount", "amount");
        WALLET_TX.put("balanceAfter", "balance_after");
        WALLET_TX.put("title", "title");
        WALLET_TX.put("channel", "channel");

        RECHARGE.put("createdAt", "o.created_at");
        RECHARGE.put("orderNo", "o.order_no");
        RECHARGE.put("realName", "u.real_name");
        RECHARGE.put("amount", "o.amount");
        RECHARGE.put("bonus", "o.bonus");
        RECHARGE.put("channel", "o.channel");
        RECHARGE.put("status", "o.status");

        TRACE.put("happenedAt", "t.happened_at");
        TRACE.put("node", "t.node");
        TRACE.put("title", "t.title");
        TRACE.put("trackingNo", "p.tracking_no");
        TRACE.put("location", "t.location");
        TRACE.put("operatorName", "t.operator_name");
    }

    public static String parcel(String key) { return resolve(PARCEL, key, "p.inbound_time"); }
    public static String user(String key) { return resolve(USER, key, "u.created_at"); }
    public static String log(String key) { return resolve(LOG, key, "created_at"); }
    public static String policy(String key) { return resolve(POLICY, key, "p.updated_at"); }
    public static String acceptance(String key) { return resolve(ACCEPTANCE, key, "a.accepted_at"); }
    public static String proxy(String key) { return resolve(PROXY, key, "o.created_at"); }
    public static String walletAccount(String key) { return resolve(WALLET_ACCOUNT, key, "w.updated_at"); }
    public static String walletTx(String key) { return resolve(WALLET_TX, key, "created_at"); }
    public static String recharge(String key) { return resolve(RECHARGE, key, "o.created_at"); }
    public static String trace(String key) { return resolve(TRACE, key, "t.happened_at"); }

    /** ascending → ASC，其余一律 DESC（默认倒序） */
    public static String dir(String order) {
        return "ascending".equals(order) || "asc".equalsIgnoreCase(order) ? "ASC" : "DESC";
    }

    private static String resolve(Map<String, String> map, String key, String def) {
        String col = key == null ? null : map.get(key);
        return col == null ? def : col;
    }
}