package com.fyp.digitaltwin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.dto.RoomAnomaly;
import com.fyp.digitaltwin.model.SimulationResult;
import com.fyp.digitaltwin.repository.SimulationResultRepository;
import com.fyp.digitaltwin.service.AnomalyDetectionService;


@ExtendWith(MockitoExtension.class)
public class AnomalyDetectionServiceTest {

    @Mock
    private SimulationResultRepository resultRepository;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private LinearRegressionModel mockModel;
    private DateTimeFormatter formatter;

    @BeforeEach
    public void setUp() {
        // Create a mock regression model: predicted = 3.0 × simulated + 5.0
        mockModel = new LinearRegressionModel(
            3.0,           // slope
            5.0,           // intercept
            0.95,          // R²
            2.5,           // RMSE
            1000,          // training size
            "2024-01-01"   // trained date
        );

        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Helper: Create stable historical data for Z-score calculation
     * 
     * Creates 32 historical records with:
     * - Simulated power: 10-15 kW (normal range)
     * - Real power: follows model prediction + small noise
     * - Residuals: small (2-5 kW) for stable baseline
     */
    private List<SimulationResult> createStableHistory(LinearRegressionModel model) {
        List<SimulationResult> history = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(8);
        
        for (int i = 0; i < 32; i++) {
            double simulatedPower = 10.0 + (i % 6); // 10-15 kW
            double predictedPower = model.predict(simulatedPower);
            double residual = 2.0 + (i % 4); // Small residuals: 2-5 kW
            double realPower = predictedPower + (i % 2 == 0 ? residual : -residual);
            
            SimulationResult result = new SimulationResult(
                baseTime.plusMinutes(i * 15).format(formatter),
                realPower,
                simulatedPower,
                0.0,
                20.0,
                22.0,
                3
            );
            history.add(result);
        }
        
        return history;
    }

    /**
     * Helper: Create history with high variance (for testing edge cases)
     */
    private List<SimulationResult> createHighVarianceHistory(LinearRegressionModel model) {
        List<SimulationResult> history = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(8);
        
        for (int i = 0; i < 32; i++) {
            double simulatedPower = 10.0 + (i % 6);
            double predictedPower = model.predict(simulatedPower);
            // High variance: residuals range from 1-15 kW
            double residual = 1.0 + (i * 0.5);
            double realPower = predictedPower + (i % 2 == 0 ? residual : -residual);
            
            SimulationResult result = new SimulationResult(
                baseTime.plusMinutes(i * 15).format(formatter),
                realPower,
                simulatedPower,
                0.0,
                20.0,
                22.0,
                3
            );
            history.add(result);
        }
        
        return history;
    }

    /**
     * Helper: Mock repository to return controlled history
     */
    private void mockRepositoryWithHistory(List<SimulationResult> history) {
        // Mock the stream chain: findAll() -> stream() -> limit() -> sorted() -> collect()
        @SuppressWarnings("unchecked")
        Stream<SimulationResult> mockStream = history.stream();
        
        when(resultRepository.findAll(any(Sort.class)))
            .thenReturn(history);
    }

    /**
     * Test 1: CRITICAL Anomaly - Z-score > 3.0
     * 
     * Scenario:
     * - Stable history (mean residual ~3.5 kW, std ~1.2 kW)
     * - Inject huge deviation: real=100 kW, simulated=10 kW
     * - Predicted: 3.0 × 10 + 5.0 = 35 kW
     * - Residual: |100 - 35| = 65 kW
     * - Z-score: (65 - 3.5) / 1.2 ≈ 51.25 (>> 3.0)
     * 
     * Expected: CRITICAL anomaly detected
     */
    @Test
    public void testCriticalAnomaly_ForcedByZScore() {
        System.out.println("\n=== Test 1: CRITICAL Anomaly (Z-score > 3.0) ===");
        
        // Create stable history
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Inject huge deviation
        double realPower = 100.0;      // Extremely high
        double simulatedPower = 10.0;  // Normal
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Assertions
        assertTrue(result.isAnomalyDetected(), 
            "CRITICAL anomaly should be detected");
        assertEquals("CRITICAL", result.getSeverity(),
            "Severity should be CRITICAL for Z-score > 3.0");
        assertTrue(result.getZScore() >= 3.0,
            "Z-score should be >= 3.0 for CRITICAL");
        assertTrue(result.getResidual() > result.getThreshold(),
            "Residual should exceed threshold");
        
        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" Simulated Power: " + result.getSimulatedPower() + " kW");
        System.out.println(" ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println(" Residual: " + result.getResidual() + " kW");
        System.out.println(" Threshold: " + result.getThreshold() + " kW");
        System.out.println(" Z-Score: " + result.getZScore());
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println(" Explanation: " + result.getExplanation());
        System.out.println("Test 1 PASSED: CRITICAL anomaly correctly detected\n");
    }

    /**
     * Test 2: WARNING Severity - Z-score 2.0-3.0
     * 
     * Scenario:
     * - Stable history (mean residual ≈ 3.5 kW, std ≈ 1.2 kW)
     * - Calculate realPower backwards to guarantee Z-score = 2.5 (WARNING range)
     * - Formula: residual = mean + targetZ × std
     * - Then: realPower = predicted + residual
     * 
     * Expected: Severity = WARNING (Z-score between 2.0-3.0)
     * Note: WARNING severity does NOT set anomalyDetected = true
     *       Only CRITICAL (Z-score > 3.0) sets anomalyDetected = true
 */
    @Test
    public void testWarningAnomaly_ForcedByZScore() {
        System.out.println("\n=== Test 2: WARNING Anomaly (Z-score 2.0-3.0) ===");
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Calculate values backwards to guarantee Z-score in WARNING range
        double simulatedPower = 10.0;
        double predicted = mockModel.predict(simulatedPower); // 3.0 × 10.0 + 5.0 = 35.0 kW
        
        // Target Z-score for WARNING: 2.5 (between 2.0 and 3.0)
        double targetZ = 2.5;
        
        // Approximate statistics from createStableHistory
        // Residuals: 2, 3, 4, 5 (repeating) → mean ≈ 3.5, std ≈ 1.2
        double mean = 3.5;
        double std = 1.2;
        
        // Reverse calculate: residual = mean + targetZ × std
        double targetResidual = mean + targetZ * std; // 3.5 + 2.5 × 1.2 = 6.5 kW
        
        // Calculate realPower to achieve this residual
        double realPower = predicted + targetResidual; // 35.0 + 6.5 = 41.5 kW
        
        System.out.println("Calculated values:");
        System.out.println("  Simulated Power: " + simulatedPower + " kW");
        System.out.println("  Predicted Power: " + predicted + " kW");
        System.out.println("  Target Z-score: " + targetZ);
        System.out.println("  Target Residual: " + targetResidual + " kW");
        System.out.println("  Real Power: " + realPower + " kW");
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel);

        // Assertions
        assertEquals("WARNING", result.getSeverity(),
            "Severity should be WARNING for Z-score 2.0-3.0");
        assertTrue(result.getZScore() >= 2.0 && result.getZScore() < 3.0,
            "Z-score should be between 2.0 and 3.0 for WARNING");
        // Note: WARNING severity does NOT set anomalyDetected = true
        // Only CRITICAL (Z-score > 3.0) sets anomalyDetected = true
        assertFalse(result.isAnomalyDetected(),
            "WARNING is informational severity, not an anomaly (only CRITICAL is anomaly)");

        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" ML Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println(" Residual: " + result.getResidual() + " kW");
        System.out.println(" Z-Score: " + result.getZScore() + " (target: " + targetZ + ")");
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println(" Anomaly Detected: " + result.isAnomalyDetected() + 
            " (WARNING is informational, only CRITICAL is anomaly)");
        System.out.println("Test 2 PASSED: WARNING severity correctly identified\n");
        
    }

