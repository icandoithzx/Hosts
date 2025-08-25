package com.example.demo.service.impl;

import com.example.demo.dto.ExternalUserDto;
import com.example.demo.service.UserService;
import com.example.demo.service.UserSyncService;
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
 * 用户同步服务实现类
 * 实现ApplicationRunner接口，在应用启动时自动检查同步
 */
@Slf4j
@Service
public class UserSyncServiceImpl implements UserSyncService, ApplicationRunner {
    
    private final UserService userService;
    private final RestTemplate restTemplate;
    
    @Value("${external.user.api.url:http://localhost:9999/api/users}")
    private String externalApiUrl;
    
    @Value("${external.user.sync.enabled:true}")
    private boolean syncEnabled;
    
    public UserSyncServiceImpl(UserService userService) {
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public void run(ApplicationArguments args) {
        initializeSync();
    }
    
    @Override
    public String getServiceName() {
        return "用户同步服务";
    }
    
    @Override
    public boolean needSync() {
        if (!syncEnabled) {
            log.debug("📋 {} - 同步功能已禁用", getServiceName());
            return false;
        }
        return userService.needSync();
    }
    
    @Override
    public boolean executeSync() {
        try {
            log.info("🔄 {} - 开始执行同步操作", getServiceName());
            
            List<ExternalUserDto> users = fetchExternalUsers();
            if (users == null || users.isEmpty()) {
                log.warn("⚠️ {} - 外部系统返回的数据为空", getServiceName());
                return false;
            }
            
            String version = "v1.0." + System.currentTimeMillis();
            boolean success = syncUsers(users, version);
            
            if (success) {
                log.info("✅ {} - 同步成功，版本: {}, 数量: {}", 
                        getServiceName(), version, users.size());
            } else {
                log.error("❌ {} - 同步失败", getServiceName());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("💥 {} - 同步过程中发生异常: {}", getServiceName(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点执行（避免与组织架构同步冲突）
    @Async
    public void scheduledSync() {
        log.info("⏰ {} - 定时任务触发", getServiceName());
        if (needSync()) {
            executeSync();
        } else {
            log.info("✨ {} - 数据较新，无需同步", getServiceName());
        }
    }
    
    @Override
    @Async
    public void manualSync() {
        log.info("🔧 {} - 手动触发同步", getServiceName());
        executeSync();
    }
    
    @Override
    public void initializeSync() {
        if (syncEnabled) {
            log.info("🚀 {} - 应用启动，检查同步状态", getServiceName());
            if (needSync()) {
                executeSync();
            } else {
                log.info("✨ {} - 数据较新，无需同步", getServiceName());
            }
        } else {
            log.info("⚠️ {} - 同步功能已禁用", getServiceName());
        }
    }
    
    @Override
    public List<ExternalUserDto> fetchExternalUsers() {
        try {
            log.debug("🌐 {} - 调用外部API: {}", getServiceName(), externalApiUrl);
            
            ResponseEntity<ExternalUserSyncData> response = restTemplate.getForEntity(
                externalApiUrl, 
                ExternalUserSyncData.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExternalUserSyncData data = response.getBody();
                log.info("📊 {} - 成功获取外部数据，版本: {}, 数量: {}", 
                        getServiceName(),
                        data.getVersion(), 
                        data.getUsers() != null ? data.getUsers().size() : 0);
                return data.getUsers();
            } else {
                log.warn("⚠️ {} - 外部系统返回异常状态: {}", getServiceName(), response.getStatusCode());
                return createMockData();
            }
            
        } catch (Exception e) {
            log.error("💥 {} - 调用外部系统失败: {}", getServiceName(), e.getMessage());
            
            // 如果是本地开发环境，返回模拟数据
            if (isLocalDevelopment()) {
                log.info("🧪 {} - 返回模拟数据用于测试", getServiceName());
                return createMockData();
            }
            
            return null;
        }
    }
    
    @Override
    public boolean syncUsers(List<ExternalUserDto> users, String version) {
        return userService.syncFromExternal(users, version);
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
    private List<ExternalUserDto> createMockData() {
        return Arrays.asList(
            createMockUser("U001", "1001", "张三", "总公司", 8),
            createMockUser("U002", "1002", "李四", "技术中心", 5),
            createMockUser("U003", "1002", "王五", "技术中心", 6),
            createMockUser("U004", "1003", "赵六", "市场部", 7),
            createMockUser("U005", "1003", "孙七", "市场部", 4)
        );
    }
    
    /**
     * 创建模拟用户数据
     */
    private ExternalUserDto createMockUser(String id, String orgId, String name, String orgName, Integer mLevel) {
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
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<ExternalUserDto> getUsers() { return users; }
        public void setUsers(List<ExternalUserDto> users) { this.users = users; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}