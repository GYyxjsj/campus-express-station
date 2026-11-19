package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.Notification;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.Station;
import com.campus.express.mapper.NotificationMapper;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.security.LoginUser;
import org.springframework.stereotype.Service;

/**
 * 提醒中心查询/已读 + 驿站员手动补发。
 */
@Service
public class ReminderService {

    private final NotificationMapper notificationMapper;
    private final ParcelMapper parcelMapper;
    private final StationMapper stationMapper;
    private final NotificationService notificationService;

    public ReminderService(NotificationMapper notificationMapper, ParcelMapper parcelMapper,
                           StationMapper stationMapper, NotificationService notificationService) {
        this.notificationMapper = notificationMapper;
        this.parcelMapper = parcelMapper;
        this.stationMapper = stationMapper;
        this.notificationService = notificationService;
    }

    public PageResult<Notification> myNotifications(Long uid, Boolean unreadOnly, int page, int size) {
        long total = notificationMapper.countPage(uid, unreadOnly);
        return new PageResult<>(total,
                notificationMapper.selectPage(uid, unreadOnly, (page - 1) * size, size));
    }

    public long unreadCount(Long uid) {
        return notificationMapper.countUnread(uid);
    }

    public void markRead(Long uid, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getUserId().equals(uid)) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权操作该提醒");
        }
        notificationMapper.markRead(id, uid);
    }

    public void markAllRead(Long uid) {
        notificationMapper.markAllRead(uid);
    }

    /** 手动补发：驿站员可补发任意在库/滞留包裹，学生仅可补发本人包裹 */
    public void resend(Long parcelId, LoginUser user) {
        Parcel parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        if (!user.hasPerm("reminder:send") && !user.getUid().equals(parcel.getOwnerId())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "只能补发本人包裹的提醒");
        }
        if (!"IN_STOCK".equals(parcel.getStatus()) && !"OVERDUE".equals(parcel.getStatus())) {
            throw new BizException(ErrorCodes.PARCEL_STATE_INVALID, "包裹已出库，无需补发");
        }
        Station station = stationMapper.selectById(parcel.getStationId());
        String shelfCode = null;
        if (parcel.getShelfId() != null) {
            com.campus.express.vo.ParcelVO vo = parcelMapper.selectVoById(parcel.getId());
            shelfCode = vo == null ? null : vo.getShelfCode();
        }
        notificationService.sendArrive(parcel, station.getName(), shelfCode);
    }
}