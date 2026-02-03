function StatusBar({ data }) {
  if (!data) return null;

  return (
    <div
      style={{
        position: 'absolute',
        bottom: '0',
        left: '0',
        right: '0',
        background: 'rgba(30, 30, 30, 0.98)',
        padding: '12px 20px',
        boxShadow: '0 -4px 20px rgba(0,0,0,0.5)',
        borderTop: '1px solid rgba(59, 130, 246, 0.2)',
        display: 'flex',
        gap: '30px',
        color: '#FFFFFF'
      }}
    >
      <div>
        <b>Avg Temp:</b> <span style={{color: '#B0B0B0'}}>{data.comfort.avgTemp} °C</span>
      </div>
      <div>
        <b>Active HVACs:</b> <span style={{color: '#B0B0B0'}}>{data.comfort.activeHvacs}</span>
      </div>
      <div>
        <b>Total Power:</b> <span style={{color: '#B0B0B0'}}>{data.power.simulated} kW</span>
      </div>
      <div>
        <b>Energy:</b> <span style={{color: '#B0B0B0'}}>{data.energy?.total?.toFixed(2)} kWh</span>
      </div>
      <div style={{ marginLeft: 'auto', color: '#B0B0B0' }}>
        Select a room in 3D to control it
      </div>
    </div>
  );
}

export default StatusBar;
