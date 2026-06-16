package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRegisterDTO {

    // 规则：长度6-20，仅英文字母、数字、下划线，字母开头
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{5,19}$",
            message = "用户名长度必须为6-20位，以字母开头，仅包含字母、数字和下划线")
    private String username;

    // 规则：长度8-16，至少一大小写字母、一数字、一特殊字符(@#$%^&*)
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&*])[A-Za-z\\d@#$%^&*]{8,16}$",
            message = "密码长度8-16位，必须包含大小写字母、数字及特殊字符(@#$%^&*)")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}