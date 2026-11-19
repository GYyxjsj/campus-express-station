package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.dto.LoginDTO;
import com.campus.express.dto.PasswordDTO;
import com.campus.express.dto.RegisterDTO;
import com.campus.express.security.UserContext;
import com.campus.express.service.AuthService;
import com.campus.express.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/** 认证与账户（5.1） */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.me(UserContext.require().getUid()));
    }

    @PutMapping("/password")
    @OpLog(module = "auth", action = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        authService.changePassword(UserContext.require().getUid(), dto);
        return R.ok();
    }
}