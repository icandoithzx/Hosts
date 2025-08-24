package com.example.demo.model.enums;

/**
 * 在线状态枚举
 */
public enum OnlineStatus {
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线");

    private final String code;
    private final String description;

    OnlineStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OnlineStatus fromCode(String code) {
        for (OnlineStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的在线状态: " + code);
    }
}