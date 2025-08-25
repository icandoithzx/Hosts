-- 数据库索引优化脚本
-- 针对心跳服务和主机管理系统的性能优化
-- 创建时间: 2024-08-25

-- ==============================================
-- 主机表 (hosts) 索引优化
-- ==============================================

-- 1. MAC地址查询优化（心跳服务高频使用）
CREATE INDEX IF NOT EXISTS idx_hosts_mac_address ON hosts (mac_address);

-- 2. 在线状态查询优化
CREATE INDEX IF NOT EXISTS idx_hosts_online_status ON hosts (online_status);

-- 3. 最后在线时间查询优化（离线检测使用）
CREATE INDEX IF NOT EXISTS idx_hosts_last_online_time ON hosts (last_online_time);

-- 4. 组织ID查询优化
CREATE INDEX IF NOT EXISTS idx_hosts_organization_id ON hosts (organization_id);

-- 5. IP地址查询优化
CREATE INDEX IF NOT EXISTS idx_hosts_ip_address ON hosts (ip_address);

-- 6. 复合索引：MAC地址 + 在线状态（心跳服务核心查询）
CREATE INDEX IF NOT EXISTS idx_hosts_mac_online ON hosts (mac_address, online_status);

-- 7. 复合索引：主机ID + 在线状态 + 最后在线时间（状态监控使用）
CREATE INDEX IF NOT EXISTS idx_hosts_id_online_time ON hosts (id, online_status, last_online_time);

-- 8. 复合索引：组织ID + 在线状态（组织级统计）
CREATE INDEX IF NOT EXISTS idx_hosts_org_online ON hosts (organization_id, online_status);

-- 9. 复合索引：IP + 组织ID（客户端识别）
CREATE INDEX IF NOT EXISTS idx_hosts_ip_org ON hosts (ip_address, organization_id);

-- 10. 离线检测专用索引：在线状态 + 最后在线时间
CREATE INDEX IF NOT EXISTS idx_hosts_online_last_time ON hosts (online_status, last_online_time);

-- ==============================================
-- 策略表 (policies) 索引优化
-- ==============================================

-- 1. 策略状态查询优化
CREATE INDEX IF NOT EXISTS idx_policies_status ON policies (status);

-- 2. 组织ID查询优化
CREATE INDEX IF NOT EXISTS idx_policies_organization_id ON policies (organization_id);

-- 3. 更新时间查询优化（策略同步使用）
CREATE INDEX IF NOT EXISTS idx_policies_updated_at ON policies (updated_at);

-- 4. 优先级查询优化
CREATE INDEX IF NOT EXISTS idx_policies_priority ON policies (priority);

-- 5. 默认策略查询优化
CREATE INDEX IF NOT EXISTS idx_policies_is_default ON policies (is_default);

-- 6. 复合索引：组织ID + 状态 + 优先级（策略匹配核心查询）
CREATE INDEX IF NOT EXISTS idx_policies_org_status_priority ON policies (organization_id, status, priority);

-- 7. 复合索引：状态 + 默认标志（默认策略查询）
CREATE INDEX IF NOT EXISTS idx_policies_status_default ON policies (status, is_default);

-- 8. 复合索引：组织ID + 更新时间（策略同步检查）
CREATE INDEX IF NOT EXISTS idx_policies_org_updated ON policies (organization_id, updated_at);

-- ==============================================
-- 主机策略关联表 (host_policies) 索引优化
-- ==============================================

-- 1. 主机ID查询优化
CREATE INDEX IF NOT EXISTS idx_host_policies_host_id ON host_policies (host_id);

-- 2. 策略ID查询优化
CREATE INDEX IF NOT EXISTS idx_host_policies_policy_id ON host_policies (policy_id);

-- 3. 复合索引：主机ID + 策略ID（关联查询核心）
CREATE INDEX IF NOT EXISTS idx_host_policies_host_policy ON host_policies (host_id, policy_id);

-- 4. 分配时间查询优化
CREATE INDEX IF NOT EXISTS idx_host_policies_assigned_at ON host_policies (assigned_at);

-- ==============================================
-- 组织表 (organizations) 索引优化 - 简化版本
-- ==============================================

-- 1. 组织名称查询优化
CREATE INDEX IF NOT EXISTS idx_organizations_name ON organizations (name);

-- 2. 父组织ID查询优化（层级查询）
CREATE INDEX IF NOT EXISTS idx_organizations_parent_id ON organizations (parent_id);

-- 3. 叶子节点查询优化
CREATE INDEX IF NOT EXISTS idx_organizations_leaf ON organizations (leaf);

-- 4. 复合索引：父组织ID + 名称（子组织查询）
CREATE INDEX IF NOT EXISTS idx_organizations_parent_name ON organizations (parent_id, name);

-- ==============================================
-- 性能分析和统计信息更新
-- ==============================================

-- 更新表统计信息（MySQL/MariaDB）
-- ANALYZE TABLE hosts;
-- ANALYZE TABLE policies;
-- ANALYZE TABLE host_policies;
-- ANALYZE TABLE organizations;

-- ==============================================
-- 索引使用情况监控查询
-- ==============================================

-- 查看索引使用情况的SQL（可用于性能监控）
/*
-- MySQL/MariaDB 索引使用情况
SELECT 
    TABLE_SCHEMA,
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hosts_management'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 慢查询分析
SELECT 
    query_time,
    lock_time,
    rows_sent,
    rows_examined,
    sql_text
FROM mysql.slow_log 
WHERE start_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY query_time DESC
LIMIT 20;
*/

-- ==============================================
-- 索引维护建议
-- ==============================================

/*
性能优化建议：

1. 定期维护
   - 每周执行 ANALYZE TABLE 更新统计信息
   - 每月检查索引碎片情况
   - 定期清理无用索引

2. 监控指标
   - 查询响应时间
   - 索引命中率
   - 慢查询日志
   - 表扫描次数

3. 调优策略
   - 根据实际查询模式调整索引
   - 避免过度索引影响写入性能
   - 考虑分区表策略（大数据量情况）

4. 心跳服务特别优化
   - idx_hosts_mac_online: 核心索引，优先级最高
   - idx_hosts_online_last_time: 离线检测专用
   - idx_policies_org_status_priority: 策略匹配核心

5. 预期性能提升
   - 心跳请求响应时间: 从 50-100ms 降低到 5-15ms
   - 离线检测查询: 从 200-500ms 降低到 20-50ms
   - 策略查询: 从 30-80ms 降低到 5-20ms
   - 整体并发能力提升: 50-80%
*/