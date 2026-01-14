package com.fyp.digitaltwin.service;

import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.dto.CostAnalysisResult;

import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Service responsible for What-If Analysis scenarios.
 * Uses Model-Driven Engineering to test different building parameters
 * and predict their impact on energy consumption.
 * 
 * Now uses Machine Learning (Linear Regression) for predictions.
 * Part of the refactored Service Layer Architecture.
 */
@Service
public class WhatIfAnalysisService {
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
    private PredictionService predictionService;
    
    /**
     * Sets the trained ML regression model (called by DigitalTwinEngine).
     * What-If Analysis delegates（委托） all predictions to PredictionService,
     * which uses the calibration factor internally.
     * This method is kept for initialization logging purposes.
     */
    public void setRegressionModel(LinearRegressionModel model) {
        System.out.println("WhatIfAnalysisService: Initialized with ML model - slope=" + 
                          String.format("%.4f", model.getSlope()) + 
                          ", intercept=" + String.format("%.4f", model.getIntercept()));
    }
    
    /**
     * Runs What-If analysis comparing baseline vs modified scenario
     * 
     * @param changes Map of parameters to change (e.g., {"targetTemp": 21.0, "insulation": 0.03})
     * @param hours Prediction horizon in hours
     * @return Comparison result with baseline, scenario, savings, and recommendations
     */
    public Map<String, Object> runAnalysis(Map<String, Object> changes, int hours,Double investmentCost) {
        System.out.println("=== WHAT-IF ANALYSIS START ===");
        System.out.println("Changes requested: " + changes);
        System.out.println("Prediction horizon: " + hours + " hours");
        
        try {
            // STEP 1: Run baseline prediction with current live state (with step data for charting)
            System.out.println("\n[1/5] Running baseline prediction...");
            Map<String, Object> baselineDetailed = predictionService.predictFutureEnergyWithSteps(hours);
            
            // Check if baseline prediction failed
            if (baselineDetailed == null || baselineDetailed.containsKey("error") || !baselineDetailed.containsKey("predictedEnergy")) {
                return createErrorResponse("Baseline prediction failed. Please wait for simulation to run.", null);
            }
            
            System.out.println("Baseline energy: " + baselineDetailed.get("predictedEnergy") + " kWh");
            
            // STEP 1.5: Extract baseline base load BEFORE cloning
            EmfModel liveModel = predictionService.getLiveModel();
            if (liveModel == null) {
                throw new IllegalStateException("Live model not available. Please wait for simulation to initialize.");
            }
            // Extract baseline values only for parameters that will be changed
            Map<String, Double> baselineValues = new HashMap<>();
            if (changes.containsKey("targetTemp")) {
                double baselineTargetTemp = extractAverageTargetTemp(liveModel);
                baselineValues.put("targetTemp", baselineTargetTemp);
                System.out.println("Baseline target temperature: " + baselineTargetTemp + "°C");
            }
            if (changes.containsKey("insulation")) {
                double baselineInsulation = extractAverageInsulation(liveModel);
                baselineValues.put("insulation", baselineInsulation);
                System.out.println("Baseline insulation: " + baselineInsulation);
            }
            if (changes.containsKey("baseLoad")) {
                double baselineBaseLoad = extractBaseLoad(liveModel);
                baselineValues.put("baseLoad", baselineBaseLoad);
                System.out.println("Baseline base load: " + baselineBaseLoad + " kW");
            }


            // STEP 2: Clone live model for scenario testing
            System.out.println("\n[2/5] Cloning live model for scenario...");
            // Pure EMF deep clone - returns Resource, not EmfModel
            Resource clonedResource = modelService.deepCloneModel(liveModel);

            // Wrap Resource in EmfModel for EOL execution
            EmfModel scenarioModel = modelService.createEmfModelFromResource(clonedResource);

            // STEP 3: Apply changes using EOL model transformation
            System.out.println("\n[3/5] Applying what-if changes to model...");
            applyChangesToModel(scenarioModel, changes);


             // STEP 3.5: Extract scenario values AFTER applying changes (only for changed parameters)
            Map<String, Double> scenarioValues = new HashMap<>();
            if (changes.containsKey("targetTemp")) {
                double scenarioTargetTemp = extractAverageTargetTemp(scenarioModel);
                scenarioValues.put("targetTemp", scenarioTargetTemp);
                System.out.println("Scenario target temperature: " + scenarioTargetTemp + "°C");
            }
            if (changes.containsKey("insulation")) {
                double scenarioInsulation = extractAverageInsulation(scenarioModel);
                scenarioValues.put("insulation", scenarioInsulation);
                System.out.println("Scenario insulation: " + scenarioInsulation);
            }
            if (changes.containsKey("baseLoad")) {
                double scenarioBaseLoad = extractBaseLoad(scenarioModel);
                scenarioValues.put("baseLoad", scenarioBaseLoad);
                System.out.println("Scenario base load: " + scenarioBaseLoad + " kW");
            }

            
            // Debug: Show what changes were applied
            System.out.println("CHANGES APPLIED:");
            for (Map.Entry<String, Object> entry : changes.entrySet()) {
                System.out.println("   • " + entry.getKey() + ": " + entry.getValue());
            }
            
            // STEP 4: Run prediction on modified model (with step data for charting)
            System.out.println("\n[4/5] Running scenario prediction with ML calibration...");
            System.out.println("   ML Model: slope=" + String.format("%.4f", predictionService.getMlSlope()) + 
                               ", intercept=" + String.format("%.4f", predictionService.getMlIntercept()));
            Map<String, Object> scenarioDetailed = predictionService.predictOnModelWithSteps(scenarioModel, hours);
            
            // Check if scenario prediction failed
            if (scenarioDetailed == null || scenarioDetailed.containsKey("error") || !scenarioDetailed.containsKey("predictedEnergy")) {
                scenarioModel.dispose();
                return createErrorResponse("Scenario prediction failed.", null);
            }
            
            System.out.println("Scenario energy: " + scenarioDetailed.get("predictedEnergy") + " kWh");
            
            // STEP 5: Calculate savings and prepare results with chart data
            System.out.println("\n[5/5] Calculating savings and building chart data...");
            Map<String, Object> result = calculateSavingsWithChartData(baselineDetailed, scenarioDetailed, changes, hours,investmentCost,baselineValues,scenarioValues);
            
            // Clean up
            scenarioModel.dispose();
            
            System.out.println("\n=== WHAT-IF ANALYSIS COMPLETE ===\n");
            return result;
            
        } catch (Exception e) {
            System.err.println("ERROR in What-If Analysis: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Analysis failed: " + e.getMessage(), null);
        }
    }

