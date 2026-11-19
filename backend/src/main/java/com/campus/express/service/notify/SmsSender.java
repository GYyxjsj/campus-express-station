package com.campus.express.service.notify;

import com.campus.express.entity.Notification;
import com.campus.express.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 短信渠道（模拟实现）：记录发送流水到 notification(channel=SMS)，日志输出。
 * 生产接入真实短信网关时替换 send 内部实现（保留流水落库）。
 */
@Component
public class SmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final NotificationMapper notificationMapper;

    public SmsSender(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void send(Notification notification) {
        log.info("[SMS模拟] to user#{}: {} | {}", notification.getUserId(),
                notification.getTitle(), notification.getContent());
        notification.setChannel(channel());
        notification.setIsRead(1); // 外部渠道无需站内已读
        notificationMapper.insert(notification);
    }
}