-- =============================================================================
-- 2. 插入示例数据
-- =============================================================================

-- 插入示例主机数据
INSERT INTO hosts (
    id,
    host_name,
    ip_address,
    mac_address,
    terminal_type,
    host_status,
    online_status,
    auth_status,
    responsible_person,
    user_id,
    version,
    operating_system,
    organization_id,
    last_online_time,
    auth_time,
    remarks,
    created_at,
    updated_at
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
    'USR001',
    '1.0.0',
    'Windows 11 Pro',
    '1001',
    CURRENT_TIMESTAMP,
    DATEADD('DAY', -1, CURRENT_TIMESTAMP),
    '开发部门主机',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
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
    'USR002',
    '2.1.0',
    'Ubuntu 20.04 LTS',
    '1002',
    CURRENT_TIMESTAMP,
    DATEADD('DAY', -7, CURRENT_TIMESTAMP),
    'Web服务器',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    2000000000000000003,
    'MOBILE-DEVICE-001',
    '192.168.1.150',
    '02:1B:44:11:3A:C1',
    'MOBILE',
    'ACTIVE',
    'OFFLINE',
    'UNAUTHORIZED',
    '王五',
    'USR003',
    '1.2.3',
    'Android 12',
    '1001',
    DATEADD('HOUR', -2, CURRENT_TIMESTAMP),
    NULL,
    '移动设备待授权',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
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
    'USR004',
    '0.9.5',
    'Windows 10 Pro',
    '1003',
    DATEADD('DAY', -1, CURRENT_TIMESTAMP),
    NULL,
    '测试机器，待维护',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 插入示例策略数据
INSERT INTO policies (
    id,
    name,
    description,
    status,
    version,
    is_default,
    priority,
    created_at,
    updated_at
) VALUES 
(
    3000000000000000001,
    '默认安全策略',
    '系统默认的安全策略，包含基础安全规则',
    'enabled',
    '1.0.0',
    TRUE,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    3000000000000000002,
    '开发环境策略',
    '开发环境专用策略，相对宽松的安全限制',
    'enabled',
    '1.1.0',
    FALSE,
    50,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    3000000000000000003,
    '生产环境策略',
    '生产环境高安全级别策略',
    'enabled',
    '2.0.0',
    FALSE,
    200,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    3000000000000000004,
    '移动设备策略',
    '专门针对移动设备的安全策略',
    'disabled',
    '1.0.1',
    FALSE,
    75,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 插入客户端策略映射数据
INSERT INTO client_policy_mappings (
    client_id,
    policy_id,
    assigned_at,
    activated_at,
    is_active
) VALUES 
(
    'DEV-PC-001',
    3000000000000000002,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    TRUE
),
(
    'WEB-SERVER-001',
    3000000000000000003,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    TRUE
),
(
    'MOBILE-DEVICE-001',
    3000000000000000001,
    CURRENT_TIMESTAMP,
    NULL,
    FALSE
),
(
    'TEST-PC-002',
    3000000000000000001,
    DATEADD('DAY', -2, CURRENT_TIMESTAMP),
    DATEADD('DAY', -2, CURRENT_TIMESTAMP),
    TRUE
);

-- 插入示例组织架构数据
INSERT INTO organizations (
    id,
    name,
    parent_id,
    leaf
) VALUES 
(
    '1001',
    '总公司',
    '0',
    1
),
(
    '1002',
    '技术中心',
    '1001',
    1
),
(
    '1003',
    '市场部',
    '1001',
    0
),
(
    '1004',
    '前端团队',
    '1002',
    0
),
(
    '1005',
    '后端团队',
    '1002',
    0
),
(
    '1006',
    '运维团队',
    '1002',
    0
);

-- 插入示例用户数据
INSERT INTO users (
    id,
    org_id,
    name,
    org_name,
    m_level
) VALUES 
(
    'USR001',
    '1001',
    '张三',
    '总公司',
    5
),
(
    'USR002',
    '1002',
    '李四',
    '总公司/技术中心',
    4
),
(
    'USR003',
    '1004',
    '王五',
    '总公司/技术中心/前端团队',
    3
),
(
    'USR004',
    '1005',
    '赵六',
    '总公司/技术中心/后端团队',
    3
),
(
    'USR005',
    '1003',
    '钱七',
    '总公司/市场部',
    2
);