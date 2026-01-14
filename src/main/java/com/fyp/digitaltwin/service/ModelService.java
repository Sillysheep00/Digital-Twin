package com.fyp.digitaltwin.service;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;


@Service
public class ModelService {
    
    private static final String MODEL_RESOURCE = "DigitalTwin.smartoffice";
    private static final String METAMODEL_RESOURCE = "SmartOffice.ecore";
    
    public ModelService() {
        registerSmartOfficeResourceFactory();
    }
    
    /**
     * Loads the SmartOffice EMF model from resources
     * @return Loaded EMF model with all cross-references resolved
     * @throws Exception if model loading fails
     */
    public EmfModel loadBaseModel() throws Exception {
        System.out.println("Loading SmartOffice model from resources...");
        
        EmfModel model = new EmfModel();
        model.setName("SmartOffice");
        model.setModelFileUri(toEmfUri(resolveResource(MODEL_RESOURCE)));
        model.setMetamodelFileUri(toEmfUri(resolveResource(METAMODEL_RESOURCE)));
        model.setReadOnLoad(true);
        model.setStoredOnDisposal(false);
        model.load();
        
        // Resolves all proxy references (required for non-containment references like neighbors)
        EcoreUtil.resolveAll(model.getResource());

        try {
            String resetScript = 
                "for (s in SmartOffice!EnergyMeter.all) {\n" +
                "    s.energyConsumed = 0.0d;\n" +
                "}\n";
            runSimpleEolScript(model, resetScript);
            System.out.println("   Reset energy meters to ensure clean base model state");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset energy meters in base model", e);
        }
        
        return model;
    }

     /**
     * Deep clones an EMF model by creating a completely isolated Resource and EObject graph.
     * This ensures no contamination between live simulation and What-If scenarios.
     * 
     * @param sourceModel The model to clone from
     * @return A deep-cloned model with isolated Resource and EObject graph
     * @throws Exception if cloning fails
     */
    public Resource deepCloneModel(EmfModel sourceModel) throws Exception {
        if (sourceModel == null) {
            throw new IllegalArgumentException("Source model cannot be null");
        }
        
        System.out.println("Deep cloning model (creating isolated Resource and EObject graph)...");
        
        // 1. Get original resource
        Resource originalResource = sourceModel.getResource();
        if (originalResource == null || originalResource.getContents().isEmpty()) {
            throw new IllegalStateException("Source model has no resource or contents");
        }
        
        // 2. Create new ResourceSet & Resource (completely isolated)
        ResourceSetImpl resourceSet = new ResourceSetImpl();
        Resource clonedResource = resourceSet.createResource(
            URI.createURI("memory:/cloned_" + System.nanoTime() + ".xmi")
        );
        resourceSet.getResourceFactoryRegistry()
        .getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        
        // 3. Deep copy all root objects (this creates new EObject instances)
        for (EObject root : originalResource.getContents()) {
            EObject copy = EcoreUtil.copy(root);
            clonedResource.getContents().add(copy);
        }
        
        // 4. Resolve all references in the cloned resource
        EcoreUtil.resolveAll(clonedResource);
        
        if (originalResource.getContents().size() > 0 && clonedResource.getContents().size() > 0) {
            EObject originalRoot = originalResource.getContents().get(0);
            EObject clonedRoot = clonedResource.getContents().get(0);
            System.out.println("   VERIFICATION - Original root hash: " + System.identityHashCode(originalRoot));
            System.out.println("   VERIFICATION - Cloned root hash: " + System.identityHashCode(clonedRoot));
            if (System.identityHashCode(originalRoot) == System.identityHashCode(clonedRoot)) {
                System.err.println("    WARNING: Hash codes match! Cloning may have failed!");
            } else {
                System.out.println("    VERIFIED: Models are isolated (different hash codes)");
            }
        }
        return clonedResource;
    }

