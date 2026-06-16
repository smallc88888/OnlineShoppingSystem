package com.shop.dto;

public class ApiResponse<T> {
    private int code;       // 状态码：200成功，400参数错误等
    private String message; // 给前端展示的提示信息
    private T data;         // 具体的业务数据（如登录成功后的用户信息）

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}