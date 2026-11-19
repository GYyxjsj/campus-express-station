package com.campus.express.service.notify;

import com.campus.express.entity.Notification;
import com.campus.express.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 邮件渠道（模拟实现）：记录发送流水，日志输出。生产可替换为 JavaMailSender/SMTP。
 */
@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final NotificationMapper notificationMapper;

    public EmailSender(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void send(Notification notification) {
        log.info("[EMAIL模拟] to user#{}: {} | {}", notification.getUserId(),
                notification.getTitle(), notification.getContent());
        notification.setChannel(channel());
        notification.setIsRead(1);
        notificationMapper.insert(notification);
    }
}