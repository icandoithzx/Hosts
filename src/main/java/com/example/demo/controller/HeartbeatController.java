package com.example.demo.controller;

import com.example.demo.dto.HeartbeatRequest;
import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.HeartbeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    public HeartbeatController(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @PostMapping("/heartbeat")
    public HeartbeatResponse handleHeartbeat(@RequestBody HeartbeatRequest request) {
        // 控制器层非常简洁，直接调用服务层的方法并返回其结果。
        // 所有的核心逻辑都封装在服务层中。
        return heartbeatService.checkPolicies(
            request.getClientId(),
            request.getCurrentPoliciesHash()
        );
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
}
