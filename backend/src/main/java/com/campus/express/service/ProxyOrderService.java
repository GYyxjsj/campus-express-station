package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.ProxyOrderCreateDTO;
import com.campus.express.dto.ProxyOrderRateDTO;
import com.campus.express.entity.Parcel;
import com.campus.express.entity.ProxyAuth;
import com.campus.express.entity.ProxyOrder;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.mapper.ProxyOrderMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.ProxyOrderVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代取服务（有偿跑腿）：发布托管 → 接单自动生成代取授权 → 柜台核销联动取件 → 送达 → 确认结算。
 * 资金：发布时全额托管扣费；完成时跑腿员得总额-平台抽成；取消/超时全额退款。
 * 并发：所有状态迁移均为带前置状态的条件 UPDATE，影响行数判定成败。
 */
@Service
public class ProxyOrderService {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final BigDecimal MAX_TIP = new BigDecimal("50");

    private final ProxyOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final ProxyAuthMapper authMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final WalletService walletService;
    private final TraceService traceService;
    private final NotificationService notificationService;

    public ProxyOrderService(ProxyOrderMapper orderMapper, ParcelMapper parcelMapper,
                             ProxyAuthMapper authMapper, SysUserMapper userMapper,
                             SysConfigMapper configMapper, WalletService walletService,
                             TraceService traceService, NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.authMapper = authMapper;
        this.userMapper = userMapper;
        this.configMapper = configMapper;
        this.walletService = walletService;
        this.traceService = traceService;
        this.notificationService = notificationService;
    }

    // ---------- 查询 ----------

    /** 服务大厅：可接单的公开单（隐藏取件码与联系方式） */
    public PageResult<ProxyOrderVO> hall(Long uid, Long stationId, String keyword, int page, int size) {
        long total = orderMapper.countPage(null, null, null, stationId, keyword, uid, true);
        List<ProxyOrderVO> list = orderMapper.selectPage(null, null, null, stationId, keyword, uid, true,
                "o.created_at", "DESC", (page - 1) * size, size);
        list.forEach(this::hideSecrets);
        return new PageResult<>(total, list);
    }

    public PageResult<ProxyOrderVO> myRequests(Long uid, String status, int page, int size) {
        long total = orderMapper.countPage(uid, null, status, null, null, null, null);
        return new PageResult<>(total,
                orderMapper.selectPage(uid, null, status, null, null, null, null,
                        "o.created_at", "DESC", (page - 1) * size, size));
    }

    public PageResult<ProxyOrderVO> myRuns(Long uid, String status, int page, int size) {
        long total = orderMapper.countPage(null, uid, status, null, null, null, null);
        return new PageResult<>(total,
                orderMapper.selectPage(null, uid, status, null, null, null, null,
                        "o.created_at", "DESC", (page - 1) * size, size));
    }

