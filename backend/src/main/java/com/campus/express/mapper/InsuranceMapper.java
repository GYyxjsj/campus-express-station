package com.campus.express.mapper;

import com.campus.express.entity.InsurancePolicy;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 超时保险 */
public interface InsuranceMapper {

    int insert(InsurancePolicy policy);

    InsurancePolicy selectById(@Param("id") Long id);

    InsurancePolicy selectByParcel(@Param("parcelId") Long parcelId);

    List<InsurancePolicy> selectPage(@Param("userId") Long userId, @Param("status") String status,
                                     @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") String status);

    /** 到期未取件自动理赔：ACTIVE 且已过期 且包裹仍在库/滞留 → CLAIM_PAID */
    List<InsurancePolicy> settleExpired();

    /** 单笔置理赔完成（条件更新，防重复入账） */
    int markClaimPaid(@Param("id") Long id);

    /** 到期前已取件：ACTIVE 且已过期 → EXPIRED */
    int expireSettled();

    /** 有效期内取件：保单随之失效 */
    int expireByParcel(@Param("parcelId") Long parcelId);
}