# Software Engineering Log – Digital Twin Smart Building Project

## 1. System Architecture

The system follows a modern client-server architecture that bridges a high-fidelity 3D frontend with a robust simulation backend.

**Frontend**: A React-based Single Page Application (SPA) built with Vite. It utilizes A-Frame (via React) to render the 3D Digital Twin of the smart office and communicates with the backend via RESTful APIs to display live telemetry data.

**Backend**: A Spring Boot application that acts as the simulation engine. It integrates the **Epsilon** family of languages (EOL, EVL) to manage the Digital Twin's state and physics.

**Database**: MongoDB is used to store the historical sensor data (`cleandata.csv`), which is replayed during the simulation to provide realistic input values (outdoor temperature, total occupancy, etc.).

### Justification of Choices

*   **Why Spring Boot?**
    Spring Boot was chosen for its robustness and ease of creating REST APIs. Crucially, its built-in `@Scheduled` annotation allowed me to create a "heartbeat" for the simulation, running the physics loop every 5 seconds without complex thread management. It also manages the lifecycle of the Epsilon engine efficiently.

*   **Why React (Vite)?**
    React's component-based structure allows for modular UI development (separating the 3D Scene from the Dashboard overlay). Vite was selected over Create-React-App for its superior hot-reloading speed, which significantly reduced development time when tweaking 3D parameters.

*   **Why Epsilon?**
    The project adopts a **Model-Driven Engineering (MDE)** approach. Epsilon allows me to define the building's structure (Rooms, HVACs, Sensors) in a high-level model (`.smartoffice`) and write the logic in **EOL (Epsilon Object Language)**. This separates the *simulation logic* (physics) from the *infrastructure code* (Java), making the rules easier to read and modify than if they were hardcoded in Java classes.

### Architecture Diagram

*(Place your diagram here. It should show: React App <--> REST API <--> Spring Boot [DigitalTwinEngine] <--> [Epsilon (EOL)] <--> [EMF Model])*

---

## 2. Data Flow

The journey of data in the system simulates a real-time IoT environment:

1.  **Ingestion**: On system startup, the `DigitalTwinEngine` checks if the database is empty. If so, it reads raw historical data from `cleandata.csv` and loads it into MongoDB.
2.  **Simulation Step**: Every 5 seconds, the engine fetches the next chronological record (outdoor temp, power, occupancy) from MongoDB.
3.  **Injection**: This raw data is injected into the Epsilon runtime context as a variable named `simulateData`.
4.  **Physics Processing (`hvac.eol`)**: The EOL script executes. It reads the injected data, applies it to the `.smartoffice` model (e.g., updating `currentTemp` based on insulation and HVAC status), and calculates the new state of every room.
5.  **State Persistence**: The updated model state (new temperatures, energy usage) is saved back to the `DigitalTwin.smartoffice` file.
6.  **Aggregation (`json.eol`)**: When the React frontend requests an update, the backend runs `json.eol`. This script iterates through the live model, aggregates data (e.g., summing up power usage of all active HVACs), and constructs a JSON object.
7.  **Visualisation**: The frontend receives this JSON and updates the dashboard gauges and 3D overlays instantly.

---

## 3. Algorithms & Logic

The intelligence of the Digital Twin is encapsulated in the Epsilon Object Language (EOL) scripts.

### HVAC Control with Hysteresis
To prevent the HVAC systems from flickering on and off rapidly (short-cycling), I implemented a hysteresis control loop. The system defines a `comfortZone` (set to 1.0°C).
*   **Cooling**: The AC only turns **ON** if the room temperature exceeds the `targetTemp` + `comfortZone`.
*   **Heating**: The heater only turns **ON** if the temperature drops below `targetTemp` - `comfortZone`.
*   **Stability**: If the temperature is within the buffer zone, the system maintains its previous state or drifts naturally, simulating realistic thermal inertia.

### Variable Load Logic
Unlike a simple boolean (On/Off) switch, the system calculates a `loadFactor` based on the temperature gap. If a room is 5°C too hot, the AC runs at 100% power. If it is only 0.5°C too hot, it runs at a partial load (e.g., 10%). This provides a much more accurate simulation of energy consumption compared to a binary model.

### Manual Override Priority
User control is critical. I implemented a `HashMap` in the Java engine to track manual overrides.
*   Before applying automatic logic, the physics engine checks this map.
*   If a user sends an "OFF" command for a specific room, the logic **forces** the status to false and power to 0, ignoring the temperature sensor.
*   This ensures that human intervention always supersedes the automated algorithm, mimicking a real-world building management system (BMS).

---

## 4. Challenges & Solutions

### Challenge 1: Dynamic vs. Static EMF
**Context**: In the Eclipse Modeling Framework (EMF), you can either generate Java classes for your model (Static) or load the model dynamically at runtime (Dynamic).

**Choice**: I chose **Dynamic EMF**.

**Reasoning**:
Static EMF requires re-generating Java code every time I change the metamodel (e.g., adding a new attribute like `insulation` to a Room). This is cumbersome during prototyping.
Dynamic EMF allows the `DigitalTwinEngine` to load `SmartOffice.ecore` and `DigitalTwin.smartoffice` directly at runtime. This offered **extreme flexibility**—I could tweak the model structure in the `.ecore` file and restart the server to see changes immediately, without fighting with code generation tools.

### Challenge 2: Aggregating Live Power Data
**Problem**: The dashboard initially showed inconsistent power readings. The "Real Power" (from CSV) and "Simulated Power" (calculated) were difficult to compare because the simulated power was decentralized—scattered across individual Room objects.

