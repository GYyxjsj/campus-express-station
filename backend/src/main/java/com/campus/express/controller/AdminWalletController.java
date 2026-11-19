package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.WalletAdjustDTO;
import com.campus.express.entity.WalletTransaction;
import com.campus.express.security.RequirePerm;
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
import java.util.Map;

/** 资金账务管理（管理员） */
@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/overview")
    @RequirePerm("wallet:manage")
    public R<Map<String, Object>> overview() {
        return R.ok(walletService.adminOverview());
    }

    @GetMapping("/accounts")
    @RequirePerm("wallet:manage")
    public R<PageResult<Map<String, Object>>> accounts(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String sortKey,
                                                       @RequestParam(required = false) String order) {
        return R.ok(walletService.accounts(keyword, sortKey, order, page, Math.min(size, 100)));
    }

    @GetMapping("/transactions")
    @RequirePerm("wallet:manage")
    public R<PageResult<WalletTransaction>> transactions(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long userId,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String sortKey,
                                                         @RequestParam(required = false) String order) {
        return R.ok(walletService.adminTransactions(userId, type, keyword, sortKey, order, page, Math.min(size, 100)));
    }

    @GetMapping("/recharge-orders")
    @RequirePerm("wallet:manage")
    public R<PageResult<Map<String, Object>>> orders(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String sortKey,
                                                     @RequestParam(required = false) String order) {
        return R.ok(walletService.adminOrders(keyword, status, sortKey, order, page, Math.min(size, 100)));
    }

    @PostMapping("/adjust")
    @RequirePerm("wallet:manage")
    @OpLog(module = "wallet", action = "账务调整")
    public R<WalletTransaction> adjust(@Valid @RequestBody WalletAdjustDTO dto) {
        return R.ok(walletService.adjust(UserContext.require(), dto));
    }

    @PostMapping("/accounts/{userId}/status")
    @RequirePerm("wallet:manage")
    @OpLog(module = "wallet", action = "钱包冻结/解冻")
    public R<Void> status(@PathVariable Long userId, @RequestParam Integer status) {
        walletService.freeze(userId, status);
        return R.ok();
    }
}