package com.campus.express.controller;

import com.campus.express.common.R;
import com.campus.express.entity.Notice;
import com.campus.express.security.LoginUser;
import com.campus.express.security.UserContext;
import com.campus.express.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 公告弹窗（仅学生端；管理端/员工端不弹，公告中心仍可主动查看） */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/popup")
    public R<Map<String, Object>> popup() {
        LoginUser user = UserContext.require();
        // 弹窗公告仅面向学生角色；ADMIN/STAFF 直接返回空，前端另有角色判断双保险
        List<Notice> list = user.hasRole("STUDENT")
                ? noticeService.popup(user.getUid())
                : Collections.emptyList();
        Map<String, Object> result = new HashMap<>();
        result.put("notices", list);
        result.put("count", list.size());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<Notice> detail(@PathVariable Long id) {
        return R.ok(noticeService.detail(id));
    }

    @PostMapping("/{id}/dismiss")
    public R<Void> dismiss(@PathVariable Long id) {
        noticeService.dismiss(UserContext.require().getUid(), id);
        return R.ok();
    }

    @PostMapping("/dismiss-all")
    public R<Void> dismissAll() {
        noticeService.dismissAll(UserContext.require().getUid());
        return R.ok();
    }
}