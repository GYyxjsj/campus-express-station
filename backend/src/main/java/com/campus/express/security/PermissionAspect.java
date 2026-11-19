package com.campus.express.security;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 权限点校验切面：@RequirePerm 声明式鉴权。
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePerm)")
    public void check(RequirePerm requirePerm) {
        LoginUser user = UserContext.require();
        if (!user.hasPerm(requirePerm.value())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权限执行该操作: " + requirePerm.value());
        }
    }
}