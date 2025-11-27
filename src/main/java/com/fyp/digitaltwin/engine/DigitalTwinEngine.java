package com.fyp.digitaltwin.engine;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream; // Import for capturing output
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream; // Import for capturing output
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

    // Engine State
    private EmfModel smartOfficeModel;
    private List<DataRecord> dataset;
    private int currentStepIndex = 0; // Tracks which CSV row we are on

    // Simulation Constants
    private static final double TIME_STEP_HOURS = 0.25;

    public DigitalTwinEngine() {
    }

    // 1. INITIALIZATION (Runs once when server starts)
    @PostConstruct
    public void init() {
        try {
            System.out.println("⚙️ Initializing Digital Twin Engine...");
            registerSmartOfficeResourceFactory();
            this.smartOfficeModel = loadModel();

            // Make sure to use the correct path to your CSV
            this.dataset = loadCsvFile("src/main/resources/cleandata.csv");

            System.out.println("✔ Engine Ready. Loaded " + dataset.size() + " rows of data.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Fatal Error: Could not start engine.");
        }
    }

    // 2. THE HEARTBEAT (Runs automatically every 5 seconds)
    @Scheduled(fixedRate = 5000)
    public void runSimulationStep() {
        if (dataset == null || smartOfficeModel == null) return;

        try {
            // A. Handle Data Looping
            if (currentStepIndex >= dataset.size()) {
                System.out.println("--- End of Dataset. Restarting Simulation... ---");
                currentStepIndex = 0;
                // Optional: Logic to reset energy meters here if you want
            }

            // B. Get Data for NOW
            DataRecord currentData = dataset.get(currentStepIndex);
            System.out.println(">> Simulating Step " + currentStepIndex + " | Date: " + currentData.getDate());

            // C. Run Physics (hvac.eol)
            // We don't need the output from this, so we just run it
            runEolScript(smartOfficeModel, "hvac.eol", "HVAC Physics", currentData, TIME_STEP_HOURS,manualOverrides);

            // D. Move to next step
            currentStepIndex++;

        } catch (Exception e) {
            System.err.println("Error in simulation step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 3. API METHOD (Called by the Web Controller)
    public String getLiveStatus() {
        try {
            // Use the previous data point (index - 1) because we just incremented it
            int reportIndex = (currentStepIndex > 0) ? currentStepIndex - 1 : 0;
            DataRecord reportData = dataset.get(reportIndex);

            // Run query.eol and CAPTURE the output string!
            return runEolScript(smartOfficeModel, "query.eol", "Query", reportData, TIME_STEP_HOURS,null);
        } catch (Exception e) {
            return "Error retrieving status: " + e.getMessage();
        }
    }
    // --- 4. VALIDATION METHOD (Called by Controller) ---
    public String getValidationReport() {
        try {
            // Run validation on the CURRENT state of the model
            return runEvlValidation(smartOfficeModel, "validation.evl");
        } catch (Exception e) {
            return "Validation Error: " + e.getMessage();
        }
    }

    public void setOverride(String roomId, String status) {
        System.out.println("⚡ Command Received: Set " + roomId + " to " + status);
        if (status.equals("AUTO")) {
            manualOverrides.remove(roomId); // Remove override, let physics take over
        } else {
            manualOverrides.put(roomId, status); // Force ON or OFF
        }
    }

    // --- HELPER METHODS ---

    // Modified to RETURN the console output as a String
    private String runEolScript(EmfModel model, String resourceName, String label, Object data, Object timeStep, Map<String, String> overrides) throws Exception {
        // System.out.printf("Running %s (%s)...%n", label, resourceName); // Optional logging
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

        // Capture Output
        // Create a buffer to hold the text
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        // Tell Epsilon to print to our buffer instead of the IntelliJ console
        module.getContext().setOutputStream(new PrintStream(outStream));

        module.execute();

        // Return the captured text
        return outStream.toString();
    }
    // --- INTERNAL HELPER: Runs EVL and returns a String report ---
    private String runEvlValidation(EmfModel model, String resourceName) throws Exception {
        EvlModule module = new EvlModule();
        parseModule(module, resourceName); // Uses your existing parseModule helper
        module.getContext().getModelRepository().addModel(model);

        module.execute(); // Run the validation checks

        // Collect results
        var unsatisfied = module.getContext().getUnsatisfiedConstraints();

        StringBuilder report = new StringBuilder();
        report.append("----------------------------------------------------------------\n");
        report.append(" VALIDATION REPORT (" + resourceName + ")\n");
        report.append("----------------------------------------------------------------\n");

        if (unsatisfied.isEmpty()) {
            report.append("✔ Validation PASSED. System is healthy.\n");
        } else {
            report.append("✖ Validation FAILED. Found " + unsatisfied.size() + " issues:\n\n");
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
        model.setStoredOnDisposal(false); // CAREFUL: If you want persistence, set this to true, but handle resets!
        model.load();
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

    public List<DataRecord> loadCsvFile(String filepath) throws Exception {
        List<DataRecord> dataset = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                DataRecord dr = new DataRecord(
                        cols[0],
                        Double.parseDouble(cols[1]),
                        Double.parseDouble(cols[2]),
                        Integer.parseInt(cols[3])
                );
                dataset.add(dr);
            }
        }
        return dataset;
    }

    private URI toEmfUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().toString());
    }
    // --- NEW METHOD: Get JSON Data ---
    public String getDashboardData() {
        try {
            int dataIndex = (currentStepIndex > 0) ? currentStepIndex - 1 : 0;
            DataRecord currentData = dataset.get(dataIndex);

            // Run the new json.eol script!
            return runEolScript(smartOfficeModel, "json.eol", "json",currentData, TIME_STEP_HOURS,null);

        } catch (Exception e) {
            // Return a valid JSON error message so frontend doesn't crash
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}