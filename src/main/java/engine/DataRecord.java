package engine;

public class DataRecord {
    private String date;
    private double powerConsumption;
    private double outdoorTemperature;
    private int occupancy;

    public DataRecord(String date, double powerConsumption, double outdoorTemperature, int occupancy) {
        this.date = date;
        this.powerConsumption = powerConsumption;
        this.outdoorTemperature = outdoorTemperature;
        this.occupancy = occupancy;
    }

    public String getDate() {
        return date;
    }

    public double getPowerConsumption() {
        return powerConsumption;
    }

    public double getOutdoorTemperature() {
        return outdoorTemperature;
    }

    public int getOccupancy() {
        return occupancy;
    }
}
