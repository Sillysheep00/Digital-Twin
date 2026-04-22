package com.fyp.digitaltwin.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.digitaltwin.dto.AnomalyResult;
import com.fyp.digitaltwin.dto.LinearRegressionModel;
import com.fyp.digitaltwin.dto.RoomAnomaly;
import com.fyp.digitaltwin.model.SimulationResult;
import com.fyp.digitaltwin.repository.SimulationResultRepository;


@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private static final double Z_SCORE_THRESHOLD = 3.0; // 3-sigma rule (99.7% confidence)
    private static final double MIN_ABSOLUTE_THRESHOLD = 5.0; // Ignore residuals < 5 kW
    private static final int ROLLING_WINDOW_SIZE = 32; // Last 32 steps = 8 hours (15-min intervals)

    // Autowire repository for Z-score calculation
    @Autowired
    private SimulationResultRepository resultRepository;
    
    
    /**
     * @param realPower Actual power consumption from sensors (kW)
     * @param simulatedPower Raw simulated power from physics model (kW)
     * @param regressionModel Trained Linear Regression model (learned from data)
     * @return AnomalyResult containing detection outcome and diagnostic information
     */
    public AnomalyResult detectAnomalyWithML(double realPower, double simulatedPower, 
                                              LinearRegressionModel regressionModel
                                            ) {
        
        //1. ML Prediciton
        //Use the trained model to predict expected power consumption.
        //The slope ,intercept were learned from historical data 
        double predictedPower = regressionModel.predict(simulatedPower);
        
        //2: Calculate Residual (Prediction Error)
        // Residual = difference between reality and ML prediction.
        // Measures how well earned model generalizes to current data.
        double residual = Math.abs(realPower - predictedPower);
    
        //3. Get Historical Residuals for Rolling Statistics
        List<Double> historicalResiduals = getHistoricalResiduals(
            regressionModel, 
            ROLLING_WINDOW_SIZE
        );
    
        historicalResiduals.add(residual);
    
        //4. Calculate Rolling Mean and Standard Deviation
        double meanResidual = calculateMean(historicalResiduals);
        double stdResidual = calculateStandardDeviation(historicalResiduals, meanResidual);
        
        //5. Calculate Z-Score
        double zScore = 0.0;
        boolean hasEnoughData = historicalResiduals.size() >= 10 && stdResidual > 0.1;
        
        if (hasEnoughData) {
            zScore = (residual - meanResidual) / stdResidual;
        } else {
            // Fallback: Use simple threshold if not enough historical data
            double fallbackThreshold = predictedPower * 0.25; // Original method
            zScore = (residual > fallbackThreshold) ? 4.0 : 0.0; // Treat as anomaly if exceeds fallback
        }

        //6. Anomaly Decision (Z-Score + Minimum Absolute Threshold)
        boolean anomalyDetected = false;
        
        // Rule 1: Ignore tiny residuals (noise filtering)
        if (residual < MIN_ABSOLUTE_THRESHOLD) {
            anomalyDetected = false;
        }
        // Rule 2: Z-score threshold (statistical significance)
        else if (hasEnoughData && Math.abs(zScore) > Z_SCORE_THRESHOLD) {
            anomalyDetected = true;
        }
        // Rule 3: Fallback for insufficient data
        else if (!hasEnoughData && residual > predictedPower * 0.25) {
            anomalyDetected = true;
        }
        
        //7. Classify Severity
        String severity;
        String explanation;
        
        if (!hasEnoughData) {
            severity = "NORMAL";
            explanation = "Insufficient historical data for statistical analysis. Using fallback threshold.";
        } else if (residual < MIN_ABSOLUTE_THRESHOLD) {
            severity = "NORMAL";
            explanation = String.format(
                "Residual (%.2f kW) is below minimum threshold (%.2f kW). Normal operation.",
                residual, MIN_ABSOLUTE_THRESHOLD
            );
        } else if (Math.abs(zScore) >= 3.0) {
            severity = "CRITICAL";
            explanation = String.format(
                "CRITICAL ANOMALY: Z-score = %.2f (residual = %.2f kW, mean = %.2f kW, std = %.2f kW). " +
                "This is %.1f standard deviations from normal. Possible equipment failure or sensor error.",
                zScore, residual, meanResidual, stdResidual, Math.abs(zScore)
            );
        } else if (Math.abs(zScore) >= 2.0) {
            severity = "WARNING";
            explanation = String.format(
                "WARNING: Z-score = %.2f (residual = %.2f kW, mean = %.2f kW, std = %.2f kW). " +
                "This is %.1f standard deviations from normal. Monitor for potential issues.",
                zScore, residual, meanResidual, stdResidual, Math.abs(zScore)
            );
        } else {
            severity = "NORMAL";
            explanation = String.format(
                "Normal operation. Z-score = %.2f (residual = %.2f kW, mean = %.2f kW, std = %.2f kW).",
                zScore, residual, meanResidual, stdResidual
            );
        }

        //8. Build Result
        AnomalyResult result = new AnomalyResult();
        result.setAnomalyDetected(anomalyDetected);
        result.setRealPower(Math.round(realPower * 100.0) / 100.0);
        result.setSimulatedPower(Math.round(simulatedPower * 100.0) / 100.0);
        result.setCalibratedSimulatedPower(Math.round(predictedPower * 100.0) / 100.0);
        result.setResidual(Math.round(residual * 100.0) / 100.0);
        result.setThreshold(Math.round((meanResidual + Z_SCORE_THRESHOLD * stdResidual) * 100.0) / 100.0);
        result.setSeverity(severity);
        result.setExplanation(explanation);
        result.setZScore(zScore);  

        return result;
    }

    /**
     * Detect anomaly using current dashboard data and ML model.
     * Extracts power values from dashboard JSON and performs ML-based detection.
     * 
     * @param dashboardJson JSON string from dashboard API
     * @param regressionModel Trained Linear Regression model
     * @return AnomalyResult
     */
    public AnomalyResult detectAnomalyFromDashboard(String dashboardJson, LinearRegressionModel regressionModel) {
        try {
            // Parse JSON to extract power values
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(dashboardJson);
            
            double realPower = root.path("power").path("real").asDouble();
            double simulatedPower = root.path("power").path("simulated_raw").asDouble();
            
            // Use ML model for detection
            return detectAnomalyWithML(realPower, simulatedPower, regressionModel);
            
        } catch (Exception e) {
            // Return error result if parsing fails
            AnomalyResult errorResult = new AnomalyResult();
            errorResult.setAnomalyDetected(false);
            errorResult.setSeverity("ERROR");
            errorResult.setExplanation("Failed to parse dashboard data: " + e.getMessage());
            return errorResult;
        }
    }

    public AnomalyResult detectAnomalyWithHistoricalData(
        String dashboardJson, 
        LinearRegressionModel regressionModel, 
        SimulationResultRepository resultRepository, 
        int numberOfSteps) {
        
        // Get current anomaly detection result
        AnomalyResult result = detectAnomalyFromDashboard(dashboardJson, regressionModel);
     
        // First get building-level Z-score and severity
        double buildingZScore = result.getZScore() != null ? result.getZScore() : 0.0;
        String buildingSeverity = result.getSeverity();

        // Calculate building Z-score if we have historical data
        if (result.getResiduals() != null && result.getResiduals().size() > 0) {
            List<Double> residuals = result.getResiduals();
            double mean = residuals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double std = Math.sqrt(
                residuals.stream()
                    .mapToDouble(r -> Math.pow(r - mean, 2))
                    .average()
                    .orElse(0.0)
            );
            if (std > 0.1) {
                buildingZScore = (result.getResidual() - mean) / std;
            }
        }

        List<RoomAnomaly> roomAnomalies = detectRoomAnomalies(
            dashboardJson, 
            regressionModel, 
            result.getThreshold(),
            buildingZScore,
            buildingSeverity
        );
        result.setRoomAnomalies(roomAnomalies);
        
        try{
            //Query recent simulation results
            List<SimulationResult> recentResults = resultRepository.findAll(Sort.by(Sort.Direction.DESC,"timestamp")
            ).stream().limit(numberOfSteps).sorted(Comparator.comparing(SimulationResult::getTimestamp)).collect(Collectors.toList());

            log.info("Found {} historical simulation results for charts", recentResults.size());

            // Build chart data arrays
            List<Integer> timeSteps = new ArrayList<>();
            List<String> timestamps = new ArrayList<>();
            List<Double> realPowerList = new ArrayList<>(); 
            List<Double> simulatedPowerList = new ArrayList<>();
            List<Double> simulatedPhysicsPowerList = new ArrayList<>();
            List<Double> predictedPowerList = new ArrayList<>();
            List<Double> residualsList = new ArrayList<>();

            for(int i = 0; i < recentResults.size();i++){
                SimulationResult sr = recentResults.get(i);

                //Time step index
                timeSteps.add(i + 1);
                
                 // Extract and format timestamp (format: "HH:mm")
                String timestamp = sr.getTimestamp();
                if (timestamp != null && !timestamp.isEmpty()) {
                    try {
                        // Parse timestamp 
                        String[] parts = timestamp.split(" ");
                        if (parts.length > 1) {
                            String timePart = parts[1].substring(0, 5); // Extract "HH:mm"
                            timestamps.add(timePart);
                        } else {
                            timestamps.add(timestamp.substring(0, Math.min(5, timestamp.length())));
                        }
                    } catch (Exception e) {
                        // Fallback: use step number if timestamp parsing fails
                        timestamps.add(String.format("%02d:00", (i % 24)));
                    }
                } else {
                    // Fallback if no timestamp
                    timestamps.add(String.format("%02d:00", (i % 24)));
                }


                realPowerList.add(Math.round(sr.getRealPower() * 100.0) / 100.0);
 
                // 1. Get raw simulated power  (fast-estimation)
                double rawSimulated = sr.getSimulatedPower();
                simulatedPowerList.add(Math.round(rawSimulated * 100.0) / 100.0);

                // Get physics-based simulated power
                double physicsSimulated = sr.getSimulatedPhysicsPower();
                simulatedPhysicsPowerList.add(Math.round(physicsSimulated * 100.0) / 100.0);

                // 2. Calculate ML- Prediction (Forward Calculation)
                // predicted = raw * slope + intercept
                double predicted = regressionModel.predict(rawSimulated);
                predictedPowerList.add(Math.round(predicted * 100.0) / 100.0);
    
                // 3. Calculate residual
                // Residual = |Real - Predicted|
                double residual = Math.abs(sr.getRealPower() - predicted);
                residualsList.add(Math.round(residual * 100.0) / 100.0);
            }
            // Set chart data in result
            result.setTimeSteps(timeSteps);
            result.setTimestamps(timestamps);
            result.setRealPowerHistory(realPowerList);
            result.setSimulatedPowerHistory(simulatedPowerList);
            result.setSimulatedPhysicsPowerHistory(simulatedPhysicsPowerList);  // NEW
            result.setPredictedPowerHistory(predictedPowerList);
            result.setResiduals(residualsList);

            log.info("Chart data prepared: {} data points", timeSteps.size());

        } catch (Exception e) {
            log.error("Error collecting historical data for charts: {}", e.getMessage(), e);
            // Initialize empty arrays so frontend doesn't break
            result.setTimeSteps(new ArrayList<>());
            result.setSimulatedPowerHistory(new ArrayList<>());
            result.setSimulatedPhysicsPowerHistory(new ArrayList<>());  // NEW
            result.setPredictedPowerHistory(new ArrayList<>());
            result.setResiduals(new ArrayList<>());
        }
        return result;
    }

    public List<RoomAnomaly> detectRoomAnomalies(
        String dashboardJson, 
        LinearRegressionModel regressionModel,
        double buildingThreshold,
        double buildingZScore,
        String buildingSeverity){

        List<RoomAnomaly> roomAnomalies = new ArrayList<>();
    
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(dashboardJson);
            
            //1.Get building-level values
            double totalRealPower = root.path("power").path("real").asDouble();
            double totalSimulatedPower = root.path("power").path("simulated_raw").asDouble();
            double totalPredictedPower = root.path("power").path("simulated").asDouble();
            
            //2.Get rooms array
            JsonNode roomsArray = root.path("rooms");
            if (!roomsArray.isArray() || roomsArray.size() == 0) {
                return roomAnomalies; // No rooms to analyze
            }
        
            //3.Calculate total simulated power from all rooms for proportional allocation
            double totalRoomSimulatedPower = 0.0;
            List<Double> roomSimulatedPowers = new ArrayList<>();
            
            for (JsonNode room : roomsArray) {
                double roomSimPower = room.path("power").asDouble();
                roomSimulatedPowers.add(roomSimPower);
                totalRoomSimulatedPower += roomSimPower;
            }
            
            // If no room power, return empty list
            if (totalRoomSimulatedPower == 0) {
                return roomAnomalies;
            }

             // Calculate per-room anomalies
            int roomIndex = 0;
            for (JsonNode room : roomsArray) {
                RoomAnomaly roomAnomaly = new RoomAnomaly();
                
                String roomName = room.path("name").asText("Unknown");
                String roomId = room.path("id").asText("");
                double roomSimulatedPower = roomSimulatedPowers.get(roomIndex);
                
                roomAnomaly.setRoomName(roomName);
                roomAnomaly.setRoomId(roomId);
                roomAnomaly.setSimulatedPower(Math.round(roomSimulatedPower * 100.0) / 100.0);
                
                // Allocate real power proportionally
                double allocationRatio = roomSimulatedPower / totalRoomSimulatedPower;
                double allocatedRealPower = totalRealPower * allocationRatio;
                roomAnomaly.setAllocatedRealPower(Math.round(allocatedRealPower * 100.0) / 100.0);
                
                // Calculate predicted power for this room (proportional to building prediction)
                double roomPredictedPower = totalPredictedPower * allocationRatio;
                roomAnomaly.setPredictedPower(Math.round(roomPredictedPower * 100.0) / 100.0);
                
                // Calculate residual
                double residual = Math.abs(allocatedRealPower - roomPredictedPower);
                roomAnomaly.setResidual(Math.round(residual * 100.0) / 100.0);
                
                // Use absolute threshold based on room's predicted power, not proportional building threshold
                double roomThreshold = Math.max(MIN_ABSOLUTE_THRESHOLD, roomPredictedPower * 0.25);

                roomAnomaly.setThreshold(Math.round(roomThreshold * 100.0) / 100.0);
                
                // Determine anomaly status - INDEPENDENT of building severity
                boolean isAnomaly = false;
                String severity = "NORMAL";
                String status = "🟢 Normal";

                // Rule 1: If building is NORMAL, all rooms are NORMAL
                if ("NORMAL".equals(buildingSeverity)) {
                    // No room-level evaluation needed when building is normal
                    isAnomaly = false;
                    severity = "NORMAL";
                    status = "🟢 Normal";
                }

                // Rule 2: If building is WARNING or CRITICAL, evaluate room-level anomalies
                else {
                    // Ignore tiny residuals (noise filtering)
                    if (residual < MIN_ABSOLUTE_THRESHOLD * 0.5) {
                        // Very small residuals are always normal
                        isAnomaly = false;
                        severity = "NORMAL";
                        status = "🟢 Normal";
                    }
                    // Check if room residual exceeds its own threshold
                    else if (residual > roomThreshold) {
                        isAnomaly = true;
                        // Classify severity based on how far above threshold
                        if (residual > roomThreshold * 2.0) {
                            severity = "CRITICAL";
                            status = "🔴 Anomaly";
                        } else {
                            severity = "WARNING";
                            status = "🟠 Slight";
                        }
                    }
                // Rule 3: Room is normal
                else {
                    isAnomaly = false;
                    severity = "NORMAL";
                    status = "🟢 Normal";
                }
            }
                roomAnomaly.setAnomalyDetected(isAnomaly);
                roomAnomaly.setSeverity(severity);
                roomAnomaly.setStatus(status);
                
                roomAnomalies.add(roomAnomaly);
                roomIndex++;
        }
        } catch (Exception e) {
            log.error("Error calculating room anomalies: {}", e.getMessage(), e);
        }      
        return roomAnomalies;
    }

    

    //Helper method : Get historical residuals from recent simulation results
     private List<Double> getHistoricalResiduals(
        LinearRegressionModel regressionModel,
        int windowSize) {
        
        List<Double> residuals = new ArrayList<>();
        
        try {
            List<SimulationResult> recentResults = resultRepository
                .findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .limit(windowSize)
                .sorted(Comparator.comparing(SimulationResult::getTimestamp))
                .collect(Collectors.toList());
            
            for (SimulationResult sr : recentResults) {
                double predicted = regressionModel.predict(sr.getSimulatedPower());
                double residual = Math.abs(sr.getRealPower() - predicted);
                residuals.add(residual);
            }
        } catch (Exception e) {
            log.warn("Error fetching historical residuals: {}", e.getMessage());
        }
        
        return residuals;
    }

    /**
     * Helper: Calculate mean of a list
     */
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Helper: Calculate standard deviation
     */
    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() < 2) return 1.0; // Avoid division by zero
        
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
}

