package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.InsurancePolicy;
import com.campus.express.entity.Parcel;
import com.campus.express.mapper.InsuranceMapper;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 超时保险：在库/滞留包裹投保，到期未取自动理赔入账 */
@Service
public class InsuranceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InsuranceMapper insuranceMapper;
    private final ParcelMapper parcelMapper;
    private final SysConfigMapper configMapper;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public InsuranceService(InsuranceMapper insuranceMapper, ParcelMapper parcelMapper,
                            SysConfigMapper configMapper, WalletService walletService,
                            NotificationService notificationService) {
        this.insuranceMapper = insuranceMapper;
        this.parcelMapper = parcelMapper;
        this.configMapper = configMapper;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    /** 保险方案参数（保费/保额/时效），供前端展示 */
    public Map<String, Object> plan() {
        Map<String, Object> result = new HashMap<>();
        result.put("premium", configDecimal("insurance.premium", "1"));
        result.put("coverage", configDecimal("insurance.coverage", "5"));
        result.put("validHours", configInt("insurance.valid.hours", 168));
        return result;
    }

    public PageResult<InsurancePolicy> my(Long uid, String status, int page, int size) {
        long total = insuranceMapper.countPage(uid, status);
        return new PageResult<>(total, insuranceMapper.selectPage(uid, status, (page - 1) * size, size));
    }

    @Transactional
    public InsurancePolicy buy(Long uid, Long parcelId) {
        Parcel parcel = parcelMapper.selectById(parcelId);
        if (parcel == null || !uid.equals(parcel.getOwnerId())) {
            throw new BizException(ErrorCodes.INSURANCE_INVALID, "包裹不存在或不属于您");
        }
        if (!"IN_STOCK".equals(parcel.getStatus()) && !"OVERDUE".equals(parcel.getStatus())) {
            throw new BizException(ErrorCodes.INSURANCE_INVALID, "仅在库或滞留包裹可投保");
        }
        InsurancePolicy exists = insuranceMapper.selectByParcel(parcelId);
        if (exists != null) {
            throw new BizException(ErrorCodes.INSURANCE_INVALID, "该包裹已投保，每单限投一次");
        }
        BigDecimal premium = configDecimal("insurance.premium", "1");
        BigDecimal coverage = configDecimal("insurance.coverage", "5");
        int validHours = configInt("insurance.valid.hours", 168);

        InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyNo("IN" + OffsetDateTime.now().format(NO_FMT) + (RANDOM.nextInt(9000) + 1000));
        policy.setUserId(uid);
        policy.setParcelId(parcelId);
        policy.setPremium(premium);
        policy.setCoverage(coverage);
        policy.setStatus("ACTIVE");
        policy.setExpireAt(OffsetDateTime.now().plusHours(validHours));
        insuranceMapper.insert(policy);

        walletService.consume(uid, premium, "超时保险保费 " + premium.toPlainString() + " 元",
                "INSURANCE", policy.getId(),
                "保单 " + policy.getPolicyNo() + "，包裹 " + parcel.getTrackingNo()
                        + "，保额 " + coverage.toPlainString() + " 元");
        notificationService.sendBiz(uid, parcelId, "INSURANCE", "超时保险已生效",
                String.format("保单 %s 已生效：包裹 %s 若在 %d 小时内仍未取件，将自动理赔 %s 元至钱包余额；期间取件保单即失效。",
                        policy.getPolicyNo(), parcel.getTrackingNo(), validHours, coverage.toPlainString()));
        return insuranceMapper.selectById(policy.getId());
    }

    /** 定时任务：到期未取件逐单理赔（条件更新防重复入账） */
    @Transactional
    public int settleExpired() {
        List<InsurancePolicy> expired = insuranceMapper.settleExpired();
        int paid = 0;
        for (InsurancePolicy policy : expired) {
            if (insuranceMapper.markClaimPaid(policy.getId()) == 0) {
                continue;
            }
            walletService.income(policy.getUserId(), policy.getCoverage(),
                    "超时保险理赔 " + policy.getCoverage().toPlainString() + " 元",
                    "INSURANCE", policy.getId(),
                    "保单 " + policy.getPolicyNo() + " 到期仍未取件，自动理赔入账");
            notificationService.sendBiz(policy.getUserId(), policy.getParcelId(), "INSURANCE", "超时保险已理赔",
                    String.format("保单 %s 保障期内包裹 %s 仍未取件，理赔金 %s 元已入钱包余额。",
                            policy.getPolicyNo(), policy.getTrackingNo(), policy.getCoverage().toPlainString()));
            paid++;
        }
        return paid;
    }

    private BigDecimal configDecimal(String key, String def) {
        String v = configMapper.selectValue(key);
        try {
            return new BigDecimal(v == null ? def : v);
        } catch (NumberFormatException e) {
            return new BigDecimal(def);
        }
    }

    private int configInt(String key, int def) {
        String v = configMapper.selectValue(key);
        try {
            int n = Integer.parseInt(v == null ? String.valueOf(def) : v.trim());
            return n > 0 ? n : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}