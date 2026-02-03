import ModalWrapper from '../ui/ModalWrapper';
import { CircleDot, Circle } from 'lucide-react';

function TemperatureModal({show, data, onClose }) {
  if (!show ||!data) return null;

  return (
    <ModalWrapper onClose={onClose}>
      
      <h2>All Room Temperatures</h2>

      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #3B82F6', textAlign: 'left' }}>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>Room</th>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>Temperature</th>
            <th style={{ padding: '10px', color: '#FFFFFF' }}>HVAC Status</th>
          </tr>
        </thead>
        <tbody>
          {data.rooms.map(r => (
            <tr key={r.id} style={{ borderBottom: '1px solid rgba(59, 130, 246, 0.2)' }}>
              <td style={{ padding: '10px', color: '#B0B0B0' }}>{r.name}</td>
              <td
                style={{
                  padding: '10px',
                  fontWeight: 'bold',
                  color: r.temp > 24 ? '#ff6b6b' : r.temp < 19 ? '#4dabf7' : '#51cf66'
                }}
              >
                {r.temp} °C
              </td>
              <td style={{ padding: '10px', color: '#B0B0B0' }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                  {r.hvac === "ON" ? <CircleDot size={14} color="#51cf66" /> : <Circle size={14} color="#adb5bd" />}
                  {r.hvac === "ON" ? "On" : "Off"}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
       
    </ModalWrapper>
  );
}

export default TemperatureModal;
