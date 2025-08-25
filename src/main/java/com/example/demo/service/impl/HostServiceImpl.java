package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.HostDto;
import com.example.demo.dto.HostQueryDto;
import com.example.demo.mapper.HostMapper;
import com.example.demo.model.entity.Host;
import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.HostStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.service.HostService;
import com.example.demo.service.CacheAvailabilityService;
import com.example.demo.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HostServiceImpl implements HostService {

    private final HostMapper hostMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Autowired
    private CacheAvailabilityService cacheAvailabilityService;
    
    @Autowired
    private CacheManager cacheManager;

    public HostServiceImpl(HostMapper hostMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.hostMapper = hostMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    @Transactional
    @CachePut(value = "hosts", key = "#result.id", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Host createOrUpdateHost(HostDto hostDto) {
        boolean cacheAvailable = cacheAvailabilityService.isCacheAvailable();
        log.info("🏠 开始创建或更新主机，缓存可用性: {} ({})", 
                cacheAvailable, cacheAvailable ? "Redis缓存" : "数据库直连");
        
        if (hostDto == null) {
            throw new IllegalArgumentException("主机信息不能为null");
        }

        // 验证MAC地址唯一性
        Long excludeHostId = null;
        if (StringUtils.hasText(hostDto.getId())) {
            try {
                excludeHostId = Long.parseLong(hostDto.getId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("主机ID格式不正确");
            }
        }

        if (isMacAddressExists(hostDto.getMacAddress(), excludeHostId)) {
            throw new IllegalArgumentException("MAC地址已存在");
        }

        // 验证IP地址在组织内唯一性
        if (isIpAddressExistsInOrganization(hostDto.getIpAddress(), hostDto.getOrganizationId(), excludeHostId)) {
            throw new IllegalArgumentException("IP地址在该组织内已存在");
        }

        Host host = convertDtoToEntity(hostDto);
        LocalDateTime now = LocalDateTime.now();

        boolean isUpdate = host.getId() != null;
        if (isUpdate) {
            // 更新操作
            host.setUpdatedAt(now);
            hostMapper.updateById(host);
        } else {
            // 创建操作
            host.setId(snowflakeIdGenerator.nextId());
            host.setHostStatus(HostStatus.ACTIVE);
            host.setOnlineStatus(OnlineStatus.OFFLINE);
            host.setAuthStatus(AuthStatus.UNAUTHORIZED);
            host.setCreatedAt(now);
            host.setUpdatedAt(now);
            hostMapper.insert(host);
        }

        Host result = host;
        boolean finalCacheAvailable = cacheAvailabilityService.isCacheAvailable();
        log.info("✅ 主机创建或更新完成，ID: {}, 缓存操作: {} ({})", 
                result.getId(), 
                finalCacheAvailable ? "已缓存" : "未缓存",
                finalCacheAvailable ? "Redis" : "直连数据库");
        return result;
    }

    @Override
    @Cacheable(value = "hosts", key = "#hostId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Host getHostById(Long hostId) {
        if (hostId == null) {
            return null;
        }
        return hostMapper.selectById(hostId);
    }

    @Override
    @Cacheable(value = "hosts", key = "'mac:' + #macAddress", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Host getHostByMacAddress(String macAddress) {
        if (!StringUtils.hasText(macAddress)) {
            return null;
        }
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        return hostMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = "hosts", key = "'ip_org:' + #ipAddress + ':' + #organizationId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Host getHostByIpAndOrganization(String ipAddress, String organizationId) {
        if (!StringUtils.hasText(ipAddress) || !StringUtils.hasText(organizationId)) {
            return null;
        }
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ip_address", ipAddress).eq("organization_id", organizationId);
        return hostMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<Host> getHostsByPage(HostQueryDto queryDto) {
        if (queryDto == null) {
            queryDto = new HostQueryDto();
        }

        Page<Host> page = new Page<>(queryDto.getPage(), queryDto.getSize());
        
        // 如果查询条件中包含责任人搜索，使用JOIN查询
        if (StringUtils.hasText(queryDto.getResponsiblePerson())) {
            return hostMapper.selectHostsWithUserPage(page, queryDto);
        }
        
        // 否则使用原有的查询方式
        QueryWrapper<Host> queryWrapper = buildQueryWrapper(queryDto);

        // 排序
        if (StringUtils.hasText(queryDto.getSortBy())) {
            if ("DESC".equalsIgnoreCase(queryDto.getSortDirection())) {
                queryWrapper.orderByDesc(queryDto.getSortBy());
            } else {
                queryWrapper.orderByAsc(queryDto.getSortBy());
            }
        } else {
            queryWrapper.orderByDesc("created_at");
        }

        return hostMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Cacheable(value = "hosts", key = "'org:' + #organizationId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public List<Host> getHostsByOrganization(String organizationId) {
        if (!StringUtils.hasText(organizationId)) {
            return Collections.emptyList();
        }
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organization_id", organizationId);
        return hostMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void updateOnlineStatus(Long hostId, OnlineStatus onlineStatus) {
        if (hostId == null || onlineStatus == null) {
            return;
        }

        // 先获取主机信息，用于检查状态是否变化
        Host existingHost = hostMapper.selectById(hostId);
        if (existingHost == null) {
            return;
        }
        
        // 性能优化：只在状态真正发生变化时才执行更新
        if (existingHost.getOnlineStatus() == onlineStatus) {
            // 状态未变化，但如果是设置为在线，需要更新最后在线时间
            if (onlineStatus == OnlineStatus.ONLINE) {
                // 只更新时间字段，不触发缓存清除
                Host timeUpdateHost = new Host();
                timeUpdateHost.setId(hostId);
                timeUpdateHost.setLastOnlineTime(LocalDateTime.now());
                timeUpdateHost.setUpdatedAt(LocalDateTime.now());
                hostMapper.updateById(timeUpdateHost);
                log.debug("🔄 更新主机 {} 最后在线时间", hostId);
            }
            return; // 状态未变化，早期返回
        }

        // 状态发生变化，执行完整的更新流程
        Host host = new Host();
        host.setId(hostId);
        host.setOnlineStatus(onlineStatus);
        host.setUpdatedAt(LocalDateTime.now());

        if (onlineStatus == OnlineStatus.ONLINE) {
            host.setLastOnlineTime(LocalDateTime.now());
        }

        hostMapper.updateById(host);
        
        // 清除组织级别的缓存
        evictOrganizationHostsCache(existingHost.getOrganizationId());
        
        log.info("🟢 主机 {} 在线状态已更新: {} -> {}", 
                hostId, existingHost.getOnlineStatus(), onlineStatus);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void updateAuthStatus(Long hostId, AuthStatus authStatus) {
        if (hostId == null || authStatus == null) {
            return;
        }

        // 先获取主机信息，用于清除组织缓存
        Host existingHost = hostMapper.selectById(hostId);
        if (existingHost == null) {
            return;
        }

        Host host = new Host();
        host.setId(hostId);
        host.setAuthStatus(authStatus);
        host.setUpdatedAt(LocalDateTime.now());

        if (authStatus == AuthStatus.AUTHORIZED) {
            host.setAuthTime(LocalDateTime.now());
        }

        hostMapper.updateById(host);
        
        // 清除组织级别的缓存
        evictOrganizationHostsCache(existingHost.getOrganizationId());
    }

    @Override
    @Transactional
    public void batchUpdateAuthStatus(List<Long> hostIds, AuthStatus authStatus) {
        if (hostIds == null || hostIds.isEmpty() || authStatus == null) {
            return;
        }

        // 获取受影响的组织ID集合
        Set<String> affectedOrganizations = new HashSet<>();
        if (cacheAvailabilityService.isCacheAvailable()) {
            // 批量获取主机信息，用于后续清除缓存
            List<Host> existingHosts = hostMapper.selectBatchIds(hostIds);
            affectedOrganizations = existingHosts.stream()
                    .map(Host::getOrganizationId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long hostId : hostIds) {
            Host host = new Host();
            host.setId(hostId);
            host.setAuthStatus(authStatus);
            host.setUpdatedAt(now);

            if (authStatus == AuthStatus.AUTHORIZED) {
                host.setAuthTime(now);
            }

            hostMapper.updateById(host);
            
            // 清除单个主机缓存
            if (cacheAvailabilityService.isCacheAvailable() && cacheManager != null) {
                Cache hostsCache = cacheManager.getCache("hosts");
                if (hostsCache != null) {
                    hostsCache.evict(hostId);
                }
            }
        }
        
        // 清除受影响的组织级别缓存
        for (String organizationId : affectedOrganizations) {
            evictOrganizationHostsCache(organizationId);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void deleteHost(Long hostId) {
        if (hostId == null) {
            return;
        }
        hostMapper.deleteById(hostId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", allEntries = true, condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void batchDeleteHosts(List<Long> hostIds) {
        if (hostIds == null || hostIds.isEmpty()) {
            return;
        }
        hostMapper.deleteBatchIds(hostIds);
    }

    @Override
    public Map<String, Object> getHostStatistics(String organizationId) {
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(organizationId)) {
            queryWrapper.eq("organization_id", organizationId);
        }

        List<Host> hosts = hostMapper.selectList(queryWrapper);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", hosts.size());

        // 按终端类型统计
        Map<String, Long> terminalTypeStats = hosts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        host -> host.getTerminalType().getDescription(),
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("terminalTypeStats", terminalTypeStats);

        // 按主机状态统计
        Map<String, Long> hostStatusStats = hosts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        host -> host.getHostStatus().getDescription(),
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("hostStatusStats", hostStatusStats);

        // 按在线状态统计
        Map<String, Long> onlineStatusStats = hosts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        host -> host.getOnlineStatus().getDescription(),
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("onlineStatusStats", onlineStatusStats);

        // 按授权状态统计
        Map<String, Long> authStatusStats = hosts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        host -> host.getAuthStatus().getDescription(),
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("authStatusStats", authStatusStats);

        return statistics;
    }

    @Override
    public boolean isMacAddressExists(String macAddress, Long excludeHostId) {
        if (!StringUtils.hasText(macAddress)) {
            return false;
        }

        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        if (excludeHostId != null) {
            queryWrapper.ne("id", excludeHostId);
        }

        return hostMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean isIpAddressExistsInOrganization(String ipAddress, String organizationId, Long excludeHostId) {
        if (!StringUtils.hasText(ipAddress) || !StringUtils.hasText(organizationId)) {
            return false;
        }

        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ip_address", ipAddress).eq("organization_id", organizationId);
        if (excludeHostId != null) {
            queryWrapper.ne("id", excludeHostId);
        }

        return hostMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<Host> buildQueryWrapper(HostQueryDto queryDto) {
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(queryDto.getHostName())) {
            queryWrapper.like("host_name", queryDto.getHostName());
        }
        
        // IP或MAC地址组合查询（模糊匹配）
        if (StringUtils.hasText(queryDto.getIpOrMacAddress())) {
            String searchValue = queryDto.getIpOrMacAddress().trim();
            queryWrapper.and(wrapper -> wrapper
                .like("ip_address", searchValue)
                .or()
                .like("mac_address", searchValue)
            );
        }
        
        if (queryDto.getTerminalType() != null) {
            queryWrapper.eq("terminal_type", queryDto.getTerminalType());
        }
        if (queryDto.getHostStatus() != null) {
            queryWrapper.eq("host_status", queryDto.getHostStatus());
        }
        if (queryDto.getOnlineStatus() != null) {
            queryWrapper.eq("online_status", queryDto.getOnlineStatus());
        }
        if (queryDto.getAuthStatus() != null) {
            queryWrapper.eq("auth_status", queryDto.getAuthStatus());
        }
        if (StringUtils.hasText(queryDto.getResponsiblePerson())) {
            queryWrapper.like("responsible_person", queryDto.getResponsiblePerson());
        }
        if (StringUtils.hasText(queryDto.getOrganizationId())) {
            queryWrapper.eq("organization_id", queryDto.getOrganizationId());
        }
        if (queryDto.getCreatedAtStart() != null) {
            queryWrapper.ge("created_at", queryDto.getCreatedAtStart());
        }
        if (queryDto.getCreatedAtEnd() != null) {
            queryWrapper.le("created_at", queryDto.getCreatedAtEnd());
        }
        if (queryDto.getLastOnlineTimeStart() != null) {
            queryWrapper.ge("last_online_time", queryDto.getLastOnlineTimeStart());
        }
        if (queryDto.getLastOnlineTimeEnd() != null) {
            queryWrapper.le("last_online_time", queryDto.getLastOnlineTimeEnd());
        }

        return queryWrapper;
    }

    /**
     * DTO转实体
     */
    private Host convertDtoToEntity(HostDto dto) {
        if (dto == null) {
            return null;
        }

        Host entity = new Host();
        if (StringUtils.hasText(dto.getId())) {
            try {
                entity.setId(Long.parseLong(dto.getId()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("主机ID格式不正确");
            }
        }

        entity.setHostName(dto.getHostName());
        entity.setIpAddress(dto.getIpAddress());
        entity.setMacAddress(dto.getMacAddress());
        entity.setTerminalType(dto.getTerminalType());
        entity.setHostStatus(dto.getHostStatus());
        entity.setOnlineStatus(dto.getOnlineStatus());
        entity.setAuthStatus(dto.getAuthStatus());
        entity.setResponsiblePerson(dto.getResponsiblePerson());
        entity.setUserId(dto.getUserId());
        entity.setVersion(dto.getVersion());
        entity.setOperatingSystem(dto.getOperatingSystem());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setLastOnlineTime(dto.getLastOnlineTime());
        entity.setAuthTime(dto.getAuthTime());
        entity.setRemarks(dto.getRemarks());

        return entity;
    }
    
    /**
     * 清除组织级别的主机缓存
     * @param organizationId 组织ID
     */
    private void evictOrganizationHostsCache(String organizationId) {
        if (!StringUtils.hasText(organizationId)) {
            return;
        }
        
        try {
            if (cacheAvailabilityService.isCacheAvailable() && cacheManager != null) {
                Cache hostsCache = cacheManager.getCache("hosts");
                if (hostsCache != null) {
                    // 清除组织级别的缓存
                    String orgCacheKey = "org:" + organizationId;
                    hostsCache.evict(orgCacheKey);
                    log.debug("🗑️ 已清除组织级别缓存: {}", orgCacheKey);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 清除组织级别缓存失败: organizationId={}, error={}", organizationId, e.getMessage());
        }
    }
    
    /**
     * 调试方法：查看缓存中的数据
     */
    public void debugCacheContent() {
        if (cacheManager != null) {
            Cache hostsCache = cacheManager.getCache("hosts");
            if (hostsCache != null) {
                log.info("🔍 缓存类型: {}", hostsCache.getClass().getSimpleName());
                log.info("🔍 缓存名称: {}", hostsCache.getName());
                
                // 尝试查看缓存统计信息
                if (hostsCache.getNativeCache() != null) {
                    log.info("🔍 Native Cache 类型: {}", hostsCache.getNativeCache().getClass().getSimpleName());
                }
            } else {
                log.warn("⚠️ hosts 缓存不存在");
            }
        }
    }
    
    /**
     * 调试方法：查看指定主机ID的缓存数据
     */
    public Host debugGetFromCache(Long hostId) {
        if (cacheManager != null && hostId != null) {
            Cache hostsCache = cacheManager.getCache("hosts");
            if (hostsCache != null) {
                Cache.ValueWrapper wrapper = hostsCache.get(hostId);
                if (wrapper != null) {
                    Host cachedHost = (Host) wrapper.get();
                    log.info("✅ 从缓存中获取到主机ID: {}, 名称: {}", hostId, cachedHost != null ? cachedHost.getHostName() : "null");
                    return cachedHost;
                } else {
                    log.warn("⚠️ 缓存中找不到主机ID: {}", hostId);
                }
            }
        }
        return null;
    }
}