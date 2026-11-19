package com.campus.express.vo;

import java.util.Map;

/** 看板总览 VO */
public class StatsOverviewVO {
    private long todayInbound;
    private long todayOutbound;
    private long inStock;
    private long overdue;
    private long activeAuth;
    private long usedAuth;
    private long totalUsers;

    public long getTodayInbound() { return todayInbound; }
    public void setTodayInbound(long todayInbound) { this.todayInbound = todayInbound; }
    public long getTodayOutbound() { return todayOutbound; }
    public void setTodayOutbound(long todayOutbound) { this.todayOutbound = todayOutbound; }
    public long getInStock() { return inStock; }
    public void setInStock(long inStock) { this.inStock = inStock; }
    public long getOverdue() { return overdue; }
    public void setOverdue(long overdue) { this.overdue = overdue; }
    public long getActiveAuth() { return activeAuth; }
    public void setActiveAuth(long activeAuth) { this.activeAuth = activeAuth; }
    public long getUsedAuth() { return usedAuth; }
    public void setUsedAuth(long usedAuth) { this.usedAuth = usedAuth; }
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
}