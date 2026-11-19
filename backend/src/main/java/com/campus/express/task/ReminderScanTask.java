package com.campus.express.task;

import com.campus.express.entity.Parcel;
import com.campus.express.entity.Station;
import com.campus.express.mapper.NotificationMapper;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 提醒扫描任务（每30分钟）：
 * 1) 滞留升级：在库超 remind.overdue.hours → OVERDUE，通知物主+管理员；
 * 2) 催领：在库/滞留超 remind.first.delay.hours → REMIND 三渠道，
 *    包裹维度 24h 内不重复（SQL 过滤），用户维度每日上限 remind.daily.max。
 * 多实例部署仅一台开启（campus.task.enabled / TASK_ENABLED）。
 */
@Component
public class ReminderScanTask {

    private static final Logger log = LoggerFactory.getLogger(ReminderScanTask.class);
    private static final int SCAN_LIMIT = 200;

    @Value("${campus.task.enabled:true}")
    private boolean enabled;

    private final ParcelMapper parcelMapper;
    private final StationMapper stationMapper;
    private final SysConfigMapper configMapper;
    private final SysUserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final com.campus.express.service.TraceService traceService;

    public ReminderScanTask(ParcelMapper parcelMapper, StationMapper stationMapper,
                            SysConfigMapper configMapper, SysUserMapper userMapper,
                            NotificationMapper notificationMapper, NotificationService notificationService,
                            com.campus.express.service.TraceService traceService) {
        this.parcelMapper = parcelMapper;
        this.stationMapper = stationMapper;
        this.configMapper = configMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.notificationService = notificationService;
        this.traceService = traceService;
    }

    @Scheduled(cron = "${campus.task.reminder-cron:0 */30 * * * ?}")
    public void scan() {
        if (!enabled) {
            return;
        }
        try {
            upgradeOverdue();
        } catch (Exception e) {
            log.error("滞留升级扫描失败", e);
        }
        try {
            sendReminders();
        } catch (Exception e) {
            log.error("催领扫描失败", e);
        }
    }

    private void upgradeOverdue() {
        int overdueHours = configInt("remind.overdue.hours", 72);
        List<Parcel> upgraded = parcelMapper.markOverdue(OffsetDateTime.now().minusHours(overdueHours));
        if (upgraded.isEmpty()) {
            return;
        }
        List<Long> adminIds = userMapper.selectAdminIds();
        for (Parcel parcel : upgraded) {
            notificationService.sendOverdue(parcel, adminIds);
            traceService.system(parcel.getId(), "OVERDUE", "滞留升级",
                    traceService.locationOf(parcel),
                    "在库超过 " + overdueHours + " 小时，系统自动标记滞留并告警物主与管理员");
        }
        log.info("滞留升级完成: {} 件（阈值{}h）", upgraded.size(), overdueHours);
    }

    private void sendReminders() {
        int firstHours = configInt("remind.first.delay.hours", 24);
        int dailyMax = configInt("remind.daily.max", 3);
        List<Parcel> candidates = parcelMapper.selectRemindCandidates(
                OffsetDateTime.now().minusHours(firstHours), 24, SCAN_LIMIT);
        int sent = 0;
        for (Parcel parcel : candidates) {
            if (notificationMapper.countTodayRemind(parcel.getOwnerId()) >= dailyMax) {
                continue; // 每人每日催领上限
            }
            Station station = stationMapper.selectById(parcel.getStationId());
            long hoursInStock = Duration.between(parcel.getInboundTime(), OffsetDateTime.now()).toHours();
            notificationService.sendRemind(parcel, station == null ? "驿站" : station.getName(), hoursInStock);
            traceService.system(parcel.getId(), "REMIND", "系统催领",
                    traceService.locationOf(parcel),
                    "在库 " + hoursInStock + " 小时，已按策略向物主发送催领提醒（站内信/短信/邮件）");
            sent++;
        }
        if (sent > 0) {
            log.info("催领提醒发送完成: {} 条（阈值{}h，每日上限{}）", sent, firstHours, dailyMax);
        }
    }

    private int configInt(String key, int def) {
        try {
            String v = configMapper.selectValue(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}