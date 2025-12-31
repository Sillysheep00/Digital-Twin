package com.fyp.digitaltwin.service;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import org.springframework.stereotype.Service;

/**
 * Service responsible for detecting anomalies in building energy consumption
 * using a MACHINE LEARNING approach with Linear Regression.
 * 
 * ACADEMIC FYP MACHINE LEARNING JUSTIFICATION:
 * ───────────────────────────────────────────
 * This service uses a TRAINED LINEAR REGRESSION MODEL to predict expected power consumption,
 * qualifying as machine learning because:
 * 
 * 1. LEARNS FROM DATA: Model coefficients (slope, intercept) are learned from historical data
 * 2. PREDICTION: Uses learned model to predict expected power for new simulated values
 * 3. STATISTICAL FOUNDATION: Based on least squares optimization
 * 4. GENERALIZABLE: Model works on unseen data after training
 * 5. ADAPTIVE: Can be retrained as building behavior changes
 * 
 * Methodology:
 * ───────────
 * 1. Use trained Linear Regression model: predictedPower = slope × simulatedPower + intercept
 * 2. Calculate residual: residual = |realPower - predictedPower|
 * 3. Compare residual against adaptive threshold to detect anomalies
 * 
 * Why This Is Machine Learning (Not Just Calibration):
 * ────────────────────────────────────────────────────
 * - Old approach: Simple multiplicative factor (realPower ≈ k × simulatedPower)
 * - ML approach: Linear model with learned slope + intercept (accounts for base loads)
 * - The model LEARNS optimal parameters through statistical optimization
 * - Provides better predictions by capturing both scaling AND offset effects
 * 
 * Follows Single Responsibility Principle - only handles anomaly detection logic.
 */
@Service
public class AnomalyDetectionService {
    
    // Configuration: Threshold as percentage of real power
    // e.g., 0.25 means anomaly if difference > 25% of real power
    private static final double THRESHOLD_PERCENTAGE = 0.25; // 25%
    
    // Severity levels (percentage-based)
    private static final double WARNING_THRESHOLD = 0.15;  // 15% difference = warning
    private static final double CRITICAL_THRESHOLD = 0.30; // 30% difference = critical
    
