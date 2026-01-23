package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.model.DataRecord;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for energy prediction logic.
 * Handles future energy consumption forecasting using the digital twin model.
 * 
 * Part of the refactored Service Layer Architecture.
 */
@Service
public class PredictionService {
    
    private static final double TIME_STEP_HOURS = 0.25; // 15 minutes
    
    @Autowired
    private SensorDataRepository repository;
    
    @Autowired
    private ModelService modelService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Injected from DigitalTwinEngine
    private int currentStepIndex = 0;
    private Map<String, String> manualOverrides = new HashMap<>();
    private EmfModel liveModel = null;
    private double mlSlope = 1.0;       // ML slope (a) - learned regression parameter
    private double mlIntercept = 0.0;   // ML intercept (b) - learned regression parameter

    
     //Sets the current simulation step index (called by DigitalTwinEngine)
    public void setCurrentStepIndex(int stepIndex) {
        this.currentStepIndex = stepIndex;
    }

     //Sets manual overrides (called by DigitalTwinEngine)
    public void setManualOverrides(Map<String, String> overrides) {
        this.manualOverrides = overrides;
    }

    //Sets the live model for predictions (called by DigitalTwinEngine)
    public void setLiveModel(EmfModel model) {
        this.liveModel = model;
    }
    
    //Sets the ML slope for predictions (called by DigitalTwinEngine)
    public void setMlSlope(double slope) {
        this.mlSlope = slope;
        System.out.println("PredictionService: ML slope set to " + String.format("%.4f", slope));
    }

    public EmfModel getLiveModel() {
        return liveModel;
    }
    
    //Sets the ML intercept for predictions (called by DigitalTwinEngine)
    public void setMlIntercept(double intercept) {
        this.mlIntercept = intercept;
        System.out.println("PredictionService: ML intercept set to " + String.format("%.4f", intercept));
    }
    
    //Gets the current ML slope (for debugging and logging)
    public double getMlSlope() {
        return mlSlope;
    }
    
    //Gets the current ML intercept (for debugging and logging)
    public double getMlIntercept() {
        return mlIntercept;
    }
    
    /**
     * Predicts future energy consumption for the next N hours (simplified API for backward compatibility)
     * Uses the current model state by cloning the live model
     * 
     * This is a wrapper method that delegates to {@link #predictFutureEnergyWithSteps(int)} and
     * converts the result to a simpler format without step-by-step data 
     * 
     * @param hoursToPredict Number of hours to predict
     * @return Map with predictedEnergy and hours, or error
     */
    public synchronized Map<String, Double> predictFutureEnergy(int hoursToPredict) {
        Map<String, Object> detailedResult = predictFutureEnergyWithSteps(hoursToPredict);

        // Convert to Map<String, Double> for backward compatibility
        Map<String, Double> result = new HashMap<>();
        result.put("predictedEnergy", (Double) detailedResult.get("predictedEnergy"));
        result.put("hours", (Double) detailedResult.get("hours"));
        if (detailedResult.containsKey("error")) {
            result.put("error", (Double) detailedResult.get("error"));
        }
        return result;
    }
    
