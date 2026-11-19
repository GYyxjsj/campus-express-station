package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.entity.Friendship;
import com.campus.express.mapper.ChatMapper;
import com.campus.express.mapper.FriendMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 好友私聊：仅 ACCEPTED 好友之间可互发消息；进入会话即标记已读。 */
@Service
public class ChatService {

    private static final int MAX_CONTENT = 500;
    private static final int MAX_HISTORY = 200;

    private final ChatMapper chatMapper;
    private final FriendMapper friendMapper;

    public ChatService(ChatMapper chatMapper, FriendMapper friendMapper) {
        this.chatMapper = chatMapper;
        this.friendMapper = friendMapper;
    }

    public List<Map<String, Object>> conversations(Long uid) {
        return chatMapper.selectConversations(uid);
    }

    /** 拉取会话记录并顺带标记对方消息已读 */
    @Transactional
    public List<Map<String, Object>> messages(Long uid, Long peerId) {
        requireFriend(uid, peerId);
        chatMapper.markRead(uid, peerId);
        return chatMapper.selectMessages(uid, peerId, MAX_HISTORY);
    }

    @Transactional
    public Map<String, Object> send(Long uid, Long peerId, String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) {
            throw new BizException(ErrorCodes.CHAT_INVALID, "消息内容不能为空");
        }
        if (c.length() > MAX_CONTENT) {
            throw new BizException(ErrorCodes.CHAT_INVALID, "单条消息不能超过 " + MAX_CONTENT + " 字");
        }
        requireFriend(uid, peerId);
        chatMapper.insert(uid, peerId, c);
        List<Map<String, Object>> tail = chatMapper.selectMessages(uid, peerId, 1);
        Map<String, Object> result = new HashMap<>();
        result.put("message", tail.isEmpty() ? null : tail.get(tail.size() - 1));
        return result;
    }

    public long unread(Long uid) {
        return chatMapper.countUnread(uid);
    }

    private void requireFriend(Long uid, Long peerId) {
        if (peerId == null || peerId.equals(uid)) {
            throw new BizException(ErrorCodes.CHAT_INVALID, "会话对象无效");
        }
        Friendship f = friendMapper.selectPair(uid, peerId);
        if (f == null || !"ACCEPTED".equals(f.getStatus())) {
            throw new BizException(ErrorCodes.CHAT_NOT_FRIEND, "你们还不是好友，先在「提醒与好友」添加对方");
        }
    }
}