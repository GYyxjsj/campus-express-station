package com.campus.express.mapper;

import com.campus.express.entity.Station;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StationMapper {

    int insert(Station station);

    Station selectById(@Param("id") Long id);

    List<Station> selectAll();

    int update(Station station);
}