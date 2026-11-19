package com.campus.express.security;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;

/**
 * 请求级登录态（ThreadLocal），拦截器写入，请求结束清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 必须已登录，否则 401 业务码 */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或令牌已失效");
        }
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}