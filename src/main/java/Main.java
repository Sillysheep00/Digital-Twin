import engine.DigitalTwinEngine;
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Digital Twin Epsilon demo ===");
        DigitalTwinEngine engine = new DigitalTwinEngine();

        try {
            engine.run();
            System.out.println("All scripts executed successfully.");
        } catch (Exception ex) {
            System.err.println("Execution failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }


}
