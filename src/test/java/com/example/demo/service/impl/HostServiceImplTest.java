package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.HostQueryDto;
import com.example.demo.mapper.HostMapper;
import com.example.demo.model.entity.Host;
import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.HostStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.model.enums.TerminalType;
import com.example.demo.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HostServiceImplTest {

    @Mock
    private HostMapper hostMapper;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private HostServiceImpl hostService;

    private Host sampleHost1;
    private Host sampleHost2;

    @BeforeEach
    void setUp() {
        sampleHost1 = new Host();
        sampleHost1.setId(1L);
        sampleHost1.setHostName("Test-PC-001");
        sampleHost1.setIpAddress("192.168.1.100");
        sampleHost1.setMacAddress("00:1B:44:11:3A:B7");
        sampleHost1.setTerminalType(TerminalType.PC);
        sampleHost1.setHostStatus(HostStatus.ACTIVE);
        sampleHost1.setOnlineStatus(OnlineStatus.ONLINE);
        sampleHost1.setAuthStatus(AuthStatus.AUTHORIZED);
        sampleHost1.setResponsiblePerson("张三");
        sampleHost1.setVersion("1.0.0");
        sampleHost1.setOperatingSystem("Windows 10");
        sampleHost1.setOrganizationId(1001L);
        sampleHost1.setCreatedAt(LocalDateTime.now());

        sampleHost2 = new Host();
        sampleHost2.setId(2L);
        sampleHost2.setHostName("Test-Server-001");
        sampleHost2.setIpAddress("192.168.1.10");
        sampleHost2.setMacAddress("00:1B:44:11:3A:B8");
        sampleHost2.setTerminalType(TerminalType.SERVER);
        sampleHost2.setHostStatus(HostStatus.ACTIVE);
        sampleHost2.setOnlineStatus(OnlineStatus.ONLINE);
        sampleHost2.setAuthStatus(AuthStatus.AUTHORIZED);
        sampleHost2.setResponsiblePerson("李四");
        sampleHost2.setVersion("2.0.0");
        sampleHost2.setOperatingSystem("Ubuntu 20.04");
        sampleHost2.setOrganizationId(1002L);
        sampleHost2.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetHostsByPage_WithIpOrMacAddress_SearchByIp() {
        // Given
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setIpOrMacAddress("192.168.1"); // 模糊查询IP段
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1));
        page.setTotal(1);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("192.168.1.100", result.getRecords().get(0).getIpAddress());

        // 验证QueryWrapper的构建（使用LIKE查询）
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            // 验证SQL片段包含LIKE查询条件
            return sqlSegment.contains("ip_address") && sqlSegment.contains("mac_address") && 
                   sqlSegment.contains("LIKE") && sqlSegment.contains("OR");
        }));
    }

    @Test
    void testGetHostsByPage_WithIpOrMacAddress_SearchByMac() {
        // Given
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setIpOrMacAddress("1B:44"); // 模糊查询MAC段
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1));
        page.setTotal(1);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("00:1B:44:11:3A:B7", result.getRecords().get(0).getMacAddress());

        // 验证QueryWrapper的构建（使用LIKE查询）
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            // 验证SQL片段包含LIKE查询条件
            return sqlSegment.contains("ip_address") && sqlSegment.contains("mac_address") && 
                   sqlSegment.contains("LIKE") && sqlSegment.contains("OR");
        }));
    }

    @Test
    void testGetHostsByPage_WithoutIpOrMacAddress() {
        // Given
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setHostName("Test");
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1, sampleHost2));
        page.setTotal(2);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证QueryWrapper的构建（不应包含IP/MAC查询条件）
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            return sqlSegment.contains("host_name") && !sqlSegment.contains("ip_address") && !sqlSegment.contains("mac_address");
        }));
    }

    @Test
    void testGetHostsByPage_WithEmptyIpOrMacAddress() {
        // Given
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setIpOrMacAddress("   "); // 空白字符串
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1, sampleHost2));
        page.setTotal(2);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());

        // 验证QueryWrapper的构建（不应包含IP/MAC查询条件）
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            return !sqlSegment.contains("ip_address") && !sqlSegment.contains("mac_address");
        }));
    }

    @Test
    void testGetHostsByPage_WithCombinedConditions() {
        // Given
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setHostName("Test");
        queryDto.setIpOrMacAddress("192.168.1.100");
        queryDto.setTerminalType(TerminalType.PC);
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1));
        page.setTotal(1);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        // 验证QueryWrapper包含所有查询条件（包含LIKE查询）
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            return sqlSegment.contains("host_name") && 
                   sqlSegment.contains("ip_address") && 
                   sqlSegment.contains("mac_address") && 
                   sqlSegment.contains("terminal_type") &&
                   sqlSegment.contains("LIKE") &&
                   sqlSegment.contains("OR");
        }));
    }

    @Test
    void testGetHostsByPage_WithNullQueryDto() {
        // Given
        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1, sampleHost2));
        page.setTotal(2);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());

        // 验证使用了默认的分页参数
        verify(hostMapper).selectPage(argThat(pageParam -> 
            pageParam.getCurrent() == 1 && pageParam.getSize() == 10), any(QueryWrapper.class));
    }

    @Test
    void testGetHostsByPage_LikeFuzzySearch() {
        // Given - 测试模糊查询能匹配部分IP或MAC地址
        HostQueryDto queryDto = new HostQueryDto();
        queryDto.setIpOrMacAddress("168"); // 应该能匹配 192.168.1.100
        queryDto.setPage(1);
        queryDto.setSize(10);

        Page<Host> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleHost1, sampleHost2)); // 两个主机的IP都包含"168"
        page.setTotal(2);

        when(hostMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);

        // When
        IPage<Host> result = hostService.getHostsByPage(queryDto);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());

        // 验证使用了LIKE模糊查询
        verify(hostMapper).selectPage(any(Page.class), argThat(queryWrapper -> {
            String sqlSegment = queryWrapper.getSqlSegment();
            return sqlSegment.contains("LIKE") && 
                   sqlSegment.contains("ip_address") && 
                   sqlSegment.contains("mac_address") &&
                   sqlSegment.contains("OR");
        }));
    }
}