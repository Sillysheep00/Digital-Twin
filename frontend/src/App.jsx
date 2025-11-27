import { useState, useEffect } from 'react'
import axios from 'axios'
import DigitalTwinScene from './DigitalTwinScene';

function App() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [selectedRoomId, setSelectedRoomId] = useState(null)

  const fetchData = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/dashboard')
      console.log("Data received:", response.data)
      setData(response.data)
      setError(null)
    } catch (err) {
      console.error("Error fetching data:", err)
      setError("Failed to connect to Digital Twin. Is the server running?")
    }
  }

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, 2000)
    return () => clearInterval(interval)
  }, [])

  const getSelectedRoomData = () => {
      if (!data || !selectedRoomId) return null;
      return data.rooms.find(r => r.id === selectedRoomId);
  };

  const handleControl = async (action) => {
    if (!selectedRoomId) return;
    try {
      // Call the new Java API
      await axios.post(`http://localhost:8080/api/control?roomId=${selectedRoomId}&action=${action}`);
      console.log(`Sent command: ${action} to ${selectedRoomId}`);
    
      // But since your app polls every 2 seconds, you can just wait for the next fetch.
    } catch (err) {
      alert("Failed to send command");
    }
  };

  const selectedRoom = getSelectedRoomData();

  return (
    <div style={{ width: '100vw', height: '100vh', overflow: 'hidden', position: 'relative', fontFamily: 'Arial' }}>
      
      {/* FULL SCREEN 3D SCENE (with bottom padding to account for overlay) */}
      <div style={{ width: '100%', height: 'calc(100% - 100px)', paddingBottom: '100px' }}>
          <DigitalTwinScene data={data} onRoomSelect={setSelectedRoomId} />
      </div>

      {/* TOP BAR - Timestamp */}
      <div style={{ 
          position: 'absolute', 
          top: '20px', 
          left: '20px', 
          background: 'rgba(255, 255, 255, 0.95)', 
          padding: '15px 25px', 
          borderRadius: '8px',
          boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
      }}>
          <span style={{ fontSize: '24px' }}>🏭</span>
          <div>
              <h2 style={{ margin: 0, fontSize: '18px' }}>Digital Twin Dashboard</h2>
              {data && <span style={{ fontSize: '12px', color: '#666' }}>🕒 {data.timestamp}</span>}
          </div>
      </div>

      {/* TOP RIGHT - SELECTED ROOM PANEL */}
      {selectedRoom && (
          <div style={{ 
              position: 'absolute', 
              top: '20px', 
              right: '20px', 
              background: 'rgba(0, 123, 255, 0.95)', 
              color: 'white',
              padding: '20px', 
              borderRadius: '12px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
              minWidth: '280px'
          }}>
              <h2 style={{ margin: '0 0 15px 0', fontSize: '20px' }}>
                  📍 Selected: {selectedRoom.name}
              </h2>
              <div style={{ fontSize: '14px', lineHeight: '1.8' }}>
                  <p style={{ margin: '5px 0' }}><b>ID:</b> {selectedRoom.id}</p>
                  <p style={{ margin: '5px 0' }}>
                      <b>Temperature:</b> 
                      <span style={{ 
                          fontSize: '20px', 
                          fontWeight: 'bold', 
                          marginLeft: '10px',
                          color: selectedRoom.temp > 24 ? '#ff6b6b' : selectedRoom.temp < 18 ? '#4dabf7' : '#51cf66'
                      }}>
                          {selectedRoom.temp} °C
                      </span>
                  </p>
                  <p style={{ margin: '5px 0' }}>
                      <b>HVAC Status:</b> 
                      <span style={{ 
                          fontWeight: 'bold', 
                          marginLeft: '10px',
                          color: selectedRoom.hvac === "ON" ? '#51cf66' : '#ff6b6b'
                      }}>
                          {selectedRoom.hvac === "ON" ? "🟢 ON" : "🔴 OFF"}
                      </span>
                  </p>
              </div>

              {/* HVAC CONTROL BUTTONS */}
              <div style={{ marginTop: '20px', display: 'flex', gap: '10px' }}>
                  {/* TOGGLE ON/OFF BUTTON */}
                  <button 
                      onClick={() => handleControl(selectedRoom.hvac === "ON" ? "OFF" : "ON")}
                      style={{ 
                          flex: 1,
                          padding: '12px', 
                          borderRadius: '6px', 
                          border: 'none', 
                          cursor: 'pointer',
                          background: selectedRoom.hvac === "ON" ? '#ff6b6b' : '#51cf66', 
                          color: 'white', 
                          fontWeight: 'bold',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                          boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                      }}
                      onMouseEnter={(e) => {
                          e.target.style.transform = 'scale(1.05)';
                          e.target.style.boxShadow = '0 4px 8px rgba(0,0,0,0.3)';
                      }}
                      onMouseLeave={(e) => {
                          e.target.style.transform = 'scale(1)';
                          e.target.style.boxShadow = '0 2px 4px rgba(0,0,0,0.2)';
                      }}
                  >
                      {selectedRoom.hvac === "ON" ? "🔴 Turn OFF" : "🟢 Turn ON"}
                  </button>
                  
                  {/* AUTO MODE BUTTON */}
                  <button 
                      onClick={() => handleControl("AUTO")}
                      style={{ 
                          flex: 1,
                          padding: '12px', 
                          borderRadius: '6px', 
                          border: 'none', 
                          cursor: 'pointer',
                          background: '#339af0', 
                          color: 'white', 
                          fontWeight: 'bold',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                          boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                      }}
                      onMouseEnter={(e) => {
                          e.target.style.transform = 'scale(1.05)';
                          e.target.style.boxShadow = '0 4px 8px rgba(0,0,0,0.3)';
                      }}
                      onMouseLeave={(e) => {
                          e.target.style.transform = 'scale(1)';
                          e.target.style.boxShadow = '0 2px 4px rgba(0,0,0,0.2)';
                      }}
                  >
                      🔄 Auto
                  </button>
              </div>
          </div>
      )}

      {/* BOTTOM BAR - Building Overview + All Rooms (Compact) */}
      {data && (
          <div style={{ 
              position: 'absolute', 
              bottom: '0', 
              left: '0',
              right: '0',
              background: 'rgba(255, 255, 255, 0.95)', 
              padding: '8px 15px', 
              boxShadow: '0 -4px 12px rgba(0,0,0,0.1)',
              display: 'flex',
              gap: '20px',
              overflowX: 'auto',
              alignItems: 'center'
          }}>
              
              {/* Building Overview */}
              <div style={{ minWidth: '180px' }}>
                  <h3 style={{ margin: '0 0 5px 0', fontSize: '14px', color: '#333' }}>🏢 Building</h3>
                  <div style={{ fontSize: '11px', color: '#555', display: 'flex', gap: '10px' }}>
                      <div><b>Power:</b> {data.power.real} kW</div>
                      <div><b>Temp:</b> {data.comfort.avgTemp} °C</div>
                      <div><b>HVACs:</b> {data.comfort.activeHvacs}</div>
                  </div>
              </div>

              {/* Divider */}
              <div style={{ width: '1px', height: '40px', background: '#ddd' }}></div>

              {/* All Rooms List */}
              <div style={{ flex: 1, display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <span style={{ fontSize: '13px', fontWeight: 'bold', color: '#333', marginRight: '5px' }}>🌡️</span>
                  {data.rooms.map((room) => (
                      <div 
                          key={room.id} 
                          onClick={() => setSelectedRoomId(room.id)}
                          style={{ 
                              padding: '6px 12px', 
                              background: selectedRoomId === room.id ? '#007BFF' : '#f8f9fa',
                              color: selectedRoomId === room.id ? 'white' : '#333',
                              cursor: 'pointer',
                              borderRadius: '4px',
                              border: selectedRoomId === room.id ? '2px solid #0056b3' : '1px solid #ddd',
                              transition: 'all 0.2s',
                              fontSize: '11px',
                              whiteSpace: 'nowrap'
                          }}
                          onMouseEnter={(e) => {
                              if (selectedRoomId !== room.id) {
                                  e.target.style.background = '#e9ecef';
                              }
                          }}
                          onMouseLeave={(e) => {
                              if (selectedRoomId !== room.id) {
                                  e.target.style.background = '#f8f9fa';
                              }
                          }}
                      >
                          <span style={{ fontWeight: 'bold' }}>{room.name}</span>
                          <span style={{ marginLeft: '6px' }}>{room.temp}°C</span>
                          <span style={{ color: selectedRoomId === room.id ? 'white' : (room.hvac === "ON" ? 'green' : 'red') }}>
                              {room.hvac === "ON" ? " 🟢" : " 🔴"}
                          </span>
                      </div>
                  ))}
              </div>
          </div>
      )}

      {/* ERROR MESSAGE (if backend down) */}
      {error && (
          <div style={{ 
              position: 'absolute', 
              top: '50%', 
              left: '50%', 
              transform: 'translate(-50%, -50%)',
              background: 'rgba(220, 53, 69, 0.95)', 
              color: 'white',
              padding: '20px', 
              borderRadius: '8px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
          }}>
              ❌ {error}
          </div>
      )}
    </div>
  )
}

export default App