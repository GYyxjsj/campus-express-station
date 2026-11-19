package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.InsurancePolicy;
import com.campus.express.security.UserContext;
import com.campus.express.service.InsuranceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 超时保险：方案参数、投保、我的保单 */
@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @GetMapping("/plan")
    public R<Map<String, Object>> plan() {
        return R.ok(insuranceService.plan());
    }

    @GetMapping("/my")
    public R<PageResult<InsurancePolicy>> my(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String status) {
        return R.ok(insuranceService.my(UserContext.require().getUid(), status, page, Math.min(size, 100)));
    }

    @PostMapping("/buy")
    @OpLog(module = "insurance", action = "购买超时保险")
    public R<InsurancePolicy> buy(@RequestBody Map<String, Object> body) {
        Object parcelId = body.get("parcelId");
        Long pid;
        try {
            pid = Long.parseLong(String.valueOf(parcelId).trim());
        } catch (Exception e) {
            pid = null;
        }
        return R.ok(insuranceService.buy(UserContext.require().getUid(), pid));
    }
}