import { MapPin, Snowflake, Flame, Power, RefreshCw, Info, Zap } from 'lucide-react';

function SelectedRoomPanel({ selectedRoom, handleControl,activeMode }) {
  if (!selectedRoom) return null;

  // Check if room has HVAC system (from backend flag)
  const hasHvac = selectedRoom.hasHvac !== false; 

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
      onClick={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        top: '20px',
        right: '20px',
        background: 'rgba(30, 64, 175, 0.85)',
        backdropFilter: 'blur(10px)',
        WebkitBackdropFilter: 'blur(10px)',
        color: 'white',
        padding: '20px',
        borderRadius: '12px',
        boxShadow: '0 8px 32px rgba(59, 130, 246, 0.4)',
        border: '1px solid rgba(59, 130, 246, 0.3)',
        minWidth: '300px',
        zIndex: 20
      }}
    >
      <h2 style={{ margin: '0 0 15px 0', fontSize: '20px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <MapPin size={20} />{selectedRoom.name}
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
            fontWeight: 'bold',
            display: 'flex',
            alignItems: 'center',
            gap: 6
          }}>
            <Zap size={12} />Mode: {currentMode}
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
              style={{...getButtonStyle('COOL', '#00d2d3', '#00ffff', '#000'), display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6}}
            ><Snowflake size={14} />COOL
            </button>

            <button
              onClick={() => handleControl('HEAT')}
              style={{...getButtonStyle('HEAT', '#ff9f43', '#ff6b35', '#000'), display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6}}
            ><Flame size={14} />HEAT
            </button>

            <button
              onClick={() => handleControl('OFF')}
              style={{...getButtonStyle('OFF', '#576574', '#c0392b', '#fff'), display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6}}
            ><Power size={14} />OFF
            </button>

            <button
              onClick={() => handleControl('AUTO')}
              style={{...getButtonStyle('AUTO', '#2e86de', '#3498db', '#fff'), display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6}}
            ><RefreshCw size={14} />AUTO
            </button>
          </div>
        </>
      )}

      {!hasHvac && (
        <p style={{ 
          marginTop: '15px', 
          fontSize: '13px', 
          fontStyle: 'italic',
          opacity: 0.8,
          display: 'flex',
          alignItems: 'center',
          gap: 6
        }}>
          <Info size={14} />This room does not have an HVAC system 
        </p>
      )}      
    </div>
  );
}

export default SelectedRoomPanel;
