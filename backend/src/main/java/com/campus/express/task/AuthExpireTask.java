package com.campus.express.task;

import com.campus.express.entity.Parcel;
import com.campus.express.entity.ProxyAuth;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 授权过期清理任务（每10分钟）：ACTIVE 且 expire_at<=now() → EXPIRED，
 * 并站内信通知被授权人。核销路径本身也带 expire_at 条件（双保险）。
 */
@Component
public class AuthExpireTask {

    private static final Logger log = LoggerFactory.getLogger(AuthExpireTask.class);

    @Value("${campus.task.enabled:true}")
    private boolean enabled;

    private final ProxyAuthMapper authMapper;
    private final ParcelMapper parcelMapper;
    private final NotificationService notificationService;
    private final com.campus.express.service.TraceService traceService;

    public AuthExpireTask(ProxyAuthMapper authMapper, ParcelMapper parcelMapper,
                          NotificationService notificationService,
                          com.campus.express.service.TraceService traceService) {
        this.authMapper = authMapper;
        this.parcelMapper = parcelMapper;
        this.notificationService = notificationService;
        this.traceService = traceService;
    }

    @Scheduled(cron = "${campus.task.expire-cron:0 */10 * * * ?}")
    public void expire() {
        if (!enabled) {
            return;
        }
        try {
            List<ProxyAuth> expired = authMapper.expireOutdated();
            for (ProxyAuth auth : expired) {
                Parcel parcel = parcelMapper.selectById(auth.getParcelId());
                if (parcel != null) {
                    notificationService.sendProxyInvalid(auth.getGranteeId(), auth.getParcelId(),
                            parcel.getTrackingNo(), "已到期自动失效");
                    traceService.system(parcel.getId(), "AUTH_REVOKED", "代取授权到期失效",
                            traceService.locationOf(parcel),
                            "授权码 " + auth.getAuthCode() + " 已过有效期，系统自动置为 EXPIRED");
                }
            }
            if (!expired.isEmpty()) {
                log.info("代取授权过期清理完成: {} 条", expired.size());
            }
        } catch (Exception e) {
            log.error("授权过期清理失败", e);
        }
    }
}