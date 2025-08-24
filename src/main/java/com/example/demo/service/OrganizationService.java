package com.example.demo.service;

import com.example.demo.dto.ExternalOrganizationDto;
import com.example.demo.dto.OrganizationDto;
import com.example.demo.model.entity.Organization;

import java.util.List;

/**
 * 组织架构服务接口
 */
public interface OrganizationService {
    
    /**
     * 获取所有组织的树形结构
     * @return 组织树
     */
    List<OrganizationDto> getOrganizationTree();
    
    /**
     * 根据组织ID获取组织信息
     * @param id 组织ID
     * @return 组织信息
     */
    Organization getById(String id);
    
    /**
     * 根据上级组织ID获取子组织
     * @param parentId 上级组织ID
     * @return 子组织列表
     */
    List<Organization> getByParentId(String parentId);
    
    /**
     * 获取所有组织的扁平列表
     * @return 组织列表
     */
    List<Organization> getAllOrganizations();
    
    /**
     * 从外部系统全量同步组织架构
     * @param externalOrganizations 外部系统的组织数据
     * @param version 数据版本号
     * @return 同步结果
     */
    boolean syncFromExternal(List<ExternalOrganizationDto> externalOrganizations, String version);
    
    /**
     * 检查是否需要同步组织架构
     * @return true-需要同步，false-不需要同步
     */
    boolean needSync();
    
    /**
     * 获取当前组织架构的数据版本
     * @return 数据版本号
     */
    String getCurrentVersion();
    
    /**
     * 获取组织架构统计信息
     * @return 统计信息
     */
    java.util.Map<String, Object> getStatistics();
}