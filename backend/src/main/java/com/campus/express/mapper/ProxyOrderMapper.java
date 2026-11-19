package com.campus.express.mapper;

import com.campus.express.entity.ProxyOrder;
import com.campus.express.vo.ProxyOrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 代取服务单 */
public interface ProxyOrderMapper {

    int insert(ProxyOrder order);

    ProxyOrder selectById(@Param("id") Long id);

    ProxyOrderVO selectVoById(@Param("id") Long id);

    /** 该包裹进行中的服务单（OPEN/ACCEPTED/PICKED/DELIVERED） */
    ProxyOrder selectActiveByParcel(@Param("parcelId") Long parcelId);

    ProxyOrder selectByAuthId(@Param("authId") Long authId);

    List<ProxyOrderVO> selectPage(@Param("requesterId") Long requesterId, @Param("runnerId") Long runnerId,
                                  @Param("status") String status, @Param("stationId") Long stationId,
                                  @Param("keyword") String keyword, @Param("excludeRequesterId") Long excludeRequesterId,
                                  @Param("openOnly") Boolean openOnly,
                                  @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                  @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("requesterId") Long requesterId, @Param("runnerId") Long runnerId,
                   @Param("status") String status, @Param("stationId") Long stationId,
                   @Param("keyword") String keyword, @Param("excludeRequesterId") Long excludeRequesterId,
                   @Param("openOnly") Boolean openOnly);

    int setAuthId(@Param("id") Long id, @Param("authId") Long authId);

    /** 接单：OPEN 且未过期且无人接 → ACCEPTED */
    int markAccepted(@Param("id") Long id, @Param("runnerId") Long runnerId);

    /** 跑腿员标记已取件 */
    int markPicked(@Param("id") Long id, @Param("runnerId") Long runnerId);

    /** 柜台代取核销联动：ACCEPTED → PICKED */
    int markPickedBySystem(@Param("id") Long id);

    int markDelivered(@Param("id") Long id, @Param("runnerId") Long runnerId);

    int markDone(@Param("id") Long id);

    int markCancelled(@Param("id") Long id, @Param("reason") String reason);

    int rate(@Param("id") Long id, @Param("requesterId") Long requesterId,
             @Param("rating") Integer rating, @Param("review") String review);

    /** 定时任务：超时无人接单 → EXPIRED（需退款） */
    List<ProxyOrder> expireOpen();

    /** 定时任务：送达 24h 未确认 → 自动 DONE（需结算） */
    List<ProxyOrder> autoConfirm();

    Map<String, Object> stats();

    List<Map<String, Object>> statusDistribution();

    List<Map<String, Object>> topRunners(@Param("limit") int limit);
}