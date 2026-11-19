package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.ProxyAuthCreateDTO;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.ProxyAuthService;
import com.campus.express.vo.ProxyAuthVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 代取授权（5.4） */
@RestController
@RequestMapping("/api/proxy-auth")
public class ProxyAuthController {

    private final ProxyAuthService proxyAuthService;

    public ProxyAuthController(ProxyAuthService proxyAuthService) {
        this.proxyAuthService = proxyAuthService;
    }

    @PostMapping
    @RequirePerm("auth:create")
    @OpLog(module = "auth", action = "发起代取授权")
    public R<ProxyAuthVO> create(@Valid @RequestBody ProxyAuthCreateDTO dto) {
        return R.ok(proxyAuthService.create(UserContext.require().getUid(), dto));
    }

    @GetMapping("/granted")
    public R<PageResult<ProxyAuthVO>> granted(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String status) {
        return R.ok(proxyAuthService.granted(UserContext.require().getUid(), status,
                page, Math.min(size, 100)));
    }

    @GetMapping("/received")
    public R<PageResult<ProxyAuthVO>> received(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String status) {
        return R.ok(proxyAuthService.received(UserContext.require().getUid(), status,
                page, Math.min(size, 100)));
    }

    @PutMapping("/{id}/revoke")
    @RequirePerm("auth:revoke")
    @OpLog(module = "auth", action = "撤销代取授权")
    public R<Void> revoke(@PathVariable Long id) {
        proxyAuthService.revoke(UserContext.require().getUid(), id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<ProxyAuthVO> detail(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        boolean staffSide = user.hasRole("STAFF") || user.hasRole("ADMIN");
        return R.ok(proxyAuthService.detail(user.getUid(), staffSide, id));
    }
}