    public PageResult<ProxyOrderVO> adminPage(String status, Long stationId, String keyword,
                                              String sortKey, String order, int page, int size) {
        long total = orderMapper.countPage(null, null, status, stationId, keyword, null, null);
        return new PageResult<>(total,
                orderMapper.selectPage(null, null, status, stationId, keyword, null, null,
                        com.campus.express.common.Sorts.proxy(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public ProxyOrderVO detail(LoginUser current, Long id) {
        ProxyOrderVO vo = orderMapper.selectVoById(id);
        if (vo == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "服务单不存在");
        }
        boolean staffSide = current.hasRole("STAFF") || current.hasRole("ADMIN");
        boolean participant = current.getUid().equals(vo.getRequesterId())
                || current.getUid().equals(vo.getRunnerId());
        if (!staffSide && !participant) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权查看该服务单");
        }
        if (!staffSide && !current.getUid().equals(vo.getRunnerId())) {
            vo.setPickupCode(null);
        }
        if (!staffSide && !participant) {
            hideSecrets(vo);
        }
        return vo;
    }

    private void hideSecrets(ProxyOrderVO vo) {
        vo.setPickupCode(null);
        vo.setAuthCode(null);
        vo.setRequesterPhone(null);
        vo.setRunnerPhone(null);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("stats", orderMapper.stats());
        result.put("distribution", orderMapper.statusDistribution());
        result.put("topRunners", orderMapper.topRunners(8));
        result.put("feeMin", configDecimal("proxy.service.fee.min", "2"));
        result.put("feeMax", configDecimal("proxy.service.fee.max", "50"));
        result.put("commissionPercent", configInt("proxy.service.commission.percent", 10));
        result.put("maxHours", configInt("proxy.service.max.hours", 48));
        return result;
    }

    /** 发布服务单前的规则参数（前端表单用） */
    /** 该包裹是否存在进行中的服务单（OPEN/ACCEPTED/PICKED/DELIVERED）：供取件核销与授权前置校验 */
    public boolean hasActiveByParcel(Long parcelId) {
        return parcelId != null && orderMapper.selectActiveByParcel(parcelId) != null;
    }

    public Map<String, Object> rules() {        Map<String, Object> result = new HashMap<>();
        result.put("feeMin", configDecimal("proxy.service.fee.min", "2"));
        result.put("feeMax", configDecimal("proxy.service.fee.max", "50"));
        result.put("commissionPercent", configInt("proxy.service.commission.percent", 10));
        result.put("maxHours", configInt("proxy.service.max.hours", 48));
        result.put("maxTip", MAX_TIP);
        return result;
    }

    // ---------- 发布 / 接单 ----------

    @Transactional
    public ProxyOrderVO create(LoginUser current, ProxyOrderCreateDTO dto) {
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BizException(ErrorCodes.PARCEL_NOT_FOUND, "包裹不存在");
        }
        if (!parcel.getOwnerId().equals(current.getUid())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "只能为本人的包裹发布代取服务");
        }
        if (!"IN_STOCK".equals(parcel.getStatus()) && !"OVERDUE".equals(parcel.getStatus())) {
            throw new BizException(ErrorCodes.PARCEL_STATE_INVALID, "包裹已出库/退回，无法发布代取服务");
        }
        if (orderMapper.selectActiveByParcel(parcel.getId()) != null) {
            throw new BizException(ErrorCodes.ORDER_ALREADY_ACTIVE, "该包裹已有进行中的代取服务单");
        }
        if (authMapper.selectActiveByParcel(parcel.getId()) != null) {
            throw new BizException(ErrorCodes.AUTH_ALREADY_ACTIVE,
                    "该包裹已有生效的代取授权，请先撤销授权再发布服务单");
        }
        BigDecimal fee = dto.getFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal min = configDecimal("proxy.service.fee.min", "2");
        BigDecimal max = configDecimal("proxy.service.fee.max", "50");
        if (fee.compareTo(min) < 0 || fee.compareTo(max) > 0) {
            throw new BizException(ErrorCodes.ORDER_FEE_INVALID,
                    "服务费需在 " + min.toPlainString() + " ~ " + max.toPlainString() + " 元之间");
        }
        BigDecimal tip = dto.getTip() == null ? BigDecimal.ZERO : dto.getTip().setScale(2, RoundingMode.HALF_UP);
        if (tip.compareTo(BigDecimal.ZERO) < 0 || tip.compareTo(MAX_TIP) > 0) {
            throw new BizException(ErrorCodes.ORDER_FEE_INVALID, "小费需在 0 ~ " + MAX_TIP.toPlainString() + " 元之间");
        }
        int maxHours = configInt("proxy.service.max.hours", 48);
        int hours = dto.getHours() == null ? 24 : Math.max(1, Math.min(dto.getHours(), maxHours));
        BigDecimal total = fee.add(tip);
        BigDecimal commission = total.multiply(new BigDecimal(configInt("proxy.service.commission.percent", 10)))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        ProxyOrder order = new ProxyOrder();
        order.setOrderNo("PS" + OffsetDateTime.now().format(NO_FMT) + (RANDOM.nextInt(9000) + 1000));
        order.setParcelId(parcel.getId());
        order.setRequesterId(current.getUid());
        order.setFee(fee);
        order.setTip(tip);
        order.setCommission(commission);
        order.setStatus("OPEN");
        order.setDeliverTo(dto.getDeliverTo().trim());
        order.setNote(dto.getNote());
        order.setExpireAt(OffsetDateTime.now().plusHours(hours));
        orderMapper.insert(order);
        // 托管失败（余额不足/冻结）时整单回滚，不留悬空服务单
        walletService.escrow(current.getUid(), total, order.getId(), order.getOrderNo());

        traceService.byUser(current, parcel.getId(), "SERVICE_OPEN", "发布代取服务",
                traceService.locationOf(parcel),
                String.format("服务单 %s，服务费 %s 元 + 小费 %s 元，送达「%s」，%d 小时内有效",
                        order.getOrderNo(), fee.toPlainString(), tip.toPlainString(),
                        order.getDeliverTo(), hours));
        return orderMapper.selectVoById(order.getId());
    }

