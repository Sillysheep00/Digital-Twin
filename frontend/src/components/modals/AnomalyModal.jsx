import React from 'react';
import ModalWrapper from '../ui/ModalWrapper';
import InfoTooltip from '../ui/Tooltip';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';
import { AlertTriangle, Loader2, RefreshCw, CircleDot, Circle, CheckCircle, BarChart3, Building2 } from 'lucide-react';

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

  if (!showAnomaly) return null;
  console.log('Anomaly Result:', anomalyResult);
  console.log('Has timeSteps:', anomalyResult?.timeSteps);
  console.log('Has realPowerHistory:', anomalyResult?.realPowerHistory);
  console.log('realPowerHistory sample:', anomalyResult?.realPowerHistory?.slice(0, 5));
  console.log('Has simulatedPowerHistory:', anomalyResult?.simulatedPowerHistory);
  console.log('Has predictedPowerHistory:', anomalyResult?.predictedPowerHistory);

  const getBorderColor = () => {
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
        return 'rgba(231, 76, 60, 0.1)'; // Dark red tint
      case 'WARNING':
        return 'rgba(243, 156, 18, 0.1)'; // Dark orange tint
      default:
        return 'rgba(39, 174, 96, 0.1)'; // Dark green tint (NORMAL)
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
    
    const currentWindowSize = windowSize || 32;
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

  // Prepare residual chart data
  const prepareResidualData = () => {
    if (!anomalyResult?.timeSteps || !anomalyResult.residuals) {
      return [];
    }

    // Filter to show only the last N steps based on selected window size
    const currentWindowSize = windowSize || 32;
    const dataLength = anomalyResult.timeSteps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);
    const timestamps = anomalyResult.timestamps || [];

    // Use threshold from backend (Z-score based) instead of calculating 25%
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
    <ModalWrapper onClose={() => setShowAnomaly(false)} title={<span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><AlertTriangle size={18} />ML-Based Anomaly Detection</span>}>

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
          {isCheckingAnomaly ? (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Loader2 size={16} className="animate-spin" />Checking...
            </span>
          ) : (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <RefreshCw size={16} />Check for Anomalies
            </span>
          )}
        </button>
      )}

      {/* Window Size Selector and Residual Chart - Show when data is available */}
      {anomalyResult && !anomalyResult.error && anomalyResult.timeSteps && (
        <>
          {/* Window Size Selector */}
          <div style={{ 
            marginBottom: '20px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '10px',
            padding: '10px',
            background: 'rgba(59, 130, 246, 0.1)',
            border: '1px solid rgba(59, 130, 246, 0.3)',
            borderRadius: '5px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1 }}>
              <InfoTooltip text="Time window for anomaly detection. Larger windows provide more context but may delay detection of recent anomalies." position="right">
                <label htmlFor="anomaly-window-size-select" style={{ fontWeight: 'bold', fontSize: '14px', color: '#FFFFFF' }}>
                  Analysis Window:
                </label>
              </InfoTooltip>
              <select
                id="anomaly-window-size-select"
                name="windowSize"
                value={windowSize || 32}
                onChange={(e) => setWindowSize(parseInt(e.target.value))}
                disabled={isCheckingAnomaly}
                style={{
                  padding: '8px 12px',
                  borderRadius: '5px',
                  border: '1px solid rgba(59, 130, 246, 0.3)',
                  fontSize: '14px',
                  cursor: isCheckingAnomaly ? 'not-allowed' : 'pointer',
                  background: 'rgba(30, 30, 30, 0.5)',
                  color: '#FFFFFF'
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
              {isCheckingAnomaly ? (
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Loader2 size={16} className="animate-spin" />Checking...
                </span>
              ) : (
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <RefreshCw size={16} />Check for Anomalies
                </span>
              )}
            </button>
          </div>

          {/* Residual Plot Chart */}
          <div style={{ background: 'rgba(30, 30, 30, 0.5)', border: '1px solid rgba(59, 130, 246, 0.3)', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <h4 style={{ marginTop: 0, color: '#FFFFFF' }}>Residuals (Actual − Predicted Power)</h4>
            <p style={{ 
              margin: '0 0 15px 0', 
              fontSize: '12px', 
              color: '#B0B0B0',
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
                  interval={getTickInterval() * 4 - 1}
                  angle={-45}
                  textAnchor="end"
                  height={60}
                />
                <YAxis 
                  label={{ value: 'Residual Power (kW)', angle: -90, position: 'insideLeft', offset: 10, dy: 80 }}
                  stroke="#666"
                />
                <Tooltip 
                  formatter={(value) => `${value} kW`}
                  labelFormatter={(label) => {
                    const dataPoint = prepareResidualData().find(d => d.timestamp === label);
                    return dataPoint?.fullTimestamp || label;
                  }}
                />
                <Legend />
                <Line 
                  type="monotone" 
                  dataKey="threshold" 
                  stroke="#f39c12" 
                  strokeWidth={2}
                  strokeDasharray="5 5"
                  name="Statistical Threshold (Z-score)" 
                  dot={false}
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
        </>
      )}

      {/* Room Anomaly Breakdown - Only show if room data exists */}
      {anomalyResult?.roomAnomalies && anomalyResult.roomAnomalies.length > 0 && (
        <div style={{ 
          background: 'rgba(30, 30, 30, 0.5)', 
          padding: '15px', 
          borderRadius: '8px', 
          marginBottom: '20px',
          border: '1px solid rgba(59, 130, 246, 0.3)'
        }}>
          <h4 style={{ marginTop: 0, marginBottom: '15px', color: '#FFFFFF', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Building2 size={18} /> Room Anomaly Breakdown
          </h4>
          <p style={{ 
            margin: '0 0 15px 0', 
            fontSize: '12px', 
            color: '#B0B0B0',
            fontStyle: 'italic'
          }}>
          </p>
          
          <div style={{ overflowX: 'auto' }}>
            <table style={{ 
              width: '100%', 
              borderCollapse: 'collapse',
              fontSize: '14px'
            }}>
              <thead>
                <tr style={{ 
                  background: 'rgba(59, 130, 246, 0.2)', 
                  borderBottom: '2px solid #3B82F6',
                  textAlign: 'left'
                }}>
                  <th style={{ padding: '10px', fontWeight: 'bold', color: '#FFFFFF' }}>Room</th>
                  <th style={{ padding: '10px', fontWeight: 'bold', textAlign: 'right', color: '#FFFFFF' }}>Residual (kW)</th>
                  <th style={{ padding: '10px', fontWeight: 'bold', textAlign: 'center', color: '#FFFFFF' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {anomalyResult.roomAnomalies.map((room, index) => {
                  const getStatusColor = (status) => {
                    if (status.includes('🔴') || status.toLowerCase().includes('critical') || status.toLowerCase().includes('anomaly')) return '#e74c3c';
                    if (status.includes('🟠') || status.toLowerCase().includes('warning')) return '#f39c12';
                    return '#27ae60';
                  };
                  
                  const getStatusText = (status) => {
                    // Remove emoji and get clean text
                    return status.replace(/🔴|🟠|🟢/g, '').trim();
                  };
                  
                  const getRowStyle = (isAnomaly) => ({
                    borderBottom: '1px solid rgba(59, 130, 246, 0.2)',
                    cursor: 'pointer',
                    transition: 'background 0.2s',
                    background: isAnomaly ? 'rgba(255, 107, 107, 0.15)' : 'transparent'
                  });
                  
                  return (
                    <tr 
                      key={room.roomId || index}
                      style={getRowStyle(room.anomalyDetected)}
                      onMouseEnter={(e) => {
                        if (room.anomalyDetected) {
                          e.currentTarget.style.background = 'rgba(255, 107, 107, 0.25)';
                        } else {
                          e.currentTarget.style.background = 'rgba(59, 130, 246, 0.1)';
                        }
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = room.anomalyDetected ? 'rgba(255, 107, 107, 0.15)' : 'transparent';
                      }}
                      onClick={() => {
                        console.log('Room clicked:', room.roomName);
                      }}
                    >
                      <td style={{ padding: '10px', fontWeight: '500', color: '#FFFFFF' }}>
                        {room.roomName}
                      </td>
                      <td style={{ 
                        padding: '10px', 
                        textAlign: 'right',
                        fontWeight: room.anomalyDetected ? 'bold' : 'normal',
                        color: room.anomalyDetected ? '#ff6b6b' : '#B0B0B0'
                      }}>
                        {room.residual?.toFixed(2) || '0.00'} kW
                      </td>
                      <td style={{ 
                        padding: '10px', 
                        textAlign: 'center',
                        fontWeight: 'bold',
                        color: getStatusColor(room.status)
                      }}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, justifyContent: 'center' }}>
                          <CircleDot size={14} style={{ fill: getStatusColor(room.status) }} />
                          {getStatusText(room.status)}
                        </span>
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
            background: 'rgba(59, 130, 246, 0.1)',
            border: '1px solid rgba(59, 130, 246, 0.3)',
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
              color: '#FFFFFF',
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}
          >
            {anomalyResult.severity === 'CRITICAL' 
              ? <><AlertTriangle size={20} /> ANOMALY DETECTED!</>
              : anomalyResult.severity === 'WARNING'
              ? <><AlertTriangle size={20} /> WARNING</>
              : <><CheckCircle size={20} /> System Normal</>}
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
              { label: 'Real Power', value: anomalyResult.realPower, color: '#FFFFFF', tooltip: 'Actual power consumption from historical sensor data (ground truth)' },
              { label: 'ML Predicted', value: anomalyResult.calibratedSimulatedPower, color: '#3498db', tooltip: 'Expected power consumption predicted by ML-calibrated model based on current conditions' },
              { label: 'Residual', value: anomalyResult.residual, color: anomalyResult.anomalyDetected ? '#e74c3c' : '#27ae60', threshold: anomalyResult.threshold, tooltip: 'Difference between real and predicted power (Real - Predicted). Large residuals indicate anomalies.' }
            ].map((item) => (
              <div
                key={item.label}
                style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  padding: '15px',
                  borderRadius: '8px',
                  textAlign: 'center',
                  border: '1px solid rgba(255, 255, 255, 0.1)'
                }}
              >
                <InfoTooltip text={item.tooltip} position="top">
                  <div style={{ fontSize: '12px', color: '#B0B0B0', marginBottom: '5px' }}>{item.label}</div>
                </InfoTooltip>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: item.color }}>
                  {item.value?.toFixed(2)} kW
                </div>
                {item.threshold && (
                  <div style={{ fontSize: '11px', color: '#B0B0B0' }}>
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
                background: 'rgba(30, 30, 30, 0.5)',
                border: '1px solid rgba(59, 130, 246, 0.3)',
                borderRadius: '5px',
                fontSize: '14px',
                color: '#FFFFFF',
                lineHeight: 1.6
              }}
            >
              <strong>Analysis:</strong>
              <p style={{ margin: '10px 0 0 0' }}>{anomalyResult.explanation}</p>
            </div>
          )}

          {/* Technical Details */}
          <details style={{ marginTop: '15px' }}>
            <summary style={{ cursor: 'pointer', fontWeight: 'bold', padding: '10px', color: '#FFFFFF', display: 'flex', alignItems: 'center', gap: 8 }}>
              <BarChart3 size={16} /> Technical Details
            </summary>
            <div
              style={{
                padding: '15px',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '5px',
                marginTop: '10px',
                fontSize: '13px',
                color: '#B0B0B0'
              }}
            >
              <p><strong style={{ color: '#FFFFFF' }}>Raw Simulated Power:</strong> {anomalyResult.simulatedPower?.toFixed(2)} kW</p>
              <p><strong style={{ color: '#FFFFFF' }}>ML-Calibrated Power:</strong> {anomalyResult.calibratedSimulatedPower?.toFixed(2)} kW</p>
              <p><strong style={{ color: '#FFFFFF' }}>Detection Method:</strong> Z-Score Statistical Analysis (Rolling Mean + Std)</p>
              <p><strong style={{ color: '#FFFFFF' }}>Threshold:</strong> {anomalyResult.threshold?.toFixed(2)} kW (Statistical threshold based on rolling statistics)</p>
              <p><strong style={{ color: '#FFFFFF' }}>Minimum Absolute Threshold:</strong> 5 kW (ignores residuals below this)</p>
              {anomalyResult.explanation && anomalyResult.explanation.includes('Z-score') && (
                <p style={{ marginTop: '10px', fontStyle: 'italic', color: '#B0B0B0' }}>
                  <strong style={{ color: '#FFFFFF' }}>Note:</strong> Detection uses rolling mean and standard deviation of residuals over the last {windowInfo.hours} hours.
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
            background: 'rgba(255, 107, 107, 0.15)',
            border: '1px solid rgba(255, 107, 107, 0.4)',
            borderRadius: '5px',
            color: '#ff6b6b'
          }}
        >
          <strong style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <AlertTriangle size={16} /> Detection Failed
          </strong>
          <p style={{ margin: '10px 0 0 0', fontSize: '14px', color: '#FFFFFF' }}>
            {anomalyResult.message || 'Failed to perform anomaly detection'}
          </p>
        </div>
      )}
    </ModalWrapper>
  );
}

export default AnomalyModal;