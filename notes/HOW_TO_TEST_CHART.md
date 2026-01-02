# How to Test the What-If Chart Feature

## 🚀 Quick Start Guide

### **Step 1: Start the Backend**
```bash
cd C:\Users\cygoh\Desktop\DigitalTwin
mvn spring-boot:run
```

**Wait for:** `"Simulation loop started"` message in console

---

### **Step 2: Start the Frontend**
```bash
cd frontend
npm run dev
```

**Open browser:** http://localhost:5173

---

### **Step 3: Test the Chart**

#### **3.1 Open What-If Analysis**
- Click the **"🔬 What-If Analysis"** button (top-left panel)

#### **3.2 Configure Scenario**
- **Target Temperature:** Drag slider to **20°C** (lower = more efficient)
- **Insulation Quality:** Drag slider to **0.02** (lower = better insulation)
- **Prediction Horizon:** Select **24 hours**

#### **3.3 Run Analysis**
- Click **"▶️ Run What-If Analysis"** button
- Wait ~5-10 seconds for simulation to complete

#### **3.4 View Results**
You should see:
```
┌─────────────────────────────────────────┐
│ 📊 Analysis Results                     │
├─────────────────────────────────────────┤
│ Metric    │ Baseline │ Scenario │ Diff  │
│ Energy    │ 1234 kWh │ 1089 kWh │ -145  │
├─────────────────────────────────────────┤
│ Cost Saved (24h): $21.80                │
│ Annual Savings: $332.35                 │
├─────────────────────────────────────────┤
│ [📊 View Energy Comparison Chart]       │
└─────────────────────────────────────────┘
```

#### **3.5 Open Chart Modal**
- Click **"📊 View Energy Comparison Chart"** button
- Chart modal should open with:
  - **Red line** = Baseline scenario
  - **Green line** = What-If scenario
  - **X-axis** = Hours (1-24)
  - **Y-axis** = Energy (kWh)

#### **3.6 Interact with Chart**
- **Hover over lines** → Tooltip shows exact values
- **Check legend** → Color-coded labels
- **View summary stats** → Below chart
- **Verify calculations** → Energy saved, percentage, cost

---

## ✅ What to Verify

### **Chart Display**
- [ ] Chart renders without errors
- [ ] Two lines visible (red and green)
- [ ] X-axis shows hours 1-24
- [ ] Y-axis shows energy in kWh
- [ ] Grid lines visible
- [ ] Legend shows "Baseline Scenario" and "What-If Scenario"

### **Interactivity**
- [ ] Hover shows tooltip with exact values
- [ ] Tooltip displays "Hour X" and energy values
- [ ] Lines are smooth and continuous
- [ ] No gaps or missing data points

### **Summary Stats**
- [ ] Baseline total matches table above
- [ ] What-If total matches table above
- [ ] Energy saved calculation is correct
- [ ] Percentage saved is correct
- [ ] Cost savings displayed

### **Visual Quality**
- [ ] Chart is responsive (fits modal)
- [ ] Colors are clear (red vs green)
- [ ] Text is readable
- [ ] No overlapping elements
- [ ] Close button works

---

## 🐛 Troubleshooting

### **Problem: "Analysis Failed" Error**
**Solution:**
- Wait 2 minutes after starting backend
- Ensure MongoDB is running
- Check backend console for errors

### **Problem: Chart Button Not Visible**
**Solution:**
- Ensure analysis completed successfully
- Check browser console for errors
- Verify `chartData` exists in response

### **Problem: Chart Shows No Data**
**Solution:**
- Check backend logs for simulation errors
- Verify `chartData` array is not empty
- Ensure hours parameter is valid (12, 24, 48, 72)

### **Problem: Lines Overlap Completely**
**Solution:**
- This means your changes had no effect
- Try more extreme parameter changes:
  - Temperature: 18°C or 25°C
  - Insulation: 0.01 or 0.08

---

## 📊 Expected Chart Behavior

### **Scenario 1: Better Insulation (0.02)**
- Green line (What-If) should be **BELOW** red line (Baseline)
- Energy saved should be **POSITIVE**
- Message: "✅ This scenario would save energy"

### **Scenario 2: Higher Temperature (25°C)**
- Green line (What-If) should be **ABOVE** red line (Baseline)
- Energy saved should be **NEGATIVE**
- Message: "⚠️ This scenario would increase energy consumption"

### **Scenario 3: No Changes**
- Lines should **OVERLAP** completely
- Energy saved should be **~0**

---

## 🎥 Demo Script (for VIVA)

1. **"Let me demonstrate the What-If analysis feature..."**
   - Open What-If modal

2. **"I'll test a scenario with better insulation..."**
   - Set insulation to 0.02
   - Set temperature to 20°C

3. **"The system runs two simulations..."**
   - Click Run Analysis
   - Explain: "One with current settings, one with my changes"

4. **"Here are the results..."**
   - Point to energy saved
   - Point to cost savings

5. **"Now let's visualize this over time..."**
   - Click View Chart button

6. **"This interactive chart shows..."**
   - Point to red line: "Baseline energy usage"
   - Point to green line: "Optimized scenario"
   - Hover to show tooltip: "Exact values at each hour"

7. **"The gap between lines represents energy saved..."**
   - Point to difference
   - Show summary stats

8. **"This helps users understand the impact of their changes..."**
   - Close chart
   - Summarize benefits

---

## 📸 Screenshots to Take

1. What-If modal with parameters
2. Analysis results table
3. Chart button highlighted
4. Full chart modal
5. Tooltip on hover
6. Summary statistics
7. Cost savings display

---

## ✅ Success Criteria

- [x] Backend compiles without errors
- [x] Frontend runs without errors
- [x] What-If analysis completes successfully
- [x] Chart button appears in results
- [x] Chart modal opens
- [x] Two lines render correctly
- [x] Tooltips work on hover
- [x] Summary stats match analysis results
- [x] Close button works
- [x] No console errors

**If all checked: ✅ Feature is working correctly!**

---

## 🎓 Key Points for VIVA

1. **"All calculations happen on the backend"**
   - Frontend just renders pre-calculated data

2. **"The chart uses the same simulation engine"**
   - Not separate calculations, actual prediction data

3. **"Data is aggregated for clarity"**
   - 96 steps → 24 hours for better visualization

4. **"It's interactive and responsive"**
   - Modern web development best practices

5. **"No external dependencies needed"**
   - Pure Java backend + React frontend

---

**Testing Date:** January 1, 2026  
**Status:** Ready for Demo ✅




