package com.fyp.digitaltwin.dto;

/**
 * Data Transfer Object for ML Model Metrics
 * Contains baseline and calibrated R² and RMSE values for comparison
 */
public class ModelMetrics {
    // Baseline metrics (no calibration)
    private double baselineRSquared;
    private double baselineRMSE;
    
    // Calibrated metrics (with ML model)
    private double calibratedRSquared;
    private double calibratedRMSE;
    
    // Improvement metrics
    private double rSquaredDifference;
    private double rmseDifference;
    private double rmseImprovementPercent;
    
    // Model parameters
    private double slope;
    private double intercept;
    private int trainingSize;
    private String trainedDate;
    private boolean isValid;
    
    // Constructors
    public ModelMetrics() {}
    
    // Getters and Setters
    public double getBaselineRSquared() { return baselineRSquared; }
    public void setBaselineRSquared(double baselineRSquared) { this.baselineRSquared = baselineRSquared; }
    
    public double getBaselineRMSE() { return baselineRMSE; }
    public void setBaselineRMSE(double baselineRMSE) { this.baselineRMSE = baselineRMSE; }
    
    public double getCalibratedRSquared() { return calibratedRSquared; }
    public void setCalibratedRSquared(double calibratedRSquared) { this.calibratedRSquared = calibratedRSquared; }
    
    public double getCalibratedRMSE() { return calibratedRMSE; }
    public void setCalibratedRMSE(double calibratedRMSE) { this.calibratedRMSE = calibratedRMSE; }
    
    public double getRSquaredDifference() { return rSquaredDifference; }
    public void setRSquaredDifference(double rSquaredDifference) { this.rSquaredDifference = rSquaredDifference; }
    
    public double getRmseDifference() { return rmseDifference; }
    public void setRmseDifference(double rmseDifference) { this.rmseDifference = rmseDifference; }
    
    public double getRmseImprovementPercent() { return rmseImprovementPercent; }
    public void setRmseImprovementPercent(double rmseImprovementPercent) { this.rmseImprovementPercent = rmseImprovementPercent; }
    
    public double getSlope() { return slope; }
    public void setSlope(double slope) { this.slope = slope; }
    
    public double getIntercept() { return intercept; }
    public void setIntercept(double intercept) { this.intercept = intercept; }
    
    public int getTrainingSize() { return trainingSize; }
    public void setTrainingSize(int trainingSize) { this.trainingSize = trainingSize; }
    
    public String getTrainedDate() { return trainedDate; }
    public void setTrainedDate(String trainedDate) { this.trainedDate = trainedDate; }
    
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
}
