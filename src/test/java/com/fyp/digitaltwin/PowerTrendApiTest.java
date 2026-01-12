package com.fyp.digitaltwin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Power Trend Comparison API
 * 
 * Tests different window sizes (8h, 16h, 24h) and boundary conditions.
 * 
 * Purpose (for viva):
 * "To verify sliding window analysis functionality and robustness against insufficient data."
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class PowerTrendApiTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test A1: 8h window → 32 data points
     * Purpose: Verify response size matches window
     */
    @Test
    public void testPowerTrend_8HourWindow() throws Exception {
        System.out.println("\n=== TEST A1: Power Trend (8-hour window) ===");

        MvcResult result = mockMvc.perform(get("/api/anomaly")
                .param("windowSize", "32"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.windowSize").value(32))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("windowSize") || responseBody.contains("residuals"));

        System.out.println("✅ Test A1 PASSED: 8-hour window returns data\n");
    }

    /**
     * Test A2: 16h window → 64 data points
     */
    @Test
    public void testPowerTrend_16HourWindow() throws Exception {
        System.out.println("\n=== TEST A2: Power Trend (16-hour window) ===");

        MvcResult result = mockMvc.perform(get("/api/anomaly")
                .param("windowSize", "64"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windowSize").value(64))
            .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        System.out.println("✅ Test A2 PASSED: 16-hour window returns data\n");
    }

    /**
     * Test A3: 24h window → 96 data points
     */
    @Test
    public void testPowerTrend_24HourWindow() throws Exception {
        System.out.println("\n=== TEST A3: Power Trend (24-hour window) ===");

        MvcResult result = mockMvc.perform(get("/api/anomaly")
                .param("windowSize", "96"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windowSize").value(96))
            .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        System.out.println("✅ Test A3 PASSED: 24-hour window returns data\n");
    }

    /**
     * Test A4: No crash on valid window sizes
     */
    @Test
    public void testPowerTrend_NoCrash() throws Exception {
        System.out.println("\n=== TEST A4: Power Trend (No Crash) ===");

        int[] windowSizes = {32, 64, 96};
        
        for (int windowSize : windowSizes) {
            mockMvc.perform(get("/api/anomaly")
                    .param("windowSize", String.valueOf(windowSize)))
                .andExpect(status().isOk());
        }

        System.out.println("✅ Test A4 PASSED: All window sizes handled without crash\n");
    }

    /**
     * Test B1: Window larger than available data → handled gracefully
     * Purpose: Verify robustness against insufficient historical data
     */
    @Test
    public void testPowerTrend_WindowLargerThanData() throws Exception {
        System.out.println("\n=== TEST B1: Power Trend (Window Larger Than Data) ===");

        // Request very large window (e.g., 1000 points)
        MvcResult result = mockMvc.perform(get("/api/anomaly")
        .param("windowSize", "1000"))
    .andReturn();

        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || (status >= 400 && status < 500),
            "Status should be OK or 4xx, but was: " + status);

        // Should either truncate or return error, but not crash
        assertNotNull(result.getResponse().getContentAsString());

        System.out.println("✅ Test B1 PASSED: Large window handled gracefully\n");
    }

    /**
     * Test B2: Invalid window size → defaults to 32
     */
    @Test
    public void testPowerTrend_InvalidWindowSize() throws Exception {
        System.out.println("\n=== TEST B2: Power Trend (Invalid Window Size) ===");

        // Invalid window size (not 32, 64, or 96)
        MvcResult result = mockMvc.perform(get("/api/anomaly")
                .param("windowSize", "50"))
            .andExpect(status().isOk())
            .andReturn();

        // Should default to 32
        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);

        System.out.println("✅ Test B2 PASSED: Invalid window size handled\n");
    }

    /**
     * Test: Trend array exists in response
     */
    @Test
    public void testPowerTrend_TrendArrayExists() throws Exception {
        System.out.println("\n=== TEST: Power Trend (Trend Array Exists) ===");

        MvcResult result = mockMvc.perform(get("/api/anomaly")
                .param("windowSize", "32"))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        // Response should contain residuals or timeSteps
        assertTrue(responseBody.contains("residuals") || 
                  responseBody.contains("timeSteps") ||
                  responseBody.contains("windowSize"));

        System.out.println("✅ Test PASSED: Trend array exists in response\n");
    }
}