    /**
     * Extracts average target temperature from all HVAC systems
     */
    private double extractAverageTargetTemp(EmfModel model) throws Exception {
        String script = 
            "var totalTemp = 0.0d;\n" +
            "var count = 0;\n" +
            "for (hvac in SmartOffice!HVACSystem.all) {\n" +
            "    totalTemp = totalTemp + hvac.targetTemperature;\n" +
            "    count = count + 1;\n" +
            "}\n" +
            "return (count > 0) ? (totalTemp / count) : 22.0d;\n";
        
        String result = modelService.runSimpleEolScript(model, script);
        try {
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse target temperature: " + result);
            return 22.0;
        }
    }

    /**
     * Extracts average insulation from all rooms
     */
    private double extractAverageInsulation(EmfModel model) throws Exception {
        String script = 
            "var totalInsulation = 0.0d;\n" +
            "var count = 0;\n" +
            "for (room in SmartOffice!Room.all) {\n" +
            "    totalInsulation = totalInsulation + room.insulation;\n" +
            "    count = count + 1;\n" +
            "}\n" +
            "return (count > 0) ? (totalInsulation / count) : 0.04d;\n";
        
        String result = modelService.runSimpleEolScript(model, script);
        try {
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse insulation: " + result);
            return 0.04;
        }
    }

    /**
     * Extracts total base load from all rooms in the model
     * @param model The model to query
     * @return Total base load in kW
     */
    private double extractBaseLoad(EmfModel model) throws Exception {
        String script = 
            "var totalBaseLoad = 0.0d;\n" +
            "for (room in SmartOffice!Room.all) {\n" +
            "    totalBaseLoad = totalBaseLoad + room.baseLoad;\n" +
            "}\n" +
            "return totalBaseLoad;\n";
        
        String result = modelService.runSimpleEolScript(model, script);
        try {
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse base load: " + result);
            return 0.0;
        }
    }
    
