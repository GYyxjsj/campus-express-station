package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.common.Sorts;
import com.campus.express.dto.ParcelInboundDTO;
import com.campus.express.dto.PickupDTO;
import com.campus.express.dto.ProxyPickupDTO;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.PickupRecord;
import com.campus.express.entity.ProxyAuth;
import com.campus.express.entity.Shelf;
import com.campus.express.entity.Station;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.InsuranceMapper;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.PickupRecordMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.mapper.ShelfMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.ParcelVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 包裹服务：入库、查询、核销（本人/代取）、退回。
 * 并发安全：核销/退回使用带前置状态的条件 UPDATE，影响行数判定成败。
 */
@Service
public class ParcelService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParcelMapper parcelMapper;
    private final SysUserMapper userMapper;
    private final StationMapper stationMapper;
    private final ShelfMapper shelfMapper;
    private final ProxyAuthMapper proxyAuthMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final NotificationService notificationService;
    private final TraceService traceService;
    private final ProxyOrderService proxyOrderService;
    private final InsuranceMapper insuranceMapper;

    public ParcelService(ParcelMapper parcelMapper, SysUserMapper userMapper,
                         StationMapper stationMapper, ShelfMapper shelfMapper,
                         ProxyAuthMapper proxyAuthMapper, PickupRecordMapper pickupRecordMapper,
                         NotificationService notificationService, TraceService traceService,
                         ProxyOrderService proxyOrderService, InsuranceMapper insuranceMapper) {
        this.parcelMapper = parcelMapper;
        this.userMapper = userMapper;
        this.stationMapper = stationMapper;
        this.shelfMapper = shelfMapper;
        this.proxyAuthMapper = proxyAuthMapper;
        this.pickupRecordMapper = pickupRecordMapper;
        this.notificationService = notificationService;
        this.traceService = traceService;
        this.proxyOrderService = proxyOrderService;
        this.insuranceMapper = insuranceMapper;
    }

    /** 入库：手机号匹配学生 → 生成取件码 → 落库 → 多渠道到站提醒 */
    @Transactional
    public ParcelVO inbound(ParcelInboundDTO dto) {
        if (parcelMapper.selectByTrackingNo(dto.getTrackingNo()) != null) {
            throw new BizException(ErrorCodes.TRACKING_EXISTS, "该运单号已入库，请勿重复登记");
        }
        SysUser owner = userMapper.selectByPhone(dto.getOwnerPhone());
        if (owner == null) {
            throw new BizException(ErrorCodes.OWNER_PHONE_NOT_MATCH,
                    "收件手机号未匹配到注册学生，请确认学生已注册且手机号一致");
        }
        Station station = stationMapper.selectById(dto.getStationId());
        if (station == null || station.getStatus() != 1) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "驿站不存在或已停用");
        }
        String shelfCode = null;
        if (dto.getShelfId() != null) {
            Shelf shelf = shelfMapper.selectById(dto.getShelfId());
            if (shelf == null || !shelf.getStationId().equals(dto.getStationId())) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "货架不属于所选驿站");
            }
            if (shelfMapper.countOccupied(shelf.getId()) >= shelf.getCapacity()) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "货架已满，请更换货架");
            }
            shelfCode = shelf.getCode();
        }
        Parcel parcel = new Parcel();
        parcel.setTrackingNo(dto.getTrackingNo().trim().toUpperCase());
        parcel.setCourierCompany(dto.getCourierCompany());
        parcel.setOwnerId(owner.getId());
        parcel.setStationId(dto.getStationId());
        parcel.setShelfId(dto.getShelfId());
        parcel.setPickupCode(genPickupCode());
        parcel.setStatus("IN_STOCK");
        parcel.setRemark(dto.getRemark());
        try {
            parcelMapper.insert(parcel);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCodes.TRACKING_EXISTS, "该运单号已入库，请勿重复登记");
        }
        notificationService.sendArrive(parcel, station.getName(), shelfCode);
        LoginUser operator = UserContext.get();
        String location = station.getName() + (shelfCode == null ? " · 待分配货架" : " · 货架 " + shelfCode);
        traceService.record(parcel.getId(), "ARRIVE", "快递到站", station.getName(),
                null, NotificationService.courierName(parcel.getCourierCompany()) + "快递员", "COURIER",
                "运单 " + parcel.getTrackingNo() + " 由" + NotificationService.courierName(parcel.getCourierCompany())
                        + "送达驿站，收件人 " + owner.getRealName());
        if (operator != null) {
            traceService.byUser(operator, parcel.getId(), "SHELVE", "入库上架", location,
                    String.format("检查员 %s 核验外包装完好后上架，取件码 %s，已发送到站提醒",
                            operator.getRealName(), parcel.getPickupCode()));
        } else {
            traceService.system(parcel.getId(), "SHELVE", "入库上架", location,
                    "取件码 " + parcel.getPickupCode() + "，已发送到站提醒");
        }
        return parcelMapper.selectVoById(parcel.getId());
    }

    /** 分页查询：学生强制只看本人；员工可按条件查全部 */
    public PageResult<ParcelVO> page(LoginUser current, String status, String keyword,
                                     Long stationId, Boolean overdueOnly,
                                     String sortKey, String order, int page, int size) {
        Long ownerId = current.hasRole("STUDENT") && !current.hasRole("STAFF")
                && !current.hasRole("ADMIN") ? current.getUid() : null;
        long total = parcelMapper.countPage(status, keyword, stationId, ownerId, overdueOnly);
        return new PageResult<>(total,
                parcelMapper.selectPage(status, keyword, stationId, ownerId, overdueOnly,
                        Sorts.parcel(sortKey), Sorts.dir(order), (page - 1) * size, size));
    }

    public PageResult<ParcelVO> myParcels(Long uid, String status, String keyword, int page, int size) {
        long total = parcelMapper.countPage(status, keyword, null, uid, null);
        return new PageResult<>(total,
                parcelMapper.selectPage(status, keyword, null, uid, null,
                        Sorts.parcel(null), Sorts.dir(null), (page - 1) * size, size));
    }

    /** 详情：学生仅本人包裹；取件码仅物主/员工可见 */
    public ParcelVO detail(LoginUser current, Long id) {
        ParcelVO vo = parcelMapper.selectVoById(id);
        if (vo == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        boolean staffSide = current.hasRole("STAFF") || current.hasRole("ADMIN");
        if (!staffSide && !vo.getOwnerId().equals(current.getUid())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权查看该包裹");
        }
        return vo;
    }    /** 本人取件核销（驿站员经办） */
    @Transactional
    public Map<String, Object> pickup(LoginUser staff, PickupDTO dto) {
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        // 进行中的代取服务单（托管款未结算）不允许物主本人直接取件：
        // 否则包裹出库而服务单悬空，托管资金无人结算，跑腿员白跑
        if (proxyOrderService.hasActiveByParcel(parcel.getId())) {
            throw new BizException(ErrorCodes.ORDER_ALREADY_ACTIVE,
                    "该包裹存在进行中的代取服务单，请先取消服务单后再本人取件");
        }
        int rows = parcelMapper.markPickedUp(parcel.getId(), dto.getPickupCode());
        if (rows == 0) {
            throw new BizException(ErrorCodes.PICKUP_CODE_INVALID, "取件码错误或包裹已出库");
        }
        // 保障期内取件 → 保单随之失效（不理赔）
        insuranceMapper.expireByParcel(parcel.getId());
        PickupRecord record = new PickupRecord();
        record.setParcelId(parcel.getId());
        record.setPickupType("SELF");
        record.setOwnerId(parcel.getOwnerId());
        record.setPickerId(parcel.getOwnerId());
        record.setStaffId(staff.getUid());
        record.setStationId(parcel.getStationId());
        pickupRecordMapper.insert(record);
        // 若存在生效授权，随出库一并失效（避免遗留 ACTIVE 授权）
        ProxyAuth active = proxyAuthMapper.selectActiveByParcel(parcel.getId());
        if (active != null) {
            proxyAuthMapper.markUsed(active.getId());
        }
        traceService.byUser(staff, parcel.getId(), "PICKUP", "本人取件出库",
                traceService.locationOf(parcel),
                String.format("检查员 %s 核验取件码 %s 通过，物主 %s 本人领取",
                        staff.getRealName(), dto.getPickupCode(), nameOf(parcel.getOwnerId())));
        return pickupResult(parcel, "SELF", null);
    }

    /**
     * 代取核销（驿站员经办）：授权码 + 取件码双凭证。
     * 事务内：parcel→PICKED_UP、proxy_auth→USED、pickup_record(PROXY)、通知物主。
     */
    @Transactional
    public Map<String, Object> proxyPickup(LoginUser staff, ProxyPickupDTO dto) {
        ProxyAuth auth = proxyAuthMapper.selectByAuthCode(dto.getAuthCode());
        if (auth == null) {
            throw new BizException(ErrorCodes.AUTH_NOT_FOUND, "授权码不存在");
        }
        if (!"ACTIVE".equals(auth.getStatus())) {
            throw new BizException(ErrorCodes.AUTH_INACTIVE, "授权已失效（已使用/已撤销/已过期）");
        }
        Parcel parcel = parcelMapper.selectById(auth.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "授权关联包裹不存在");
        }
        if (!parcel.getPickupCode().equals(dto.getPickupCode())) {
            throw new BizException(ErrorCodes.AUTH_CODE_MISMATCH, "取件码与授权包裹不匹配");
        }
        // 先核销授权（ACTIVE+未过期条件更新），失败即授权过期/已被并发使用
        int authRows = proxyAuthMapper.markUsed(auth.getId());
        if (authRows == 0) {
            throw new BizException(ErrorCodes.AUTH_INACTIVE, "授权已过期或已被使用");
        }
        int rows = parcelMapper.markPickedUp(parcel.getId(), dto.getPickupCode());
        if (rows == 0) {
            // 包裹已出库 → 整体回滚（授权恢复 ACTIVE）
            throw new BizException(ErrorCodes.PICKUP_CODE_INVALID, "包裹已出库，无法重复核销");
        }
        insuranceMapper.expireByParcel(parcel.getId());
        PickupRecord record = new PickupRecord();
        record.setParcelId(parcel.getId());
        record.setPickupType("PROXY");
        record.setOwnerId(auth.getGrantorId());
        record.setPickerId(auth.getGranteeId());
        record.setAuthId(auth.getId());
        record.setStaffId(staff.getUid());
        record.setStationId(parcel.getStationId());
        pickupRecordMapper.insert(record);

        SysUser grantee = userMapper.selectById(auth.getGranteeId());
        notificationService.sendProxyUsed(auth.getGrantorId(), parcel.getId(),
                parcel.getTrackingNo(), grantee.getRealName());
        traceService.byUser(staff, parcel.getId(), "PICKUP", "代取授权出库",
                traceService.locationOf(parcel),
                String.format("检查员 %s 核验授权码 %s + 取件码通过，代取人 %s（物主 %s）",
                        staff.getRealName(), dto.getAuthCode(), grantee.getRealName(),
                        nameOf(auth.getGrantorId())));
        proxyOrderService.pickedByAuth(auth.getId(), staff);
        return pickupResult(parcel, "PROXY", grantee.getRealName());
    }

    /** 退回登记 */
    @Transactional
    public void returnParcel(Long id, String reason) {
        Parcel parcel = parcelMapper.selectById(id);
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        // 与服务单托管互斥：退回会导致包裹离站，托管款与服务单将失去结算对象
        if (proxyOrderService.hasActiveByParcel(id)) {
            throw new BizException(ErrorCodes.ORDER_ALREADY_ACTIVE,
                    "该包裹存在进行中的代取服务单，请先取消服务单后再退回");
        }
        int rows = parcelMapper.markReturned(id, reason);
        if (rows == 0) {
            throw new BizException(ErrorCodes.PARCEL_STATE_INVALID, "当前状态不允许退回");
        }
        insuranceMapper.expireByParcel(id);
        ProxyAuth active = proxyAuthMapper.selectActiveByParcel(id);
        if (active != null) {
            proxyAuthMapper.markRevoked(active.getId(), active.getGrantorId());
            notificationService.sendProxyInvalid(active.getGranteeId(), id,
                    parcel.getTrackingNo(), "因包裹退回而撤销");
        }
        LoginUser operator = UserContext.get();
        String location = traceService.locationOf(parcel);
        String detail = "退回原因：" + (reason == null || reason.trim().isEmpty() ? "未填写" : reason.trim());
        if (operator != null) {
            traceService.byUser(operator, parcel.getId(), "RETURN", "包裹退回", location,
                    detail + "，检查员 " + operator.getRealName());
        } else {
            traceService.system(parcel.getId(), "RETURN", "包裹退回", location, detail);
        }
    }

    private String nameOf(Long uid) {
        SysUser user = userMapper.selectById(uid);
        return user == null ? "用户#" + uid : user.getRealName();
    }

    private Map<String, Object> pickupResult(Parcel parcel, String type, String pickerName) {
        SysUser owner = userMapper.selectById(parcel.getOwnerId());
        Map<String, Object> result = new HashMap<>();
        result.put("parcelId", parcel.getId());
        result.put("trackingNo", parcel.getTrackingNo());
        result.put("pickupType", type);
        result.put("ownerName", owner.getRealName());
        result.put("ownerUsername", owner.getUsername());
        result.put("pickerName", pickerName == null ? owner.getRealName() : pickerName);
        return result;
    }

    /** 6 位数字取件码（SecureRandom） */
    private String genPickupCode() {
        int n = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(n);
    }
}