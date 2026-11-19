package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.ShipOrder;
import com.campus.express.entity.Station;
import com.campus.express.mapper.ShipOrderMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 服务站自寄：运费预算、下单扣费、交件/寄出状态机、取消退费 */
@Service
public class ShipService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> COURIERS = new HashSet<>(Arrays.asList("SF", "ZT", "YT", "JD", "STO", "YD", "EMS", "JT"));

    private final ShipOrderMapper shipOrderMapper;
    private final StationMapper stationMapper;
    private final SysConfigMapper configMapper;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public ShipService(ShipOrderMapper shipOrderMapper, StationMapper stationMapper,
                       SysConfigMapper configMapper, WalletService walletService,
                       NotificationService notificationService) {
        this.shipOrderMapper = shipOrderMapper;
        this.stationMapper = stationMapper;
        this.configMapper = configMapper;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    /** 运费预算：首重 + 续重（向上取整）+ 偏远附加 */
    public Map<String, Object> quote(BigDecimal weightKg, boolean remote) {
        BigDecimal weight = normalizeWeight(weightKg);
        BigDecimal firstKg = configDecimal("freight.first.kg", "1");
        BigDecimal firstPrice = configDecimal("freight.first.price", "8");
        BigDecimal extraPrice = configDecimal("freight.extra.price", "2");
        BigDecimal remoteExtra = configDecimal("freight.remote.extra", "4");

        BigDecimal extraKg = weight.subtract(firstKg).max(BigDecimal.ZERO);
        int extraUnits = extraKg.setScale(0, RoundingMode.CEILING).intValue();
        BigDecimal extraFee = extraPrice.multiply(BigDecimal.valueOf(extraUnits));
        BigDecimal remoteFee = remote ? remoteExtra : BigDecimal.ZERO;
        BigDecimal total = firstPrice.add(extraFee).add(remoteFee);

        Map<String, Object> result = new HashMap<>();
        result.put("weightKg", weight);
        result.put("firstKg", firstKg);
        result.put("firstPrice", firstPrice);
        result.put("extraUnits", extraUnits);
        result.put("extraPrice", extraPrice);
        result.put("extraFee", extraFee);
        result.put("remote", remote);
        result.put("remoteFee", remoteFee);
        result.put("total", total);
        return result;
    }

    public PageResult<ShipOrder> page(Long userId, String status, String keyword, int page, int size) {
        long total = shipOrderMapper.countPage(userId, status, keyword);
        return new PageResult<>(total,
                shipOrderMapper.selectPage(userId, status, keyword, (page - 1) * size, size));
    }

    public ShipOrder detail(Long uid, Long id, boolean staffView) {
        ShipOrder order = require(id);
        if (!staffView && !uid.equals(order.getUserId())) {
            throw new BizException(ErrorCodes.SHIP_NOT_FOUND, "自寄单不存在");
        }
        return order;
    }

    /** 下单：校验 + 计费 + 余额扣运费，同一事务 */
    @Transactional
    public ShipOrder create(Long uid, Map<String, Object> body) {
        String courier = str(body.get("courierCompany")).toUpperCase();
        if (!COURIERS.contains(courier)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请选择支持的快递公司");
        }
        Long stationId = longVal(body.get("stationId"));
        Station station = stationId == null ? null : stationMapper.selectAll().stream()
                .filter(s -> s.getId().equals(stationId)).findFirst().orElse(null);
        if (station == null || station.getStatus() == null || station.getStatus() != 1) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请选择营业中的服务站");
        }
        BigDecimal weight = normalizeWeight(decimal(body.get("weightKg")));
        boolean remote = truthy(body.get("remote"));
        BigDecimal freight = (BigDecimal) quote(weight, remote).get("total");

        ShipOrder order = new ShipOrder();
        order.setOrderNo("SP" + OffsetDateTime.now().format(NO_FMT) + (RANDOM.nextInt(9000) + 1000));
        order.setUserId(uid);
        order.setStationId(stationId);
        order.setCourierCompany(courier);
        order.setSenderName(required(body.get("senderName"), "寄件人", 32));
        order.setSenderPhone(phone(body.get("senderPhone"), "寄件人电话"));
        order.setReceiverName(required(body.get("receiverName"), "收件人", 32));
        order.setReceiverPhone(phone(body.get("receiverPhone"), "收件人电话"));
        order.setReceiverAddress(required(body.get("receiverAddress"), "收件地址", 200));
        order.setGoodsType(limit(strOr(body.get("goodsType"), "日常用品"), 32));
        order.setWeightKg(weight);
        order.setFreight(freight);
        order.setInsured(truthy(body.get("insured")) ? 1 : 0);
        order.setNote(limit(strOr(body.get("note"), ""), 200));
        order.setStatus("CREATED");
        shipOrderMapper.insert(order);

        walletService.consume(uid, freight, "自寄运费 " + freight.toPlainString() + " 元",
                "SHIP_ORDER", order.getId(),
                "自寄单 " + order.getOrderNo() + "（" + NotificationService.courierName(courier)
                        + "，" + weight.toPlainString() + " kg）");
        notificationService.sendBiz(uid, null, "SHIP", "自寄下单成功",
                String.format("自寄单 %s 已创建，运费 %s 元已从余额扣除。请将包裹交至「%s」柜台，交件后驿站安排寄出。",
                        order.getOrderNo(), freight.toPlainString(), station.getName()));
        return shipOrderMapper.selectById(order.getId());
    }

    /** 学生交件到站 */
    @Transactional
    public ShipOrder drop(Long uid, Long id) {
        ShipOrder order = detail(uid, id, false);
        if (shipOrderMapper.markDropped(id) == 0) {
            throw new BizException(ErrorCodes.SHIP_STATE_INVALID, "仅待交件的自寄单可确认交件");
        }
        notificationService.sendBiz(uid, null, "SHIP", "自寄包裹已交件",
                "自寄单 " + order.getOrderNo() + " 已交至驿站，寄出后将回填运单号。");
        return shipOrderMapper.selectById(id);
    }

    /** 员工寄出并回填运单号 */
    @Transactional
    public ShipOrder ship(Long id, String trackingNo) {
        ShipOrder order = require(id);
        String no = str(trackingNo);
        if (no.isEmpty() || no.length() > 32) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请填写有效运单号");
        }
        if (shipOrderMapper.markShipped(id, no) == 0) {
            throw new BizException(ErrorCodes.SHIP_STATE_INVALID, "仅已交件的自寄单可寄出");
        }
        notificationService.sendBiz(order.getUserId(), null, "SHIP", "自寄包裹已寄出",
                String.format("自寄单 %s 已由 %s 寄出，运单号 %s，可在承运方官网跟踪物流。",
                        order.getOrderNo(), NotificationService.courierName(order.getCourierCompany()), no));
        return shipOrderMapper.selectById(id);
    }

    /** 取消并退运费（学生本人或员工均可，已寄出不可取消） */
    @Transactional
    public ShipOrder cancel(Long uid, Long id, boolean staffView) {
        ShipOrder order = detail(uid, id, staffView);
        if (shipOrderMapper.markCancelled(id) == 0) {
            throw new BizException(ErrorCodes.SHIP_STATE_INVALID, "已寄出或已取消的自寄单无法取消");
        }
        walletService.refundGeneric(order.getUserId(), order.getFreight(),
                "自寄取消退款 " + order.getFreight().toPlainString() + " 元",
                "SHIP_ORDER", id, "自寄单 " + order.getOrderNo() + " 已取消，运费原路退回余额");
        notificationService.sendBiz(order.getUserId(), null, "SHIP", "自寄单已取消",
                String.format("自寄单 %s 已取消，运费 %s 元已退回钱包余额。",
                        order.getOrderNo(), order.getFreight().toPlainString()));
        return shipOrderMapper.selectById(id);
    }

    private ShipOrder require(Long id) {
        ShipOrder order = shipOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCodes.SHIP_NOT_FOUND, "自寄单不存在");
        }
        return order;
    }

    private BigDecimal normalizeWeight(BigDecimal weight) {
        if (weight == null || weight.compareTo(new BigDecimal("0.1")) < 0
                || weight.compareTo(new BigDecimal("50")) > 0) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "重量须在 0.1 ~ 50 kg 之间");
        }
        return weight.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal configDecimal(String key, String def) {
        String v = configMapper.selectValue(key);
        try {
            return new BigDecimal(v == null ? def : v);
        } catch (NumberFormatException e) {
            return new BigDecimal(def);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String strOr(Object o, String def) {
        String s = str(o);
        return s.isEmpty() ? def : s;
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String required(Object o, String field, int max) {
        String s = str(o);
        if (s.isEmpty()) {
            throw new BizException(ErrorCodes.PARAM_INVALID, field + "必填");
        }
        if (s.length() > max) {
            throw new BizException(ErrorCodes.PARAM_INVALID, field + "不能超过 " + max + " 字");
        }
        return s;
    }

    private static String phone(Object o, String field) {
        String s = str(o);
        if (!s.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodes.PARAM_INVALID, field + "须为有效手机号");
        }
        return s;
    }

    private static BigDecimal decimal(Object o) {
        try {
            return o == null ? null : new BigDecimal(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long longVal(Object o) {
        try {
            return o == null ? null : Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean truthy(Object o) {
        if (o == null) { return false; }
        String s = String.valueOf(o).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
}