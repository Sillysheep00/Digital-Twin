package com.fyp.digitaltwin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

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
            log.debug("Using cached temperature: {}°C", cachedTemperature);
            return cachedTemperature;
        }
        
        try {
            // Fetch live temperature from OpenWeatherMap API
            double liveTemp = fetchLiveTemperature();
            
            // Update cache
            cachedTemperature = liveTemp;
            cacheExpiry = LocalDateTime.now().plusMinutes(cacheMinutes);
            
            log.info("Fetched LIVE outdoor temperature: {}°C (cached for {} min)", liveTemp, cacheMinutes);
            return liveTemp;

        } catch (Exception e) {
            log.warn("Weather API failed: {}", e.getMessage());

            // Fallback to historical CSV data if enabled
            if (fallbackEnabled) {
                return getFallbackTemperature(currentDate);
            } else {
                log.warn("Fallback disabled. Using default temperature: 15°C");
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
            log.info("Falling back to historical CSV data for date: {}", currentDate);

            // Query MongoDB for the matching date
            SensorData data = sensorDataRepository.findByDate(currentDate);

            if (data != null) {
                double historicalTemp = data.getOutdoorTemperature();
                log.info("Using historical CSV temperature: {}°C", historicalTemp);
                return historicalTemp;
            } else {
                log.warn("No historical data found for date: {}. Using default 15°C", currentDate);
                return 15.0;
            }

        } catch (Exception e) {
            log.warn("Fallback failed: {}. Using default 15°C", e.getMessage());
            return 15.0;
        }
    }
    
    /**
     * Clears the temperature cache (useful for testing)
     */
    public void clearCache() {
        cachedTemperature = null;
        cacheExpiry = null;
        log.info("Weather cache cleared");
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
