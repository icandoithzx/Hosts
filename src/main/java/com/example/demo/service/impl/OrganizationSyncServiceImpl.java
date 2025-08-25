package com.example.demo.service.impl;

import com.example.demo.dto.ExternalOrganizationDto;
import com.example.demo.service.OrganizationService;
import com.example.demo.service.OrganizationSyncService;
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
 * 组织架构同步服务实现类
 * 实现ApplicationRunner接口，在应用启动时自动检查同步
 */
@Slf4j
@Service
public class OrganizationSyncServiceImpl implements OrganizationSyncService, ApplicationRunner {
    
    private final OrganizationService organizationService;
    private final RestTemplate restTemplate;
    
    @Value("${external.organization.api.url:http://localhost:9999/api/organizations}")
    private String externalApiUrl;
    
    @Value("${external.organization.sync.enabled:true}")
    private boolean syncEnabled;
    
    public OrganizationSyncServiceImpl(OrganizationService organizationService) {
        this.organizationService = organizationService;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public void run(ApplicationArguments args) {
        initializeSync();
    }
    
    @Override
    public String getServiceName() {
        return "组织架构同步服务";
    }
    
    @Override
    public boolean needSync() {
        if (!syncEnabled) {
            log.debug("📋 {} - 同步功能已禁用", getServiceName());
            return false;
        }
        return organizationService.needSync();
    }
    
    @Override
    public boolean executeSync() {
        try {
            log.info("🔄 {} - 开始执行同步操作", getServiceName());
            
            List<ExternalOrganizationDto> organizations = fetchExternalOrganizations();
            if (organizations == null || organizations.isEmpty()) {
                log.warn("⚠️ {} - 外部系统返回的数据为空", getServiceName());
                return false;
            }
            
            String version = "v1.0." + System.currentTimeMillis();
            boolean success = syncOrganizations(organizations, version);
            
            if (success) {
                log.info("✅ {} - 同步成功，版本: {}, 数量: {}", 
                        getServiceName(), version, organizations.size());
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
    @Scheduled(cron = "0 0 2 * * ?")
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
    public List<ExternalOrganizationDto> fetchExternalOrganizations() {
        try {
            log.debug("🌐 {} - 调用外部API: {}", getServiceName(), externalApiUrl);
            
            ResponseEntity<ExternalOrganizationSyncData> response = restTemplate.getForEntity(
                externalApiUrl, 
                ExternalOrganizationSyncData.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExternalOrganizationSyncData data = response.getBody();
                log.info("📊 {} - 成功获取外部数据，版本: {}, 数量: {}", 
                        getServiceName(),
                        data.getVersion(), 
                        data.getOrganizations() != null ? data.getOrganizations().size() : 0);
                return data.getOrganizations();
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
    public boolean syncOrganizations(List<ExternalOrganizationDto> organizations, String version) {
        return organizationService.syncFromExternal(organizations, version);
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
    private List<ExternalOrganizationDto> createMockData() {
        return Arrays.asList(
            createMockOrg("1001", "总公司", "0", 0, "公司总部"),
            createMockOrg("1002", "技术中心", "1001", 1, "负责技术研发"),
            createMockOrg("1003", "市场部", "1001", 2, "负责市场营销"),
            createMockOrg("1004", "前端团队", "1002", 1, "前端开发团队"),
            createMockOrg("1005", "后端团队", "1002", 2, "后端开发团队"),
            createMockOrg("1006", "运维团队", "1002", 3, "运维保障团队")
        );
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
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<ExternalOrganizationDto> getOrganizations() { return organizations; }
        public void setOrganizations(List<ExternalOrganizationDto> organizations) { this.organizations = organizations; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}