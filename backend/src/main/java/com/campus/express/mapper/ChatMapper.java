package com.campus.express.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 好友私聊消息 */
public interface ChatMapper {

    int insert(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId,
               @Param("content") String content);

    /** 会话联系人：好友列表 + 最近一条消息 + 未读数 */
    List<Map<String, Object>> selectConversations(@Param("userId") Long userId);

    /** 与某好友的消息记录（时间正序，最多 limit 条取最近） */
    List<Map<String, Object>> selectMessages(@Param("me") Long me, @Param("peer") Long peer,
                                             @Param("limit") int limit);

    /** 将对方发给我的消息标记已读 */
    int markRead(@Param("me") Long me, @Param("peer") Long peer);

    /** 我的未读私聊总数 */
    long countUnread(@Param("userId") Long userId);
}