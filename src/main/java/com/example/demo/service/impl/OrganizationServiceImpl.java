package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.dto.ExternalOrganizationDto;
import com.example.demo.dto.OrganizationDto;
import com.example.demo.mapper.OrganizationMapper;
import com.example.demo.model.entity.Organization;
import com.example.demo.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织架构服务实现类
 */
@Slf4j
@Service
public class OrganizationServiceImpl implements OrganizationService {
    
    private final OrganizationMapper organizationMapper;
    
    public OrganizationServiceImpl(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }
    
    @Override
    public List<OrganizationDto> getOrganizationTree() {
        List<Organization> allOrgs = getAllOrganizations();
        return buildTree(allOrgs, "0");
    }
    
    @Override
    public Organization getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return organizationMapper.selectById(id);
    }
    
    @Override
    public List<Organization> getByParentId(String parentId) {
        return organizationMapper.selectByParentId(parentId != null ? parentId : "0");
    }
    
    @Override
    public List<Organization> getAllOrganizations() {
        QueryWrapper<Organization> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                   .orderByAsc("level", "sort_order", "name");
        return organizationMapper.selectList(queryWrapper);
    }
    
    @Override
    @Transactional
    public boolean syncFromExternal(List<ExternalOrganizationDto> externalOrganizations, String version) {
        try {
            log.info("🔄 开始同步组织架构，版本: {}, 组织数量: {}", version, 
                    externalOrganizations != null ? externalOrganizations.size() : 0);
            
            if (externalOrganizations == null || externalOrganizations.isEmpty()) {
                log.warn("⚠️ 外部组织数据为空，跳过同步");
                return false;
            }
            
            // 1. 清空现有数据
            organizationMapper.deleteAll();
            log.info("🗑️ 已清空现有组织架构数据");
            
            // 2. 转换外部数据为内部实体
            List<Organization> organizations = convertExternalToInternal(externalOrganizations, version);
            
            // 3. 计算层级和路径
            calculateLevelAndPath(organizations);
            
            // 4. 批量插入新数据
            LocalDateTime now = LocalDateTime.now();
            for (Organization org : organizations) {
                org.setCreatedAt(now);
                org.setUpdatedAt(now);
                org.setLastSyncTime(now);
                organizationMapper.insert(org);
            }
            
            log.info("✅ 组织架构同步完成，成功插入 {} 条记录", organizations.size());
            return true;
            
        } catch (Exception e) {
            log.error("❌ 组织架构同步失败: {}", e.getMessage(), e);
            throw new RuntimeException("组织架构同步失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean needSync() {
        // 同步策略：
        // 1. 如果数据库为空，需要同步
        // 2. 如果超过1天没有同步，需要同步
        // 3. 如果外部系统版本比本地版本新，需要同步
        
        Long count = organizationMapper.countAll();
        if (count == 0) {
            log.info("📋 组织架构数据为空，需要同步");
            return true;
        }
        
        // 检查最后同步时间
        QueryWrapper<Organization> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("MAX(last_sync_time) as last_sync_time");
        Organization latestSync = organizationMapper.selectOne(queryWrapper);
        
        if (latestSync == null || latestSync.getLastSyncTime() == null) {
            log.info("📅 未找到最后同步时间，需要同步");
            return true;
        }
        
        LocalDateTime lastSyncTime = latestSync.getLastSyncTime();
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        
        if (lastSyncTime.isBefore(oneDayAgo)) {
            log.info("⏰ 距离上次同步超过1天，需要同步。上次同步时间: {}", lastSyncTime);
            return true;
        }
        
        log.debug("✨ 组织架构数据较新，暂不需要同步。上次同步时间: {}", lastSyncTime);
        return false;
    }
    
    @Override
    public String getCurrentVersion() {
        QueryWrapper<Organization> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("external_version")
                   .orderByDesc("last_sync_time")
                   .last("LIMIT 1");
        
        Organization latest = organizationMapper.selectOne(queryWrapper);
        return latest != null ? latest.getExternalVersion() : null;
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总组织数
        Long totalCount = organizationMapper.countAll();
        stats.put("totalCount", totalCount);
        
        // 根级别组织数
        List<Organization> rootOrgs = organizationMapper.selectRootOrganizations();
        stats.put("rootCount", rootOrgs.size());
        
        // 按级别统计
        List<Organization> allOrgs = getAllOrganizations();
        Map<Integer, Long> levelStats = allOrgs.stream()
                .collect(Collectors.groupingBy(
                        Organization::getLevel,
                        Collectors.counting()
                ));
        stats.put("levelStats", levelStats);
        
        // 最大层级
        int maxLevel = allOrgs.stream()
                .mapToInt(Organization::getLevel)
                .max()
                .orElse(0);
        stats.put("maxLevel", maxLevel);
        
        // 最后同步时间
        String currentVersion = getCurrentVersion();
        stats.put("currentVersion", currentVersion);
        
        return stats;
    }
    
    /**
     * 构建组织树
     */
    private List<OrganizationDto> buildTree(List<Organization> organizations, String parentId) {
        return organizations.stream()
                .filter(org -> Objects.equals(org.getParentId(), parentId))
                .map(org -> {
                    OrganizationDto dto = new OrganizationDto();
                    BeanUtils.copyProperties(org, dto);
                    dto.setChildren(buildTree(organizations, org.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 转换外部数据为内部实体
     */
    private List<Organization> convertExternalToInternal(List<ExternalOrganizationDto> externalOrgs, String version) {
        return externalOrgs.stream()
                .map(external -> {
                    Organization org = new Organization();
                    org.setId(external.getId());
                    org.setName(external.getName());
                    org.setParentId(external.getParentId() != null ? external.getParentId() : "0");
                    org.setStatus(external.getStatus() != null ? external.getStatus() : 1);
                    org.setSortOrder(external.getSortOrder() != null ? external.getSortOrder() : 0);
                    org.setDescription(external.getDescription());
                    org.setSourceSystem("external");
                    org.setExternalVersion(version);
                    return org;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算组织层级和路径
     */
    private void calculateLevelAndPath(List<Organization> organizations) {
        Map<String, Organization> orgMap = organizations.stream()
                .collect(Collectors.toMap(Organization::getId, org -> org));
        
        for (Organization org : organizations) {
            calculateSingleOrgLevelAndPath(org, orgMap);
        }
    }
    
    /**
     * 计算单个组织的层级和路径
     */
    private void calculateSingleOrgLevelAndPath(Organization org, Map<String, Organization> orgMap) {
        if (org.getLevel() != null && org.getPath() != null) {
            return; // 已经计算过
        }
        
        if ("0".equals(org.getParentId())) {
            // 根级别组织
            org.setLevel(1);
            org.setPath(org.getId());
        } else {
            // 子组织，需要先计算父组织
            Organization parent = orgMap.get(org.getParentId());
            if (parent != null) {
                calculateSingleOrgLevelAndPath(parent, orgMap);
                org.setLevel(parent.getLevel() + 1);
                org.setPath(parent.getPath() + "/" + org.getId());
            } else {
                // 找不到父组织，当作根级别处理
                log.warn("⚠️ 组织 {} 的父组织 {} 不存在，当作根级别处理", org.getId(), org.getParentId());
                org.setLevel(1);
                org.setPath(org.getId());
                org.setParentId("0");
            }
        }
    }
}