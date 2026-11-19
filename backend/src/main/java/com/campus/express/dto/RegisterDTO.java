package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 学生注册入参 */
public class RegisterDTO {
    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "学号须为4-20位字母数字")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名过长")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为6-32位")
    private String password;

    private String email;

    /** 邀请码（选填，注册时绑定邀请人） */
    private String inviteCode;

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}