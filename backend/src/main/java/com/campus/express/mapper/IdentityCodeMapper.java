package com.campus.express.mapper;

import com.campus.express.entity.IdentityCode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 身份条纹码 */
public interface IdentityCodeMapper {

    int insert(IdentityCode code);

    IdentityCode selectById(@Param("id") Long id);

    IdentityCode selectByCode(@Param("code") String code);

    IdentityCode selectActiveByUser(@Param("userId") Long userId);

    List<IdentityCode> selectPage(@Param("userId") Long userId,
                                  @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId);

    /** 重新生成前作废本人当前生效码 */
    int revokeActiveByUser(@Param("userId") Long userId);

    int markRevoked(@Param("id") Long id, @Param("userId") Long userId);

    /** 核验成功计数（仅 ACTIVE 且未过期） */
    int touchUsed(@Param("id") Long id);

    List<IdentityCode> expireOutdated();
}