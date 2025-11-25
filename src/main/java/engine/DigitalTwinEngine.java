package engine;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DigitalTwinEngine {
    private static final String MODEL_RESOURCE = "DigitalTwin.smartoffice";
    private static final String METAMODEL_RESOURCE = "SmartOffice.ecore";

    public DigitalTwinEngine() {
    }

    public void run() throws Exception {
        registerSmartOfficeResourceFactory();

        //1. Load the smart office emf model
        EmfModel smartOfficeModel = loadModel();

        // 2. Load your CSV dataset in Java
        List<DataRecord> dataset = loadCsvFile("src/main/resources/cleandata.csv");
        System.out.println("✔ CSV loaded with " + dataset.size() + " rows.");

        try {
            double TIME_STEP_HOURS = 0.25; // 15 minutes = 0.25 hours
            for (int i = 0; i < 5; i++) {
                DataRecord simulateData = dataset.get(i);
                System.out.println("... Simulating with data from: " + simulateData.getDate());
                runEolScript(smartOfficeModel, "hvac.eol", "HVAC analytics",simulateData,TIME_STEP_HOURS);
                runEolScript(smartOfficeModel, "query.eol", "Query utilities",simulateData);
            }
            runEvlValidation(smartOfficeModel, "validation.evl");
        } finally {
            smartOfficeModel.dispose();
        }
    }


    private static void registerSmartOfficeResourceFactory() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .putIfAbsent(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("smartoffice", new XMIResourceFactoryImpl());
    }

    private static EmfModel loadModel() throws Exception {
        System.out.printf("Loading SmartOffice model (%s | %s)%n", MODEL_RESOURCE, METAMODEL_RESOURCE);
        EmfModel model = new EmfModel();
        model.setName("SmartOffice");
        model.setModelFileUri(toEmfUri(resolveResource(MODEL_RESOURCE)));
        model.setMetamodelFileUri(toEmfUri(resolveResource(METAMODEL_RESOURCE)));
        model.setReadOnLoad(true);
        model.setStoredOnDisposal(false);
        model.load();
        System.out.printf("✔ Model loaded with %d elements.%n", model.allContents().size());
        return model;
    }

    private static void runEolScript(EmfModel model, String resourceName, String label,Object data ) throws Exception {
        System.out.printf("Running %s (%s)...%n", label, resourceName);
        EolModule module = new EolModule();
        parseModule(module, resourceName);
        module.getContext().getModelRepository().addModel(model);

        if (data != null) {
            // It will be named "simData" inside the EOL script
            module.getContext().getFrameStack().put("simulateData", data);
        }

        Object result = module.execute();
        System.out.printf("✔ %s completed. Result: %s%n", label, result);
    }

    //Use to cal
    private static void runEolScript(EmfModel model, String resourceName, String label, Object data, Object timeStep) throws Exception {
        System.out.printf("Running %s (%s)...%n", label, resourceName);
        EolModule module = new EolModule();
        parseModule(module, resourceName);
        module.getContext().getModelRepository().addModel(model);

        if (data != null) {
            module.getContext().getFrameStack().put("simulateData", data);
        }
        if (timeStep != null) {
            module.getContext().getFrameStack().put("TIME_STEP_HOURS", timeStep);
        }

        Object result = module.execute();
        System.out.printf("✔ %s completed. Result: %s%n", label, result);
    }

    private static void runEvlValidation(EmfModel model, String resourceName) throws Exception {
        System.out.printf("Running EVL validation (%s)...%n", resourceName);
        EvlModule module = new EvlModule();
        parseModule(module, resourceName);
        module.getContext().getModelRepository().addModel(model);
        module.execute();

        var unsatisfied = module.getContext().getUnsatisfiedConstraints();
        if (unsatisfied.isEmpty()) {
            System.out.println("✔ Validation passed. No unsatisfied constraints.");
        } else {
            System.out.printf("✖ Validation produced %d issue(s):%n", unsatisfied.size());
            for (UnsatisfiedConstraint issue : unsatisfied) {
                System.out.printf("- [%s] %s :: %s%n",
                        issue.getConstraint().getName(),
                        issue.getInstance(),
                        issue.getMessage());
            }
        }
    }

    private static void parseModule(IEolModule module, String resourceName) throws Exception {
        File script = resolveResource(resourceName).toFile();
        module.parse(script);
        if (!module.getParseProblems().isEmpty()) {
            throw new IllegalStateException(formatParseErrors(resourceName, module.getParseProblems()));
        }
    }

    private static String formatParseErrors(String resourceName, List<ParseProblem> problems) {
        StringBuilder builder = new StringBuilder("Failed to parse ")
                .append(resourceName)
                .append(System.lineSeparator());
        for (ParseProblem problem : problems) {
            builder.append(" - ").append(problem.toString()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static Path resolveResource(String resourceName) {
        URL url = DigitalTwinEngine.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourceName);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI for resource " + resourceName, e);
        }
    }

    public List<DataRecord> loadCsvFile(String filepath) throws Exception {
        List<DataRecord> dataset = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            // Skip header row
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");

                // cols: [date, Power Consumption, Outdoor Temperature, Occupancy]
                DataRecord dr = new DataRecord(
                        cols[0],
                        Double.parseDouble(cols[1]),
                        Double.parseDouble(cols[2]),
                        Integer.parseInt(cols[3])
                );
                dataset.add(dr);
            }
        }
        return dataset;
    }

    private static URI toEmfUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().toString());
    }
}
