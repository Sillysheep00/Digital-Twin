# Anomaly Detection Frontend Implementation

## Overview

Successfully added a **🚨 Anomaly Detection** button and modal to the Digital Twin Dashboard frontend, allowing users to detect unusual energy consumption patterns using the ML-based backend API.

## Changes Made

### 1. **State Variables Added** (Lines 13-28)

```javascript
// Modal States
const [showAnomalyModal, setShowAnomalyModal] = useState(false);

// Anomaly Detection States
const [anomalyResult, setAnomalyResult] = useState(null);
const [isCheckingAnomaly, setIsCheckingAnomaly] = useState(false);
```

### 2. **Handler Function Added** (After line 101)

```javascript
const handleAnomalyCheck = async () => {
    setIsCheckingAnomaly(true);
    setAnomalyResult(null);
    try {
        const response = await axios.get('http://localhost:8080/api/anomaly');
        setAnomalyResult(response.data);
    } catch (err) {
        console.error("Anomaly detection failed:", err);
        alert("Failed to check for anomalies. Ensure backend is running.");
        setAnomalyResult({
            error: true,
            message: "Failed to connect to anomaly detection service"
        });
    } finally {
        setIsCheckingAnomaly(false);
    }
};
```

### 3. **Button Added** (Line ~195)

```javascript
<button 
    onClick={() => {
        setShowAnomalyModal(true);
        handleAnomalyCheck();
    }}
    style={{ 
        padding: '10px', 
        borderRadius: '5px', 
        border: 'none', 
        cursor: 'pointer', 
        background: '#e74c3c',  // Red color
        color: 'white', 
        boxShadow: '0 2px 4px rgba(0,0,0,0.2)', 
        fontWeight: 'bold' 
    }}>
    🚨 Anomaly Detection
</button>
```

### 4. **Modal Added** (After Energy Usage Modal)

The modal includes:

#### **Header Section**
- Title: "🚨 ML-Based Anomaly Detection"
- Description explaining Linear Regression usage
- "Check for Anomalies" button to refresh detection

#### **Results Display**
When anomaly data is available:

**Status Banner:**
- Green border + "✅ System Normal" for normal operation
- Red border + "⚠️ ANOMALY DETECTED!" for anomalies
- Severity badge: NORMAL (green) / WARNING (orange) / CRITICAL (red)

**Power Comparison Grid (3 columns):**
1. **Real Power**: Actual measured power from sensors
2. **ML Predicted**: Power predicted by Linear Regression model
3. **Residual**: Difference between real and predicted (with threshold)

**Explanation Section:**
- Detailed analysis message from the backend
- Contextual explanation of the anomaly or normal state

**Technical Details (Collapsible):**
- Raw simulated power
- ML-calibrated power
- Detection method description
- Threshold calculation info

#### **Error Handling**
- Displays user-friendly error messages if detection fails
- Shows connection issues or backend errors

## Features

### ✅ Real-Time Detection
- Fetches current anomaly status on button click
- Can refresh detection at any time

### ✅ Visual Indicators
- Color-coded severity levels (NORMAL/WARNING/CRITICAL)
- Clear visual distinction between normal and anomalous states

### ✅ Detailed Analysis
- Shows exact power values (real vs predicted)
- Displays residual and threshold
- Provides contextual explanation

### ✅ User-Friendly
- Clean, modern UI matching existing modals
- Responsive layout with grid system
- Collapsible technical details for advanced users

## API Integration

**Endpoint**: `GET http://localhost:8080/api/anomaly`

**Response Format**:
```json
{
  "anomalyDetected": false,
  "realPower": 44.49,
  "simulatedPower": 13.81,
  "calibratedSimulatedPower": 44.47,
  "residual": 0.02,
  "threshold": 11.12,
  "severity": "NORMAL",
  "explanation": "System operating normally. Power consumption matches ML predictions."
}
```

## UI Design

### Color Scheme
- **Normal State**: Green (#27ae60) with light green background (#d4efdf)
- **Anomaly State**: Red (#e74c3c) with light red background (#fadbd8)
- **Button**: Red (#e74c3c) to indicate alertness
- **Severity Badges**:
  - NORMAL: Green (#27ae60)
  - WARNING: Orange (#f39c12)
  - CRITICAL: Dark Red (#c0392b)

### Layout
- Modal overlay with centered content
- Responsive grid layout for power metrics
- Collapsible sections for technical details
- Consistent with existing modal styling

## How to Use

1. **Start Backend**: Ensure Spring Boot backend is running with ML model trained
2. **Open Dashboard**: Access `http://localhost:3000` (or your frontend URL)
3. **Click Button**: Click the "🚨 Anomaly Detection" button in the top-left area
4. **View Results**: Modal opens and automatically checks for anomalies
5. **Refresh**: Click "🔄 Check for Anomalies" to refresh detection
6. **Close**: Click × to close the modal

## Testing Checklist

- [x] Button appears in the dashboard
- [x] Clicking button opens modal and triggers detection
- [x] Loading state shows "⏳ Checking..." during API call
- [x] Normal operation displays with green styling
- [x] Anomaly displays with red styling and appropriate severity
- [x] Refresh button works to update detection
- [x] Error handling works when backend is offline
- [x] Modal closes properly
- [x] Responsive layout works on different screen sizes

## Next Steps (Optional Enhancements)

1. **Auto-Refresh**: Add option to continuously monitor for anomalies
2. **History**: Show historical anomaly events
3. **Notifications**: Desktop/browser notifications for critical anomalies
4. **Charts**: Add graphs showing power trends and residuals over time
5. **Filters**: Filter anomalies by severity level
6. **Export**: Export anomaly reports as PDF or CSV

---

**Implementation Date**: December 30, 2025  
**Status**: ✅ Complete and functional

