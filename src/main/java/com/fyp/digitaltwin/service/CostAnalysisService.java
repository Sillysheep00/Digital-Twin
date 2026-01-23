package com.fyp.digitaltwin.service;

import com.fyp.digitaltwin.dto.CostAnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Service responsible for economic cost analysis of What-If scenarios.
 * 
 * Converts energy savings (kWh) into monetary savings using UK electricity pricing
 * and calculates ROI metrics for building upgrade investments.
 */
@Service
public class CostAnalysisService {
    
    // UK Electricity Tariff (default: £0.30/kWh - UK average 2024)
    private static final double DEFAULT_UK_ELECTRICITY_TARIFF = 0.30; // £/kWh
    
    /**
     * Performs comprehensive cost analysis for a What-If scenario
     * 
     * @param energySaved Energy savings in kWh (baseline - scenario)
     * @param analysisPeriodHours Duration of analysis period in hours
     * @param investmentCost Optional capital investment cost in £ (null if not provided)
     * @return CostAnalysisResult with all monetary and ROI metrics
     */
    public CostAnalysisResult analyzeCosts(double energySaved, int analysisPeriodHours, Double investmentCost) {
        return analyzeCosts(energySaved, analysisPeriodHours, investmentCost, DEFAULT_UK_ELECTRICITY_TARIFF);
    }
    
    /**
     * Performs comprehensive cost analysis with custom electricity tariff
     * 
     * @param energySaved Energy savings in kWh
     * @param analysisPeriodHours Duration of analysis period in hours
     * @param investmentCost Optional capital investment cost in £
     * @param electricityTariff Electricity price in £/kWh
     * @return CostAnalysisResult with all monetary and ROI metrics
     */
    public CostAnalysisResult analyzeCosts(double energySaved, int analysisPeriodHours, 
                                           Double investmentCost, double electricityTariff) {
        
        CostAnalysisResult result = new CostAnalysisResult(energySaved, analysisPeriodHours, electricityTariff);
       
        //Monetary Saving Calculations

        // Period cost savings (for the analysis duration)
        double periodCostSaved = energySaved * electricityTariff;
        result.setPeriodCostSaved(Math.round(periodCostSaved * 100.0) / 100.0);
        
        // Daily cost savings
        // If analysis period is less than 24 hours, extrapolate
        double dailyEnergySaved = (analysisPeriodHours < 24) 
            ? energySaved * (24.0 / analysisPeriodHours)
            : energySaved / (analysisPeriodHours / 24.0);
        double dailyCostSaved = dailyEnergySaved * electricityTariff;
        result.setDailyCostSaved(Math.round(dailyCostSaved * 100.0) / 100.0);
        
        // Monthly cost savings (30 days)
        double monthlyCostSaved = dailyCostSaved * 30.0;
        result.setMonthlyCostSaved(Math.round(monthlyCostSaved * 100.0) / 100.0);
        
        // Annual cost savings (365 days)
        double annualCostSaved = dailyCostSaved * 365.0;
        result.setAnnualCostSaved(Math.round(annualCostSaved * 100.0) / 100.0);
        
       //ROI & Payback Period Calculation
        if (investmentCost != null && investmentCost > 0) {
            result.setInvestmentCost(investmentCost);
            
            if(annualCostSaved > 0){
                // Payback period in months
                double paybackMonths = (investmentCost / monthlyCostSaved);
                result.setPaybackPeriodMonths(Math.round(paybackMonths * 100.0) / 100.0);
                
                // Payback period in years
                double paybackYears = paybackMonths / 12.0;
                result.setPaybackPeriodYears(Math.round(paybackYears * 100.0) / 100.0);
                
                // ROI percentage (annual savings / investment * 100)
                // This represents the annual return on investment
                double roi = (annualCostSaved / investmentCost) * 100.0;
                result.setRoiPercentage(Math.round(roi * 100.0) / 100.0);
            }
        }
        return result;
    }
    
    /**
     * Gets the default UK electricity tariff
     * @return Tariff in £/kWh
     */
    public double getDefaultTariff() {
        return DEFAULT_UK_ELECTRICITY_TARIFF;
    }
}