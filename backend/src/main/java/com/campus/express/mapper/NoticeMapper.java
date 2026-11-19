package com.campus.express.mapper;

import com.campus.express.entity.Notice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoticeMapper {

    int insert(Notice notice);

    Notice selectById(@Param("id") Long id);

    List<Notice> selectPage(@Param("publishedOnly") boolean publishedOnly,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("publishedOnly") boolean publishedOnly);

    int update(Notice notice);

    /** 登录后弹窗：已发布 + is_popup=1，按优先级倒序；是否不再弹由前端本地勾选记录决定 */
    List<Notice> selectPopup(@Param("userId") Long userId, @Param("limit") int limit);

    int insertRead(@Param("userId") Long userId, @Param("noticeId") Long noticeId);

    long countPopup(@Param("userId") Long userId);
}