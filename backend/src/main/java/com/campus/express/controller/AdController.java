package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.entity.AdSlot;
import com.campus.express.security.RequirePerm;
import com.campus.express.service.AdService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 广告位：前台读取生效广告，管理端增删改 */
@RestController
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    @GetMapping("/api/ads/active")
    public R<List<AdSlot>> active() {
        return R.ok(adService.active());
    }

    @GetMapping("/api/admin/ads")
    @RequirePerm("ad:manage")
    public R<List<AdSlot>> all() {
        return R.ok(adService.all());
    }

    @PostMapping("/api/admin/ads")
    @RequirePerm("ad:manage")
    @OpLog(module = "ad", action = "新增广告")
    public R<AdSlot> create(@RequestBody Map<String, Object> body) {
        return R.ok(adService.save(null, body));
    }

    @PutMapping("/api/admin/ads/{id}")
    @RequirePerm("ad:manage")
    @OpLog(module = "ad", action = "修改广告")
    public R<AdSlot> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(adService.save(id, body));
    }

    @DeleteMapping("/api/admin/ads/{id}")
    @RequirePerm("ad:manage")
    @OpLog(module = "ad", action = "删除广告")
    public R<Void> delete(@PathVariable Long id) {
        adService.delete(id);
        return R.ok();
    }
}