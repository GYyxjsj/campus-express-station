package com.campus.express.controller;

import com.campus.express.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/** 健康检查（拦截器白名单，Tomcat/Nginx 探活） */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("app", "campus-express");
        data.put("time", OffsetDateTime.now().toString());
        return R.ok(data);
    }
}