package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.model.SensorData;
import com.fyp.digitaltwin.model.SimulationResult;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import com.fyp.digitaltwin.repository.SimulationResultRepository;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DigitalTwinEngine {
    private static final String MODEL_RESOURCE = "DigitalTwin.smartoffice";
    private static final String METAMODEL_RESOURCE = "SmartOffice.ecore";
    private Map<String, String> manualOverrides = new HashMap<>();

    @Autowired
    private SensorDataRepository repository;

    @Autowired
    private SimulationResultRepository resultRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Engine State
    private EmfModel smartOfficeModel;
    private int currentStepIndex = 0; // Tracks which row index we are on
    private long totalDataCount = 0;  // Total rows in MongoDB

    // Simulation Constants
    private static final double TIME_STEP_HOURS = 0.25;

    public DigitalTwinEngine() {
    }

    // 1. INITIALIZATION
    @PostConstruct
    public void init() {
        try {
            System.out.println("Initializing Digital Twin Engine...");
            registerSmartOfficeResourceFactory();
            this.smartOfficeModel = loadModel();

            // Check if MongoDB has data
            totalDataCount = repository.count();
            if (totalDataCount == 0) {
                System.out.println("MongoDB is empty. Importing data from CSV...");
                loadCsvToMongo("src/main/resources/cleandata.csv");
                totalDataCount = repository.count();
            }

            System.out.println("Engine Ready. Using MongoDB with " + totalDataCount + " records.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fatal Error: Could not start engine.");
        }
    }

    // 2. THE HEARTBEAT (Runs every 5 seconds)
    @Scheduled(fixedRate = 5000)
    public synchronized void runSimulationStep() {
        if (smartOfficeModel == null || totalDataCount == 0) return;

        try {
            // A. Handle Data Looping
            if (currentStepIndex >= totalDataCount) {
                System.out.println("--- End of Dataset. Restarting Simulation... ---");
                currentStepIndex = 0;
            }

            // B. Get Data for NOW from MongoDB (Fetch 1 record at current index)
            DataRecord currentData = fetchRecordByIndex(currentStepIndex);
            
            if (currentData != null) {
                System.out.println(">> Simulating Step " + currentStepIndex + " | Date: " + currentData.getDate());

                // C. Run Physics (hvac.eol)
                String physicsLog = runEolScript(smartOfficeModel, "hvac.eol", "HVAC Physics", currentData, TIME_STEP_HOURS, manualOverrides);
                if (!physicsLog.isBlank()) {
                    System.out.println(physicsLog);
                }

                // D. SAVE RESULTS TO MONGODB (New Feature)
                saveSimulationSnapshot(currentData);
            }

            // E. Move to next step
            currentStepIndex++;

        } catch (Exception e) {
            System.err.println("Error in simulation step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSimulationSnapshot(DataRecord currentData) {
        try {
            // 1. Run json.eol to aggregate the data (Power, Avg Temp, etc.)
            String jsonOutput = runEolScript(smartOfficeModel, "json.eol", "JSON Aggregation", currentData, TIME_STEP_HOURS, null);
            
            // 2. Parse the JSON
            JsonNode root = objectMapper.readTree(jsonOutput);
            
            // 3. Extract Values
            String timestamp = root.path("timestamp").asText();
            double realPower = root.path("power").path("real").asDouble();
            double simulatedPower = root.path("power").path("simulated").asDouble();
            double totalEnergy = root.path("energy").path("total").asDouble();
            double avgTemp = root.path("comfort").path("avgTemp").asDouble();
            int activeHvacs = root.path("comfort").path("activeHvacs").asInt();
            
            // 4. Create and Save
            SimulationResult result = new SimulationResult(
                timestamp, 
                realPower, 
                simulatedPower, 
                totalEnergy, 
                currentData.getOutdoorTemperature(), // From input data
                avgTemp, 
                activeHvacs
            );
            
            resultRepository.save(result);
            // System.out.println("   [Saved Snapshot] Sim Power: " + simulatedPower + "kW");

        } catch (Exception e) {
            System.err.println("Failed to save simulation snapshot: " + e.getMessage());
        }
    }

    // 4. MODEL-BASED PREDICTION (Digital Twin Look-Ahead)
    public synchronized Map<String, Double> predictFutureEnergy(int hoursToPredict) {
        System.out.println("Running Prediction for next " + hoursToPredict + " hours...");
        double totalPredictedEnergy = 0.0;
        
        try {
            // 4.1  CLONE THE MODEL
            // We cannot run prediction on the live 'smartOfficeModel' because it would mess up the live dashboard.
            // So we reload a fresh copy from disk to act as our "Sandbox" for prediction.
            EmfModel predictionModel = loadModel(); 

            // We keep the name "SmartOffice" because this EolModule is isolated from the main one.
            predictionModel.setName("SmartOffice");

            // 4.2 PREPARE FUTURE DATA
            // We need to fetch the *next* N hours of data from MongoDB.
            int stepsNeeded = hoursToPredict * 4; // 4 steps per hour (15 min intervals)
            
            // 4.3 Get Current Date from current simulation step
            DataRecord currentRecord = fetchRecordByIndex(currentStepIndex);
            if (currentRecord == null) {
                 return Map.of("error", 0.0);
            }
            String currentDate = currentRecord.getDate();

            // 4.4 Fetch FUTURE records starting from this date
            // Using new efficient Repository method
            List<SensorData> futureDataList = repository.findByDateGreaterThan(
                currentDate, 
                PageRequest.of(0, stepsNeeded, Sort.by(Sort.Direction.ASC, "date"))
            );

            System.out.println("   Found " + futureDataList.size() + " future records for prediction.");

            // 4.5 RUN FAST-FORWARD SIMULATION
            // Loop through the future data and run 'hvac.eol' on the Sandbox Model
            for (SensorData mongoData : futureDataList) {
                // Convert to DTO
                DataRecord stepData = new DataRecord(
                    mongoData.getDate(),
                    mongoData.getPowerConsumption(),
                    mongoData.getOutdoorTemperature(),
                    mongoData.getOccupancy()
                );

                // Run Physics (Quietly, no system.out)
                // Reuse the exact same hvac.eol logic.This is the power of the Digital Twin.
                runEolScript(predictionModel, "hvac.eol", "Prediction", stepData, TIME_STEP_HOURS, manualOverrides);
                
                // After the step, calculate the energy used in this 15-min window
                // We can extract this by running a mini-query or just summing it up from the model
                // For simplicity, let's run json.eol to get the aggregate power
                String jsonOutput = runEolScript(predictionModel, "json.eol", "PredictionAgg", stepData, TIME_STEP_HOURS, null);
                JsonNode root = objectMapper.readTree(jsonOutput);
                double stepPower = root.path("power").path("simulated").asDouble();
                
                // Energy (kWh) = Power (kW) * Time (0.25h)
                totalPredictedEnergy += (stepPower * TIME_STEP_HOURS);
            }
            
            // 4.6 Clean up
            predictionModel.dispose();

            System.out.println("Prediction Complete. Est. Energy: " + totalPredictedEnergy + " kWh");

            // 4.7 Return result map
            Map<String, Double> result = new HashMap<>();
            result.put("predictedEnergy", totalPredictedEnergy);
            result.put("hours", (double) hoursToPredict);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", -1.0);
        }
    }

    // 3. API METHOD (Called by the Web Controller)
    public String getLiveStatus() {
        try {
            // Use previous data point (index - 1)
            int reportIndex = (currentStepIndex > 0) ? currentStepIndex - 1 : 0;
            DataRecord reportData = fetchRecordByIndex(reportIndex);

            // Run query.eol
            return runEolScript(smartOfficeModel, "query.eol", "Query", reportData, TIME_STEP_HOURS, null);
        } catch (Exception e) {
            return "Error retrieving status: " + e.getMessage();
        }
    }

    // Get JSON Data for the dashboard
    public String getDashboardData() {
        try {
            int dataIndex = (currentStepIndex > 0) ? currentStepIndex - 1 : 0;
            DataRecord currentData = fetchRecordByIndex(dataIndex);

            // Run the new json.eol script
            return runEolScript(smartOfficeModel, "json.eol", "json", currentData, TIME_STEP_HOURS, null);

        } catch (Exception e) {
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    // Helper to fetch record from MongoDB and convert to DataRecord
    private DataRecord fetchRecordByIndex(int index) {
        // Use Pagination to get the Nth record efficiently
        Page<SensorData> page = repository.findAll(PageRequest.of(index, 1, Sort.by(Sort.Direction.ASC, "date")));
        
        if (page.hasContent()) {
            SensorData mongoData = page.getContent().get(0);
            // Convert to the DTO expected by EOL
            return new DataRecord(
                mongoData.getDate(),
                mongoData.getPowerConsumption(),
                mongoData.getOutdoorTemperature(),
                mongoData.getOccupancy()
            );
        }
        return null;
    }

    // CSV Loader Logic (Run only once)
    public void loadCsvToMongo(String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            br.readLine(); // Skip header
            List<SensorData> batch = new ArrayList<>();
            
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                SensorData data = new SensorData(
                        cols[0], // Date
                        Double.parseDouble(cols[1]), // Power
                        Double.parseDouble(cols[2]), // Temp
                        Integer.parseInt(cols[3])    // Occupancy
                );
                batch.add(data);
                
                // Batch save every 1000 records for speed
                if (batch.size() >= 1000) {
                    repository.saveAll(batch);
                    batch.clear();
                    System.out.println("... Imported 1000 records");
                }
            }
            // Save remaining
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }
            System.out.println("CSV Import Complete!");
            
        } catch (Exception e) {
            System.err.println("Failed to import CSV: " + e.getMessage());
        }
    }


    public String getValidationReport() {
        try {
            return runEvlValidation(smartOfficeModel, "validation.evl");
        } catch (Exception e) {
            return "Validation Error: " + e.getMessage();
        }
    }

    public void setOverride(String roomId, String status) {
        System.out.println("Command Received: Set " + roomId + " to " + status);
        if (status.equals("AUTO")) {
            manualOverrides.remove(roomId);
        } else {
            manualOverrides.put(roomId, status);
        }
    }

    private String runEolScript(EmfModel model, String resourceName, String label, Object data, Object timeStep, Map<String, String> overrides) throws Exception {
        EolModule module = new EolModule();
        parseModule(module, resourceName);
        module.getContext().getModelRepository().addModel(model);

        if (data != null) {
            module.getContext().getFrameStack().put("simulateData", data);
        }
        if (timeStep != null) {
            module.getContext().getFrameStack().put("TIME_STEP_HOURS", timeStep);
        }
        if (overrides != null) {
            module.getContext().getFrameStack().put("manualOverrides", overrides);
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        module.getContext().setOutputStream(new PrintStream(outStream));

        module.execute();

        return outStream.toString();
    }

    private String runEvlValidation(EmfModel model, String resourceName) throws Exception {
        EvlModule module = new EvlModule();
        parseModule(module, resourceName);
        module.getContext().getModelRepository().addModel(model);
        module.execute();

        var unsatisfied = module.getContext().getUnsatisfiedConstraints();
        StringBuilder report = new StringBuilder();
        report.append("----------------------------------------------------------------\n");
        report.append(" VALIDATION REPORT (" + resourceName + ")\n");
        report.append("----------------------------------------------------------------\n");

        if (unsatisfied.isEmpty()) {
            report.append("Validation PASSED. System is healthy.\n");
        } else {
            report.append("Validation FAILED. Found " + unsatisfied.size() + " issues:\n\n");
            for (UnsatisfiedConstraint issue : unsatisfied) {
                report.append("  [CONSTRAINT] ").append(issue.getConstraint().getName()).append("\n");
                report.append("  [ELEMENT]    ").append(issue.getInstance()).append("\n");
                report.append("  [MESSAGE]    ").append(issue.getMessage()).append("\n");
                report.append("  ------------------------------------------------------------\n");
            }
        }
        return report.toString();
    }

    private void registerSmartOfficeResourceFactory() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .putIfAbsent(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("smartoffice", new XMIResourceFactoryImpl());
    }

    private EmfModel loadModel() throws Exception {
        System.out.printf("Loading SmartOffice model (%s | %s)%n", MODEL_RESOURCE, METAMODEL_RESOURCE);
        EmfModel model = new EmfModel();
        model.setName("SmartOffice");
        model.setModelFileUri(toEmfUri(resolveResource(MODEL_RESOURCE)));
        model.setMetamodelFileUri(toEmfUri(resolveResource(METAMODEL_RESOURCE)));
        model.setReadOnLoad(true);
        model.setStoredOnDisposal(false);
        model.load();
        
        // 🔥 FORCE RESOLVE ALL CROSS-REFERENCES (neighbour links)
        EcoreUtil.resolveAll(model.getResource());
        
        return model;
    }

    private void parseModule(IEolModule module, String resourceName) throws Exception {
        File script = resolveResource(resourceName).toFile();
        module.parse(script);
        if (!module.getParseProblems().isEmpty()) {
            throw new IllegalStateException(formatParseErrors(resourceName, module.getParseProblems()));
        }
    }

    private String formatParseErrors(String resourceName, List<ParseProblem> problems) {
        StringBuilder builder = new StringBuilder("Failed to parse ")
                .append(resourceName)
                .append(System.lineSeparator());
        for (ParseProblem problem : problems) {
            builder.append(" - ").append(problem.toString()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private Path resolveResource(String resourceName) {
        URL url = DigitalTwinEngine.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourceName);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI for resource " + resourceName, e);
        }
    }

    private URI toEmfUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().toString());
    }
}
