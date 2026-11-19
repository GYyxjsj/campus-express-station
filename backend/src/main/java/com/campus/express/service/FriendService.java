package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.entity.Friendship;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.FriendMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 好友：按学号/手机号申请、接受/拒绝、删除；通知走站内信（与提醒中心合并展示）。 */
@Service
public class FriendService {

    private final FriendMapper friendMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    public FriendService(FriendMapper friendMapper, SysUserMapper userMapper,
                         NotificationService notificationService) {
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public List<Map<String, Object>> myFriends(Long uid) {
        return friendMapper.selectFriends(uid);
    }

    public Map<String, Object> requests(Long uid) {
        Map<String, Object> m = new HashMap<>();
        m.put("incoming", friendMapper.selectIncoming(uid));
        m.put("outgoing", friendMapper.selectOutgoing(uid));
        return m;
    }

    /** 申请：keyword 为学号或手机号 */
    @Transactional
    public void request(LoginUser me, String keyword, String remark) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.isEmpty()) {
            throw new BizException(ErrorCodes.FRIEND_INVALID, "请输入对方学号或手机号");
        }
        SysUser target = userMapper.selectByUsername(k);
        if (target == null) {
            target = userMapper.selectByPhone(k);
        }
        if (target == null || target.getStatus() == null || target.getStatus() != 1) {
            throw new BizException(ErrorCodes.FRIEND_INVALID, "未找到该同学（需已注册且账号启用）");
        }
        if (target.getId().equals(me.getUid())) {
            throw new BizException(ErrorCodes.FRIEND_INVALID, "不能添加自己为好友");
        }
        if (friendMapper.selectPair(me.getUid(), target.getId()) != null) {
            throw new BizException(ErrorCodes.FRIEND_EXISTS, "你们已是好友或申请正在处理中");
        }
        Friendship f = new Friendship();
        f.setUserId(me.getUid());
        f.setFriendId(target.getId());
        f.setStatus("PENDING");
        f.setRemark(remark == null || remark.trim().isEmpty() ? null : remark.trim());
        friendMapper.insert(f);
        notificationService.sendBiz(target.getId(), null, "FRIEND", "好友申请",
                String.format("%s（%s）请求添加您为好友%s。请到「提醒中心 · 好友」处理。",
                        me.getRealName(), me.getUsername(),
                        f.getRemark() == null ? "" : "，留言：" + f.getRemark()));
    }

    @Transactional
    public void respond(LoginUser me, Long id, boolean accept) {
        Friendship f = friendMapper.selectById(id);
        if (f == null || !f.getFriendId().equals(me.getUid()) || !"PENDING".equals(f.getStatus())) {
            throw new BizException(ErrorCodes.FRIEND_NOT_FOUND, "申请不存在或已处理");
        }
        if (accept) {
            if (friendMapper.accept(id) == 0) {
                throw new BizException(ErrorCodes.FRIEND_NOT_FOUND, "申请已处理");
            }
            notificationService.sendBiz(f.getUserId(), null, "FRIEND", "好友申请已通过",
                    String.format("%s 已接受您的好友申请，现在可以互相委托代取了。", me.getRealName()));
        } else {
            friendMapper.delete(id);
            notificationService.sendBiz(f.getUserId(), null, "FRIEND", "好友申请未通过",
                    String.format("%s 婉拒了您的好友申请。", me.getRealName()));
        }
    }

    @Transactional
    public void remove(Long uid, Long id) {
        Friendship f = friendMapper.selectById(id);
        if (f == null || (!f.getUserId().equals(uid) && !f.getFriendId().equals(uid))) {
            throw new BizException(ErrorCodes.FRIEND_NOT_FOUND, "好友关系不存在");
        }
        friendMapper.delete(id);
    }
}