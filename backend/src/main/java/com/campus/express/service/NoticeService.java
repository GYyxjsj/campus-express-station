package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.entity.Notice;
import com.campus.express.mapper.NoticeMapper;
import com.campus.express.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 公告弹窗：登录后拉取未关闭的弹窗公告，按优先级逐条展示，关闭即按用户留痕。
 */
@Service
public class NoticeService {

    private static final int POPUP_LIMIT = 3;

    private final NoticeMapper noticeMapper;
    private final SysConfigMapper configMapper;

    public NoticeService(NoticeMapper noticeMapper, SysConfigMapper configMapper) {
        this.noticeMapper = noticeMapper;
        this.configMapper = configMapper;
    }

    public List<Notice> popup(Long userId) {
        String enabled = configMapper.selectValue("notice.popup.enabled");
        if (enabled != null && !"1".equals(enabled.trim())) {
            return Collections.emptyList();
        }
        return noticeMapper.selectPopup(userId, POPUP_LIMIT);
    }

    @Transactional
    public void dismiss(Long userId, Long noticeId) {
        if (noticeMapper.selectById(noticeId) == null) {
            throw new BizException(ErrorCodes.NOTICE_NOT_FOUND, "公告不存在");
        }
        noticeMapper.insertRead(userId, noticeId);
    }

    @Transactional
    public void dismissAll(Long userId) {
        for (Notice notice : noticeMapper.selectPopup(userId, POPUP_LIMIT)) {
            noticeMapper.insertRead(userId, notice.getId());
        }
    }

    public Notice detail(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
            throw new BizException(ErrorCodes.NOTICE_NOT_FOUND, "公告不存在或已下线");
        }
        return notice;
    }

    public long popupCount(Long userId) {
        return noticeMapper.countPopup(userId);
    }
}