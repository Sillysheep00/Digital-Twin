package com.fyp.digitaltwin.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "simulation_results")
public class SimulationResult {

    @Id
    private String id;

    private String timestamp;
    private double realPower;
    private double simulatedPower;
    private double simulatedPhysicsPower;  
    private double totalEnergy;
    private double outdoorTemp;
    private double avgIndoorTemp;
    private int activeHvacs;

    public SimulationResult() {}

    public SimulationResult(String timestamp, double realPower, double simulatedPower, double totalEnergy, double outdoorTemp, double avgIndoorTemp, int activeHvacs) {
        this.timestamp = timestamp;
        this.realPower = realPower;
        this.simulatedPower = simulatedPower;
        this.totalEnergy = totalEnergy;
        this.outdoorTemp = outdoorTemp;
        this.avgIndoorTemp = avgIndoorTemp;
        this.activeHvacs = activeHvacs;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getRealPower() { return realPower; }
    public void setRealPower(double realPower) { this.realPower = realPower; }

    public double getSimulatedPower() { return simulatedPower; }
    public void setSimulatedPower(double simulatedPower) { this.simulatedPower = simulatedPower; }

    public double getSimulatedPhysicsPower() { return simulatedPhysicsPower; }
    public void setSimulatedPhysicsPower(double simulatedPhysicsPower) { this.simulatedPhysicsPower = simulatedPhysicsPower; }

    public double getTotalEnergy() { return totalEnergy; }
    public void setTotalEnergy(double totalEnergy) { this.totalEnergy = totalEnergy; }

    public double getOutdoorTemp() { return outdoorTemp; }
    public void setOutdoorTemp(double outdoorTemp) { this.outdoorTemp = outdoorTemp; }

    public double getAvgIndoorTemp() { return avgIndoorTemp; }
    public void setAvgIndoorTemp(double avgIndoorTemp) { this.avgIndoorTemp = avgIndoorTemp; }

    public int getActiveHvacs() { return activeHvacs; }
    public void setActiveHvacs(int activeHvacs) { this.activeHvacs = activeHvacs; }
}