    /**
     * Applies parameter changes to the model using EOL transformation
     * This is pure Model-Driven Engineering - we transform the model, not the code!
     * 
     * @param model The model to transform
     * @param changes Map of parameters to change
     * @throws Exception if transformation fails
     */
    private void applyChangesToModel(EmfModel model, Map<String, Object> changes) throws Exception {
        StringBuilder eolScript = new StringBuilder();
        eolScript.append("// What-If Model Transformation\n");
        
        // Change 1: Target Temperature (applies to all HVAC systems)
        if (changes.containsKey("targetTemp")) {
            double newTarget = ((Number) changes.get("targetTemp")).doubleValue();
            System.out.println("  - Setting all HVAC target temperatures to: " + newTarget + "°C");
            eolScript.append("for (hvac in SmartOffice!HVACSystem.all) {\n");
            eolScript.append("    hvac.targetTemperature = ").append(newTarget).append("d;\n");
            eolScript.append("}\n");
        }
        
        // Change 2: Insulation (applies to all rooms)
        if (changes.containsKey("insulation")) {
            double newInsulation = ((Number) changes.get("insulation")).doubleValue();
            System.out.println("  - Setting all room insulation to: " + newInsulation);
            eolScript.append("for (room in SmartOffice!Room.all) {\n");
            eolScript.append("    room.insulation = ").append(newInsulation).append("d;\n");
            eolScript.append("}\n");
        }
        
        // Change 3: Per-room insulation (for specific rooms)
        if (changes.containsKey("roomInsulation")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> roomChanges = (Map<String, Object>) changes.get("roomInsulation");
            System.out.println("  - Setting per-room insulation:");
            for (Map.Entry<String, Object> entry : roomChanges.entrySet()) {
                double value = ((Number) entry.getValue()).doubleValue();
                System.out.println("    * " + entry.getKey() + " → " + value);
                eolScript.append("var room = SmartOffice!Room.all.selectOne(r | r.roomName == '")
                         .append(entry.getKey()).append("');\n");
                eolScript.append("if (room.isDefined()) { room.insulation = ").append(value).append("d; }\n");
            }
        }
        
        // Change 4: Base load (equipment power)
        if (changes.containsKey("baseLoad")) {
            double newBaseLoad = ((Number) changes.get("baseLoad")).doubleValue();
            System.out.println("  - Setting all room base loads to: " + newBaseLoad + " kW");
            eolScript.append("for (room in SmartOffice!Room.all) {\n");
            eolScript.append("    room.baseLoad = ").append(newBaseLoad).append("d;\n");
            eolScript.append("}\n");
        }
        
        // Change 5: Per-room base load
        if (changes.containsKey("roomBaseLoad")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> roomChanges = (Map<String, Object>) changes.get("roomBaseLoad");
            System.out.println("  - Setting per-room base load:");
            for (Map.Entry<String, Object> entry : roomChanges.entrySet()) {
                double value = ((Number) entry.getValue()).doubleValue();
                System.out.println("    * " + entry.getKey() + " → " + value + " kW");
                eolScript.append("var room = SmartOffice!Room.all.selectOne(r | r.roomName == '")
                         .append(entry.getKey()).append("');\n");
                eolScript.append("if (room.isDefined()) { room.baseLoad = ").append(value).append("d; }\n");
            }
        }
        
        // Execute the transformation
        String scriptContent = eolScript.toString();
        if (scriptContent.contains("SmartOffice")) {
            System.out.println("\n  Executing EOL transformation...");
            EolModule module = new EolModule();
            module.parse(scriptContent);
            if (!module.getParseProblems().isEmpty()) {
                throw new IllegalStateException("EOL transformation has parse errors");
            }
            module.getContext().getModelRepository().addModel(model);
            module.execute();
            module.getContext().getModelRepository().removeModel(model);
            System.out.println("  Transformation complete!");
        } else {
            System.out.println("  No changes to apply.");
        }
    }
    
