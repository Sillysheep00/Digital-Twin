package com.fyp.digitaltwin.dto;

import java.util.Map;

/**
 * DTO for What-If Analysis requests
 * Allows users to test different scenarios by modifying model parameters
 */
public class WhatIfRequest {
    private Map<String, Object> changes;  // Parameters to modify (e.g., {"targetTemp": 21.0, "insulation": 0.03})
    private int hours;            // Prediction horizon in hours
    private Double investmentCost;         

    // Constructors
    public WhatIfRequest() {}

    public WhatIfRequest(Map<String, Object> changes, int hours) {
        this.changes = changes;
        this.hours = hours;
    }

    public WhatIfRequest(Map<String, Object> changes, int hours,Double investmentCost) {
        this.changes = changes;
        this.hours = hours;
        this.investmentCost = investmentCost;
    }

    // Getters and Setters
    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public Double getInvestmentCost() {
        return investmentCost;
    }
    
    public void setInvestmentCost(Double investmentCost) {
        this.investmentCost = investmentCost;
    }

    @Override
    public String toString() {
        return "WhatIfRequest{" +
                "changes=" + changes +
                ", hours=" + hours +
                ", investmentCost=" + investmentCost +
                '}';
    }
}

