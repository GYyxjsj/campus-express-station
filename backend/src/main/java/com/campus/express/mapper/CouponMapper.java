package com.campus.express.mapper;

import com.campus.express.entity.CouponTemplate;
import com.campus.express.entity.RedeemCode;
import com.campus.express.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 卡券模板 / 用户卡券 / 兑换码 */
public interface CouponMapper {

    CouponTemplate selectTemplate(@Param("id") Long id);

    int insertTemplate(CouponTemplate template);

    List<CouponTemplate> selectTemplates(@Param("status") Integer status,
                                         @Param("offset") int offset, @Param("limit") int limit);

    long countTemplates(@Param("status") Integer status);

    /** 库存条件递增：claimed_qty < total_qty 才 +1 */
    int claimTemplate(@Param("id") Long id);

    // ---------- 用户卡券 ----------

    int insertUserCoupon(UserCoupon coupon);

    UserCoupon selectUserCoupon(@Param("id") Long id);

    UserCoupon selectByTemplateUser(@Param("templateId") Long templateId, @Param("userId") Long userId);

    List<UserCoupon> selectMyCoupons(@Param("userId") Long userId, @Param("status") String status,
                                     @Param("offset") int offset, @Param("limit") int limit);

    long countMyCoupons(@Param("userId") Long userId, @Param("status") String status);

    /** 过期批量置 EXPIRED */
    int expireCoupons();

    // ---------- 兑换码 ----------

    RedeemCode selectRedeemByCode(@Param("code") String code);

    /** 条件核销：仅 UNUSED 可置 USED，防并发重复兑换 */
    int markRedeemUsed(@Param("id") Long id, @Param("userId") Long userId);

    int insertRedeemCode(RedeemCode redeemCode);

    List<RedeemCode> selectRedeemPage(@Param("status") String status,
                                      @Param("offset") int offset, @Param("limit") int limit);

    long countRedeemPage(@Param("status") String status);
}