     /**
     * Wraps a Resource in an EmfModel for EOL script execution.
     * This allows us to use pure EMF Resources while still being able to run EOL scripts.
     * 
     * @param resource The EMF Resource to wrap
     * @return EmfModel wrapper around the Resource
     * @throws Exception if wrapping fails
     */
    public EmfModel createEmfModelFromResource(Resource resource) throws Exception {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        
        // Create EmfModel wrapper
        EmfModel model = new EmfModel();
        model.setName("SmartOffice");
        model.setMetamodelFileUri(toEmfUri(resolveResource(METAMODEL_RESOURCE)));
        model.setModelFileUri(resource.getURI());
        model.setReadOnLoad(false);
        model.setStoredOnDisposal(false);
        
        // Use reflection to inject the resource directly into EmfModel
        // This avoids load() which would create a new empty resource
        try {
           Field resourceField = model.getClass().getDeclaredField("resource");
            resourceField.setAccessible(true);
            resourceField.set(model, resource);
            
            // Also ensure the ResourceSet contains our resource
            ResourceSet resourceSet = resource.getResourceSet();
            if (resourceSet != null && !resourceSet.getResources().contains(resource)) {
                resourceSet.getResources().add(resource);
            }
            
            System.out.println("   Wrapped Resource in EmfModel for EOL execution");
        } catch (Exception e) {
            // Fallback: Save to temp file and load
            File tempFile = File.createTempFile("eol_model_", ".smartoffice");
            tempFile.deleteOnExit();
            resource.setURI(URI.createFileURI(tempFile.getAbsolutePath()));
            resource.save(null);
            
            model.setModelFileUri(URI.createFileURI(tempFile.getAbsolutePath()));
            model.setReadOnLoad(true);
            model.load();
            
            System.out.println("   Wrapped Resource in EmfModel via temp file: " + tempFile.getAbsolutePath());
        }
        
        return model;
    }

    
    /**
     * Runs an EOL script on the given model with optional context variables
     * @param model The EMF model to operate on
     * @param scriptName Name of the EOL script file (e.g., "hvac.eol")
     * @param logPrefix Prefix for logging (used to determine if in prediction mode)
     * @param data Data object to pass as "simulateData" variable
     * @param timeStep Time step value to pass as "TIME_STEP_HOURS" variable
     * @param overrides Manual overrides map to pass as "manualOverrides" variable
     * @param mlSlope ML slope (a) - learned regression parameter to scale simulated power
     * @param mlIntercept ML intercept (b) - learned regression parameter as base offset
     * @return Script output or console output
     * @throws Exception if script execution fails
     */
    public synchronized String runEolScript(EmfModel model, String scriptName, String logPrefix, 
                            Object data, Double timeStep, Map<String, String> overrides, 
                            double mlSlope, double mlIntercept,String simulationStartTime) throws Exception {
        EolModule module = new EolModule();
        parseModule(module, scriptName);
        
        module.getContext().getModelRepository().addModel(model);
        
        // Add variables to execution context
        if (data != null) {
            module.getContext().getFrameStack().put("simulateData", data);
        }
        if (timeStep != null) {
            module.getContext().getFrameStack().put("TIME_STEP_HOURS", timeStep);
        }
        if (overrides != null) {
            module.getContext().getFrameStack().put("manualOverrides", overrides);
        }
        
        // Pass ML model parameters to EOL scripts (Linear Regression: y = a*x + b)
        module.getContext().getFrameStack().put("ML_SLOPE", mlSlope);          // slope (a)
        module.getContext().getFrameStack().put("ML_INTERCEPT", mlIntercept);  // intercept (b)

        if (simulationStartTime != null) {
            module.getContext().getFrameStack().put("SIMULATION_START_TIME", simulationStartTime);
        }
        
        // Enable silent mode for predictions to reduce console output
        boolean silentMode = logPrefix != null && 
                            (logPrefix.contains("Prediction") || 
                            logPrefix.contains("Scenario") ||
                            logPrefix.contains("Calibration"));
        module.getContext().getFrameStack().put("SILENT_MODE", silentMode);
        
        // Capture console output - synchronized to prevent race conditions
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        
        try {
            System.setOut(ps);
            Object result = module.execute();
            System.out.flush();
            
            module.getContext().getModelRepository().removeModel(model);
            
            String output = result != null ? result.toString() : "";
            String consoleOutput = baos.toString();
            
            // For JSON scripts, filter out non-JSON lines
            if (scriptName.equals("json.eol")) {
                String filtered = filterJsonOutput(consoleOutput);
                return filtered.isBlank() ? output : filtered;
            }
            
            return consoleOutput.isBlank() ? output : consoleOutput;
            
        } finally {
            // Always restore System.out even if an exception occurs
            System.setOut(old);
        }
    }
    
    /**
     * Run a simple EOL script that returns a value (no full model transformation)
     * Used for quick queries like extracting model parameters
     * 
     * @param model The EMF model to query
     * @param script EOL script to execute
     * @return String result from script execution
     * @throws Exception if script execution fails
     */
    public String runSimpleEolScript(EmfModel model, String script) throws Exception {
        EolModule module = new EolModule();
        module.parse(script);
        
        module.getContext().getModelRepository().addModel(model);
        Object result = module.execute();
        module.getContext().getModelRepository().removeModel(model);
        
        return result != null ? result.toString() : "";
    }
    
