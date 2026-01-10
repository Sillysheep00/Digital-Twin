import ModalWrapper from '../ui/ModalWrapper';
import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';

const WINDOW_SIZE_OPTIONS = [
  { value: 32, label: '8 hours', hours: 8 },
  { value: 64, label: '16 hours', hours: 16 }, 
  { value: 96, label: '24 hours', hours: 24 } 
];

function AnomalyModal({
  showAnomaly,
  setShowAnomaly,
  anomalyResult,
  handleAnomalyCheck,
  isCheckingAnomaly,
  windowSize,
  setWindowSize
}) {
  const [activeChart, setActiveChart] = useState('trend'); // 'trend' or 'residual'

  if (!showAnomaly) return null;
  console.log('Anomaly Result:', anomalyResult);
  console.log('Has timeSteps:', anomalyResult?.timeSteps);
  console.log('Has realPowerHistory:', anomalyResult?.realPowerHistory);
  console.log('realPowerHistory sample:', anomalyResult?.realPowerHistory?.slice(0, 5));
  console.log('Has simulatedPowerHistory:', anomalyResult?.simulatedPowerHistory);
  console.log('Has predictedPowerHistory:', anomalyResult?.predictedPowerHistory);

  const getBorderColor = () =>{
    const severity = anomalyResult?.severity;
      switch (severity) {
        case 'CRITICAL':
          return '#e74c3c'; // Red
        case 'WARNING':
          return '#f39c12'; // Orange
        default:
          return '#27ae60'; // Green (NORMAL)
    }
  }

  const getBackgroundColor = () => {
    const severity = anomalyResult?.severity;
    switch (severity) {
      case 'CRITICAL':
        return '#fadbd8'; // Light red
      case 'WARNING':
        return '#fef5e7'; // Light orange
      default:
        return '#d4efdf'; // Light green (NORMAL)
    }
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return '#c0392b';
      case 'WARNING':
        return '#f39c12';
      default:
        return '#27ae60';
    }
  };

  // Helper function to get window info
  const getWindowInfo = () => {
    const option = WINDOW_SIZE_OPTIONS.find(opt => opt.value === windowSize);
    if (option) {
      return {
        steps: option.value,
        hours: option.hours,
        label: option.label
      };
    }
    // Fallback: calculate from windowSize if not in predefined options
    const hours = windowSize / 4; // 4 steps per hour (15-min resolution)
    return {
      steps: windowSize,
      hours: hours,
      label: `Last ${hours} hours`
    };
  };
  const windowInfo = getWindowInfo();

  // Helper function to format time range for subtitle
  const getTimeRangeSubtitle = () => {
    if (!anomalyResult?.timestamps || anomalyResult.timestamps.length === 0) {
      return `Window: Last ${windowInfo.hours} hours (${windowInfo.steps} steps, 15-min resolution)`;
    }
    
    const currentWindowSize = windowSize || 96;
    const dataLength = anomalyResult.timestamps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);
    
    const startTime = anomalyResult.timestamps[startIndex];
    const endTime = anomalyResult.timestamps[dataLength - 1];
    
    // Parse dates if available (check if we have full timestamps)
    // For now, assume timestamps are in HH:mm format
    return `Window: ${windowInfo.hours} hours (${startTime} → ${endTime}) | Resolution: 15 minutes`;
  };

  // Helper function to get tick interval based on window size
  const getTickInterval = () => {
    const hours = windowInfo.hours;
    if (hours === 8) return 1; // Every 1 hour (4 steps)
    if (hours === 16) return 2; // Every 2 hours (8 steps)
    if (hours === 24) return 3; // Every 3 hours (12 steps)
    return 2; // Default: every 2 hours
  };

  // Helper function to format timestamp for display
  const formatTimeForAxis = (timestamp) => {
    if (!timestamp) return '';
    // If timestamp is already in HH:mm format, return as is
    if (timestamp.length >= 5 && timestamp.includes(':')) {
      return timestamp.substring(0, 5); // "HH:mm"
    }
    return timestamp;
  };

  

   // Prepare chart data
   const prepareTrendData = () => {
    if (!anomalyResult?.timeSteps || !anomalyResult.simulatedPowerHistory || !anomalyResult.predictedPowerHistory) {
      return [];
    }
     // Filter to show only the last N steps based on selected window size
    const currentWindowSize = windowSize || 96;
    const dataLength = anomalyResult.timeSteps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);

    const timestamps = anomalyResult.timestamps || [];
    
    return anomalyResult.timeSteps.slice(startIndex).map((step, index) => {
      const actualIndex = startIndex + index;
      const timestamp = timestamps[actualIndex] || `Step ${index + 1}`;
      
      return {
        step: index + 1,
        timestamp: formatTimeForAxis(timestamp),
        fullTimestamp: timestamps[actualIndex] || null,
        simulated: anomalyResult.simulatedPowerHistory[actualIndex],
        predicted: anomalyResult.predictedPowerHistory[actualIndex]
      };
    });
  };

  const prepareResidualData = () => {
    if (!anomalyResult?.timeSteps || !anomalyResult.residuals) {
      return [];
    }

    // Filter to show only the last N steps based on selected window size
    const currentWindowSize = windowSize || 96;
    const dataLength = anomalyResult.timeSteps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);

    const timestamps = anomalyResult.timestamps || [];

     // FIX: Use threshold from backend (Z-score based) instead of calculating 25%
    // The backend now provides the correct statistical threshold
    const thresholdValue = anomalyResult.threshold || 0;
  
    
    return anomalyResult.timeSteps.slice(startIndex).map((step, index) => {
      const actualIndex = startIndex + index;
      const timestamp = timestamps[actualIndex] || `Step ${index + 1}`;
      const historicalRealPower = anomalyResult.realPowerHistory ? anomalyResult.realPowerHistory[actualIndex] : 0;
    
      return {
        step: index + 1,
        timestamp: formatTimeForAxis(timestamp),
        fullTimestamp: timestamps[actualIndex] || null,
        residual: anomalyResult.residuals[actualIndex],
        threshold: thresholdValue
      };
    });
  };


  return (
    <ModalWrapper onClose={() => setShowAnomaly(false)} title="🚨 ML-Based Anomaly Detection">
      <p style={{ color: '#666', marginBottom: '20px' }}>
        Using Linear Regression to detect unusual energy consumption patterns
      </p>

      {/* Initial Check Button - Show when no data yet */}
      {(!anomalyResult || anomalyResult.error || !anomalyResult.timeSteps) && (
        <button
          onClick={handleAnomalyCheck}
          disabled={isCheckingAnomaly}
          style={{
            width: '100%',
            padding: '12px',
            background: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontWeight: 'bold',
            cursor: isCheckingAnomaly ? 'not-allowed' : 'pointer',
            opacity: isCheckingAnomaly ? 0.6 : 1,
            marginBottom: '20px'
          }}
        >
          {isCheckingAnomaly ? '⏳ Checking...' : '🔄 Check for Anomalies'}
        </button>
      )}

       {/* Chart Toggle */}
       {anomalyResult && !anomalyResult.error && anomalyResult.timeSteps && (
        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
            <button
              onClick={() => setActiveChart('trend')}
              style={{
                flex: 1,
                padding: '10px',
                background: activeChart === 'trend' ? '#3498db' : '#ecf0f1',
                color: activeChart === 'trend' ? 'white' : '#2c3e50',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              📈 Power Trend Comparison
            </button>
            <button
              onClick={() => setActiveChart('residual')}
              style={{
                flex: 1,
                padding: '10px',
                background: activeChart === 'residual' ? '#3498db' : '#ecf0f1',
                color: activeChart === 'residual' ? 'white' : '#2c3e50',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              📊 Residual Plot
            </button>
          </div>

          {/* Window Size Selector */}
          <div style={{ 
            marginBottom: '20px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '10px',
            padding: '10px',
            background: '#f8f9fa',
            borderRadius: '5px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1 }}>
              <label htmlFor="anomaly-window-size-select" style={{ fontWeight: 'bold', fontSize: '14px' }}>
                  Analysis Window:
              </label>
              <select
                id="anomaly-window-size-select"
                name="windowSize"
                value={windowSize || 32}
                onChange={(e) => setWindowSize(parseInt(e.target.value))}
                disabled={isCheckingAnomaly}
                style={{
                  padding: '8px 12px',
                  borderRadius: '5px',
                  border: '1px solid #ddd',
                  fontSize: '14px',
                  cursor: isCheckingAnomaly ? 'not-allowed' : 'pointer',
                  background: 'white'
                }}
              >
                {WINDOW_SIZE_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label} ({option.value} steps)
                  </option>
                ))}
              </select>
            </div>

            {/* Refresh Button */}
            <button
              onClick={handleAnomalyCheck}
              disabled={isCheckingAnomaly}
              style={{
                padding: '12px 20px',
                background: '#3498db',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                fontWeight: 'bold',
                cursor: isCheckingAnomaly ? 'not-allowed' : 'pointer',
                opacity: isCheckingAnomaly ? 0.6 : 1,
                whiteSpace: 'nowrap'
              }}
            >
              {isCheckingAnomaly ? '⏳ Checking...' : '🔄 Check for Anomalies'}
            </button>
          </div>
            

          {/* Chart 1: Power Trend Comparison */}
          {activeChart === 'trend' && (
          <div style={{ background: 'white', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <h4 style={{ marginTop: 0 }}>Power Trend Comparison</h4>
            <p style={{ 
              margin: '0 0 15px 0', 
              fontSize: '12px', 
              color: '#666',
              fontStyle: 'italic'
            }}>
              {getTimeRangeSubtitle()}
            </p>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={prepareTrendData()}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                <XAxis 
                  dataKey="timestamp" 
                  label={{ value: 'Time', position: 'insideBottom', offset: -5 }}
                  stroke="#666"
                  interval={getTickInterval() * 4 - 1} // Convert hours to steps (4 steps per hour)
                  angle={-45}
                  textAnchor="end"
                  height={60}
                />
                <YAxis 
                  label={{ value: 'Power (kW)', angle: -90, position: 'insideLeft' }}
                  stroke="#666"
                />
                <Tooltip 
                  formatter={(value) => `${value} kW`}
                  labelFormatter={(label) => {
                    const dataPoint = prepareTrendData().find(d => d.timestamp === label);
                    return dataPoint?.fullTimestamp || label;
                  }}
                />
                <Legend />
                <Line 
                  type="monotone" 
                  dataKey="simulated" 
                  stroke="#ff6b6b" 
                  strokeWidth={2}
                  name="Simulated Power"
                  dot={{ r: 3 }}
                />
                <Line 
                    type="monotone" 
                    dataKey="predicted" 
                    stroke="#51cf66" 
                    strokeWidth={2}
                    name="Calibrated Power (Simulation + ML)"
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          {/* Chart 2: Residual Plot */}
          {activeChart === 'residual' && (
            <div style={{ background: 'white', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
              <h4 style={{ marginTop: 0 }}>Residuals (Actual − Predicted Power)</h4>
              <p style={{ 
                margin: '0 0 15px 0', 
                fontSize: '12px', 
                color: '#666',
                fontStyle: 'italic'
              }}>
                {getTimeRangeSubtitle()}
              </p>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={prepareResidualData()}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis 
                    dataKey="timestamp" 
                    label={{ value: 'Time', position: 'insideBottom', offset: -5 }}
                    stroke="#666"
                    interval={getTickInterval() * 4 - 1} // Convert hours to steps
                    angle={-45}
                    textAnchor="end"
                    height={60}
                  />
                  <YAxis 
                    label={{ value: 'Residual Power (kW)', angle: -90, position: 'insideLeft',offset:10,dy: 80}}
                    stroke="#666"
                  />
                  <Tooltip 
                    formatter={(value) => `${value} kW`}
                    labelFormatter={(label) => {
                      const dataPoint = prepareResidualData().find(d => d.timestamp === label);
                      return dataPoint?.fullTimestamp || label;
                    }}
                  />
                  {/* <ReferenceLine y={0} stroke="#666" strokeDasharray="3 3" /> */}
                  <Legend />
                  {/* --- ADD THIS DYNAMIC THRESHOLD LINE --- */}
                  <Line 
                    type="monotone" 
                    dataKey="threshold" 
                    stroke="#f39c12" 
                    strokeWidth={2}
                    strokeDasharray="5 5"   // Dashed line makes it look like a "limit"
                    name= "Statistical Threshold (Z-score)" 
                    dot={false}             // No dots, just a smooth boundary line
                    isAnimationActive={false}
                  />

                  <Line 
                    type="monotone" 
                    dataKey="residual" 
                    stroke="#e74c3c" 
                    strokeWidth={2}
                    name="Residual"
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      )}

      {/* Room Anomaly Breakdown - Only show if room data exists */}
      {anomalyResult?.roomAnomalies && anomalyResult.roomAnomalies.length > 0 && (
        <div style={{ 
          background: 'white', 
          padding: '15px', 
          borderRadius: '8px', 
          marginBottom: '20px',
          border: '1px solid #ddd'
        }}>
          <h4 style={{ marginTop: 0, marginBottom: '15px', color: '#2c3e50' }}>
            🏢 Room Anomaly Breakdown
          </h4>
          <p style={{ 
            margin: '0 0 15px 0', 
            fontSize: '12px', 
            color: '#666',
            fontStyle: 'italic'
          }}>
            Residuals calculated by proportionally allocating building power to rooms
          </p>
          
          <div style={{ overflowX: 'auto' }}>
            <table style={{ 
              width: '100%', 
              borderCollapse: 'collapse',
              fontSize: '14px'
            }}>
              <thead>
                <tr style={{ 
                  background: '#f5f5f5', 
                  borderBottom: '2px solid #ddd',
                  textAlign: 'left'
                }}>
                  <th style={{ padding: '10px', fontWeight: 'bold' }}>Room</th>
                  <th style={{ padding: '10px', fontWeight: 'bold', textAlign: 'right' }}>Residual (kW)</th>
                  <th style={{ padding: '10px', fontWeight: 'bold', textAlign: 'center' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {anomalyResult.roomAnomalies.map((room, index) => {
                  const getStatusColor = (status) => {
                    if (status.includes('🔴')) return '#e74c3c';
                    if (status.includes('🟠')) return '#f39c12';
                    return '#27ae60';
                  };
                  
                  const getRowStyle = (isAnomaly) => ({
                    borderBottom: '1px solid #eee',
                    cursor: 'pointer',
                    transition: 'background 0.2s',
                    background: isAnomaly ? '#fff5f5' : 'white'
                  });
                  
                  return (
                    <tr 
                      key={room.roomId || index}
                      style={getRowStyle(room.anomalyDetected)}
                      onMouseEnter={(e) => {
                        if (room.anomalyDetected) {
                          e.currentTarget.style.background = '#ffe5e5';
                        } else {
                          e.currentTarget.style.background = '#f9f9f9';
                        }
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = room.anomalyDetected ? '#fff5f5' : 'white';
                      }}
                      onClick={() => {
                        // Optional: Open room detail modal
                        console.log('Room clicked:', room.roomName);
                        // You can implement room detail modal here
                      }}
                    >
                      <td style={{ padding: '10px', fontWeight: '500' }}>
                        {room.roomName}
                      </td>
                      <td style={{ 
                        padding: '10px', 
                        textAlign: 'right',
                        fontWeight: room.anomalyDetected ? 'bold' : 'normal',
                        color: room.anomalyDetected ? '#e74c3c' : '#2c3e50'
                      }}>
                        {room.residual?.toFixed(2) || '0.00'} kW
                      </td>
                      <td style={{ 
                        padding: '10px', 
                        textAlign: 'center',
                        fontWeight: 'bold',
                        color: getStatusColor(room.status)
                      }}>
                        {room.status}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          
          {/* Summary Stats */}
          <div style={{ 
            marginTop: '15px', 
            padding: '10px', 
            background: '#f8f9fa',
            borderRadius: '4px',
            display: 'flex',
            justifyContent: 'space-around',
            fontSize: '12px'
          }}>
            <div>
              <strong>Total Rooms:</strong> {anomalyResult.roomAnomalies.length}
            </div>
            <div>
              <strong>Anomalies:</strong> {
                anomalyResult.roomAnomalies.filter(r => r.anomalyDetected).length
              }
            </div>
            <div>
              <strong>Normal:</strong> {
                anomalyResult.roomAnomalies.filter(r => !r.anomalyDetected).length
              }
            </div>
          </div>
        </div>
      )}

      {/* Results */}
      {anomalyResult && !anomalyResult.error && (
        <div
          style={{
            border: `2px solid ${getBorderColor()}`,
            borderRadius: '8px',
            padding: '20px',
            background: getBackgroundColor()
          }}
        >
          {/* Header */}
          <h3
            style={{
              marginTop: 0,
              color: getBorderColor(),
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}
          >
            {anomalyResult.severity === 'CRITICAL' 
              ? '⚠️ ANOMALY DETECTED!' 
              : anomalyResult.severity === 'WARNING'
              ? '⚠️ WARNING'
              : '✅ System Normal'}
            <span
              style={{
                fontSize: '14px',
                padding: '4px 12px',
                borderRadius: '12px',
                background: getSeverityColor(anomalyResult.severity),
                color: 'white'
              }}
            >
              {anomalyResult.severity}
            </span>
          </h3>

          {/* Power Comparison */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr 1fr',
              gap: '15px',
              marginBottom: '20px'
            }}
          >
            {[
              { label: 'Real Power', value: anomalyResult.realPower, color: '#2c3e50' },
              { label: 'ML Predicted', value: anomalyResult.calibratedSimulatedPower, color: '#3498db' },
              { label: 'Residual', value: anomalyResult.residual, color: anomalyResult.anomalyDetected ? '#e74c3c' : '#27ae60', threshold: anomalyResult.threshold }
            ].map((item) => (
              <div
                key={item.label}
                style={{
                  background: 'white',
                  padding: '15px',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}
              >
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>{item.label}</div>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: item.color }}>
                  {item.value?.toFixed(2)} kW
                </div>
                {item.threshold && (
                  <div style={{ fontSize: '11px', color: '#666' }}>
                    (Threshold: {item.threshold?.toFixed(2)} kW)
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Explanation */}
          {anomalyResult.explanation && (
            <div
              style={{
                padding: '15px',
                background: 'white',
                borderRadius: '5px',
                fontSize: '14px',
                lineHeight: 1.6
              }}
            >
              <strong>Analysis:</strong>
              <p style={{ margin: '10px 0 0 0' }}>{anomalyResult.explanation}</p>
            </div>
          )}

          {/* Technical Details */}
          <details style={{ marginTop: '15px' }}>
            <summary style={{ cursor: 'pointer', fontWeight: 'bold', padding: '10px' }}>
              📊 Technical Details
            </summary>
            <div
              style={{
                padding: '15px',
                background: 'white',
                borderRadius: '5px',
                marginTop: '10px',
                fontSize: '13px'
              }}
            >
              <p><strong>Raw Simulated Power:</strong> {anomalyResult.simulatedPower?.toFixed(2)} kW</p>
              <p><strong>ML-Calibrated Power:</strong> {anomalyResult.calibratedSimulatedPower?.toFixed(2)} kW</p>
              <p><strong>Detection Method:</strong> Z-Score Statistical Analysis (Rolling Mean + Std)</p>
              <p><strong>Threshold:</strong> {anomalyResult.threshold?.toFixed(2)} kW (Statistical threshold based on rolling statistics)</p>
              <p><strong>Minimum Absolute Threshold:</strong> 5 kW (ignores residuals below this)</p>
              {anomalyResult.explanation && anomalyResult.explanation.includes('Z-score') && (
                <p style={{ marginTop: '10px', fontStyle: 'italic', color: '#666' }}>
                  <strong>Note:</strong> Detection uses rolling mean and standard deviation of residuals over the last {windowInfo.hours} hours.
                </p>
              )}
            </div>
          </details>
        </div>
      )}

      {/* Error Handling */}
      {anomalyResult && anomalyResult.error && (
        <div
          style={{
            padding: '15px',
            background: '#f8d7da',
            border: '1px solid #f5c6cb',
            borderRadius: '5px',
            color: '#721c24'
          }}
        >
          <strong>⚠️ Detection Failed</strong>
          <p style={{ margin: '10px 0 0 0', fontSize: '14px' }}>
            {anomalyResult.message || 'Failed to perform anomaly detection'}
          </p>
        </div>
      )}
    </ModalWrapper>
  );
}

export default AnomalyModal;
