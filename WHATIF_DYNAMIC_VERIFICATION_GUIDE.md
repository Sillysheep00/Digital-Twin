# What-If Analysis Dynamic Prediction Verification Guide

## 🎯 Purpose

This guide helps you verify that your What-If Analysis predictions **change dynamically** based on different scenarios, proving that the ML model responds to different HVAC conditions.

---

## ✅ Changes Applied

### 1. **PredictionService.java**
Added detailed step-by-step logging to show:
- Raw simulated HVAC power (from physics model)
- ML-predicted power (after applying Linear Regression)
- Energy consumed per step
- ML model parameters (slope × input + intercept)

### 2. **WhatIfAnalysisService.java**
Added logging to show:
- What parameters are being changed
- ML model configuration being used

### 3. **Added Getter Methods**
- `getMlSlope()` - Returns the learned slope parameter
- `getMlIntercept()` - Returns the learned intercept parameter

---

## 🧪 How to Test

### **Step 1: Restart Your Backend**

```bash
mvn spring-boot:run
```

Wait for the ML training to complete. You should see:
```
✅ TRAINING COMPLETE!
   Model equation: realPower = 2.1229 × simulatedPower + 47.1731
```

### **Step 2: Open Frontend and Run What-If Analysis**

1. Open your dashboard: `http://localhost:5173` (or your frontend URL)
2. Click the **"🔬 What-If Analysis"** button
3. Try these test scenarios:

---

## 📊 Test Scenarios

### **Test 1: Lower Temperature (Less Energy)**

**Goal:** Prove that lowering target temperature reduces HVAC load and total energy

**Settings:**
- Target Temperature: **18°C** (slider to minimum)
- Insulation: 0.04 (keep default)
- Hours: **24**

**Expected Output in Backend Console:**

```
=== WHAT-IF ANALYSIS START ===
Changes requested: {targetTemp=18.0}
Prediction horizon: 24 hours

[1/5] Running baseline prediction...
  📊 Step 1 | Raw HVAC: 13.81 kW → ML Predicted: 76.50 kW (2.12x + 47.17) → Energy: 19.13 kWh
  📊 Step 2 | Raw HVAC: 13.75 kW → ML Predicted: 76.38 kW (2.12x + 47.17) → Energy: 19.09 kWh
  📊 Step 3 | Raw HVAC: 13.69 kW → ML Predicted: 76.25 kW (2.12x + 47.17) → Energy: 19.06 kWh
  ...
Baseline energy: 1835.20 kWh

[3/5] Applying what-if changes to model...
📝 CHANGES APPLIED:
   • targetTemp: 18.0

[4/5] Running scenario prediction with ML calibration...
   ML Model: slope=2.1229, intercept=47.1731
  📊 Step 1 | Raw HVAC: 8.45 kW → ML Predicted: 65.11 kW (2.12x + 47.17) → Energy: 16.28 kWh  ⬅️ LOWER!
  📊 Step 2 | Raw HVAC: 8.12 kW → ML Predicted: 64.41 kW (2.12x + 47.17) → Energy: 16.10 kWh  ⬅️ LOWER!
  📊 Step 3 | Raw HVAC: 7.89 kW → ML Predicted: 63.92 kW (2.12x + 47.17) → Energy: 15.98 kWh  ⬅️ LOWER!
  ...
Scenario energy: 1548.60 kWh  ⬅️ ENERGY SAVED!

[5/5] Calculating savings...
Energy saved: 286.60 kWh (15.6%)
Cost saved: $28.66
```

**✅ Success Criteria:**
- Raw HVAC power **decreases** (from ~13.8 kW to ~8.5 kW)
- ML predicted power **decreases** (from ~76 kW to ~65 kW)
- Total energy is **lower** in scenario
- Frontend shows **positive savings**

---

### **Test 2: Higher Temperature (More Energy)**

**Goal:** Prove that raising target temperature increases HVAC load

**Settings:**
- Target Temperature: **25°C** (slider to maximum)
- Insulation: 0.04 (keep default)
- Hours: **24**

**Expected Output:**

```
[1/5] Running baseline prediction...
  📊 Step 1 | Raw HVAC: 13.81 kW → ML Predicted: 76.50 kW ...
Baseline energy: 1835.20 kWh

[3/5] Applying what-if changes to model...
📝 CHANGES APPLIED:
   • targetTemp: 25.0

[4/5] Running scenario prediction with ML calibration...
  📊 Step 1 | Raw HVAC: 18.32 kW → ML Predicted: 86.06 kW ...  ⬅️ HIGHER!
  📊 Step 2 | Raw HVAC: 18.45 kW → ML Predicted: 86.33 kW ...  ⬅️ HIGHER!
  ...
Scenario energy: 2067.84 kWh  ⬅️ MORE ENERGY!

Energy saved: -232.64 kWh (INCREASE)
```

**✅ Success Criteria:**
- Raw HVAC power **increases** (from ~13.8 kW to ~18.3 kW)
- ML predicted power **increases** (from ~76 kW to ~86 kW)
- Total energy is **higher** in scenario
- Frontend shows **negative savings** (warning message)

