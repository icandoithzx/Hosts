-- 策略管理系统 - 完整初始化脚本
-- 包含所有表结构、索引和默认数据

-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 策略表 (policies)
-- =============================================================================
DROP TABLE IF EXISTS `policies`;
CREATE TABLE `policies` (
    `id` BIGINT NOT NULL COMMENT '策略ID，使用雪花算法生成',
    `name` VARCHAR(255) NOT NULL COMMENT '策略名称',
    `description` TEXT COMMENT '策略描述',
    `status` ENUM('enabled', 'disabled') NOT NULL DEFAULT 'enabled' COMMENT '策略状态',
    `version` VARCHAR(50) NOT NULL COMMENT '策略版本',
    `is_default` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认策略',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '策略优先级，数值越大优先级越高',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_name` (`name`) COMMENT '策略名称唯一',
    KEY `idx_policies_is_default` (`is_default`),
    KEY `idx_policies_status` (`status`),
    KEY `idx_policies_priority` (`priority` DESC),
    KEY `idx_policies_created_at` (`created_at`),
    KEY `idx_policies_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略表';

-- =============================================================================
-- 2. 客户端策略映射表 (client_policy_mappings)
-- =============================================================================
DROP TABLE IF EXISTS `client_policy_mappings`;
CREATE TABLE `client_policy_mappings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_id` VARCHAR(255) NOT NULL COMMENT '客户端ID',
    `policy_id` BIGINT NOT NULL COMMENT '策略ID',
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    `activated_at` DATETIME NULL COMMENT '激活时间（最后一次生效时间）',
    `is_active` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否当前激活状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_policy` (`client_id`, `policy_id`) COMMENT '同一客户端不能重复分配相同策略',
    KEY `idx_client_policy_client_id` (`client_id`),
    KEY `idx_client_policy_policy_id` (`policy_id`),
    KEY `idx_client_policy_active` (`client_id`, `is_active`),
    KEY `idx_client_policy_activated_at` (`activated_at` DESC),
    KEY `idx_client_policy_assigned_at` (`assigned_at` DESC),
    CONSTRAINT `fk_client_policy_mapping_policy` FOREIGN KEY (`policy_id`) REFERENCES `policies` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户端策略映射表';

-- =============================================================================
-- 3. 插入默认策略数据
-- =============================================================================
-- 插入系统默认策略
INSERT INTO `policies` (
    `id`,
    `name`,
    `description`,
    `status`,
    `version`,
    `is_default`,
    `priority`,
    `created_at`,
    `updated_at`
) VALUES (
    1000000000000000001,
    '系统默认策略',
    '系统默认策略，适用于所有未分配特定策略的客户端',
    'enabled',
    '1.0.0',
    TRUE,
    -1000,
    NOW(),
    NOW()
);

-- 插入示例策略
INSERT INTO `policies` (
    `id`,
    `name`,
    `description`,
    `status`,
    `version`,
    `is_default`,
    `priority`,
    `created_at`,
    `updated_at`
) VALUES 
(
    1000000000000000002,
    '高级用户策略',
    '适用于高级用户的策略配置',
    'enabled',
    '1.0.0',
    FALSE,
    100,
    NOW(),
    NOW()
),
(
    1000000000000000003,
    '企业级策略',
    '适用于企业级用户的策略配置',
    'enabled',
    '1.0.0',
    FALSE,
    200,
    NOW(),
    NOW()
),
(
    1000000000000000004,
    '测试策略',
    '用于测试环境的策略配置',
    'disabled',
    '0.1.0',
    FALSE,
    50,
    NOW(),
    NOW()
);

-- =============================================================================
-- 4. 插入示例客户端策略映射数据
-- =============================================================================
-- 为示例客户端分配策略
INSERT INTO `client_policy_mappings` (
    `client_id`,
    `policy_id`,
    `assigned_at`,
    `activated_at`,
    `is_active`
) VALUES 
(
    'client001',
    1000000000000000002,
    NOW(),
    NOW(),
    TRUE
),
(
    'client002',
    1000000000000000003,
    NOW(),
    NOW(),
    TRUE
),
(
    'client003',
    1000000000000000002,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    NULL,
    FALSE
),
(
    'client003',
    1000000000000000003,
    NOW(),
    NOW(),
    TRUE
);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 初始化脚本执行完成
-- =============================================================================

-- 查询验证数据
SELECT '=== 策略表数据 ===' AS info;
SELECT * FROM policies ORDER BY priority DESC;

SELECT '=== 客户端策略映射数据 ===' AS info;
SELECT * FROM client_policy_mappings ORDER BY assigned_at DESC;