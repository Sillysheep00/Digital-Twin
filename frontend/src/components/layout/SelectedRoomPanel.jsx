function SelectedRoomPanel({ selectedRoom, handleControl,activeMode }) {
  if (!selectedRoom) return null;

  // Check if room has HVAC system (from backend flag)
  const hasHvac = selectedRoom.hasHvac !== false; // Default to true if not specified (backward compatibility)

  // Default to AUTO if no mode is set
  const currentMode = activeMode || 'AUTO';

  // Helper function to get button style based on active state
  const getButtonStyle = (mode, baseColor, activeColor, textColor = '#000') => {
    const isActive = currentMode === mode;
    return {
      padding: '8px',
      cursor: 'pointer',
      borderRadius: '4px',
      border: isActive ? '3px solid #fff' : '2px solid transparent',
      background: isActive ? activeColor : baseColor,
      fontWeight: 'bold',
      color: textColor,
      boxShadow: isActive ? '0 0 10px rgba(255,255,255,0.5), 0 2px 4px rgba(0,0,0,0.3)' : '0 2px 4px rgba(0,0,0,0.2)',
      transform: isActive ? 'scale(1.05)' : 'scale(1)',
      transition: 'all 0.2s ease',
      opacity: isActive ? 1 : 0.9
    };
  };

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
        {hasHvac &&(
          <p style={{ margin: '5px 0' }}>
            <b>Status:</b> {selectedRoom.hvac === 'ON' ? 'Running' : 'Off'}
          </p>
        )}
        <p style={{ margin: '5px 0' }}>
          <b>Power:</b> {selectedRoom.power} kW
        </p>

        {/* Only show mode indicator if room has HVAC */}
        {hasHvac && (
          <p style={{ 
            margin: '5px 0', 
            fontSize: '12px', 
            color: '#ffd700',
            fontWeight: 'bold'
          }}>
            ⚡ Mode: {currentMode}
          </p>
        )}
      </div>

      {/* CONTROLS */}
      {hasHvac && (
        <>
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
              style={getButtonStyle('COOL', '#00d2d3', '#00ffff', '#000')}
            >❄️ COOL
            </button>

            <button
              onClick={() => handleControl('HEAT')}
              style={getButtonStyle('HEAT', '#ff9f43', '#ff6b35', '#000')}
            >🔥 HEAT
            </button>

            <button
              onClick={() => handleControl('OFF')}
              style={getButtonStyle('OFF', '#576574', '#c0392b', '#fff')}
            >⭕ OFF
            </button>

            <button
              onClick={() => handleControl('AUTO')}
              style={getButtonStyle('AUTO', '#2e86de', '#3498db', '#fff')}
            >🔄 AUTO
            </button>
          </div>
        </>
      )}

      {!hasHvac && (
        <p style={{ 
          marginTop: '15px', 
          fontSize: '13px', 
          fontStyle: 'italic',
          opacity: 0.8
        }}>
          ℹ️ This room does not have an HVAC system 
        </p>
      )}      
    </div>
  );
}

export default SelectedRoomPanel;
