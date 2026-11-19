package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.CouponTemplate;
import com.campus.express.entity.RedeemCode;
import com.campus.express.entity.UserCoupon;
import com.campus.express.mapper.CouponMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 卡券红包：兑换码核销发卡、我的卡券、管理端模板与批量兑换码。 */
@Service
public class CouponService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("COUPON", "RED_PACKET"));
    private static final Set<String> SCOPES = new HashSet<>(Arrays.asList("ALL", "SHIP", "PROXY", "INSURANCE"));

    private final CouponMapper couponMapper;
    private final NotificationService notificationService;

    public CouponService(CouponMapper couponMapper, NotificationService notificationService) {
        this.couponMapper = couponMapper;
        this.notificationService = notificationService;
    }

    public PageResult<UserCoupon> my(Long uid, String status, int page, int size) {
        long total = couponMapper.countMyCoupons(uid, status);
        return new PageResult<>(total, couponMapper.selectMyCoupons(uid, status, (page - 1) * size, size));
    }

    /** 兑换码兑换：条件核销 UNUSED→USED，再扣模板库存发卡，全程一个事务 */
    @Transactional
    public UserCoupon redeem(Long uid, String code) {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (c.isEmpty() || c.length() > 32) {
            throw new BizException(ErrorCodes.REDEEM_INVALID, "请输入有效兑换码");
        }
        RedeemCode redeem = couponMapper.selectRedeemByCode(c);
        if (redeem == null) {
            throw new BizException(ErrorCodes.REDEEM_INVALID, "兑换码不存在，请核对后重试");
        }
        if (!"UNUSED".equals(redeem.getStatus())) {
            throw new BizException(ErrorCodes.REDEEM_INVALID, "兑换码已被使用或已停用");
        }
        if (couponMapper.markRedeemUsed(redeem.getId(), uid) == 0) {
            throw new BizException(ErrorCodes.REDEEM_INVALID, "兑换码已被使用，手慢了");
        }
        UserCoupon coupon = issue(uid, redeem.getTemplateId(), "REDEEM");
        notificationService.sendBiz(uid, null, "COUPON", "兑换成功",
                String.format("兑换码 %s 兑换成功：%s（面值 %s 元），有效期至 %s。",
                        c, coupon.getName(), coupon.getAmount().toPlainString(),
                        coupon.getExpireAt().toLocalDate()));
        return coupon;
    }

    /** 发卡：模板库存条件递增 + 一人一张约束 */
    private UserCoupon issue(Long uid, Long templateId, String source) {
        CouponTemplate template = couponMapper.selectTemplate(templateId);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BizException(ErrorCodes.COUPON_INVALID, "卡券已下架");
        }
        if (couponMapper.selectByTemplateUser(templateId, uid) != null) {
            throw new BizException(ErrorCodes.COUPON_INVALID, "您已领取过该卡券，每种限领一张");
        }
        if (couponMapper.claimTemplate(templateId) == 0) {
            throw new BizException(ErrorCodes.COUPON_INVALID, "卡券已被抢光");
        }
        UserCoupon coupon = new UserCoupon();
        coupon.setTemplateId(templateId);
        coupon.setUserId(uid);
        coupon.setCode(genCode());
        coupon.setStatus("UNUSED");
        coupon.setSource(source);
        int days = template.getValidDays() == null ? 30 : template.getValidDays();
        coupon.setExpireAt(OffsetDateTime.now().plusDays(days));
        try {
            couponMapper.insertUserCoupon(coupon);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCodes.COUPON_INVALID, "您已领取过该卡券，每种限领一张");
        }
        return couponMapper.selectUserCoupon(coupon.getId());
    }

    // ---------- 管理端 ----------

    public PageResult<CouponTemplate> templates(Integer status, int page, int size) {
        long total = couponMapper.countTemplates(status);
        return new PageResult<>(total, couponMapper.selectTemplates(status, (page - 1) * size, size));
    }

    /** 创建模板 + 批量生成兑换码（一次操作产出） */
    @Transactional
    public Map<String, Object> createBatch(Map<String, Object> body) {
        String name = str(body.get("name"));
        String type = str(body.get("type")).toUpperCase();
        String scope = str(body.get("scope")).toUpperCase();
        if (name.isEmpty() || name.length() > 64) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "卡券名称必填且不超过 64 字");
        }
        if (!TYPES.contains(type)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "类型须为 COUPON 或 RED_PACKET");
        }
        if (scope.isEmpty()) { scope = "ALL"; }
        if (!SCOPES.contains(scope)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "适用范围须为 ALL/SHIP/PROXY/INSURANCE");
        }
        BigDecimal amount = decimal(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(new BigDecimal("200")) > 0) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "面值须在 0 ~ 200 元之间");
        }
        BigDecimal minSpend = decimal(body.get("minSpend"));
        if (minSpend == null) { minSpend = BigDecimal.ZERO; }
        int qty = intVal(body.get("quantity"), 10);
        if (qty < 1 || qty > 500) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "批量数量须在 1 ~ 500 之间");
        }
        int validDays = intVal(body.get("validDays"), 30);
        if (validDays < 1 || validDays > 365) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "有效期须在 1 ~ 365 天之间");
        }
        CouponTemplate template = new CouponTemplate();
        template.setName(name);
        template.setType(type);
        template.setAmount(amount);
        template.setMinSpend(minSpend);
        template.setScope(scope);
        template.setTotalQty(qty);
        template.setClaimedQty(0);
        template.setValidDays(validDays);
        template.setDescription(str(body.get("description")));
        template.setStatus(1);
        couponMapper.insertTemplate(template);

        List<String> codes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < qty; i++) {
            String code;
            do {
                code = genCode();
            } while (!seen.add(code));
            RedeemCode rc = new RedeemCode();
            rc.setCode(code);
            rc.setTemplateId(template.getId());
            rc.setStatus("UNUSED");
            couponMapper.insertRedeemCode(rc);
            codes.add(code);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("template", couponMapper.selectTemplate(template.getId()));
        result.put("codes", codes);
        return result;
    }

    public PageResult<RedeemCode> redeemCodes(String status, int page, int size) {
        long total = couponMapper.countRedeemPage(status);
        return new PageResult<>(total, couponMapper.selectRedeemPage(status, (page - 1) * size, size));
    }

    private String genCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) { sb.append('-'); }
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static BigDecimal decimal(Object o) {
        try {
            return o == null ? null : new BigDecimal(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int intVal(Object o, int def) {
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}