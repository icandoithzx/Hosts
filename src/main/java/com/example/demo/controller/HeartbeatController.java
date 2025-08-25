package com.example.demo.controller;

import com.example.demo.dto.HeartbeatRequest;
import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.HeartbeatService;
import com.example.demo.service.impl.HostOnlineStatusMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HeartbeatController {

    private final HeartbeatService heartbeatService;
    private final HostOnlineStatusMonitorService monitorService;

    public HeartbeatController(HeartbeatService heartbeatService, HostOnlineStatusMonitorService monitorService) {
        this.heartbeatService = heartbeatService;
        this.monitorService = monitorService;
    }

    @PostMapping("/heartbeat")
    public HeartbeatResponse handleHeartbeat(@RequestBody HeartbeatRequest request) {
        // 使用新的handleHeartbeat方法，支持更详细的心跳信息处理
        return heartbeatService.handleHeartbeat(request);
    }
    
    /**
     * 兼容性接口：简单的策略检查（旧版本兼容）
     */
    @PostMapping("/check-policies")
    public HeartbeatResponse checkPolicies(@RequestBody Map<String, String> request) {
        String clientId = request.get("clientId");
        String currentPoliciesHash = request.get("currentPoliciesHash");
        
        return heartbeatService.checkPolicies(clientId, currentPoliciesHash);
    }

    /**
     * 获取客户端当前生效的策略（新增）。
     * 该接口为客户端提供了查询最新策略的功能。
     * 
     * @param clientId 客户端ID
     * @return 客户端当前生效的策略
     */
    @GetMapping("/client/{clientId}/policy")
    public ResponseEntity<Policy> getClientPolicy(@PathVariable String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Policy policy = heartbeatService.getClientEffectivePolicy(clientId);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(policy);
    }

    /**
     * 获取客户端策略哈希值（新增）。
     * 用于客户端快速检查策略是否有变更。
     * 
     * @param clientId 客户端ID
     * @return 策略哈希值
     */
    @GetMapping("/client/{clientId}/policy-hash")
    public ResponseEntity<String> getClientPolicyHash(@PathVariable String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        String hash = heartbeatService.getClientPolicyHash(clientId);
        if (hash == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(hash);
    }
    
    /**
     * 获取主机在线统计信息（新增）
     */
    @GetMapping("/online-statistics")
    public ResponseEntity<HostOnlineStatusMonitorService.HostOnlineStatistics> getOnlineStatistics() {
        HostOnlineStatusMonitorService.HostOnlineStatistics statistics = monitorService.getOnlineStatistics();
        return ResponseEntity.ok(statistics);
    }
}
