import { useState, useEffect } from 'react'
import axios from 'axios'
import DigitalTwinScene from './DigitalTwinScene';

function App() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [selectedRoomId, setSelectedRoomId] = useState(null)
  
  // Modal States
  const [showTempModal, setShowTempModal] = useState(false);
  const [showEnergyModal, setShowEnergyModal] = useState(false);
  
  // Prediction States
  const [prediction, setPrediction] = useState(null);
  const [isPredicting, setIsPredicting] = useState(false);

  const fetchData = async () => {
    try {
      // NOTE: Based on DigitalTwinController.java line 10 (@RequestMapping("/api")) and line 34 (@GetMapping("/dashboard"))
      // The correct full URL is /api/dashboard, NOT /api/digitaltwin/dashboard
      const response = await axios.get('http://localhost:8080/api/dashboard')
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
      // API call to set override
      // Based on DigitalTwinController.java line 50 (@PostMapping("/control"))
      await axios.post(`http://localhost:8080/api/control?roomId=${selectedRoomId}&action=${action}`);
      console.log(`Sent command: ${action} to ${selectedRoomId}`);
    } catch (err) {
      console.error(err);
      alert("Failed to send command");
    }
  };
  
  const handlePrediction = async () => {
      setIsPredicting(true);
      setPrediction(null);
      try {
          const response = await axios.get('http://localhost:8080/api/predict?hours=24');
          setPrediction(response.data);
      } catch (err) {
          alert("Prediction failed");
      } finally {
          setIsPredicting(false);
      }
  };

  const selectedRoom = getSelectedRoomData();

  // Styles for Modal Overlay
  const modalOverlayStyle = {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000
  };

  const modalContentStyle = {
    backgroundColor: 'white',
    padding: '20px',
    borderRadius: '8px',
    maxWidth: '800px',
    width: '90%',
    maxHeight: '80vh',
    overflowY: 'auto',
    boxShadow: '0 4px 12px rgba(0,0,0,0.2)'
  };

  const closeButtonStyle = {
    float: 'right',
    cursor: 'pointer',
    border: 'none',
    background: 'none',
    fontSize: '20px',
    fontWeight: 'bold'
  };

  return (
    <div style={{ width: '100vw', height: '100vh', overflow: 'hidden', position: 'relative', fontFamily: 'Arial' }}>
      
      {/* FULL SCREEN 3D SCENE */}
      <div style={{ width: '100%', height: 'calc(100% - 100px)', paddingBottom: '100px' }}>
          <DigitalTwinScene data={data} onRoomSelect={setSelectedRoomId} />
      </div>

      {/* TOP LEFT - DASHBOARD TITLE & BUTTONS */}
      <div style={{ position: 'absolute', top: '20px', left: '20px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          
          {/* Title Box */}
          <div style={{ 
              background: 'rgba(255, 255, 255, 0.95)', 
              padding: '15px 25px', 
              borderRadius: '8px',
              boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
              display: 'flex', alignItems: 'center', gap: '10px'
          }}>
              <span style={{ fontSize: '24px' }}>🏭</span>
              <div>
                  <h2 style={{ margin: 0, fontSize: '18px' }}>Digital Twin Dashboard</h2>
                  {data && <span style={{ fontSize: '12px', color: '#666' }}>🕒 {data.timestamp}</span>}
              </div>
          </div>

          {/* New Feature Buttons */}
          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
                onClick={() => setShowTempModal(true)}
                style={{ padding: '10px', borderRadius: '5px', border: 'none', cursor: 'pointer', background: '#fff', boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontWeight: 'bold' }}>
                🌡️ View All Temps
            </button>
            <button 
                onClick={() => setShowEnergyModal(true)}
                style={{ padding: '10px', borderRadius: '5px', border: 'none', cursor: 'pointer', background: '#fff', boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontWeight: 'bold' }}>
                ⚡ View Energy
            </button>
            <button 
                onClick={handlePrediction}
                disabled={isPredicting}
                style={{ padding: '10px', borderRadius: '5px', border: 'none', cursor: 'pointer', background: '#6c5ce7', color: 'white', boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontWeight: 'bold' }}>
                {isPredicting ? "Running..." : "🔮 Predict Next 24H"}
            </button>
          </div>
          
           {/* Prediction Result Box */}
           {prediction && (
              <div style={{ 
                  background: 'rgba(255, 255, 255, 0.95)', 
                  padding: '15px', 
                  borderRadius: '8px',
                  boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                  marginTop: '5px',
                  borderLeft: '5px solid #6c5ce7'
              }}>
                  <h4 style={{ margin: '0 0 5px 0', color: '#6c5ce7' }}>Prediction Result</h4>
                  {prediction.error ? (
                       <div style={{ color: 'red', fontSize: '13px' }}>
                           ⚠️ Prediction Failed. <br/> (Check backend logs)
                       </div>
                  ) : (
                      <div style={{ fontSize: '14px' }}>
                          Next <b>{prediction.hours} hours</b> energy usage:
                          <br/>
                          <span style={{ fontSize: '20px', fontWeight: 'bold' }}>
                              {prediction.predictedEnergy ? prediction.predictedEnergy.toFixed(2) : "0.00"} kWh
                          </span>
                      </div>
                  )}
              </div>
          )}
      </div>

      {/* TOP RIGHT - SELECTED ROOM PANEL */}
      {selectedRoom && (
          <div style={{ 
              position: 'absolute', top: '20px', right: '20px', 
              background: 'rgba(0, 123, 255, 0.95)', color: 'white',
              padding: '20px', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
              minWidth: '300px'
          }}>
              <h2 style={{ margin: '0 0 15px 0', fontSize: '20px' }}>📍 {selectedRoom.name}</h2>
              <div style={{ fontSize: '14px', lineHeight: '1.8' }}>
                  <p style={{ margin: '5px 0' }}><b>Temp:</b> {selectedRoom.temp} °C</p>
                  <p style={{ margin: '5px 0' }}><b>Status:</b> {selectedRoom.hvac === "ON" ? "Running" : "Off"}</p>
                  <p style={{ margin: '5px 0' }}><b>Power:</b> {selectedRoom.power} kW</p>
              </div>

              {/* CONTROLS */}
              <h4 style={{ margin: '15px 0 5px 0', borderBottom: '1px solid rgba(255,255,255,0.3)' }}>Manual Controls</h4>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                  <button onClick={() => handleControl("COOL")} style={{ padding: '8px', cursor: 'pointer', borderRadius: '4px', border: 'none', background: '#00d2d3', fontWeight: 'bold', color: '#000' }}>❄️ COOL</button>
                  <button onClick={() => handleControl("HEAT")} style={{ padding: '8px', cursor: 'pointer', borderRadius: '4px', border: 'none', background: '#ff9f43', fontWeight: 'bold', color: '#000' }}>🔥 HEAT</button>
                  <button onClick={() => handleControl("OFF")} style={{ padding: '8px', cursor: 'pointer', borderRadius: '4px', border: 'none', background: '#576574', fontWeight: 'bold', color: '#fff' }}>⭕ OFF</button>
                  <button onClick={() => handleControl("AUTO")} style={{ padding: '8px', cursor: 'pointer', borderRadius: '4px', border: 'none', background: '#2e86de', fontWeight: 'bold', color: '#fff' }}>🔄 AUTO</button>
              </div>
          </div>
      )}

      {/* MODAL: ALL TEMPERATURES */}
      {showTempModal && data && (
        <div style={modalOverlayStyle}>
            <div style={modalContentStyle}>
                <button style={closeButtonStyle} onClick={() => setShowTempModal(false)}>×</button>
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
                                <td style={{ padding: '10px', fontWeight: 'bold', color: r.temp > 24 ? 'red' : r.temp < 19 ? 'blue' : 'green' }}>{r.temp} °C</td>
                                <td style={{ padding: '10px' }}>{r.hvac === "ON" ? "🟢 On" : "⚪ Off"}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
      )}

      {/* MODAL: ENERGY USAGE */}
      {showEnergyModal && data && (
        <div style={modalOverlayStyle}>
            <div style={modalContentStyle}>
                <button style={closeButtonStyle} onClick={() => setShowEnergyModal(false)}>×</button>
                <h2>Energy Consumption Report</h2>
                <div style={{ marginBottom: '15px', padding: '10px', background: '#f8f9fa', borderRadius: '5px' }}>
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
                                <td style={{ padding: '10px', fontWeight: 'bold' }}>{r.energy} kWh</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
      )}

      {/* BOTTOM BAR */}
      {data && (
          <div style={{ 
              position: 'absolute', bottom: '0', left: '0', right: '0',
              background: 'rgba(255, 255, 255, 0.95)', padding: '10px 20px',
              boxShadow: '0 -4px 12px rgba(0,0,0,0.1)', display: 'flex', gap: '30px'
          }}>
              <div><b>Avg Temp:</b> {data.comfort.avgTemp} °C</div>
              <div><b>Active HVACs:</b> {data.comfort.activeHvacs}</div>
              <div><b>Total Power:</b> {data.power.simulated} kW</div>
              <div style={{ marginLeft: 'auto', color: '#666' }}>
                  Select a room in 3D to control it
              </div>
          </div>
      )}

      {/* ERROR */}
      {error && (
          <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', background: 'red', color: 'white', padding: '20px', borderRadius: '8px' }}>
              {error}
          </div>
      )}
    </div>
  )
}

export default App
