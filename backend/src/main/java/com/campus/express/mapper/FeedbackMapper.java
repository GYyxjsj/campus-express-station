package com.campus.express.mapper;

import com.campus.express.entity.Feedback;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 意见反馈 */
public interface FeedbackMapper {

    int insert(Feedback feedback);

    Feedback selectById(@Param("id") Long id);

    List<Feedback> selectPage(@Param("userId") Long userId, @Param("status") String status,
                              @Param("type") String type,
                              @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") String status,
                   @Param("type") String type);

    /** 回复：仅 PENDING → REPLIED */
    int reply(@Param("id") Long id, @Param("reply") String reply, @Param("replierId") Long replierId);
}