package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.FriendMapper;
import com.campus.express.mapper.SysConfigMapper;
import com.campus.express.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 邀请有礼：邀请码生成/绑定；充值返利入账在 WalletService.pay 事务内完成（防循环依赖）。 */
@Service
public class InviteService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserMapper userMapper;
    private final FriendMapper friendMapper;
    private final SysConfigMapper configMapper;
    private final NotificationService notificationService;

    public InviteService(SysUserMapper userMapper, FriendMapper friendMapper,
                         SysConfigMapper configMapper,
                         NotificationService notificationService) {
        this.userMapper = userMapper;
        this.friendMapper = friendMapper;
        this.configMapper = configMapper;
        this.notificationService = notificationService;
    }

    /** 我的邀请总览：邀请码 + 人数 + 累计返利 + 比例 */
    public Map<String, Object> info(Long uid) {
        SysUser me = userMapper.selectById(uid);
        if (me == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        if (me.getInviteCode() == null || me.getInviteCode().isEmpty()) {
            ensureInviteCode(me);
            me = userMapper.selectById(uid);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("inviteCode", me.getInviteCode());
        m.put("inviteeCount", userMapper.countInvitees(uid));
        m.put("invitees", userMapper.selectInvitees(uid));
        m.put("totalRebate", friendMapper.sumRebate(uid));
        m.put("ratePercent", ratePercent());
        m.put("boundInviter", me.getInvitedBy() != null);
        if (me.getInvitedBy() != null) {
            SysUser inviter = userMapper.selectById(me.getInvitedBy());
            m.put("inviterName", inviter == null ? null : inviter.getRealName());
        }
        return m;
    }

    public PageResult<Map<String, Object>> records(Long uid, int page, int size) {
        long total = friendMapper.countRebatePage(uid);
        return new PageResult<>(total, friendMapper.selectRebatePage(uid, (page - 1) * size, size));
    }

    /** 绑定邀请码（注册后补填；防自邀/重复绑定） */
    @Transactional
    public void bind(Long uid, String code) {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (c.isEmpty()) {
            throw new BizException(ErrorCodes.INVITE_INVALID, "请输入邀请码");
        }
        SysUser me = userMapper.selectById(uid);
        if (me == null) {
            throw new BizException(ErrorCodes.USER_NOT_FOUND, "用户不存在");
        }
        if (me.getInvitedBy() != null) {
            throw new BizException(ErrorCodes.INVITE_ALREADY_BOUND, "您已绑定过邀请人，不可更换");
        }
        SysUser inviter = userMapper.selectByInviteCode(c);
        if (inviter == null || inviter.getStatus() == null || inviter.getStatus() != 1) {
            throw new BizException(ErrorCodes.INVITE_INVALID, "邀请码无效");
        }
        if (inviter.getId().equals(uid)) {
            throw new BizException(ErrorCodes.INVITE_INVALID, "不能使用自己的邀请码");
        }
        if (userMapper.bindInviter(uid, inviter.getId()) == 0) {
            throw new BizException(ErrorCodes.INVITE_ALREADY_BOUND, "您已绑定过邀请人，不可更换");
        }
        notificationService.sendBiz(inviter.getId(), null, "INVITE", "邀请绑定成功",
                String.format("%s（%s）使用了您的邀请码，TA 每次充值您都可获得 %s%% 返利。",
                        me.getRealName(), me.getUsername(), ratePercent().toPlainString()));
    }

    /** 注册时生成 8 位邀请码（md5 截断大写，冲突重试） */
    public void ensureInviteCode(SysUser user) {
        for (int i = 0; i < 5; i++) {
            String code = genCode(user.getId());
            if (userMapper.selectByInviteCode(code) == null) {
                userMapper.updateInviteCode(user.getId(), code);
                return;
            }
        }
    }

    private String genCode(Long uid) {
        try {
            String seed = uid + ":" + UUID.randomUUID() + ":" + RANDOM.nextLong();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02X", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.format("%08X", RANDOM.nextInt());
        }
    }

    /** 注册时按邀请码绑定（无效码静默忽略，不阻塞注册） */
    public void bindOnRegister(Long newUserId, String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return;
        }
        SysUser inviter = userMapper.selectByInviteCode(inviteCode.trim().toUpperCase());
        if (inviter == null || inviter.getId().equals(newUserId)
                || inviter.getStatus() == null || inviter.getStatus() != 1) {
            return;
        }
        userMapper.bindInviter(newUserId, inviter.getId());
        SysUser newbie = userMapper.selectById(newUserId);
        notificationService.sendBiz(inviter.getId(), null, "INVITE", "邀请注册成功",
                String.format("%s（%s）通过您的邀请码完成注册，TA 充值后您可获得 %s%% 返利。",
                        newbie.getRealName(), newbie.getUsername(), ratePercent().toPlainString()));
    }

    /**
     * 返利入账由 WalletService.pay 在支付事务内直接完成（避免循环依赖），
     * 本服务只负责邀请码与绑定关系。
     */
    public BigDecimal ratePercent() {
        String v = configMapper.selectValue("invite.rebate.percent");
        try {
            BigDecimal rate = new BigDecimal(v == null ? "10" : v);
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("50")) > 0) {
                return new BigDecimal("10");
            }
            return rate;
        } catch (NumberFormatException e) {
            return new BigDecimal("10");
        }
    }
}