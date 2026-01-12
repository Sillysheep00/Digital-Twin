package com.fyp.digitaltwin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for What-If Analysis API Endpoints
 * 
 * Tests API endpoints, input validation, and per-room base load functionality.
 * Uses full Spring Boot context for end-to-end testing.
 * 
 * Purpose (for viva):
 * "To validate economic parameters and input validation."
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class WhatIfAnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test: POST /api/what-if with valid request → returns result
     */
    @Test
    public void testWhatIfAnalysis_ValidRequest() throws Exception {
        System.out.println("\n=== API TEST: Valid What-If Request ===");

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> changes = new HashMap<>();
        changes.put("targetTemp", 21.0);
        request.put("changes", changes);
        request.put("hours", 24);
        request.put("investmentCost", 1000.0);

        MvcResult result = mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.baseline").exists())
            .andExpect(jsonPath("$.scenario").exists())
            .andExpect(jsonPath("$.energySaved").exists())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("baseline"));
        assertTrue(responseBody.contains("scenario"));

        System.out.println("API Test PASSED: Valid request returns result\n");
    }

    /**
     * Test: Missing 'changes' field → 400 Bad Request
     */
    @Test
    public void testWhatIfAnalysis_MissingChanges() throws Exception {
        System.out.println("\n=== API TEST: Missing 'changes' Field ===");

        Map<String, Object> request = new HashMap<>();
        request.put("hours", 24);
        // Missing 'changes' field

        mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        System.out.println("API Test PASSED: Missing 'changes' rejected\n");
    }

    /**
     * Test: Missing 'hours' field → 400 Bad Request
     */
    @Test
    public void testWhatIfAnalysis_MissingHours() throws Exception {
        System.out.println("\n=== API TEST: Missing 'hours' Field ===");

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> changes = new HashMap<>();
        changes.put("targetTemp", 21.0);
        request.put("changes", changes);
        // Missing 'hours' field

        mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        System.out.println("API Test PASSED: Missing 'hours' rejected\n");
    }

    /**
     * Test: Null 'changes' → handled gracefully
     */
    @Test
    public void testWhatIfAnalysis_NullChanges() throws Exception {
        System.out.println("\n=== API TEST: Null 'changes' ===");

        Map<String, Object> request = new HashMap<>();
        request.put("changes", null);
        request.put("hours", 24);

        // Should either return 400 or handle gracefully
        mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError());

        System.out.println("API Test PASSED: Null 'changes' handled\n");
    }

    /**
     * Test: Per-room base load via API → works correctly
     */
    @Test
    public void testWhatIfAnalysis_PerRoomBaseLoad() throws Exception {
        System.out.println("\n=== API TEST: Per-Room Base Load ===");

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> changes = new HashMap<>();
        Map<String, Object> roomBaseLoad = new HashMap<>();
        roomBaseLoad.put("Meeting Room", 0.5);
        roomBaseLoad.put("Staff Lounge", 0.7);
        changes.put("roomBaseLoad", roomBaseLoad);
        request.put("changes", changes);
        request.put("hours", 24);

        MvcResult result = mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("baseline") || responseBody.contains("error"));

        System.out.println("API Test PASSED: Per-room base load works\n");
    }

    /**
     * Test: Negative investment cost → rejected (400)
     */
    @Test
    public void testWhatIfAnalysis_NegativeInvestmentCost() throws Exception {
        System.out.println("\n=== API TEST: Negative Investment Cost ===");

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> changes = new HashMap<>();
        changes.put("targetTemp", 21.0);
        request.put("changes", changes);
        request.put("hours", 24);
        request.put("investmentCost", -1000.0); // Negative cost

        // Should be rejected or handled gracefully
        mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError());

        System.out.println("API Test PASSED: Negative investment cost rejected\n");
    }

    /**
     * Test: Extremely large investment cost → handled
     */
    @Test
    public void testWhatIfAnalysis_ExtremelyLargeInvestmentCost() throws Exception {
        System.out.println("\n=== API TEST: Extremely Large Investment Cost ===");

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> changes = new HashMap<>();
        changes.put("targetTemp", 21.0);
        request.put("changes", changes);
        request.put("hours", 24);
        request.put("investmentCost", 1_000_000_000.0); // Very large
    
        // Should handle without crashing
        MvcResult result = mockMvc.perform(post("/api/what-if")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || (status >= 400 && status < 500),
                   "Status should be OK or 4xx, but was: " + status);
        
        assertNotNull(result.getResponse().getContentAsString());
    
        System.out.println("API Test PASSED: Extremely large cost handled\n");
    }
}