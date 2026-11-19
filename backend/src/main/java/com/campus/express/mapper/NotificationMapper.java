package com.campus.express.mapper;

import com.campus.express.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {

    int insert(Notification notification);

    Notification selectById(@Param("id") Long id);

    List<Notification> selectPage(@Param("userId") Long userId, @Param("unreadOnly") Boolean unreadOnly,
                                  @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("unreadOnly") Boolean unreadOnly);

    long countUnread(@Param("userId") Long userId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);

    /** 某包裹最近一条指定类型提醒时间（催领去重用），无则 null */
    java.time.OffsetDateTime lastSentAt(@Param("parcelId") Long parcelId, @Param("type") String type);

    /** 某用户今日已收到的催领条数（每日上限用） */
    long countTodayRemind(@Param("userId") Long userId);
}