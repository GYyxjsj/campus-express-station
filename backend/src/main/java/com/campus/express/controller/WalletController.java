package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.RechargeDTO;
import com.campus.express.entity.RechargeOrder;
import com.campus.express.entity.WalletTransaction;
import com.campus.express.security.UserContext;
import com.campus.express.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 我的钱包：余额、充值、流水 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public R<Map<String, Object>> overview() {
        return R.ok(walletService.overview(UserContext.require().getUid()));
    }

    @GetMapping("/tiers")
    public R<List<Map<String, Object>>> tiers() {
        return R.ok(walletService.tiers());
    }

    @GetMapping("/transactions")
    public R<PageResult<WalletTransaction>> transactions(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) String keyword) {
        return R.ok(walletService.myTransactions(UserContext.require().getUid(), type, keyword,
                page, Math.min(size, 100)));
    }

    @GetMapping("/recharge-orders")
    public R<PageResult<RechargeOrder>> orders(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String status) {
        return R.ok(walletService.myOrders(UserContext.require().getUid(), status, page, Math.min(size, 100)));
    }

    @PostMapping("/recharge")
    @OpLog(module = "wallet", action = "创建充值订单")
    public R<RechargeOrder> recharge(@Valid @RequestBody RechargeDTO dto) {
        return R.ok(walletService.createOrder(UserContext.require().getUid(), dto));
    }

    @PostMapping("/recharge/{id}/pay")
    @OpLog(module = "wallet", action = "充值支付")
    public R<Map<String, Object>> pay(@PathVariable Long id) {
        return R.ok(walletService.pay(UserContext.require().getUid(), id));
    }

    @PostMapping("/recharge/{id}/cancel")
    @OpLog(module = "wallet", action = "取消充值订单")
    public R<Void> cancel(@PathVariable Long id) {
        walletService.cancelOrder(UserContext.require().getUid(), id);
        return R.ok();
    }
}