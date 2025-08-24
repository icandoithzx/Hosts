package com.example.demo.model.enums;

/**
 * 终端类型枚举
 */
public enum TerminalType {
    PC("PC", "个人电脑"),
    SERVER("SERVER", "服务器"),
    MOBILE("MOBILE", "移动设备"),
    TABLET("TABLET", "平板电脑"),
    EMBEDDED("EMBEDDED", "嵌入式设备"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    TerminalType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TerminalType fromCode(String code) {
        for (TerminalType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的终端类型: " + code);
    }
}