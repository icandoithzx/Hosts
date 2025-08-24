package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import lombok.Data;

/**
 * 用于接收删除策略请求的数据传输对象。
 */
@Data
public class DeletePolicyRequestDto {
    /**
     * 需要被删除的策略ID。
     * 接受 String 类型输入，自动转换为 Long 类型处理。
     */
    @JsonProperty("policyId")
    @JsonDeserialize(using = NumberDeserializers.LongDeserializer.class)
    private Long policyId;
}
