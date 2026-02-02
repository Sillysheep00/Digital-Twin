import ModalWrapper from '../ui/ModalWrapper';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { TrendingUp, Loader2, RefreshCw } from 'lucide-react';

const WINDOW_SIZE_OPTIONS = [
  { value: 32, label: '8 hours', hours: 8 },
  { value: 64, label: '16 hours', hours: 16 }, 
  { value: 96, label: '24 hours', hours: 24 } 
];

function PowerTrendModal({
  showPowerTrend,
  setShowPowerTrend,
  windowSize,
  setWindowSize,
  trendData,
  handleFetchTrends,
  isLoading
}) {
  if (!showPowerTrend) return null;

  // Check for error in trendData
  const error = trendData?.error ? trendData.message : null;

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
    const hours = windowSize / 4;
    return {
      steps: windowSize,
      hours: hours,
      label: `Last ${hours} hours`
    };
  };
  const windowInfo = getWindowInfo();

  // Helper function to format time range
  const getTimeRangeSubtitle = () => {
    if (!trendData?.timestamps || trendData.timestamps.length === 0) {
      return `Window: Last ${windowInfo.hours} hours (${windowInfo.steps} steps, 15-min resolution)`;
    }
    
    const currentWindowSize = windowSize || 96;
    const dataLength = trendData.timestamps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);
    
    const startTime = trendData.timestamps[startIndex];
    const endTime = trendData.timestamps[dataLength - 1];
    
    return `Window: ${windowInfo.hours} hours (${startTime} → ${endTime}) | Resolution: 15 minutes`;
  };

  // Helper function to get tick interval
  const getTickInterval = () => {
    const hours = windowInfo.hours;
    if (hours === 8) return 1;
    if (hours === 16) return 2;
    if (hours === 24) return 3;
    return 2;
  };

  // Helper function to format timestamp
  const formatTimeForAxis = (timestamp) => {
    if (!timestamp) return '';
    if (timestamp.length >= 5 && timestamp.includes(':')) {
      return timestamp.substring(0, 5);
    }
    return timestamp;
  };

  // Prepare chart data
  const prepareTrendData = () => {
    if (!trendData?.timeSteps || !trendData.simulatedPowerHistory || !trendData.predictedPowerHistory) {
      return [];
    }
    
    const currentWindowSize = windowSize || 32;
    const dataLength = trendData.timeSteps.length;
    const startIndex = Math.max(0, dataLength - currentWindowSize);
    const timestamps = trendData.timestamps || [];
    
    return trendData.timeSteps.slice(startIndex).map((step, index) => {
      const actualIndex = startIndex + index;
      const timestamp = timestamps[actualIndex] || `Step ${index + 1}`;
      
      return {
        step: index + 1,
        timestamp: formatTimeForAxis(timestamp),
        fullTimestamp: timestamps[actualIndex] || null,
        real: trendData.realPowerHistory ? trendData.realPowerHistory[actualIndex] : null,
        simulated: trendData.simulatedPowerHistory[actualIndex],
        physics: trendData.simulatedPhysicsPowerHistory ? trendData.simulatedPhysicsPowerHistory[actualIndex] : null,
        predicted: trendData.predictedPowerHistory[actualIndex]
      };
    });
  };

  return (
    <ModalWrapper onClose={() => setShowPowerTrend(false)} title={<span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><TrendingUp size={18} />Power Trends</span>}>
      <p style={{ color: '#666', marginBottom: '20px' }}>
        Compare simulated power consumption with ML-calibrated predictions over time
      </p>

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
          <label htmlFor="trend-window-size-select" style={{ fontWeight: 'bold', fontSize: '14px' }}>
            Analysis Window:
          </label>
          <select
            id="trend-window-size-select"
            value={windowSize || 32}
            onChange={(e) => {
              setWindowSize(parseInt(e.target.value));
              handleFetchTrends();
            }}
            disabled={isLoading}
            style={{
              padding: '8px 12px',
              borderRadius: '5px',
              border: '1px solid #ddd',
              fontSize: '14px',
              cursor: isLoading ? 'not-allowed' : 'pointer',
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
          onClick={() => handleFetchTrends()}
          disabled={isLoading}
          style={{
            padding: '12px 20px',
            background: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontWeight: 'bold',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            opacity: isLoading ? 0.6 : 1,
            whiteSpace: 'nowrap'
          }}
        >
          {isLoading ? (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Loader2 size={16} className="animate-spin" />Loading...
            </span>
          ) : (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <RefreshCw size={16} />Refresh
            </span>
          )}
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div style={{
          padding: '15px',
          background: '#fee',
          color: '#c33',
          borderRadius: '5px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      {/* Chart */}
      {trendData && !error && (
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
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={prepareTrendData()}>
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
                dataKey="real" 
                stroke="#3498db" 
                strokeWidth={2}
                name="Real Power (Ground Truth)"
                dot={{ r: 3 }}
                connectNulls={false}
              />
              <Line 
                type="monotone" 
                dataKey="simulated" 
                stroke="#ff6b6b" 
                strokeWidth={2}
                name="Simulated Power (Fast Est.)"
                dot={{ r: 3 }}
              />
              <Line 
                type="monotone" 
                dataKey="physics" 
                stroke="#ff922b" 
                strokeWidth={2}
                name="Simulated Power (Physics)"
                dot={{ r: 3 }}
                connectNulls={false}
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

      {/* Initial Load Message */}
      {!trendData && !error && !isLoading && (
        <div style={{
          padding: '20px',
          textAlign: 'center',
          color: '#666'
        }}>
          Click "Refresh" to load power trend data
        </div>
      )}
    </ModalWrapper>
  );
}

export default PowerTrendModal;