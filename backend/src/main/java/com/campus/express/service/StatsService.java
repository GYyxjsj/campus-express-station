package com.campus.express.service;

import com.campus.express.mapper.ParcelMapper;
import com.campus.express.mapper.ProxyAuthMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.vo.StatsOverviewVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板统计：总览指标卡 + 出入库趋势 + 分布。
 */
@Service
public class StatsService {

    private final ParcelMapper parcelMapper;
    private final ProxyAuthMapper authMapper;
    private final SysUserMapper userMapper;

    public StatsService(ParcelMapper parcelMapper, ProxyAuthMapper authMapper, SysUserMapper userMapper) {
        this.parcelMapper = parcelMapper;
        this.authMapper = authMapper;
        this.userMapper = userMapper;
    }

    public StatsOverviewVO overview() {
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTodayInbound(parcelMapper.countTodayInbound());
        vo.setTodayOutbound(parcelMapper.countTodayOutbound());
        vo.setInStock(parcelMapper.countByStatus("IN_STOCK"));
        vo.setOverdue(parcelMapper.countByStatus("OVERDUE"));
        vo.setActiveAuth(authMapper.countByStatus("ACTIVE"));
        vo.setUsedAuth(authMapper.countByStatus("USED"));
        vo.setTotalUsers(userMapper.countAll());
        return vo;
    }

    public List<Map<String, Object>> trend(int days) {
        int d = Math.max(1, Math.min(days, 30));
        return parcelMapper.selectTrend(d);
    }

    public Map<String, Object> distribution() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", parcelMapper.selectStatusDistribution());
        result.put("courier", parcelMapper.selectCourierDistribution());
        return result;
    }

    /** 星期×小时 时段热力（近 N 周，1-12 周） */
    public Map<String, Object> heatmap(int weeks) {
        int w = Math.max(1, Math.min(weeks, 12));
        Map<String, Object> result = new HashMap<>();
        result.put("weeks", w);
        result.put("cells", parcelMapper.selectHeatmap(w));
        return result;
    }
}