package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织架构实体类
 * 用于存储从其他服务同步过来的组织架构信息
 */
@Data
@TableName("organizations")
public class Organization implements Serializable {
    
    /**
     * 组织ID，来自外部系统
     */
    @TableId(type = IdType.INPUT)
    @TableField("id")
    private String id;
    
    /**
     * 组织名称
     */
    @TableField("name")
    private String name;
    
    /**
     * 上级组织ID，根级别组织为"0"
     */
    @TableField("parent_id")
    private String parentId;
    
    /**
     * 组织级别（可选，用于层级显示）
     */
    @TableField("level")
    private Integer level;
    
    /**
     * 组织路径（可选，如：1/2/3，便于查询子组织）
     */
    @TableField("path")
    private String path;
    
    /**
     * 组织状态（1-正常，0-停用）
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 排序字段
     */
    @TableField("sort_order")
    private Integer sortOrder;
    
    /**
     * 描述信息
     */
    @TableField("description")
    private String description;
    
    /**
     * 数据来源标识
     */
    @TableField("source_system")
    private String sourceSystem;
    
    /**
     * 外部系统的数据版本号
     */
    @TableField("external_version")
    private String externalVersion;
    
    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}