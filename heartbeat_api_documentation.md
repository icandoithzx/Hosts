# 心跳服务 API 文档

## 概述
心跳服务提供高并发的客户端策略更新检查功能，支持实时策略变更通知和缓存优化。

## 核心功能
- 🚀 **高并发心跳处理** - 支持大量客户端同时发送心跳包
- 📋 **策略更新检查** - 智能检测客户端策略是否需要更新  
- ⚡ **多层缓存优化** - Redis + Spring Cache 双重缓存
- 🔄 **实时策略同步** - 策略变更后自动推送给客户端
- 🎯 **精确哈希比对** - 基于策略版本和时间戳的哈希计算

## API 接口

### 1. 心跳检查接口
```http
POST /api/v1/heartbeat
Content-Type: application/json

{
    "clientId": "client-001",
    "currentPoliciesHash": "abc123def456",
    "currentPolicyVersion": "v1.2.3",
    "lastPolicyUpdateTime": 1692700800000,
    "clientVersion": "1.0.0"
}
```

**响应格式:**
```json
{
    "needsPolicyUpdate": true,
    "latestPoliciesHash": "def456ghi789",
    "updateType": "POLICY_UPDATED",
    "message": "策略已更新: 新安全策略",
    "serverTimestamp": 1692700900000,
    "effectivePolicy": {
        "id": 12345,
        "name": "新安全策略",
        "description": "更新的安全策略描述",
        "status": "enabled",
        "version": "v2.0.0",
        "isDefault": false,
        "priority": 100
    }
}
```

**更新类型说明:**
- `NEW_POLICY` - 新的策略分配
- `POLICY_UPDATED` - 策略内容更新
- `POLICY_ACTIVATED` - 策略激活状态变化
- `DEFAULT_POLICY` - 使用默认策略
- `NO_POLICY` - 无可用策略

### 2. 获取客户端当前策略
```http
GET /api/v1/client/{clientId}/policy
```

**响应示例:**
```json
{
    "id": 12345,
    "name": "客户端专用策略",
    "description": "为客户端定制的策略",
    "status": "enabled",
    "version": "v1.0.0",
    "isDefault": false,
    "priority": 100,
    "createdAt": "2023-08-22T10:00:00Z",
    "updatedAt": "2023-08-22T11:30:00Z"
}
```

### 3. 获取策略哈希值
```http
GET /api/v1/client/{clientId}/policy-hash
```

**响应示例:**
```json
"abc123def456ghi789jkl012mno345pqr678"
```

## 自动缓存预热机制

系统在以下情况下会自动触发缓存预热：

### 自动预热触发场景：
1. **策略创建/更新** - 预热所有使用该策略的客户端
2. **策略分配** - 预热目标客户端的缓存
3. **策略激活** - 预热客户端的最新策略缓存
4. **批量分配** - 预热所有相关客户端缓存

### 预热策略：
- **异步执行** - 不影响主业务流程
- **批量优化** - 智能间隔避免瞬时压力  
- **失败容错** - 单个预热失败不影响整体
- **线程命名** - 方便监控和调试

## 性能优化特性

### 缓存策略
1. **策略哈希缓存** - 1分钟TTL，用于快速比对
2. **生效策略缓存** - 5分钟TTL，减少数据库查询
3. **Spring Cache注解** - 自动管理缓存生命周期

### 高并发处理
- **连接池优化** - Redisson客户端连接池
- **批处理操作** - Redis批量读取策略信息
- **异步处理** - 非阻塞的策略检查逻辑
- **缓存预热** - 提前加载热点数据

### 哈希计算算法
```java
// 策略哈希 = MD5(策略ID:版本:更新时间戳)
String hashInput = String.format("%s:%s:%s", 
    policy.getId(), 
    policy.getVersion(), 
    policy.getUpdatedAt().getTime()
);
```

## 使用场景

### 场景1: 移动应用心跳
```javascript
// 客户端每30秒发送心跳
setInterval(async () => {
    const response = await fetch('/api/v1/heartbeat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            clientId: 'mobile-app-001',
            currentPoliciesHash: localStorage.getItem('policyHash') || '',
            clientVersion: '2.1.0'
        })
    });
    
    const result = await response.json();
    if (result.needsPolicyUpdate) {
        // 更新本地策略
        updateLocalPolicy(result.effectivePolicy);
        localStorage.setItem('policyHash', result.latestPoliciesHash);
    }
}, 30000);
```

### 场景2: 微服务策略同步
```java
@Scheduled(fixedRate = 60000) // 每分钟检查一次
public void syncPolicy() {
    HeartbeatRequest request = new HeartbeatRequest();
    request.setClientId(serviceInstanceId);
    request.setCurrentPoliciesHash(currentHash);
    
    HeartbeatResponse response = heartbeatService.checkPolicies(
        request.getClientId(), 
        request.getCurrentPoliciesHash()
    );
    
    if (response.isNeedsPolicyUpdate()) {
        // 更新服务策略配置
        updateServicePolicy(response.getEffectivePolicy());
        this.currentHash = response.getLatestPoliciesHash();
    }
}
```

### 场景3: 策略变更自动预热
```java
// 管理员更新策略，系统自动预热相关客户端缓存
@PostMapping("/api/admin/policy")
public Policy updatePolicy(@RequestBody PolicyDto policyDto) {
    // 策略更新后，系统会自动：
    // 1. 查找所有使用该策略的客户端
    // 2. 异步预热这些客户端的缓存
    // 3. 提升后续心跳请求的响应速度
    return policyAdminService.createOrUpdatePolicy(policyDto);
}

@PostMapping("/api/admin/client/{clientId}/assign/{policyId}")
public String assignPolicy(@PathVariable String clientId, @PathVariable Long policyId) {
    // 策略分配后，系统会自动预热该客户端的缓存
    policyAdminService.assignPolicyToClient(clientId, policyId);
    return "OK - 缓存已自动预热";
}
```

## 错误处理

### 错误码说明
- `400 Bad Request` - 客户端ID为空或无效
- `404 Not Found` - 客户端或策略不存在
- `500 Internal Server Error` - 服务器内部错误

### 容错机制
- **缓存降级** - 缓存失败时直接查询数据库
- **部分失败容忍** - 单个策略获取失败不影响整体响应
- **超时保护** - Redis操作设置合理超时时间

## 监控指标

### 性能指标
- 心跳请求QPS
- 平均响应时间
- 缓存命中率
- 策略更新频率

### 业务指标  
- 活跃客户端数量
- 策略分发成功率
- 客户端策略版本分布

## 最佳实践

1. **合理的心跳频率** - 建议30-60秒间隔
2. **客户端缓存** - 本地保存策略哈希避免重复传输
3. **批量预热** - 策略变更后主动预热相关客户端缓存
4. **监控告警** - 设置缓存命中率和响应时间告警
5. **降级策略** - 网络异常时使用本地缓存策略