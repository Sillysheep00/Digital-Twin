import ModalWrapper from '../ui/ModalWrapper';
import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';


function AnomalyModal({
  showAnomaly,
  setShowAnomaly,
  anomalyResult,
  handleAnomalyCheck,
  isCheckingAnomaly
}) {
  const [activeChart, setActiveChart] = useState('trend'); // 'trend' or 'residual'

  if (!showAnomaly) return null;
  console.log('Anomaly Result:', anomalyResult);
  console.log('Has timeSteps:', anomalyResult?.timeSteps);
  console.log('Has realPowerHistory:', anomalyResult?.realPowerHistory);
  console.log('realPowerHistory sample:', anomalyResult?.realPowerHistory?.slice(0, 5));
  console.log('Has simulatedPowerHistory:', anomalyResult?.simulatedPowerHistory);
  console.log('Has predictedPowerHistory:', anomalyResult?.predictedPowerHistory);

  const getBorderColor = () =>
    anomalyResult?.anomalyDetected ? '#e74c3c' : '#27ae60';

  const getBackgroundColor = () =>
    anomalyResult?.anomalyDetected ? '#fadbd8' : '#d4efdf';

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
   // Prepare chart data
   const prepareTrendData = () => {
    if (!anomalyResult?.timeSteps || !anomalyResult.simulatedPowerHistory || !anomalyResult.predictedPowerHistory) {
      return [];
    }
    return anomalyResult.timeSteps.map((step, index) => ({
      step,
      simulated: anomalyResult.simulatedPowerHistory[index],
      predicted: anomalyResult.predictedPowerHistory[index]
    }));
  };

  const prepareResidualData = () => {
    if (!anomalyResult?.timeSteps || !anomalyResult.residuals) {
      return [];
    }
    return anomalyResult.timeSteps.map((step, index) => {
      // Get the real power for this specific moment in history
      const historicalRealPower = anomalyResult.realPowerHistory ? anomalyResult.realPowerHistory[index] : 0;
    
      // Calculate dynamic threshold (15%)
      const dynamicThreshold = historicalRealPower * 0.25;
      return{
        step,
        residual: anomalyResult.residuals[index],
        threshold: dynamicThreshold
      };
    });
  };


  return (
    <ModalWrapper onClose={() => setShowAnomaly(false)} title="🚨 ML-Based Anomaly Detection">
      <p style={{ color: '#666', marginBottom: '20px' }}>
        Using Linear Regression to detect unusual energy consumption patterns
      </p>

      {/* Refresh Button */}
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

          {/* Chart 1: Power Trend Comparison */}
          {activeChart === 'trend' && (
          <div style={{ background: 'white', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <h4 style={{ marginTop: 0 }}>Power Trend Comparison</h4>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={prepareTrendData()}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                <XAxis 
                  dataKey="step" 
                  label={{ value: 'Time Step', position: 'insideBottom', offset: -5 }}
                  stroke="#666"
                />
                <YAxis 
                  label={{ value: 'Power (kW)', angle: -90, position: 'insideLeft' }}
                  stroke="#666"
                />
                <Tooltip 
                  formatter={(value) => `${value} kW`}
                  labelFormatter={(label) => `Step ${label}`}
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
                    name="ML-Predicted Power"
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
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={prepareResidualData()}>
                  
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis 
                    dataKey="step" 
                    label={{ value: 'Time Step', position: 'insideBottom', offset: -5 }}
                    stroke="#666"
                  />
                  <YAxis 
                    label={{ value: 'Residual Power (kW)', angle: -90, position: 'Left' }}
                    stroke="#666"
                  />
                  <Tooltip 
                    formatter={(value) => `${value} kW`}
                    labelFormatter={(label) => `Step ${label}`}
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
                    name="Warning Limit (25%)" 
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
            {anomalyResult.anomalyDetected ? '⚠️ ANOMALY DETECTED!' : '✅ System Normal'}
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
              <p><strong>Detection Method:</strong> Residual-based with Linear Regression</p>
              <p><strong>Threshold:</strong> 25% of real power (min 5 kW)</p>
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
