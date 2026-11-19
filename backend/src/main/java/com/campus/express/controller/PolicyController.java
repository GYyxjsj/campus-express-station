package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.dto.PolicyAcceptDTO;
import com.campus.express.security.UserContext;
import com.campus.express.service.PolicyService;
import com.campus.express.vo.PolicyVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 政策协议：阅读 / 签署 / 必签门禁 */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public R<List<PolicyVO>> list(@RequestParam(required = false) String category,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(policyService.list(UserContext.require().getUid(), category, keyword));
    }

    /** 尚未签署的必签协议（登录后门禁） */
    @GetMapping("/pending")
    public R<List<PolicyVO>> pending() {
        return R.ok(policyService.pending(UserContext.require().getUid()));
    }

    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        return R.ok(policyService.myAcceptances(UserContext.require().getUid()));
    }

    @GetMapping("/{code}")
    public R<PolicyVO> detail(@PathVariable String code) {
        return R.ok(policyService.detail(UserContext.require().getUid(), code));
    }

    @PostMapping("/accept")
    @OpLog(module = "policy", action = "签署协议")
    public R<Integer> accept(@Valid @RequestBody PolicyAcceptDTO dto, HttpServletRequest request) {
        return R.ok(policyService.accept(UserContext.require().getUid(), dto.getPolicyIds(), clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}