    /**
     * Predicts future energy consumption with step-by-step data for charting (called by WhatIfAnalysisService.java)
     * Uses the current model state by cloning the live model
     * 
     * @param hoursToPredict Number of hours to predict
     * @return Map with predictedEnergy, stepEnergyList, hours, or error
     */
    public synchronized Map<String, Object> predictFutureEnergyWithSteps(int hoursToPredict) {
        System.out.println("Running prediction for next " + hoursToPredict + " hours...");
        double totalPredictedEnergy = 0.0;
        List<Double> stepEnergyList = new ArrayList<>();
        
        try {
            // 1. Clone the live model to preserve current state
            EmfModel predictionModel;
            if (liveModel != null) {
                Resource clonedResource = modelService.deepCloneModel(liveModel);
                predictionModel = modelService.createEmfModelFromResource(clonedResource);
            } else {
                // Fallback: load fresh model if live model not available
                predictionModel = modelService.loadBaseModel();
            }
            predictionModel.setName("SmartOffice");
            // Reset energy meters for prediction timeline (separate from historical timeline)
            try {
                // Ensure model is loaded
                if (!predictionModel.isLoaded()) {
                    predictionModel.load();
                }
    
                String resetScript = 
                    "for (s in SmartOffice!EnergyMeter.all) {\n" +
                    "    s.energyConsumed = 0.0d;\n" +
                    "}\n";
                modelService.runSimpleEolScript(predictionModel, resetScript);
                System.out.println("   Reset energy meters for prediction (starting from 0 kWh)");
            } catch (Exception e) {
                System.err.println("Warning: Failed to reset energy meters: " + e.getMessage());
                e.printStackTrace();
            }

            // 2. Prepare future data
            int stepsNeeded = hoursToPredict * 4; // 4 steps per hour (15 min intervals)
            
            // 3. Get current date from current simulation step
            DataRecord currentRecord = fetchRecordByIndex(currentStepIndex);
            if (currentRecord == null) {
                 Map<String, Object> errorResult = new HashMap<>();
                 errorResult.put("error", 0.0);
                 return errorResult;
            }
            String currentDate = currentRecord.getDate();

            // 4. Fetch FUTURE records starting from this date
            List<SensorData> futureDataList = repository.findByDateGreaterThan(
                currentDate, 
                PageRequest.of(0, stepsNeeded, Sort.by(Sort.Direction.ASC, "date"))
            );

            System.out.println("   Found " + futureDataList.size() + " future records for prediction.");

            // 5. Run fast-forward simulation and collect step data
            // Track cumulative energy to calculate step-by-step energy (energy.total is cumulative)
            double previousCumulativeEnergy = 0.0;
            
            for (int stepIndex = 0; stepIndex < futureDataList.size(); stepIndex++) {
                SensorData mongoData = futureDataList.get(stepIndex);
                
                // Convert to DTO
                DataRecord stepData = new DataRecord(
                    mongoData.getDate(),
                    mongoData.getPowerConsumption(),
                    mongoData.getOutdoorTemperature(),
                    mongoData.getOccupancy()
                );

                // Run physics simulation (this updates energy sensors based on actual HVAC usage)
                modelService.runEolScript(predictionModel, "hvac.eol", "Prediction", stepData, TIME_STEP_HOURS, manualOverrides, mlSlope, mlIntercept, null);
                
                // Get energy aggregation (includes actual accumulated energy from sensors)
                String jsonOutput = modelService.runEolScript(predictionModel, "json.eol", "PredictionAgg", stepData, TIME_STEP_HOURS, null, mlSlope, mlIntercept, null);

                // Validate JSON output before parsing
                if (jsonOutput == null || jsonOutput.trim().isEmpty() || 
                (!jsonOutput.trim().startsWith("{") && !jsonOutput.trim().startsWith("["))) {
                System.err.println("Warning: Invalid JSON output from json.eol, skipping step. Output: " + 
                                (jsonOutput != null ? jsonOutput.substring(0, Math.min(100, jsonOutput.length())) : "null"));

                // Skip this step and continue with next (keep previous cumulative energy unchanged)
                stepEnergyList.add(0.0); //Add 0 instead of crashing 
                continue;
                }
             
                JsonNode root;
                try {
                    root = objectMapper.readTree(jsonOutput);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to parse JSON from json.eol: " + e.getMessage());
                    System.err.println("JSON output (first 200 chars): " + 
                                    (jsonOutput.length() > 200 ? jsonOutput.substring(0, 200) : jsonOutput));
                    // Skip this step and continue with next (keep previous cumulative energy unchanged)
                    stepEnergyList.add(0.0);
                    continue;
                }
                
                // Use ACTUAL accumulated energy from sensors (reflects insulation/targetTemp changes)
                // Instead of fast estimation power calculation
                double currentCumulativeEnergy = root.path("energy").path("total").asDouble();
                
                // Energy for this step = current cumulative - previous cumulative
                double stepEnergy = currentCumulativeEnergy - previousCumulativeEnergy;
                
                // Handle first step (should be 0 or very small after reset, but use directly if negative)
                if (stepIndex == 0 && stepEnergy < 0) {
                    stepEnergy = currentCumulativeEnergy;  // Use total directly for first step
                }
                
                // Update for next step
                previousCumulativeEnergy = currentCumulativeEnergy;
                
                totalPredictedEnergy += stepEnergy;
                stepEnergyList.add(Math.round(stepEnergy * 100.0) / 100.0);
            }
            
            // 6. Clean up
            predictionModel.dispose();

            System.out.println("Prediction Complete. Est. Energy: " + totalPredictedEnergy + " kWh");

            // 7. Return result map with step data
            Map<String, Object> result = new HashMap<>();
            result.put("predictedEnergy", Math.round(totalPredictedEnergy * 100.0) / 100.0);
            result.put("hours", (double) hoursToPredict);
            result.put("stepEnergyList", stepEnergyList);
            result.put("currentDate", currentDate);

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", -1.0);
            return errorResult;
        }
    }
    
