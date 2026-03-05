package com.fyp.digitaltwin.dto;

/**
 * Data Transfer Object representing a trained Linear Regression model.
 * 
 * This model learns the relationship between simulated and real power consumption:
 *     realPower ≈ slope × simulatedPower + intercept
 * 
 */
public class LinearRegressionModel {
    
    // Model coefficients learned from training data
    private double slope;        // 'a' in: y = a*x + b
    private double intercept;    // 'b' in: y = a*x + b
    
    // Model quality metrics
    private double rSquared;     // Coefficient of determination (0-1, higher is better)
    private double rmse;         // Root Mean Squared Error (lower is better)
    private int trainingSize;    // Number of samples used for training
    
    // Metadata
    private String trainedDate;
    private boolean isValid;
    
    public LinearRegressionModel() {
        this.isValid = false;
    }
    
    public LinearRegressionModel(double slope, double intercept, double rSquared, 
                                 double rmse, int trainingSize, String trainedDate) {
        this.slope = slope;
        this.intercept = intercept;
        this.rSquared = rSquared;
        this.rmse = rmse;
        this.trainingSize = trainingSize;
        this.trainedDate = trainedDate;
        this.isValid = true;
    }
    
    /**
     * Predict real power using the learned linear model.
     * 
     * @param simulatedPower Raw simulated power from physics model
     * @return Predicted real power consumption
     */
    public double predict(double simulatedPower) {
        return slope * simulatedPower + intercept;
    }
    
    // Getters and Setters
    
    public double getSlope() {
        return slope;
    }
    
    public void setSlope(double slope) {
        this.slope = slope;
    }
    
    public double getIntercept() {
        return intercept;
    }
    
    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }
    
    public double getrSquared() {
        return rSquared;
    }
    
    public void setrSquared(double rSquared) {
        this.rSquared = rSquared;
    }
    
    public double getRmse() {
        return rmse;
    }
    
    public void setRmse(double rmse) {
        this.rmse = rmse;
    }
    
    public int getTrainingSize() {
        return trainingSize;
    }
    
    public void setTrainingSize(int trainingSize) {
        this.trainingSize = trainingSize;
    }
    
    public String getTrainedDate() {
        return trainedDate;
    }
    
    public void setTrainedDate(String trainedDate) {
        this.trainedDate = trainedDate;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    @Override
    public String toString() {
        return String.format("LinearRegressionModel{slope=%.4f, intercept=%.4f, R²=%.4f, RMSE=%.4f, samples=%d}",
                slope, intercept, rSquared, rmse, trainingSize);
    }
}

