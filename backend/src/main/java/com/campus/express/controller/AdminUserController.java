package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.UserCreateDTO;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.AdminUserService;
import com.campus.express.vo.UserVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 用户管理（5.5，user:manage；search 对登录用户开放供授权选人） */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @RequirePerm("user:manage")
    public R<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String roleCode,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String sortKey,
                                      @RequestParam(required = false) String order) {
        return R.ok(adminUserService.page(keyword, roleCode, status, sortKey, order, page, Math.min(size, 100)));
    }

    /** 授权选人搜索：脱敏返回，登录即可 */
    @GetMapping("/search")
    public R<List<UserVO>> search(@RequestParam String keyword) {
        UserContext.require();
        return R.ok(adminUserService.search(keyword));
    }

    @PostMapping
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "创建用户")
    public R<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(adminUserService.create(dto));
    }

    @PutMapping("/{id}/status")
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "启用/停用用户")
    public R<Void> setStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminUserService.setStatus(id, body.get("status"));
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "重置用户密码")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminUserService.resetPassword(id, body.get("newPassword"));
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "分配用户角色")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        adminUserService.assignRoles(id, body.get("roles"));
        return R.ok();
    }

    /** 注销账号（软删除，护栏校验后阻断登录并吊销身份条码） */
    @DeleteMapping("/{id}")
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "注销用户")
    public R<Void> cancel(@PathVariable Long id) {
        adminUserService.cancel(id, UserContext.require());
        return R.ok();
    }

    /** 恢复已注销账号 */
    @PutMapping("/{id}/restore")
    @RequirePerm("user:manage")
    @OpLog(module = "admin", action = "恢复用户")
    public R<Void> restore(@PathVariable Long id) {
        adminUserService.restore(id);
        return R.ok();
    }
}