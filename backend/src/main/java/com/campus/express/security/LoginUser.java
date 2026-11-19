package com.campus.express.security;

import java.util.Set;

/**
 * 登录态用户（JWT 解析结果）。
 */
public class LoginUser {

    private Long uid;
    private String username;
    private String realName;
    private Set<String> roles;
    private Set<String> perms;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPerm(String perm) {
        return roles != null && roles.contains("ADMIN")
                || (perms != null && perms.contains(perm));
    }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public Set<String> getPerms() { return perms; }
    public void setPerms(Set<String> perms) { this.perms = perms; }
}