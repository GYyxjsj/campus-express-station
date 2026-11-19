package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.ProxyOrderCreateDTO;
import com.campus.express.dto.ProxyOrderRateDTO;
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

import javax.validation.Valid;
import java.util.Map;

/** 代取服务（有偿跑腿）：大厅 / 我的单 / 履约流转 */
@RestController
@RequestMapping("/api/proxy-orders")
public class ProxyOrderController {

    private final ProxyOrderService orderService;

    public ProxyOrderController(ProxyOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/rules")
    public R<Map<String, Object>> rules() {
        return R.ok(orderService.rules());
    }

    @GetMapping("/hall")
    public R<PageResult<ProxyOrderVO>> hall(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Long stationId,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(orderService.hall(UserContext.require().getUid(), stationId, keyword,
                page, Math.min(size, 100)));
    }

    @GetMapping("/my-requests")
    public R<PageResult<ProxyOrderVO>> myRequests(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String status) {
        return R.ok(orderService.myRequests(UserContext.require().getUid(), status, page, Math.min(size, 100)));
    }

    @GetMapping("/my-runs")
    public R<PageResult<ProxyOrderVO>> myRuns(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String status) {
        return R.ok(orderService.myRuns(UserContext.require().getUid(), status, page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public R<ProxyOrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(UserContext.require(), id));
    }

    @PostMapping
    @OpLog(module = "proxy", action = "发布代取服务单")
    public R<ProxyOrderVO> create(@Valid @RequestBody ProxyOrderCreateDTO dto) {
        return R.ok(orderService.create(UserContext.require(), dto));
    }

    @PostMapping("/{id}/accept")
    @OpLog(module = "proxy", action = "接代取服务单")
    public R<ProxyOrderVO> accept(@PathVariable Long id) {
        return R.ok(orderService.accept(UserContext.require(), id));
    }

    @PostMapping("/{id}/picked")
    @OpLog(module = "proxy", action = "代取服务标记已取件")
    public R<ProxyOrderVO> picked(@PathVariable Long id) {
        return R.ok(orderService.picked(UserContext.require(), id));
    }

    @PostMapping("/{id}/delivered")
    @OpLog(module = "proxy", action = "代取服务标记已送达")
    public R<ProxyOrderVO> delivered(@PathVariable Long id) {
        return R.ok(orderService.delivered(UserContext.require(), id));
    }

    @PostMapping("/{id}/confirm")
    @OpLog(module = "proxy", action = "确认代取服务完成")
    public R<ProxyOrderVO> confirm(@PathVariable Long id) {
        return R.ok(orderService.confirm(UserContext.require(), id));
    }

    @PostMapping("/{id}/cancel")
    @OpLog(module = "proxy", action = "取消代取服务单")
    public R<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        orderService.cancel(UserContext.require(), id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    @PostMapping("/{id}/rate")
    @OpLog(module = "proxy", action = "评价代取服务")
    public R<Void> rate(@PathVariable Long id, @Valid @RequestBody ProxyOrderRateDTO dto) {
        orderService.rate(UserContext.require(), id, dto);
        return R.ok();
    }
}