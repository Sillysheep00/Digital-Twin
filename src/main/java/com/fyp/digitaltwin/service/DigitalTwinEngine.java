package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.model.DataRecord;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.model.SimulationResult;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import com.fyp.digitaltwin.repository.SimulationResultRepository;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.emf.ecore.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DigitalTwinEngine {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinEngine.class);

    @Autowired
    private SensorDataRepository repository;

    @Autowired
    private SimulationResultRepository resultRepository;
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
    private PredictionService predictionService;
    
    @Autowired
    private WhatIfAnalysisService whatIfAnalysisService;
    
    @Autowired
    private RegressionTrainingService regressionTrainingService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Model fields
    private EmfModel smartOfficeModel;
    private int currentStepIndex = 0;
    private long totalDataCount = 0;
    private Map<String, String> manualOverrides = new HashMap<>();

    // Simulation Constants
    private static final double TIME_STEP_HOURS = 0.25;
    
    // Machine Learning Model fields
    private LinearRegressionModel regressionModel;
    private double mlSlope = 1.0;      
    private double mlIntercept = 0.0;  
    private boolean isCalibrated = false;

    private String simulationStartTime = null;
    
     //Getter for trained regression model (used by anomaly detection)
     public LinearRegressionModel getRegressionModel() {
        return regressionModel;
    }
    
    //Getter for ML slope (used by other services and for API)
    public double getMlSlope() {
        return mlSlope;
    }
    
    //Check if ML model has been trained
    public boolean isCalibrated() {
        return isCalibrated;
    }
    
    //Initialization
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Digital Twin Engine...");

            //1.Wipe DB to fix graph values
            resultRepository.deleteAll();
            log.info("Database cleared");

            //2. Load and Clone model
            EmfModel baseModel = modelService.loadBaseModel();
            Resource clonedResource = modelService.deepCloneModel(baseModel);
            this.smartOfficeModel = modelService.createEmfModelFromResource(clonedResource);

            //3. Clean up base model, only need the runtime clone
            baseModel.dispose();
            log.info("Runtime twin model created from base model (deep cloned, isolated)");

            //4.Check if MongoDB has data
            totalDataCount = repository.count();
            if (totalDataCount == 0) {
                log.info("MongoDB is empty. Importing data from CSV...");
                loadCsvToMongo("src/main/resources/cleandata.csv");
                totalDataCount = repository.count();
            }

            log.info("Digital Twin Engine initialized successfully!");
            log.info("Engine Ready. Using MongoDB with {} records.", totalDataCount);
            
            //5.Set live model reference in prediction service BEFORE training
            predictionService.setLiveModel(smartOfficeModel);
            
            //6.Train Linear Regression model 
            regressionModel = regressionTrainingService.trainModel(smartOfficeModel, totalDataCount);
            isCalibrated = regressionModel.isValid();
            
            //7.Extract ML model parameters
            mlSlope = regressionModel.getSlope();      
            mlIntercept = regressionModel.getIntercept(); 
            
            //8.Pass ML model parameters to services
            predictionService.setMlSlope(mlSlope);      
            predictionService.setMlIntercept(mlIntercept); 
            whatIfAnalysisService.setRegressionModel(regressionModel);  // Full ML model for What-If
            
            //9.Fast-forward initialization
            fastForwardInitialization(20); // Run 20 steps = 5 hours of simulation
            
            //10.Capture the start time
            if(totalDataCount > 0){
                DataRecord firstDate = fetchRecordByIndex(0);
                if(firstDate != null){
                    simulationStartTime = firstDate.getDate();
                }
            }
            
        } catch (Exception e) {
            log.error("Fatal Error: Could not start engine.", e);
        }
    }
    
    /**
     * Fast-forward simulation to build up realistic state quickly
     * Useful for demos to avoid waiting for scheduled simulation
     * Runs multiple simulation steps in rapid succession
     * 
     * @param steps Number of steps to fast-forward (4 steps = 1 hour)
     */
    private void fastForwardInitialization(int steps) {
        log.info("FAST-FORWARD MODE: Running {} simulation steps ({} hours of building operation)...",
                steps, (steps * 0.25));
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < steps; i++) {
            try {
                runSimulationStep();
                successCount++;
                
                // Show progress every 5 steps
                if ((i + 1) % 5 == 0) {
                    log.info("Fast-forward progress: {}/{} steps completed", (i + 1), steps);
                }
            } catch (Exception e) {
                log.warn("Fast-forward step {} failed: {}", (i + 1), e.getMessage());
                // Continue with remaining steps even if one fails
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("FAST-FORWARD COMPLETE! {}/{} steps in {}s. Current step: {}",
                successCount, steps, (duration / 1000.0), currentStepIndex);
    }

    //Simulation Loop
    /**
     * Main simulation loop - runs every 5 seconds
     * Simulates building behavior in real-time
     */
    @Scheduled(fixedRate = 5000)
    public synchronized void runSimulationStep() {
        if (smartOfficeModel == null || totalDataCount == 0) return;

        try {
            // 1. Handle Data Looping, restart when reaching end 
            if (currentStepIndex >= totalDataCount) {
                log.info("End of dataset. Restarting simulation...");
                currentStepIndex = 0;
            }

            // 2. Get current sensor from MongoDB
            DataRecord currentData = fetchRecordByIndex(currentStepIndex);

            if (currentData != null) {
                log.debug("Simulating Step {} | Date: {}", currentStepIndex, currentData.getDate());

                // 3. Run Physics Simulation (hvac.eol) with DATASET temperature
                // Note: Using historical outdoor temperature for reproducible simulation
                String physicsLog = modelService.runEolScript(
                    smartOfficeModel, 
                    "hvac.eol", 
                    "HVAC Physics", 
                    currentData, // Using dataset temperature for consistent physics
                    TIME_STEP_HOURS, 
                    manualOverrides,
                    mlSlope,
                    mlIntercept,
                    null  // Not needed for hvac.eol
                );
                if (!physicsLog.isBlank()) {
                    log.debug("Physics log: {}", physicsLog);
                }

                // 4. Save simulation snapshot to MongoDB
                saveSimulationSnapshot(currentData);
            }

            // 5. Move to next step
            currentStepIndex++;

        } catch (Exception e) {
            log.error("Error in simulation step: {}", e.getMessage(), e);
        }
    }

     // Saves current simulation state to MongoDB for dashboard and analysis
    private void saveSimulationSnapshot(DataRecord currentData) {
        try {
            // 1. Run json.eol to aggregate the data 
            String jsonOutput = modelService.runEolScript(
                smartOfficeModel, 
                "json.eol", 
                "JSON Aggregation", 
                currentData, 
                TIME_STEP_HOURS, 
                null,
                mlSlope,
                mlIntercept,
                simulationStartTime
            );

            log.debug("JSON output: {}", jsonOutput);
            
            // 2. Parse the JSON
            JsonNode root = objectMapper.readTree(jsonOutput);
            
            // 3. Extract values
            double realPower = root.path("power").path("real").asDouble();
            double simulatedPower = root.path("power").path("simulated_raw").asDouble();
            double simulatedPhysicsPower = root.path("power").path("simulated_physics_raw").asDouble();
            double totalEnergy = root.path("energy").path("total").asDouble();
            double outdoorTemp = root.path("environment").path("outdoorTemp").asDouble();
            double avgIndoorTemp = root.path("environment").path("avgIndoorTemp").asDouble();
            int activeHvacs = root.path("environment").path("activeHvacs").asInt();
            
            // 4. Create and save result
            SimulationResult result = new SimulationResult(
                currentData.getDate(),
                realPower,
                simulatedPower,
                totalEnergy,
                outdoorTemp,
                avgIndoorTemp,
                activeHvacs
            );
            result.setSimulatedPhysicsPower(simulatedPhysicsPower);
            
            resultRepository.save(result);
            
        } catch (Exception e) {
            log.error("Failed to save simulation snapshot: {}", e.getMessage(), e);
        }
    }
    
    // API Methods called by Controller
    // Gets live status for dashboard (calls query.eol)
    public String getLiveStatus() {
        try {
            int reportIndex = Math.max(0, currentStepIndex - 1);
            DataRecord reportData = fetchRecordByIndex(reportIndex);

            return modelService.runEolScript(
                smartOfficeModel, 
                "query.eol", 
                "Query", 
                reportData, 
                TIME_STEP_HOURS, 
                null,
                mlSlope,
                mlIntercept,
                null  // Not needed for query.eol
            );
        } catch (Exception e) {
            return "Error retrieving status: " + e.getMessage();
        }
    }

    //Gets dashboard data as JSON (calls json.eol)
    public String getDashboardData() {
        try {
            int dataIndex = Math.max(0, currentStepIndex - 1);
            DataRecord currentData = fetchRecordByIndex(dataIndex);

            return modelService.runEolScript(
                smartOfficeModel, 
                "json.eol", 
                "json", 
                currentData, 
                TIME_STEP_HOURS, 
                null,
                mlSlope,
                mlIntercept,
                simulationStartTime
            );

        } catch (Exception e) {
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    //Gets validation report (calls validation.evl)
    public String getValidationReport() {
        try {
            return modelService.runValidation(smartOfficeModel);
        } catch (Exception e) {
            return "Error running validation: " + e.getMessage();
        }
    }

     //Sets manual override for a room's HVAC (e.g., "COOL", "HEAT", "OFF", "AUTO")
    public void setOverride(String roomId, String action) {
        if (action.equals("AUTO")) {
            manualOverrides.remove(roomId);
            log.info("Removed override for {} -> AUTO mode", roomId);
        } else {
            manualOverrides.put(roomId, action);
            log.info("Set override: {} -> {}", roomId, action);
        }
    }
    
    // Prediction and What-If Analysis
    /**
     * Predicts future energy consumption (delegates to PredictionService)
     * 
     * @param hours Number of hours to predict
     * @return Prediction result with energy consumption
     */
    public Map<String, Double> predictFutureEnergy(int hours) {
        // Update prediction service with current state
        predictionService.setCurrentStepIndex(currentStepIndex);
        predictionService.setManualOverrides(new HashMap<>(manualOverrides));
        
        // Delegate to prediction service
        return predictionService.predictFutureEnergy(hours);
    }
    
    /**
     * Runs What-If analysis (delegates to WhatIfAnalysisService)
     * 
     * @param changes Parameters to change
     * @param hours Prediction horizon
     * @return Analysis result with savings calculations
     */
    public Map<String, Object> predictWithWhatIf(Map<String, Object> changes, int hours,Double investmentCost) {
        // Update prediction service with current state
        predictionService.setCurrentStepIndex(currentStepIndex);
        predictionService.setManualOverrides(new HashMap<>(manualOverrides));
        predictionService.setLiveModel(smartOfficeModel);
        
        // Delegate to what-if service
        return whatIfAnalysisService.runAnalysis(changes, hours,investmentCost);
    }
    
    /**
     * Gets the current live model state
     * Used by prediction services to clone from current state
     */
    public EmfModel getLiveModel() {
        return smartOfficeModel;
    }
    
   
    //Helper Methods
    //Fetches a data record by index from MongoDB
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
            log.error("Error fetching record at index {}: {}", index, e.getMessage());
            return null;
        }
    }

    //Loads CSV data into MongoDB (runs once on first startup)
    private void loadCsvToMongo(String csvPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean isHeader = true;
            
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                String[] columns = line.split(",");
                if (columns.length < 4) continue;
                
                String date = columns[0].trim();
                double powerConsumption = Double.parseDouble(columns[1].trim());
                double outdoorTemperature = Double.parseDouble(columns[2].trim());
                int occupancy = Integer.parseInt(columns[3].trim());
                
                SensorData sensorData = new SensorData(date, powerConsumption, outdoorTemperature, occupancy);
                repository.save(sensorData);
                
                count++;
                if (count % 1000 == 0) {
                    log.info("Imported {} records...", count);
                }
            }

            log.info("CSV Import Complete. Total: {} records.", count);
        } catch (Exception e) {
            log.error("CSV Import Failed: {}", e.getMessage(), e);
        }
    }
}
