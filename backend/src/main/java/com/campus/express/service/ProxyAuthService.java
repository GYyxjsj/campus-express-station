package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.ProxyAuthCreateDTO;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.ProxyAuth;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.mapper.ProxyOrderMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.vo.ProxyAuthVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 代取授权服务：发起 / 撤销 / 查询。核销在 ParcelService.proxyPickup。
 * 约束：单包裹绑定、不能授权给自己、同包裹仅一条 ACTIVE（DB 部分唯一索引兜底）、有效期≤配置上限。
 */
@Service
public class ProxyAuthService {

    /** 去除易混淆字符 0/O/1/I */
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProxyAuthMapper authMapper;
    private final ProxyOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final NotificationService notificationService;
    private final TraceService traceService;

    public ProxyAuthService(ProxyAuthMapper authMapper, ProxyOrderMapper orderMapper,
                            ParcelMapper parcelMapper,
                            SysUserMapper userMapper, SysConfigMapper configMapper,
                            NotificationService notificationService, TraceService traceService) {
        this.authMapper = authMapper;
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.userMapper = userMapper;
        this.configMapper = configMapper;
        this.notificationService = notificationService;
        this.traceService = traceService;
    }

    @Transactional
    public ProxyAuthVO create(Long grantorId, ProxyAuthCreateDTO dto) {
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        if (!parcel.getOwnerId().equals(grantorId)) {
            throw new BizException(ErrorCodes.FORBIDDEN, "只能授权本人的包裹");
        }
        if (!"IN_STOCK".equals(parcel.getStatus()) && !"OVERDUE".equals(parcel.getStatus())) {
            throw new BizException(ErrorCodes.PARCEL_STATE_INVALID, "包裹已出库/退回，无法授权");
        }
        // 已有进行中的代取服务单（托管款已扣）时不允许再授权他人，避免同一包裹出现两条取件路径
        if (orderMapper.selectActiveByParcel(parcel.getId()) != null) {
            throw new BizException(ErrorCodes.ORDER_ALREADY_ACTIVE,
                    "该包裹已有进行中的代取服务单，请先完成或取消");
        }
        SysUser grantee = userMapper.selectByUsername(dto.getGranteeUsername());
        if (grantee == null || grantee.getStatus() != 1) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "被授权人学号不存在或已停用");
        }
        if (grantee.getId().equals(grantorId)) {
            throw new BizException(ErrorCodes.AUTH_SELF_FORBIDDEN, "不能授权给自己");
        }
        if (authMapper.selectActiveByParcel(parcel.getId()) != null) {
            throw new BizException(ErrorCodes.AUTH_ALREADY_ACTIVE, "该包裹已有生效中的授权，请先撤销");
        }
        int maxHours = configInt("auth.max.valid.hours", 72);
        int validHours = Math.min(dto.getValidHours(), maxHours);

        ProxyAuth auth = new ProxyAuth();
        auth.setParcelId(parcel.getId());
        auth.setGrantorId(grantorId);
        auth.setGranteeId(grantee.getId());
        auth.setExpireAt(OffsetDateTime.now().plusHours(validHours));
        auth.setRemark(dto.getRemark());
        // 授权码唯一冲突时重试（概率极低）
        for (int i = 0; ; i++) {
            auth.setAuthCode(genAuthCode());
            try {
                authMapper.insert(auth);
                break;
            } catch (DuplicateKeyException e) {
                if (i >= 4) {
                    throw new BizException(ErrorCodes.INTERNAL_ERROR, "授权码生成冲突，请重试");
                }
                // 若是"同包裹 ACTIVE 唯一索引"冲突，转业务错误
                if (authMapper.selectActiveByParcel(parcel.getId()) != null) {
                    throw new BizException(ErrorCodes.AUTH_ALREADY_ACTIVE, "该包裹已有生效中的授权");
                }
            }
        }
        SysUser grantor = userMapper.selectById(grantorId);
        notificationService.sendProxyGranted(grantee.getId(), parcel.getId(), parcel.getTrackingNo(),
                grantor.getRealName(), auth.getAuthCode(), auth.getExpireAt().format(FMT));
        traceService.record(parcel.getId(), "AUTH_GRANTED", "发起代取授权",
                traceService.locationOf(parcel), grantorId, grantor.getRealName(), "STUDENT",
                String.format("授权 %s 代取，授权码 %s，有效期至 %s",
                        grantee.getRealName(), auth.getAuthCode(), auth.getExpireAt().format(FMT)));
        return authMapper.selectVoById(auth.getId());
    }

    @Transactional
    public void revoke(Long grantorId, Long authId) {
        ProxyAuth auth = authMapper.selectById(authId);
        if (auth == null) {
            throw new BizException(ErrorCodes.AUTH_NOT_FOUND, "授权不存在");
        }
        if (!auth.getGrantorId().equals(grantorId)) {
            throw new BizException(ErrorCodes.FORBIDDEN, "只能撤销本人发起的授权");
        }
        int rows = authMapper.markRevoked(authId, grantorId);
        if (rows == 0) {
            throw new BizException(ErrorCodes.AUTH_INACTIVE, "授权已失效，无法撤销");
        }
        Parcel parcel = parcelMapper.selectById(auth.getParcelId());
        notificationService.sendProxyInvalid(auth.getGranteeId(), auth.getParcelId(),
                parcel.getTrackingNo(), "已被授权人撤销");
        SysUser grantor = userMapper.selectById(grantorId);
        traceService.record(parcel.getId(), "AUTH_REVOKED", "撤销代取授权",
                traceService.locationOf(parcel), grantorId,
                grantor == null ? "授权人" : grantor.getRealName(), "STUDENT",
                "授权码 " + auth.getAuthCode() + " 已由授权人主动撤销");
    }

    public PageResult<ProxyAuthVO> granted(Long uid, String status, int page, int size) {
        long total = authMapper.countPage(uid, null, status);
        return new PageResult<>(total,
                authMapper.selectPage(uid, null, status, (page - 1) * size, size));
    }

    public PageResult<ProxyAuthVO> received(Long uid, String status, int page, int size) {
        long total = authMapper.countPage(null, uid, status);
        return new PageResult<>(total,
                authMapper.selectPage(null, uid, status, (page - 1) * size, size));
    }

    public ProxyAuthVO detail(Long uid, boolean staffSide, Long authId) {
        ProxyAuthVO vo = authMapper.selectVoById(authId);
        if (vo == null) {
            throw new BizException(ErrorCodes.AUTH_NOT_FOUND, "授权不存在");
        }
        if (!staffSide && !vo.getGrantorId().equals(uid) && !vo.getGranteeId().equals(uid)) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权查看该授权");
        }
        return vo;
    }

    private int configInt(String key, int def) {
        try {
            String v = configMapper.selectValue(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String genAuthCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }
}