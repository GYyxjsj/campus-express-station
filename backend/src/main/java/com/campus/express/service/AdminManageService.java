package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.NoticeDTO;
import com.campus.express.dto.ShelfDTO;
import com.campus.express.dto.StationDTO;
import com.campus.express.entity.Notice;
import com.campus.express.entity.OperationLog;
import com.campus.express.entity.Shelf;
import com.campus.express.entity.Station;
import com.campus.express.entity.SysConfig;
import com.campus.express.mapper.NoticeMapper;
import com.campus.express.mapper.OperationLogMapper;
import com.campus.express.mapper.ShelfMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysConfigMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 平台管理：驿站 / 货架 / 提醒策略配置 / 公告 / 操作日志。
 */
@Service
public class AdminManageService {

    /** 允许通过接口修改的配置键及取值范围（白名单，防任意键写入） */
    private static final Map<String, int[]> CONFIG_RANGES = new LinkedHashMap<>();

    static {
        CONFIG_RANGES.put("remind.first.delay.hours", new int[]{1, 720});
        CONFIG_RANGES.put("remind.overdue.hours", new int[]{1, 720});
        CONFIG_RANGES.put("remind.daily.max", new int[]{1, 20});
        CONFIG_RANGES.put("auth.max.valid.hours", new int[]{1, 720});
        CONFIG_RANGES.put("proxy.service.fee.min", new int[]{0, 100});
        CONFIG_RANGES.put("proxy.service.fee.max", new int[]{1, 500});
        CONFIG_RANGES.put("proxy.service.commission.percent", new int[]{0, 50});
        CONFIG_RANGES.put("proxy.service.max.hours", new int[]{1, 168});
        CONFIG_RANGES.put("wallet.recharge.min", new int[]{1, 1000});
        CONFIG_RANGES.put("wallet.recharge.max", new int[]{1, 10000});
        CONFIG_RANGES.put("identity.code.valid.hours", new int[]{1, 720});
        CONFIG_RANGES.put("notice.popup.enabled", new int[]{0, 1});
    }

    private final StationMapper stationMapper;
    private final ShelfMapper shelfMapper;
    private final SysConfigMapper configMapper;
    private final NoticeMapper noticeMapper;
    private final OperationLogMapper logMapper;

    public AdminManageService(StationMapper stationMapper, ShelfMapper shelfMapper,
                              SysConfigMapper configMapper, NoticeMapper noticeMapper,
                              OperationLogMapper logMapper) {
        this.stationMapper = stationMapper;
        this.shelfMapper = shelfMapper;
        this.configMapper = configMapper;
        this.noticeMapper = noticeMapper;
        this.logMapper = logMapper;
    }

    // ---------- 驿站 ----------

    public List<Station> stations() {
        return stationMapper.selectAll();
    }

    public Station createStation(StationDTO dto) {
        Station s = new Station();
        s.setName(dto.getName());
        s.setLocation(dto.getLocation());
        s.setOpenHours(dto.getOpenHours());
        s.setContact(dto.getContact());
        s.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        stationMapper.insert(s);
        return s;
    }

