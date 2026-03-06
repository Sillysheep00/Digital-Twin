package com.fyp.digitaltwin.service;

import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegressionTrainingService {

    private static final Logger log = LoggerFactory.getLogger(RegressionTrainingService.class);

    @Autowired
    private SensorDataRepository repository;
    
    @Autowired
    private ModelService modelService;
    
    // Training configuration
    private static final double TRAINING_SAMPLE_RATIO = 0.20; // Use first 20% of dataset
    
    // Power estimation constants (for fast training without full simulation)
    private static final double HVAC_POWER_PER_UNIT = 5.0;
    private static final double HVAC_DUTY_CYCLE = 0.35;
    private static final double STANDBY_RATIO = 0.1;
    
    // Baseline metrics storage (for API access)
    private double baselineRSquared = 0.0;
    private double baselineRMSE = 0.0;
    private double rSquaredDifference = 0.0;
    private double rmseDifference = 0.0;
    private double rmseImprovementPercent = 0.0;
    
    /**
     * Trains a Linear Regression model using historical data.
     * 
     * @param model The EMF model containing building configuration
     * @param totalDataCount Total number of records in database
     * @return Trained LinearRegressionModel with learned coefficients
     */
    public LinearRegressionModel trainModel(EmfModel model, long totalDataCount) {
        log.info("TRAINING LINEAR REGRESSION MODEL...");
        log.info("Using Machine Learning approach for energy prediction");
        log.info("Training set: First {}% of dataset", (TRAINING_SAMPLE_RATIO * 100));
        
        try {
            // STEP 1: Extract Building Parameters
            BuildingParameters params = extractBuildingParameters(model);
            
            log.info("Building Configuration: Capacity={} people, BaseLoad={} kW, HVAC Systems={}",
                    params.totalCapacity, String.format("%.2f", params.totalBaseLoad), params.hvacCount);
            
           
            // STEP 2: Fetch Training Data
            int trainingSampleSize = (int) (totalDataCount * TRAINING_SAMPLE_RATIO);
            List<SensorData> trainingData = repository.findAll(
                PageRequest.of(0, trainingSampleSize, Sort.by(Sort.Direction.ASC, "date"))
            ).getContent();
            
            log.info("Loaded {} training samples", trainingData.size());
            
            // STEP 3: Collect Training Pairs (X, Y)
            List<Double> X = new ArrayList<>(); // simulatedPower (independent variable)
            List<Double> Y = new ArrayList<>(); // realPower (dependent variable)
            
            for (SensorData mongoData : trainingData) {
                double realPower = mongoData.getPowerConsumption();
                double occupancy = mongoData.getOccupancy();
                
                if (realPower > 0) {
                    // Fast power estimation (no thermal simulation needed for training)
                    double estimatedHvacPower = params.hvacCount * HVAC_POWER_PER_UNIT * HVAC_DUTY_CYCLE;
                    double estimatedPlugPower = (occupancy > 0) 
                        ? params.totalBaseLoad 
                        : params.totalBaseLoad * STANDBY_RATIO;
                    double simulatedPower = estimatedHvacPower + estimatedPlugPower;
                    
                    X.add(simulatedPower);
                    Y.add(realPower);
                }
            }
            
            int n = X.size();
            log.info("Valid training pairs: {}", n);

            if (n < 10) {
                log.error("Insufficient training data (need at least 10 samples)");
                return new LinearRegressionModel();
            }
            
            // STEP 4: Compute Least Squares Solution (to get optimal parameters)
            double sumX = 0.0;
            double sumY = 0.0;
            double sumXY = 0.0;
            double sumX2 = 0.0;
            
            for (int i = 0; i < n; i++) {
                double x = X.get(i);
                double y = Y.get(i);
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }
            
            // Least Squares formulas:
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            
            log.info("Learned Parameters - Slope: {}, Intercept: {} kW",
                    String.format("%.4f", slope), String.format("%.4f", intercept));
            
            // STEP 5: Calculate Model Quality Metrics
            // R² (Coefficient of Determination): measures how well model explains variance
            double meanY = sumY / n;
            double ssTotal = 0.0;  // Total sum of squares
            double ssResidual = 0.0;  // Residual sum of squares
            
            for (int i = 0; i < n; i++) {
                double yTrue = Y.get(i);
                double yPred = slope * X.get(i) + intercept;
                ssTotal += Math.pow(yTrue - meanY, 2);
                ssResidual += Math.pow(yTrue - yPred, 2);
            }
            
            double rSquared = 1.0 - (ssResidual / ssTotal);
            
            // RMSE (Root Mean Squared Error): average prediction error
            double rmse = Math.sqrt(ssResidual / n);
            
            log.info("Model Quality Metrics - R²: {} (1.0=perfect fit), RMSE: {} kW",
                    String.format("%.4f", rSquared), String.format("%.4f", rmse));
            
            // STEP 5.5: Calculate Baseline Metrics (NO CALIBRATION)
            // Baseline prediction: directly use simulatedPower (no slope/intercept adjustment)
            double ssResidualBaseline = 0.0;
            for (int i = 0; i < n; i++) {
                double yTrue = Y.get(i);
                double yPredBaseline = X.get(i);  // Baseline: simulatedPower directly
                ssResidualBaseline += Math.pow(yTrue - yPredBaseline, 2);
            }
            
            double rSquaredBaseline = 1.0 - (ssResidualBaseline / ssTotal);
            double rmseBaseline = Math.sqrt(ssResidualBaseline / n);
            
            // Calculate differences
            double rSquaredDifference = rSquared - rSquaredBaseline;
            double rmseDifference = rmseBaseline - rmse;  // Positive = improvement (lower RMSE is better)
            double rmseImprovementPercent = 0.0;
            if (rmseBaseline > 0) {
                rmseImprovementPercent = (rmseDifference / rmseBaseline) * 100.0;
            }
            
            // Store baseline metrics for API access
            this.baselineRSquared = rSquaredBaseline;
            this.baselineRMSE = rmseBaseline;
            this.rSquaredDifference = rSquaredDifference;
            this.rmseDifference = rmseDifference;
            this.rmseImprovementPercent = rmseImprovementPercent;
            
            System.out.println("===== COMPARISON: BASELINE vs CALIBRATED =====");
            log.info("Baseline (No Calibration) - R²: {}, RMSE: {} kW",
                    String.format("%.4f", rSquaredBaseline), String.format("%.4f", rmseBaseline));
            log.info("Calibrated (With ML Model) - R²: {}, RMSE: {} kW",
                    String.format("%.4f", rSquared), String.format("%.4f", rmse));
            log.info("Improvement - R² diff: {} {}, RMSE diff: {} kW {}",
                    String.format("%.4f", rSquaredDifference),
                    (rSquaredDifference > 0 ? "(improvement)" : "(no improvement)"),
                    String.format("%.4f", rmseDifference),
                    (rmseDifference > 0 ? "(reduction)" : "(increase)"));
            if (rmseBaseline > 0) {
                log.info("RMSE Improvement: {}%", String.format("%.2f", rmseImprovementPercent));
            }
            System.out.println("   ================================================\n");
            
            // STEP 6: Create and Return Trained Model
            LinearRegressionModel trainedModel = new LinearRegressionModel(
                slope,
                intercept,
                rSquared,
                rmse,
                n,
                LocalDateTime.now().toString()
            );
            
            log.info("TRAINING COMPLETE - Model: realPower = {} x simulatedPower + {} (used for anomaly detection)",
                    String.format("%.4f", slope), String.format("%.4f", intercept));
            
            return trainedModel;
            
        } catch (Exception e) {
            log.error("Training failed: {}", e.getMessage(), e);
            return new LinearRegressionModel();
        }
    }
    
    /**
     * Extract building parameters from the EMF model.
     * 
     * @param model The digital twin model
     * @return BuildingParameters containing capacity, base load, and HVAC count
     * @throws Exception if model query fails
     */
    private BuildingParameters extractBuildingParameters(EmfModel model) throws Exception {
        String queryEol = 
            "var allRooms = SmartOffice!Room.all;\n" +
            "var totalCapacity = allRooms.collect(r | r.capacity).sum();\n" +
            "var totalBaseLoad = allRooms.collect(r | r.baseLoad).sum();\n" +
            "var hvacCount = SmartOffice!HVACSystem.all.size();\n" +
            "return totalCapacity + ',' + totalBaseLoad + ',' + hvacCount;";
        
        String result = modelService.runSimpleEolScript(model, queryEol);
        String[] parts = result.split(",");
        
        BuildingParameters params = new BuildingParameters();
        params.totalCapacity = Double.parseDouble(parts[0].trim());
        params.totalBaseLoad = Double.parseDouble(parts[1].trim());
        params.hvacCount = Integer.parseInt(parts[2].trim());
        
        return params;
    }
    
    /**
     * Data class to hold building configuration parameters.
     */
    private static class BuildingParameters {
        double totalCapacity;
        double totalBaseLoad;
        int hvacCount;
    }
    
    /**
     * Get baseline metrics for API access
     */
    public double getBaselineRSquared() {
        return baselineRSquared;
    }
    
    public double getBaselineRMSE() {
        return baselineRMSE;
    }
    
    public double getRSquaredDifference() {
        return rSquaredDifference;
    }
    
    public double getRmseDifference() {
        return rmseDifference;
    }
    
    public double getRmseImprovementPercent() {
        return rmseImprovementPercent;
    }
}

