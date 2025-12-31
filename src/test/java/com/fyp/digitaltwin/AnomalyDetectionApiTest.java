package com.fyp.digitaltwin;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.service.AnomalyDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    /**
     * Helper method to create a mock Linear Regression model for testing.
     * Simulates a trained model with slope=3.2 and intercept=0.
     */
    private LinearRegressionModel createMockModel(double slope, double intercept) {
        return new LinearRegressionModel(
            slope,           // slope (a)
            intercept,       // intercept (b)
            0.95,           // R² score
            2.5,            // RMSE
            1000,           // training size
            "2024-01-01"    // trained date
        );
    }
    
    /**
     * Test 1: Normal Operation - No Anomaly Detected (ML-Based)
     * 
     * Scenario: Real power closely matches ML-predicted power
     * Expected: anomalyDetected = false, severity = "NORMAL"
     */
    @Test
    public void testNormalOperation_NoAnomalyDetected() throws Exception {
        System.out.println("\n=== Test 1: Normal Operation (No Anomaly) - ML ===");
        
        // Create mock ML model: predictedPower = 3.2 × simulatedPower + 0
        LinearRegressionModel model = createMockModel(3.2, 0.0);
        
        // Test with normal values
        // Real: 45 kW, Simulated: 14 kW
        // ML Predicted: 3.2 × 14 + 0 = 44.8 kW
        // Residual = |45 - 44.8| = 0.2 kW
        // Threshold = 45 × 0.25 = 11.25 kW
        // 0.2 < 11.25 → No anomaly
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(45.0, 14.0, model);
        
        assertFalse(result.isAnomalyDetected(), 
            "No anomaly should be detected when residual is small");
        assertEquals("NORMAL", result.getSeverity(),
            "Severity should be NORMAL for small residuals");
        assertEquals(45.0, result.getRealPower(), 0.01);
        assertEquals(44.8, result.getCalibratedSimulatedPower(), 0.01);
        
        System.out.println("✓ Real Power: " + result.getRealPower() + " kW");
        System.out.println("✓ ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println("✓ Residual: " + result.getResidual() + " kW");
        System.out.println("✓ Threshold: " + result.getThreshold() + " kW");
        System.out.println("✓ Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println("✓ Severity: " + result.getSeverity());
        System.out.println("Test 1 PASSED: Normal operation correctly identified (ML)\n");
    }
    
    /**
     * Test 2: Anomaly Detection - High Residual (ML-Based)
     * 
     * Scenario: Real power significantly exceeds ML-predicted power
     * Expected: anomalyDetected = true, severity = "WARNING" or "CRITICAL"
     */
    @Test
    public void testAnomalyDetection_HighResidual() throws Exception {
        System.out.println("\n=== Test 2: Anomaly Detection (High Residual) - ML ===");
        
        // Create mock ML model
        LinearRegressionModel model = createMockModel(3.2, 0.0);
        
        // Test with anomalous values
        // Real: 80 kW, Simulated: 14 kW
        // ML Predicted: 3.2 × 14 + 0 = 44.8 kW
        // Residual = |80 - 44.8| = 35.2 kW
        // Threshold = 80 × 0.25 = 20 kW
        // 35.2 > 20 → Anomaly detected!
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(80.0, 14.0, model);
        
        assertTrue(result.isAnomalyDetected(), 
            "Anomaly should be detected when residual exceeds threshold");
        assertNotEquals("NORMAL", result.getSeverity(),
            "Severity should not be NORMAL for high residuals");
        assertTrue(result.getResidual() > result.getThreshold(),
            "Residual should exceed threshold for anomaly detection");
        
        System.out.println("✓ Real Power: " + result.getRealPower() + " kW");
        System.out.println("✓ ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println("✓ Residual: " + result.getResidual() + " kW");
        System.out.println("✓ Threshold: " + result.getThreshold() + " kW");
        System.out.println("✓ Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println("✓ Severity: " + result.getSeverity());
        System.out.println("✓ Explanation: " + result.getExplanation());
        System.out.println("Test 2 PASSED: Anomaly correctly detected (ML)\n");
    }
    
    /**
     * Test 3: API Endpoint - Returns Valid Anomaly Result
     * 
     * Tests the /api/anomaly endpoint to ensure it:
     * - Returns HTTP 200 OK
     * - Returns valid JSON with all required fields
     * - Integrates correctly with the live simulation
     */
    @Test
    public void testAnomalyApiEndpoint_ReturnsValidResult() throws Exception {
        System.out.println("\n=== Test 3: API Endpoint Integration ===");
        
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
    
    /**
     * Test 4: Critical Anomaly Detection (ML-Based)
     * 
     * Scenario: Real power is extremely different from ML prediction (>30% deviation)
     * Expected: severity = "CRITICAL"
     */
    @Test
    public void testCriticalAnomaly_DetectedCorrectly() throws Exception {
        System.out.println("\n=== Test 4: Critical Anomaly Detection - ML ===");
        
        // Create mock ML model
        LinearRegressionModel model = createMockModel(3.2, 0.0);
        
        // Test with extreme deviation
        // Real: 100 kW, Simulated: 14 kW
        // ML Predicted: 3.2 × 14 + 0 = 44.8 kW
        // Residual = |100 - 44.8| = 55.2 kW
        // Residual percentage = 55.2/100 = 55.2% (> 30% = CRITICAL)
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(100.0, 14.0, model);
        
        assertTrue(result.isAnomalyDetected(), 
            "Critical anomaly should be detected");
        assertEquals("CRITICAL", result.getSeverity(),
            "Severity should be CRITICAL for extreme deviations");
        
        System.out.println("✓ Real Power: " + result.getRealPower() + " kW");
        System.out.println("✓ ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println("✓ Residual: " + result.getResidual() + " kW");
        System.out.println("✓ Severity: " + result.getSeverity());
        System.out.println("✓ Explanation: " + result.getExplanation());
        System.out.println("Test 4 PASSED: Critical anomaly correctly identified (ML)\n");
    }
    
    /**
     * Test 5: Edge Case - Low Power Conditions (ML-Based)
     * 
     * Scenario: Both real and ML-predicted power are very low
     * Expected: Should not produce false positives
     */
    @Test
    public void testLowPowerConditions_NoFalsePositive() throws Exception {
        System.out.println("\n=== Test 5: Low Power Edge Case - ML ===");
        
        // Create mock ML model
        LinearRegressionModel model = createMockModel(3.2, 0.0);
        
        // Test with low power values
        // Real: 2 kW, Simulated: 0.6 kW
        // ML Predicted: 3.2 × 0.6 + 0 = 1.92 kW
        // Residual = |2 - 1.92| = 0.08 kW
        // Threshold = max(2 × 0.25, 1.0) = max(0.5, 1.0) = 1.0 kW (minimum threshold)
        // 0.08 < 1.0 → No anomaly
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(2.0, 0.6, model);
        
        assertFalse(result.isAnomalyDetected(), 
            "Should not detect false positives in low power conditions");
        assertEquals("NORMAL", result.getSeverity(),
            "Low power normal operation should be classified as NORMAL");
        
        System.out.println("✓ Real Power: " + result.getRealPower() + " kW");
        System.out.println("✓ ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println("✓ Residual: " + result.getResidual() + " kW");
        System.out.println("✓ Threshold: " + result.getThreshold() + " kW (minimum applied)");
        System.out.println("✓ Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println("Test 5 PASSED: No false positives in low power conditions (ML)\n");
    }
}

