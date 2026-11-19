package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.UserCreateDTO;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理（管理员）。
 */
@Service
public class AdminUserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<UserVO> page(String keyword, String roleCode, Integer status,
                                   String sortKey, String order, int page, int size) {
        long total = userMapper.countPage(keyword, roleCode, status);
        List<SysUser> users = userMapper.selectPage(keyword, roleCode, status,
                com.campus.express.common.Sorts.user(sortKey),
                com.campus.express.common.Sorts.dir(order), (page - 1) * size, size);
        List<UserVO> vos = users.stream().map(this::toVo).collect(Collectors.toList());
        return new PageResult<>(total, vos);
    }

    @Transactional
    public UserVO create(UserCreateDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        for (String role : dto.getRoles()) {
            if (!"ADMIN".equals(role) && !"STAFF".equals(role) && !"STUDENT".equals(role)) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "非法角色: " + role);
            }
            userMapper.insertUserRole(user.getId(), role);
        }
        return toVo(user);
    }

    @Transactional
    public void setStatus(Long id, Integer status) {
        requireUser(id);
        userMapper.updateStatus(id, status);
    }

    /**
     * 注销账号（软删除 status=2）：保留审计与外键完整性，阻断登录，可恢复。
     * 护栏：不能注销自己 / 最后一名管理员 / 名下有在库包裹 / 进行中代取单 / 钱包余额。
     */
    @Transactional
    public void cancel(Long id, LoginUser actor) {
        SysUser user = requireUser(id);
        if (actor != null && id.equals(actor.getUid())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "不能注销当前登录的账号");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw new BizException(ErrorCodes.USER_CANCEL_BLOCKED, "该账号已注销");
        }
        if (userMapper.selectRoleCodes(id).contains("ADMIN") && userMapper.selectAdminIds().size() <= 1) {
            throw new BizException(ErrorCodes.FORBIDDEN, "系统至少须保留一名启用中的管理员");
        }
        long parcels = userMapper.countActiveParcels(id);
        if (parcels > 0) {
            throw new BizException(ErrorCodes.USER_CANCEL_BLOCKED, "名下仍有 " + parcels + " 件在库/滞留包裹，请先处理");
        }
        long orders = userMapper.countActiveProxyOrders(id);
        if (orders > 0) {
            throw new BizException(ErrorCodes.USER_CANCEL_BLOCKED, "名下仍有 " + orders + " 单进行中的代取服务，请先完结");
        }
        BigDecimal balance = userMapper.selectWalletBalance(id);
        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException(ErrorCodes.USER_CANCEL_BLOCKED, "钱包仍有余额 " + balance.toPlainString() + " 元，请先清零或退款");
        }
        userMapper.revokeActiveIdentity(id);
        userMapper.updateStatus(id, 2);
    }

    /** 恢复已注销账号（status 2 → 1）。 */
    @Transactional
    public void restore(Long id) {
        SysUser user = requireUser(id);
        if (user.getStatus() == null || user.getStatus() != 2) {
            throw new BizException(ErrorCodes.USER_CANCEL_BLOCKED, "仅已注销的账号可恢复");
        }
        userMapper.updateStatus(id, 1);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        requireUser(id);
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "密码长度须为6-32位");
        }
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void assignRoles(Long id, List<String> roles) {
        requireUser(id);
        userMapper.deleteUserRoles(id);
        for (String role : roles) {
            userMapper.insertUserRole(id, role);
        }
    }

    /** 授权选人搜索（脱敏：手机号打码） */
    public List<UserVO> search(String keyword) {
        if (keyword == null || keyword.trim().length() < 1) {
            return new ArrayList<>();
        }
        return userMapper.searchStudents(keyword.trim()).stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setPhone(maskPhone(u.getPhone()));
            return vo;
        }).collect(Collectors.toList());
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toVo(SysUser u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setStatus(u.getStatus());
        vo.setCreatedAt(u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        vo.setRoles(new HashSet<>(userMapper.selectRoleCodes(u.getId())));
        vo.setRoleNames(userMapper.selectRoleNames(u.getId()));
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}