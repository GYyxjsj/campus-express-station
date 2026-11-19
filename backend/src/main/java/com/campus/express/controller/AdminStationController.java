package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.dto.ShelfDTO;
import com.campus.express.dto.StationDTO;
import com.campus.express.entity.Shelf;
import com.campus.express.entity.Station;
import com.campus.express.security.LoginUser;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.AdminManageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 驿站与货架管理（5.5）。GET 列表对员工开放（入库工作台需要）；写操作 station:manage */
@RestController
@RequestMapping("/api/admin")
public class AdminStationController {

    private final AdminManageService manageService;

    public AdminStationController(AdminManageService manageService) {
        this.manageService = manageService;
    }

    @GetMapping("/stations")
    public R<List<Station>> stations() {
        UserContext.require();
        return R.ok(manageService.stations());
    }

    @PostMapping("/stations")
    @RequirePerm("station:manage")
    @OpLog(module = "admin", action = "创建驿站")
    public R<Station> createStation(@Valid @RequestBody StationDTO dto) {
        return R.ok(manageService.createStation(dto));
    }

    @PutMapping("/stations/{id}")
    @RequirePerm("station:manage")
    @OpLog(module = "admin", action = "更新驿站")
    public R<Void> updateStation(@PathVariable Long id, @Valid @RequestBody StationDTO dto) {
        manageService.updateStation(id, dto);
        return R.ok();
    }

    @GetMapping("/shelves")
    public R<List<Map<String, Object>>> shelves(@RequestParam(required = false) Long stationId) {
        LoginUser user = UserContext.require();
        if (stationId == null && !user.hasPerm("station:manage")) {
            // 员工必须指定驿站查询
            throw new com.campus.express.common.BizException(
                    com.campus.express.common.ErrorCodes.PARAM_INVALID, "请选择驿站");
        }
        return R.ok(manageService.shelves(stationId));
    }

    @PostMapping("/shelves")
    @RequirePerm("station:manage")
    @OpLog(module = "admin", action = "创建货架")
    public R<Shelf> createShelf(@Valid @RequestBody ShelfDTO dto) {
        return R.ok(manageService.createShelf(dto));
    }

    @PutMapping("/shelves/{id}/status")
    @RequirePerm("station:manage")
    @OpLog(module = "admin", action = "启用/停用货架")
    public R<Void> setShelfStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        manageService.setShelfStatus(id, body.get("status"));
        return R.ok();
    }
}