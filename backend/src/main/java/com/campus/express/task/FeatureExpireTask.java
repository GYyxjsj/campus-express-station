package com.campus.express.task;

import com.campus.express.entity.IdentityCode;
import com.campus.express.entity.ProxyOrder;
import com.campus.express.entity.RechargeOrder;
import com.campus.express.mapper.CouponMapper;
import com.campus.express.mapper.IdentityCodeMapper;
import com.campus.express.mapper.InsuranceMapper;
import com.campus.express.mapper.ProxyOrderMapper;
import com.campus.express.mapper.RechargeOrderMapper;
import com.campus.express.service.InsuranceService;
import com.campus.express.service.ProxyOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 扩展功能过期清理（每10分钟）：
 * 1) 充值订单超时未支付 → EXPIRED；
 * 2) 身份条码到期 → EXPIRED；
 * 3) 代取服务单超时无人接单 → EXPIRED + 全额退款；
 * 4) 送达 24h 未确认 → 自动完成并结算。
 */
@Component
public class FeatureExpireTask {

    private static final Logger log = LoggerFactory.getLogger(FeatureExpireTask.class);

    @Value("${campus.task.enabled:true}")
    private boolean enabled;

    private final RechargeOrderMapper rechargeOrderMapper;
    private final IdentityCodeMapper identityCodeMapper;
    private final ProxyOrderMapper proxyOrderMapper;
    private final ProxyOrderService proxyOrderService;
    private final InsuranceService insuranceService;
    private final InsuranceMapper insuranceMapper;
    private final CouponMapper couponMapper;

    public FeatureExpireTask(RechargeOrderMapper rechargeOrderMapper, IdentityCodeMapper identityCodeMapper,
                             ProxyOrderMapper proxyOrderMapper, ProxyOrderService proxyOrderService,
                             InsuranceService insuranceService, InsuranceMapper insuranceMapper,
                             CouponMapper couponMapper) {
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.identityCodeMapper = identityCodeMapper;
        this.proxyOrderMapper = proxyOrderMapper;
        this.proxyOrderService = proxyOrderService;
        this.insuranceService = insuranceService;
        this.insuranceMapper = insuranceMapper;
        this.couponMapper = couponMapper;
    }

    @Scheduled(cron = "${campus.task.expire-cron:0 */10 * * * ?}")
    public void expire() {
        if (!enabled) {
            return;
        }
        try {
            List<RechargeOrder> orders = rechargeOrderMapper.expireOutdated();
            if (!orders.isEmpty()) {
                log.info("充值订单超时关闭: {} 笔", orders.size());
            }
        } catch (Exception e) {
            log.error("充值订单过期清理失败", e);
        }
        try {
            List<IdentityCode> codes = identityCodeMapper.expireOutdated();
            if (!codes.isEmpty()) {
                log.info("身份条码过期清理: {} 条", codes.size());
            }
        } catch (Exception e) {
            log.error("身份条码过期清理失败", e);
        }
        try {
            List<ProxyOrder> expired = proxyOrderMapper.expireOpen();
            for (ProxyOrder order : expired) {
                try {
                    proxyOrderService.expireAndRefund(order);
                } catch (Exception e) {
                    log.error("代取服务单退款失败: {}", order.getOrderNo(), e);
                }
            }
            if (!expired.isEmpty()) {
                log.info("代取服务单超时关闭并退款: {} 单", expired.size());
            }
        } catch (Exception e) {
            log.error("代取服务单过期清理失败", e);
        }
        try {
            List<ProxyOrder> autoDone = proxyOrderMapper.autoConfirm();
            for (ProxyOrder order : autoDone) {
                try {
                    proxyOrderService.settleAuto(order);
                } catch (Exception e) {
                    log.error("代取服务单自动结算失败: {}", order.getOrderNo(), e);
                }
            }
            if (!autoDone.isEmpty()) {
                log.info("代取服务单自动确认结算: {} 单", autoDone.size());
            }
        } catch (Exception e) {
            log.error("代取服务单自动确认失败", e);
        }
        try {
            int paid = insuranceService.settleExpired();
            if (paid > 0) {
                log.info("超时保险到期自动理赔: {} 单", paid);
            }
        } catch (Exception e) {
            log.error("超时保险理赔失败", e);
        }
        try {
            int expired = insuranceMapper.expireSettled();
            if (expired > 0) {
                log.info("超时保险到期失效（期内已取件）: {} 单", expired);
            }
        } catch (Exception e) {
            log.error("超时保险到期清理失败", e);
        }
        try {
            int coupons = couponMapper.expireCoupons();
            if (coupons > 0) {
                log.info("卡券到期失效: {} 张", coupons);
            }
        } catch (Exception e) {
            log.error("卡券过期清理失败", e);
        }
    }
}