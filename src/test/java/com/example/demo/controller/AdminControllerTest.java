package com.example.demo.controller;

import com.example.demo.service.PolicyAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private PolicyAdminService policyAdminService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void testEnablePolicy_Success() {
        // Given
        Long policyId = 1L;
        doNothing().when(policyAdminService).updatePolicyStatus(policyId, "enabled");

        // When
        ResponseEntity<String> response = adminController.enablePolicy(policyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Policy enabled successfully", response.getBody());
        verify(policyAdminService).updatePolicyStatus(policyId, "enabled");
    }

    @Test
    void testEnablePolicy_Exception() {
        // Given
        Long policyId = 1L;
        doThrow(new IllegalArgumentException("策略不存在")).when(policyAdminService).updatePolicyStatus(policyId, "enabled");

        // When
        ResponseEntity<String> response = adminController.enablePolicy(policyId);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to enable policy"));
        assertTrue(response.getBody().contains("策略不存在"));
        verify(policyAdminService).updatePolicyStatus(policyId, "enabled");
    }

    @Test
    void testDisablePolicy_Success() {
        // Given
        Long policyId = 1L;
        doNothing().when(policyAdminService).updatePolicyStatus(policyId, "disabled");

        // When
        ResponseEntity<String> response = adminController.disablePolicy(policyId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Policy disabled successfully", response.getBody());
        verify(policyAdminService).updatePolicyStatus(policyId, "disabled");
    }

    @Test
    void testDisablePolicy_Exception() {
        // Given
        Long policyId = 1L;
        doThrow(new IllegalArgumentException("默认策略不可修改状态")).when(policyAdminService).updatePolicyStatus(policyId, "disabled");

        // When
        ResponseEntity<String> response = adminController.disablePolicy(policyId);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to disable policy"));
        assertTrue(response.getBody().contains("默认策略不可修改状态"));
        verify(policyAdminService).updatePolicyStatus(policyId, "disabled");
    }

    @Test
    void testUpdatePolicyStatus_ValidatesParameters() {
        // Given
        Long policyId = null;
        doThrow(new IllegalArgumentException("策略ID和状态不能为null")).when(policyAdminService).updatePolicyStatus(policyId, "enabled");

        // When
        ResponseEntity<String> response = adminController.enablePolicy(policyId);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("策略ID和状态不能为null"));
        verify(policyAdminService).updatePolicyStatus(policyId, "enabled");
    }
}