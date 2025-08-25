package com.example.demo.service.impl;

import com.example.demo.dto.ExternalUserDto;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * 用户同步服务
 * 负责从外部系统定期同步用户数据
 */
@Slf4j
@Service
public class UserSyncService implements ApplicationRunner {
    
    private final UserService userService;
    private final RestTemplate restTemplate;
    
    @Value("${external.user.api.url:http://localhost:9999/api/users}")
    private String externalApiUrl;
    
    @Value("${external.user.sync.enabled:true}")
    private boolean syncEnabled;
    
    public UserSyncService(UserService userService) {
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 应用启动时检查是否需要同步
     */
    @Override
    public void run(ApplicationArguments args) {
        if (syncEnabled) {
            log.info("🚀 应用启动，检查用户数据同步状态");
            checkAndSync();
        } else {
            log.info("⚠️ 用户数据同步功能已禁用");
        }
    }
    
    /**
     * 定时任务：每天凌晨3点检查并同步用户数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Async
    public void scheduledSync() {
        if (syncEnabled) {
            log.info("⏰ 定时任务触发，检查用户数据同步");
            checkAndSync();
        }
    }
    
    /**
     * 手动触发同步检查
     */
    @Async
    public void manualSync() {
        log.info("🔧 手动触发用户数据同步检查");
        checkAndSync();
    }
    
    /**
     * 检查并执行同步
     */
    private void checkAndSync() {
        try {
            // 1. 检查是否需要同步
            if (!userService.needSync()) {
                log.info("✨ 用户数据较新，无需同步");
                return;
            }
            
            // 2. 从外部系统获取数据
            log.info("📡 正在从外部系统获取用户数据...");
            ExternalUserSyncData syncData = fetchFromExternalSystem();
            
            if (syncData == null || syncData.getUsers() == null || syncData.getUsers().isEmpty()) {
                log.warn("⚠️ 外部系统返回的用户数据为空");
                return;
            }
            
            // 3. 执行同步
            boolean success = userService.syncFromExternal(
                syncData.getUsers(), 
                syncData.getVersion()
            );
            
            if (success) {
                log.info("✅ 用户数据同步成功，版本: {}, 同步数量: {}", 
                        syncData.getVersion(), syncData.getUsers().size());
            } else {
                log.error("❌ 用户数据同步失败");
            }
            
        } catch (Exception e) {
            log.error("💥 用户数据同步过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从外部系统获取用户数据
     */
    private ExternalUserSyncData fetchFromExternalSystem() {
        try {
            log.debug("🌐 调用外部API: {}", externalApiUrl);
            
            ResponseEntity<ExternalUserSyncData> response = restTemplate.getForEntity(
                externalApiUrl, 
                ExternalUserSyncData.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExternalUserSyncData data = response.getBody();
                log.info("📊 成功获取外部用户数据，版本: {}, 数量: {}", 
                        data.getVersion(), 
                        data.getUsers() != null ? data.getUsers().size() : 0);
                return data;
            } else {
                log.warn("⚠️ 外部系统返回异常状态: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("💥 调用外部系统失败: {}", e.getMessage());
            
            // 如果是本地开发环境，返回模拟数据
            if (isLocalDevelopment()) {
                log.info("🧪 返回模拟用户数据用于测试");
                return createMockData();
            }
            
            return null;
        }
    }
    
    /**
     * 判断是否为本地开发环境
     */
    private boolean isLocalDevelopment() {
        return externalApiUrl.contains("localhost") || externalApiUrl.contains("127.0.0.1");
    }
    
    /**
     * 创建模拟数据（用于本地开发测试）
     */
    private ExternalUserSyncData createMockData() {
        ExternalUserSyncData mockData = new ExternalUserSyncData();
        mockData.setVersion("mock-v1.0." + System.currentTimeMillis());
        mockData.setTimestamp(System.currentTimeMillis());
        
        // 创建模拟用户数据
        List<ExternalUserDto> mockUsers = Arrays.asList(
            createMockUser("USR001", "1001", "张三", "总公司", 5),
            createMockUser("USR002", "1002", "李四", "总公司/技术中心", 4),
            createMockUser("USR003", "1004", "王五", "总公司/技术中心/前端团队", 3),
            createMockUser("USR004", "1005", "赵六", "总公司/技术中心/后端团队", 3),
            createMockUser("USR005", "1003", "钱七", "总公司/市场部", 2)
        );
        
        mockData.setUsers(mockUsers);
        return mockData;
    }
    
    /**
     * 创建模拟用户数据
     */
    private ExternalUserDto createMockUser(String id, String orgId, String name, String orgName, 
                                          Integer mLevel) {
        ExternalUserDto user = new ExternalUserDto();
        user.setId(id);
        user.setOrgId(orgId);
        user.setName(name);
        user.setOrgName(orgName);
        user.setMLevel(mLevel);
        return user;
    }
    
    /**
     * 外部系统用户同步数据结构
     */
    public static class ExternalUserSyncData {
        private String version;
        private List<ExternalUserDto> users;
        private Long timestamp;
        
        // Getters and Setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<ExternalUserDto> getUsers() { return users; }
        public void setUsers(List<ExternalUserDto> users) { this.users = users; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}