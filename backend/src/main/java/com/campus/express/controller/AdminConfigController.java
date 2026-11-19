package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.entity.SysConfig;
import com.campus.express.security.RequirePerm;
import com.campus.express.service.AdminManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 提醒策略配置（5.5，user:manage） */
@RestController
@RequestMapping("/api/admin/configs")
public class AdminConfigController {

    private final AdminManageService manageService;

    public AdminConfigController(AdminManageService manageService) {
        this.manageService = manageService;
    }

    @GetMapping
    @RequirePerm("user:manage")
    public R<List<SysConfig>> list() {
        return R.ok(manageService.configs());
    }

    @PutMapping
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "修改提醒策略")
    public R<Void> update(@RequestBody Map<String, String> values) {
        manageService.updateConfigs(values);
        return R.ok();
    }
}