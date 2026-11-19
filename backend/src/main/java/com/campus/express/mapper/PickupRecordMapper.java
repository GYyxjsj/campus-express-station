package com.campus.express.mapper;

import com.campus.express.entity.PickupRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PickupRecordMapper {

    int insert(PickupRecord record);

    PickupRecord selectByParcel(@Param("parcelId") Long parcelId);

    List<PickupRecord> selectByOwner(@Param("ownerId") Long ownerId, @Param("limit") int limit);
}