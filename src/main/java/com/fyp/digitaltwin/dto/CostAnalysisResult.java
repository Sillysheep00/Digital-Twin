package com.fyp.digitaltwin.dto;

/**
 * DTO for Cost Analysis results from What-If scenarios
 * Encapsulates all economic calculations (monetary savings, ROI, payback)
 */
public class CostAnalysisResult {
    
    // Energy savings (input)
    private double energySaved; // kWh
    private int analysisPeriodHours; // hours
    
    // Monetary savings (calculated)
    private double dailyCostSaved;      
    private double monthlyCostSaved;    
    private double annualCostSaved;     
    private double periodCostSaved;     
    
    // ROI metrics (if investment provided)
    private Double investmentCost;      
    private Double paybackPeriodMonths;
    private Double paybackPeriodYears;  
    private Double roiPercentage;      
    
    private double electricityTariff;  
    
    public CostAnalysisResult() {}
    
    public CostAnalysisResult(double energySaved, int analysisPeriodHours, double electricityTariff) {
        this.energySaved = energySaved;
        this.analysisPeriodHours = analysisPeriodHours;
        this.electricityTariff = electricityTariff;
    }
    
    public double getEnergySaved() { return energySaved; }
    public void setEnergySaved(double energySaved) { this.energySaved = energySaved; }
    
    public int getAnalysisPeriodHours() { return analysisPeriodHours; }
    public void setAnalysisPeriodHours(int analysisPeriodHours) { this.analysisPeriodHours = analysisPeriodHours; }
    
    public double getDailyCostSaved() { return dailyCostSaved; }
    public void setDailyCostSaved(double dailyCostSaved) { this.dailyCostSaved = dailyCostSaved; }
    
    public double getMonthlyCostSaved() { return monthlyCostSaved; }
    public void setMonthlyCostSaved(double monthlyCostSaved) { this.monthlyCostSaved = monthlyCostSaved; }
    
    public double getAnnualCostSaved() { return annualCostSaved; }
    public void setAnnualCostSaved(double annualCostSaved) { this.annualCostSaved = annualCostSaved; }
    
    public double getPeriodCostSaved() { return periodCostSaved; }
    public void setPeriodCostSaved(double periodCostSaved) { this.periodCostSaved = periodCostSaved; }
    
    public Double getInvestmentCost() { return investmentCost; }
    public void setInvestmentCost(Double investmentCost) { this.investmentCost = investmentCost; }
    
    public Double getPaybackPeriodMonths() { return paybackPeriodMonths; }
    public void setPaybackPeriodMonths(Double paybackPeriodMonths) { this.paybackPeriodMonths = paybackPeriodMonths; }
    
    public Double getPaybackPeriodYears() { return paybackPeriodYears; }
    public void setPaybackPeriodYears(Double paybackPeriodYears) { this.paybackPeriodYears = paybackPeriodYears; }
    
    public Double getRoiPercentage() { return roiPercentage; }
    public void setRoiPercentage(Double roiPercentage) { this.roiPercentage = roiPercentage; }
    
    public double getElectricityTariff() { return electricityTariff; }
    public void setElectricityTariff(double electricityTariff) { this.electricityTariff = electricityTariff; }
}