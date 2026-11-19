package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.PolicyDTO;
import com.campus.express.entity.Policy;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.PolicyService;
import com.campus.express.vo.PolicyVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/** 政策协议管理（管理员） */
@RestController
@RequestMapping("/api/admin/policies")
public class AdminPolicyController {

    private final PolicyService policyService;

    public AdminPolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/stats")
    @RequirePerm("policy:manage")
    public R<Map<String, Object>> stats() {
        return R.ok(policyService.stats());
    }

    @GetMapping
    @RequirePerm("policy:manage")
    public R<PageResult<PolicyVO>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String sortKey,
                                        @RequestParam(required = false) String order) {
        return R.ok(policyService.adminPage(category, status, keyword, sortKey, order, page, Math.min(size, 100)));
    }

    @GetMapping("/acceptances")
    @RequirePerm("policy:manage")
    public R<PageResult<Map<String, Object>>> acceptances(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) Long policyId,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String sortKey,
                                                          @RequestParam(required = false) String order) {
        return R.ok(policyService.acceptances(policyId, keyword, sortKey, order, page, Math.min(size, 100)));
    }

    @PostMapping
    @RequirePerm("policy:manage")
    @OpLog(module = "policy", action = "新建协议")
    public R<Policy> create(@Valid @RequestBody PolicyDTO dto) {
        return R.ok(policyService.create(UserContext.require(), dto));
    }

    @PutMapping("/{id}")
    @RequirePerm("policy:manage")
    @OpLog(module = "policy", action = "修订协议")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PolicyDTO dto) {
        policyService.update(UserContext.require(), id, dto);
        return R.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePerm("policy:manage")
    @OpLog(module = "policy", action = "协议上下线")
    public R<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        policyService.setStatus(id, status);
        return R.ok();
    }
}