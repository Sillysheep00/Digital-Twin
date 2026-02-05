import Tooltip from '../ui/Tooltip';

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
      <Tooltip text="Average indoor temperature" position="top">
        <div>
          <b>Avg Temp:</b> <span style={{color: '#B0B0B0'}}>{data.comfort.avgTemp} °C</span>
        </div>
      </Tooltip>
      <Tooltip text="Number of active HVAC systems" position="top">
        <div>
          <b>Active HVACs:</b> <span style={{color: '#B0B0B0'}}>{data.comfort.activeHvacs}</span>
        </div>
      </Tooltip>
      <Tooltip text="Total power (physics-based simulation)" position="top">
        <div>
          <b>Total Power:</b> <span style={{color: '#B0B0B0'}}>{data.power.simulated_physics_raw} kW</span>
        </div>
      </Tooltip>
      <Tooltip text="Cumulative energy consumption" position="top">
        <div>
          <b>Energy:</b> <span style={{color: '#B0B0B0'}}>{data.energy?.total?.toFixed(2)} kWh</span>
        </div>
      </Tooltip>
      <div style={{ marginLeft: 'auto', color: '#B0B0B0' }}>
        Select a room in 3D to control it
      </div>
    </div>
  );
}

export default StatusBar;
