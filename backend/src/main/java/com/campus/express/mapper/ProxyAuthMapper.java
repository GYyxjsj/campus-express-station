package com.campus.express.mapper;

import com.campus.express.entity.ProxyAuth;
import com.campus.express.vo.ProxyAuthVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProxyAuthMapper {

    int insert(ProxyAuth auth);

    ProxyAuth selectById(@Param("id") Long id);

    ProxyAuthVO selectVoById(@Param("id") Long id);

    ProxyAuth selectByAuthCode(@Param("authCode") String authCode);

    /** 包裹当前生效授权 */
    ProxyAuth selectActiveByParcel(@Param("parcelId") Long parcelId);

    List<ProxyAuthVO> selectPage(@Param("grantorId") Long grantorId, @Param("granteeId") Long granteeId,
                                 @Param("status") String status,
                                 @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("grantorId") Long grantorId, @Param("granteeId") Long granteeId,
                   @Param("status") String status);

    /** 核销：ACTIVE 且未过期 → USED（条件更新） */
    int markUsed(@Param("id") Long id);

    /** 撤销：仅授权人本人、仅 ACTIVE */
    int markRevoked(@Param("id") Long id, @Param("grantorId") Long grantorId);

    /** 定时任务：过期 ACTIVE → EXPIRED（返回受影响授权，供失效通知） */
    List<ProxyAuth> expireOutdated();

    long countByStatus(@Param("status") String status);
}