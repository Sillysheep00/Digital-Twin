# What-If Analysis - Troubleshooting Guide

## 🔴 Error: "Failed to load resource: the server responded with a status of 500"

### **What This Means**
The frontend successfully sent the request, but the backend encountered an internal error while processing it.

---

## 🔍 **Step-by-Step Diagnosis**

### **Step 1: Check Backend Console**

Look for these messages in your backend terminal:

```
=== WHAT-IF ANALYSIS START ===
Changes requested: {targetTemp=21.0}
Prediction horizon: 24 hours

[1/5] Running baseline prediction...
```

**What happened after `[1/5]`?** This tells us where it failed.

---

## 🐛 **Common Causes & Solutions**

### **Cause 1: Simulation Not Running Yet** ⭐ (MOST COMMON)

**Symptoms:**
- Error occurs immediately after starting backend
- Backend console shows: `ERROR: Baseline prediction failed`
- Message: "Simulation hasn't run yet"

**Why:**
The What-If analysis needs historical data to predict the future. If the simulation just started, there's no data yet.

**Solution:**
```
✅ Wait 2-3 minutes after starting the backend
✅ Let the simulation run through at least 8-10 steps
✅ Try clicking "🔮 Predict Next 24H" first to verify prediction works
✅ Then try What-If analysis again
```

**How to Verify:**
1. Check backend console for: `Step 10/21264 completed`
2. Check dashboard shows temperature values (not 0.0)
3. Try regular prediction first

---

### **Cause 2: No Prediction Data Available**

**Symptoms:**
- Backend console shows: `Found 0 future records for prediction`
- Error at step `[1/5]` or `[4/5]`

**Why:**
The simulation has reached the end of the dataset, so there's no future data to predict.

**Solution:**
```
✅ Restart the backend (simulation will loop back to start)
✅ Wait for simulation to run a few steps
✅ Try again
```

---

### **Cause 3: MongoDB Not Running**

**Symptoms:**
- Backend console shows: `Connection refused` or `MongoException`
- Error occurs during data fetching

**Why:**
The backend can't fetch sensor data from MongoDB.

**Solution:**
```
✅ Start MongoDB service:
   - Windows: net start MongoDB
   - Mac/Linux: sudo systemctl start mongod
✅ Verify MongoDB is running on port 27017
✅ Restart backend
```

---

### **Cause 4: Model Loading Error**

**Symptoms:**
- Backend console shows: `Failed to load model` or `Resource not found`
- Error at step `[2/5]`

**Why:**
The model files (.ecore or .smartoffice) are missing or corrupted.

**Solution:**
```
✅ Verify files exist:
   - src/main/resources/SmartOffice.ecore
   - src/main/resources/DigitalTwin.smartoffice
✅ Check file permissions
✅ Rebuild project: mvn clean install
```

---

### **Cause 5: EOL Transformation Error**

**Symptoms:**
- Backend console shows: `EOL transformation has parse errors`
- Error at step `[3/5]`

**Why:**
The dynamically generated EOL script has syntax errors.

**Solution:**
```
✅ Check your input parameters are valid numbers
✅ Temperature: 18-25
✅ Insulation: 0.01-0.08
✅ Restart backend if issue persists
```

---

## 🔧 **Quick Fixes**

### **Fix 1: Restart Everything**

```bash
# Stop backend (Ctrl+C)
# Stop frontend (Ctrl+C)

# Restart backend
cd C:\Users\cygoh\Desktop\DigitalTwin
mvn spring-boot:run

# Wait 2 minutes

# Restart frontend
cd frontend
npm run dev
```

---

### **Fix 2: Test Regular Prediction First**

Before trying What-If:
1. Click "🔮 Predict Next 24H"
2. If this works, What-If should work too
3. If this fails, wait longer for simulation to run

---

### **Fix 3: Check Backend Logs**

Look for these specific error messages:

