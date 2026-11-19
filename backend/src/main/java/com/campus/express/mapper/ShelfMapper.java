package com.campus.express.mapper;

import com.campus.express.entity.Shelf;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShelfMapper {

    int insert(Shelf shelf);

    Shelf selectById(@Param("id") Long id);

    List<Shelf> selectByStation(@Param("stationId") Long stationId);

    List<Shelf> selectAll();

    /** 货架当前占用数（在库+滞留） */
    long countOccupied(@Param("shelfId") Long shelfId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}