package com.fyp.digitaltwin.dto;

public class RoomAnomaly{
    private String roomName;
    private String roomId;
    private double simulatedPower;
    private double allocatedRealPower;  // Proportionally allocated from total
    private double predictedPower;      // ML-predicted for this room
    private double residual;
    private double threshold;
    private boolean anomalyDetected;
    private String severity; // "NORMAL", "WARNING", "CRITICAL"
    private String status;   // " Normal", " Slight", "Anomaly"
    
    // Default constructor
    public RoomAnomaly() {}

    // Getters and Setters
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    
    public double getSimulatedPower() { return simulatedPower; }
    public void setSimulatedPower(double simulatedPower) { this.simulatedPower = simulatedPower; }
    
    public double getAllocatedRealPower() { return allocatedRealPower; }
    public void setAllocatedRealPower(double allocatedRealPower) { this.allocatedRealPower = allocatedRealPower; }
    
    public double getPredictedPower() { return predictedPower; }
    public void setPredictedPower(double predictedPower) { this.predictedPower = predictedPower; }
    
    public double getResidual() { return residual; }
    public void setResidual(double residual) { this.residual = residual; }
    
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    
    public boolean isAnomalyDetected() { return anomalyDetected; }
    public void setAnomalyDetected(boolean anomalyDetected) { this.anomalyDetected = anomalyDetected; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    

}