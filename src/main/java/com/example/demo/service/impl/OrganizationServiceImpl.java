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
        queryWrapper.orderByAsc("name");
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
            List<Organization> organizations = convertExternalToInternal(externalOrganizations);
            
            // 3. 计算leaf字段
            calculateLeafField(organizations);
            
            // 4. 批量插入新数据
            for (Organization org : organizations) {
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
        // 简化同步策略：如果数据库为空，需要同步
        Long count = organizationMapper.countAll();
        if (count == 0) {
            log.info("📋 组织架构数据为空，需要同步");
            return true;
        }
        
        log.debug("✨ 组织架构数据存在，暂不需要同步");
        return false;
    }
    
    @Override
    public String getCurrentVersion() {
        // 简化版本不再跟踪外部版本号
        return "simplified-v1.0.0";
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
        
        // 叶子节点和非叶子节点统计
        List<Organization> allOrgs = getAllOrganizations();
        Map<Integer, Long> leafStats = allOrgs.stream()
                .collect(Collectors.groupingBy(
                        Organization::getLeaf,
                        Collectors.counting()
                ));
        stats.put("leafStats", leafStats);
        
        // 当前版本
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
    private List<Organization> convertExternalToInternal(List<ExternalOrganizationDto> externalOrgs) {
        return externalOrgs.stream()
                .map(external -> {
                    Organization org = new Organization();
                    org.setId(external.getId());
                    org.setName(external.getName());
                    org.setParentId(external.getParentId() != null ? external.getParentId() : "0");
                    org.setLeaf(0); // 默认为非叶子节点，后面会重新计算
                    return org;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算leaf字段（是否有子部门）
     */
    private void calculateLeafField(List<Organization> organizations) {
        // 创建 parentId -> children 的映射
        Map<String, List<Organization>> parentChildrenMap = organizations.stream()
                .collect(Collectors.groupingBy(Organization::getParentId));
        
        for (Organization org : organizations) {
            List<Organization> children = parentChildrenMap.get(org.getId());
            if (children != null && !children.isEmpty()) {
                org.setLeaf(1); // 有子部门，非叶子节点
            } else {
                org.setLeaf(0); // 无子部门，叶子节点
            }
        }
    }
}