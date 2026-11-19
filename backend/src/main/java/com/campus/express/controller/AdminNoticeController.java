package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.NoticeDTO;
import com.campus.express.entity.Notice;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.AdminManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 公告（5.5）。GET：登录可见已发布公告；有 notice:manage 可看全部。写操作 notice:manage */
@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final AdminManageService manageService;

    public AdminNoticeController(AdminManageService manageService) {
        this.manageService = manageService;
    }

    @GetMapping
    public R<PageResult<Notice>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        LoginUser user = UserContext.require();
        boolean publishedOnly = !user.hasPerm("notice:manage");
        return R.ok(manageService.notices(publishedOnly, page, Math.min(size, 100)));
    }

    @PostMapping
    @RequirePerm("notice:manage")
    @OpLog(module = "admin", action = "发布公告")
    public R<Notice> create(@Valid @RequestBody NoticeDTO dto) {
        return R.ok(manageService.createNotice(UserContext.require().getUid(), dto));
    }

    @PutMapping("/{id}")
    @RequirePerm("notice:manage")
    @OpLog(module = "admin", action = "更新公告")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        manageService.updateNotice(id, dto);
        return R.ok();
    }
}