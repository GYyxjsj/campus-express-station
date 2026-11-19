package com.campus.express.mapper;

import com.campus.express.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperationLogMapper {

    int insert(OperationLog log);

    List<OperationLog> selectPage(@Param("module") String module, @Param("keyword") String keyword,
                                  @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                  @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("module") String module, @Param("keyword") String keyword);
}