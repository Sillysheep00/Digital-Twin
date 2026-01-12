package com.fyp.digitaltwin.controller;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.WhatIfRequest;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.service.AnomalyDetectionService;
import com.fyp.digitaltwin.service.DigitalTwinEngine;
import com.fyp.digitaltwin.repository.SimulationResultRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DigitalTwinController {

    @Autowired
    private DigitalTwinEngine engine;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    // Endpoint: http://localhost:8080/api/status
    @GetMapping("/status")
    public String getStatus() {
        return engine.getLiveStatus();
    }

    // Endpoint: http://localhost:8080/api/hello
    @GetMapping("/hello")
    public String sayHello() {
        return "Digital Twin Server is Online!";
    }

    // Endpoint: http://localhost:8080/api/validate
    @GetMapping(value = "/validation",produces = MediaType.TEXT_PLAIN_VALUE) //MediaType.TEXT_PLAIN_VALUE mean this api return plain
    public String getValidation() {
        return engine.getValidationReport();
    }
    // Endpoint: http://localhost:8080/api/dashboard
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE) // Tells browser this is JSON
    /*ResponseEntity is a springboot class that represents a full HTTP response
      <String> : the type of data in the body in this case a JSON string
     */
    public ResponseEntity<String> getDashboard(){
        //Get raw JSON string from the engine
        String json = engine.getDashboardData();

        //Send it to the web browser as json
        return ResponseEntity.ok(json);
    }

    // Endpoint for Control
    // URL: POST http://localhost:8080/api/control?roomId=R1&action=OFF
    @PostMapping("/control")
    public ResponseEntity<String> controlHvac(@RequestParam String roomId, @RequestParam String action) {
        // action can be "ON", "OFF", or "AUTO"
        engine.setOverride(roomId, action);
        return ResponseEntity.ok("Command sent: " + roomId + " -> " + action);
    }

    // Endpoint for What-If Analysis
    // URL: POST http://localhost:8080/api/what-if
    // Body: {"changes": {"targetTemp": 21.0, "insulation": 0.03}, "hours": 24}
    @PostMapping("/what-if")
    public ResponseEntity<Map<String, Object>> runWhatIfAnalysis(@Valid @RequestBody WhatIfRequest request) {
        System.out.println("What-If Analysis Request: " + request);
        Map<String, Object> result = engine.predictWithWhatIf(request.getChanges(), request.getHours(),request.getInvestmentCost());
        if (result == null) {
            return ResponseEntity.status(500).body(Map.of("error", "What-If analysis failed"));
        }
        return ResponseEntity.ok(result);
    }
    
    @Autowired
    private SimulationResultRepository resultRepository;

    // Endpoint for Anomaly Detection (Machine Learning-Based)
    // URL: GET http://localhost:8080/api/anomaly
    // Returns anomaly detection results using trained Linear Regression model
    @GetMapping(value = "/anomaly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnomalyResult> detectAnomaly(
        @RequestParam(value ="windowSize", defaultValue = "32") int windowSize){
         // Validate window size (only allow supported values)
        if (windowSize != 32 && windowSize != 64 && windowSize != 96) {
            windowSize = 32; // Default to 24 hours if invalid
        }

        // Get current dashboard data (contains real and simulated power)
        String dashboardJson = engine.getDashboardData();
        
        // Get trained ML model from engine
        LinearRegressionModel regressionModel = engine.getRegressionModel();
        
        // Perform ML-based anomaly detection
        AnomalyResult result = anomalyDetectionService.detectAnomalyWithHistoricalData(
            dashboardJson, 
            regressionModel,
            resultRepository,
            windowSize
        );
        
        // Add window size metadata to result for frontend display
        result.setWindowSize(windowSize);
    

        return ResponseEntity.ok(result);
    }

    
}
