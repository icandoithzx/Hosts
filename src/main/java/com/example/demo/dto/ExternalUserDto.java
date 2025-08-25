package com.example.demo.dto;

import lombok.Data;

/**
 * 外部系统用户同步请求DTO
 */
@Data
public class ExternalUserDto {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 组织架构ID
     */
    private String orgId;
    
    /**
     * 用户名称
     */
    private String name;
    
    /**
     * 组织架构的全名称
     */
    private String orgName;
    
    /**
     * 用户的等级
     */
    private Integer mLevel;
}