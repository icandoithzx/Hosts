package com.example.demo.service;

/**
 * 缓存可用性检测服务
 * 用于动态检测缓存服务是否可用，支持缓存热插拔功能
 */
public interface CacheAvailabilityService {
    
    /**
     * 检测缓存服务是否可用
     * @return true: 缓存可用，false: 缓存不可用
     */
    boolean isCacheAvailable();
    
    /**
     * 获取缓存状态描述
     * @return 缓存状态信息
     */
    String getCacheStatus();
    
    /**
     * 强制刷新缓存可用性状态
     */
    void refreshCacheStatus();
}