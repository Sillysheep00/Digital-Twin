import ModalWrapper from '../ui/ModalWrapper';

function EnergyModal({show, data, onClose }) {
  if (!show ||!data) return null;

  return (
    <ModalWrapper onClose={onClose}>
      <h2>Energy Consumption Report</h2>

      {/* Simulation Period Section */}
      {data.simulationStartTime && data.timestamp && (
        <div style={{
          marginBottom: '15px',
          padding: '10px',
          background: 'rgba(52, 152, 219, 0.15)',
          borderRadius: '5px',
          border: '1px solid rgba(59, 130, 246, 0.4)'
        }}>
          <strong style={{ display: 'block', marginBottom: '5px', color: '#FFFFFF' }}>
            Simulation Period:
          </strong>
          <div style={{ color: '#B0B0B0', fontSize: '14px' }}>
            {data.simulationStartTime} → {data.timestamp}
          </div>
        </div>
      )}


      <div style={{marginBottom: '15px',padding: '10px',background: 'rgba(59, 130, 246, 0.15)',borderRadius: '5px', border: '1px solid rgba(59, 130, 246, 0.3)'}}>
        <strong style={{color: '#FFFFFF'}}>Total Simulated Energy:</strong> <span style={{color: '#B0B0B0'}}>{data.energy.total} kWh</span>
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #3B82F6', textAlign: 'left' }}>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>Room</th>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>Current Power (kW)</th>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>Total Energy (kWh)</th>
          </tr>
        </thead>
        <tbody>
          {data.rooms.map(r => (
            <tr key={r.id} style={{ borderBottom: '1px solid rgba(59, 130, 246, 0.2)' }}>
              <td style={{ padding: '10px', color: '#B0B0B0' }}>{r.name}</td>
              <td style={{ padding: '10px', color: '#B0B0B0' }}>{r.power} kW</td>
              <td style={{ padding: '10px', fontWeight: 'bold', color: '#FFFFFF' }}>
                {r.energy} kWh
              </td>
            </tr>
          ))}
        </tbody>
      </table>
        
    </ModalWrapper>
  );
}

export default EnergyModal;
