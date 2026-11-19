package com.campus.express.mapper;

import com.campus.express.entity.AdSlot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 广告位 */
public interface AdMapper {

    /** 生效中的广告（status=1 且在投放时间窗内），按权重倒序 */
    List<AdSlot> selectActive();

    List<AdSlot> selectAll();

    AdSlot selectById(@Param("id") Long id);

    int insert(AdSlot ad);

    int update(AdSlot ad);

    int delete(@Param("id") Long id);
}