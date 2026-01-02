import ModalWrapper from '../ui/ModalWrapper';

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
                {r.hvac === "ON" ? "🟢 On" : "⚪ Off"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
       
    </ModalWrapper>
  );
}

export default TemperatureModal;
