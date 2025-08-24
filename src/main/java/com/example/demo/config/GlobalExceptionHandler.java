package com.example.demo.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 * 用于捕获特定类型的异常，并返回统一、规范的JSON错误响应。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理因请求参数类型不匹配而引发的异常。
     * 例如，当URL路径变量或请求参数期望是数字，但收到的是字符串时。
     * @param ex 捕获到的异常
     * @return 包含友好错误信息的ResponseEntity
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "无效的参数类型");
        // 构建更具体的错误消息
        String message = String.format("参数 '%s' 的值 '%s' 格式不正确。期望的类型是 '%s'。",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        body.put("message", message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理在业务逻辑中手动抛出的非法参数异常。
     * 例如，当DTO中的ID字段无法转换为Long时。
     * @param ex 捕获到的异常
     * @return 包含友好错误信息的ResponseEntity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "非法的参数");
        body.put("message", ex.getMessage()); // 直接使用我们抛出时定义的具体消息
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
