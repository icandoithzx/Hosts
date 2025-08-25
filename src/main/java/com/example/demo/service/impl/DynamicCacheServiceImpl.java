package com.example.demo.service.impl;

import com.example.demo.service.CacheAvailabilityService;
import com.example.demo.service.DynamicCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 动态缓存服务实现类
 * 根据Redis可用性动态切换Redis和内存缓存
 */
@Slf4j
@Service
public class DynamicCacheServiceImpl implements DynamicCacheService {

    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    @Autowired
    private CacheAvailabilityService cacheAvailabilityService;
    
    // 内存缓存存储
    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    
    // 定时清理过期缓存的线程池
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    
    public DynamicCacheServiceImpl() {
        // 每分钟清理一次过期的内存缓存
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    public void putMap(String key, Map<String, Object> value, long ttl, TimeUnit timeUnit) {
        if (isRedisAvailable()) {
            putToRedis(key, value, ttl, timeUnit);
        } else {
            putToMemory(key, value, ttl, timeUnit);
        }
    }
    
    @Override
    public Map<String, Object> getMap(String key) {
        if (isRedisAvailable()) {
            return getFromRedis(key);
        } else {
            return getFromMemory(key);
        }
    }
    
    @Override
    public boolean isMapEmpty(String key) {
        Map<String, Object> data = getMap(key);
        return data == null || data.isEmpty();
    }
    
    @Override
    public void putString(String key, String field, String value, long ttl, TimeUnit timeUnit) {
        if (isRedisAvailable()) {
            putStringToRedis(key, field, value, ttl, timeUnit);
        } else {
            putStringToMemory(key, field, value, ttl, timeUnit);
        }
    }
    
    @Override
    public String getString(String key, String field) {
        if (isRedisAvailable()) {
            return getStringFromRedis(key, field);
        } else {
            return getStringFromMemory(key, field);
        }
    }
    
    @Override
    public void evict(String key) {
        if (isRedisAvailable()) {
            evictFromRedis(key);
        } else {
            evictFromMemory(key);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return true; // 动态缓存总是可用的（要么Redis，要么内存）
    }
    
    @Override
    public String getCurrentMode() {
        return isRedisAvailable() ? "Redis" : "Memory";
    }
    
    /**
     * 检查Redis是否可用
     */
    private boolean isRedisAvailable() {
        return redissonClient != null && cacheAvailabilityService.isCacheAvailable();
    }
    
    /**
     * 存储到Redis
     */
    private void putToRedis(String key, Map<String, Object> value, long ttl, TimeUnit timeUnit) {
        try {
            RMap<String, Object> cache = redissonClient.getMap(key);
            cache.putAll(value);
            cache.expire(ttl, timeUnit);
            log.debug("🚀 Redis缓存写入成功: key={}", key);
        } catch (Exception e) {
            log.warn("⚠️ Redis缓存写入失败，降级到内存缓存: key={}, error={}", key, e.getMessage());
            putToMemory(key, value, ttl, timeUnit);
        }
    }
    
    /**
     * 从Redis获取
     */
    private Map<String, Object> getFromRedis(String key) {
        try {
            RMap<String, Object> cache = redissonClient.getMap(key);
            Map<String, Object> result = new HashMap<>(cache.readAllMap());
            log.debug("🔍 Redis缓存读取: key={}, found={}", key, !result.isEmpty());
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Redis缓存读取失败，降级到内存缓存: key={}, error={}", key, e.getMessage());
            return getFromMemory(key);
        }
    }
    
    /**
     * 存储字符串到Redis
     */
    private void putStringToRedis(String key, String field, String value, long ttl, TimeUnit timeUnit) {
        try {
            RMap<String, String> cache = redissonClient.getMap(key);
            cache.put(field, value);
            cache.expire(ttl, timeUnit);
            log.debug("🚀 Redis字符串缓存写入成功: key={}, field={}", key, field);
        } catch (Exception e) {
            log.warn("⚠️ Redis字符串缓存写入失败，降级到内存缓存: key={}, field={}, error={}", key, field, e.getMessage());
            putStringToMemory(key, field, value, ttl, timeUnit);
        }
    }
    
    /**
     * 从Redis获取字符串
     */
    private String getStringFromRedis(String key, String field) {
        try {
            RMap<String, String> cache = redissonClient.getMap(key);
            String result = cache.get(field);
            log.debug("🔍 Redis字符串缓存读取: key={}, field={}, found={}", key, field, result != null);
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Redis字符串缓存读取失败，降级到内存缓存: key={}, field={}, error={}", key, field, e.getMessage());
            return getStringFromMemory(key, field);
        }
    }
    
    /**
     * 从Redis删除
     */
    private void evictFromRedis(String key) {
        try {
            redissonClient.getBucket(key).delete();
            log.debug("🗑️ Redis缓存删除成功: key={}", key);
        } catch (Exception e) {
            log.warn("⚠️ Redis缓存删除失败: key={}, error={}", key, e.getMessage());
        }
    }
    
    /**
     * 存储到内存
     */
    private void putToMemory(String key, Map<String, Object> value, long ttl, TimeUnit timeUnit) {
        long expirationTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        memoryCache.put(key, new CacheEntry(value, expirationTime));
        log.debug("💾 内存缓存写入成功: key={}", key);
    }
    
    /**
     * 从内存获取
     */
    private Map<String, Object> getFromMemory(String key) {
        CacheEntry entry = memoryCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                memoryCache.remove(key); // 移除过期条目
            }
            log.debug("💾 内存缓存读取: key={}, found=false", key);
            return new HashMap<>();
        }
        log.debug("💾 内存缓存读取: key={}, found=true", key);
        return new HashMap<>(entry.getData());
    }
    
    /**
     * 存储字符串到内存
     */
    @SuppressWarnings("unchecked")
    private void putStringToMemory(String key, String field, String value, long ttl, TimeUnit timeUnit) {
        long expirationTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        CacheEntry entry = memoryCache.get(key);
        
        Map<String, Object> data;
        if (entry == null || entry.isExpired()) {
            data = new HashMap<>();
        } else {
            data = new HashMap<>(entry.getData());
        }
        
        data.put(field, value);
        memoryCache.put(key, new CacheEntry(data, expirationTime));
        log.debug("💾 内存字符串缓存写入成功: key={}, field={}", key, field);
    }
    
    /**
     * 从内存获取字符串
     */
    private String getStringFromMemory(String key, String field) {
        CacheEntry entry = memoryCache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                memoryCache.remove(key); // 移除过期条目
            }
            log.debug("💾 内存字符串缓存读取: key={}, field={}, found=false", key, field);
            return null;
        }
        
        Object value = entry.getData().get(field);
        log.debug("💾 内存字符串缓存读取: key={}, field={}, found={}", key, field, value != null);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 从内存删除
     */
    private void evictFromMemory(String key) {
        memoryCache.remove(key);
        log.debug("💾 内存缓存删除成功: key={}", key);
    }
    
    /**
     * 清理过期的内存缓存条目
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        for (Map.Entry<String, CacheEntry> entry : memoryCache.entrySet()) {
            if (entry.getValue().getExpirationTime() < currentTime) {
                memoryCache.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("🧹 清理过期内存缓存条目: 数量={}, 剩余={}", removedCount, memoryCache.size());
        }
    }
    
    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        private final Map<String, Object> data;
        private final long expirationTime;
        
        public CacheEntry(Map<String, Object> data, long expirationTime) {
            this.data = data;
            this.expirationTime = expirationTime;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}