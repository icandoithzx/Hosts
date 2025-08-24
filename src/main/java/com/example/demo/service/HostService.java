package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.HostDto;
import com.example.demo.dto.HostQueryDto;
import com.example.demo.model.entity.Host;
import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.OnlineStatus;

import java.util.List;

/**
 * 主机注册管理服务接口
 */
public interface HostService {

    /**
     * 创建或更新主机信息
     *
     * @param hostDto 主机信息
     * @return 主机实体
     */
    Host createOrUpdateHost(HostDto hostDto);

    /**
     * 根据ID获取主机信息
     *
     * @param hostId 主机ID
     * @return 主机实体
     */
    Host getHostById(Long hostId);

    /**
     * 根据MAC地址获取主机信息
     *
     * @param macAddress MAC地址
     * @return 主机实体
     */
    Host getHostByMacAddress(String macAddress);

    /**
     * 根据IP地址和组织ID获取主机信息
     *
     * @param ipAddress IP地址
     * @param organizationId 组织ID
     * @return 主机实体
     */
    Host getHostByIpAndOrganization(String ipAddress, String organizationId);

    /**
     * 分页查询主机信息
     *
     * @param queryDto 查询条件
     * @return 分页结果
     */
    IPage<Host> getHostsByPage(HostQueryDto queryDto);

    /**
     * 根据组织ID获取主机列表
     *
     * @param organizationId 组织ID
     * @return 主机列表
     */
    List<Host> getHostsByOrganization(String organizationId);

    /**
     * 更新主机在线状态
     *
     * @param hostId 主机ID
     * @param onlineStatus 在线状态
     */
    void updateOnlineStatus(Long hostId, OnlineStatus onlineStatus);

    /**
     * 更新主机授权状态
     *
     * @param hostId 主机ID
     * @param authStatus 授权状态
     */
    void updateAuthStatus(Long hostId, AuthStatus authStatus);

    /**
     * 批量更新主机授权状态
     *
     * @param hostIds 主机ID列表
     * @param authStatus 授权状态
     */
    void batchUpdateAuthStatus(List<Long> hostIds, AuthStatus authStatus);

    /**
     * 删除主机
     *
     * @param hostId 主机ID
     */
    void deleteHost(Long hostId);

    /**
     * 批量删除主机
     *
     * @param hostIds 主机ID列表
     */
    void batchDeleteHosts(List<Long> hostIds);

    /**
     * 获取主机统计信息
     *
     * @param organizationId 组织ID（可选）
     * @return 统计信息
     */
    java.util.Map<String, Object> getHostStatistics(String organizationId);

    /**
     * 检查MAC地址是否已存在
     *
     * @param macAddress MAC地址
     * @param excludeHostId 排除的主机ID（更新时使用）
     * @return 是否存在
     */
    boolean isMacAddressExists(String macAddress, Long excludeHostId);

    /**
     * 检查IP地址在组织内是否已存在
     *
     * @param ipAddress IP地址
     * @param organizationId 组织ID
     * @param excludeHostId 排除的主机ID（更新时使用）
     * @return 是否存在
     */
    boolean isIpAddressExistsInOrganization(String ipAddress, String organizationId, Long excludeHostId);
}