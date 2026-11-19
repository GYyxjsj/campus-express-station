package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.Feedback;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 意见反馈：学生提交与查看，管理员回复 */
@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/api/feedback")
    @OpLog(module = "feedback", action = "提交意见反馈")
    public R<Feedback> submit(@RequestBody Map<String, Object> body) {
        LoginUser me = UserContext.require();
        return R.ok(feedbackService.submit(me.getUid(), me.getRealName(), body));
    }

    @GetMapping("/api/feedback/my")
    public R<PageResult<Feedback>> my(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(feedbackService.page(UserContext.require().getUid(), null, null, page, Math.min(size, 100)));
    }

    @GetMapping("/api/admin/feedback")
    @RequirePerm("feedback:reply")
    public R<PageResult<Feedback>> managePage(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String type) {
        return R.ok(feedbackService.page(null, status, type, page, Math.min(size, 100)));
    }

    @PostMapping("/api/admin/feedback/{id}/reply")
    @RequirePerm("feedback:reply")
    @OpLog(module = "feedback", action = "回复意见反馈")
    public R<Feedback> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LoginUser me = UserContext.require();
        return R.ok(feedbackService.reply(me.getUid(), me.getUid(), me.getRealName(), id, body.get("reply")));
    }
}