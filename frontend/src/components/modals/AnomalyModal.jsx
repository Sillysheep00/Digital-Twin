import ModalWrapper from '../ui/ModalWrapper';

function AnomalyModal({
  showAnomaly,
  setShowAnomaly,
  anomalyResult,
  handleAnomalyCheck,
  isCheckingAnomaly
}) {
  if (!showAnomaly) return null;

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
