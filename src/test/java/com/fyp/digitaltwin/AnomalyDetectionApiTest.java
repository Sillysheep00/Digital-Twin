package com.fyp.digitaltwin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Tests for Machine Learning-Based Anomaly Detection API
 * 
 * Tests the ML-based anomaly detection system that uses a trained Linear Regression model
 * to predict expected power consumption and identify unusual patterns.
 * 
 * Test Coverage:
 * 1. Normal operation (no anomaly) with ML model
 * 2. Anomaly detection (high residual) with ML model
 * 3. API endpoint integration test
 * 4. Critical anomaly detection
 * 5. Low power edge cases
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AnomalyDetectionApiTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    /**
     * Test 1: API Endpoint - Returns Valid Anomaly Result
     * 
     * Tests the /api/anomaly endpoint to ensure it:
     * - Returns HTTP 200 OK
     * - Returns valid JSON with all required fields
     * - Integrates correctly with the live simulation
     */
    @Test
    public void testAnomalyApiEndpoint_ReturnsValidResult() throws Exception {
        System.out.println("\n=== Test 1: API Endpoint Integration ===");
        
        // Call the /api/anomaly endpoint
        MvcResult mvcResult = mockMvc.perform(get("/api/anomaly")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.anomalyDetected").exists())
                .andExpect(jsonPath("$.realPower").exists())
                .andExpect(jsonPath("$.simulatedPower").exists())
                .andExpect(jsonPath("$.calibratedSimulatedPower").exists())
                .andExpect(jsonPath("$.residual").exists())
                .andExpect(jsonPath("$.threshold").exists())
                .andExpect(jsonPath("$.severity").exists())
                .andExpect(jsonPath("$.explanation").exists())
                .andReturn();
        
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        System.out.println("API Response:\n" + jsonResponse);
        
        // Verify the response contains valid data
        assertTrue(jsonResponse.contains("anomalyDetected"), 
            "Response should contain anomalyDetected field");
        assertTrue(jsonResponse.contains("severity"), 
            "Response should contain severity field");
        
        System.out.println("Test 3 PASSED: API endpoint returns valid anomaly result\n");
    }
    
}

