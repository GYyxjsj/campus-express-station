package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.ServiceTicket;
import com.campus.express.entity.TicketReply;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 客服工单：学生提交/跟进，员工与管理员处理 */
@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    private boolean staffView(LoginUser me) {
        return me.hasRole("ADMIN") || me.hasPerm("ticket:handle");
    }

    // ---------- 学生 ----------

    @PostMapping("/api/tickets")
    @OpLog(module = "ticket", action = "提交客服工单")
    public R<ServiceTicket> create(@RequestBody Map<String, Object> body) {
        return R.ok(ticketService.create(UserContext.require(), body));
    }

    @GetMapping("/api/tickets/my")
    public R<PageResult<ServiceTicket>> my(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String status) {
        return R.ok(ticketService.page(UserContext.require().getUid(), status, null, null,
                page, Math.min(size, 100)));
    }

    @GetMapping("/api/tickets/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        LoginUser me = UserContext.require();
        return R.ok(ticketService.detail(me, id, staffView(me)));
    }

    @PostMapping("/api/tickets/{id}/reply")
    @OpLog(module = "ticket", action = "回复工单")
    public R<TicketReply> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LoginUser me = UserContext.require();
        return R.ok(ticketService.reply(me, id, body.get("content"), staffView(me)));
    }

    @PostMapping("/api/tickets/{id}/close")
    @OpLog(module = "ticket", action = "用户关闭工单")
    public R<ServiceTicket> close(@PathVariable Long id) {
        return R.ok(ticketService.closeByUser(UserContext.require().getUid(), id));
    }

    // ---------- 员工 / 管理员 ----------

    @GetMapping("/api/manage/tickets")
    @RequirePerm("ticket:handle")
    public R<PageResult<ServiceTicket>> managePage(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Boolean unread,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(ticketService.page(null, status, unread, keyword, page, Math.min(size, 100)));
    }

    @GetMapping("/api/manage/tickets/stats")
    @RequirePerm("ticket:handle")
    public R<Map<String, Object>> stats() {
        return R.ok(ticketService.stats());
    }

    @PostMapping("/api/manage/tickets/{id}/processing")
    @RequirePerm("ticket:handle")
    @OpLog(module = "ticket", action = "开始处理工单")
    public R<ServiceTicket> processing(@PathVariable Long id) {
        return R.ok(ticketService.processing(UserContext.require(), id));
    }

    @PostMapping("/api/manage/tickets/{id}/done")
    @RequirePerm("ticket:handle")
    @OpLog(module = "ticket", action = "完成工单处理")
    public R<ServiceTicket> done(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        return R.ok(ticketService.done(UserContext.require(), id, remark));
    }
}