    /**
     * Predicts future energy consumption on a specific model (simplified API for backward compability)
     * 
     * This is a wrappper method that delegates to {@link #predictOnModelWithSteps(EmfModel, int)} and 
     * converts the result to a simpler format without step-by-step data.
     * 
     * @param model The modified model to run prediction on
     * @param hours Number of hours to predict
     * @return Map with predictedEnergy and hours, or error
     *      
     */
    public synchronized Map<String, Double> predictOnModel(EmfModel model, int hours) {
        Map<String, Object> detailedResult = predictOnModelWithSteps(model, hours);
        // Convert to Map<String, Double> for backward compatibility
        Map<String, Double> result = new HashMap<>();
        if (detailedResult.containsKey("predictedEnergy")) {
            result.put("predictedEnergy", (Double) detailedResult.get("predictedEnergy"));
        }
        if (detailedResult.containsKey("steps")) {
            result.put("steps", (Double) detailedResult.get("steps"));
        }
        if (detailedResult.containsKey("hours")) {
            result.put("hours", (Double) detailedResult.get("hours"));
        }
        if (detailedResult.containsKey("error")) {
            result.put("error", (Double) detailedResult.get("error"));
        }
        return result;
    }
    
    /**
     * Runs a prediction on a specific model with step-by-step data (for What-If scenarios with charting)
     * This allows testing different scenarios without affecting the live model
     * 
     * The provided model is assumed to be an isolated scenario clone.
     * This method will mutate the model state during simulation.
     * 
     * @param model The model to predict on
     * @param hours Prediction horizon in hours
     * @return Prediction result with stepEnergyList
     */
    public synchronized Map<String, Object> predictOnModelWithSteps(EmfModel model, int hours) {
        Map<String, Object> result = new HashMap<>();
        List<Double> stepEnergyList = new ArrayList<>();
        
        try{
            int stepsNeeded = hours * 4; // 4 steps per hour (15-min intervals)

            // Reset energy meters for prediction timeline (separate from historical timeline)
            // This ensures energy report only shows energy for the prediction window
            try {
                String resetScript = 
                    "for (s in SmartOffice!EnergyMeter.all) {\n" +
                    "    s.energyConsumed = 0.0d;\n" +
                    "}\n";
                modelService.runSimpleEolScript(model, resetScript);
                System.out.println("   Reset energy meters for What-If prediction (starting from 0 kWh)");
            } catch (Exception e) {
                System.err.println("Warning: Failed to reset energy meters: " + e.getMessage());
            }
            
            // Get current date from current simulation step
            DataRecord currentRecord = fetchRecordByIndex(currentStepIndex);
            if (currentRecord == null) {
                result.put("error", 1.0);
                result.put("message", 404.0);
                return result;
            }
            String currentDate = currentRecord.getDate();
            
            // Fetch future data using the same method as predictFutureEnergy
            List<SensorData> futureData = repository.findByDateGreaterThan(
                currentDate, 
                PageRequest.of(0, stepsNeeded, Sort.by(Sort.Direction.ASC, "date"))
            );
            
            if (futureData.size() < stepsNeeded) {
                result.put("error", 1.0);
                result.put("availableSteps", (double) futureData.size());
                result.put("requestedSteps", (double) stepsNeeded);
                return result;
            }
            
            // Run simulation steps on the modified model and collect step data
            // Track cumulative energy to calculate step-by-step energy (energy.total is cumulative)
            double totalEnergy = 0.0;
            double previousCumulativeEnergy = 0.0;
            
            for (int i = 0; i < stepsNeeded; i++) {
                SensorData data = futureData.get(i);
                // Method returns current cumulative energy, we calculate step energy
                double currentCumulativeEnergy = runSimulationStepOnModel(model, data, currentStepIndex + i + 1);
                
                // Energy for this step = current cumulative - previous cumulative
                double stepEnergy = currentCumulativeEnergy - previousCumulativeEnergy;
                
                // Handle first step (should be 0 or very small after reset, but use directly if negative)
                if (i == 0 && stepEnergy < 0) {
                    stepEnergy = currentCumulativeEnergy;  // Use total directly for first step
                }
                
                // Update cumulative energy for next step
                previousCumulativeEnergy = currentCumulativeEnergy;
                
                totalEnergy += stepEnergy;
                stepEnergyList.add(Math.round(stepEnergy * 100.0) / 100.0);
            }
            
            result.put("predictedEnergy", Math.round(totalEnergy * 100.0) / 100.0);
            result.put("steps", (double) stepsNeeded);
            result.put("hours", (double) hours);
            result.put("stepEnergyList", stepEnergyList);
            
        } catch (Exception e) {
            System.err.println("Prediction on model failed: " + e.getMessage());
            e.printStackTrace();
            result.put("error", 1.0);
        }
        return result;
    }
    
