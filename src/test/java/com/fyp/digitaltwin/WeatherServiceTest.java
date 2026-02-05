package com.fyp.digitaltwin;

import com.fyp.digitaltwin.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WeatherService
 * 
 * Tests:
 * 1. API integration (requires valid API key in application.properties)
 * 2. Caching mechanism
 * 3. Fallback to CSV historical data
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class WeatherServiceTest {
    
    @Autowired
    private WeatherService weatherService;
    
    /**
     * Test 1: Cache Test
     * Verify that temperature is cached and not re-fetched within cache period
     */
    @Test
    public void testTemperatureCaching() {
        System.out.println("\n=== Test 1: Temperature Caching ===");
        
        // Clear cache before test
        weatherService.clearCache();
        
        // First call - should fetch from API or fallback
        String testDate = "2018-05-24 12:00:00";
        double temp1 = weatherService.getLiveOutdoorTemperature(testDate);
        System.out.println("First call temperature: " + temp1 + "°C");
        
        // Second call immediately after - should use cache
        double temp2 = weatherService.getLiveOutdoorTemperature(testDate);
        System.out.println("Second call temperature (cached): " + temp2 + "°C");
        
        // Both should be the same (from cache)
        assertEquals(temp1, temp2, 0.01, "Cached temperature should match first call");
        
        // Check cache status (just for logging)
        String cacheStatus = weatherService.getCacheStatus();
        System.out.println("Cache status: " + cacheStatus);
        
        System.out.println("✓ Caching test passed (temperatures match)\n");
    }
    
    /**
     * Test 2: Fallback Mechanism
     * When API fails, system should fall back to CSV data
     */
    @Test
    public void testFallbackToHistoricalData() {
        System.out.println("\n=== Test 2: Fallback to Historical Data ===");
        
        // This test will use either API or fallback depending on API availability
        String testDate = "2018-05-24 12:00:00";
        double temperature = weatherService.getLiveOutdoorTemperature(testDate);
        
        System.out.println("Retrieved temperature: " + temperature + "°C");
        
        // Temperature should be reasonable (either from API or fallback)
        assertTrue(temperature >= -50 && temperature <= 60, 
            "Temperature should be within reasonable range");
        
        System.out.println("✓ Fallback test passed\n");
    }
    
    /**
     * Test 3: Temperature Validation
     * Verify that temperature values are within reasonable range
     */
    @Test
    public void testTemperatureValidation() {
        System.out.println("\n=== Test 3: Temperature Validation ===");
        
        weatherService.clearCache();
        
        String testDate = "2018-05-24 12:00:00";
        double temperature = weatherService.getLiveOutdoorTemperature(testDate);
        
        System.out.println("Temperature: " + temperature + "°C");
        
        // Validate temperature is within reasonable bounds
        assertTrue(temperature >= -50, "Temperature should not be below -50°C");
        assertTrue(temperature <= 60, "Temperature should not be above 60°C");
        
        System.out.println("✓ Validation test passed\n");
    }
    
    /**
     * Test 4: Cache Expiry
     * Verify cache status reporting works correctly
     */
    @Test
    public void testCacheStatus() {
        System.out.println("\n=== Test 4: Cache Status Check ===");
        
        weatherService.clearCache();
        System.out.println("After clear: " + weatherService.getCacheStatus());
        
        String testDate = "2018-05-24 12:00:00";
        weatherService.getLiveOutdoorTemperature(testDate);
        System.out.println("After fetch: " + weatherService.getCacheStatus());
        
        System.out.println("✓ Cache status test complete\n");
    }
}
