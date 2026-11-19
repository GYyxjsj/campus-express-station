package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.entity.ShipOrder;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.ShipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/** 自寄单（学生）+ 运费预算 + 员工/管理员处理 */
@RestController
public class ShipController {

    private final ShipService shipService;

    public ShipController(ShipService shipService) {
        this.shipService = shipService;
    }

    @GetMapping("/api/freight/quote")
    public R<Map<String, Object>> quote(@RequestParam BigDecimal weightKg,
                                        @RequestParam(defaultValue = "false") boolean remote) {
        return R.ok(shipService.quote(weightKg, remote));
    }

    @GetMapping("/api/ship-orders")
    public R<PageResult<ShipOrder>> my(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(shipService.page(UserContext.require().getUid(), status, keyword, page, Math.min(size, 100)));
    }

    @GetMapping("/api/ship-orders/{id}")
    public R<ShipOrder> detail(@PathVariable Long id) {
        LoginUser me = UserContext.require();
        boolean staffView = me.hasRole("ADMIN") || me.hasPerm("ship:manage");
        return R.ok(shipService.detail(me.getUid(), id, staffView));
    }

    @PostMapping("/api/ship-orders")
    @OpLog(module = "ship", action = "创建自寄单")
    public R<ShipOrder> create(@RequestBody Map<String, Object> body) {
        return R.ok(shipService.create(UserContext.require().getUid(), body));
    }

    @PostMapping("/api/ship-orders/{id}/drop")
    @OpLog(module = "ship", action = "自寄交件确认")
    public R<ShipOrder> drop(@PathVariable Long id) {
        return R.ok(shipService.drop(UserContext.require().getUid(), id));
    }

    @PostMapping("/api/ship-orders/{id}/cancel")
    @OpLog(module = "ship", action = "取消自寄单")
    public R<ShipOrder> cancel(@PathVariable Long id) {
        LoginUser me = UserContext.require();
        boolean staffView = me.hasRole("ADMIN") || me.hasPerm("ship:manage");
        return R.ok(shipService.cancel(me.getUid(), id, staffView));
    }

    // ---------- 员工 / 管理员 ----------

    @GetMapping("/api/manage/ship-orders")
    @RequirePerm("ship:manage")
    public R<PageResult<ShipOrder>> managePage(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword) {
        return R.ok(shipService.page(null, status, keyword, page, Math.min(size, 100)));
    }

    @PostMapping("/api/manage/ship-orders/{id}/ship")
    @RequirePerm("ship:manage")
    @OpLog(module = "ship", action = "自寄包裹寄出")
    public R<ShipOrder> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(shipService.ship(id, body.get("trackingNo")));
    }

    @PostMapping("/api/manage/ship-orders/{id}/cancel")
    @RequirePerm("ship:manage")
    @OpLog(module = "ship", action = "代客取消自寄单")
    public R<ShipOrder> manageCancel(@PathVariable Long id) {
        return R.ok(shipService.cancel(UserContext.require().getUid(), id, true));
    }
}