package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.TraceAddDTO;
import com.campus.express.entity.ParcelTrace;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.TraceService;
import com.campus.express.vo.ParcelTraceVO;
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

/** 包裹去向台账：时间 / 地点 / 检查员 */
@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    /** 某包裹完整去向（学生仅本人） */
    @GetMapping("/parcel/{parcelId}")
    public R<List<ParcelTrace>> ofParcel(@PathVariable Long parcelId) {
        return R.ok(traceService.ofParcel(UserContext.require(), parcelId));
    }

    /** 台账分页（驿站员/管理员） */
    @GetMapping
    @RequirePerm("trace:manage")
    public R<PageResult<ParcelTraceVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) Long parcelId,
                                             @RequestParam(required = false) String node,
                                             @RequestParam(required = false) Long operatorId,
                                             @RequestParam(required = false) Long stationId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String from,
                                             @RequestParam(required = false) String to,
                                             @RequestParam(required = false) String sortKey,
                                             @RequestParam(required = false) String order) {
        return R.ok(traceService.page(parcelId, node, operatorId, stationId, keyword, from, to,
                sortKey, order, page, Math.min(size, 100)));
    }

    @GetMapping("/overview")
    @RequirePerm("trace:manage")
    public R<Map<String, Object>> overview() {
        return R.ok(traceService.overview());
    }

    /** 人工登记：检查 / 移架 / 上架 / 备注 */
    @PostMapping
    @RequirePerm("parcel:inspect")
    @OpLog(module = "parcel", action = "去向登记")
    public R<ParcelTrace> add(@Valid @RequestBody TraceAddDTO dto) {
        return R.ok(traceService.add(UserContext.require(), dto));
    }
}