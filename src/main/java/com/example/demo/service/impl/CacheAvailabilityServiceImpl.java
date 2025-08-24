package com.example.demo.service.impl;

import com.example.demo.service.CacheAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存可用性检测服务实现类
 * 支持缓存热插拔功能，动态检测Redis服务状态
 */
@Slf4j
@Service("cacheAvailabilityService")
public class CacheAvailabilityServiceImpl implements CacheAvailabilityService {

    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    // 缓存状态，默认为false
    private final AtomicBoolean cacheAvailable = new AtomicBoolean(false);
    
    // 上次检测时间
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    
    // 检测间隔（毫秒），避免频繁检测
    private static final long CHECK_INTERVAL = 30000; // 30秒
    
    // 缓存状态描述
    private volatile String cacheStatus = "未初始化";

    @Override
    public boolean isCacheAvailable() {
        long currentTime = System.currentTimeMillis();
        
        // 如果距离上次检测时间超过间隔，则重新检测
        if (currentTime - lastCheckTime.get() > CHECK_INTERVAL) {
            refreshCacheStatus();
        }
        
        boolean available = cacheAvailable.get();
        log.debug("🔍 缓存可用性检测结果: {}, 状态: {}", available, cacheStatus);
        return available;
    }

    @Override
    public String getCacheStatus() {
        // 确保状态是最新的
        isCacheAvailable();
        return cacheStatus;
    }

    @Override
    public void refreshCacheStatus() {
        boolean available = checkRedisConnection();
        boolean previousStatus = cacheAvailable.get();
        cacheAvailable.set(available);
        lastCheckTime.set(System.currentTimeMillis());
        
        if (available) {
            cacheStatus = "Redis缓存服务可用";
            if (!previousStatus) {
                log.info("✅ 缓存服务恢复: Redis连接正常，切换到Redis缓存模式");
            } else {
                log.debug("🚀 Redis连接正常");
            }
        } else {
            cacheStatus = "Redis缓存服务不可用，将使用数据库直连模式";
            if (previousStatus) {
                log.warn("⚠️ 缓存服务不可用: Redis连接失败，切换到数据库直连模式");
            } else {
                log.debug("💾 Redis仍不可用，继续使用数据库直连模式");
            }
        }
    }

    /**
     * 检测Redis连接是否正常
     */
    private boolean checkRedisConnection() {
        if (redissonClient == null) {
            log.debug("🚫 RedissonClient未配置或注入失败");
            return false;
        }

        try {
            // 使用简单的操作检测连接
            redissonClient.getBucket("__cache_availability_test__").isExists();
            log.debug("✅ Redis连接检测成功");
            return true;
        } catch (Exception e) {
            log.debug("❌ Redis连接检测失败: {}", e.getMessage());
            return false;
        }
    }
}