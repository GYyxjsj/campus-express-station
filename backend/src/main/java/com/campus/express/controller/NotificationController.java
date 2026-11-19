package com.campus.express.controller;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.Notification;
import com.campus.express.security.UserContext;
import com.campus.express.service.ReminderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 取件提醒（5.3） */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ReminderService reminderService;

    public NotificationController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/my")
    public R<PageResult<Notification>> my(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Boolean unreadOnly) {
        return R.ok(reminderService.myNotifications(UserContext.require().getUid(),
                unreadOnly, page, Math.min(size, 100)));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(reminderService.unreadCount(UserContext.require().getUid()));
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        reminderService.markRead(UserContext.require().getUid(), id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        reminderService.markAllRead(UserContext.require().getUid());
        return R.ok();
    }

    @PostMapping("/resend")
    @OpLog(module = "reminder", action = "手动补发到站提醒")
    public R<Void> resend(@RequestBody Map<String, Object> body) {
        Object parcelId = body.get("parcelId");
        if (!(parcelId instanceof Number)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "缺少 parcelId");
        }
        reminderService.resend(((Number) parcelId).longValue(), UserContext.require());
        return R.ok();
    }
}