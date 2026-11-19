package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/** 管理员创建用户入参 */
public class UserCreateDTO {
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "账号须为4-20位字母数字")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64)
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为6-32位")
    private String password;

    @NotEmpty(message = "至少分配一个角色")
    private List<String> roles;   // STAFF / ADMIN / STUDENT

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}