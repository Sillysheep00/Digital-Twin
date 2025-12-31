package com.fyp.digitaltwin.service;

import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for training a Linear Regression model to predict real power
 * consumption from simulated power values.
 * 
 * MACHINE LEARNING APPROACH (Academic FYP Context):
 * ─────────────────────────────────────────────────
 * This service implements SIMPLE LINEAR REGRESSION, which qualifies as machine learning because:
 * 
 * 1. LEARNS FROM DATA: Uses historical (simulatedPower, realPower) pairs to learn parameters
 * 2. STATISTICAL OPTIMIZATION: Uses Least Squares method to minimize prediction error
 * 3. GENERALIZATION: Trained model can predict for new, unseen data points
 * 4. ADAPTIVE: Model coefficients reflect actual building behavior patterns
 * 
 * Mathematical Foundation:
 * ──────────────────────
 * We learn the linear relationship: realPower = a × simulatedPower + b
 * 
 * Where:
 * - a (slope): Scaling factor (similar to old calibration factor, but optimized)
 * - b (intercept): Base load offset (accounts for unmodeled constant loads)
 * 
 * Least Squares Solution:
 * ──────────────────────
 * Given n training samples (x₁, y₁), (x₂, y₂), ..., (xₙ, yₙ):
 * 
 * slope = (n×Σxy - Σx×Σy) / (n×Σx² - (Σx)²)
 * intercept = (Σy - slope×Σx) / n
 * 
 * Why This Is Better Than Simple Calibration Factor:
 * ──────────────────────────────────────────────────
 * Old: calibratedPower = simulatedPower × constantFactor
 * New: predictedPower = simulatedPower × slope + intercept
 * 
 * The intercept term captures constant base loads that the physics model doesn't simulate
 * (e.g., always-on equipment, phantom loads, network equipment).
 * 
 * Follows Single Responsibility Principle - only handles model training.
 */
@Service
public class RegressionTrainingService {
    
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
    
    /**
     * Trains a Linear Regression model using historical data.
     * 
     * TRAINING PROCESS:
     * ────────────────
     * 1. Extract building parameters from digital twin model
     * 2. Fetch training data (first 20% of dataset)
     * 3. For each sample, estimate simulated power (fast method, no thermal simulation)
     * 4. Collect (simulatedPower, realPower) training pairs
     * 5. Apply Least Squares method to compute optimal slope and intercept
     * 6. Calculate model quality metrics (R², RMSE)
     * 7. Return trained model
     * 
     * @param model The EMF model containing building configuration
     * @param totalDataCount Total number of records in database
     * @return Trained LinearRegressionModel with learned coefficients
     */
    public LinearRegressionModel trainModel(EmfModel model, long totalDataCount) {
        System.out.println("\n TRAINING LINEAR REGRESSION MODEL...");
        System.out.println("   Using Machine Learning approach for energy prediction");
        System.out.println("   Training set: First " + (TRAINING_SAMPLE_RATIO * 100) + "% of dataset");
        
        try {
            // ═══════════════════════════════════════════════════════════
            // STEP 1: Extract Building Parameters
            // ═══════════════════════════════════════════════════════════
            BuildingParameters params = extractBuildingParameters(model);
            
            System.out.println("   Building Configuration:");
            System.out.println("     - Capacity: " + params.totalCapacity + " people");
            System.out.println("     - Base Load: " + String.format("%.2f", params.totalBaseLoad) + " kW");
            System.out.println("     - HVAC Systems: " + params.hvacCount);
            
            // ═══════════════════════════════════════════════════════════
            // STEP 2: Fetch Training Data
            // ═══════════════════════════════════════════════════════════
            int trainingSampleSize = (int) (totalDataCount * TRAINING_SAMPLE_RATIO);
            List<SensorData> trainingData = repository.findAll(
                PageRequest.of(0, trainingSampleSize, Sort.by(Sort.Direction.ASC, "date"))
            ).getContent();
            
            System.out.println("   Loaded " + trainingData.size() + " training samples");
            
            // ═══════════════════════════════════════════════════════════
            // STEP 3: Collect Training Pairs (X, Y)
            // ═══════════════════════════════════════════════════════════
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
            System.out.println("   Valid training pairs: " + n);
            
            if (n < 10) {
                System.err.println("   ERROR: Insufficient training data (need at least 10 samples)");
                return new LinearRegressionModel();
            }
            
            // ═══════════════════════════════════════════════════════════
            // STEP 4: Compute Least Squares Solution
            // ═══════════════════════════════════════════════════════════
            // This is the MACHINE LEARNING step - we learn optimal parameters!
            
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
            
            System.out.println("\n   LEARNED PARAMETERS (via Least Squares):");
            System.out.println("     - Slope (a):     " + String.format("%.4f", slope));
            System.out.println("     - Intercept (b): " + String.format("%.4f", intercept) + " kW");
            
            // ═══════════════════════════════════════════════════════════
            // STEP 5: Calculate Model Quality Metrics
            // ═══════════════════════════════════════════════════════════
            
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
            
            System.out.println("\n   MODEL QUALITY METRICS:");
            System.out.println("     - R² Score:      " + String.format("%.4f", rSquared) + 
                               " (1.0 = perfect fit)");
            System.out.println("     - RMSE:          " + String.format("%.4f", rmse) + " kW");
            
            // ═══════════════════════════════════════════════════════════
            // STEP 6: Create and Return Trained Model
            // ═══════════════════════════════════════════════════════════
            LinearRegressionModel trainedModel = new LinearRegressionModel(
                slope,
                intercept,
                rSquared,
                rmse,
                n,
                LocalDateTime.now().toString()
            );
            
            System.out.println("\n    TRAINING COMPLETE!");
            System.out.println("   Model equation: realPower = " + 
                               String.format("%.4f", slope) + " × simulatedPower + " + 
                               String.format("%.4f", intercept));
            System.out.println("   (This model will now be used for anomaly detection)\n");
            
            return trainedModel;
            
        } catch (Exception e) {
            System.err.println("   Training failed: " + e.getMessage());
            e.printStackTrace();
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
}

