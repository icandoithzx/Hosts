package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AppConfig {
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            String address = "redis://" + redisHost + ":" + redisPort;
            config.useSingleServer()
                    .setAddress(address)
                    .setConnectionMinimumIdleSize(8)
                    .setConnectionPoolSize(32)
                    .setDnsMonitoringInterval(5000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500)
                    .setTimeout(10000)
                    .setConnectTimeout(10000)
                    .setIdleConnectionTimeout(10000);
            
            RedissonClient redissonClient = Redisson.create(config);
            log.info(" RedissonClient 创建成功，地址: {}", address);
            return redissonClient;
        } catch (Exception e) {
            log.error("RedissonClient 创建失败: {}", e.getMessage());
            throw e ;
        }
    }
}
