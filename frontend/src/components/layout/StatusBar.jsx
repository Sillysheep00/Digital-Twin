function StatusBar({ data }) {
  if (!data) return null;

  return (
    <div
      style={{
        position: 'absolute',
        bottom: '0',
        left: '0',
        right: '0',
        background: 'rgba(255, 255, 255, 0.95)',
        padding: '10px 20px',
        boxShadow: '0 -4px 12px rgba(0,0,0,0.1)',
        display: 'flex',
        gap: '30px'
      }}
    >
      <div>
        <b>Avg Temp:</b> {data.comfort.avgTemp} °C
      </div>
      <div>
        <b>Active HVACs:</b> {data.comfort.activeHvacs}
      </div>
      <div>
        <b>Total Power:</b> {data.power.simulated} kW
      </div>
      <div>
        <b>Energy:</b> {data.energy?.total?.toFixed(2)} kWh
      </div>
      <div style={{ marginLeft: 'auto', color: '#666' }}>
        Select a room in 3D to control it
      </div>
    </div>
  );
}

export default StatusBar;
