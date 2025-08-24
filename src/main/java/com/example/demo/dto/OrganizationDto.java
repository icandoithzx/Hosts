package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织架构数据传输对象
 */
@Data
public class OrganizationDto {
    
    /**
     * 组织ID
     */
    private String id;
    
    /**
     * 组织名称
     */
    private String name;
    
    /**
     * 上级组织ID
     */
    private String parentId;
    
    /**
     * 组织级别
     */
    private Integer level;
    
    /**
     * 组织路径
     */
    private String path;
    
    /**
     * 组织状态
     */
    private Integer status;
    
    /**
     * 排序字段
     */
    private Integer sortOrder;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 子组织列表（用于树形结构展示）
     */
    private List<OrganizationDto> children;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}