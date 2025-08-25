package com.example.demo.model.enums;

/**
 * 授权状态枚举
 */
public enum AuthStatus {
    UNAUTHORIZED("UNAUTHORIZED", "未授权"),
    AUTHORIZED("AUTHORIZED", "已授权");

    private final String code;
    private final String description;

    AuthStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AuthStatus fromCode(String code) {
        for (AuthStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的授权状态: " + code);
    }
}