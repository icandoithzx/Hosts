package com.example.demo.dto;

import lombok.Data;

/**
 * 用于接收删除策略请求的数据传输对象。
 */
@Data
public class DeletePolicyRequestDto {
    /**
     * 需要被删除的策略ID。
     */
    private Long policyId;
}
