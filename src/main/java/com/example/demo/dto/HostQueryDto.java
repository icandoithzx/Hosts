package com.example.demo.dto;

import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.HostStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.model.enums.TerminalType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HostQueryDto {
    /**
     * 主机名称（模糊查询）
     */
    private String hostName;

    /**
     * IP地址或MAC地址（支持IP或MAC地址查询）
     */
    private String ipOrMacAddress;

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
     * 责任人（模糊查询）
     */
    private String responsiblePerson;

    /**
     * 组织架构ID
     */
    private String organizationId;

    /**
     * 创建时间开始
     */
    private LocalDateTime createdAtStart;

    /**
     * 创建时间结束
     */
    private LocalDateTime createdAtEnd;

    /**
     * 最后在线时间开始
     */
    private LocalDateTime lastOnlineTimeStart;

    /**
     * 最后在线时间结束
     */
    private LocalDateTime lastOnlineTimeEnd;

    /**
     * 页码（默认第1页）
     */
    private Integer page = 1;

    /**
     * 每页大小（默认10条）
     */
    private Integer size = 10;

    /**
     * 排序字段（默认按创建时间倒序）
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向（ASC/DESC，默认DESC）
     */
    private String sortDirection = "DESC";
}