package com.campus.express.mapper;

import com.campus.express.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SysUserMapper {

    SysUser selectById(@Param("id") Long id);

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectByPhone(@Param("phone") String phone);

    int insert(SysUser user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateProfile(@Param("id") Long id, @Param("realName") String realName,
                      @Param("phone") String phone, @Param("email") String email);

    long countAll();

    long countByStatus(@Param("status") Integer status);

    List<SysUser> selectPage(@Param("keyword") String keyword, @Param("roleCode") String roleCode,
                             @Param("status") Integer status,
                             @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                             @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword, @Param("roleCode") String roleCode,
                   @Param("status") Integer status);

    /** 授权选人：学号/姓名模糊搜索（仅启用用户，限量10） */
    List<SysUser> searchStudents(@Param("keyword") String keyword);

    List<String> selectRoleCodes(@Param("userId") Long userId);

    List<String> selectRoleNames(@Param("userId") Long userId);

    List<String> selectPermCodes(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    int deleteUserRoles(@Param("userId") Long userId);

    /** 定时任务：管理员 id 列表（滞留升级告警收件人） */
    List<Long> selectAdminIds();

    /** 注销护栏：名下在库/滞留包裹数 */
    long countActiveParcels(@Param("userId") Long userId);

    /** 注销护栏：进行中代取服务单数（发布人或跑腿员） */
    long countActiveProxyOrders(@Param("userId") Long userId);

    /** 注销护栏：钱包余额（无账户返回 null） */
    BigDecimal selectWalletBalance(@Param("userId") Long userId);

    /** 注销联动：吊销名下生效中的身份条码 */
    int revokeActiveIdentity(@Param("userId") Long userId);

    // ---------- 邀请有礼 ----------

    SysUser selectByInviteCode(@Param("code") String code);

    /** 绑定邀请人（仅未绑定者，条件更新防重复绑定） */
    int bindInviter(@Param("id") Long id, @Param("inviterId") Long inviterId);

    int updateInviteCode(@Param("id") Long id, @Param("code") String code);

    /** 员工 id 列表（客服工单通知收件人） */
    List<Long> selectStaffIds();

    /** 我邀请的用户列表 */
    List<SysUser> selectInvitees(@Param("inviterId") Long inviterId);

    long countInvitees(@Param("inviterId") Long inviterId);
}