package com.campus.express.mapper;

import com.campus.express.entity.Policy;
import com.campus.express.vo.PolicyVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 政策协议与签署记录 */
public interface PolicyMapper {

    int insert(Policy policy);

    int update(Policy policy);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    Policy selectById(@Param("id") Long id);

    Policy selectByCode(@Param("code") String code);

    /** 协议详情（含正文与当前用户签署状态） */
    PolicyVO selectVoByCode(@Param("userId") Long userId, @Param("code") String code);

    PolicyVO selectVoById(@Param("userId") Long userId, @Param("id") Long id);

    /** 协议列表（带当前用户签署状态） */
    List<PolicyVO> selectList(@Param("userId") Long userId, @Param("publishedOnly") boolean publishedOnly,
                              @Param("category") String category, @Param("keyword") String keyword);

    /** 当前用户尚未签署的必签协议 */
    List<PolicyVO> selectPending(@Param("userId") Long userId);

    List<PolicyVO> selectAdminPage(@Param("category") String category, @Param("status") Integer status,
                                   @Param("keyword") String keyword,
                                   @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countAdminPage(@Param("category") String category, @Param("status") Integer status,
                        @Param("keyword") String keyword);

    int insertAcceptance(@Param("userId") Long userId, @Param("policyId") Long policyId,
                         @Param("version") String version, @Param("ip") String ip);

    List<Map<String, Object>> selectAcceptances(@Param("policyId") Long policyId, @Param("keyword") String keyword,
                                                @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                                @Param("offset") int offset, @Param("limit") int limit);

    long countAcceptances(@Param("policyId") Long policyId, @Param("keyword") String keyword);

    Map<String, Object> acceptanceStats();
}