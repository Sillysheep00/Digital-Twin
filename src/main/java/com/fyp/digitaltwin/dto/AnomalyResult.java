package com.fyp.digitaltwin.dto;

import java.util.List;
/**
 * Data Transfer Object for Anomaly Detection Results
 * 
 * Represents the outcome of residual-based anomaly detection,
 * comparing real power consumption against calibrated simulated power.
 * 
 * Used in the Digital Twin system to identify unusual energy consumption patterns
 * that may indicate equipment malfunction, sensor errors, or unexpected usage.
 */
public class AnomalyResult {
    
    private boolean anomalyDetected;
    private double realPower;
    private double simulatedPower;
    private double calibratedSimulatedPower;
    private double residual;
    private double threshold;
    private String severity; // "NORMAL", "WARNING", "CRITICAL"
    private String explanation;
    private List <Integer> timeSteps;
    private List<Double> realPowerHistory; // Historical real(dateset) power
    private List<Double> simulatedPowerHistory;  // Historical raw simulated power
    private List<Double> predictedPowerHistory; // Historical ML-predicted power  
    private List<Double> residuals;            // Historical residuals

    
    
    // Default constructor
    public AnomalyResult() {}
    
    // Full constructor
    public AnomalyResult(boolean anomalyDetected, double realPower, double simulatedPower, 
                        double calibratedSimulatedPower, double residual, double threshold,
                        String severity, String explanation) {
        this.anomalyDetected = anomalyDetected;
        this.realPower = realPower;
        this.simulatedPower = simulatedPower;
        this.calibratedSimulatedPower = calibratedSimulatedPower;
        this.residual = residual;
        this.threshold = threshold;
        this.severity = severity;
        this.explanation = explanation;
    }
    
    // Getters and Setters
    public boolean isAnomalyDetected() {
        return anomalyDetected;
    }
    
    public void setAnomalyDetected(boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }
    
    public double getRealPower() {
        return realPower;
    }
    
    public void setRealPower(double realPower) {
        this.realPower = realPower;
    }
    
    public double getSimulatedPower() {
        return simulatedPower;
    }
    
    public void setSimulatedPower(double simulatedPower) {
        this.simulatedPower = simulatedPower;
    }
    
    public double getCalibratedSimulatedPower() {
        return calibratedSimulatedPower;
    }
    
    public void setCalibratedSimulatedPower(double calibratedSimulatedPower) {
        this.calibratedSimulatedPower = calibratedSimulatedPower;
    }
    
    public double getResidual() {
        return residual;
    }
    
    public void setResidual(double residual) {
        this.residual = residual;
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<Integer> getTimeSteps() {
        return timeSteps;
    }

    public void setTimeSteps(List<Integer> timeSteps) {
        this.timeSteps = timeSteps;
    }

    public List<Double> getRealPowerHistory() { 
        return realPowerHistory; 
    }
    public void setRealPowerHistory(List<Double> realPowerHistory) { 
        this.realPowerHistory = realPowerHistory; 
    }

    public List<Double> getSimulatedPowerHistory() {
        return simulatedPowerHistory;
    }

    public void setSimulatedPowerHistory(List<Double> simulatedPowerHistory) {
        this.simulatedPowerHistory = simulatedPowerHistory;
    }
    
    public List<Double> getPredictedPowerHistory() {
        return predictedPowerHistory;
    }
    
    public void setPredictedPowerHistory(List<Double> predictedPowerHistory) {
        this.predictedPowerHistory = predictedPowerHistory;
    }
    
    public List<Double> getResiduals() {
        return residuals;
    }
    
    public void setResiduals(List<Double> residuals) {
        this.residuals = residuals;
    }

}

