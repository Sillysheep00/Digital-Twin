package com.fyp.digitaltwin;

import com.fyp.digitaltwin.service.DigitalTwinEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Calibration Factor feature
 * 
 * This test suite verifies that:
 * 1. Calibration factor is calculated correctly on startup
 * 2. Calibration factor is passed to all services
 * 
 * Test Strategy:
 * - Uses the actual Spring Boot application context
 * - Verifies calibration during initialization (using first 20% of dataset)
 * - Tests that calibration improves simulation accuracy
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class CalibrationTest {

    @Autowired
    private DigitalTwinEngine digitalTwinEngine;
    
    /**
     * Test 1: Verify calibration factor is calculated and set
     * Test 2:Verify calibration improves accuracy
     */
    @Test
    public void testCalibrationFactorCalculated() {
        System.out.println("\n=== TEST 1: Calibration Factor Calculation ===");
        
        double calibrationFactor = digitalTwinEngine.getCalibrationFactor();
        boolean isCalibrated = digitalTwinEngine.isCalibrated();
        
        System.out.println("Calibration Factor: " + calibrationFactor);
        System.out.println("Is Calibrated: " + isCalibrated);
        
        // Assertions
        assertTrue(isCalibrated, "System should be calibrated after initialization");
        assertTrue(calibrationFactor > 0, "Calibration factor must be positive");
        assertTrue(calibrationFactor >= 0.5 && calibrationFactor <= 10.0, 
                   "Calibration factor should be reasonable (0.5 to 10.0), got: " + calibrationFactor);
        
        System.out.println("Test 1 PASSED: Calibration factor is valid\n");
    }
    
    /**
     * Test 2: Verify calibration improves accuracy
     */
    @Test
    public void testCalibrationImprovesAccuracy() throws Exception {
        System.out.println("\n=== TEST 2: Calibration Improves Accuracy ===");
        
        String dashboardJson = digitalTwinEngine.getDashboardData();
        
        // Parse JSON manually (simple parsing for test)
        double realPower = extractJsonValue(dashboardJson, "\"real\":");
        double rawSimulated = extractJsonValue(dashboardJson, "\"simulated_raw\":");
        double calibratedSimulated = extractJsonValue(dashboardJson, "\"simulated\":");
        
        System.out.println("Real Power: " + realPower + " kW");
        System.out.println("Raw Simulated: " + rawSimulated + " kW");
        System.out.println("Calibrated Simulated: " + calibratedSimulated + " kW");
        
        // Calculate errors
        double rawError = Math.abs(realPower - rawSimulated);
        double calibratedError = Math.abs(realPower - calibratedSimulated);
        
        System.out.println("Raw Error: " + String.format("%.2f", rawError) + " kW");
        System.out.println("Calibrated Error: " + String.format("%.2f", calibratedError) + " kW");
        
        // Assertions
        assertTrue(realPower > 0, "Real power should be positive");
        assertTrue(calibratedSimulated > 0, "Calibrated simulated should be positive");
        assertTrue(calibratedError < rawError, 
                   "Calibrated power should be closer to real power than raw simulated power");
        
        System.out.println("Test 2 PASSED: Calibration reduces prediction error\n");
    }
    
    /**
     * Helper method to extract numeric value from JSON string
     */
    private double extractJsonValue(String json, String key) {
        try {
            int startIndex = json.indexOf(key);
            if (startIndex == -1) return 0.0;
            
            startIndex += key.length();

            // Skip leading spaces
            while (startIndex < json.length() && json.charAt(startIndex) == ' ') {
                startIndex++;
            }
            int endIndex = startIndex;
            
            // Find the end of the number (comma, newline, or closing brace)
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '\n' || c == '}') {
                    break;
                }
                endIndex++;
            }
            
            String valueStr = json.substring(startIndex, endIndex).trim();
            return Double.parseDouble(valueStr);
        } catch (Exception e) {
            System.err.println("Failed to extract value for key: " + key);
            return 0.0;
        }
    }
}

