package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fyp.digitaltwin.repository.SensorDataRepository;
import com.fyp.digitaltwin.model.SensorData;

import java.time.LocalDateTime;

/**
 * WeatherService - Fetches live outdoor temperature from OpenWeatherMap API
 * 
 * Features:
 * - Fetches current temperature for configured location
 * - Caches temperature for 10 minutes to reduce API calls
 * - Automatic fallback to CSV historical data on API failure
 * - Transforms system from simulation to true digital twin
 */
@Service
public class WeatherService {
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    @Value("${weather.api.url}")
    private String apiUrl;
    
    @Value("${weather.location.lat}")
    private String latitude;
    
    @Value("${weather.location.lon}")
    private String longitude;
    
    @Value("${weather.cache.minutes:10}")
    private int cacheMinutes;
    
    @Value("${weather.fallback.enabled:true}")
    private boolean fallbackEnabled;
    
    @Autowired
    private SensorDataRepository sensorDataRepository;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Cache variables
    private Double cachedTemperature = null;
    private LocalDateTime cacheExpiry = null;
    
    // Constructor
    public WeatherService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gets live outdoor temperature with caching and fallback
     * 
     * @param currentDate Current simulation date (used for fallback lookup)
     * @return Outdoor temperature in Celsius
     */
    public double getLiveOutdoorTemperature(String currentDate) {
        // Check if cached temperature is still valid
        if (isCacheValid()) {
            System.out.println("Using cached temperature: " + cachedTemperature + "°C");
            return cachedTemperature;
        }
        
        try {
            // Fetch live temperature from OpenWeatherMap API
            double liveTemp = fetchLiveTemperature();
            
            // Update cache
            cachedTemperature = liveTemp;
            cacheExpiry = LocalDateTime.now().plusMinutes(cacheMinutes);
            
            System.out.println("Fetched LIVE outdoor temperature: " + liveTemp + "°C (cached for " + cacheMinutes + " min)");
            return liveTemp;
            
        } catch (Exception e) {
            System.err.println("Weather API failed: " + e.getMessage());
            
            // Fallback to historical CSV data if enabled
            if (fallbackEnabled) {
                return getFallbackTemperature(currentDate);
            } else {
                // If fallback disabled, use a default temperature
                System.err.println("Fallback disabled. Using default temperature: 15°C");
                return 15.0;
            }
        }
    }
    
    /**
     * Checks if cached temperature is still valid
     */
    private boolean isCacheValid() {
        if (cachedTemperature == null || cacheExpiry == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(cacheExpiry);
    }
    
    /**
     * Fetches live temperature from OpenWeatherMap API
     * 
     * API Endpoint: https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={apiKey}&units=metric
     * 
     * @return Temperature in Celsius
     * @throws Exception if API call fails
     */
    private double fetchLiveTemperature() throws Exception {
        String url = String.format("%s?lat=%s&lon=%s&appid=%s&units=metric",
            apiUrl, latitude, longitude, apiKey);
        
        // Make HTTP request to OpenWeatherMap API
        String response = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        if (response == null || response.isEmpty()) {
            throw new Exception("Empty response from Weather API");
        }
        
        // Parse JSON response
        JsonNode root = objectMapper.readTree(response);
        
        // Extract temperature from response
        // Response structure: { "main": { "temp": 15.5, ... }, ... }
        JsonNode mainNode = root.path("main");
        if (mainNode.isMissingNode()) {
            throw new Exception("Invalid response structure: 'main' node not found");
        }
        
        JsonNode tempNode = mainNode.path("temp");
        if (tempNode.isMissingNode()) {
            throw new Exception("Invalid response structure: 'temp' node not found");
        }
        
        double temperature = tempNode.asDouble();
        
        // Validation: Ensure temperature is within reasonable range
        if (temperature < -50 || temperature > 60) {
            throw new Exception("Temperature out of reasonable range: " + temperature + "°C");
        }
        
        return temperature;
    }
    
    /**
     * Fallback: Get historical temperature from CSV data stored in MongoDB
     * Uses the same date from the simulation to find matching historical temperature
     * 
     * @param currentDate Date to look up in historical data
     * @return Historical outdoor temperature
     */
    private double getFallbackTemperature(String currentDate) {
        try {
            System.out.println("Falling back to historical CSV data for date: " + currentDate);
            
            // Query MongoDB for the matching date
            SensorData data = sensorDataRepository.findByDate(currentDate);
            
            if (data != null) {
                double historicalTemp = data.getOutdoorTemperature();
                System.out.println("Using historical CSV temperature: " + historicalTemp + "°C");
                return historicalTemp;
            } else {
                // If no matching date found, use a default
                System.err.println("No historical data found for date: " + currentDate + ". Using default 15°C");
                return 15.0;
            }
            
        } catch (Exception e) {
            System.err.println("Fallback failed: " + e.getMessage() + ". Using default 15°C");
            return 15.0;
        }
    }
    
    /**
     * Clears the temperature cache (useful for testing)
     */
    public void clearCache() {
        cachedTemperature = null;
        cacheExpiry = null;
        System.out.println("Weather cache cleared");
    }
    
    /**
     * Gets cache status for monitoring/debugging
     */
    public String getCacheStatus() {
        if (isCacheValid()) {
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), cacheExpiry).toMinutes();
            return String.format("Cache valid: %.1f°C (expires in %d minutes)", cachedTemperature, minutesRemaining);
        } else {
            return "Cache expired or empty";
        }
    }
}
