package com.campus.express.controller;

import com.campus.express.common.R;
import com.campus.express.security.UserContext;
import com.campus.express.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI 助手（登录即可用；注入/越域由 Service 拦截） */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(aiService.status());
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String message = body.get("message") == null ? null : String.valueOf(body.get("message"));
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
        return R.ok(aiService.chat(UserContext.require().getUid(), message, history));
    }
}