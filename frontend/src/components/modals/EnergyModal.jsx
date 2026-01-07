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
          background: '#e8f4f8',
          borderRadius: '5px',
          border: '1px solid #3498db'
        }}>
          <strong style={{ display: 'block', marginBottom: '5px', color: '#2c3e50' }}>
            Simulation Period:
          </strong>
          <div style={{ color: '#34495e', fontSize: '14px' }}>
            {data.simulationStartTime} → {data.timestamp}
          </div>
        </div>
      )}


      <div style={{marginBottom: '15px',padding: '10px',background: '#f8f9fa',borderRadius: '5px'}}>
        <strong>Total Simulated Energy:</strong> {data.energy.total} kWh
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
            <th style={{ padding: '10px' }}>Room</th>
            <th style={{ padding: '10px' }}>Current Power (kW)</th>
            <th style={{ padding: '10px' }}>Total Energy (kWh)</th>
          </tr>
        </thead>
        <tbody>
          {data.rooms.map(r => (
            <tr key={r.id} style={{ borderBottom: '1px solid #eee' }}>
              <td style={{ padding: '10px' }}>{r.name}</td>
              <td style={{ padding: '10px' }}>{r.power} kW</td>
              <td style={{ padding: '10px', fontWeight: 'bold' }}>
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