    @Autowired
    private CostAnalysisService CostAnalysisService;
    /**
     * Calculates energy and cost savings with chart data from What-If analysis
     * 
     * @param baselineDetailed Baseline prediction results with step data
     * @param scenarioDetailed Scenario prediction results with step data
     * @param changes Parameters that were changed
     * @param hours Prediction horizon
     * @return Result map with savings calculations and chart data
     */
    private Map<String, Object> calculateSavingsWithChartData(Map<String, Object> baselineDetailed, 
                                                            Map<String, Object> scenarioDetailed,
                                                            Map<String, Object> changes,
                                                            int hours,
                                                            Double investmentCost,
                                                            Map<String, Double> baselineValues,
                                                            Map<String, Double> scenarioValues) {
        double baselineEnergy = (Double) baselineDetailed.get("predictedEnergy");
        double scenarioEnergy = (Double) scenarioDetailed.get("predictedEnergy");
        double energySaved = baselineEnergy - scenarioEnergy;
        double percentSaved = (energySaved / baselineEnergy) * 100;
        
        // Cost calculations (assuming $0.15/kWh - configurable in future)
        double costSaved = energySaved * 0.15;
        double annualCostSaved = costSaved * (365.0 / (hours / 24.0));
        
        // Build chart data from step-by-step energy lists
        // Extract currentDate from baselineDetailed (set by PredictionService)                                               
        String currentDate = null;
        if (baselineDetailed.containsKey("currentDate")) {
            currentDate = (String)  baselineDetailed.get("currentDate");
        }

        List<Double> baselineSteps = extractDoubleList(baselineDetailed,"stepEnergyList");
        List<Double> scenarioSteps = extractDoubleList(scenarioDetailed,"stepEnergyList");
        List<Map<String, Object>> chartData =  buildChartData(baselineSteps, scenarioSteps, currentDate, hours);

         
        // Extract start time from first data point if available
        String analysisStartTime = null;
        if (!chartData.isEmpty() && chartData.get(0).containsKey("startTime")) {
            analysisStartTime = (String) chartData.get(0).get("startTime");
        }
       

        // ═════════════════════════════════════════════════════════════════
        // COST ANALYSIS (Delegated to CostAnalysisService - SRP)
        // ═════════════════════════════════════════════════════════════════
        CostAnalysisResult costAnalysis = CostAnalysisService.analyzeCosts(
            energySaved, 
            hours, 
            investmentCost
        );    

        
        // Create simplified baseline/scenario maps for backward compatibility
        Map<String, Double> baseline = new HashMap<>();
        baseline.put("predictedEnergy", baselineEnergy);
        baseline.put("hours", (Double) baselineDetailed.get("hours"));
       
        Map<String, Double> scenario = new HashMap<>();
        scenario.put("predictedEnergy", scenarioEnergy);
        scenario.put("hours", (Double) scenarioDetailed.get("hours"));
        
        if (changes.containsKey("targetTemp")) {
            baseline.put("targetTemp", baselineValues.get("targetTemp"));
            scenario.put("targetTemp", scenarioValues.get("targetTemp"));
        }
        if (changes.containsKey("insulation")) {
            baseline.put("insulation", baselineValues.get("insulation"));
            scenario.put("insulation", scenarioValues.get("insulation"));
        }
        if (changes.containsKey("baseLoad")) {
            baseline.put("baseLoad", baselineValues.get("baseLoad"));
            scenario.put("baseLoad", scenarioValues.get("baseLoad"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("baseline", baseline);
        result.put("scenario", scenario);
        result.put("energySaved", Math.round(energySaved * 100.0) / 100.0);
        result.put("percentSaved", Math.round(percentSaved * 100.0) / 100.0);

       // Calculate differences for changed parameters
        if (changes.containsKey("targetTemp")) {
            double targetTempDiff = scenarioValues.get("targetTemp") - baselineValues.get("targetTemp");
            result.put("targetTempDifference", Math.round(targetTempDiff * 10.0) / 10.0);
        }
        if (changes.containsKey("insulation")) {
            double insulationDiff = scenarioValues.get("insulation") - baselineValues.get("insulation");
            result.put("insulationDifference", Math.round(insulationDiff * 1000.0) / 1000.0);
        }
        if (changes.containsKey("baseLoad")) {
            double baseLoadDiff = scenarioValues.get("baseLoad") - baselineValues.get("baseLoad");
            result.put("baseLoadDifference", Math.round(baseLoadDiff * 100.0) / 100.0);
        }

        result.put("costAnalysis", costAnalysis);  
        result.put("costSaved", costAnalysis.getPeriodCostSaved());  
        result.put("annualCostSaved", costAnalysis.getAnnualCostSaved());  

        result.put("changes", changes);
        result.put("hours", hours);
        result.put("chartData", chartData); 
       
        if (analysisStartTime != null) {
            result.put("analysisStartTime", analysisStartTime);
        }
        
        System.out.println("Energy Saved: " + energySaved + " kWh (" + percentSaved + "%)");
        System.out.println("Cost Analysis: Daily=£" + costAnalysis.getDailyCostSaved() + 
                      ", Monthly=£" + costAnalysis.getMonthlyCostSaved() + 
                      ", Annual=£" + costAnalysis.getAnnualCostSaved());
        if (costAnalysis.getPaybackPeriodMonths() != null) {
            System.out.println("ROI: Payback=" + costAnalysis.getPaybackPeriodMonths() + " months, " +
                            "ROI=" + costAnalysis.getRoiPercentage() + "%");
        }
        
        return result;
    }

    private List<Double> extractDoubleList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Double> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                result.add(n.doubleValue());
            }
        }
        return result;
    }
    /**
     * Builds chart data array from baseline and scenario step energy lists
     * Aggregates 15-minute steps into hourly data points for cleaner visualization
     * 
     * @param baselineSteps List of baseline energy per step (15-min intervals)
     * @param scenarioSteps List of scenario energy per step (15-min intervals)
     * @return List of chart data points (hourly aggregated)
     */
    private List<Map<String, Object>> buildChartData(List<Double> baselineSteps, List<Double> scenarioSteps, String startDate , int totalHours) {
        List<Map<String, Object>> chartData = new ArrayList<>();
        
        // Aggregate 4 steps (15-min each) into 1 hour for cleaner visualization
        LocalDateTime startDateTime = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                // Try common date formats
                if (startDate.contains(" ")) {
                    String[] parts = startDate.split(" ");
                    String datePart = parts[0];
                    String timePart = parts.length > 1 ? parts[1] : "00:00:00";
                    
                    String[] dateParts = datePart.split("-");
                    String[] timeParts = timePart.split(":");
                    
                    int year = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int day = Integer.parseInt(dateParts[2]);
                    int hour = timeParts.length > 0 ? Integer.parseInt(timeParts[0]) : 0;
                    int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
                    
                    startDateTime = java.time.LocalDateTime.of(year, month, day, hour, minute);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not parse start date: " + startDate);
            }
        }

