package com.campus.express.controller;

import com.campus.express.common.R;
import com.campus.express.mapper.SysConfigMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 关于我们：读取 about.* 配置，前台展示 */
@RestController
@RequestMapping("/api/about")
public class AboutController {

    private static final String[][] FIELDS = {
            {"company", "运营主体"},
            {"owner", "负责人"},
            {"email", "联系邮箱"},
            {"phone", "服务热线"},
            {"address", "办公地址"},
            {"icp", "备案号"},
            {"version", "系统版本"}
    };

    private final SysConfigMapper configMapper;

    public AboutController(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @GetMapping
    public R<Map<String, Object>> about() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String[] field : FIELDS) {
            String v = configMapper.selectValue("about." + field[0]);
            result.put(field[0], v == null ? "" : v);
        }
        return R.ok(result);
    }
}