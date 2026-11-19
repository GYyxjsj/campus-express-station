package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.dto.IdentityPickupDTO;
import com.campus.express.entity.IdentityCode;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.PickupRecord;
import com.campus.express.entity.ProxyAuth;
import com.campus.express.entity.ProxyOrder;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.IdentityCodeMapper;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.PickupRecordMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.mapper.ProxyOrderMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.ParcelVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 身份条纹码：学生生成 Code128 条码，柜台扫码核验身份后免取件码出库。
 * 一人同时仅一条生效码（DB 部分唯一索引兜底），重新生成即作废旧码。
 */
@Service
public class IdentityService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final IdentityCodeMapper codeMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final ParcelMapper parcelMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final ProxyAuthMapper proxyAuthMapper;
    private final ProxyOrderMapper proxyOrderMapper;
    private final TraceService traceService;
    private final NotificationService notificationService;
    private final ProxyOrderService proxyOrderService;

    public IdentityService(IdentityCodeMapper codeMapper, SysUserMapper userMapper,
                           SysConfigMapper configMapper, ParcelMapper parcelMapper,
                           PickupRecordMapper pickupRecordMapper, ProxyAuthMapper proxyAuthMapper,
                           ProxyOrderMapper proxyOrderMapper, TraceService traceService,
                           NotificationService notificationService, ProxyOrderService proxyOrderService) {
        this.codeMapper = codeMapper;
        this.userMapper = userMapper;
        this.configMapper = configMapper;
        this.parcelMapper = parcelMapper;
        this.pickupRecordMapper = pickupRecordMapper;
        this.proxyAuthMapper = proxyAuthMapper;
        this.proxyOrderMapper = proxyOrderMapper;
        this.traceService = traceService;
        this.notificationService = notificationService;
        this.proxyOrderService = proxyOrderService;
    }

    /** 我的条码：当前生效码 + 历史 + 待取包裹数 */
    public Map<String, Object> my(Long userId) {
        SysUser user = userMapper.selectById(userId);
        IdentityCode active = codeMapper.selectActiveByUser(userId);
        if (active != null && active.getExpireAt().isBefore(OffsetDateTime.now())) {
            codeMapper.markRevoked(active.getId(), userId);
            active = null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("active", active);
        result.put("history", codeMapper.selectPage(userId, 0, 10));
        result.put("validHours", configInt("identity.code.valid.hours", 168));
        result.put("user", briefUser(user, false));
        result.put("inStock", parcelMapper.countPage("IN_STOCK", null, null, userId, null)
                + parcelMapper.countPage("OVERDUE", null, null, userId, null));
        return result;
    }

    /** 生成（作废旧码） */
    @Transactional
    public IdentityCode generate(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        codeMapper.revokeActiveByUser(userId);
        int hours = configInt("identity.code.valid.hours", 168);
        IdentityCode code = new IdentityCode();
        code.setUserId(userId);
        code.setStatus("ACTIVE");
        code.setExpireAt(OffsetDateTime.now().plusHours(hours));
        for (int attempt = 0; attempt < 5; attempt++) {
            code.setCode(newCode());
            try {
                codeMapper.insert(code);
                return code;
            } catch (DuplicateKeyException e) {
                // 码碰撞或并发生成：重试
            }
        }
        throw new BizException(ErrorCodes.INTERNAL_ERROR, "条码生成失败，请稍后重试");
    }

    @Transactional
    public void revoke(Long userId, Long id) {
        if (codeMapper.markRevoked(id, userId) == 0) {
            throw new BizException(ErrorCodes.IDENTITY_INACTIVE, "条码不存在或已失效");
        }
    }

    /** 柜台核验：返回身份与待取包裹（不含取件码，凭条码即可出库） */
    @Transactional
    public Map<String, Object> verify(LoginUser staff, String rawCode) {
        IdentityCode code = requireActive(rawCode);
        codeMapper.touchUsed(code.getId());
        SysUser owner = userMapper.selectById(code.getUserId());
        List<ParcelVO> parcels = parcelMapper.selectPage(null, null, null, owner.getId(), null, "p.created_at", "DESC", 0, 50);
        parcels.removeIf(p -> !"IN_STOCK".equals(p.getStatus()) && !"OVERDUE".equals(p.getStatus()));
        for (ParcelVO p : parcels) {
            p.setPickupCode(null);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("identity", codeMapper.selectById(code.getId()));
        result.put("user", briefUser(owner, true));
        result.put("parcels", parcels);
        result.put("verifiedBy", staff.getRealName());
        return result;
    }

    /** 凭身份条码出库（免取件码） */
    @Transactional
    public Map<String, Object> pickup(LoginUser staff, IdentityPickupDTO dto) {
        IdentityCode code = requireActive(dto.getIdentityCode());
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        if (!parcel.getOwnerId().equals(code.getUserId())) {
            throw new BizException(ErrorCodes.IDENTITY_OWNER_MISMATCH, "该包裹不属于条码持有人");
        }
        ProxyOrder order = proxyOrderMapper.selectActiveByParcel(parcel.getId());
        if (order != null && !"OPEN".equals(order.getStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                    "该包裹有进行中的代取服务单 " + order.getOrderNo() + "，请先处理服务单");
        }
        if (parcelMapper.markPickedUpByIdentity(parcel.getId()) == 0) {
            throw new BizException(ErrorCodes.PARCEL_STATE_INVALID, "包裹已出库，无法重复核销");
        }
        codeMapper.touchUsed(code.getId());

        PickupRecord record = new PickupRecord();
        record.setParcelId(parcel.getId());
        record.setPickupType("SELF");
        record.setOwnerId(parcel.getOwnerId());
        record.setPickerId(parcel.getOwnerId());
        record.setStaffId(staff.getUid());
        record.setStationId(parcel.getStationId());
        pickupRecordMapper.insert(record);

        ProxyAuth active = proxyAuthMapper.selectActiveByParcel(parcel.getId());
        if (active != null) {
            proxyAuthMapper.markUsed(active.getId());
        }
        if (order != null) {
            // 物主凭身份码自取：OPEN 服务单取消并全额退还托管款（含小费），
            // 复用统一关闭退款逻辑，避免包裹已出库而托管资金悬空
            proxyOrderService.cancelOpenForOwnerPickup(order, "物主本人凭身份条码到站自取");
        }

        SysUser owner = userMapper.selectById(parcel.getOwnerId());
        String location = traceService.locationOf(parcel);
        traceService.byUser(staff, parcel.getId(), "PICKUP_IDENTITY", "身份条码核验出库", location,
                String.format("检查员 %s 扫描身份条码 %s 核验通过，本人取件出库（免取件码）",
                        staff.getRealName(), mask(code.getCode())));
        notificationService.sendBiz(owner.getId(), parcel.getId(), "PROXY_USED", "包裹已出库",
                String.format("您的包裹 %s 已在「%s」凭身份条码由本人领取，经办检查员：%s。",
                        parcel.getTrackingNo(), location, staff.getRealName()));

        Map<String, Object> result = new HashMap<>();
        result.put("parcelId", parcel.getId());
        result.put("trackingNo", parcel.getTrackingNo());
        result.put("pickupType", "IDENTITY");
        result.put("ownerName", owner.getRealName());
        result.put("ownerUsername", owner.getUsername());
        result.put("pickerName", owner.getRealName());
        result.put("inspector", staff.getRealName());
        result.put("location", location);
        return result;
    }

    private IdentityCode requireActive(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请扫描或输入身份条码");
        }
        IdentityCode code = codeMapper.selectByCode(rawCode.trim().toUpperCase());
        if (code == null) {
            throw new BizException(ErrorCodes.IDENTITY_NOT_FOUND, "条码不存在，请确认学生出示的是本系统条码");
        }
        if (!"ACTIVE".equals(code.getStatus())) {
            throw new BizException(ErrorCodes.IDENTITY_INACTIVE, "条码已作废（已重新生成/已撤销/已过期）");
        }
        if (code.getExpireAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCodes.IDENTITY_EXPIRED, "条码已过期，请让学生重新生成");
        }
        return code;
    }

    private Map<String, Object> briefUser(SysUser user, boolean maskPhone) {
        Map<String, Object> m = new HashMap<>();
        if (user == null) {
            return m;
        }
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("phone", maskPhone ? mask(user.getPhone()) : user.getPhone());
        m.put("email", user.getEmail());
        m.put("status", user.getStatus());
        return m;
    }

    public static String mask(String value) {
        if (value == null || value.length() < 7) {
            return value == null ? "" : "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String newCode() {
        StringBuilder sb = new StringBuilder("CE");
        for (int i = 0; i < 10; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private int configInt(String key, int def) {
        String v = configMapper.selectValue(key);
        try {
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}