---

### **Test 3: Better Insulation (Less Energy)**

**Goal:** Prove that improving insulation reduces heat loss and HVAC load

**Settings:**
- Target Temperature: 22°C (keep default)
- Insulation: **0.01** (slider to minimum = better insulation)
- Hours: **24**

**Expected Output:**

```
📝 CHANGES APPLIED:
   • insulation: 0.01

[4/5] Running scenario prediction with ML calibration...
  📊 Step 1 | Raw HVAC: 11.23 kW → ML Predicted: 71.00 kW ...  ⬅️ LOWER!
  📊 Step 2 | Raw HVAC: 11.15 kW → ML Predicted: 70.83 kW ...  ⬅️ LOWER!
  ...
Scenario energy: 1702.40 kWh  ⬅️ ENERGY SAVED!

Energy saved: 132.80 kWh (7.2%)
```

**✅ Success Criteria:**
- Raw HVAC power **decreases** (less heat loss = less heating needed)
- ML predicted power **decreases**
- Total energy is **lower**
- Frontend shows **positive savings**

---

### **Test 4: Worse Insulation (More Energy)**

**Goal:** Prove that poor insulation increases heat loss and HVAC load

**Settings:**
- Target Temperature: 22°C (keep default)
- Insulation: **0.08** (slider to maximum = poor insulation)
- Hours: **24**

**Expected Output:**

```
📝 CHANGES APPLIED:
   • insulation: 0.08

[4/5] Running scenario prediction with ML calibration...
  📊 Step 1 | Raw HVAC: 16.89 kW → ML Predicted: 83.02 kW ...  ⬅️ HIGHER!
  📊 Step 2 | Raw HVAC: 17.02 kW → ML Predicted: 83.30 kW ...  ⬅️ HIGHER!
  ...
Scenario energy: 1995.36 kWh  ⬅️ MORE ENERGY!

Energy saved: -160.16 kWh (INCREASE)
```

**✅ Success Criteria:**
- Raw HVAC power **increases** (more heat loss = more heating needed)
- ML predicted power **increases**
- Total energy is **higher**
- Frontend shows **negative savings**

---

## 🔍 What the Logs Prove

### **The Formula in Action:**

```
ML Predicted Power = slope × Raw HVAC Power + intercept
                   = 2.1229 × rawSimulated + 47.1731
```

**Key Observations:**

1. **Raw HVAC changes** → Different scenarios produce different HVAC loads
2. **ML prediction changes proportionally** → Shows the linear relationship
3. **Intercept stays constant** → Represents fixed loads (lighting, equipment)
4. **Total energy varies** → Accumulation of step-by-step changes

### **Why This Is Important for Your VIVA:**

✅ **Proves dynamic behavior** - Not a static value, responds to conditions  
✅ **Shows ML in action** - Linear model scales predictions based on physics  
✅ **Validates the hybrid approach** - Physics handles HVAC, ML handles total building  
✅ **Demonstrates understanding** - You can explain each component of the formula

---

## 📸 Screenshots to Capture for Report

1. **Backend console** showing step-by-step power changes
2. **Frontend result** showing energy savings/increases
3. **Comparison table** showing Baseline vs Scenario differences
4. **Multiple test scenarios** proving bidirectional changes (up/down)

---

## 🎓 VIVA Talking Points

### When asked: "Does your prediction change dynamically?"

> "Yes, absolutely. Let me show you the logs from a test run..."
>
> *Point to console output:*
>
> "In the baseline scenario with 22°C target temperature, the HVAC draws ~13.8 kW, 
> which the ML model predicts as 76.5 kW total building power.
>
> When I reduce the target to 18°C in the What-If scenario, the HVAC only needs 
> ~8.5 kW to maintain that lower temperature. The ML model correctly scales this 
> down to 65.1 kW predicted power.
>
> This demonstrates the **linear relationship learned during training**: the model 
> multiplies the simulated HVAC load by 2.12 and adds 47 kW of base load (lighting, 
> equipment, etc.).
>
> The step-by-step logs prove that each 15-minute interval produces different power 
> values based on the modified parameters, not a static prediction."

### When asked: "How does ML help here?"

> "Without ML, our physics model only captures HVAC at ~13 kW. The Linear Regression 
> model learns that real buildings use ~76 kW total by discovering the relationship:
>
> `realPower = 2.12 × simulatedHVAC + 47 kW`
>
> This hybrid approach is computationally efficient and interpretable - much better 
> than trying to model every lightbulb and computer in the building!"

---

## ✅ Verification Checklist

Before your VIVA, make sure you've tested and captured:

- [ ] Lower temperature scenario (energy decreases)
- [ ] Higher temperature scenario (energy increases)
- [ ] Better insulation scenario (energy decreases)
- [ ] Worse insulation scenario (energy increases)
- [ ] Console logs showing step-by-step changes
- [ ] Frontend showing savings calculations
- [ ] Screenshots of at least 2 scenarios
- [ ] Can explain the formula: `y = 2.12x + 47`

---

**Last Updated:** December 30, 2025  
**Status:** ✅ Ready for Testing

