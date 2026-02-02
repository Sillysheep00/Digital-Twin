import ModalWrapper from '../ui/ModalWrapper';
import { CircleDot, Circle } from 'lucide-react';

function TemperatureModal({show, data, onClose }) {
  if (!show ||!data) return null;

  return (
    <ModalWrapper onClose={onClose}>
      
      <h2>All Room Temperatures</h2>

      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
            <th style={{ padding: '10px' }}>Room</th>
            <th style={{ padding: '10px' }}>Temperature</th>
            <th style={{ padding: '10px' }}>HVAC Status</th>
          </tr>
        </thead>
        <tbody>
          {data.rooms.map(r => (
            <tr key={r.id} style={{ borderBottom: '1px solid #eee' }}>
              <td style={{ padding: '10px' }}>{r.name}</td>
              <td
                style={{
                  padding: '10px',
                  fontWeight: 'bold',
                  color: r.temp > 24 ? 'red' : r.temp < 19 ? 'blue' : 'green'
                }}
              >
                {r.temp} °C
              </td>
              <td style={{ padding: '10px' }}>
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
