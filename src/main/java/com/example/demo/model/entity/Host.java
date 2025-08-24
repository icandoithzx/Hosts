package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.HostStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.model.enums.TerminalType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hosts")
public class Host implements Serializable {
    /**
     * 主机ID，使用雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    private Long id;

    @TableField("host_name")
    private String hostName; // 主机名称

    @TableField("ip_address")
    private String ipAddress; // IP地址

    @TableField("mac_address")
    private String macAddress; // MAC地址

    @TableField("terminal_type")
    private TerminalType terminalType; // 终端类型

    @TableField("host_status")
    private HostStatus hostStatus; // 主机状态

    @TableField("online_status")
    private OnlineStatus onlineStatus; // 在线状态

    @TableField("auth_status")
    private AuthStatus authStatus; // 授权状态

    @TableField("responsible_person")
    private String responsiblePerson; // 责任人

    @TableField("version")
    private String version; // 版本号

    @TableField("operating_system")
    private String operatingSystem; // 操作系统

    @TableField("organization_id")
    private Long organizationId; // 组织架构ID

    @TableField("last_online_time")
    private LocalDateTime lastOnlineTime; // 最后在线时间

    @TableField("auth_time")
    private LocalDateTime authTime; // 授权时间

    @TableField("remarks")
    private String remarks; // 备注信息

    @TableField("created_at")
    private LocalDateTime createdAt; // 创建时间

    @TableField("updated_at")
    private LocalDateTime updatedAt; // 更新时间
}