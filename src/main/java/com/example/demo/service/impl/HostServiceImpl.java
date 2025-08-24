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
import com.example.demo.util.SnowflakeIdGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HostServiceImpl implements HostService {

    private final HostMapper hostMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public HostServiceImpl(HostMapper hostMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.hostMapper = hostMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    @Transactional
    @CachePut(value = "hosts", key = "#result.id")
    public Host createOrUpdateHost(HostDto hostDto) {
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

        return host;
    }

    @Override
    @Cacheable(value = "hosts", key = "#hostId")
    public Host getHostById(Long hostId) {
        if (hostId == null) {
            return null;
        }
        return hostMapper.selectById(hostId);
    }

    @Override
    @Cacheable(value = "hosts", key = "'mac:' + #macAddress")
    public Host getHostByMacAddress(String macAddress) {
        if (!StringUtils.hasText(macAddress)) {
            return null;
        }
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        return hostMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = "hosts", key = "'ip_org:' + #ipAddress + ':' + #organizationId")
    public Host getHostByIpAndOrganization(String ipAddress, Long organizationId) {
        if (!StringUtils.hasText(ipAddress) || organizationId == null) {
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
    @Cacheable(value = "hosts", key = "'org:' + #organizationId")
    public List<Host> getHostsByOrganization(Long organizationId) {
        if (organizationId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organization_id", organizationId);
        return hostMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId")
    public void updateOnlineStatus(Long hostId, OnlineStatus onlineStatus) {
        if (hostId == null || onlineStatus == null) {
            return;
        }

        Host host = new Host();
        host.setId(hostId);
        host.setOnlineStatus(onlineStatus);
        host.setUpdatedAt(LocalDateTime.now());

        if (onlineStatus == OnlineStatus.ONLINE) {
            host.setLastOnlineTime(LocalDateTime.now());
        }

        hostMapper.updateById(host);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId")
    public void updateAuthStatus(Long hostId, AuthStatus authStatus) {
        if (hostId == null || authStatus == null) {
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
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", allEntries = true)
    public void batchUpdateAuthStatus(List<Long> hostIds, AuthStatus authStatus) {
        if (hostIds == null || hostIds.isEmpty() || authStatus == null) {
            return;
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
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", key = "#hostId")
    public void deleteHost(Long hostId) {
        if (hostId == null) {
            return;
        }
        hostMapper.deleteById(hostId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "hosts", allEntries = true)
    public void batchDeleteHosts(List<Long> hostIds) {
        if (hostIds == null || hostIds.isEmpty()) {
            return;
        }
        hostMapper.deleteBatchIds(hostIds);
    }

    @Override
    public Map<String, Object> getHostStatistics(Long organizationId) {
        QueryWrapper<Host> queryWrapper = new QueryWrapper<>();
        if (organizationId != null) {
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
    public boolean isIpAddressExistsInOrganization(String ipAddress, Long organizationId, Long excludeHostId) {
        if (!StringUtils.hasText(ipAddress) || organizationId == null) {
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
        if (queryDto.getOrganizationId() != null) {
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
        entity.setVersion(dto.getVersion());
        entity.setOperatingSystem(dto.getOperatingSystem());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setLastOnlineTime(dto.getLastOnlineTime());
        entity.setAuthTime(dto.getAuthTime());
        entity.setRemarks(dto.getRemarks());

        return entity;
    }
}