```
❌ "ERROR: Baseline prediction failed"
   → Wait for simulation to run

❌ "Found 0 future records"
   → Restart backend (dataset loop)

❌ "Connection refused"
   → Start MongoDB

❌ "Resource not found"
   → Check model files exist

❌ "NullPointerException"
   → Check backend console for stack trace
```

---

## 📊 **Verification Steps**

### **Before Running What-If:**

1. ✅ Backend running (no errors in console)
2. ✅ Frontend running (dashboard loads)
3. ✅ MongoDB running (check with `mongosh`)
4. ✅ Simulation running (step count increasing)
5. ✅ Dashboard shows live data (temperatures updating)
6. ✅ Regular prediction works ("🔮 Predict Next 24H")

### **If All Above Pass:**

What-If analysis should work! If it still fails, check:

7. ✅ Parameters are valid (temp 18-25, insulation 0.01-0.08)
8. ✅ No typos in parameter names
9. ✅ Backend console shows all 5 steps completing

---

## 🎯 **Expected Behavior**

### **Successful What-If Analysis:**

Backend console should show:

```
=== WHAT-IF ANALYSIS START ===
Changes requested: {targetTemp=21.0}
Prediction horizon: 24 hours

[1/5] Running baseline prediction...
Running Prediction for next 24 hours...
   Found 96 future records for prediction.
Baseline energy: 49.5 kWh

[2/5] Loading model for scenario...
Loading SmartOffice model...

[3/5] Applying what-if changes to model...
  - Setting all HVAC target temperatures to: 21.0°C
  Executing EOL transformation...
  Transformation complete!

[4/5] Running scenario prediction...
Scenario energy: 45.3 kWh

[5/5] Calculating savings...

=== WHAT-IF RESULTS ===
Energy Saved: 4.2 kWh (8.5%)
Cost Saved: $0.63 (Annual: $230.0)
=== WHAT-IF ANALYSIS COMPLETE ===
```

---

## 🆘 **Still Not Working?**

### **Collect Debug Information:**

1. **Backend Console Output:**
   - Copy the entire error stack trace
   - Look for the line that says `ERROR in What-If Analysis:`

2. **Frontend Console (F12):**
   - Open browser developer tools
   - Check Console tab for errors
   - Look for network errors (Status 500)

3. **System Status:**
   - Backend running? ✅/❌
   - Frontend running? ✅/❌
   - MongoDB running? ✅/❌
   - Simulation step count? (should be > 10)

4. **Test Regular Prediction:**
   - Does "🔮 Predict Next 24H" work? ✅/❌
   - If no, What-If won't work either

---

## 💡 **Pro Tips**

1. **Always test regular prediction first** - If that works, What-If should work
2. **Wait 2 minutes after starting** - Simulation needs time to initialize
3. **Check step count** - Should be at least step 10 before trying What-If
4. **Monitor backend console** - It shows exactly where the error occurs
5. **Restart if stuck** - Sometimes a fresh start fixes everything

---

## 📝 **Error Messages Decoded**

| Error Message | Meaning | Solution |
|--------------|---------|----------|
| `Baseline prediction failed` | No data available yet | Wait 2 minutes |
| `Found 0 future records` | End of dataset | Restart backend |
| `Connection refused` | MongoDB not running | Start MongoDB |
| `Resource not found` | Model files missing | Check files exist |
| `NullPointerException` | Data structure issue | Check backend logs |
| `Status 500` | Backend internal error | See backend console |

---

## ✅ **Success Checklist**

After fixing, you should see:

- ✅ Backend shows all 5 steps completing
- ✅ Frontend displays results table
- ✅ Savings are calculated (positive or negative)
- ✅ Recommendation appears
- ✅ No errors in console

---

## 🎓 **Understanding the Error**

The 500 error means the backend code threw an exception. With the improved error handling I just added:

1. **Better Error Messages:** Backend now tells you exactly what failed
2. **Frontend Feedback:** UI shows helpful error message with solutions
3. **Graceful Degradation:** Error doesn't crash the app

---

**Most likely: Just wait 2 minutes after starting the backend, then try again!** ⏰

