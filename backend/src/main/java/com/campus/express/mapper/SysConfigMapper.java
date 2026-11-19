package com.campus.express.mapper;

import com.campus.express.entity.SysConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysConfigMapper {

    String selectValue(@Param("key") String key);

    List<SysConfig> selectAll();

    int upsert(@Param("key") String key, @Param("value") String value);
}