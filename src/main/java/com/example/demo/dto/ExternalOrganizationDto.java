package com.example.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 外部系统组织架构同步请求DTO
 */
@Data
public class ExternalOrganizationDto {
    
    /**
     * 组织ID
     */
    private String id;
    
    /**
     * 组织名称
     */
    private String name;
    
    /**
     * 上级组织ID，根级别为"0"
     */
    private String parentId;
    
    /**
     * 排序字段
     */
    private Integer sortOrder;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 组织状态（1-正常，0-停用）
     */
    private Integer status;
}

/**
 * 外部系统组织架构全量同步响应DTO
 */
@Data
class ExternalOrganizationSyncResponse {
    
    /**
     * 数据版本号
     */
    private String version;
    
    /**
     * 组织架构列表
     */
    private List<ExternalOrganizationDto> organizations;
    
    /**
     * 同步时间戳
     */
    private Long timestamp;
}