package com.campus.express.service;

import com.campus.express.entity.Notification;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.service.notify.NotificationSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 提醒编排服务：按类型选择渠道组合并发送。
 * ARRIVE/REMIND: 站内信 + 短信 + 邮件（模拟）
 * OVERDUE/PROXY_*: 仅站内信
 */
@Service
public class NotificationService {

    private final Map<String, NotificationSender> senders;
    private final SysUserMapper userMapper;

    public NotificationService(List<NotificationSender> senderList, SysUserMapper userMapper) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
        this.userMapper = userMapper;
    }

    /** 包裹到站提醒（物主） */
    public void sendArrive(Parcel parcel, String stationName, String shelfCode) {
        SysUser owner = userMapper.selectById(parcel.getOwnerId());
        String content = String.format("您的%s包裹 %s 已到达%s，货架 %s，取件码 %s，请及时领取。",
                courierName(parcel.getCourierCompany()), parcel.getTrackingNo(),
                stationName, shelfCode == null ? "待分配" : shelfCode, parcel.getPickupCode());
        dispatch(owner.getId(), parcel.getId(), "ARRIVE", "包裹到站提醒", content, true);
    }

    /** 催领提醒（物主） */
    public void sendRemind(Parcel parcel, String stationName, long hoursInStock) {
        String content = String.format("您的包裹 %s 已在库超过%d小时，请尽快前往%s领取（取件码 %s）。",
                parcel.getTrackingNo(), hoursInStock, stationName, parcel.getPickupCode());
        dispatch(parcel.getOwnerId(), parcel.getId(), "REMIND", "取件催领提醒", content, true);
    }

    /** 滞留告警（物主 + 管理员） */
    public void sendOverdue(Parcel parcel, List<Long> adminIds) {
        String ownerContent = String.format(
                "您的包裹 %s 已滞留超过72小时，请尽快领取，逾期将按驿站规定退回。", parcel.getTrackingNo());
        dispatch(parcel.getOwnerId(), parcel.getId(), "OVERDUE", "包裹滞留告警", ownerContent, false);
        for (Long adminId : adminIds) {
            String adminContent = String.format("包裹 %s（用户#%d）已滞留超72小时，请安排处理。",
                    parcel.getTrackingNo(), parcel.getOwnerId());
            dispatch(adminId, parcel.getId(), "OVERDUE", "滞留件升级告警", adminContent, false);
        }
    }

    /** 代取委托通知（被授权人） */
    public void sendProxyGranted(Long granteeId, Long parcelId, String trackingNo,
                                 String grantorName, String authCode, String expireText) {
        String content = String.format("%s 委托您代取包裹 %s，授权码 %s，有效期至 %s。请携带授权码到驿站取件。",
                grantorName, trackingNo, authCode, expireText);
        dispatch(granteeId, parcelId, "PROXY_GRANTED", "代取委托通知", content, false);
    }

    /** 代取完成通知（物主） */
    public void sendProxyUsed(Long ownerId, Long parcelId, String trackingNo, String granteeName) {
        String content = String.format("您的包裹 %s 已由 %s 凭代取授权成功领取。如非本人意愿请立即联系驿站。",
                trackingNo, granteeName);
        dispatch(ownerId, parcelId, "PROXY_USED", "包裹已被代取", content, false);
    }

    /** 授权撤销/过期通知（被授权人） */
    public void sendProxyInvalid(Long granteeId, Long parcelId, String trackingNo, String reason) {
        String content = String.format("包裹 %s 的代取授权%s，授权码已失效。", trackingNo, reason);
        dispatch(granteeId, parcelId, "PROXY_GRANTED", "代取授权失效通知", content, false);
    }

    /** 通用业务通知（钱包/代取服务/身份核验/协议等），仅站内信 */
    public void sendBiz(Long userId, Long parcelId, String type, String title, String content) {
        dispatch(userId, parcelId, type, title, content, false);
    }

    private void dispatch(Long userId, Long parcelId, String type, String title,
                          String content, boolean multiChannel) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setParcelId(parcelId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        senders.get("IN_APP").send(copy(n));
        if (multiChannel) {
            senders.get("SMS").send(copy(n));
            senders.get("EMAIL").send(copy(n));
        }
    }

    private Notification copy(Notification src) {
        Notification n = new Notification();
        n.setUserId(src.getUserId());
        n.setParcelId(src.getParcelId());
        n.setType(src.getType());
        n.setTitle(src.getTitle());
        n.setContent(src.getContent());
        return n;
    }

    public static String courierName(String code) {
        switch (code) {
            case "SF": return "顺丰";
            case "ZT": return "中通";
            case "YT": return "圆通";
            case "JD": return "京东";
            case "STO": return "申通";
            case "YD": return "韵达";
            case "EMS": return "EMS";
            case "JT": return "极兔";
            default: return code;
        }
    }
}