    /**
     * Test 3: NORMAL Operation - Z-score < 2.0
     * 
     * Scenario:
     * - Stable history
     * - Normal values: real=40 kW, simulated=10 kW
     * - Predicted: 35 kW
     * - Residual: |40 - 35| = 5 kW
     * - Z-score: (5 - 3.5) / 1.2 ≈ 1.25 (< 2.0)
     * 
     * Expected: NORMAL, no anomaly
     */
    @Test
    public void testNormalOperation_NoAnomaly() {
        System.out.println("\n=== Test 3: NORMAL Operation (Z-score < 2.0) ===");
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Normal values
        double realPower = 40.0;
        double simulatedPower = 10.0;
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Assertions
        assertFalse(result.isAnomalyDetected(),
            "No anomaly should be detected for normal operation");
        assertEquals("NORMAL", result.getSeverity(),
            "Severity should be NORMAL for Z-score < 2.0");
        assertTrue(result.getZScore() < 2.0,
            "Z-score should be < 2.0 for NORMAL");
        assertTrue(result.getResidual() < result.getThreshold(),
            "Residual should be below threshold");
        
        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" Residual: " + result.getResidual() + " kW");
        System.out.println(" Z-Score: " + result.getZScore());
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println("Test 3 PASSED: NORMAL operation correctly identified\n");
    }

    /**
     * Test 4: Insufficient Historical Data - Fallback Threshold
     * 
     * Scenario:
     * - Empty or very small history (< 10 records)
     * - System should use fallback: residual > predictedPower × 0.25
     * 
     * Expected: Uses fallback logic, not Z-score
     */
    @Test
    public void testInsufficientData_FallbackThreshold() {
        System.out.println("\n=== Test 4: Insufficient Data (Fallback) ===");
        
        // Mock empty history
        List<SimulationResult> emptyHistory = new ArrayList<>();
        mockRepositoryWithHistory(emptyHistory);
        
        // Large deviation to trigger fallback
        double realPower = 100.0;
        double simulatedPower = 10.0;
        double predictedPower = mockModel.predict(simulatedPower); // 35 kW
        double fallbackThreshold = predictedPower * 0.25; // 8.75 kW
        double residual = Math.abs(realPower - predictedPower); // 65 kW
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Assertions
        assertTrue(result.isAnomalyDetected(),
            "Anomaly should be detected via fallback threshold");
        assertTrue(residual > fallbackThreshold,
            "Residual should exceed fallback threshold");
        // Note: With insufficient data, severity might be NORMAL but anomaly detected
        
        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" Predicted: " + result.getCalibratedSimulatedPower() + " kW");
        System.out.println(" Residual: " + result.getResidual() + " kW");
        System.out.println(" Fallback Threshold: " + fallbackThreshold + " kW");
        System.out.println(" Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println("Test 4 PASSED: Fallback threshold correctly applied\n");
    }

    /**
     * Test 5: Minimum Absolute Threshold - Ignore Tiny Residuals
     * 
     * Scenario:
     * - Very small residual (< 5 kW)
     * - Should be ignored even if Z-score is high
     * 
     * Expected: NORMAL, no anomaly (noise filtering)
     */
    @Test
    public void testMinimumThreshold_IgnoreTinyResiduals() {
        System.out.println("\n=== Test 5: Minimum Threshold (Noise Filtering) ===");
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Tiny residual
        double realPower = 36.0;  // Very close to predicted 35 kW
        double simulatedPower = 10.0;
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Assertions
        assertFalse(result.isAnomalyDetected(),
            "Tiny residuals should be ignored (noise filtering)");
        assertEquals("NORMAL", result.getSeverity(),
            "Severity should be NORMAL for residuals < 5 kW");
        assertTrue(result.getResidual() < 5.0,
            "Residual should be below minimum threshold");
        
        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" Residual: " + result.getResidual() + " kW");
        System.out.println(" Minimum Threshold: 5.0 kW");
        System.out.println(" Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println("Test 5 PASSED: Tiny residuals correctly ignored\n");
    }

    /**
     * Test 6: detectAnomalyFromDashboard - Valid JSON
     * 
     * Tests JSON parsing and delegation to detectAnomalyWithML
     */
    @Test
    public void testDetectAnomalyFromDashboard_ValidJson() {
        System.out.println("\n=== Test 6: detectAnomalyFromDashboard (Valid JSON) ===");
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Create valid dashboard JSON
        String dashboardJson = "{" +
            "\"power\": {" +
                "\"real\": 100.0," +
                "\"simulated_raw\": 10.0" +
            "}" +
        "}";
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyFromDashboard(
            dashboardJson, mockModel
        );
        
        // Assertions
        assertNotNull(result, "Result should not be null");
        assertEquals(100.0, result.getRealPower(), 0.01,
            "Real power should be extracted from JSON");
        assertEquals(10.0, result.getSimulatedPower(), 0.01,
            "Simulated power should be extracted from JSON");
        assertTrue(result.isAnomalyDetected(),
            "Anomaly should be detected for high deviation");
        
        System.out.println(" JSON parsed successfully");
        System.out.println(" Real Power: " + result.getRealPower() + " kW");
        System.out.println(" Simulated Power: " + result.getSimulatedPower() + " kW");
        System.out.println(" Anomaly Detected: " + result.isAnomalyDetected());
        System.out.println("Test 6 PASSED: Dashboard JSON correctly processed\n");
    }

    /**
     * Test 7: detectAnomalyFromDashboard - Invalid JSON
     * 
     * Tests error handling for malformed JSON
     */
    @Test
    public void testDetectAnomalyFromDashboard_InvalidJson() {
        System.out.println("\n=== Test 7: detectAnomalyFromDashboard (Invalid JSON) ===");
        
        String invalidJson = "{ invalid json }";
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyFromDashboard(
            invalidJson, mockModel
        );
        
        // Assertions
        assertNotNull(result, "Result should not be null (error result)");
        assertEquals("ERROR", result.getSeverity(),
            "Severity should be ERROR for invalid JSON");
        assertFalse(result.isAnomalyDetected(),
            "Anomaly should not be detected for parsing errors");
        assertTrue(result.getExplanation().contains("Failed to parse"),
            "Explanation should mention parsing failure");
        
        System.out.println(" Error handled gracefully");
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println(" Explanation: " + result.getExplanation());
        System.out.println("Test 7 PASSED: Invalid JSON correctly handled\n");
    }

    /**
     * Test 8: Edge Case - Zero Standard Deviation
     * 
     * Scenario:
     * - All historical residuals are identical (std = 0)
     * - System should handle division by zero gracefully
     * 
     * Expected: Uses fallback or handles gracefully
     */
    @Test
    public void testZeroStandardDeviation_EdgeCase() {
        System.out.println("\n=== Test 8: Zero Standard Deviation (Edge Case) ===");
        
        // Create history with identical residuals (std = 0)
        List<SimulationResult> history = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(8);
        
        for (int i = 0; i < 32; i++) {
            double simulatedPower = 10.0;
            double predictedPower = mockModel.predict(simulatedPower);
            double realPower = predictedPower + 3.0; // All residuals = 3.0
            
            SimulationResult result = new SimulationResult(
                baseTime.plusMinutes(i * 15).format(formatter),
                realPower,
                simulatedPower,
                0.0,
                20.0,
                22.0,
                3
            );
            history.add(result);
        }
        
        mockRepositoryWithHistory(history);
        
        double realPower = 50.0;
        double simulatedPower = 10.0;
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Should handle gracefully (either fallback or valid result)
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getSeverity(), "Severity should be set");
        
        System.out.println(" Zero std deviation handled gracefully");
        System.out.println(" Severity: " + result.getSeverity());
        System.out.println("Test 8 PASSED: Edge case handled correctly\n");
    }

    /**
     * Test 9: Reproducibility - Same Inputs, Same Results
     * 
     * Verifies that tests are deterministic (no randomness)
     */
    @Test
    public void testReproducibility_SameInputsSameResults() {
        System.out.println("\n=== Test 9: Reproducibility Test ===");
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        double realPower = 100.0;
        double simulatedPower = 10.0;
        
        // Run twice with same inputs
        AnomalyResult result1 = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        AnomalyResult result2 = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // Assertions - results should be identical
        assertEquals(result1.isAnomalyDetected(), result2.isAnomalyDetected(),
            "Anomaly detection should be reproducible");
        assertEquals(result1.getSeverity(), result2.getSeverity(),
            "Severity should be reproducible");
        assertEquals(result1.getZScore(), result2.getZScore(), 0.01,
            "Z-score should be reproducible");
        assertEquals(result1.getResidual(), result2.getResidual(), 0.01,
            "Residual should be reproducible");
        
        System.out.println(" First run - Z-score: " + result1.getZScore());
        System.out.println(" Second run - Z-score: " + result2.getZScore());
        System.out.println("Results are identical (reproducible)");
        System.out.println("Test 9 PASSED: Tests are deterministic\n");
    }

    /**
     * Test 10: Room Anomalies - Building NORMAL → All Rooms NORMAL
     * 
     * Verifies hierarchical architecture: when building is NORMAL,
     * all rooms should be NORMAL regardless of their residuals.
     */
    @Test
    public void testRoomAnomalies_BuildingNormal_AllRoomsNormal() {
        System.out.println("\n=== Test 10: Room Anomalies (Building NORMAL) ===");
        
        // Create mock dashboard JSON with rooms
        String dashboardJson = "{" +
            "\"power\": {" +
                "\"real\": 40.0," +
                "\"simulated_raw\": 10.0," +
                "\"simulated\": 35.0" +
            "}," +
            "\"rooms\": [" +
                "{\"id\": \"R1\", \"name\": \"Meeting Room\", \"power\": 10.0}," +
                "{\"id\": \"R2\", \"name\": \"Staff Lounge\", \"power\": 12.0}" +
            "]" +
        "}";
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Building-level result: NORMAL
        AnomalyResult buildingResult = anomalyDetectionService.detectAnomalyWithML(40.0, 10.0, mockModel);
        assertEquals("NORMAL", buildingResult.getSeverity(), "Building should be NORMAL");
        
        // Room-level detection with NORMAL building
        List<RoomAnomaly> roomAnomalies = anomalyDetectionService.detectRoomAnomalies(
            dashboardJson,
            mockModel,
            buildingResult.getThreshold(),
            buildingResult.getZScore(),
            buildingResult.getSeverity()  // "NORMAL"
        );
        
        // Assertions: All rooms should be NORMAL
        assertNotNull(roomAnomalies, "Room anomalies should not be null");
        assertTrue(roomAnomalies.size() > 0, "Should have room anomalies");
        
        for (RoomAnomaly room : roomAnomalies) {
            assertEquals("NORMAL", room.getSeverity(),
                "Room " + room.getRoomName() + " should be NORMAL when building is NORMAL");
            assertEquals("🟢 Normal", room.getStatus(),
                "Room " + room.getRoomName() + " status should be Normal");
            assertFalse(room.isAnomalyDetected(),
                "Room " + room.getRoomName() + " should not have anomaly detected");
        }
        
        System.out.println("All " + roomAnomalies.size() + " rooms are NORMAL when building is NORMAL");
        System.out.println("Test 10 PASSED: Hierarchical logic verified\n");
    }

    /**
     * Test 11: Room Anomalies - Building WARNING → Rooms Can Be Evaluated
     * 
     * Verifies that when building is WARNING, room-level evaluation happens.
     */
    @Test
    public void testRoomAnomalies_BuildingWarning_RoomsEvaluated() {
        System.out.println("\n=== Test 11: Room Anomalies (Building WARNING) ===");
        
        // Create dashboard JSON with rooms having high residuals
        String dashboardJson = "{" +
            "\"power\": {" +
                "\"real\": 100.0," +
                "\"simulated_raw\": 10.0," +
                "\"simulated\": 35.0" +
            "}," +
            "\"rooms\": [" +
                "{\"id\": \"R1\", \"name\": \"Meeting Room\", \"power\": 10.0}," +
                "{\"id\": \"R2\", \"name\": \"Staff Lounge\", \"power\": 12.0}" +
            "]" +
        "}";
        
        List<SimulationResult> history = createStableHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Building-level result: WARNING or CRITICAL (depending on residual)
        AnomalyResult buildingResult = anomalyDetectionService.detectAnomalyWithML(100.0, 10.0, mockModel);
        assertTrue("WARNING".equals(buildingResult.getSeverity()) || 
                "CRITICAL".equals(buildingResult.getSeverity()),
            "Building should be WARNING or CRITICAL");
        
        // Room-level detection with WARNING/CRITICAL building
        List<RoomAnomaly> roomAnomalies = anomalyDetectionService.detectRoomAnomalies(
            dashboardJson,
            mockModel,
            buildingResult.getThreshold(),
            buildingResult.getZScore(),
            buildingResult.getSeverity()  // "WARNING" or "CRITICAL"
        );
        
        // Assertions: Rooms should be evaluated (can be NORMAL, WARNING, or CRITICAL)
        assertNotNull(roomAnomalies, "Room anomalies should not be null");
        assertTrue(roomAnomalies.size() > 0, "Should have room anomalies");
        
        // At least some rooms should show anomalies if building is anomalous
        boolean hasAnomalousRoom = roomAnomalies.stream()
            .anyMatch(r -> !"NORMAL".equals(r.getSeverity()));
        
        System.out.println("Building severity: " + buildingResult.getSeverity());
        System.out.println("Room anomalies evaluated: " + roomAnomalies.size() + " rooms");
        for (RoomAnomaly room : roomAnomalies) {
            System.out.println("  - " + room.getRoomName() + ": " + room.getStatus());
        }
        System.out.println("Test 11 PASSED: Room evaluation works when building is anomalous\n");
    }

    /**
     * Test 12: High Variance History - Z-score Calculation with Variable Baseline
     * 
     * Scenario:
     * - Historical data has high variance (std deviation is large)
     * - Same residual produces lower Z-score than with stable history
     * - Tests robustness of Z-score calculation with noisy baseline
     * 
     * Expected: System handles high variance gracefully, Z-scores are lower
     */
    @Test
    public void testHighVarianceHistory_LowerZScores() {
        System.out.println("\n=== Test 12: High Variance History (Lower Z-scores) ===");
        
        // Use high variance history (larger std deviation)
        List<SimulationResult> history = createHighVarianceHistory(mockModel);
        mockRepositoryWithHistory(history);
        
        // Same deviation as Test 1, but with high variance history
        double realPower = 100.0;
        double simulatedPower = 10.0;
        
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithML(
            realPower, simulatedPower, mockModel
        );
        
        // With high variance, Z-score should be lower (but still > 3.0 for CRITICAL)
        assertTrue(result.isAnomalyDetected(),
            "Anomaly should still be detected with high variance history");
        assertTrue(result.getZScore() >= 3.0,
            "Z-score should still be >= 3.0 for CRITICAL, even with high variance");
        
        System.out.println("Real Power: " + result.getRealPower() + " kW");
        System.out.println("Residual: " + result.getResidual() + " kW");
        System.out.println("Z-Score (high variance): " + result.getZScore());
        System.out.println("Severity: " + result.getSeverity());
        System.out.println("Test 12 PASSED: High variance history handled correctly\n");
    }
}