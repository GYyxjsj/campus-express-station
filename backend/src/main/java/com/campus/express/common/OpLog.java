package com.campus.express.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：AOP 记录关键业务操作到 operation_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 模块: parcel/auth/admin/reminder */
    String module();

    /** 动作描述，如 "包裹入库" */
    String action();
}