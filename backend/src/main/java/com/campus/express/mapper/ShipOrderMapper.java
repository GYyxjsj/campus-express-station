package com.campus.express.mapper;

import com.campus.express.entity.ShipOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 服务站自寄单 */
public interface ShipOrderMapper {

    int insert(ShipOrder order);

    ShipOrder selectById(@Param("id") Long id);

    List<ShipOrder> selectPage(@Param("userId") Long userId, @Param("status") String status,
                               @Param("keyword") String keyword,
                               @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") String status,
                   @Param("keyword") String keyword);

    /** 学生交件到站：CREATED → DROPPED */
    int markDropped(@Param("id") Long id);

    /** 驿站寄出：DROPPED → SHIPPED，补运单号 */
    int markShipped(@Param("id") Long id, @Param("trackingNo") String trackingNo);

    /** 取消：CREATED/DROPPED → CANCELLED */
    int markCancelled(@Param("id") Long id);
}