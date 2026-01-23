package com.fyp.digitaltwin;

import com.fyp.digitaltwin.service.DigitalTwinEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for Simulation Run
 * 
 * Verifies that the simulation engine is operational end-to-end.
 * 
 * Purpose (for viva):
 * "The digital twin engine is operational end-to-end."
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class SimulationRunTest {

    @Autowired
    private DigitalTwinEngine digitalTwinEngine;

    /**
     * Test: Call simulation endpoint → returns result
     * Verifies dashboard returns valid data (proving simulation ran)
     */
    @Test
    public void testSimulationRun_ReturnsResult() throws Exception {
        System.out.println("\n=== TEST: Simulation Run Returns Result ===");

        // Wait for simulation to initialize
        Thread.sleep(2000);

        String dashboardJson = digitalTwinEngine.getDashboardData();

        assertNotNull(dashboardJson, "Dashboard data should not be null");
        assertFalse(dashboardJson.isEmpty(), "Dashboard data should not be empty");
        assertTrue(dashboardJson.contains("power") || dashboardJson.contains("rooms"), 
                  "Dashboard should contain power or rooms data");

        // Verify JSON structure
        assertTrue(dashboardJson.startsWith("{"), "Should be valid JSON object");

        System.out.println("Dashboard JSON length: " + dashboardJson.length());
        System.out.println("Test PASSED: Simulation runs and returns result\n");
    }

    /**
     * Test: Dashboard contains required fields
     */
    @Test
    public void testSimulationRun_ContainsRequiredFields() throws Exception {
        System.out.println("\n=== TEST: Simulation Run Contains Required Fields ===");

        Thread.sleep(2000);

        String dashboardJson = digitalTwinEngine.getDashboardData();

        // Check for key fields (power, rooms, etc.)
        boolean hasPower = dashboardJson.contains("\"power\"") || 
                          dashboardJson.contains("power");
        boolean hasRooms = dashboardJson.contains("\"rooms\"") || 
                          dashboardJson.contains("rooms");

        assertTrue(hasPower || hasRooms, 
                  "Dashboard should contain power or rooms data");

        System.out.println("Test PASSED: Dashboard contains required fields\n");
    }
}