        int stepsPerHour = 4;
        int totalHoursCalculated = baselineSteps.size() / stepsPerHour;
        
        for (int hour = 0; hour < totalHoursCalculated; hour++) {
            double baselineHourEnergy = 0.0;
            double scenarioHourEnergy = 0.0;
            
            // Sum up 4 steps to get hourly energy
            for (int step = 0; step < stepsPerHour; step++) {
                int index = hour * stepsPerHour + step;
                if (index < baselineSteps.size()) {
                    baselineHourEnergy += baselineSteps.get(index);
                    scenarioHourEnergy += scenarioSteps.get(index);
                }
            }
            
            Map<String, Object> dataPoint = new HashMap<>();

             // Always show HH:mm format on x-axis (no dates)
            if (startDateTime != null) {
                java.time.LocalDateTime hourDateTime = startDateTime.plusHours(hour);
                // Format: Always HH:mm (14:00, 15:00, etc.)
                dataPoint.put("timestamp", hourDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                // Store full timestamp for subtitle/caption display
                if (hour == 0) {
                    // Store start time in result for frontend display
                    dataPoint.put("startTime", hourDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            } else {
                // Fallback: use hour number
                dataPoint.put("timestamp", "H" + (hour + 1));
            }

            dataPoint.put("hour", hour + 1);  // Hour 1, 2, 3, ... (not 0-indexed for display)
            dataPoint.put("baseline", Math.round(baselineHourEnergy * 100.0) / 100.0);
            dataPoint.put("whatif", Math.round(scenarioHourEnergy * 100.0) / 100.0);

            chartData.add(dataPoint);
        }
        
        return chartData;
    }
    
    
    /**
     * Creates an error response for What-If analysis failures
     */
    private Map<String, Object> createErrorResponse(String message, Map<String, Double> data) {
        System.err.println("ERROR: " + message);
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", true);
        errorResult.put("message", message);
        if (data != null) {
            errorResult.putAll(data);
        }
        return errorResult;
    }
}

