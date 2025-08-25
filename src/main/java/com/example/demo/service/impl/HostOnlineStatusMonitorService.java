package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.mapper.HostMapper;
import com.example.demo.model.entity.Host;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.service.CacheAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主机在线状态监控服务
 * 负责定时检查主机在线状态，将长时间无心跳的主机标记为离线
 */
@Slf4j
@Service
public class HostOnlineStatusMonitorService {

    @Autowired
    private HostMapper hostMapper;
    
    @Autowired
    private CacheAvailabilityService cacheAvailabilityService;
    
    @Autowired
    private CacheManager cacheManager;
    
    // 心跳超时时间（分钟）
    private static final int HEARTBEAT_TIMEOUT_MINUTES = 5;
    
    /**
     * 定时检查主机在线状态
     * 每2分钟执行一次，检查是否有主机超过5分钟没有心跳
     */
    @Scheduled(fixedRate = 120000) // 每2分钟执行一次
    @Transactional
    public void checkOfflineHosts() {
        try {
            log.debug("🔍 开始检查离线主机...");
            
            // 计算超时时间点
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
            
            // 查询当前标记为在线但最后在线时间超过阈值的主机
            QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("online_status", OnlineStatus.ONLINE)
                       .and(wrapper -> wrapper
                           .lt("last_online_time", timeoutThreshold)
                           .or()
                           .isNull("last_online_time")
                       );
            
            List<Host> offlineHosts = hostMapper.selectList(queryWrapper);
            
            if (!offlineHosts.isEmpty()) {
                log.info("📴 发现 {} 个离线主机，准备更新状态", offlineHosts.size());
                
                LocalDateTime now = LocalDateTime.now();
                for (Host host : offlineHosts) {
                    // 更新主机为离线状态
                    Host updateHost = new Host();
                    updateHost.setId(host.getId());
                    updateHost.setOnlineStatus(OnlineStatus.OFFLINE);
                    updateHost.setUpdatedAt(now);
                    
                    hostMapper.updateById(updateHost);
                    
                    // 清除相关缓存
                    evictHostCache(host);
                    
                    log.info("📴 主机 {} (ID: {}) 已标记为离线，最后在线时间: {}", 
                            host.getHostName(), host.getId(), 
                            host.getLastOnlineTime() != null ? host.getLastOnlineTime() : "从未在线");
                }
                
                log.info("✅ 离线主机状态更新完成，共处理 {} 个主机", offlineHosts.size());
            } else {
                log.debug("✅ 所有主机状态正常，无需更新");
            }
            
        } catch (Exception e) {
            log.error("❌ 检查离线主机失败", e);
        }
    }
    
    /**
     * 清除主机相关的缓存
     */
    private void evictHostCache(Host host) {
        try {
            if (cacheAvailabilityService.isCacheAvailable() && cacheManager != null) {
                Cache hostsCache = cacheManager.getCache("hosts");
                if (hostsCache != null) {
                    // 清除单个主机缓存
                    hostsCache.evict(host.getId());
                    
                    // 清除MAC地址缓存
                    if (host.getMacAddress() != null) {
                        hostsCache.evict("mac:" + host.getMacAddress());
                    }
                    
                    // 清除组织级别缓存
                    if (host.getOrganizationId() != null) {
                        hostsCache.evict("org:" + host.getOrganizationId());
                    }
                    
                    // 清除IP+组织缓存
                    if (host.getIpAddress() != null && host.getOrganizationId() != null) {
                        hostsCache.evict("ip_org:" + host.getIpAddress() + ":" + host.getOrganizationId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 清除主机缓存失败: hostId={}, error={}", host.getId(), e.getMessage());
        }
    }
    
    /**
     * 获取在线主机统计信息
     */
    public HostOnlineStatistics getOnlineStatistics() {
        try {
            QueryWrapper<Host> onlineQuery = new QueryWrapper<>();
            onlineQuery.eq("online_status", OnlineStatus.ONLINE);
            long onlineCount = hostMapper.selectCount(onlineQuery);
            
            QueryWrapper<Host> offlineQuery = new QueryWrapper<>();
            offlineQuery.eq("online_status", OnlineStatus.OFFLINE);
            long offlineCount = hostMapper.selectCount(offlineQuery);
            
            QueryWrapper<Host> totalQuery = new QueryWrapper<>();
            long totalCount = hostMapper.selectCount(totalQuery);
            
            return new HostOnlineStatistics(onlineCount, offlineCount, totalCount);
            
        } catch (Exception e) {
            log.error("❌ 获取主机在线统计失败", e);
            return new HostOnlineStatistics(0, 0, 0);
        }
    }
    
    /**
     * 主机在线统计信息
     */
    public static class HostOnlineStatistics {
        private final long onlineCount;
        private final long offlineCount;
        private final long totalCount;
        
        public HostOnlineStatistics(long onlineCount, long offlineCount, long totalCount) {
            this.onlineCount = onlineCount;
            this.offlineCount = offlineCount;
            this.totalCount = totalCount;
        }
        
        public long getOnlineCount() { return onlineCount; }
        public long getOfflineCount() { return offlineCount; }
        public long getTotalCount() { return totalCount; }
        public double getOnlineRate() { 
            return totalCount > 0 ? (double) onlineCount / totalCount : 0.0; 
        }
        
        @Override
        public String toString() {
            return String.format("主机统计 [总数: %d, 在线: %d, 离线: %d, 在线率: %.2f%%]", 
                    totalCount, onlineCount, offlineCount, getOnlineRate() * 100);
        }
    }
}