package com.campus.express.service.notify;

import com.campus.express.entity.Notification;

/**
 * 提醒渠道抽象（策略模式）。
 * 当前提供站内信落库实现与 SMS/EMAIL 日志模拟实现；
 * 接入真实网关（阿里云SMS/SMTP/微信订阅消息）时新增实现类即可，业务层零改动。
 */
public interface NotificationSender {

    /** 渠道编码: IN_APP / SMS / EMAIL */
    String channel();

    /** 发送（站内信=落库；外部渠道=调用网关，当前为日志模拟） */
    void send(Notification notification);
}