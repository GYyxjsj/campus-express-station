package com.campus.express.security;

import com.campus.express.common.R;
import com.campus.express.common.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JWT 认证拦截器：
 * - 白名单（登录/注册/健康检查）直接放行；
 * - 其余请求解析 Bearer Token，成功写入 UserContext，失败返回 HTTP 401 + code 40100；
 * - 注入 traceId 便于日志追踪；afterCompletion 清理 ThreadLocal。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        MDC.put("traceId", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            LoginUser user = jwtUtil.parse(auth.substring(7));
            if (user != null) {
                UserContext.set(user);
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(
                R.fail(ErrorCodes.UNAUTHORIZED, "未登录或令牌已失效")));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
        MDC.remove("traceId");
    }
}