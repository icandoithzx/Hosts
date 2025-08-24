package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 * 支持缓存热插拔功能：
 * - 有Redis配置时自动使用Redis缓存
 * - 无Redis配置时自动降级到内存缓存
 * - 运行时Redis连接失败时自动降级
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * Redis缓存管理器（当RedissonClient可用时）- 优先使用
     */
    @Bean("redisCacheManager")
    @ConditionalOnBean(RedissonClient.class)
    @Primary
    public CacheManager redisCacheManager() throws IOException {
        log.info("✅ 配置Redis缓存管理器作为主要缓存管理器 - Redis缓存优先");
        
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();
        
        // 策略缓存配置 - 长期缓存，1小时过期
        config.put("policies", new org.redisson.spring.cache.CacheConfig(
                3600000,  // TTL: 1 hour in milliseconds
                1800000   // Max idle time: 30 minutes in milliseconds
        ));
        
        // 客户端策略关联缓存 - 中期缓存，30分钟过期
        config.put("clientPolicies", new org.redisson.spring.cache.CacheConfig(
                1800000,  // TTL: 30 minutes in milliseconds
                900000    // Max idle time: 15 minutes in milliseconds
        ));
        
        // 心跳服务高频缓存 - 短期缓存，5分钟过期
        config.put("heartbeatCache", new org.redisson.spring.cache.CacheConfig(
                300000,   // TTL: 5 minutes in milliseconds
                150000    // Max idle time: 2.5 minutes in milliseconds
        ));
        
        // 客户端策略哈希缓存 - 超短期高频缓存，1分钟过期
        config.put("policyHash", new org.redisson.spring.cache.CacheConfig(
                60000,    // TTL: 1 minute in milliseconds
                30000     // Max idle time: 30 seconds in milliseconds
        ));
        
        // 客户端有效策略缓存
        config.put("clientEffectivePolicies", new org.redisson.spring.cache.CacheConfig(
                1800000,  // TTL: 30 minutes in milliseconds
                900000    // Max idle time: 15 minutes in milliseconds
        ));
        
        // 主机管理缓存 - 中期缓存，30分钟过期
        config.put("hosts", new org.redisson.spring.cache.CacheConfig(
                1800000,  // TTL: 30 minutes in milliseconds
                900000    // Max idle time: 15 minutes in milliseconds
        ));
        
        // 默认缓存配置 - 15分钟过期
        config.put("default", new org.redisson.spring.cache.CacheConfig(
                900000,   // TTL: 15 minutes in milliseconds
                450000    // Max idle time: 7.5 minutes in milliseconds
        ));

        log.info("🚀 Redis缓存管理器创建成功，缓存配置项: {}", config.keySet());
        return new RedissonSpringCacheManager(redissonClient, config);
    }

    /**
     * 内存缓存管理器（备用方案，仅当Redis不可用时使用）
     */
    @Bean("fallbackCacheManager")
    @ConditionalOnMissingBean(name = "redisCacheManager")
    public CacheManager fallbackCacheManager() {
        log.warn("⚠️  Redis不可用，使用内存缓存管理器作为备用方案");
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "policies", 
                "clientPolicies", 
                "heartbeatCache", 
                "policyHash", 
                "clientEffectivePolicies", 
                "hosts",
                "default"
        );
        log.info("💾 内存缓存管理器创建成功，缓存名称: {}", cacheManager.getCacheNames());
        return cacheManager;
    }
}