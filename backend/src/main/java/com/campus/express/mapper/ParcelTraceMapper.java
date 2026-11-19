package com.campus.express.mapper;

import com.campus.express.entity.ParcelTrace;
import com.campus.express.vo.ParcelTraceVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 包裹去向轨迹台账 */
public interface ParcelTraceMapper {

    int insert(ParcelTrace trace);

    List<ParcelTrace> selectByParcel(@Param("parcelId") Long parcelId);

    List<ParcelTraceVO> selectPage(@Param("parcelId") Long parcelId, @Param("node") String node,
                                   @Param("operatorId") Long operatorId, @Param("stationId") Long stationId,
                                   @Param("keyword") String keyword, @Param("from") String from,
                                   @Param("to") String to,
                                   @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("parcelId") Long parcelId, @Param("node") String node,
                   @Param("operatorId") Long operatorId, @Param("stationId") Long stationId,
                   @Param("keyword") String keyword, @Param("from") String from, @Param("to") String to);

    List<ParcelTraceVO> selectRecent(@Param("limit") int limit);

    Map<String, Object> stats();

    List<Map<String, Object>> nodeDistribution();

    List<Map<String, Object>> topInspectors(@Param("limit") int limit);
}