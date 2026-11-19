package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.CouponTemplate;
import com.campus.express.entity.RedeemCode;
import com.campus.express.entity.UserCoupon;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.CouponService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 卡券红包与兑换码：学生兑换/查看，管理员建模板批量发码 */
@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/api/coupons/my")
    public R<PageResult<UserCoupon>> my(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String status) {
        return R.ok(couponService.my(UserContext.require().getUid(), status, page, Math.min(size, 100)));
    }

    @PostMapping("/api/coupons/redeem")
    @OpLog(module = "coupon", action = "兑换卡券")
    public R<UserCoupon> redeem(@RequestBody Map<String, String> body) {
        return R.ok(couponService.redeem(UserContext.require().getUid(), body.get("code")));
    }

    @GetMapping("/api/admin/coupons")
    @RequirePerm("coupon:manage")
    public R<PageResult<CouponTemplate>> templates(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) Integer status) {
        return R.ok(couponService.templates(status, page, Math.min(size, 100)));
    }

    @PostMapping("/api/admin/coupons/batch")
    @RequirePerm("coupon:manage")
    @OpLog(module = "coupon", action = "创建卡券模板与兑换码")
    public R<Map<String, Object>> batch(@RequestBody Map<String, Object> body) {
        return R.ok(couponService.createBatch(body));
    }

    @GetMapping("/api/admin/coupons/codes")
    @RequirePerm("coupon:manage")
    public R<PageResult<RedeemCode>> codes(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String status) {
        return R.ok(couponService.redeemCodes(status, page, Math.min(size, 100)));
    }
}