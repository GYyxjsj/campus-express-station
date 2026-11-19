package com.campus.express.service.notify;

import com.campus.express.entity.Notification;
import com.campus.express.mapper.NotificationMapper;
import org.springframework.stereotype.Component;

/** 站内信渠道：落库，前端提醒中心/铃铛未读数消费 */
@Component
public class InAppSender implements NotificationSender {

    private final NotificationMapper notificationMapper;

    public InAppSender(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public String channel() {
        return "IN_APP";
    }

    @Override
    public void send(Notification notification) {
        notification.setChannel(channel());
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }
}