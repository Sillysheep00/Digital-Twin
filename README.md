# Digital Twin System for Smart Building Energy Analysis

A model-driven Digital Twin system for smart building energy monitoring, anomaly detection, and what-if analysis. The system is built with Spring Boot, EMF/Epsilon, React, and MongoDB.

## Prerequisites

Ensure the following are installed before running the system:

- Java 19
- Maven
- Node.js and npm
- MongoDB running locally on port `27017`

## Running the System

### Terminal 1 - Frontend

```bash
cd frontend
npm install
npm run dev
```

### Terminal 2 - Backend

```bash
mvn spring-boot:run
```

After startup, Spring Boot will automatically:

- load the EMF model
- import historical CSV data into MongoDB on first run
- train the calibration model

The frontend will be available at `http://localhost:5173` and the backend API at `http://localhost:8080`.

## Running Tests

```bash
mvn test
```

Test coverage includes:

- simulation pipeline
- calibration logic and API
- anomaly detection logic and API
- HVAC automatic and manual control
- manual override API
- deep cloning and model isolation
- energy timeline tracking
- what-if scenario analysis
- weather service behaviour
- power trend API

## System Overview

On startup, the system:

1. Loads the EMF building model (`SmartOffice.ecore`) and creates a runtime clone
2. Imports historical sensor data from CSV into MongoDB on first run
3. Trains a linear regression model for power calibration
4. Fast-forwards 20 simulation steps for demonstration readiness
5. Starts a scheduled simulation loop every 5 seconds

## Configuration

Key settings are defined in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.data.mongodb.port` | `27017` | MongoDB port |
| `spring.data.mongodb.database` | `digitaltwin` | Database name |
| `server.port` | `8080` | Backend API port |
| `weather.location.lat` / `weather.location.lon` | London coordinates | Weather location |
| `weather.cache.minutes` | `10` | Weather API cache duration |

## Tech Stack

- **Backend:** Spring Boot 3.2, Java 19
- **Modelling:** Eclipse EMF, Epsilon EOL/EVL
- **Database:** MongoDB
- **Machine Learning:** Linear Regression (custom implementation)
- **Frontend:** React, Recharts, Three.js
- **Build Tools:** Maven, Vite

## Project Structure

```text
.
├── frontend/                    # React frontend
├── src/main/java/               # Spring Boot backend source code
├── src/main/resources/          # Configuration files, EMF model, scripts
├── src/test/java/               # Unit and integration tests
├── pom.xml                      # Maven build configuration
└── README.md
```

## Main Features

- Continuous simulation of building behaviour
- Runtime EMF model execution and updating
- Historical data replay using CSV-backed sensor records
- Linear regression calibration for simulated power
- Residual-based anomaly detection
- What-if analysis for scenario testing
- Frontend dashboard for monitoring and visualisation
- Live weather integration with caching

## Notes

- MongoDB must be running before starting the backend.
- Historical CSV data is imported only on the first run if the database is empty.
- The weather service uses cached responses to reduce repeated API calls.
- The system is designed for analytical decision support rather than real-time building control.

## Troubleshooting

### MongoDB connection issues

Make sure MongoDB is running locally on port `27017`.

### Frontend cannot connect to backend

Check that the backend is running on port `8080` and the frontend is configured to call the correct API base URL.

### Port already in use

If ports `5173` or `8080` are already occupied, update the relevant configuration before starting the system.

## Limitations

- The system currently uses historical replay rather than full live sensor streaming.
- Weather data is used as contextual reference and is not directly injected into the historical simulation loop.
- The implementation represents an analytical Digital Twin prototype rather than a fully bidirectional operational Digital Twin.

## Author

Developed as part of a smart building Digital Twin engineering project focused on energy analysis, anomaly detection, and scenario evaluation.