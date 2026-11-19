package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.OperationLog;
import com.campus.express.security.RequirePerm;
import com.campus.express.service.AdminManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志（5.5，log:view） */
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final AdminManageService manageService;

    public AdminLogController(AdminManageService manageService) {
        this.manageService = manageService;
    }

    @GetMapping
    @RequirePerm("log:view")
    public R<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String sortKey,
                                            @RequestParam(required = false) String order) {
        return R.ok(manageService.logs(module, keyword, sortKey, order, page, Math.min(size, 100)));
    }
}