    /**
     * Runs a single simulation step on a specific model (for What-If scenarios)
     * 
     * @param model The model to simulate on
     * @param data Sensor data for this step
     * @param stepIndex Current step index
     * @return Current cumulative energy from sensors (kWh) - caller calculates step energy
     * @throws Exception if simulation fails
     */
    private double runSimulationStepOnModel(EmfModel model, SensorData data, int stepIndex) throws Exception {
        // Convert SensorData to DataRecord
        DataRecord stepData = new DataRecord(
            data.getDate(),
            data.getPowerConsumption(),
            data.getOutdoorTemperature(),
            data.getOccupancy()
        );
        
        // Run physics simulation (this updates energy sensors based on actual HVAC usage)
        modelService.runEolScript(model, "hvac.eol", "Scenario", stepData, TIME_STEP_HOURS, manualOverrides, mlSlope, mlIntercept, null);
        
        // Get energy aggregation (includes actual accumulated energy from sensors)
        String jsonOutput = modelService.runEolScript(model, "json.eol", "ScenarioAgg", stepData, TIME_STEP_HOURS, null, mlSlope, mlIntercept, null);
        
        // Parse and return current cumulative energy from sensors
        double currentCumulativeEnergy = 0.0;
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonOutput);
            
            // Use actual accumulated energy from sensors (reflects insulation/targetTemp changes)
            currentCumulativeEnergy = jsonNode.path("energy").path("total").asDouble();
            
            // Debug log to show actual energy changes
            if (jsonNode.has("power") && jsonNode.get("power").has("simulated")) {
                double rawSimulated = jsonNode.get("power").get("simulated_raw").asDouble();
                double mlPredicted = jsonNode.get("power").get("simulated").asDouble();
                System.out.println(String.format(
                    "  📊 Step %d | Raw HVAC: %.2f kW → ML Predicted: %.2f kW | Cumulative Energy: %.3f kWh (from sensors)",
                    stepIndex, rawSimulated, mlPredicted, currentCumulativeEnergy
                ));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse energy from JSON: " + e.getMessage());
        }
        
        return currentCumulativeEnergy;
    }
    
    
    //Helper Method
    //Fetches a data record by index from the repository
    private DataRecord fetchRecordByIndex(int index) {
        try {
            PageRequest pageRequest = PageRequest.of(index, 1, Sort.by(Sort.Direction.ASC, "date"));
            List<SensorData> results = repository.findAll(pageRequest).getContent();
            
            if (results.isEmpty()) {
                return null;
            }
            
            SensorData data = results.get(0);
            return new DataRecord(
                data.getDate(),
                data.getPowerConsumption(),
                data.getOutdoorTemperature(),
                data.getOccupancy()
            );
        } catch (Exception e) {
            System.err.println("Error fetching record at index " + index + ": " + e.getMessage());
            return null;
        }
    }
}

