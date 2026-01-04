const MetricCard = ({ label, value, prefix = '', suffix = '', color = '#00b894' }) => (
    <div
      style={{
        background: 'white',
        padding: '15px',
        borderRadius: '8px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
      }}
    >
      <div style={{ fontSize: '12px', color: '#666' }}>{label}</div>
      <div style={{ fontSize: '20px', fontWeight: 'bold', color }}>
        {prefix}{value}{suffix}
      </div>
    </div>
  );
  
  export default MetricCard;
  