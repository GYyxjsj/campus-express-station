package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.dto.IdentityPickupDTO;
import com.campus.express.entity.IdentityCode;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.IdentityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/** 身份条纹码：学生生成/出示，柜台核验/免码出库 */
@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping
    public R<Map<String, Object>> my() {
        return R.ok(identityService.my(UserContext.require().getUid()));
    }

    @PostMapping("/generate")
    @OpLog(module = "identity", action = "生成身份条码")
    public R<IdentityCode> generate() {
        return R.ok(identityService.generate(UserContext.require().getUid()));
    }

    @PostMapping("/{id}/revoke")
    @OpLog(module = "identity", action = "作废身份条码")
    public R<Void> revoke(@PathVariable Long id) {
        identityService.revoke(UserContext.require().getUid(), id);
        return R.ok();
    }

    @PostMapping("/verify")
    @RequirePerm("identity:verify")
    @OpLog(module = "identity", action = "柜台核验身份条码")
    public R<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        return R.ok(identityService.verify(UserContext.require(), body == null ? null : body.get("code")));
    }

    @PostMapping("/pickup")
    @RequirePerm("identity:verify")
    @OpLog(module = "identity", action = "身份条码免码出库")
    public R<Map<String, Object>> pickup(@Valid @RequestBody IdentityPickupDTO dto) {
        return R.ok(identityService.pickup(UserContext.require(), dto));
    }
}