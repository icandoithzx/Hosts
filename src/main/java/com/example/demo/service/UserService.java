package com.example.demo.service;

import com.example.demo.dto.ExternalUserDto;
import com.example.demo.model.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    User getById(String id);
    
    /**
     * 根据组织ID获取用户列表
     * @param orgId 组织ID
     * @return 用户列表
     */
    List<User> getByOrgId(String orgId);
    
    /**
     * 根据用户等级获取用户列表
     * @param mLevel 用户等级
     * @return 用户列表
     */
    List<User> getByMLevel(Integer mLevel);
    
    /**
     * 获取所有用户的扁平列表
     * @return 用户列表
     */
    List<User> getAllUsers();
    
    /**
     * 从外部系统全量同步用户数据
     * @param externalUsers 外部系统的用户数据
     * @param version 数据版本号
     * @return 同步结果
     */
    boolean syncFromExternal(List<ExternalUserDto> externalUsers, String version);
    
    /**
     * 检查是否需要同步用户数据
     * @return true-需要同步，false-不需要同步
     */
    boolean needSync();
    
    /**
     * 获取当前用户数据的版本
     * @return 数据版本号
     */
    String getCurrentVersion();
    
    /**
     * 获取用户统计信息
     * @return 统计信息
     */
    Map<String, Object> getStatistics();
}