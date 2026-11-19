package com.campus.express.mapper;

import com.campus.express.entity.Parcel;
import com.campus.express.vo.ParcelVO;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface ParcelMapper {

    int insert(Parcel parcel);

    Parcel selectById(@Param("id") Long id);

    ParcelVO selectVoById(@Param("id") Long id);

    Parcel selectByTrackingNo(@Param("trackingNo") String trackingNo);

    List<ParcelVO> selectPage(@Param("status") String status, @Param("keyword") String keyword,
                              @Param("stationId") Long stationId, @Param("ownerId") Long ownerId,
                              @Param("overdueOnly") Boolean overdueOnly,
                              @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                              @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("status") String status, @Param("keyword") String keyword,
                   @Param("stationId") Long stationId, @Param("ownerId") Long ownerId,
                   @Param("overdueOnly") Boolean overdueOnly);

    /**
     * 核销（本人/代取共用）：前置状态 + 取件码条件更新，防并发重复核销。
     * @return 影响行数（0=取件码错误或已出库）
     */
    int markPickedUp(@Param("id") Long id, @Param("pickupCode") String pickupCode);

    /** 退回登记：仅 IN_STOCK/OVERDUE 可退回 */
    int markReturned(@Param("id") Long id, @Param("reason") String reason);

    /** 身份条码核销：柜台已核验身份，免取件码（仅 IN_STOCK/OVERDUE） */
    int markPickedUpByIdentity(@Param("id") Long id);

    /** 移架：更新货架归属 */
    int updateShelf(@Param("id") Long id, @Param("shelfId") Long shelfId);

    /** 定时任务：在库超阈值 → 滞留（返回受影响包裹，供告警通知） */
    List<Parcel> markOverdue(@Param("before") OffsetDateTime before);

    /** 定时任务：待催领包裹（在库/滞留、超阈值、24h 内未催领过） */
    List<Parcel> selectRemindCandidates(@Param("before") OffsetDateTime before,
                                        @Param("remindGapHours") int remindGapHours,
                                        @Param("limit") int limit);

    long countByStatus(@Param("status") String status);

    long countTodayInbound();

    long countTodayOutbound();

    /** 近 N 日出入库趋势 [{day, inbound, outbound}] */
    List<Map<String, Object>> selectTrend(@Param("days") int days);

    /** 状态分布 [{name, value}] */
    List<Map<String, Object>> selectStatusDistribution();

    /** 近 N 周 星期×小时 出入库热力 [{dow, hour, inbound, outbound}]，dow 0=周一 */
    List<Map<String, Object>> selectHeatmap(@Param("weeks") int weeks);

    /** 快递公司占比 [{name, value}] */
    List<Map<String, Object>> selectCourierDistribution();
}