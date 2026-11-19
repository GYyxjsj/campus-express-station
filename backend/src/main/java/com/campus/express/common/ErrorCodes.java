package com.campus.express.common;

/**
 * 业务错误码常量（与设计文档 5.6 对齐）。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final int PARAM_INVALID = 40001;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    /** 接口不存在（HTTP 404） */
    public static final int API_NOT_FOUND = 40400;
    /** 请求方法不被支持（HTTP 405） */
    public static final int METHOD_NOT_ALLOWED = 40500;

    public static final int LOGIN_FAILED = 50001;
    public static final int USERNAME_EXISTS = 50002;

    public static final int PARCEL_NOT_FOUND = 60001;
    public static final int TRACKING_EXISTS = 60002;
    public static final int OWNER_PHONE_NOT_MATCH = 60003;
    public static final int PICKUP_CODE_INVALID = 60004;
    public static final int PARCEL_STATE_INVALID = 60005;

    public static final int AUTH_NOT_FOUND = 70001;
    public static final int AUTH_INACTIVE = 70002;
    public static final int AUTH_SELF_FORBIDDEN = 70003;
    public static final int AUTH_ALREADY_ACTIVE = 70004;
    public static final int AUTH_CODE_MISMATCH = 70005;

    public static final int USER_NOT_FOUND = 80001;
    public static final int USER_DISABLED = 80002;
    public static final int USER_CANCEL_BLOCKED = 80003;

    // 钱包 / 充值
    public static final int WALLET_NOT_FOUND = 81001;
    public static final int WALLET_FROZEN = 81002;
    public static final int BALANCE_NOT_ENOUGH = 81003;
    public static final int RECHARGE_AMOUNT_INVALID = 81004;
    public static final int RECHARGE_ORDER_NOT_FOUND = 81005;
    public static final int RECHARGE_ORDER_STATE = 81006;

    // 身份条码
    public static final int IDENTITY_NOT_FOUND = 82001;
    public static final int IDENTITY_INACTIVE = 82002;
    public static final int IDENTITY_EXPIRED = 82003;
    public static final int IDENTITY_OWNER_MISMATCH = 82004;

    // 代取服务单
    public static final int ORDER_NOT_FOUND = 83001;
    public static final int ORDER_STATE_INVALID = 83002;
    public static final int ORDER_SELF_FORBIDDEN = 83003;
    public static final int ORDER_ALREADY_ACTIVE = 83004;
    public static final int ORDER_FEE_INVALID = 83005;
    public static final int ORDER_NOT_RUNNER = 83006;
    public static final int ORDER_ALREADY_RATED = 83007;

    // 去向台账
    public static final int TRACE_NODE_INVALID = 84001;

    // 政策协议 / 公告
    public static final int POLICY_NOT_FOUND = 85001;
    public static final int POLICY_CODE_EXISTS = 85002;
    public static final int POLICY_NOT_PUBLISHED = 85003;
    public static final int NOTICE_NOT_FOUND = 86001;

    // 好友 / 邀请 / 卡券 / 地址簿等社区功能
    public static final int FRIEND_INVALID = 87001;
    public static final int FRIEND_EXISTS = 87002;
    public static final int FRIEND_NOT_FOUND = 87003;
    public static final int INVITE_INVALID = 87101;
    public static final int INVITE_ALREADY_BOUND = 87102;
    public static final int COUPON_INVALID = 87201;
    public static final int REDEEM_INVALID = 87202;
    public static final int ADDRESS_NOT_FOUND = 87301;
    public static final int SHIP_NOT_FOUND = 87401;
    public static final int SHIP_STATE_INVALID = 87402;
    public static final int INSURANCE_INVALID = 87501;
    public static final int TICKET_NOT_FOUND = 87601;
    public static final int TICKET_STATE_INVALID = 87602;
    public static final int FEEDBACK_NOT_FOUND = 87701;
    public static final int AI_REJECTED = 87801;
    public static final int IMPORT_INVALID = 87901;
    public static final int CHAT_INVALID = 88001;
    public static final int CHAT_NOT_FRIEND = 88002;

    public static final int INTERNAL_ERROR = 90000;
}