    /**
     * Filters output to extract only JSON content, ignoring debug messages
     * @param output The raw output that may contain mixed content
     * @return Only the JSON portion of the output
     */
    private String filterJsonOutput(String output) {
        if (output == null || output.isBlank()) {
            return output;
        }
        
        // Find lines that look like JSON (start with { or [)
        String[] lines = output.split("\\r?\\n");
        StringBuilder jsonBuilder = new StringBuilder();
        boolean inJson = false;
        int braceCount = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();

            // Skip lines that are clearly not JSON (simulation messages, debug output, etc.)
            if (trimmed.startsWith(">>") || 
                trimmed.startsWith("DEBUG") || 
                trimmed.startsWith("Warning") ||
                trimmed.startsWith("Error") ||
                trimmed.isEmpty()) {
                continue;
            }
            
            
            // Start of JSON object or array
            if (!inJson && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
                inJson = true;
                jsonBuilder.append(line).append("\n");
                braceCount += countChar(trimmed, '{') + countChar(trimmed, '[');
                braceCount -= countChar(trimmed, '}') + countChar(trimmed, ']');
            }
            // Continue capturing JSON
            else if (inJson) {
                jsonBuilder.append(line).append("\n");
                braceCount += countChar(trimmed, '{') + countChar(trimmed, '[');
                braceCount -= countChar(trimmed, '}') + countChar(trimmed, ']');
                
                // End of JSON
                if (braceCount <= 0) {
                    break;
                }
            }
        }
        
        String filtered = jsonBuilder.toString().trim();

        // Validate that we actually have JSON (starts with { or [)
        if (filtered.isEmpty() || (!filtered.startsWith("{") && !filtered.startsWith("["))) {
            // If no valid JSON found, return empty string instead of original output
            // This will cause the caller to handle the error gracefully
            return "";
        }
        
        return filtered;
    }
    
    //Counts occurrences of a character in a string
    private int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Runs EVL validation on the model
     * @param model The EMF model to validate
     * @return Validation report string
     * @throws Exception if validation fails
     */
    public String runValidation(EmfModel model) throws Exception {
        EvlModule evlModule = new EvlModule();
        parseModule(evlModule, "validation.evl");
        
        evlModule.getContext().getModelRepository().addModel(model);
        evlModule.execute();
        
        StringBuilder report = new StringBuilder();
        report.append("=== Model Validation Report ===\n\n");
        
        for (UnsatisfiedConstraint uc : evlModule.getContext().getUnsatisfiedConstraints()) {
            report.append(" [").append(uc.getConstraint().getName()).append("] ");
            report.append(uc.getMessage()).append("\n");
            if (uc.getInstance() != null) {
                report.append("   Context: ").append(uc.getInstance().toString()).append("\n");
            }
        }
        
        if (evlModule.getContext().getUnsatisfiedConstraints().isEmpty()) {
            report.append(" All validation constraints satisfied!\n");
        }
        
        evlModule.getContext().getModelRepository().removeModel(model);
        return report.toString();
    }
    
    // Private Helper Methods
    //Registers the SmartOffice resource factory for EMF
    private void registerSmartOfficeResourceFactory() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .putIfAbsent(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("smartoffice", new XMIResourceFactoryImpl());
    }
    
    //Parses an EOL/EVL module and checks for parse errors
    private void parseModule(IEolModule module, String resourceName) throws Exception {
        File script = resolveResource(resourceName).toFile();
        module.parse(script);
        if (!module.getParseProblems().isEmpty()) {
            throw new IllegalStateException(formatParseErrors(resourceName, module.getParseProblems()));
        }
    }
    
    //Formats parse errors into a readable string
    private String formatParseErrors(String resourceName, List<ParseProblem> problems) {
        StringBuilder builder = new StringBuilder("Failed to parse ")
                .append(resourceName)
                .append(System.lineSeparator());
        for (ParseProblem problem : problems) {
            builder.append(" - ").append(problem.toString()).append(System.lineSeparator());
        }
        return builder.toString();
    }
    
    //Resolves a resource name to a filesystem path
    private Path resolveResource(String resourceName) {
        URL url = ModelService.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourceName);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI for resource " + resourceName, e);
        }
    }
    
     //Converts a filesystem path to an EMF URI
    private URI toEmfUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().toString());
    }
}

