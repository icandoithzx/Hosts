package com.example.demo.service;

import com.example.demo.dto.ExternalUserDto;

import java.util.List;

/**
 * 用户同步服务接口
 * 负责从外部系统同步用户数据
 */
public interface UserSyncService extends SyncService {
    
    /**
     * 从外部系统获取用户数据
     * @return 外部用户数据列表
     */
    List<ExternalUserDto> fetchExternalUsers();
    
    /**
     * 同步用户数据
     * @param users 外部用户数据
     * @param version 数据版本
     * @return 同步结果
     */
    boolean syncUsers(List<ExternalUserDto> users, String version);
}