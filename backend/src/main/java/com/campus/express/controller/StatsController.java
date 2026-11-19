package com.campus.express.controller;

import com.campus.express.common.R;
import com.campus.express.security.RequirePerm;
import com.campus.express.service.StatsService;
import com.campus.express.vo.StatsOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 看板统计（5.5，stats:view） */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @RequirePerm("stats:view")
    public R<StatsOverviewVO> overview() {
        return R.ok(statsService.overview());
    }

    @GetMapping("/trend")
    @RequirePerm("stats:view")
    public R<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(statsService.trend(days));
    }

    @GetMapping("/distribution")
    @RequirePerm("stats:view")
    public R<Map<String, Object>> distribution() {
        return R.ok(statsService.distribution());
    }

    @GetMapping("/heatmap")
    @RequirePerm("stats:view")
    public R<Map<String, Object>> heatmap(@RequestParam(defaultValue = "4") int weeks) {
        return R.ok(statsService.heatmap(weeks));
    }
}