package com.example.demo.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 动态缓存服务接口
 * 抽象Redis和内存缓存操作，支持运行时动态切换
 */
public interface DynamicCacheService {
    
    /**
     * 存储Map类型的缓存数据
     * @param key 缓存键
     * @param value 缓存值（Map格式）
     * @param ttl 过期时间
     * @param timeUnit 时间单位
     */
    void putMap(String key, Map<String, Object> value, long ttl, TimeUnit timeUnit);
    
    /**
     * 获取Map类型的缓存数据
     * @param key 缓存键
     * @return 缓存值（Map格式），如果不存在则返回空Map
     */
    Map<String, Object> getMap(String key);
    
    /**
     * 检查Map类型缓存是否为空
     * @param key 缓存键
     * @return true表示为空，false表示有数据
     */
    boolean isMapEmpty(String key);
    
    /**
     * 存储字符串类型的缓存数据
     * @param key 缓存键
     * @param field 字段名
     * @param value 字段值
     * @param ttl 过期时间
     * @param timeUnit 时间单位
     */
    void putString(String key, String field, String value, long ttl, TimeUnit timeUnit);
    
    /**
     * 获取字符串类型的缓存数据
     * @param key 缓存键
     * @param field 字段名
     * @return 字段值，如果不存在则返回null
     */
    String getString(String key, String field);
    
    /**
     * 删除缓存
     * @param key 缓存键
     */
    void evict(String key);
    
    /**
     * 检查缓存是否可用
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();
    
    /**
     * 获取当前缓存模式
     * @return "Redis" 或 "Memory"
     */
    String getCurrentMode();
}