package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.RechargeDTO;
import com.campus.express.dto.WalletAdjustDTO;
import com.campus.express.entity.InviteRebate;
import com.campus.express.entity.RechargeOrder;
import com.campus.express.entity.SysUser;
import com.campus.express.entity.WalletAccount;
import com.campus.express.entity.WalletTransaction;
import com.campus.express.mapper.FriendMapper;
import com.campus.express.mapper.RechargeOrderMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.mapper.WalletMapper;
import com.campus.express.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 钱包与充值：开户、充值下单/支付/取消、流水查询、代取服务托管与结算、管理员账务。
 * 资金安全：余额变动一律条件 UPDATE（充足才扣）+ RETURNING 取回余额，流水与余额同事务。
 */
@Service
public class WalletService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> CHANNELS =
            new HashSet<>(Arrays.asList("CAMPUS_CARD", "ALIPAY", "WECHAT"));
    private static final BigDecimal[] TIER_AMOUNTS = {
            new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("200")};
    private static final BigDecimal[] TIER_BONUS = {
            BigDecimal.ZERO, new BigDecimal("3"), new BigDecimal("8"), new BigDecimal("20")};

    private final WalletMapper walletMapper;
    private final RechargeOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final FriendMapper friendMapper;
    private final NotificationService notificationService;

    public WalletService(WalletMapper walletMapper, RechargeOrderMapper orderMapper,
                         SysUserMapper userMapper, SysConfigMapper configMapper,
                         FriendMapper friendMapper,
                         NotificationService notificationService) {
        this.walletMapper = walletMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.configMapper = configMapper;
        this.friendMapper = friendMapper;
        this.notificationService = notificationService;
    }

    public WalletAccount ensureAccount(Long userId) {
        WalletAccount account = walletMapper.selectByUser(userId);
        if (account == null) {
            walletMapper.insertAccount(userId);
            account = walletMapper.selectByUser(userId);
        }
        return account;
    }

    /** 我的钱包总览 */
    public Map<String, Object> overview(Long userId) {
        WalletAccount account = ensureAccount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("account", account);
        result.put("recent", walletMapper.selectTxPage(userId, null, null, "created_at", "DESC", 0, 8));
        result.put("pending", orderMapper.selectPage(userId, "PENDING", 0, 5));
        result.put("tiers", tiers());
        result.put("min", configDecimal("wallet.recharge.min", "1"));
        result.put("max", configDecimal("wallet.recharge.max", "500"));
        result.put("monthRecharge", sumMonth(userId, "RECHARGE"));
        result.put("monthConsume", sumMonth(userId, "CONSUME"));
        result.put("monthEarn", sumMonth(userId, "EARN"));
        return result;
    }

    private BigDecimal sumMonth(Long userId, String type) {
        List<WalletTransaction> list = walletMapper.selectTxPage(userId, type, null, "created_at", "DESC", 0, 500);
        BigDecimal sum = BigDecimal.ZERO;
        OffsetDateTime monthStart = OffsetDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        for (WalletTransaction t : list) {
            if (t.getCreatedAt() != null && t.getCreatedAt().isAfter(monthStart)) {
                sum = sum.add(t.getAmount().abs());
            }
        }
        return sum;
    }

    public List<Map<String, Object>> tiers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < TIER_AMOUNTS.length; i++) {
            Map<String, Object> tier = new HashMap<>();
            tier.put("amount", TIER_AMOUNTS[i]);
            tier.put("bonus", TIER_BONUS[i]);
            tier.put("label", TIER_AMOUNTS[i].toPlainString() + " 元");
            list.add(tier);
        }
        return list;
    }

    public static BigDecimal bonusOf(BigDecimal amount) {
        BigDecimal bonus = BigDecimal.ZERO;
        for (int i = 0; i < TIER_AMOUNTS.length; i++) {
            if (amount.compareTo(TIER_AMOUNTS[i]) >= 0) {
                bonus = TIER_BONUS[i];
            }
        }
        return bonus;
    }

    /** 充值下单（15 分钟内有效） */
    @Transactional
    public RechargeOrder createOrder(Long userId, RechargeDTO dto) {
        String channel = dto.getChannel() == null ? "" : dto.getChannel().trim().toUpperCase();
        if (!CHANNELS.contains(channel)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "不支持的支付渠道");
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal min = configDecimal("wallet.recharge.min", "1");
        BigDecimal max = configDecimal("wallet.recharge.max", "500");
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            throw new BizException(ErrorCodes.RECHARGE_AMOUNT_INVALID,
                    "单笔充值金额需在 " + min.toPlainString() + " ~ " + max.toPlainString() + " 元之间");
        }
        ensureAccount(userId);
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo("RC" + OffsetDateTime.now().format(NO_FMT) + (RANDOM.nextInt(9000) + 1000));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setBonus(bonusOf(amount));
        order.setChannel(channel);
        order.setStatus("PENDING");
        order.setExpireAt(OffsetDateTime.now().plusMinutes(15));
        orderMapper.insert(order);
        return order;
    }

    /** 支付（模拟渠道回调）：条件更新防重复入账 */
    @Transactional
    public Map<String, Object> pay(Long userId, Long orderId) {
        RechargeOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCodes.RECHARGE_ORDER_NOT_FOUND, "充值订单不存在");
        }
        if (orderMapper.markPaid(orderId) == 0) {
            throw new BizException(ErrorCodes.RECHARGE_ORDER_STATE, "订单已支付、已取消或已超时，请重新下单");
        }
        BigDecimal total = order.getAmount().add(order.getBonus());
        BigDecimal after = walletMapper.addBalance(userId, total, "RECHARGE");
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法入账");
        }
        String title = "账户充值 " + order.getAmount().toPlainString() + " 元";
        if (order.getBonus().compareTo(BigDecimal.ZERO) > 0) {
            title += "（赠 " + order.getBonus().toPlainString() + " 元）";
        }
        tx(userId, "RECHARGE", total, after, order.getChannel(), title,
                "RECHARGE_ORDER", orderId, channelName(order.getChannel()) + " 充值到账", null);
        notificationService.sendBiz(userId, null, "WALLET_RECHARGE", "充值到账通知",
                String.format("充值成功：实付 %s 元，到账 %s 元，当前余额 %s 元。订单号 %s。",
                        order.getAmount().toPlainString(), total.toPlainString(),
                        after.toPlainString(), order.getOrderNo()));
        rebateInviter(order);
        Map<String, Object> result = new HashMap<>();
        result.put("order", orderMapper.selectById(orderId));
        result.put("account", walletMapper.selectByUser(userId));
        return result;
    }

    /**
     * 邀请返利（支付同事务）：被邀请人充值实付金额 × 配置比例入账邀请人。
     * invite_rebate.recharge_order_id 唯一约束 + ON CONFLICT DO NOTHING 幂等。
     */
    private void rebateInviter(RechargeOrder order) {
        SysUser invitee = userMapper.selectById(order.getUserId());
        if (invitee == null || invitee.getInvitedBy() == null) {
            return;
        }
        SysUser inviter = userMapper.selectById(invitee.getInvitedBy());
        if (inviter == null || inviter.getStatus() == null || inviter.getStatus() != 1) {
            return;
        }
        if (friendMapper.selectRebateByOrder(order.getId()) != null) {
            return;
        }
        BigDecimal rate = configDecimal("invite.rebate.percent", "10");
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(new BigDecimal("50")) > 0) {
            rate = new BigDecimal("10");
        }
        BigDecimal amount = order.getAmount().multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        InviteRebate rebate = new InviteRebate();
        rebate.setInviterId(inviter.getId());
        rebate.setInviteeId(invitee.getId());
        rebate.setRechargeOrderId(order.getId());
        rebate.setRechargeAmount(order.getAmount());
        rebate.setRatePercent(rate);
        rebate.setAmount(amount);
        friendMapper.insertRebate(rebate);
        if (rebate.getId() == null) {
            return; // 并发下已由另一事务写入
        }
        income(inviter.getId(), amount, "邀请返利 " + amount.toPlainString() + " 元",
                "INVITE_REBATE", rebate.getId(),
                String.format("被邀请人 %s 充值 %s 元，按 %s%% 返利",
                        invitee.getRealName(), order.getAmount().toPlainString(), rate.toPlainString()));
        notificationService.sendBiz(inviter.getId(), null, "INVITE_REBATE", "邀请返利到账",
                String.format("%s 充值 %s 元，您获得 %s%% 返利 %s 元，已存入钱包。",
                        invitee.getRealName(), order.getAmount().toPlainString(),
                        rate.toPlainString(), amount.toPlainString()));
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        RechargeOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCodes.RECHARGE_ORDER_NOT_FOUND, "充值订单不存在");
        }
        if (orderMapper.markCancelled(orderId, userId) == 0) {
            throw new BizException(ErrorCodes.RECHARGE_ORDER_STATE, "仅待支付订单可取消");
        }
    }

    public PageResult<RechargeOrder> myOrders(Long userId, String status, int page, int size) {
        long total = orderMapper.countPage(userId, status);
        return new PageResult<>(total, orderMapper.selectPage(userId, status, (page - 1) * size, size));
    }

    public PageResult<WalletTransaction> myTransactions(Long userId, String type, String keyword,
                                                        int page, int size) {
        long total = walletMapper.countTxPage(userId, type, keyword);
        return new PageResult<>(total,
                walletMapper.selectTxPage(userId, type, keyword, "created_at", "DESC", (page - 1) * size, size));
    }

    // ---------- 代取服务资金动作（同一事务内被调用） ----------

    /** 托管扣费 */
    public void escrow(Long userId, BigDecimal amount, Long orderId, String orderNo) {
        BigDecimal after = walletMapper.subBalance(userId, amount);
        if (after == null) {
            WalletAccount account = walletMapper.selectByUser(userId);
            if (account != null && account.getStatus() != null && account.getStatus() == 0) {
                throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法下单");
            }
            throw new BizException(ErrorCodes.BALANCE_NOT_ENOUGH,
                    "余额不足，需 " + amount.toPlainString() + " 元，请先充值");
        }
        tx(userId, "CONSUME", amount.negate(), after, "BALANCE", "代取服务托管 " + amount.toPlainString() + " 元",
                "PROXY_ORDER", orderId, "服务单 " + orderNo + " 发布托管，完成或取消后结算/退回", null);
    }

    /** 取消/超时退款 */
    public void refund(Long userId, BigDecimal amount, Long orderId, String orderNo, String reason) {
        BigDecimal after = walletMapper.addBalance(userId, amount, "REFUND");
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法退款");
        }
        tx(userId, "REFUND", amount, after, "BALANCE", "代取服务退款 " + amount.toPlainString() + " 元",
                "PROXY_ORDER", orderId, reason, null);
    }

    /** 跑腿收入结算 */
    public void earn(Long userId, BigDecimal amount, Long orderId, String orderNo, BigDecimal commission) {
        BigDecimal after = walletMapper.addBalance(userId, amount, "EARN");
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法结算");
        }
        tx(userId, "EARN", amount, after, "BALANCE", "代取服务收入 " + amount.toPlainString() + " 元",
                "PROXY_ORDER", orderId,
                "服务单 " + orderNo + " 已完成，平台服务费 " + commission.toPlainString() + " 元", null);
    }

    /** 通用消费扣款（自寄运费/保险保费等），余额不足/冻结抛业务异常 */
    public void consume(Long userId, BigDecimal amount, String title, String refType, Long refId, String remark) {
        BigDecimal after = walletMapper.subBalance(userId, amount);
        if (after == null) {
            WalletAccount account = walletMapper.selectByUser(userId);
            if (account != null && account.getStatus() != null && account.getStatus() == 0) {
                throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法支付");
            }
            throw new BizException(ErrorCodes.BALANCE_NOT_ENOUGH,
                    "余额不足，需 " + amount.toPlainString() + " 元，请先充值");
        }
        tx(userId, "CONSUME", amount.negate(), after, "BALANCE", title, refType, refId, remark, null);
    }

    /** 通用收入入账（邀请返利/保险理赔等） */
    public void income(Long userId, BigDecimal amount, String title, String refType, Long refId, String remark) {
        BigDecimal after = walletMapper.addBalance(userId, amount, "EARN");
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法入账");
        }
        tx(userId, "EARN", amount, after, "SYSTEM", title, refType, refId, remark, null);
    }

    /** 通用退款入账 */
    public void refundGeneric(Long userId, BigDecimal amount, String title, String refType, Long refId, String remark) {
        BigDecimal after = walletMapper.addBalance(userId, amount, "REFUND");
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "钱包已冻结，无法退款");
        }
        tx(userId, "REFUND", amount, after, "BALANCE", title, refType, refId, remark, null);
    }

    private void tx(Long userId, String type, BigDecimal amount, BigDecimal after, String channel,
                    String title, String refType, Long refId, String remark, Long operatorId) {
        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType(type);
        t.setAmount(amount);
        t.setBalanceAfter(after);
        t.setChannel(channel);
        t.setTitle(title);
        t.setRefType(refType);
        t.setRefId(refId);
        t.setRemark(remark);
        t.setOperatorId(operatorId);
        walletMapper.insertTx(t);
    }

    // ---------- 管理端 ----------

    public PageResult<Map<String, Object>> accounts(String keyword, String sortKey, String order, int page, int size) {
        long total = walletMapper.countAccounts(keyword);
        return new PageResult<>(total, walletMapper.selectAccounts(keyword,
                com.campus.express.common.Sorts.walletAccount(sortKey),
                com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public PageResult<Map<String, Object>> adminOrders(String keyword, String status,
                                                       String sortKey, String order, int page, int size) {
        long total = orderMapper.countAdminPage(keyword, status);
        return new PageResult<>(total,
                orderMapper.selectAdminPage(keyword, status,
                        com.campus.express.common.Sorts.recharge(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public PageResult<WalletTransaction> adminTransactions(Long userId, String type, String keyword,
                                                           String sortKey, String order, int page, int size) {
        long total = walletMapper.countTxPage(userId, type, keyword);
        return new PageResult<>(total,
                walletMapper.selectTxPage(userId, type, keyword,
                        com.campus.express.common.Sorts.walletTx(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public Map<String, Object> adminOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", walletMapper.platformSummary());
        result.put("daily", walletMapper.dailyFlow(14));
        result.put("types", walletMapper.typeDistribution());
        return result;
    }

    /** 管理员账务调整（正=补入，负=扣减） */
    @Transactional
    public WalletTransaction adjust(LoginUser admin, WalletAdjustDTO dto) {
        SysUser user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "调整金额不能为 0");
        }
        if (amount.abs().compareTo(new BigDecimal("10000")) > 0) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "单次调整金额不得超过 10000 元");
        }
        ensureAccount(user.getId());
        BigDecimal after;
        String type;
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            after = walletMapper.addBalance(user.getId(), amount, "ADJUST");
            type = "ADJUST";
        } else {
            after = walletMapper.subBalance(user.getId(), amount.abs());
            type = "ADJUST";
            if (after == null) {
                throw new BizException(ErrorCodes.BALANCE_NOT_ENOUGH, "用户余额不足，无法扣减");
            }
        }
        if (after == null) {
            throw new BizException(ErrorCodes.WALLET_FROZEN, "该用户钱包已冻结");
        }
        tx(user.getId(), type, amount, after, "SYSTEM",
                (amount.compareTo(BigDecimal.ZERO) > 0 ? "管理员补入 " : "管理员扣减 ")
                        + amount.abs().toPlainString() + " 元",
                "ADMIN", admin.getUid(), dto.getRemark(), admin.getUid());
        notificationService.sendBiz(user.getId(), null, "WALLET_RECHARGE", "账务调整通知",
                String.format("管理员 %s 对您的钱包进行了调整：%s %s 元，事由：%s，当前余额 %s 元。",
                        admin.getRealName(), amount.compareTo(BigDecimal.ZERO) > 0 ? "补入" : "扣减",
                        amount.abs().toPlainString(), dto.getRemark(), after.toPlainString()));
        List<WalletTransaction> latest = walletMapper.selectTxPage(user.getId(), "ADJUST", null, "created_at", "DESC", 0, 1);
        return latest.isEmpty() ? null : latest.get(0);
    }

    @Transactional
    public void freeze(Long userId, Integer status) {
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        ensureAccount(userId);
        walletMapper.updateStatus(userId, status != null && status == 0 ? 0 : 1);
    }

    public static String channelName(String code) {
        if (code == null) {
            return "余额";
        }
        switch (code) {
            case "CAMPUS_CARD": return "校园一卡通";
            case "ALIPAY": return "支付宝";
            case "WECHAT": return "微信支付";
            case "BALANCE": return "钱包余额";
            case "SYSTEM": return "系统";
            default: return code;
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