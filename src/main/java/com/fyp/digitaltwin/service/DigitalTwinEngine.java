package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.model.DataRecord;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.model.SimulationResult;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import com.fyp.digitaltwin.repository.SimulationResultRepository;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.emf.ecore.resource.Resource;
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

/**
 * Main Digital Twin Engine - Orchestrator
 * 
 * This is the main service that coordinates the digital twin simulation.
 * After refactoring, this class now delegates specific responsibilities to:
 * - ModelService: Model loading and EOL execution
 * - PredictionService: Energy prediction logic
 * - WhatIfAnalysisService: What-If scenario analysis
 * 
 * This follows the Service Layer Pattern for better maintainability and testability.
 */
@Service
public class DigitalTwinEngine {
    
    // ========== Dependencies ==========

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
    private com.fyp.digitaltwin.dto.LinearRegressionModel regressionModel;
    private double mlSlope = 1.0;      // ML slope (a) - learned regression parameter
    private double mlIntercept = 0.0;  // ML intercept (b) - learned regression parameter
    private boolean isCalibrated = false;

    private String simulationStartTime = null;

    //Initialization
    @PostConstruct
    public void init() {
        try {
            System.out.println("Initializing Digital Twin Engine...");

            // --- TEMPORARY LINE: WIPE DB TO FIX GRAPH VALUES ---
            resultRepository.deleteAll(); 
            System.out.println("!!! DATABASE CLEARED !!!");
            // ---------------------------------------------------

             // ARCHITECTURAL FIX: Load base model and clone for runtime
           // Base model is read-only, runtime model is the working copy
            EmfModel baseModel = modelService.loadBaseModel();
            Resource clonedResource = modelService.deepCloneModel(baseModel);
            this.smartOfficeModel = modelService.createEmfModelFromResource(clonedResource);
            baseModel.dispose(); // Clean up base model, we only need the runtime clone

            System.out.println("Runtime twin model created from base model (deep cloned, isolated)");

            // Check if MongoDB has data
            totalDataCount = repository.count();
            if (totalDataCount == 0) {
                System.out.println("MongoDB is empty. Importing data from CSV...");
                loadCsvToMongo("src/main/resources/cleandata.csv");
                totalDataCount = repository.count();
            }

            System.out.println("Digital Twin Engine initialized successfully!");
            System.out.println("Engine Ready. Using MongoDB with " + totalDataCount + " records.");
            
            // Set live model reference in prediction service BEFORE training
            predictionService.setLiveModel(smartOfficeModel);
            
            // Train Linear Regression model using Machine Learning
            regressionModel = regressionTrainingService.trainModel(smartOfficeModel, totalDataCount);
            isCalibrated = regressionModel.isValid();
            
            // Extract ML model parameters
            mlSlope = regressionModel.getSlope();        // slope (a)
            mlIntercept = regressionModel.getIntercept();  // intercept (b)
            
            // Pass ML model parameters to services
            predictionService.setMlSlope(mlSlope);        // slope (a)
            predictionService.setMlIntercept(mlIntercept);  // intercept (b)
            whatIfAnalysisService.setRegressionModel(regressionModel);  // Full ML model for What-If
            
            // Fast-forward initialization for demo readiness
            fastForwardInitialization(20); // Run 20 steps = 5 hours of simulation
            
            //Capture the start time
            if(totalDataCount > 0){
                DataRecord firstDate = fetchRecordByIndex(0);
                if(firstDate != null){
                    simulationStartTime = firstDate.getDate();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fatal Error: Could not start engine.");
        }
    }
    
    /**
     * Getter for trained regression model (used by anomaly detection)
     */
    public com.fyp.digitaltwin.dto.LinearRegressionModel getRegressionModel() {
        return regressionModel;
    }
    
    /**
     * Getter for ML slope (used by other services and for API)
     */
    public double getMlSlope() {
        return mlSlope;
    }
    
    /**
     * Check if ML model has been trained
     */
    public boolean isCalibrated() {
        return isCalibrated;
    }
    
    /**
     * Fast-forward simulation to build up realistic state quickly
     * Useful for demos to avoid waiting for scheduled simulation
     * Runs multiple simulation steps in rapid succession
     * 
     * @param steps Number of steps to fast-forward (4 steps = 1 hour)
     */
    private void fastForwardInitialization(int steps) {
        System.out.println("\n FAST-FORWARD MODE: Running " + steps + " simulation steps for demo readiness...");
        System.out.println("   (This simulates " + (steps * 0.25) + " hours of building operation)");
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < steps; i++) {
            try {
                runSimulationStep();
                successCount++;
                
                // Show progress every 5 steps
                if ((i + 1) % 5 == 0) {
                    System.out.println("   Progress: " + (i + 1) + "/" + steps + " steps completed");
                }
            } catch (Exception e) {
                System.err.println("     Fast-forward step " + (i + 1) + " failed: " + e.getMessage());
                // Continue with remaining steps even if one fails
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("\n FAST-FORWARD COMPLETE!");
        System.out.println("   Completed: " + successCount + "/" + steps + " steps");
        System.out.println("   Duration: " + (duration / 1000.0) + " seconds");
        System.out.println("   Model is now ready for What-If analysis!");
        System.out.println("   Current simulation step: " + currentStepIndex + "\n");
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
            // 1. Handle Data Looping
            if (currentStepIndex >= totalDataCount) {
                System.out.println("--- End of Dataset. Restarting Simulation... ---");
                currentStepIndex = 0;
            }

            // 2. Get Data for NOW from MongoDB
            DataRecord currentData = fetchRecordByIndex(currentStepIndex);
            
            if (currentData != null) {
                System.out.println(">> Simulating Step " + currentStepIndex + " | Date: " + currentData.getDate());

                // 3. Run Physics (hvac.eol)
                String physicsLog = modelService.runEolScript(
                    smartOfficeModel, 
                    "hvac.eol", 
                    "HVAC Physics", 
                    currentData, 
                    TIME_STEP_HOURS, 
                    manualOverrides,
                    mlSlope,
                    mlIntercept,
                    null  // Not needed for hvac.eol
                );
                if (!physicsLog.isBlank()) {
                    System.out.println(physicsLog);
            }

                // 4. Save simulation snapshot to MongoDB
                saveSimulationSnapshot(currentData);
            }

            // 5. Move to next step
            currentStepIndex++;

        } catch (Exception e) {
            System.err.println("Error in simulation step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves current simulation state to MongoDB for dashboard and analysis
     */
    private void saveSimulationSnapshot(DataRecord currentData) {
        try {
            // 1. Run json.eol to aggregate the data (Power, Avg Temp, etc.)
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

            System.out.println("DEBUG JSON OUTPUT: " + jsonOutput);
            
            // 2. Parse the JSON
            JsonNode root = objectMapper.readTree(jsonOutput);
            
            // 3. Extract values
            double realPower = root.path("power").path("real").asDouble();
            double simulatedPower = root.path("power").path("simulated_raw").asDouble();
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
            
            resultRepository.save(result);
            
        } catch (Exception e) {
            System.err.println("Failed to save simulation snapshot: " + e.getMessage());
        }
    }
    
    // API Methods called by Controllers
    /**
     * Gets live status for dashboard (calls query.eol)
     */
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

    /**
     * Gets dashboard data as JSON (calls json.eol)
     */
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

    /**
     * Gets validation report (calls validation.evl)
     */
    public String getValidationReport() {
        try {
            return modelService.runValidation(smartOfficeModel);
        } catch (Exception e) {
            return "Error running validation: " + e.getMessage();
        }
    }

    /**
     * Sets manual override for a room's HVAC (e.g., "COOL", "HEAT", "OFF", "AUTO")
     */
    public void setOverride(String roomId, String action) {
        if (action.equals("AUTO")) {
            manualOverrides.remove(roomId);
            System.out.println("Removed override for " + roomId + " → AUTO mode");
        } else {
            manualOverrides.put(roomId, action);
            System.out.println("Set override: " + roomId + " → " + action);
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
    /**
     * Fetches a data record by index from MongoDB
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
    
    /**
     * Loads CSV data into MongoDB (runs once on first startup)
     */
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
                    System.out.println("Imported " + count + " records...");
        }
            }
            
            System.out.println(" CSV Import Complete. Total: " + count + " records.");
        } catch (Exception e) {
            System.err.println(" CSV Import Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