    @Transactional
    public ProxyOrderVO accept(LoginUser current, Long id) {
        ProxyOrder order = requireOrder(id);
        if (current.getUid().equals(order.getRequesterId())) {
            throw new BizException(ErrorCodes.ORDER_SELF_FORBIDDEN, "不能接自己发布的服务单");
        }
        if (orderMapper.markAccepted(id, current.getUid()) == 0) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "该单已被接走或已超时关闭");
        }
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        ProxyAuth auth = new ProxyAuth();
        auth.setParcelId(parcel.getId());
        auth.setGrantorId(order.getRequesterId());
        auth.setGranteeId(current.getUid());
        auth.setStatus("ACTIVE");
        auth.setExpireAt(order.getExpireAt());
        auth.setRemark("代取服务单 " + order.getOrderNo() + " 自动生成");
        for (int i = 0; ; i++) {
            auth.setAuthCode(genAuthCode());
            try {
                authMapper.insert(auth);
                break;
            } catch (DuplicateKeyException e) {
                if (authMapper.selectActiveByParcel(parcel.getId()) != null) {
                    throw new BizException(ErrorCodes.AUTH_ALREADY_ACTIVE,
                            "该包裹已有生效授权，无法接单，请联系物主撤销");
                }
                if (i >= 4) {
                    throw new BizException(ErrorCodes.INTERNAL_ERROR, "授权码生成冲突，请重试");
                }
            }
        }
        orderMapper.setAuthId(id, auth.getId());
        traceService.byUser(current, parcel.getId(), "SERVICE_ACCEPT", "代取服务接单",
                traceService.locationOf(parcel),
                String.format("%s 接下服务单 %s，授权码 %s，有效期至 %s",
                        current.getRealName(), order.getOrderNo(), auth.getAuthCode(),
                        order.getExpireAt().format(FMT)));
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取服务已接单",
                String.format("您的服务单 %s（包裹 %s）已由 %s 接单，授权码 %s，请等待取件与送达通知。",
                        order.getOrderNo(), parcel.getTrackingNo(), current.getRealName(), auth.getAuthCode()));
        notificationService.sendBiz(current.getUid(), parcel.getId(), "PROXY_ORDER", "接单成功",
                String.format("您已接下服务单 %s，请凭授权码 %s 与取件码到「%s」取件，并送往「%s」。",
                        order.getOrderNo(), auth.getAuthCode(),
                        traceService.locationOf(parcel), order.getDeliverTo()));
        return orderMapper.selectVoById(id);
    }

    // ---------- 履约 ----------

    @Transactional
    public ProxyOrderVO picked(LoginUser current, Long id) {
        ProxyOrder order = requireOrder(id);
        if (orderMapper.markPicked(id, current.getUid()) == 0) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "仅接单人可在已接单状态下标记取件");
        }
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        traceService.byUser(current, parcel.getId(), "SERVICE_PICKED", "代取员已取件",
                traceService.locationOf(parcel), "服务单 " + order.getOrderNo() + " 跑腿员确认已从驿站取件");
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取包裹已取出",
                String.format("服务单 %s：%s 已从驿站取出您的包裹 %s，正在送往「%s」。",
                        order.getOrderNo(), current.getRealName(), parcel.getTrackingNo(), order.getDeliverTo()));
        return orderMapper.selectVoById(id);
    }

    /** 柜台代取核销联动（ParcelService.proxyPickup 调用） */
    @Transactional
    public void pickedByAuth(Long authId, LoginUser staff) {
        ProxyOrder order = orderMapper.selectByAuthId(authId);
        if (order == null || !"ACCEPTED".equals(order.getStatus())) {
            return;
        }
        if (orderMapper.markPickedBySystem(order.getId()) == 0) {
            return;
        }
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        traceService.byUser(staff, parcel.getId(), "SERVICE_PICKED", "柜台核销·代取服务取件",
                traceService.locationOf(parcel),
                String.format("服务单 %s 由驿站员 %s 核销授权码出库，跑腿员：%s",
                        order.getOrderNo(), staff.getRealName(), nameOf(order.getRunnerId())));
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取包裹已取出",
                String.format("服务单 %s：驿站已核销出库，跑腿员 %s 将送往「%s」。",
                        order.getOrderNo(), nameOf(order.getRunnerId()), order.getDeliverTo()));
    }

    @Transactional
    public ProxyOrderVO delivered(LoginUser current, Long id) {
        ProxyOrder order = requireOrder(id);
        if (orderMapper.markDelivered(id, current.getUid()) == 0) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "仅接单人可在已取件状态下标记送达");
        }
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        traceService.byUser(current, parcel.getId(), "SERVICE_DELIVERED", "代取服务已送达",
                order.getDeliverTo(), "服务单 " + order.getOrderNo() + " 跑腿员确认送达，等待物主确认");
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取包裹已送达",
                String.format("服务单 %s：包裹 %s 已送达「%s」，请在 24 小时内确认完成，逾期将自动确认并结算。",
                        order.getOrderNo(), parcel.getTrackingNo(), order.getDeliverTo()));
        return orderMapper.selectVoById(id);
    }

    @Transactional
    public ProxyOrderVO confirm(LoginUser current, Long id) {
        ProxyOrder order = requireOrder(id);
        boolean staffSide = current.hasRole("STAFF") || current.hasRole("ADMIN");
        if (!staffSide && !current.getUid().equals(order.getRequesterId())) {
            throw new BizException(ErrorCodes.FORBIDDEN, "仅发布人或管理员可确认完成");
        }
        if (orderMapper.markDone(id) == 0) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "仅已送达的服务单可确认完成");
        }
        settle(order, current);
        return orderMapper.selectVoById(id);
    }

    /** 结算：跑腿员入账（总额-抽成），留痕 + 通知 */
    private void settle(ProxyOrder order, LoginUser operator) {
        BigDecimal total = order.getFee().add(order.getTip());
        BigDecimal income = total.subtract(order.getCommission());
        walletService.earn(order.getRunnerId(), income, order.getId(), order.getOrderNo(), order.getCommission());
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        String detail = String.format("服务单 %s 完成，跑腿员 %s 入账 %s 元（平台服务费 %s 元）",
                order.getOrderNo(), nameOf(order.getRunnerId()),
                income.toPlainString(), order.getCommission().toPlainString());
        if (operator == null) {
            traceService.system(parcel.getId(), "SERVICE_DONE", "代取服务完成结算", order.getDeliverTo(), detail);
        } else {
            traceService.byUser(operator, parcel.getId(), "SERVICE_DONE", "代取服务完成结算",
                    order.getDeliverTo(), detail);
        }
        notificationService.sendBiz(order.getRunnerId(), parcel.getId(), "PROXY_ORDER", "代取收入已到账",
                String.format("服务单 %s 已完成，收入 %s 元 已存入钱包（平台服务费 %s 元）。",
                        order.getOrderNo(), income.toPlainString(), order.getCommission().toPlainString()));
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取服务已完成",
                String.format("服务单 %s 已确认完成，包裹 %s 已送达「%s」，欢迎评价本次服务。",
                        order.getOrderNo(), parcel.getTrackingNo(), order.getDeliverTo()));
    }

    /** 定时任务自动确认结算 */
    @Transactional
    public void settleAuto(ProxyOrder order) {
        settle(order, null);
    }

    // ---------- 取消 / 退款 / 评价 ----------

    @Transactional
    public void cancel(LoginUser current, Long id, String reason) {
        ProxyOrder order = requireOrder(id);
        boolean staffSide = current.hasRole("STAFF") || current.hasRole("ADMIN");
        boolean requester = current.getUid().equals(order.getRequesterId());
        boolean runner = current.getUid().equals(order.getRunnerId());
        if (!staffSide && !requester && !runner) {
            throw new BizException(ErrorCodes.FORBIDDEN, "无权取消该服务单");
        }
        if (!staffSide && ("PICKED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus()))) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                    "包裹已取出，无法自行取消，请联系驿站管理员处理");
        }
        closeAndRefund(order, reason == null || reason.trim().isEmpty()
                ? (requester ? "发布人取消" : runner ? "跑腿员取消" : "管理员取消") : reason.trim());
    }

    @Transactional
    public void adminCancel(LoginUser admin, Long id, String reason) {
        ProxyOrder order = requireOrder(id);
        closeAndRefund(order, reason == null || reason.trim().isEmpty() ? "管理员强制关闭" : reason.trim());
    }

    /**
     * 物主凭身份条码到站自取时，联动关闭尚无人接单的 OPEN 服务单：
     * 取消 + 全额退还托管款（含小费）+ 撤销授权 + 留痕通知，避免资金悬空。
     * 条件更新仅命中 OPEN；并发下已被接单/关闭则不重复退款。
     */
    @Transactional
    public void cancelOpenForOwnerPickup(ProxyOrder order, String reason) {
        if (order == null || !"OPEN".equals(order.getStatus())) {
            return;
        }
        if (orderMapper.markCancelled(order.getId(), reason) == 0) {
            return;
        }
        BigDecimal total = order.getFee().add(order.getTip());
        walletService.refund(order.getRequesterId(), total, order.getId(), order.getOrderNo(), reason);
        if (order.getAuthId() != null) {
            ProxyAuth auth = authMapper.selectById(order.getAuthId());
            if (auth != null && "ACTIVE".equals(auth.getStatus())) {
                authMapper.markRevoked(auth.getId(), auth.getGrantorId());
            }
        }
        traceService.system(order.getParcelId(), "SERVICE_CANCEL", "代取服务关闭退款",
                "物主到站自取",
                String.format("服务单 %s 关闭：%s，已全额退款 %s 元", order.getOrderNo(), reason, total.toPlainString()));
        notificationService.sendBiz(order.getRequesterId(), order.getParcelId(), "PROXY_ORDER", "代取服务已关闭",
                String.format("您已凭身份条码到站自取，服务单 %s 自动关闭，托管金额 %s 元 已退回钱包。",
                        order.getOrderNo(), total.toPlainString()));
    }

    /** 关闭 + 全额退款 + 撤销授权 + 留痕（取消/超时共用） */
    private void closeAndRefund(ProxyOrder order, String reason) {
        if (orderMapper.markCancelled(order.getId(), reason) == 0) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "当前状态不允许取消");
        }
        BigDecimal total = order.getFee().add(order.getTip());
        walletService.refund(order.getRequesterId(), total, order.getId(), order.getOrderNo(), reason);
        if (order.getAuthId() != null) {
            ProxyAuth auth = authMapper.selectById(order.getAuthId());
            if (auth != null && "ACTIVE".equals(auth.getStatus())) {
                authMapper.markRevoked(auth.getId(), auth.getGrantorId());
            }
        }
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        traceService.system(parcel.getId(), "SERVICE_CANCEL", "代取服务关闭退款",
                traceService.locationOf(parcel),
                String.format("服务单 %s 关闭：%s，已全额退款 %s 元", order.getOrderNo(), reason, total.toPlainString()));
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取服务已关闭",
                String.format("服务单 %s 已关闭（%s），托管金额 %s 元 已退回钱包。",
                        order.getOrderNo(), reason, total.toPlainString()));
        if (order.getRunnerId() != null) {
            notificationService.sendBiz(order.getRunnerId(), parcel.getId(), "PROXY_ORDER", "代取服务已关闭",
                    String.format("服务单 %s 已关闭（%s），相关代取授权同步失效。", order.getOrderNo(), reason));
        }
    }

    /** 定时任务：超时无人接单 */
    @Transactional
    public void expireAndRefund(ProxyOrder order) {
        BigDecimal total = order.getFee().add(order.getTip());
        walletService.refund(order.getRequesterId(), total, order.getId(), order.getOrderNo(),
                "超时无人接单，自动关闭并退款");
        Parcel parcel = parcelMapper.selectById(order.getParcelId());
        traceService.system(parcel.getId(), "SERVICE_CANCEL", "代取服务超时关闭",
                traceService.locationOf(parcel),
                String.format("服务单 %s 超时无人接单，已退款 %s 元", order.getOrderNo(), total.toPlainString()));
        notificationService.sendBiz(order.getRequesterId(), parcel.getId(), "PROXY_ORDER", "代取服务超时关闭",
                String.format("服务单 %s 超时无人接单，已自动关闭，托管金额 %s 元 退回钱包。",
                        order.getOrderNo(), total.toPlainString()));
    }

    @Transactional
    public void rate(LoginUser current, Long id, ProxyOrderRateDTO dto) {
        if (orderMapper.rate(id, current.getUid(), dto.getRating(), dto.getReview()) == 0) {
            ProxyOrder order = orderMapper.selectById(id);
            if (order == null) {
                throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "服务单不存在");
            }
            if (!current.getUid().equals(order.getRequesterId())) {
                throw new BizException(ErrorCodes.FORBIDDEN, "仅发布人可评价");
            }
            if (order.getRating() != null) {
                throw new BizException(ErrorCodes.ORDER_ALREADY_RATED, "该服务单已评价");
            }
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID, "仅已完成的服务单可评价");
        }
    }

    private ProxyOrder requireOrder(Long id) {
        ProxyOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "服务单不存在");
        }
        return order;
    }

    private String nameOf(Long uid) {
        if (uid == null) {
            return "—";
        }
        SysUser user = userMapper.selectById(uid);
        return user == null ? "用户#" + uid : user.getRealName();
    }

    private String genAuthCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
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

    private BigDecimal configDecimal(String key, String def) {
        String v = configMapper.selectValue(key);
        try {
            return new BigDecimal(v == null ? def : v);
        } catch (NumberFormatException e) {
            return new BigDecimal(def);
        }
    }
}