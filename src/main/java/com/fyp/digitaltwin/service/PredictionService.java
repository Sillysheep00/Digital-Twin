package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.model.DataRecord;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import org.eclipse.epsilon.emc.emf.EmfModel;
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
    
    /**
     * Sets the current simulation step index (called by DigitalTwinEngine)
     */
    public void setCurrentStepIndex(int stepIndex) {
        this.currentStepIndex = stepIndex;
    }
    
    /**
     * Sets manual overrides (called by DigitalTwinEngine)
     */
    public void setManualOverrides(Map<String, String> overrides) {
        this.manualOverrides = overrides;
    }
    
    /**
     * Sets the live model for predictions (called by DigitalTwinEngine)
     */
    public void setLiveModel(EmfModel model) {
        this.liveModel = model;
    }
    
    /**
     * Sets the ML slope for predictions (called by DigitalTwinEngine)
     */
    public void setMlSlope(double slope) {
        this.mlSlope = slope;
        System.out.println("PredictionService: ML slope set to " + String.format("%.4f", slope));
    }
    
    /**
     * Sets the ML intercept for predictions (called by DigitalTwinEngine)
     */
    public void setMlIntercept(double intercept) {
        this.mlIntercept = intercept;
        System.out.println("PredictionService: ML intercept set to " + String.format("%.4f", intercept));
    }
    
    /**
     * Gets the current ML slope (for debugging and logging)
     */
    public double getMlSlope() {
        return mlSlope;
    }
    
    /**
     * Gets the current ML intercept (for debugging and logging)
     */
    public double getMlIntercept() {
        return mlIntercept;
    }
    
    /**
     * Predicts future energy consumption for the next N hours
     * Uses the current model state by cloning the live model
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
     * Predicts future energy consumption with step-by-step data for charting
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
                predictionModel = modelService.cloneModel(liveModel);
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
            for (SensorData mongoData : futureDataList) {
                // Convert to DTO
                DataRecord stepData = new DataRecord(
                    mongoData.getDate(),
                    mongoData.getPowerConsumption(),
                    mongoData.getOutdoorTemperature(),
                    mongoData.getOccupancy()
                );

                // Run physics simulation
                modelService.runEolScript(predictionModel, "hvac.eol", "Prediction", stepData, TIME_STEP_HOURS, manualOverrides, mlSlope, mlIntercept, null);
                
                // Calculate energy used in this 15-min window
                String jsonOutput = modelService.runEolScript(predictionModel, "json.eol", "PredictionAgg", stepData, TIME_STEP_HOURS, null, mlSlope, mlIntercept, null);
                JsonNode root = objectMapper.readTree(jsonOutput);
                double stepPower = root.path("power").path("simulated").asDouble();
                
                // Energy (kWh) = Power (kW) * Time (0.25h)
                double stepEnergy = stepPower * TIME_STEP_HOURS;
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
     * Predicts future energy consumption on a specific model (used for What-If scenarios)
     * 
     * @param model The modified model to run prediction on
     * @param hours Number of hours to predict
     * @return Map with predictedEnergy and hours, or error
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
            double totalEnergy = 0.0;
            for (int i = 0; i < stepsNeeded; i++) {
                SensorData data = futureData.get(i);
                double stepEnergy = runSimulationStepOnModel(model, data, currentStepIndex + i + 1);
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
     * @return Energy consumed in this step (kWh)
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
        
        // Run physics simulation
        modelService.runEolScript(model, "hvac.eol", "Scenario", stepData, TIME_STEP_HOURS, manualOverrides, mlSlope, mlIntercept, null);
        
        // Get energy consumption
        String jsonOutput = modelService.runEolScript(model, "json.eol", "ScenarioAgg", stepData, TIME_STEP_HOURS, null, mlSlope, mlIntercept, null);
        
        // Parse energy from result
        double totalEnergy = 0.0;
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonOutput);
            if (jsonNode.has("power") && jsonNode.get("power").has("simulated")) {
                double rawSimulated = jsonNode.get("power").get("simulated_raw").asDouble();
                double mlPredicted = jsonNode.get("power").get("simulated").asDouble();
                double stepPower = mlPredicted;
                totalEnergy = stepPower * TIME_STEP_HOURS;
                
                // Debug log to show dynamic power changes
                System.out.println(String.format(
                    "  📊 Step %d | Raw HVAC: %.2f kW → ML Predicted: %.2f kW (%.2fx + %.2f) → Energy: %.3f kWh",
                    stepIndex, rawSimulated, mlPredicted, mlSlope, mlIntercept, totalEnergy
                ));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse energy from JSON: " + e.getMessage());
        }
        
        return totalEnergy;
    }
    
    /**
     * Fetches a data record by index from the repository
     */
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

