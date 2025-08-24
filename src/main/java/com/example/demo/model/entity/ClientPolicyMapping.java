package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("client_policy_mappings")
public class ClientPolicyMapping implements Serializable {
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id; // 主键
    
    @TableField("client_id")
    private String clientId;
    
    @TableField("policy_id")
    private Long policyId; // 修改为 Long 类型以匹配 Policy 的 ID
    
    @TableField("assigned_at")
    private LocalDateTime assignedAt; // 分配时间
    
    @TableField("activated_at")
    private LocalDateTime activatedAt; // 激活时间（最后一次生效时间）
    
    @TableField("is_active")
    private Boolean isActive; // 是否当前激活状态
}