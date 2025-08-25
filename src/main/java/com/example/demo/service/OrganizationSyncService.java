package com.example.demo.service;

import com.example.demo.dto.ExternalOrganizationDto;

import java.util.List;

/**
 * 组织架构同步服务接口
 * 负责从外部系统同步组织架构数据
 */
public interface OrganizationSyncService extends SyncService {
    
    /**
     * 从外部系统获取组织架构数据
     * @return 外部组织架构数据列表
     */
    List<ExternalOrganizationDto> fetchExternalOrganizations();
    
    /**
     * 同步组织架构数据
     * @param organizations 外部组织架构数据
     * @param version 数据版本
     * @return 同步结果
     */
    boolean syncOrganizations(List<ExternalOrganizationDto> organizations, String version);
}