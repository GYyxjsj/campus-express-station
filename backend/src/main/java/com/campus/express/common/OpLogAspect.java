package com.campus.express.common;

import com.campus.express.entity.OperationLog;
import com.campus.express.mapper.OperationLogMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 操作日志切面：环绕 @OpLog 方法，成功/失败均落库 operation_log。
 * 日志失败不影响主业务（仅 warn）。入参序列化前脱敏密码类字段。
 */
@Aspect
@Component
public class OpLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OpLogAspect.class);
    private static final int DETAIL_MAX = 1800;

    private final OperationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public OpLogAspect(OperationLogMapper logMapper, ObjectMapper objectMapper) {
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                save(pjp, opLog, result, error);
            } catch (Exception e) {
                log.warn("操作日志落库失败: {}", e.getMessage());
            }
        }
    }

    private void save(ProceedingJoinPoint pjp, OpLog opLog, Object result, Throwable error) {
        OperationLog entity = new OperationLog();
        LoginUser user = UserContext.get();
        if (user != null) {
            entity.setOperatorId(user.getUid());
            entity.setOperatorName(user.getRealName() + "(" + user.getUsername() + ")");
        } else {
            entity.setOperatorName("anonymous");
        }
        entity.setModule(opLog.module());
        entity.setAction(opLog.action());
        entity.setSuccess(error == null ? 1 : 0);
        entity.setIp(clientIp());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("args", sanitize(pjp.getArgs()));
        if (error != null) {
            detail.put("error", error.getMessage());
        } else {
            detail.put("result", brief(result));
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            json = String.valueOf(detail);
        }
        entity.setDetail(json.length() > DETAIL_MAX ? json.substring(0, DETAIL_MAX) : json);
        entity.setTargetId(extractTargetId(pjp.getArgs(), result));
        logMapper.insert(entity);
    }

    /** 参数脱敏：跳过 Servlet 对象；密码/取件码类字段打码 */
    private Object sanitize(Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || arg instanceof javax.servlet.ServletRequest
                    || arg instanceof javax.servlet.ServletResponse) {
                continue;
            }
            try {
                String json = objectMapper.writeValueAsString(arg);
                json = json.replaceAll("(\"(?:password|oldPassword|newPassword|pickupCode|authCode)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
                map.put("arg" + i, json.length() > 400 ? json.substring(0, 400) : json);
            } catch (Exception e) {
                map.put("arg" + i, String.valueOf(arg));
            }
        }
        return map;
    }

    /** 返回值摘要：只取常见 id/单号字段，避免大对象入库 */
    private Object brief(Object result) {
        if (result == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return json.length() > 300 ? json.substring(0, 300) : json;
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }

    /** 目标id：优先取路径/入参中的 Long，其次 DTO 的 parcelId/id 反射 */
    private String extractTargetId(Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return String.valueOf(arg);
            }
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof String || arg instanceof Number) {
                continue;
            }
            for (String getter : new String[]{"getParcelId", "getId"}) {
                try {
                    Object v = arg.getClass().getMethod(getter).invoke(arg);
                    if (v != null) {
                        return String.valueOf(v);
                    }
                } catch (Exception ignored) {
                    // 无该 getter，继续尝试
                }
            }
        }
        return null;
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}