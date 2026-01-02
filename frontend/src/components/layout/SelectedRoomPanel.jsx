function SelectedRoomPanel({ selectedRoom, handleControl }) {
  if (!selectedRoom) return null;

  return (
    <div
      style={{
        position: 'absolute',
        top: '20px',
        right: '20px',
        background: 'rgba(0, 123, 255, 0.95)',
        color: 'white',
        padding: '20px',
        borderRadius: '12px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
        minWidth: '300px'
      }}
    >
      <h2 style={{ margin: '0 0 15px 0', fontSize: '20px' }}>
        📍 {selectedRoom.name}
      </h2>

      <div style={{ fontSize: '14px', lineHeight: '1.8' }}>
        <p style={{ margin: '5px 0' }}>
          <b>Temp:</b> {selectedRoom.temp} °C
        </p>
        <p style={{ margin: '5px 0' }}>
          <b>Status:</b> {selectedRoom.hvac === 'ON' ? 'Running' : 'Off'}
        </p>
        <p style={{ margin: '5px 0' }}>
          <b>Power:</b> {selectedRoom.power} kW
        </p>
      </div>

      {/* CONTROLS */}
      <h4
        style={{
          margin: '15px 0 5px 0',
          borderBottom: '1px solid rgba(255,255,255,0.3)'
        }}
      >
        Manual Controls
      </h4>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
        <button
          onClick={() => handleControl('COOL')}
          style={{
            padding: '8px',
            cursor: 'pointer',
            borderRadius: '4px',
            border: 'none',
            background: '#00d2d3',
            fontWeight: 'bold',
            color: '#000'
          }}
        >
          ❄️ COOL
        </button>

        <button
          onClick={() => handleControl('HEAT')}
          style={{
            padding: '8px',
            cursor: 'pointer',
            borderRadius: '4px',
            border: 'none',
            background: '#ff9f43',
            fontWeight: 'bold',
            color: '#000'
          }}
        >
          🔥 HEAT
        </button>

        <button
          onClick={() => handleControl('OFF')}
          style={{
            padding: '8px',
            cursor: 'pointer',
            borderRadius: '4px',
            border: 'none',
            background: '#576574',
            fontWeight: 'bold',
            color: '#fff'
          }}
        >
          ⭕ OFF
        </button>

        <button
          onClick={() => handleControl('AUTO')}
          style={{
            padding: '8px',
            cursor: 'pointer',
            borderRadius: '4px',
            border: 'none',
            background: '#2e86de',
            fontWeight: 'bold',
            color: '#fff'
          }}
        >
          🔄 AUTO
        </button>
      </div>
    </div>
  );
}

export default SelectedRoomPanel;
