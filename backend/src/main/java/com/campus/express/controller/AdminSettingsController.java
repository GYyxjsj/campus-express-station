package com.campus.express.controller;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.security.RequirePerm;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI 助手与站点信息设置（ai:config）。
 * API Key 只回显脱敏片段；提交空值或脱敏占位表示不修改。
 */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private static final String KEY = "ai.api.key";
    private static final Set<String> TEXT_KEYS = new HashSet<>(Arrays.asList(
            "ai.api.url", "ai.api.model",
            "about.company", "about.owner", "about.email", "about.phone",
            "about.address", "about.icp", "about.version"));
    private static final Map<String, int[]> INT_KEYS = new LinkedHashMap<>();

    static {
        INT_KEYS.put("ai.max.history", new int[]{0, 20});
        INT_KEYS.put("ai.timeout.ms", new int[]{3000, 60000});
    }

    private final SysConfigMapper configMapper;

    public AdminSettingsController(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @GetMapping
    @RequirePerm("ai:config")
    public R<Map<String, String>> get() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("ai.enabled", val("ai.enabled", "1"));
        result.put("ai.api.url", val("ai.api.url", ""));
        result.put(KEY, mask(val(KEY, "")));
        result.put("ai.api.model", val("ai.api.model", ""));
        for (Map.Entry<String, int[]> e : INT_KEYS.entrySet()) {
            result.put(e.getKey(), val(e.getKey(), String.valueOf(e.getValue()[0])));
        }
        for (String k : TEXT_KEYS) {
            if (!result.containsKey(k)) {
                result.put(k, val(k, ""));
            }
        }
        return R.ok(result);
    }

    @PutMapping
    @RequirePerm("ai:config")
    @OpLog(module = "admin", action = "修改 AI/站点设置")
    public R<Void> update(@RequestBody Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            String k = e.getKey();
            String v = e.getValue() == null ? "" : e.getValue().trim();
            if (KEY.equals(k)) {
                // 空值或脱敏占位 = 保持原 Key 不变
                if (v.isEmpty() || v.contains("****")) {
                    continue;
                }
                if (v.length() > 200) {
                    throw new BizException(ErrorCodes.PARAM_INVALID, "API Key 长度异常");
                }
                configMapper.upsert(k, v);
            } else if ("ai.enabled".equals(k)) {
                if (!"0".equals(v) && !"1".equals(v)) {
                    throw new BizException(ErrorCodes.PARAM_INVALID, "ai.enabled 须为 0 或 1");
                }
                configMapper.upsert(k, v);
            } else if (INT_KEYS.containsKey(k)) {
                int[] range = INT_KEYS.get(k);
                int n;
                try {
                    n = Integer.parseInt(v);
                } catch (NumberFormatException ex) {
                    throw new BizException(ErrorCodes.PARAM_INVALID, k + " 须为整数");
                }
                if (n < range[0] || n > range[1]) {
                    throw new BizException(ErrorCodes.PARAM_INVALID,
                            k + " 须在 " + range[0] + " ~ " + range[1] + " 之间");
                }
                configMapper.upsert(k, String.valueOf(n));
            } else if (TEXT_KEYS.contains(k)) {
                if ("ai.api.url".equals(k) && !v.isEmpty()
                        && !v.startsWith("https://") && !v.startsWith("http://")) {
                    throw new BizException(ErrorCodes.PARAM_INVALID, "API 地址须为 http(s) URL");
                }
                if (v.length() > 300) {
                    throw new BizException(ErrorCodes.PARAM_INVALID, k + " 不能超过 300 字");
                }
                configMapper.upsert(k, v);
            } else {
                throw new BizException(ErrorCodes.PARAM_INVALID, "不支持的配置项：" + k);
            }
        }
        return R.ok();
    }

    private String val(String key, String def) {
        String v = configMapper.selectValue(key);
        return v == null ? def : v;
    }

    private static String mask(String key) {
        if (key.isEmpty()) { return ""; }
        if (key.length() <= 8) { return "****"; }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}