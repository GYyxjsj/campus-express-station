package com.campus.express.controller;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.OpLog;
import com.campus.express.common.PageResult;
import com.campus.express.common.R;
import com.campus.express.dto.ParcelInboundDTO;
import com.campus.express.dto.PickupDTO;
import com.campus.express.dto.ProxyPickupDTO;
import com.campus.express.security.RequirePerm;
import com.campus.express.security.UserContext;
import com.campus.express.service.ParcelService;
import com.campus.express.vo.ParcelVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 包裹管理（5.2） */
@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @PostMapping("/inbound")
    @RequirePerm("parcel:inbound")
    @OpLog(module = "parcel", action = "包裹入库")
    public R<ParcelVO> inbound(@Valid @RequestBody ParcelInboundDTO dto) {
        return R.ok(parcelService.inbound(dto));
    }

    /** 分页：学生强制只看本人（Service 内约束） */
    @GetMapping
    public R<PageResult<ParcelVO>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long stationId,
                                        @RequestParam(required = false) Boolean overdueOnly,
                                        @RequestParam(required = false) String sortKey,
                                        @RequestParam(required = false) String order) {
        return R.ok(parcelService.page(UserContext.require(), status, keyword, stationId,
                overdueOnly, sortKey, order, page, Math.min(size, 100)));
    }

    @GetMapping("/my")
    public R<PageResult<ParcelVO>> my(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(parcelService.myParcels(UserContext.require().getUid(), status, keyword,
                page, Math.min(size, 100)));
    }

    @GetMapping("/overdue")
    @RequirePerm("parcel:inbound")
    public R<PageResult<ParcelVO>> overdue(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long stationId) {
        return R.ok(parcelService.page(UserContext.require(), "OVERDUE", null, stationId,
                true, null, null, page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public R<ParcelVO> detail(@PathVariable Long id) {
        return R.ok(parcelService.detail(UserContext.require(), id));
    }

    @PostMapping("/pickup")
    @RequirePerm("parcel:pickup")
    @OpLog(module = "parcel", action = "本人取件核销")
    public R<Map<String, Object>> pickup(@Valid @RequestBody PickupDTO dto) {
        return R.ok(parcelService.pickup(UserContext.require(), dto));
    }

    @PostMapping("/proxy-pickup")
    @RequirePerm("parcel:proxy-pickup")
    @OpLog(module = "parcel", action = "代取核销")
    public R<Map<String, Object>> proxyPickup(@Valid @RequestBody ProxyPickupDTO dto) {
        return R.ok(parcelService.proxyPickup(UserContext.require(), dto));
    }

    @PostMapping("/{id}/return")
    @RequirePerm("parcel:inbound")
    @OpLog(module = "parcel", action = "包裹退回登记")
    public R<Void> returnParcel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        parcelService.returnParcel(id, reason);
        return R.ok();
    }

    /**
     * 批量导入包裹（CSV 文本或结构化行）。
     * 逐行独立入库：单行失败不影响其他行，返回逐行结果供前端展示。
     */
    @PostMapping("/import")
    @RequirePerm("parcel:inbound")
    @OpLog(module = "parcel", action = "批量导入包裹")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> importParcels(@RequestBody Map<String, Object> body) {
        Long stationId = parseLong(body.get("stationId"));
        if (stationId == null) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "请先选择入库驿站");
        }
        Long shelfId = parseLong(body.get("shelfId"));
        List<Map<String, Object>> rows = new ArrayList<>();
        Object rawRows = body.get("rows");
        if (rawRows instanceof List) {
            for (Object o : (List<Object>) rawRows) {
                if (o instanceof Map) {
                    rows.add((Map<String, Object>) o);
                }
            }
        } else {
            String csv = body.get("csv") == null ? "" : String.valueOf(body.get("csv"));
            rows.addAll(parseCsv(csv));
        }
        if (rows.isEmpty()) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "未解析到有效数据行，请检查格式：运单号,快递公司,收件手机号[,备注]");
        }
        if (rows.size() > 200) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "单次最多导入 200 行，请分批导入");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("line", i + 1);
            item.put("trackingNo", str(row.get("trackingNo")));
            try {
                ParcelInboundDTO dto = new ParcelInboundDTO();
                dto.setTrackingNo(str(row.get("trackingNo")));
                dto.setCourierCompany(str(row.get("courierCompany")).toUpperCase());
                dto.setOwnerPhone(str(row.get("ownerPhone")));
                dto.setStationId(stationId);
                dto.setShelfId(row.get("shelfId") != null ? parseLong(row.get("shelfId")) : shelfId);
                dto.setRemark(str(row.get("remark")));
                validateImportRow(dto);
                ParcelVO vo = parcelService.inbound(dto);
                item.put("success", true);
                item.put("pickupCode", vo.getPickupCode());
                success++;
            } catch (BizException e) {
                item.put("success", false);
                item.put("message", e.getMessage());
            } catch (Exception e) {
                item.put("success", false);
                item.put("message", "入库失败：" + e.getMessage());
            }
            results.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("failed", rows.size() - success);
        result.put("rows", results);
        return R.ok(result);
    }

    private void validateImportRow(ParcelInboundDTO dto) {
        if (dto.getTrackingNo().isEmpty() || dto.getTrackingNo().length() > 64) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "运单号必填且不超过 64 字");
        }
        if (dto.getCourierCompany().isEmpty()) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "快递公司必填");
        }
        if (!dto.getOwnerPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodes.IMPORT_INVALID, "收件手机号格式不正确");
        }
        if (dto.getRemark() != null && dto.getRemark().length() > 255) {
            dto.setRemark(dto.getRemark().substring(0, 255));
        }
    }

    /** CSV：首行可为表头（含「运单」字样即跳过），逗号分隔，支持带引号字段 */
    private List<Map<String, Object>> parseCsv(String csv) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String line : csv.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            List<String> cells = splitCsvLine(trimmed);
            if (cells.isEmpty()) {
                continue;
            }
            if (cells.get(0).contains("运单") || cells.get(0).equalsIgnoreCase("trackingNo")) {
                continue;
            }
            if (cells.size() < 3) {
                Map<String, Object> bad = new LinkedHashMap<>();
                bad.put("trackingNo", cells.get(0));
                bad.put("courierCompany", cells.size() > 1 ? cells.get(1) : "");
                bad.put("ownerPhone", "");
                rows.add(bad);
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("trackingNo", cells.get(0));
            row.put("courierCompany", cells.get(1));
            row.put("ownerPhone", cells.get(2));
            row.put("remark", cells.size() > 3 ? cells.get(3) : "");
            rows.add(row);
        }
        return rows;
    }

    private List<String> splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if ((c == ',' || c == '，') && !quoted) {
                cells.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cells.add(cur.toString().trim());
        return cells;
    }

    private static Long parseLong(Object o) {
        try {
            return o == null || String.valueOf(o).trim().isEmpty()
                    ? null : Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}