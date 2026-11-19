package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.TraceAddDTO;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.ParcelTrace;
import com.campus.express.entity.Shelf;
import com.campus.express.entity.Station;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.ParcelTraceMapper;
import com.campus.express.mapper.ShelfMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.ParcelTraceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 包裹去向台账：每个节点记录时间、地点、检查员（经办人）。
 * 业务动作自动留痕，驿站员可人工补录检查/移架/备注。
 */
@Service
public class TraceService {

    private static final Set<String> MANUAL_NODES =
            new HashSet<>(Arrays.asList("INSPECT", "TRANSFER", "SHELVE", "NOTE"));

    private final ParcelTraceMapper traceMapper;
    private final ParcelMapper parcelMapper;
    private final StationMapper stationMapper;
    private final ShelfMapper shelfMapper;
    private final NotificationService notificationService;

    public TraceService(ParcelTraceMapper traceMapper, ParcelMapper parcelMapper,
                        StationMapper stationMapper, ShelfMapper shelfMapper,
                        NotificationService notificationService) {
        this.traceMapper = traceMapper;
        this.parcelMapper = parcelMapper;
        this.stationMapper = stationMapper;
        this.shelfMapper = shelfMapper;
        this.notificationService = notificationService;
    }

    /** 地点串：驿站名 · 货架号（无货架则用驿站地址） */
    public String locationOf(Parcel parcel) {
        Station station = stationMapper.selectById(parcel.getStationId());
        String base = station == null ? "驿站" : station.getName();
        if (parcel.getShelfId() != null) {
            Shelf shelf = shelfMapper.selectById(parcel.getShelfId());
            if (shelf != null) {
                return base + " · 货架 " + shelf.getCode();
            }
        }
        return station == null ? base : base + " · " + station.getLocation();
    }

    public ParcelTrace record(Long parcelId, String node, String title, String location,
                              Long operatorId, String operatorName, String operatorRole, String detail) {
        ParcelTrace t = new ParcelTrace();
        t.setParcelId(parcelId);
        t.setNode(node);
        t.setTitle(title);
        t.setLocation(location);
        t.setOperatorId(operatorId);
        t.setOperatorName(operatorName);
        t.setOperatorRole(operatorRole);
        t.setDetail(detail);
        t.setHappenedAt(OffsetDateTime.now());
        traceMapper.insert(t);
        return t;
    }

    /** 以当前登录人身份留痕 */
    public ParcelTrace byUser(LoginUser user, Long parcelId, String node, String title,
                              String location, String detail) {
        String role = user.hasRole("ADMIN") ? "ADMIN" : user.hasRole("STAFF") ? "STAFF" : "STUDENT";
        return record(parcelId, node, title, location, user.getUid(), user.getRealName(), role, detail);
    }

    /** 系统自动留痕（定时任务/自动结算） */
    public ParcelTrace system(Long parcelId, String node, String title, String location, String detail) {
        return record(parcelId, node, title, location, null, "系统", "SYSTEM", detail);
    }

    /** 某包裹完整去向：学生仅本人，员工不限 */
    public List<ParcelTrace> ofParcel(LoginUser current, Long parcelId) {
        Parcel parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        boolean staffSide = current.hasRole("STAFF") || current.hasRole("ADMIN");
        if (!staffSide && !parcel.getOwnerId().equals(current.getUid())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权查看该包裹去向");
        }
        return traceMapper.selectByParcel(parcelId);
    }

    public PageResult<ParcelTraceVO> page(Long parcelId, String node, Long operatorId, Long stationId,
                                          String keyword, String from, String to,
                                          String sortKey, String order, int page, int size) {
        long total = traceMapper.countPage(parcelId, node, operatorId, stationId, keyword, from, to);
        return new PageResult<>(total, traceMapper.selectPage(parcelId, node, operatorId, stationId,
                keyword, from, to, com.campus.express.common.Sorts.trace(sortKey),
                com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("stats", traceMapper.stats());
        result.put("nodes", traceMapper.nodeDistribution());
        result.put("inspectors", traceMapper.topInspectors(8));
        result.put("recent", traceMapper.selectRecent(12));
        return result;
    }

    /** 人工登记：检查 / 移架 / 上架 / 备注 */
    @Transactional
    public ParcelTrace add(LoginUser staff, TraceAddDTO dto) {
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        String node = dto.getNode().trim().toUpperCase();
        if (!MANUAL_NODES.contains(node)) {
            throw new BizException(ErrorCodes.TRACE_NODE_INVALID, "不支持的节点类型");
        }
        String location = dto.getLocation().trim();
        String title = dto.getTitle() == null || dto.getTitle().trim().isEmpty()
                ? defaultTitle(node) : dto.getTitle().trim();
        String detail = dto.getDetail();

        if ("TRANSFER".equals(node) || "SHELVE".equals(node)) {
            if (dto.getShelfId() == null) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "请选择目标货架");
            }
            Shelf shelf = shelfMapper.selectById(dto.getShelfId());
            if (shelf == null || !shelf.getStationId().equals(parcel.getStationId())) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "货架不属于该包裹所在驿站");
            }
            if (shelf.getStatus() != null && shelf.getStatus() != 1) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "货架已停用");
            }
            if (!shelf.getId().equals(parcel.getShelfId())
                    && shelfMapper.countOccupied(shelf.getId()) >= shelf.getCapacity()) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "目标货架已满");
            }
            String from = locationOf(parcel);
            parcelMapper.updateShelf(parcel.getId(), shelf.getId());
            parcel.setShelfId(shelf.getId());
            location = stationMapper.selectById(parcel.getStationId()).getName() + " · 货架 " + shelf.getCode();
            detail = (detail == null || detail.isEmpty() ? "" : detail + "；") + "由 " + from + " 移至本货架";
        }

        ParcelTrace trace = byUser(staff, parcel.getId(), node, title, location, detail);
        if ("INSPECT".equals(node)) {
            notificationService.sendBiz(parcel.getOwnerId(), parcel.getId(), "INSPECT", "包裹检查登记",
                    String.format("您的包裹 %s 已由检查员 %s 在「%s」完成检查：%s",
                            parcel.getTrackingNo(), staff.getRealName(), location,
                            detail == null || detail.isEmpty() ? "外观完好" : detail));
        }
        return trace;
    }

    private String defaultTitle(String node) {
        switch (node) {
            case "INSPECT": return "包裹检查";
            case "TRANSFER": return "货架转移";
            case "SHELVE": return "重新上架";
            default: return "驿站备注";
        }
    }
}