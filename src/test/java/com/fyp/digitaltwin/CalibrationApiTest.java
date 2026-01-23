package com.fyp.digitaltwin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API test for Calibration Feature
 * 
 * Verifies that the dashboard endpoint returns calibrated power values
 * in the expected JSON format.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class CalibrationApiTest {

    @Autowired
    private MockMvc mockMvc;
    
    /**
     * Test: GET /api/dashboard returns calibrated values
     * 
     * Expected Response:
     * {
     *   "power": {
     *     "real": 44.49,
     *     "simulated_raw": 13.81,
     *     "simulated": 44.47,
     *     "calibration_factor": 3.2217,
     *     ...
     *   }
     * }
     */
    @Test
    public void testDashboardReturnsCalibrationData() throws Exception {
        System.out.println("\n=== API TEST: GET /api/dashboard ===");
        
        MvcResult result = mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.power").exists())
            .andExpect(jsonPath("$.power.real").isNumber())
            .andExpect(jsonPath("$.power.simulated_raw").isNumber())
            .andExpect(jsonPath("$.power.simulated").isNumber())
            .andExpect(jsonPath("$.power.calibration_factor").isNumber())
            .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        
        // Extract and display the power section
        String powerSection = extractPowerSection(responseBody);
        System.out.println("Power Data:");
        System.out.println(powerSection);
        
        System.out.println(" API Test PASSED: Dashboard returns calibrated data\n");
    }
    
    /**
     * Helper to extract and format the power section from JSON response
     */
    private String extractPowerSection(String json) {
        try {
            int powerStart = json.indexOf("\"power\":");
            if (powerStart == -1) return "Power section not found";
            
            int braceCount = 0;
            int start = json.indexOf("{", powerStart);
            int end = start;
            
            for (int i = start; i < json.length(); i++) {
                if (json.charAt(i) == '{') braceCount++;
                if (json.charAt(i) == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        end = i + 1;
                        break;
                    }
                }
            }
            
            String powerJson = json.substring(start, end);
            
            // Extract key values
            double real = extractValue(powerJson, "\"real\":");
            double raw = extractValue(powerJson, "\"simulated_raw\":");
            double calibrated = extractValue(powerJson, "\"simulated\":");
            double factor = extractValue(powerJson, "\"calibration_factor\":");
            
            return String.format(
                "{\n  \"real\": %.2f,\n  \"simulated_raw\": %.2f,\n  \"simulated\": %.2f,\n  \"calibration_factor\": %.4f\n}",
                real, raw, calibrated, factor
            );
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * Helper to extract numeric value from JSON string
     */
    private double extractValue(String json, String key) {
        try {
            int start = json.indexOf(key) + key.length();
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
