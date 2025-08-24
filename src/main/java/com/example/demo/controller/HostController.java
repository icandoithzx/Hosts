package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.HostDto;
import com.example.demo.dto.HostQueryDto;
import com.example.demo.model.entity.Host;
import com.example.demo.model.enums.AuthStatus;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.service.HostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 主机注册管理控制器
 */
@RestController
@RequestMapping("/api/v1/hosts")
public class HostController {

    private final HostService hostService;

    public HostController(HostService hostService) {
        this.hostService = hostService;
    }

    /**
     * 创建或更新主机
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Host>> createOrUpdateHost(@RequestBody HostDto hostDto) {
        try {
            Host host = hostService.createOrUpdateHost(hostDto);
            return ResponseEntity.ok(ApiResponse.success(host));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Host>errorWithType(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Host>errorWithType(500, "创建或更新主机失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取主机信息
     */
    @GetMapping("/{hostId}")
    public ResponseEntity<ApiResponse<Host>> getHostById(@PathVariable Long hostId) {
        try {
            Host host = hostService.getHostById(hostId);
            if (host == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(host));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Host>errorWithType(500, "获取主机信息失败: " + e.getMessage()));
        }
    }

    /**
     * 根据MAC地址获取主机信息
     */
    @GetMapping("/mac/{macAddress}")
    public ResponseEntity<ApiResponse<Host>> getHostByMacAddress(@PathVariable String macAddress) {
        try {
            Host host = hostService.getHostByMacAddress(macAddress);
            if (host == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(host));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Host>errorWithType(500, "获取主机信息失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询主机列表
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<IPage<Host>>> searchHosts(@RequestBody HostQueryDto queryDto) {
        try {
            IPage<Host> hosts = hostService.getHostsByPage(queryDto);
            return ResponseEntity.ok(ApiResponse.success(hosts));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<IPage<Host>>errorWithType(500, "查询主机列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据组织ID获取主机列表
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<Host>>> getHostsByOrganization(@PathVariable Long organizationId) {
        try {
            List<Host> hosts = hostService.getHostsByOrganization(organizationId);
            return ResponseEntity.ok(ApiResponse.success(hosts));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<List<Host>>errorWithType(500, "获取组织主机列表失败: " + e.getMessage()));
        }
    }

    /**
     * 更新主机在线状态
     */
    @PutMapping("/{hostId}/online-status")
    public ResponseEntity<ApiResponse<Void>> updateOnlineStatus(
            @PathVariable Long hostId,
            @RequestParam OnlineStatus onlineStatus) {
        try {
            hostService.updateOnlineStatus(hostId, onlineStatus);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>errorWithType(500, "更新在线状态失败: " + e.getMessage()));
        }
    }

    /**
     * 更新主机授权状态
     */
    @PutMapping("/{hostId}/auth-status")
    public ResponseEntity<ApiResponse<Void>> updateAuthStatus(
            @PathVariable Long hostId,
            @RequestParam AuthStatus authStatus) {
        try {
            hostService.updateAuthStatus(hostId, authStatus);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>errorWithType(500, "更新授权状态失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新主机授权状态
     */
    @PutMapping("/batch/auth-status")
    public ResponseEntity<ApiResponse<Void>> batchUpdateAuthStatus(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> hostIds = (List<Long>) request.get("hostIds");
            String authStatusStr = (String) request.get("authStatus");
            
            if (hostIds == null || hostIds.isEmpty() || authStatusStr == null) {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>errorWithType(400, "参数不能为空"));
            }

            AuthStatus authStatus = AuthStatus.fromCode(authStatusStr);
            hostService.batchUpdateAuthStatus(hostIds, authStatus);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>errorWithType(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>errorWithType(500, "批量更新授权状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除主机
     */
    @DeleteMapping("/{hostId}")
    public ResponseEntity<ApiResponse<Void>> deleteHost(@PathVariable Long hostId) {
        try {
            hostService.deleteHost(hostId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>errorWithType(500, "删除主机失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除主机
     */
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteHosts(@RequestBody List<Long> hostIds) {
        try {
            if (hostIds == null || hostIds.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>errorWithType(400, "主机ID列表不能为空"));
            }
            hostService.batchDeleteHosts(hostIds);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>errorWithType(500, "批量删除主机失败: " + e.getMessage()));
        }
    }

    /**
     * 获取主机统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHostStatistics(
            @RequestParam(required = false) Long organizationId) {
        try {
            Map<String, Object> statistics = hostService.getHostStatistics(organizationId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>errorWithType(500, "获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 检查MAC地址是否已存在
     */
    @GetMapping("/check/mac-address")
    public ResponseEntity<ApiResponse<Boolean>> checkMacAddressExists(
            @RequestParam String macAddress,
            @RequestParam(required = false) Long excludeHostId) {
        try {
            boolean exists = hostService.isMacAddressExists(macAddress, excludeHostId);
            return ResponseEntity.ok(ApiResponse.success(exists));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Boolean>errorWithType(500, "检查MAC地址失败: " + e.getMessage()));
        }
    }

    /**
     * 检查IP地址在组织内是否已存在
     */
    @GetMapping("/check/ip-address")
    public ResponseEntity<ApiResponse<Boolean>> checkIpAddressExistsInOrganization(
            @RequestParam String ipAddress,
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long excludeHostId) {
        try {
            boolean exists = hostService.isIpAddressExistsInOrganization(ipAddress, organizationId, excludeHostId);
            return ResponseEntity.ok(ApiResponse.success(exists));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Boolean>errorWithType(500, "检查IP地址失败: " + e.getMessage()));
        }
    }
    
    /**
     * 调试接口：查看缓存内容
     */
    @GetMapping("/debug/cache")
    public ResponseEntity<ApiResponse<String>> debugCache() {
        try {
            if (hostService instanceof com.example.demo.service.impl.HostServiceImpl) {
                ((com.example.demo.service.impl.HostServiceImpl) hostService).debugCacheContent();
                return ResponseEntity.ok(ApiResponse.success("缓存调试信息已输出到日志"));
            }
            return ResponseEntity.ok(ApiResponse.success("无法访问调试功能"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<String>errorWithType(500, "调试失败: " + e.getMessage()));
        }
    }
    
    /**
     * 调试接口：从缓存获取主机
     */
    @GetMapping("/debug/cache/{hostId}")
    public ResponseEntity<ApiResponse<Host>> debugGetFromCache(@PathVariable Long hostId) {
        try {
            if (hostService instanceof com.example.demo.service.impl.HostServiceImpl) {
                Host cachedHost = ((com.example.demo.service.impl.HostServiceImpl) hostService).debugGetFromCache(hostId);
                return ResponseEntity.ok(ApiResponse.success(cachedHost));
            }
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<Host>errorWithType(500, "调试失败: " + e.getMessage()));
        }
    }
}