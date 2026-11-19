package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.entity.ServiceTicket;
import com.campus.express.entity.TicketReply;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.mapper.TicketMapper;
import com.campus.express.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 联系客服工单：提交自动通知管理员+员工，处理流转，双向回复 */
@Service
public class TicketService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList("PARCEL", "PROXY", "WALLET", "SHIP", "OTHER"));

    private final TicketMapper ticketMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    public TicketService(TicketMapper ticketMapper, SysUserMapper userMapper,
                         NotificationService notificationService) {
        this.ticketMapper = ticketMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public ServiceTicket create(LoginUser me, Map<String, Object> body) {
        String category = str(body.get("category")).toUpperCase();
        if (!CATEGORIES.contains(category)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请选择工单类别");
        }
        String title = required(body.get("title"), "标题", 64);
        String content = required(body.get("content"), "问题描述", 1000);

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketNo("TK" + OffsetDateTime.now().format(NO_FMT) + (RANDOM.nextInt(9000) + 1000));
        ticket.setUserId(me.getUid());
        ticket.setCategory(category);
        ticket.setTitle(title);
        ticket.setContent(content);
        ticket.setContact(limit(str(body.get("contact")), 32));
        ticket.setStatus("PENDING");
        ticket.setIsRead(0);
        ticketMapper.insert(ticket);

        String notice = String.format("%s 提交了客服工单 %s「%s」，请及时处理。",
                me.getRealName(), ticket.getTicketNo(), title);
        Set<Long> notified = new HashSet<>();
        for (Long adminId : userMapper.selectAdminIds()) {
            if (notified.add(adminId)) {
                notificationService.sendBiz(adminId, null, "TICKET", "新客服工单待处理", notice);
            }
        }
        for (Long staffId : userMapper.selectStaffIds()) {
            if (notified.add(staffId)) {
                notificationService.sendBiz(staffId, null, "TICKET", "新客服工单待处理", notice);
            }
        }
        notificationService.sendBiz(me.getUid(), null, "TICKET", "工单已受理",
                String.format("工单 %s 已提交，客服将尽快处理，进展会在此通知您。", ticket.getTicketNo()));
        return ticketMapper.selectById(ticket.getId());
    }

    public PageResult<ServiceTicket> page(Long userId, String status, Boolean unread, String keyword,
                                          int page, int size) {
        long total = ticketMapper.countPage(userId, status, unread, keyword);
        return new PageResult<>(total,
                ticketMapper.selectPage(userId, status, unread, keyword, (page - 1) * size, size));
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("pending", ticketMapper.countByStatus("PENDING"));
        result.put("processing", ticketMapper.countByStatus("PROCESSING"));
        result.put("done", ticketMapper.countByStatus("DONE"));
        result.put("unread", ticketMapper.countUnread());
        return result;
    }

    /** 详情 + 回复列表；员工查看即置已读 */
    public Map<String, Object> detail(LoginUser me, Long id, boolean staffView) {
        ServiceTicket ticket = require(id);
        if (!staffView && !me.getUid().equals(ticket.getUserId())) {
            throw new BizException(ErrorCodes.TICKET_NOT_FOUND, "工单不存在");
        }
        if (staffView) {
            ticketMapper.markRead(id);
            ticket = require(id);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticket);
        result.put("replies", ticketMapper.selectReplies(id));
        return result;
    }

    @Transactional
    public TicketReply reply(LoginUser me, Long id, String content, boolean staffView) {
        ServiceTicket ticket = require(id);
        if (!staffView && !me.getUid().equals(ticket.getUserId())) {
            throw new BizException(ErrorCodes.TICKET_NOT_FOUND, "工单不存在");
        }
        if ("DONE".equals(ticket.getStatus()) && !staffView) {
            throw new BizException(ErrorCodes.TICKET_STATE_INVALID, "工单已处理完成，如需帮助请新建工单");
        }
        String text = required(content, "回复内容", 1000);
        TicketReply reply = new TicketReply();
        reply.setTicketId(id);
        reply.setAuthorId(me.getUid());
        reply.setAuthorRole(staffView ? (me.hasRole("ADMIN") ? "ADMIN" : "STAFF") : "USER");
        reply.setContent(text);
        ticketMapper.insertReply(reply);
        if (staffView) {
            notificationService.sendBiz(ticket.getUserId(), null, "TICKET", "工单有新回复",
                    String.format("客服回复了工单 %s：%s", ticket.getTicketNo(), abbr(text)));
        } else if (ticket.getHandlerId() != null) {
            notificationService.sendBiz(ticket.getHandlerId(), null, "TICKET", "工单用户补充说明",
                    String.format("用户在工单 %s 补充：%s", ticket.getTicketNo(), abbr(text)));
        }
        return reply;
    }

    @Transactional
    public ServiceTicket processing(LoginUser me, Long id) {
        require(id);
        if (ticketMapper.markProcessing(id, me.getUid()) == 0) {
            throw new BizException(ErrorCodes.TICKET_STATE_INVALID, "仅待处理工单可开始处理");
        }
        ServiceTicket ticket = require(id);
        notificationService.sendBiz(ticket.getUserId(), null, "TICKET", "工单处理中",
                String.format("工单 %s 已由 %s 受理，正在处理中。", ticket.getTicketNo(), me.getRealName()));
        return ticket;
    }

    @Transactional
    public ServiceTicket done(LoginUser me, Long id, String remark) {
        require(id);
        if (ticketMapper.markDone(id, me.getUid()) == 0) {
            throw new BizException(ErrorCodes.TICKET_STATE_INVALID, "工单已完成，无需重复操作");
        }
        ServiceTicket ticket = require(id);
        String extra = str(remark);
        if (!extra.isEmpty()) {
            TicketReply reply = new TicketReply();
            reply.setTicketId(id);
            reply.setAuthorId(me.getUid());
            reply.setAuthorRole(me.hasRole("ADMIN") ? "ADMIN" : "STAFF");
            reply.setContent("【处理结果】" + limit(extra, 990));
            ticketMapper.insertReply(reply);
        }
        notificationService.sendBiz(ticket.getUserId(), null, "TICKET", "工单已处理完成",
                String.format("工单 %s 已处理完成%s。如仍有疑问可新建工单。", ticket.getTicketNo(),
                        extra.isEmpty() ? "" : "：" + abbr(extra)));
        return ticket;
    }

    /** 用户主动关闭（问题已自行解决） */
    @Transactional
    public ServiceTicket closeByUser(Long uid, Long id) {
        require(id);
        if (ticketMapper.closeByUser(id, uid) == 0) {
            throw new BizException(ErrorCodes.TICKET_STATE_INVALID, "工单不存在或已完成");
        }
        return require(id);
    }

    private ServiceTicket require(Long id) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCodes.TICKET_NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private static String abbr(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
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
}