    public void updateStation(Long id, StationDTO dto) {
        Station s = stationMapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "驿站不存在");
        }
        s.setName(dto.getName());
        s.setLocation(dto.getLocation());
        s.setOpenHours(dto.getOpenHours());
        s.setContact(dto.getContact());
        if (dto.getStatus() != null) {
            s.setStatus(dto.getStatus());
        }
        stationMapper.update(s);
    }

    // ---------- 货架 ----------

    public List<Map<String, Object>> shelves(Long stationId) {
        List<Shelf> shelves = stationId == null
                ? shelfMapper.selectAll() : shelfMapper.selectByStation(stationId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Shelf shelf : shelves) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", shelf.getId());
            row.put("stationId", shelf.getStationId());
            row.put("code", shelf.getCode());
            row.put("capacity", shelf.getCapacity());
            row.put("status", shelf.getStatus());
            row.put("occupied", shelfMapper.countOccupied(shelf.getId()));
            result.add(row);
        }
        return result;
    }

    public Shelf createShelf(ShelfDTO dto) {
        Station station = stationMapper.selectById(dto.getStationId());
        if (station == null) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "驿站不存在");
        }
        Shelf shelf = new Shelf();
        shelf.setStationId(dto.getStationId());
        shelf.setCode(dto.getCode());
        shelf.setCapacity(dto.getCapacity());
        shelf.setStatus(1);
        try {
            shelfMapper.insert(shelf);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "该驿站下货架编码已存在");
        }
        return shelf;
    }

    public void setShelfStatus(Long id, Integer status) {
        if (shelfMapper.selectById(id) == null) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "货架不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "状态只能为0/1");
        }
        shelfMapper.updateStatus(id, status);
    }

    // ---------- 提醒策略配置 ----------

    public List<SysConfig> configs() {
        return configMapper.selectAll();
    }

    public void updateConfigs(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            int[] range = CONFIG_RANGES.get(key);
            if (range == null) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "不允许修改的配置项: " + key);
            }
            int v;
            try {
                v = Integer.parseInt(entry.getValue().trim());
            } catch (NumberFormatException | NullPointerException e) {
                throw new BizException(ErrorCodes.PARAM_INVALID, key + " 必须为整数");
            }
            if (v < range[0] || v > range[1]) {
                throw new BizException(ErrorCodes.PARAM_INVALID,
                        key + " 取值范围 " + range[0] + "-" + range[1]);
            }
        }
        // 语义校验：滞留阈值须大于首次催领阈值
        int firstV = mergedInt(values, "remind.first.delay.hours", 24);
        int overdueV = mergedInt(values, "remind.overdue.hours", 72);
        if (overdueV <= firstV) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "滞留阈值必须大于首次催领阈值");
        }
        if (mergedInt(values, "proxy.service.fee.max", 50) <= mergedInt(values, "proxy.service.fee.min", 2)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "代取服务费上限必须大于下限");
        }
        if (mergedInt(values, "wallet.recharge.max", 500) <= mergedInt(values, "wallet.recharge.min", 1)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "充值上限必须大于下限");
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            configMapper.upsert(entry.getKey(), entry.getValue().trim());
        }
    }

    private int mergedInt(Map<String, String> values, String key, int def) {
        String v = values.get(key);
        return v != null ? Integer.parseInt(v.trim()) : configInt(key, def);
    }

    // ---------- 公告 ----------

    public PageResult<Notice> notices(boolean publishedOnly, int page, int size) {
        long total = noticeMapper.countPage(publishedOnly);
        return new PageResult<>(total,
                noticeMapper.selectPage(publishedOnly, (page - 1) * size, size));
    }

    public Notice createNotice(Long publisherId, NoticeDTO dto) {
        Notice n = new Notice();
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        n.setIsPopup(dto.getIsPopup() == null ? 0 : dto.getIsPopup());
        n.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        n.setCategory(dto.getCategory() == null ? "NOTICE" : dto.getCategory());
        n.setPublisherId(publisherId);
        noticeMapper.insert(n);
        return n;
    }

    public void updateNotice(Long id, NoticeDTO dto) {
        Notice n = noticeMapper.selectById(id);
        if (n == null) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "公告不存在");
        }
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        if (dto.getStatus() != null) {
            n.setStatus(dto.getStatus());
        }
        if (dto.getIsPopup() != null) {
            n.setIsPopup(dto.getIsPopup());
        }
        if (dto.getPriority() != null) {
            n.setPriority(dto.getPriority());
        }
        if (dto.getCategory() != null) {
            n.setCategory(dto.getCategory());
        }
        noticeMapper.update(n);
    }

    // ---------- 操作日志 ----------

    public PageResult<OperationLog> logs(String module, String keyword, String sortKey, String order, int page, int size) {
        long total = logMapper.countPage(module, keyword);
        return new PageResult<>(total,
                logMapper.selectPage(module, keyword,
                        com.campus.express.common.Sorts.log(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    private int configInt(String key, int def) {
        try {
            String v = configMapper.selectValue(key);
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}