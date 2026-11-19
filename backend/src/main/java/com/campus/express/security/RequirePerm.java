package com.campus.express.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限点注解：标注在 Controller 方法上，由 PermissionAspect 校验。
 * ADMIN 角色默认拥有全部权限点。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    /** 权限点编码，如 parcel:inbound */
    String value();
}