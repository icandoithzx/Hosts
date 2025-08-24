-- 主机注册管理系统 - H2内存数据库初始化脚本
-- 包含主机表结构、索引和默认数据

-- =============================================================================
-- 1. 主机注册表 (hosts)
-- =============================================================================
DROP TABLE IF EXISTS hosts;
CREATE TABLE hosts (
    id BIGINT NOT NULL COMMENT '主机ID，使用雪花算法生成',
    host_name VARCHAR(255) NOT NULL COMMENT '主机名称',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址（支持IPv4和IPv6）',
    mac_address VARCHAR(17) NOT NULL COMMENT 'MAC地址',
    terminal_type VARCHAR(20) NOT NULL DEFAULT 'PC' COMMENT '终端类型',
    host_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '主机状态',
    online_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态',
    auth_status VARCHAR(20) NOT NULL DEFAULT 'UNAUTHORIZED' COMMENT '授权状态',
    responsible_person VARCHAR(255) NOT NULL COMMENT '责任人',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    operating_system VARCHAR(255) NOT NULL COMMENT '操作系统',
    organization_id BIGINT NOT NULL COMMENT '组织架构ID',
    last_online_time TIMESTAMP NULL COMMENT '最后在线时间',
    auth_time TIMESTAMP NULL COMMENT '授权时间',
    remarks CLOB COMMENT '备注信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
);

-- 创建唯一索引
CREATE UNIQUE INDEX uk_host_mac ON hosts (mac_address);
CREATE UNIQUE INDEX uk_host_ip_org ON hosts (ip_address, organization_id);

-- 创建普通索引
CREATE INDEX idx_hosts_host_name ON hosts (host_name);
CREATE INDEX idx_hosts_ip_address ON hosts (ip_address);
CREATE INDEX idx_hosts_terminal_type ON hosts (terminal_type);
CREATE INDEX idx_hosts_host_status ON hosts (host_status);
CREATE INDEX idx_hosts_online_status ON hosts (online_status);
CREATE INDEX idx_hosts_auth_status ON hosts (auth_status);
CREATE INDEX idx_hosts_organization_id ON hosts (organization_id);
CREATE INDEX idx_hosts_responsible_person ON hosts (responsible_person);
CREATE INDEX idx_hosts_created_at ON hosts (created_at);
CREATE INDEX idx_hosts_last_online_time ON hosts (last_online_time);

-- 添加约束检查
ALTER TABLE hosts ADD CONSTRAINT chk_terminal_type 
    CHECK (terminal_type IN ('PC', 'SERVER', 'MOBILE', 'TABLET', 'EMBEDDED', 'OTHER'));

ALTER TABLE hosts ADD CONSTRAINT chk_host_status 
    CHECK (host_status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DISABLED'));

ALTER TABLE hosts ADD CONSTRAINT chk_online_status 
    CHECK (online_status IN ('ONLINE', 'OFFLINE'));

ALTER TABLE hosts ADD CONSTRAINT chk_auth_status 
    CHECK (auth_status IN ('UNAUTHORIZED', 'AUTHORIZED', 'PENDING', 'REJECTED'));

-- =============================================================================
-- 2. 策略表 (policies)
-- =============================================================================
DROP TABLE IF EXISTS policies;
CREATE TABLE policies (
    id BIGINT NOT NULL COMMENT '策略ID，使用雪花算法生成',
    name VARCHAR(255) NOT NULL COMMENT '策略名称',
    description CLOB COMMENT '策略描述',
    status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '策略状态',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认策略',
    priority INTEGER NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_policies_name ON policies (name);
CREATE INDEX idx_policies_status ON policies (status);
CREATE INDEX idx_policies_is_default ON policies (is_default);
CREATE INDEX idx_policies_priority ON policies (priority);
CREATE INDEX idx_policies_created_at ON policies (created_at);

-- 添加约束检查
ALTER TABLE policies ADD CONSTRAINT chk_policy_status 
    CHECK (status IN ('enabled', 'disabled'));

-- =============================================================================
-- 3. 客户端策略映射表 (client_policy_mappings)
-- =============================================================================
DROP TABLE IF EXISTS client_policy_mappings;
CREATE TABLE client_policy_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_id VARCHAR(255) NOT NULL COMMENT '客户端ID',
    policy_id BIGINT NOT NULL COMMENT '策略ID',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    activated_at TIMESTAMP NULL COMMENT '激活时间（最后一次生效时间）',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否当前激活状态',
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_client_policy_client_id ON client_policy_mappings (client_id);
CREATE INDEX idx_client_policy_policy_id ON client_policy_mappings (policy_id);
CREATE INDEX idx_client_policy_is_active ON client_policy_mappings (is_active);
CREATE INDEX idx_client_policy_assigned_at ON client_policy_mappings (assigned_at);

-- 创建外键约束
ALTER TABLE client_policy_mappings ADD CONSTRAINT fk_client_policy_policy_id 
    FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE CASCADE;

-- =============================================================================
-- 4. 组织架构表 (organizations)
-- =============================================================================
DROP TABLE IF EXISTS organizations;
CREATE TABLE organizations (
    id BIGINT NOT NULL COMMENT '组织ID，来自外部系统',
    name VARCHAR(255) NOT NULL COMMENT '组织名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级组织ID，根级别为0',
    level INTEGER NOT NULL DEFAULT 1 COMMENT '组织级别',
    path VARCHAR(1000) COMMENT '组织路径，如：1/2/3',
    status INTEGER NOT NULL DEFAULT 1 COMMENT '组织状态（1-正常，0-停用）',
    sort_order INTEGER NOT NULL DEFAULT 0 COMMENT '排序字段',
    description CLOB COMMENT '描述信息',
    source_system VARCHAR(100) NOT NULL DEFAULT 'external' COMMENT '数据来源标识',
    external_version VARCHAR(100) COMMENT '外部系统的数据版本号',
    last_sync_time TIMESTAMP NULL COMMENT '最后同步时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_organizations_name ON organizations (name);
CREATE INDEX idx_organizations_parent_id ON organizations (parent_id);
CREATE INDEX idx_organizations_level ON organizations (level);
CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_source_system ON organizations (source_system);
CREATE INDEX idx_organizations_last_sync_time ON organizations (last_sync_time);
CREATE INDEX idx_organizations_created_at ON organizations (created_at);

-- 添加约束检查
ALTER TABLE organizations ADD CONSTRAINT chk_org_status 
    CHECK (status IN (0, 1));

-- 为主机表添加外键约束（可选）
-- ALTER TABLE hosts ADD CONSTRAINT fk_hosts_organization_id 
--     FOREIGN KEY (organization_id) REFERENCES organizations(id);