package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.security.UserContext;
import com.campus.express.service.InviteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 邀请有礼 */
@RestController
@RequestMapping("/api/invite")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        return R.ok(inviteService.info(UserContext.require().getUid()));
    }

    @GetMapping("/records")
    public R<PageResult<Map<String, Object>>> records(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(inviteService.records(UserContext.require().getUid(), page, Math.min(size, 100)));
    }

    @PostMapping("/bind")
    public R<Void> bind(@RequestBody Map<String, String> body) {
        inviteService.bind(UserContext.require().getUid(), body.get("code"));
        return R.ok();
    }
}