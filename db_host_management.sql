-- 主机注册管理系统 - 完整初始化脚本
-- 包含主机表结构、索引和默认数据

-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 主机注册表 (hosts)
-- =============================================================================
DROP TABLE IF EXISTS `hosts`;
CREATE TABLE `hosts` (
    `id` BIGINT NOT NULL COMMENT '主机ID，使用雪花算法生成',
    `host_name` VARCHAR(255) NOT NULL COMMENT '主机名称',
    `ip_address` VARCHAR(45) NOT NULL COMMENT 'IP地址（支持IPv4和IPv6）',
    `mac_address` VARCHAR(17) NOT NULL COMMENT 'MAC地址',
    `terminal_type` ENUM('PC', 'SERVER', 'MOBILE', 'TABLET', 'EMBEDDED', 'OTHER') NOT NULL DEFAULT 'PC' COMMENT '终端类型',
    `host_status` ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '主机状态',
    `online_status` ENUM('ONLINE', 'OFFLINE') NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态',
    `auth_status` ENUM('UNAUTHORIZED', 'AUTHORIZED', 'PENDING', 'REJECTED') NOT NULL DEFAULT 'UNAUTHORIZED' COMMENT '授权状态',
    `responsible_person` VARCHAR(255) NOT NULL COMMENT '责任人',
    `version` VARCHAR(50) NOT NULL COMMENT '版本号',
    `operating_system` VARCHAR(255) NOT NULL COMMENT '操作系统',
    `organization_id` BIGINT NOT NULL COMMENT '组织架构ID',
    `last_online_time` DATETIME NULL COMMENT '最后在线时间',
    `auth_time` DATETIME NULL COMMENT '授权时间',
    `remarks` TEXT COMMENT '备注信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_host_mac` (`mac_address`) COMMENT 'MAC地址唯一',
    UNIQUE KEY `uk_host_ip_org` (`ip_address`, `organization_id`) COMMENT '同一组织内IP地址唯一',
    KEY `idx_hosts_host_name` (`host_name`),
    KEY `idx_hosts_ip_address` (`ip_address`),
    KEY `idx_hosts_terminal_type` (`terminal_type`),
    KEY `idx_hosts_host_status` (`host_status`),
    KEY `idx_hosts_online_status` (`online_status`),
    KEY `idx_hosts_auth_status` (`auth_status`),
    KEY `idx_hosts_organization_id` (`organization_id`),
    KEY `idx_hosts_responsible_person` (`responsible_person`),
    KEY `idx_hosts_created_at` (`created_at`),
    KEY `idx_hosts_last_online_time` (`last_online_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主机注册表';

-- =============================================================================
-- 2. 插入示例数据
-- =============================================================================
-- 插入示例主机数据
INSERT INTO `hosts` (
    `id`,
    `host_name`,
    `ip_address`,
    `mac_address`,
    `terminal_type`,
    `host_status`,
    `online_status`,
    `auth_status`,
    `responsible_person`,
    `version`,
    `operating_system`,
    `organization_id`,
    `last_online_time`,
    `auth_time`,
    `remarks`,
    `created_at`,
    `updated_at`
) VALUES 
(
    2000000000000000001,
    'DEV-PC-001',
    '192.168.1.100',
    '00:1B:44:11:3A:B7',
    'PC',
    'ACTIVE',
    'ONLINE',
    'AUTHORIZED',
    '张三',
    '1.0.0',
    'Windows 11 Pro',
    1001,
    NOW(),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    '开发部门主机',
    NOW(),
    NOW()
),
(
    2000000000000000002,
    'WEB-SERVER-001',
    '192.168.1.10',
    '00:1B:44:11:3A:B8',
    'SERVER',
    'ACTIVE',
    'ONLINE',
    'AUTHORIZED',
    '李四',
    '2.1.0',
    'Ubuntu 20.04 LTS',
    1002,
    NOW(),
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    'Web服务器',
    NOW(),
    NOW()
),
(
    2000000000000000003,
    'MOBILE-DEVICE-001',
    '192.168.1.150',
    '02:1B:44:11:3A:C1',
    'MOBILE',
    'ACTIVE',
    'OFFLINE',
    'PENDING',
    '王五',
    '1.2.3',
    'Android 12',
    1001,
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    NULL,
    '移动设备待授权',
    NOW(),
    NOW()
),
(
    2000000000000000004,
    'TEST-PC-002',
    '192.168.1.101',
    '00:1B:44:11:3A:B9',
    'PC',
    'MAINTENANCE',
    'OFFLINE',
    'UNAUTHORIZED',
    '赵六',
    '0.9.5',
    'Windows 10 Pro',
    1003,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    NULL,
    '测试机器，待维护',
    NOW(),
    NOW()
);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 初始化脚本执行完成
-- =============================================================================

-- 查询验证数据
SELECT '=== 主机注册表数据 ===' AS info;
SELECT * FROM hosts ORDER BY created_at DESC;

SELECT '=== 主机状态统计 ===' AS info;
SELECT 
    terminal_type,
    host_status,
    online_status,
    auth_status,
    COUNT(*) as count
FROM hosts 
GROUP BY terminal_type, host_status, online_status, auth_status
ORDER BY terminal_type, host_status;