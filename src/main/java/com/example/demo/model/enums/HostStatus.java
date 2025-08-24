package com.example.demo.model.enums;

/**
 * 主机状态枚举
 */
public enum HostStatus {
    ACTIVE("ACTIVE", "活跃"),
    INACTIVE("INACTIVE", "不活跃"),
    MAINTENANCE("MAINTENANCE", "维护中"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String description;

    HostStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static HostStatus fromCode(String code) {
        for (HostStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的主机状态: " + code);
    }
}