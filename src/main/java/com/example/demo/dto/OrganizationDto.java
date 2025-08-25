package com.example.demo.dto;

import lombok.Data;

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
     * 是否有子部门（0-无子部门/叶子节点，1-有子部门/非叶子节点）
     */
    private Integer leaf;
    
    /**
     * 子组织列表（用于树形结构展示）
     */
    private List<OrganizationDto> children;
}