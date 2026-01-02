import ModalWrapper from '../ui/ModalWrapper';
import {LineChart,Line,XAxis,YAxis,CartesianGrid,Tooltip,Legend,ResponsiveContainer} from 'recharts';

function ChartModal({show, whatIfResult, whatIfParams, onClose }) {
  if (!show ||!whatIfResult || !whatIfResult.chartData) return null;

  return (
    <ModalWrapper onClose={onClose} maxWidth="1100px">
      <h2 style={{ marginBottom: '10px' }}>📊 Energy Usage Comparison</h2>

      <p style={{ color: '#666', marginBottom: '20px', fontSize: '14px' }}>
        Comparing baseline scenario vs. your what-if scenario over {whatIfParams.hours} hours
      </p>

      <ResponsiveContainer width="100%" height={400}>
        <LineChart
          data={whatIfResult.chartData}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
          <XAxis
            dataKey="hour"
            label={{ value: 'Hour', position: 'insideBottom', offset: -5 }}
            stroke="#666"
          />
          <YAxis
            label={{ value: 'Energy (kWh)', angle: -90, position: 'insideLeft' }}
            stroke="#666"
          />
          <Tooltip
            contentStyle={{
              background: 'rgba(255, 255, 255, 0.95)',
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '10px'
            }}
            formatter={(value) => `${value} kWh`}
            labelFormatter={(label) => `Hour ${label}`}
          />
          <Legend wrapperStyle={{ paddingTop: '20px' }} iconType="line" />

          <Line
            type="monotone"
            dataKey="baseline"
            stroke="#ff6b6b"
            strokeWidth={3}
            name="Baseline Scenario"
            dot={{ r: 4, fill: '#ff6b6b' }}
            activeDot={{ r: 6 }}
          />
          <Line
            type="monotone"
            dataKey="whatif"
            stroke="#51cf66"
            strokeWidth={3}
            name="What-If Scenario"
            dot={{ r: 4, fill: '#51cf66' }}
            activeDot={{ r: 6 }}
          />
        </LineChart>
      </ResponsiveContainer>

      {/* Summary Stats */}
      <div
        style={{
          marginTop: '30px',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr',
          gap: '15px',
          padding: '20px',
          background: '#f8f9fa',
          borderRadius: '8px'
        }}
      >
        <SummaryBox
          label="Baseline Total"
          value={`${whatIfResult.baseline.predictedEnergy.toFixed(2)} kWh`}
          color="#ff6b6b"
        />
        <SummaryBox
          label="What-If Total"
          value={`${whatIfResult.scenario.predictedEnergy.toFixed(2)} kWh`}
          color="#51cf66"
        />
        <SummaryBox
          label={whatIfResult.energySaved > 0 ? 'Energy Saved' : 'Extra Energy'}
          value={`${Math.abs(whatIfResult.energySaved).toFixed(2)} kWh`}
          color={whatIfResult.energySaved > 0 ? '#00b894' : '#d63031'}
        />
      </div>

      <button
        onClick={onClose}
        style={{
          width: '100%',
          marginTop: '20px',
          padding: '10px',
          background: '#6c757d',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          cursor: 'pointer',
          fontWeight: 'bold'
        }}
      >
        Close Chart
      </button>
    </ModalWrapper>
  );
}

function SummaryBox({ label, value, color }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
        {label}
      </div>
      <div style={{ fontSize: '24px', fontWeight: 'bold', color }}>
        {value}
      </div>
    </div>
  );
}

export default ChartModal;
