package com.fyp.digitaltwin.controller;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.WhatIfRequest;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.dto.ModelMetrics;
import com.fyp.digitaltwin.service.AnomalyDetectionService;
import com.fyp.digitaltwin.service.DigitalTwinEngine;
import com.fyp.digitaltwin.service.WeatherService;
import com.fyp.digitaltwin.repository.SimulationResultRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class DigitalTwinController {

    @Autowired
    private DigitalTwinEngine engine;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private WeatherService weatherService;
    
    @Value("${weather.location.name:Unknown}")
    private String locationName;

    // Endpoint: http://localhost:8080/api/status
    @GetMapping("/status")
    public String getStatus() {
        return engine.getLiveStatus();
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
    
    // Endpoint for Live Weather Info (Display Only)
    // URL: GET http://localhost:8080/api/weather/live
    // Returns current live weather data for dashboard display
    @GetMapping(value = "/weather/live", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLiveWeather() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get live temperature (with fallback)
            double liveTemp = weatherService.getLiveOutdoorTemperature(null);
            String cacheStatus = weatherService.getCacheStatus();
            
            response.put("temperature", liveTemp);
            response.put("location", locationName);
            response.put("cacheStatus", cacheStatus);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to fetch live weather");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 在 DigitalTwinController.java 中添加
    @GetMapping("/weather/status")
    public ResponseEntity<Map<String, Object>> getWeatherStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            double temp = weatherService.getLiveOutdoorTemperature(null);
            String cacheStatus = weatherService.getCacheStatus();
            
            status.put("temperature", temp);
            status.put("cacheStatus", cacheStatus);
            status.put("apiWorking", true);
            status.put("message", "Weather API is working");
        } catch (Exception e) {
            status.put("apiWorking", false);
            status.put("error", e.getMessage());
            status.put("message", "Weather API failed: " + e.getMessage());
        }
        return ResponseEntity.ok(status);
    }
    
    // Endpoint for Model Metrics (R² and RMSE values)
    // URL: GET http://localhost:8080/api/model/metrics
    // Returns baseline and calibrated R² and RMSE values
    @GetMapping(value = "/model/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ModelMetrics> getModelMetrics() {
        ModelMetrics metrics = engine.getModelMetrics();
        if (metrics == null) {
            return ResponseEntity.status(503).body(null); // Service Unavailable - model not trained yet
        }
        return ResponseEntity.ok(metrics);
    }
}
