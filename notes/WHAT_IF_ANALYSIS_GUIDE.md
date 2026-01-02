# What-If Analysis Feature - User Guide

## 🎯 Overview

The What-If Analysis feature allows you to test different scenarios and see their impact on energy consumption and costs **before** implementing them in the real building. This is a powerful tool for:

- **Energy Optimization**: Find the best temperature settings
- **Cost Reduction**: Estimate savings from building improvements
- **Decision Support**: Compare different scenarios objectively
- **Investment Planning**: Calculate ROI for insulation upgrades

---

## 🏗️ Architecture (Model-Driven Engineering)

```
User Input → REST API → Model Cloning → EOL Transformation → Simulation → Results
```

### Key Components:

1. **Frontend** (`App.jsx`): Interactive UI with sliders and result visualization
2. **Backend API** (`DigitalTwinController.java`): REST endpoint `/api/what-if`
3. **DTO** (`WhatIfRequest.java`): Request structure
4. **Engine** (`DigitalTwinEngine.java`): Core logic with model transformation
5. **EOL Transformation**: Dynamic model modification using Epsilon

---




## 📋 Example Scenarios

### Scenario 1: Reduce Temperature by 1°C

**Input:**
- Target Temperature: 21°C (down from 22°C)
- Insulation: 0.04 (unchanged)
- Horizon: 24 hours

**Expected Result:**
```
Baseline: 49.5 kWh
Scenario: 45.3 kWh
Savings: 4.2 kWh (8.5%)
Cost Saved: $0.63/day
Annual Savings: $230/year
```

**Recommendation:** ✅ Implement - Significant savings with minimal comfort impact

---

### Scenario 2: Improve Insulation

**Input:**
- Target Temperature: 22°C (unchanged)
- Insulation: 0.02 (improved from 0.04)
- Horizon: 24 hours

**Expected Result:**
```
Baseline: 49.5 kWh
Scenario: 42.7 kWh
Savings: 6.8 kWh (13.7%)
Cost Saved: $1.02/day
Annual Savings: $372/year
```

**Recommendation:** ✅ Implement - High ROI if insulation upgrade costs < $2000

---

### Scenario 3: Combined Optimization

**Input:**
- Target Temperature: 21°C
- Insulation: 0.03
- Horizon: 24 hours

**Expected Result:**
```
Baseline: 49.5 kWh
Scenario: 40.0 kWh
Savings: 9.5 kWh (19.2%)
Cost Saved: $1.43/day
Annual Savings: $522/year
```

**Recommendation:** ✅ Implement - Maximum savings strategy

---

### Scenario 4: Increase Temperature (Comfort Test)

**Input:**
- Target Temperature: 24°C (up from 22°C)
- Insulation: 0.04 (unchanged)
- Horizon: 24 hours

**Expected Result:**
```
Baseline: 49.5 kWh
Scenario: 56.2 kWh
Increase: 6.7 kWh (13.5%)
Extra Cost: $1.01/day
Annual Cost: $369/year
```

**Recommendation:** ⚠️ Not recommended - Increased costs without significant benefit



## 🧪 Testing Checklist

### Backend Tests

- [ ] API endpoint responds correctly
- [ ] Model cloning works without affecting live simulation
- [ ] EOL transformation applies changes correctly
- [ ] Prediction runs on modified model
- [ ] Results calculation is accurate
- [ ] Error handling works

### Frontend Tests

- [ ] What-If button opens modal
- [ ] Sliders update parameter values
- [ ] Run button triggers analysis
- [ ] Loading state displays during analysis
- [ ] Results display correctly
- [ ] Recommendation logic works
- [ ] Modal closes properly

### Integration Tests

- [ ] Frontend → Backend communication
- [ ] Multiple scenarios in sequence
- [ ] Edge cases (extreme values)
- [ ] Error scenarios (backend down, invalid data)

---

## 🐛 Troubleshooting

### Issue: "What-If analysis failed"

**Possible Causes:**
1. Backend not running
2. No prediction data available
3. Model loading error

**Solution:**
- Check backend console for errors
- Ensure simulation has run at least once
- Verify model files exist in `src/main/resources/`

---

### Issue: Results show 0 savings

**Possible Causes:**
1. Parameters unchanged from baseline
2. Insufficient prediction data
3. Calculation error

**Solution:**
- Change at least one parameter significantly
- Run simulation for longer before testing
- Check backend logs for calculation details

---

### Issue: Analysis takes too long

**Possible Causes:**
1. Large prediction horizon (72+ hours)
2. Model transformation overhead
3. Database query performance

**Solution:**
- Start with 24-hour horizon
- Ensure MongoDB is running efficiently
- Check system resources

---

## 💡 Best Practices

1. **Start Small**: Test with 24-hour horizon first
2. **One Change at a Time**: Isolate the impact of each parameter
3. **Document Results**: Keep track of successful scenarios
4. **Validate Assumptions**: Compare predictions with actual data
5. **Consider Comfort**: Don't sacrifice occupant comfort for savings
6. **Calculate ROI**: For insulation improvements, factor in upgrade costs

---

## 🎓 Educational Value (For FYP)

This feature demonstrates:

1. **Model-Driven Engineering**: Model transformation using EOL
2. **Digital Twin Concept**: Virtual testing before physical changes
3. **Predictive Analytics**: Energy forecasting
4. **Decision Support Systems**: Data-driven recommendations
5. **Full-Stack Development**: React + Spring Boot + MongoDB + Epsilon
6. **RESTful API Design**: Clean separation of concerns
7. **User Experience**: Interactive, visual feedback

---

## 📈 Future Enhancements

1. **More Parameters**: Base load, room capacity, HVAC efficiency
2. **Per-Room Analysis**: Test changes for specific rooms
3. **Schedule Optimization**: Different settings for different times
4. **Multi-Scenario Comparison**: Compare 3+ scenarios side-by-side
5. **Historical Comparison**: Compare with past actual data
6. **Export Reports**: PDF generation for stakeholders
7. **Machine Learning**: Suggest optimal parameters automatically

---

## 📝 Technical Notes

### Model Cloning Strategy

We clone the model to avoid affecting the live simulation:

```java
EmfModel scenarioModel = loadModel();  // Fresh copy
applyWhatIfChanges(scenarioModel, changes);  // Modify
predictFutureEnergyOnModel(scenarioModel, hours);  // Simulate
scenarioModel.dispose();  // Clean up
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

### Cost Calculation

```
Cost per kWh = $0.15 (configurable)
Daily Cost = Energy (kWh) × $0.15
Annual Cost = Daily Cost × 365
```

---


