package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.dto.LoginDTO;
import com.campus.express.dto.PasswordDTO;
import com.campus.express.dto.RegisterDTO;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.JwtUtil;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 认证与账户服务。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final com.campus.express.mapper.WalletMapper walletMapper;
    private final InviteService inviteService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       com.campus.express.mapper.WalletMapper walletMapper, InviteService inviteService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.walletMapper = walletMapper;
        this.inviteService = inviteService;
    }

    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        // 统一错误提示，不区分"用户不存在/密码错误"
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCodes.LOGIN_FAILED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            String msg = user.getStatus() != null && user.getStatus() == 2
                    ? "账号已注销，请联系管理员恢复" : "账号已停用，请联系管理员";
            throw new BizException(ErrorCodes.USER_DISABLED, msg);
        }
        LoginUser loginUser = buildLoginUser(user);
        String token = jwtUtil.issue(loginUser);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toVo(user, loginUser));
        return result;
    }

    @Transactional
    public void register(RegisterDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "该学号已注册");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), "STUDENT");
        walletMapper.insertAccount(user.getId());
        inviteService.ensureInviteCode(user);
        inviteService.bindOnRegister(user.getId(), dto.getInviteCode());
    }

    public UserVO me(Long uid) {
        SysUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        return toVo(user, buildLoginUser(user));
    }

    @Transactional
    public void changePassword(Long uid, PasswordDTO dto) {
        SysUser user = userMapper.selectById(uid);
        if (user == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCodes.LOGIN_FAILED, "原密码错误");
        }
        userMapper.updatePassword(uid, passwordEncoder.encode(dto.getNewPassword()));
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser lu = new LoginUser();
        lu.setUid(user.getId());
        lu.setUsername(user.getUsername());
        lu.setRealName(user.getRealName());
        lu.setRoles(new HashSet<>(userMapper.selectRoleCodes(user.getId())));
        lu.setPerms(new HashSet<>(userMapper.selectPermCodes(user.getId())));
        return lu;
    }

    private UserVO toVo(SysUser user, LoginUser lu) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoles(lu.getRoles());
        vo.setPerms(lu.getPerms());
        vo.setRoleNames(userMapper.selectRoleNames(user.getId()));
        return vo;
    }
}