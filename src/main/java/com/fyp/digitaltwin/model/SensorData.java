package com.fyp.digitaltwin.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sensor_data")
public class SensorData {
    
    @Id
    private String id;

    @Indexed(unique = true) // Index for fast lookup
    private String date;
    
    private double powerConsumption;
    private double outdoorTemperature;
    private int occupancy;

    // Constructors
    public SensorData() {}

    public SensorData(String date, double powerConsumption, double outdoorTemperature, int occupancy) {
        this.date = date;
        this.powerConsumption = powerConsumption;
        this.outdoorTemperature = outdoorTemperature;
        this.occupancy = occupancy;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getPowerConsumption() { return powerConsumption; }
    public void setPowerConsumption(double powerConsumption) { this.powerConsumption = powerConsumption; }

    public double getOutdoorTemperature() { return outdoorTemperature; }
    public void setOutdoorTemperature(double outdoorTemperature) { this.outdoorTemperature = outdoorTemperature; }

    public int getOccupancy() { return occupancy; }
    public void setOccupancy(int occupancy) { this.occupancy = occupancy; }
}

