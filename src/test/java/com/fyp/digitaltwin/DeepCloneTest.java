package com.fyp.digitaltwin;

import com.fyp.digitaltwin.service.ModelService;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 6: Deep Clone Isolation - Proves true isolation with no contamination
 * 
 * This test verifies three critical properties:
 * A. Object identity is different (true deep clone)
 * B. Data is copied correctly
 * C. Mutation does NOT leak (most important - proves no contamination)
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
public class DeepCloneTest {

    @Autowired
    private ModelService modelService;
        
    @BeforeEach
    public void setUp() {
        // Wait for system to initialize and calibrate
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test: Deep Clone Isolation - Proves true isolation with no contamination
     * 
     * This test verifies three critical properties:
     * A. Object identity is different (true deep clone)
     * B. Data is copied correctly
     * C. Mutation does NOT leak (most important - proves no contamination)
     */
    @Test
    public void testDeepCloneIsolation() throws Exception {
        System.out.println("\n=== TEST 6: Deep Clone Isolation Verification ===");
        
        // STEP 1: Load original model and set up test data
        EmfModel originalModel = modelService.loadBaseModel();
        
        // Set some test values in original model
        String setupScript = 
            "var firstEnergyMeter = SmartOffice!EnergyMeter.all.first();\n" +
            "if (firstEnergyMeter.isDefined()) {\n" +
            "    firstEnergyMeter.energyConsumed = 50.0d;\n" +
            "}\n" +
            "var firstRoom = SmartOffice!Room.all.first();\n" +
            "if (firstRoom.isDefined()) {\n" +
            "    firstRoom.currentTemp = 22.5d;\n" +
            "    if (firstRoom.hvac.isDefined()) {\n" +
            "        firstRoom.hvac.targetTemperature = 23.0d;\n" +
            "    }\n" +
            "}\n";
        modelService.runSimpleEolScript(originalModel, setupScript);
        
        // STEP 2: Get objects directly from EMF Resource (Java side) for identity comparison
        org.eclipse.emf.ecore.resource.Resource originalResource = originalModel.getResource();
        org.eclipse.emf.ecore.EObject originalRoot = originalResource.getContents().get(0);
        
        // Access EnergyMeter and Room from the EMF model structure
        // Building -> Floor -> Room -> sensors (EnergyMeter), hvac
        org.eclipse.emf.ecore.EObject originalEnergyMeter = null;
        org.eclipse.emf.ecore.EObject originalRoom = null;
        org.eclipse.emf.ecore.EObject originalHvac = null;
        
        // Navigate EMF structure to find first EnergyMeter and Room
        java.util.Iterator<org.eclipse.emf.ecore.EObject> iterator = originalRoot.eAllContents();
        while (iterator.hasNext()) {
            org.eclipse.emf.ecore.EObject obj = iterator.next();
            String className = obj.eClass().getName();
            if (originalEnergyMeter == null && "EnergyMeter".equals(className)) {
                originalEnergyMeter = obj;
            }
            if (originalRoom == null && "Room".equals(className)) {
                originalRoom = obj;
                // Get HVAC from room
                org.eclipse.emf.ecore.EStructuralFeature hvacFeature = obj.eClass().getEStructuralFeature("hvac");
                if (hvacFeature != null) {
                    Object hvacValue = obj.eGet(hvacFeature);
                    if (hvacValue instanceof org.eclipse.emf.ecore.EObject) {
                        originalHvac = (org.eclipse.emf.ecore.EObject) hvacValue;
                    }
                }
            }
            if (originalEnergyMeter != null && originalRoom != null && originalHvac != null) {
                break;
            }
        }
        
        // Extract data values using EOL
        String getDataScript = 
            "var firstEnergyMeter = SmartOffice!EnergyMeter.all.first();\n" +
            "var firstRoom = SmartOffice!Room.all.first();\n" +
            "var result = '';\n" +
            "if (firstEnergyMeter.isDefined()) {\n" +
            "    result = result + 'ENERGY_VALUE:' + firstEnergyMeter.energyConsumed.format('%.2f') + ':';\n" +
            "}\n" +
            "if (firstRoom.isDefined()) {\n" +
            "    result = result + 'ROOM_TEMP:' + firstRoom.currentTemp.format('%.2f') + ':';\n" +
            "    if (firstRoom.hvac.isDefined()) {\n" +
            "        result = result + 'HVAC_TARGET:' + firstRoom.hvac.targetTemperature.format('%.2f') + ':';\n" +
            "    }\n" +
            "}\n" +
            "return result;\n";
        
        String originalData = modelService.runSimpleEolScript(originalModel, getDataScript);
        System.out.println("Original Model Data: " + originalData);
        
        // Parse original data
        double originalEnergyValue = extractValue(originalData, "ENERGY_VALUE");
        double originalRoomTemp = extractValue(originalData, "ROOM_TEMP");
        double originalHvacTarget = extractValue(originalData, "HVAC_TARGET");
        
        // Get identity hash codes in Java (not EOL)
        int originalEnergyId = (originalEnergyMeter != null) ? System.identityHashCode(originalEnergyMeter) : 0;
        int originalRoomId = (originalRoom != null) ? System.identityHashCode(originalRoom) : 0;
        int originalHvacId = (originalHvac != null) ? System.identityHashCode(originalHvac) : 0;
        
        System.out.println("Original EnergyMeter ID: " + originalEnergyId + ", Value: " + originalEnergyValue);
        System.out.println("Original Room ID: " + originalRoomId + ", Temp: " + originalRoomTemp);
        System.out.println("Original HVAC ID: " + originalHvacId + ", Target: " + originalHvacTarget);
        
        // STEP 3: Deep clone the model
        System.out.println("\n[STEP 3] Deep cloning model...");
        org.eclipse.emf.ecore.resource.Resource clonedResource = modelService.deepCloneModel(originalModel);
        EmfModel clonedModel = modelService.createEmfModelFromResource(clonedResource);
        
        // STEP 4: Get cloned objects directly from EMF Resource (Java side)
        org.eclipse.emf.ecore.EObject clonedRoot = clonedResource.getContents().get(0);
        org.eclipse.emf.ecore.EObject clonedEnergyMeter = null;
        org.eclipse.emf.ecore.EObject clonedRoom = null;
        org.eclipse.emf.ecore.EObject clonedHvac = null;
        
        // Navigate cloned EMF structure
        java.util.Iterator<org.eclipse.emf.ecore.EObject> clonedIterator = clonedRoot.eAllContents();
        while (clonedIterator.hasNext()) {
            org.eclipse.emf.ecore.EObject obj = clonedIterator.next();
            String className = obj.eClass().getName();
            if (clonedEnergyMeter == null && "EnergyMeter".equals(className)) {
                clonedEnergyMeter = obj;
            }
            if (clonedRoom == null && "Room".equals(className)) {
                clonedRoom = obj;
                org.eclipse.emf.ecore.EStructuralFeature hvacFeature = obj.eClass().getEStructuralFeature("hvac");
                if (hvacFeature != null) {
                    Object hvacValue = obj.eGet(hvacFeature);
                    if (hvacValue instanceof org.eclipse.emf.ecore.EObject) {
                        clonedHvac = (org.eclipse.emf.ecore.EObject) hvacValue;
                    }
                }
            }
            if (clonedEnergyMeter != null && clonedRoom != null && clonedHvac != null) {
                break;
            }
        }
        
        // Extract cloned data values using EOL
        String clonedData = modelService.runSimpleEolScript(clonedModel, getDataScript);
        System.out.println("Cloned Model Data: " + clonedData);
        
        // Parse cloned data
        double clonedEnergyValue = extractValue(clonedData, "ENERGY_VALUE");
        double clonedRoomTemp = extractValue(clonedData, "ROOM_TEMP");
        double clonedHvacTarget = extractValue(clonedData, "HVAC_TARGET");
        
        // Get identity hash codes in Java
        int clonedEnergyId = (clonedEnergyMeter != null) ? System.identityHashCode(clonedEnergyMeter) : 0;
        int clonedRoomId = (clonedRoom != null) ? System.identityHashCode(clonedRoom) : 0;
        int clonedHvacId = (clonedHvac != null) ? System.identityHashCode(clonedHvac) : 0;
        
        System.out.println("Cloned EnergyMeter ID: " + clonedEnergyId + ", Value: " + clonedEnergyValue);
        System.out.println("Cloned Room ID: " + clonedRoomId + ", Temp: " + clonedRoomTemp);
        System.out.println("Cloned HVAC ID: " + clonedHvacId + ", Target: " + clonedHvacTarget);
        
        // ═════════════════════════════════════════════════════════════════
        // TEST A: Object Identity is Different (True Deep Clone)
        // ═════════════════════════════════════════════════════════════════
        System.out.println("\n[TEST A] Verifying object identity is different...");
        assertNotNull(originalEnergyMeter, "Original EnergyMeter should exist");
        assertNotNull(clonedEnergyMeter, "Cloned EnergyMeter should exist");
        assertNotEquals(originalEnergyId, clonedEnergyId, 
                    "EnergyMeter objects should have different identity (not same object)");
        assertNotEquals(originalRoomId, clonedRoomId, 
                    "Room objects should have different identity (not same object)");
        assertNotEquals(originalHvacId, clonedHvacId, 
                    "HVAC objects should have different identity (not same object)");
        System.out.println("✓ TEST A PASSED: Objects have different identity (true deep clone)");
        
        // ═════════════════════════════════════════════════════════════════
        // TEST B: Data is Copied Correctly
        // ═════════════════════════════════════════════════════════════════
        System.out.println("\n[TEST B] Verifying data is copied correctly...");
        assertEquals(originalEnergyValue, clonedEnergyValue, 0.01, 
                    "EnergyMeter energyConsumed should be copied correctly");
        assertEquals(originalRoomTemp, clonedRoomTemp, 0.01, 
                    "Room currentTemp should be copied correctly");
        assertEquals(originalHvacTarget, clonedHvacTarget, 0.01, 
                    "HVAC targetTemperature should be copied correctly");
        System.out.println("✓ TEST B PASSED: Data is copied correctly");
        
        // ═════════════════════════════════════════════════════════════════
        // TEST C: Mutation Does NOT Leak (Most Important - Proves No Contamination)
        // ═════════════════════════════════════════════════════════════════
        System.out.println("\n[TEST C] Verifying mutation does NOT leak (no contamination)...");
        
        // Mutate cloned model
        String mutateScript = 
            "var firstEnergyMeter = SmartOffice!EnergyMeter.all.first();\n" +
            "if (firstEnergyMeter.isDefined()) {\n" +
            "    firstEnergyMeter.energyConsumed = 999.0d;\n" +
            "}\n" +
            "var firstRoom = SmartOffice!Room.all.first();\n" +
            "if (firstRoom.isDefined()) {\n" +
            "    firstRoom.currentTemp = 99.9d;\n" +
            "    if (firstRoom.hvac.isDefined()) {\n" +
            "        firstRoom.hvac.targetTemperature = 99.0d;\n" +
            "    }\n" +
            "}\n";
        modelService.runSimpleEolScript(clonedModel, mutateScript);
        
        // Verify cloned model changed
        String mutatedClonedData = modelService.runSimpleEolScript(clonedModel, getDataScript);
        double mutatedClonedEnergy = extractValue(mutatedClonedData, "ENERGY_VALUE");
        double mutatedClonedRoomTemp = extractValue(mutatedClonedData, "ROOM_TEMP");
        double mutatedClonedHvacTarget = extractValue(mutatedClonedData, "HVAC_TARGET");
        
        assertEquals(999.0, mutatedClonedEnergy, 0.01, 
                    "Cloned model should be mutated");
        assertEquals(99.9, mutatedClonedRoomTemp, 0.01, 
                    "Cloned model should be mutated");
        assertEquals(99.0, mutatedClonedHvacTarget, 0.01, 
                    "Cloned model should be mutated");
        
        // CRITICAL: Verify original model is UNCHANGED
        String originalDataAfterMutation = modelService.runSimpleEolScript(originalModel, getDataScript);
        double originalEnergyAfterMutation = extractValue(originalDataAfterMutation, "ENERGY_VALUE");
        double originalRoomTempAfterMutation = extractValue(originalDataAfterMutation, "ROOM_TEMP");
        double originalHvacTargetAfterMutation = extractValue(originalDataAfterMutation, "HVAC_TARGET");
        
        assertNotEquals(999.0, originalEnergyAfterMutation, 0.01, 
                    "Original EnergyMeter should NOT be affected by cloned model mutation");
        assertNotEquals(99.9, originalRoomTempAfterMutation, 0.01, 
                    "Original Room should NOT be affected by cloned model mutation");
        assertNotEquals(99.0, originalHvacTargetAfterMutation, 0.01, 
                    "Original HVAC should NOT be affected by cloned model mutation");
        
        // Verify original values are preserved
        assertEquals(originalEnergyValue, originalEnergyAfterMutation, 0.01, 
                    "Original EnergyMeter value should remain unchanged");
        assertEquals(originalRoomTemp, originalRoomTempAfterMutation, 0.01, 
                    "Original Room temp should remain unchanged");
        assertEquals(originalHvacTarget, originalHvacTargetAfterMutation, 0.01, 
                    "Original HVAC target should remain unchanged");
        
        System.out.println("✓ TEST C PASSED: Mutation does NOT leak - no contamination!");
        System.out.println("  Original EnergyMeter: " + originalEnergyAfterMutation + " (unchanged)");
        System.out.println("  Cloned EnergyMeter: " + mutatedClonedEnergy + " (mutated)");
        
        // Clean up
        originalModel.dispose();
        clonedModel.dispose();
        
        System.out.println("\n✅ ALL TESTS PASSED: Deep clone provides true isolation with no contamination!");
        System.out.println("Test 6 PASSED: Deep clone isolation verified\n");
    }

    /**
     * Helper method to extract double value from colon-separated string
     */
    private double extractValue(String data, String key) {
        try {
            String pattern = key + ":";
            int startIndex = data.indexOf(pattern);
            if (startIndex == -1) return 0.0;
            startIndex += pattern.length();
            int endIndex = data.indexOf(":", startIndex);
            if (endIndex == -1) endIndex = data.length();
            return Double.parseDouble(data.substring(startIndex, endIndex));
        } catch (Exception e) {
            return 0.0;
        }
    }


}