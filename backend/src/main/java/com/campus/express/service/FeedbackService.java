package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.Feedback;
import com.campus.express.mapper.FeedbackMapper;
import com.campus.express.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 意见反馈：提交即通知管理员，管理员回复后通知提交人 */
@Service
public class FeedbackService {

    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("SUGGEST", "BUG", "COMPLAINT", "PRAISE"));

    private final FeedbackMapper feedbackMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    public FeedbackService(FeedbackMapper feedbackMapper, SysUserMapper userMapper,
                           NotificationService notificationService) {
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public Feedback submit(Long uid, String realName, Map<String, Object> body) {
        String type = str(body.get("type")).toUpperCase();
        if (!TYPES.contains(type)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请选择反馈类型");
        }
        String content = str(body.get("content"));
        if (content.isEmpty() || content.length() > 1000) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "反馈内容必填且不超过 1000 字");
        }
        Feedback feedback = new Feedback();
        feedback.setUserId(uid);
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setContact(limit(str(body.get("contact")), 64));
        feedback.setStatus("PENDING");
        feedbackMapper.insert(feedback);
        for (Long adminId : userMapper.selectAdminIds()) {
            notificationService.sendBiz(adminId, null, "FEEDBACK", "收到新的意见反馈",
                    String.format("%s 提交了%s：%s", realName, typeName(type), abbr(content)));
        }
        return feedbackMapper.selectById(feedback.getId());
    }

    public PageResult<Feedback> page(Long userId, String status, String type, int page, int size) {
        long total = feedbackMapper.countPage(userId, status, type);
        return new PageResult<>(total, feedbackMapper.selectPage(userId, status, type, (page - 1) * size, size));
    }

    @Transactional
    public Feedback reply(Long uid, Long replierId, String replierName, Long id, String replyText) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException(ErrorCodes.FEEDBACK_NOT_FOUND, "反馈不存在");
        }
        String text = str(replyText);
        if (text.isEmpty() || text.length() > 1000) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "回复内容必填且不超过 1000 字");
        }
        if (feedbackMapper.reply(id, text, replierId) == 0) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "该反馈已回复过");
        }
        notificationService.sendBiz(feedback.getUserId(), null, "FEEDBACK", "您的反馈已收到回复",
                String.format("%s 回复了您的反馈：%s", replierName, abbr(text)));
        return feedbackMapper.selectById(id);
    }

    private static String typeName(String type) {
        switch (type) {
            case "SUGGEST": return "功能建议";
            case "BUG": return "问题报告";
            case "COMPLAINT": return "服务投诉";
            case "PRAISE": return "表扬鼓励";
            default: return "反馈";
        }
    }

    private static String abbr(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}