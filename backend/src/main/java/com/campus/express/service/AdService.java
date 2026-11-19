package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.entity.AdSlot;
import com.campus.express.mapper.AdMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 广告位：前台按位取生效广告，管理端维护 */
@Service
public class AdService {

    private static final Set<String> SLOTS = new HashSet<>(Arrays.asList("HOME_BANNER", "PICKUP_SIDE", "WALLET_FOOT"));
    private static final Set<String> THEMES = new HashSet<>(Arrays.asList("neutral", "warm", "cool"));

    private final AdMapper adMapper;

    public AdService(AdMapper adMapper) {
        this.adMapper = adMapper;
    }

    public List<AdSlot> active() {
        return adMapper.selectActive();
    }

    public List<AdSlot> all() {
        return adMapper.selectAll();
    }

    @Transactional
    public AdSlot save(Long id, Map<String, Object> body) {
        String slotKey = str(body.get("slotKey")).toUpperCase();
        if (!SLOTS.contains(slotKey)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "广告位须为 HOME_BANNER/PICKUP_SIDE/WALLET_FOOT");
        }
        String title = str(body.get("title"));
        if (title.isEmpty() || title.length() > 64) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "广告标题必填且不超过 64 字");
        }
        String theme = str(body.get("theme"));
        if (theme.isEmpty()) { theme = "neutral"; }
        if (!THEMES.contains(theme)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "主题须为 neutral/warm/cool");
        }
        String link = str(body.get("linkUrl"));
        if (!link.isEmpty() && !link.startsWith("https://") && !link.startsWith("http://")
                && !link.startsWith("/")) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "跳转链接须为 http(s) 或站内路径");
        }
        AdSlot ad = new AdSlot();
        ad.setSlotKey(slotKey);
        ad.setTitle(title);
        ad.setContent(limit(str(body.get("content")), 200));
        ad.setLinkUrl(limit(link, 200));
        ad.setTheme(theme);
        ad.setWeight(intVal(body.get("weight"), 0));
        ad.setStatus(truthy(body.get("status")) ? 1 : 0);
        ad.setStartAt(time(body.get("startAt")));
        ad.setEndAt(time(body.get("endAt")));
        if (ad.getStartAt() != null && ad.getEndAt() != null && ad.getEndAt().isBefore(ad.getStartAt())) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "结束时间不能早于开始时间");
        }
        if (id == null) {
            adMapper.insert(ad);
            return adMapper.selectById(ad.getId());
        }
        if (adMapper.selectById(id) == null) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "广告不存在");
        }
        ad.setId(id);
        adMapper.update(ad);
        return adMapper.selectById(id);
    }

    @Transactional
    public void delete(Long id) {
        adMapper.delete(id);
    }

    private static OffsetDateTime time(Object o) {
        String s = str(o);
        if (s.isEmpty()) { return null; }
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "投放时间格式无效");
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static int intVal(Object o, int def) {
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean truthy(Object o) {
        if (o == null) { return false; }
        String s = String.valueOf(o).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
}