**Impact**: It was impossible to validate if the Digital Twin was accurate.

**Solution**: I created a dedicated aggregation script, `json.eol`. Instead of the Java backend trying to query the model (which is slow and complex), `json.eol` iterates internally over `SmartOffice!HVACSystem.all` and `SmartOffice!Room.all`, sums up the `powerUsage` and `baseLoad`, and returns a single, clean JSON structure. This moved the computation close to the data, significantly improving performance and accuracy.

### Challenge 3: 3D Coordinate Mismatch (Blender to A-Frame)
**Problem**: When exporting the office model from Blender to GLTF, the building appeared rotated by 90 degrees or floating in the air in the React application.

**Cause**: Blender uses a "Z-up" coordinate system, whereas A-Frame (and WebGL) uses a "Y-up" system.

**Solution**:
1.  I applied a rotation fix in the React component (`<primitive object={scene} rotation={[0, -Math.PI / 2, 0]} />`) to align the model correctly.
2.  I meticulously named the meshes in Blender (e.g., `R1_HVAC`, `R2_Sensor`) to match the IDs in my `.smartoffice` model, allowing the code to dynamically find and color-code the 3D objects based on live data.


Design pattern
Repository pattern:
The code have a Repository interface (like SimulationResultRepository) separate from your Logic (DigitalTwinEngine) and your Data Model (SimulationResult) 

Repository Pattern isolates the "Business Logic" from the "Data Access Logic."
Business Logic: Your DigitalTwinEngine knows what to do (calculate physics, run EOL). It doesn't care if the data is stored in MongoDB, MySQL, or a Text File. It just says "Save this."
Repository: The SimulationResultRepository handles the dirty work of how to save it (opening connections, writing BSON, handling indexes).


why use this pattern?
Decoupling : Allow switching from mongoDB to MySQL 
Testability: You can easily mock the Repository during testing (e.g., "Pretend the database is empty") without needing a real running database.

The Broader Architecture: "Layered Architecture"
Your entire Spring Boot backend follows the Layered Architecture (Controller-Service-Repository) pattern, which is the industry standard for Java web apps:
Controller Layer (DigitalTwinController): Handles "Traffic Control" (HTTP Requests).
Service Layer (DigitalTwinEngine): Handles "Business Logic" (Physics, Simulation).
Repository Layer (SimulationResultRepository): Handles "Data Storage" (Database).

HVAC explanation
The HVAC system is configured with a target temperature of 22°C. However, during the simulation, the room temperature stabilizes around 21.42°C instead of reaching the exact target. This is a realistic physical behavior known as a thermal steady state, where the heat energy supplied by the HVAC system (running at full capacity) exactly equals the heat energy being lost to the cold outdoor environment through the walls. Even though the heater is actively running, the poor insulation and low outdoor temperature create a heat loss rate that matches the heater's maximum output, preventing the room from climbing that final 0.6°C. This demonstrates that the Digital Twin correctly simulates real-world thermodynamic limits rather than artificially forcing values.

Washroom temperature stick very close to other room temperature explanation
This is because washroom is surrounded by 2-3 heated room ( their neighbours may be main office,etc), so they are getting heat from multiple sources simultaneously.This creates a thermal blanket effect.

why night time washroom temperature does not have a 3-4 degree celsius difference with the other room
When everything gets colder, temperatures converge toward outdoor temperature.

the formula in the code: Heat Loss Rate = (Room Temp - Outdoor Temp) × Insulation
insulation is U-value meaning conductivity so low number = good insulation

Let's say outdoor temp is 8°C (from your dataset):
DAYTIME:
Heated rooms: 22°C → Heat loss rate = (22-8) = 14°C difference → HIGH loss
Washrooms: 17°C → Heat loss rate = (17-8) = 9°C difference → MODERATE loss
Large gradient maintained because rooms are aggressively heating
NIGHTTIME:
Heated rooms: 16°C → Heat loss rate = (16-8) = 8°C difference → LOWER loss
Washrooms: 14°C → Heat loss rate = (14-8) = 6°C difference → LOW loss
Small gradient because everyone is closer to outdoor temp


Deterministic pseudo-random heuristic for occupancy simulation
3 techniques:
1.Seeded pseudo-randomness
    -derive randomness from:
        -time (date + hour)
        -room identity

    The same inputs → same output
    This is called: Deterministic pseudo-random generation
    Used in: simulation , digital twins
    Give random looking , but reproducible
2.Capacity-weighted allocation
    -capacityWeight = room.capacity / totalCapacity , this is a proportional allocation technique
    This ensures big rooms get more people, small room get lesser people
3.Stochastic perturbation / noise injection

These part: 
    -randomFactor (0.3–1.0)
    -±20% variation
are called controlled noise injection
This prevents uniform behaviour and unrealistic static patterns.

The random occupancy distribution algorithm uses a pseudo-random seeding technique based on time and room characteristics to realistically distribute building occupants across rooms. It combines capacity-based weighting (70%) with random factors (30%) to ensure larger rooms are more likely to have people while maintaining natural variation. The algorithm generates whole-number occupancy values by applying ±20% random fluctuation and rounding, respects room capacity limits, and includes special logic to reduce washroom occupancy by 80% since these spaces are briefly occupied. This approach creates deterministic but realistic patterns where the same timestamp produces the same distribution, while different times and rooms yield varied occupancy patterns that reflect real-world building usage


A fast-forward initialization phase is executed at system startup to warm up the simulation model. This ensures that the digital twin reaches a realistic operating state before prediction and What-If analysis are performed. After initialization, the simulation continues from the advanced timestep rather than restarting from the beginning of the dataset.
