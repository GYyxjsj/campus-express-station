package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.ProxyOrderService;
import com.campus.express.vo.ProxyOrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 代取服务监管（管理员） */
@RestController
@RequestMapping("/api/admin/proxy-orders")
public class AdminProxyOrderController {

    private final ProxyOrderService orderService;

    public AdminProxyOrderController(ProxyOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/overview")
    @RequirePerm("proxy:manage")
    public R<Map<String, Object>> overview() {
        return R.ok(orderService.stats());
    }

    @GetMapping
    @RequirePerm("proxy:manage")
    public R<PageResult<ProxyOrderVO>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long stationId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String sortKey,
                                            @RequestParam(required = false) String order) {
        return R.ok(orderService.adminPage(status, stationId, keyword, sortKey, order, page, Math.min(size, 100)));
    }

    @PostMapping("/{id}/cancel")
    @RequirePerm("proxy:manage")
    @OpLog(module = "proxy", action = "管理员关闭代取服务单")
    public R<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        orderService.adminCancel(UserContext.require(), id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @RequirePerm("proxy:manage")
    @OpLog(module = "proxy", action = "管理员代确认完成")
    public R<ProxyOrderVO> confirm(@PathVariable Long id) {
        return R.ok(orderService.confirm(UserContext.require(), id));
    }
}