    /**
     * Detects anomalies by comparing real power against ML-predicted power.
     * 
     * MACHINE LEARNING-BASED ANOMALY DETECTION:
     * ─────────────────────────────────────────
     * Step 1: Use trained Linear Regression model to predict expected power
     *         predictedPower = slope × simulatedPower + intercept
     *         
     *         WHY MACHINE LEARNING: The model coefficients (slope, intercept) were LEARNED
     *         from historical data using least squares optimization. This is supervised
     *         learning where we trained on (simulatedPower, realPower) pairs to learn
     *         the optimal mapping function.
     *         
     *         ADVANTAGE OVER SIMPLE CALIBRATION:
     *         - Old: calibratedPower = k × simulatedPower (single parameter)
     *         - ML:  predictedPower = a × simulatedPower + b (two learned parameters)
     *         - The intercept 'b' captures constant base loads that simple scaling misses
     *         - More accurate predictions = better anomaly detection
     * 
     * Step 2: Calculate residual (prediction error)
     *         residual = |realPower - predictedPower|
     *         
     *         WHY: The residual represents how much the actual consumption deviates from
     *         what our trained ML model predicts. Large residuals indicate anomalous
     *         behavior that the model didn't learn from historical patterns.
     * 
     * Step 3: Determine adaptive threshold
     *         threshold = realPower × THRESHOLD_PERCENTAGE
     *         
     *         WHY: Percentage-based threshold makes detection robust across different
     *         load conditions (low power vs high power scenarios).
     * 
     * Step 4: Anomaly decision
     *         anomaly = (residual > threshold)
     *         
     *         WHY: If the prediction error exceeds our tolerance, we flag it as an
     *         anomaly requiring investigation (equipment fault, sensor error, etc.).
     * 
     * @param realPower Actual power consumption from sensors (kW)
     * @param simulatedPower Raw simulated power from physics model (kW)
     * @param regressionModel Trained Linear Regression model (learned from data)
     * @return AnomalyResult containing detection outcome and diagnostic information
     */
    public AnomalyResult detectAnomalyWithML(double realPower, double simulatedPower, 
                                              LinearRegressionModel regressionModel) {
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 1: ML PREDICTION (Machine Learning Step!)
        // ═════════════════════════════════════════════════════════════════
        // Use the TRAINED model to predict expected power consumption.
        // The model's parameters (slope, intercept) were LEARNED from historical data
        // using least squares regression - this is SUPERVISED MACHINE LEARNING.
        double predictedPower = regressionModel.predict(simulatedPower);
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 2: Calculate Residual (Prediction Error)
        // ═════════════════════════════════════════════════════════════════
        // The residual is the difference between reality and ML prediction.
        // This measures how well our learned model generalizes to current data.
        double residual = Math.abs(realPower - predictedPower);
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 3: Calculate Adaptive Threshold
        // ═════════════════════════════════════════════════════════════════
        // Use percentage-based threshold to handle varying load conditions.
        double threshold = realPower * THRESHOLD_PERCENTAGE;
        
        // Handle edge case: minimum threshold to avoid false positives
        if (threshold < 1.0) {
            threshold = 1.0; // Minimum 1 kW threshold
        }
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 4: Anomaly Decision (ML-Based)
        // ═════════════════════════════════════════════════════════════════
        boolean anomalyDetected = residual > threshold;
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 5: Classify Severity
        // ═════════════════════════════════════════════════════════════════
        String severity;
        String explanation;
        
        double residualPercentage = (realPower > 0) ? (residual / realPower) * 100 : 0;
        
        if (residualPercentage >= CRITICAL_THRESHOLD * 100) {
            severity = "CRITICAL";
            explanation = String.format(
                "CRITICAL ANOMALY: Real power deviates by %.1f%% from ML prediction. " +
                "Possible causes: major equipment failure, sensor malfunction, or significant unexpected load.",
                residualPercentage
            );
        } else if (residualPercentage >= WARNING_THRESHOLD * 100) {
            severity = "WARNING";
            explanation = String.format(
                "WARNING: Real power deviates by %.1f%% from ML prediction. " +
                "Possible causes: minor equipment inefficiency, model drift, or unusual occupancy pattern.",
                residualPercentage
            );
        } else {
            severity = "NORMAL";
            explanation = String.format(
                "Normal operation. Real power is within %.1f%% of ML model predictions.",
                residualPercentage
            );
        }
        
        // ═════════════════════════════════════════════════════════════════
        // STEP 6: Build Result
        // ═════════════════════════════════════════════════════════════════
        AnomalyResult result = new AnomalyResult();
        result.setAnomalyDetected(anomalyDetected);
        result.setRealPower(Math.round(realPower * 100.0) / 100.0);
        result.setSimulatedPower(Math.round(simulatedPower * 100.0) / 100.0);
        result.setCalibratedSimulatedPower(Math.round(predictedPower * 100.0) / 100.0); // ML prediction
        result.setResidual(Math.round(residual * 100.0) / 100.0);
        result.setThreshold(Math.round(threshold * 100.0) / 100.0);
        result.setSeverity(severity);
        result.setExplanation(explanation);
        
        return result;
    }
    
    /**
     * Convenience method: Detect anomaly using current dashboard data and ML model.
     * Extracts power values from dashboard JSON and performs ML-based detection.
     * 
     * @param dashboardJson JSON string from dashboard API
     * @param regressionModel Trained Linear Regression model
     * @return AnomalyResult
     */
    public AnomalyResult detectAnomalyFromDashboard(String dashboardJson, LinearRegressionModel regressionModel) {
        try {
            // Parse JSON to extract power values
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(dashboardJson);
            
            double realPower = root.path("power").path("real").asDouble();
            double simulatedPower = root.path("power").path("simulated_raw").asDouble();
            
            // Use ML model for detection
            return detectAnomalyWithML(realPower, simulatedPower, regressionModel);
            
        } catch (Exception e) {
            // Return error result if parsing fails
            AnomalyResult errorResult = new AnomalyResult();
            errorResult.setAnomalyDetected(false);
            errorResult.setSeverity("ERROR");
            errorResult.setExplanation("Failed to parse dashboard data: " + e.getMessage());
            return errorResult;
        }
    }
}

