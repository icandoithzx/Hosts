package com.example.demo.service.impl;

import com.example.demo.dto.ExternalOrganizationDto;
import com.example.demo.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 组织架构同步服务
 * 负责从外部系统定期同步组织架构数据
 */
@Slf4j
@Service
public class OrganizationSyncService implements ApplicationRunner {
    
    private final OrganizationService organizationService;
    private final RestTemplate restTemplate;
    
    @Value("${external.organization.api.url:http://localhost:9999/api/organizations}")
    private String externalApiUrl;
    
    @Value("${external.organization.sync.enabled:true}")
    private boolean syncEnabled;
    
    public OrganizationSyncService(OrganizationService organizationService) {
        this.organizationService = organizationService;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 应用启动时检查是否需要同步
     */
    @Override
    public void run(ApplicationArguments args) {
        if (syncEnabled) {
            log.info("🚀 应用启动，检查组织架构同步状态");
            checkAndSync();
        } else {
            log.info("⚠️ 组织架构同步功能已禁用");
        }
    }
    
    /**
     * 定时任务：每天凌晨2点检查并同步组织架构
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Async
    public void scheduledSync() {
        if (syncEnabled) {
            log.info("⏰ 定时任务触发，检查组织架构同步");
            checkAndSync();
        }
    }
    
    /**
     * 手动触发同步检查
     */
    @Async
    public void manualSync() {
        log.info("🔧 手动触发组织架构同步检查");
        checkAndSync();
    }
    
    /**
     * 检查并执行同步
     */
    private void checkAndSync() {
        try {
            // 1. 检查是否需要同步
            if (!organizationService.needSync()) {
                log.info("✨ 组织架构数据较新，无需同步");
                return;
            }
            
            // 2. 从外部系统获取数据
            log.info("📡 正在从外部系统获取组织架构数据...");
            ExternalOrganizationSyncData syncData = fetchFromExternalSystem();
            
            if (syncData == null || syncData.getOrganizations() == null || syncData.getOrganizations().isEmpty()) {
                log.warn("⚠️ 外部系统返回的组织架构数据为空");
                return;
            }
            
            // 3. 执行同步
            boolean success = organizationService.syncFromExternal(
                syncData.getOrganizations(), 
                syncData.getVersion()
            );
            
            if (success) {
                log.info("✅ 组织架构同步成功，版本: {}, 同步数量: {}", 
                        syncData.getVersion(), syncData.getOrganizations().size());
            } else {
                log.error("❌ 组织架构同步失败");
            }
            
        } catch (Exception e) {
            log.error("💥 组织架构同步过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从外部系统获取组织架构数据
     */
    private ExternalOrganizationSyncData fetchFromExternalSystem() {
        try {
            log.debug("🌐 调用外部API: {}", externalApiUrl);
            
            ResponseEntity<ExternalOrganizationSyncData> response = restTemplate.getForEntity(
                externalApiUrl, 
                ExternalOrganizationSyncData.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExternalOrganizationSyncData data = response.getBody();
                log.info("📊 成功获取外部组织架构数据，版本: {}, 数量: {}", 
                        data.getVersion(), 
                        data.getOrganizations() != null ? data.getOrganizations().size() : 0);
                return data;
            } else {
                log.warn("⚠️ 外部系统返回异常状态: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("💥 调用外部系统失败: {}", e.getMessage());
            
            // 如果是本地开发环境，返回模拟数据
            if (isLocalDevelopment()) {
                log.info("🧪 返回模拟组织架构数据用于测试");
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
    private ExternalOrganizationSyncData createMockData() {
        ExternalOrganizationSyncData mockData = new ExternalOrganizationSyncData();
        mockData.setVersion("mock-v1.0." + System.currentTimeMillis());
        mockData.setTimestamp(System.currentTimeMillis());
        
        // 创建模拟组织架构
        List<ExternalOrganizationDto> mockOrgs = List.of(
            createMockOrg("1001", "总公司", "0", 0, "公司总部"),
            createMockOrg("1002", "技术中心", "1001", 1, "负责技术研发"),
            createMockOrg("1003", "市场部", "1001", 2, "负责市场营销"),
            createMockOrg("1004", "前端团队", "1002", 1, "前端开发团队"),
            createMockOrg("1005", "后端团队", "1002", 2, "后端开发团队"),
            createMockOrg("1006", "运维团队", "1002", 3, "运维保障团队")
        );
        
        mockData.setOrganizations(mockOrgs);
        return mockData;
    }
    
    /**
     * 创建模拟组织数据
     */
    private ExternalOrganizationDto createMockOrg(String id, String name, String parentId, Integer sortOrder, String description) {
        ExternalOrganizationDto org = new ExternalOrganizationDto();
        org.setId(id);
        org.setName(name);
        org.setParentId(parentId);
        org.setSortOrder(sortOrder);
        org.setDescription(description);
        org.setStatus(1);
        return org;
    }
    
    /**
     * 外部系统组织架构同步数据结构
     */
    public static class ExternalOrganizationSyncData {
        private String version;
        private List<ExternalOrganizationDto> organizations;
        private Long timestamp;
        
        // Getters and Setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<ExternalOrganizationDto> getOrganizations() { return organizations; }
        public void setOrganizations(List<ExternalOrganizationDto> organizations) { this.organizations = organizations; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}