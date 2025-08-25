package com.example.demo.dto;

import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.HostStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.model.enums.TerminalType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HostDto {
    /**
     * 主机ID（更新时使用）
     */
    private String id;

    /**
     * 主机名称
     */
    private String hostName;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * MAC地址
     */
    private String macAddress;

    /**
     * 终端类型
     */
    private TerminalType terminalType;

    /**
     * 主机状态
     */
    private HostStatus hostStatus;

    /**
     * 在线状态
     */
    private OnlineStatus onlineStatus;

    /**
     * 授权状态
     */
    private AuthStatus authStatus;

    /**
     * 责任人
     */
    private String responsiblePerson;

    /**
     * 关联用户ID
     */
    private String userId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 操作系统
     */
    private String operatingSystem;

    /**
     * 组织架构ID
     */
    private String organizationId;

    /**
     * 最后在线时间
     */
    private LocalDateTime lastOnlineTime;

    /**
     * 授权时间
     */
    private LocalDateTime authTime;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}