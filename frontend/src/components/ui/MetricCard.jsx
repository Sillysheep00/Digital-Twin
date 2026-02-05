const MetricCard = ({ label, value, prefix = '', suffix = '', color = '#00b894' }) => (
    <div
      style={{
        background: 'rgba(255, 255, 255, 0.05)',
        padding: '15px',
        borderRadius: '8px',
        border: '1px solid rgba(255, 255, 255, 0.1)'
      }}
    >
      <div style={{ fontSize: '12px', color: '#B0B0B0' }}>{label}</div>
      <div style={{ fontSize: '20px', fontWeight: 'bold', color }}>
        {prefix}{value}{suffix}
      </div>
    </div>
  );
  
  export default MetricCard;
  