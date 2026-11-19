package com.campus.express.mapper;

import com.campus.express.entity.RechargeOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 充值订单 */
public interface RechargeOrderMapper {

    int insert(RechargeOrder order);

    RechargeOrder selectById(@Param("id") Long id);

    List<RechargeOrder> selectPage(@Param("userId") Long userId, @Param("status") String status,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") String status);

    List<Map<String, Object>> selectAdminPage(@Param("keyword") String keyword, @Param("status") String status,
                                              @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                              @Param("offset") int offset, @Param("limit") int limit);

    long countAdminPage(@Param("keyword") String keyword, @Param("status") String status);

    /** 支付：PENDING 且未过期 → PAID（条件更新，防重复支付） */
    int markPaid(@Param("id") Long id);

    int markCancelled(@Param("id") Long id, @Param("userId") Long userId);

    /** 定时任务：过期未支付 → EXPIRED */
    List<RechargeOrder> expireOutdated();
}