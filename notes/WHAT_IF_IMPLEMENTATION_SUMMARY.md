

## 🔧 Technical Details

### Model Cloning Strategy
```java
// 1. Load fresh model (doesn't affect live simulation)
EmfModel scenarioModel = loadModel();

// 2. Apply changes using EOL transformation
applyWhatIfChanges(scenarioModel, changes);

// 3. Run prediction on modified model
Map<String, Double> scenario = predictFutureEnergyOnModel(scenarioModel, hours);

// 4. Clean up
scenarioModel.dispose();
```

### EOL Transformation Example
```eol
// Change all HVAC target temperatures
for (hvac in SmartOffice!HVACSystem.all) {
    hvac.targetTemperature = 21.0;
}

// Change all room insulation
for (room in SmartOffice!Room.all) {
    room.insulation = 0.03;
}
```

### API Request Format
```json
{
  "changes": {
    "targetTemp": 21.0,      // Optional: New target temperature
    "insulation": 0.03,      // Optional: New insulation value
    "baseLoad": 0.5,         // Optional: New base load
    "roomInsulation": {      // Optional: Per-room insulation
      "Office 1": 0.02,
      "Office 2": 0.025
    }
  },
  "hours": 24                // Required: Prediction horizon
}
```

---

## ✨ Benefits for Your FYP

1. **Demonstrates Advanced Concepts:**
   - Model-Driven Engineering
   - Digital Twin technology
   - Predictive analytics
   - Decision support systems

2. **Real-World Application:**
   - Energy optimization
   - Cost reduction
   - Investment planning
   - Building management

3. **Technical Excellence:**
   - Clean architecture
   - RESTful API design
   - Model transformation
   - Full-stack integration

4. **User-Centric Design:**
   - Interactive UI
   - Visual feedback
   - Clear recommendations
   - Error handling


## 🐛 Known Limitations

1. **Requires Prediction Data:** Must have future data in database
2. **Single Building:** Currently analyzes entire building (not per-room)
3. **Fixed Cost Rate:** Uses $0.15/kWh (could be made configurable)
4. **No Historical Comparison:** Doesn't compare with past actual data

---

## 🚀 Future Enhancements

1. Per-room What-If analysis
2. Schedule-based optimization (different settings for different times)
3. Multi-scenario comparison (compare 3+ scenarios side-by-side)
4. ML-based parameter suggestions
5. PDF report generation
6. Historical data comparison
7. Configurable cost rates

---

