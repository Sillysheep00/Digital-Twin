package com.fyp.digitaltwin.controller;

import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.WhatIfRequest;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.service.AnomalyDetectionService;
import com.fyp.digitaltwin.service.DigitalTwinEngine;
import com.fyp.digitaltwin.service.RegressionTrainingService;
import com.fyp.digitaltwin.service.WeatherService;
import com.fyp.digitaltwin.repository.SimulationResultRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class DigitalTwinController {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinController.class);

    @Autowired
    private DigitalTwinEngine engine;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private WeatherService weatherService;
    
    @Autowired
    private RegressionTrainingService regressionTrainingService;
    
    @Value("${weather.location.name:Unknown}")
    private String locationName;

    // Endpoint: http://localhost:8080/api/status
    @GetMapping("/status")
    public String getStatus() {
        return engine.getLiveStatus();
    }

    // Endpoint: http://localhost:8080/api/validate
    @GetMapping(value = "/validation",produces = MediaType.TEXT_PLAIN_VALUE) //return plain 
    public String getValidation() {
        return engine.getValidationReport();
    }
    // Endpoint: http://localhost:8080/api/dashboard
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE) // Tells browser this is JSON
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
        log.info("What-If Analysis Request: {}", request);
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
    
    // Endpoint for Live Weather Info
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

    // Endpoint for Residual Distribution Plot
    // URL: GET http://localhost:8080/api/model/residuals
    // Returns all residual data points for histogram computation on frontend
    @GetMapping(value = "/model/residuals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getResiduals() {
        List<Map<String, Object>> allResiduals = regressionTrainingService.getResidualData();
        int total = allResiduals.size();

        // Compute summary stats server-side
        double rawSum = 0, calSum = 0;
        for (Map<String, Object> r : allResiduals) {
            rawSum += ((Number) r.get("rawResidual")).doubleValue();
            calSum += ((Number) r.get("calibratedResidual")).doubleValue();
        }
        double rawMean = total > 0 ? rawSum / total : 0;
        double calMean = total > 0 ? calSum / total : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("residuals", allResiduals);
        response.put("totalSamples", total);
        response.put("rawMean", Math.round(rawMean * 100.0) / 100.0);
        response.put("calibratedMean", Math.round(calMean * 100.0) / 100.0);
        return ResponseEntity.ok(response);
    }

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
    
}
