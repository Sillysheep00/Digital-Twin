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
  const [showWhatIfModal, setShowWhatIfModal] = useState(false);
  const [showAnomalyModal, setShowAnomalyModal] = useState(false);
  
  // What-If Analysis States
  const [whatIfResult, setWhatIfResult] = useState(null);
  const [isRunningWhatIf, setIsRunningWhatIf] = useState(false);
  const [whatIfParams, setWhatIfParams] = useState({
    targetTemp: 22,
    insulation: 0.04,
    hours: 24
  });
  
  // Anomaly Detection States
  const [anomalyResult, setAnomalyResult] = useState(null);
  const [isCheckingAnomaly, setIsCheckingAnomaly] = useState(false);

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
  
  const handleWhatIfAnalysis = async () => {
      setIsRunningWhatIf(true);
      setWhatIfResult(null);
      try {
          const changes = {};
          
          // Only include changed parameters
          if (whatIfParams.targetTemp !== 22) {
              changes.targetTemp = parseFloat(whatIfParams.targetTemp);
          }
          if (whatIfParams.insulation !== 0.04) {
              changes.insulation = parseFloat(whatIfParams.insulation);
          }
          
          const response = await axios.post('http://localhost:8080/api/what-if', {
              changes: changes,
              hours: parseInt(whatIfParams.hours)
          });
          
          setWhatIfResult(response.data);
      } catch (err) {
          console.error("What-If analysis failed:", err);
          
          // Show helpful error message
          const errorMsg = err.response?.data?.message || 
                          "What-If analysis failed. Please ensure:\n\n" +
                          "1. Backend is running\n" +
                          "2. Simulation has been running for at least 2 minutes\n" +
                          "3. There is prediction data available\n\n" +
                          "Check the backend console for detailed error messages.";
          alert(errorMsg);
          
          // Set error result for UI display
          setWhatIfResult({
              error: true,
              message: errorMsg
          });
      } finally {
          setIsRunningWhatIf(false);
      }
  };
  
  const handleAnomalyCheck = async () => {
      setIsCheckingAnomaly(true);
      setAnomalyResult(null);
      try {
          const response = await axios.get('http://localhost:8080/api/anomaly');
          setAnomalyResult(response.data);
      } catch (err) {
          console.error("Anomaly detection failed:", err);
          alert("Failed to check for anomalies. Ensure backend is running.");
          setAnomalyResult({
              error: true,
              message: "Failed to connect to anomaly detection service"
          });
      } finally {
          setIsCheckingAnomaly(false);
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
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
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
                onClick={() => setShowWhatIfModal(true)}
                style={{ padding: '10px', borderRadius: '5px', border: 'none', cursor: 'pointer', background: '#00b894', color: 'white', boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontWeight: 'bold' }}>
                🔬 What-If Analysis
            </button>
            <button 
                onClick={() => {
                    setShowAnomalyModal(true);
                    handleAnomalyCheck();
                }}
                style={{ padding: '10px', borderRadius: '5px', border: 'none', cursor: 'pointer', background: '#e74c3c', color: 'white', boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontWeight: 'bold' }}>
                🚨 Anomaly Detection
            </button>
          </div>
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

      {/* MODAL: WHAT-IF ANALYSIS */}
      {showWhatIfModal && (
        <div style={modalOverlayStyle}>
            <div style={modalContentStyle}>
                <button style={closeButtonStyle} onClick={() => setShowWhatIfModal(false)}>×</button>
                <h2>🔬 What-If Analysis</h2>
                <p style={{ color: '#666', marginBottom: '20px' }}>
                    Test different scenarios to optimize energy usage and costs. Adjust parameters below and see the impact!
                </p>
                
                {/* Input Controls */}
                <div style={{ 
                    background: '#f8f9fa', 
                    padding: '20px', 
                    borderRadius: '8px', 
                    marginBottom: '20px' 
                }}>
                    <h3 style={{ marginTop: 0 }}>Scenario Parameters</h3>
                    
                    {/* Target Temperature */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
                            Target Temperature: {whatIfParams.targetTemp}°C
                        </label>
                        <input 
                            type="range" 
                            min="18" 
                            max="25" 
                            step="0.5"
                            value={whatIfParams.targetTemp}
                            onChange={(e) => setWhatIfParams({...whatIfParams, targetTemp: e.target.value})}
                            style={{ width: '100%' }}
                        />
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666' }}>
                            <span>18°C (Cold)</span>
                            <span>22°C (Default)</span>
                            <span>25°C (Warm)</span>
                        </div>
                    </div>
                    
                    {/* Insulation */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
                            Insulation Quality: {whatIfParams.insulation} (Lower = Better)
                        </label>
                        <input 
                            type="range" 
                            min="0.01" 
                            max="0.08" 
                            step="0.005"
                            value={whatIfParams.insulation}
                            onChange={(e) => setWhatIfParams({...whatIfParams, insulation: e.target.value})}
                            style={{ width: '100%' }}
                        />
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666' }}>
                            <span>0.01 (Excellent)</span>
                            <span>0.04 (Current)</span>
                            <span>0.08 (Poor)</span>
                        </div>
                    </div>
                    
                    {/* Prediction Horizon */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
                            Prediction Horizon: {whatIfParams.hours} hours
                        </label>
                        <select 
                            value={whatIfParams.hours}
                            onChange={(e) => setWhatIfParams({...whatIfParams, hours: e.target.value})}
                            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                        >
                            <option value="12">12 hours</option>
                            <option value="24">24 hours (1 day)</option>
                            <option value="48">48 hours (2 days)</option>
                            <option value="72">72 hours (3 days)</option>
                        </select>
                    </div>
                    
                    {/* Run Button */}
                    <button 
                        onClick={handleWhatIfAnalysis}
                        disabled={isRunningWhatIf}
                        style={{ 
                            width: '100%', 
                            padding: '12px', 
                            background: '#00b894', 
                            color: 'white', 
                            border: 'none', 
                            borderRadius: '5px', 
                            fontWeight: 'bold', 
                            cursor: isRunningWhatIf ? 'not-allowed' : 'pointer',
                            opacity: isRunningWhatIf ? 0.6 : 1
                        }}
                    >
                        {isRunningWhatIf ? '⏳ Running Analysis...' : '▶️ Run What-If Analysis'}
                    </button>
                </div>
                
                {/* Results */}
                {whatIfResult && !whatIfResult.error && (
                    <div style={{ 
                        border: '2px solid #00b894', 
                        borderRadius: '8px', 
                        padding: '20px',
                        background: '#e8f8f5'
                    }}>
                        <h3 style={{ marginTop: 0, color: '#00b894' }}>📊 Analysis Results</h3>
                        
                        {/* Comparison Table */}
                        <table style={{ width: '100%', marginBottom: '20px', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ borderBottom: '2px solid #00b894' }}>
                                    <th style={{ textAlign: 'left', padding: '10px' }}>Metric</th>
                                    <th style={{ textAlign: 'center', padding: '10px' }}>Baseline</th>
                                    <th style={{ textAlign: 'center', padding: '10px' }}>Scenario</th>
                                    <th style={{ textAlign: 'center', padding: '10px' }}>Difference</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                    <td style={{ padding: '10px' }}><strong>Energy Usage</strong></td>
                                    <td style={{ textAlign: 'center', padding: '10px' }}>
                                        {whatIfResult.baseline.predictedEnergy?.toFixed(2)} kWh
                                    </td>
                                    <td style={{ textAlign: 'center', padding: '10px' }}>
                                        {whatIfResult.scenario.predictedEnergy?.toFixed(2)} kWh
                                    </td>
                                    <td style={{ 
                                        textAlign: 'center', 
                                        padding: '10px',
                                        fontWeight: 'bold',
                                        color: whatIfResult.energySaved > 0 ? '#00b894' : '#d63031'
                                    }}>
                                        {whatIfResult.energySaved > 0 ? '▼' : '▲'} {Math.abs(whatIfResult.energySaved).toFixed(2)} kWh
                                        <br/>
                                        <span style={{ fontSize: '12px' }}>
                                            ({whatIfResult.energySaved > 0 ? '-' : '+'}{Math.abs(whatIfResult.percentSaved).toFixed(1)}%)
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        
                        {/* Savings Summary */}
                        <div style={{ 
                            display: 'grid', 
                            gridTemplateColumns: '1fr 1fr', 
                            gap: '15px',
                            marginTop: '15px'
                        }}>
                            <div style={{ 
                                background: 'white', 
                                padding: '15px', 
                                borderRadius: '8px',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }}>
                                <div style={{ fontSize: '12px', color: '#666' }}>Cost Saved (Period)</div>
                                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#00b894' }}>
                                    ${whatIfResult.costSaved?.toFixed(2)}
                                </div>
                            </div>
                            <div style={{ 
                                background: 'white', 
                                padding: '15px', 
                                borderRadius: '8px',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }}>
                                <div style={{ fontSize: '12px', color: '#666' }}>Annual Savings</div>
                                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#00b894' }}>
                                    ${whatIfResult.annualCostSaved?.toFixed(2)}
                                </div>
                            </div>
                        </div>
                        
                        {/* Recommendation */}
                        {whatIfResult.energySaved > 0 ? (
                            <div style={{ 
                                marginTop: '20px', 
                                padding: '15px', 
                                background: '#d4edda', 
                                border: '1px solid #c3e6cb',
                                borderRadius: '5px',
                                color: '#155724'
                            }}>
                                <strong>✅ Recommendation:</strong> This scenario would save energy and reduce costs. 
                                Consider implementing these changes!
                            </div>
                        ) : (
                            <div style={{ 
                                marginTop: '20px', 
                                padding: '15px', 
                                background: '#f8d7da', 
                                border: '1px solid #f5c6cb',
                                borderRadius: '5px',
                                color: '#721c24'
                            }}>
                                <strong>⚠️ Note:</strong> This scenario would increase energy consumption. 
                                Current settings are more efficient.
                            </div>
                        )}
                    </div>
                )}
                
                {whatIfResult && whatIfResult.error && (
                    <div style={{ 
                        padding: '15px', 
                        background: '#f8d7da', 
                        border: '1px solid #f5c6cb',
                        borderRadius: '5px',
                        color: '#721c24'
                    }}>
                        <strong>⚠️ Analysis Failed</strong>
                        <p style={{ margin: '10px 0 0 0', fontSize: '14px' }}>
                            {whatIfResult.message || 'Please check backend logs for details.'}
                        </p>
                        <div style={{ marginTop: '10px', fontSize: '13px', background: '#fff', padding: '10px', borderRadius: '4px' }}>
                            <strong>Common Solutions:</strong>
                            <ul style={{ margin: '5px 0', paddingLeft: '20px' }}>
                                <li>Wait 2 minutes after starting backend</li>
                                <li>Try clicking "🔮 Predict Next 24H" first</li>
                                <li>Check backend console for errors</li>
                                <li>Ensure MongoDB is running</li>
                            </ul>
                        </div>
                    </div>
                )}
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

      {/* MODAL: ANOMALY DETECTION */}
      {showAnomalyModal && (
        <div style={modalOverlayStyle}>
            <div style={modalContentStyle}>
                <button style={closeButtonStyle} onClick={() => setShowAnomalyModal(false)}>×</button>
                <h2>🚨 ML-Based Anomaly Detection</h2>
                <p style={{ color: '#666', marginBottom: '20px' }}>
                    Using Linear Regression to detect unusual energy consumption patterns
                </p>
                
                {/* Refresh Button */}
                <button 
                    onClick={handleAnomalyCheck}
                    disabled={isCheckingAnomaly}
                    style={{ 
                        width: '100%', 
                        padding: '12px', 
                        background: '#3498db', 
                        color: 'white', 
                        border: 'none', 
                        borderRadius: '5px', 
                        fontWeight: 'bold', 
                        cursor: isCheckingAnomaly ? 'not-allowed' : 'pointer',
                        opacity: isCheckingAnomaly ? 0.6 : 1,
                        marginBottom: '20px'
                    }}
                >
                    {isCheckingAnomaly ? '⏳ Checking...' : '🔄 Check for Anomalies'}
                </button>
                
                {/* Results */}
                {anomalyResult && !anomalyResult.error && (
                    <div style={{ 
                        border: `2px solid ${anomalyResult.anomalyDetected ? '#e74c3c' : '#27ae60'}`, 
                        borderRadius: '8px', 
                        padding: '20px',
                        background: anomalyResult.anomalyDetected ? '#fadbd8' : '#d4efdf'
                    }}>
                        <h3 style={{ 
                            marginTop: 0, 
                            color: anomalyResult.anomalyDetected ? '#e74c3c' : '#27ae60',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '10px'
                        }}>
                            {anomalyResult.anomalyDetected ? '⚠️ ANOMALY DETECTED!' : '✅ System Normal'}
                            <span style={{
                                fontSize: '14px',
                                padding: '4px 12px',
                                borderRadius: '12px',
                                background: anomalyResult.severity === 'CRITICAL' ? '#c0392b' :
                                           anomalyResult.severity === 'WARNING' ? '#f39c12' : '#27ae60',
                                color: 'white'
                            }}>
                                {anomalyResult.severity}
                            </span>
                        </h3>
                        
                        {/* Power Comparison */}
                        <div style={{ 
                            display: 'grid', 
                            gridTemplateColumns: '1fr 1fr 1fr', 
                            gap: '15px',
                            marginBottom: '20px'
                        }}>
                            <div style={{ 
                                background: 'white', 
                                padding: '15px', 
                                borderRadius: '8px',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
                                    Real Power
                                </div>
                                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#2c3e50' }}>
                                    {anomalyResult.realPower?.toFixed(2)} kW
                                </div>
                            </div>
                            
                            <div style={{ 
                                background: 'white', 
                                padding: '15px', 
                                borderRadius: '8px',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
                                    ML Predicted
                                </div>
                                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#3498db' }}>
                                    {anomalyResult.calibratedSimulatedPower?.toFixed(2)} kW
                                </div>
                            </div>
                            
                            <div style={{ 
                                background: 'white', 
                                padding: '15px', 
                                borderRadius: '8px',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
                                    Residual
                                </div>
                                <div style={{ 
                                    fontSize: '20px', 
                                    fontWeight: 'bold', 
                                    color: anomalyResult.anomalyDetected ? '#e74c3c' : '#27ae60'
                                }}>
                                    {anomalyResult.residual?.toFixed(2)} kW
                                </div>
                                <div style={{ fontSize: '11px', color: '#666' }}>
                                    (Threshold: {anomalyResult.threshold?.toFixed(2)} kW)
                                </div>
                            </div>
                        </div>
                        
                        {/* Explanation */}
                        {anomalyResult.explanation && (
                            <div style={{ 
                                padding: '15px', 
                                background: 'white',
                                borderRadius: '5px',
                                fontSize: '14px',
                                lineHeight: '1.6'
                            }}>
                                <strong>Analysis:</strong>
                                <p style={{ margin: '10px 0 0 0' }}>
                                    {anomalyResult.explanation}
                                </p>
                            </div>
                        )}
                        
                        {/* Technical Details */}
                        <details style={{ marginTop: '15px' }}>
                            <summary style={{ cursor: 'pointer', fontWeight: 'bold', padding: '10px' }}>
                                📊 Technical Details
                            </summary>
                            <div style={{ 
                                padding: '15px', 
                                background: 'white',
                                borderRadius: '5px',
                                marginTop: '10px',
                                fontSize: '13px'
                            }}>
                                <p><strong>Raw Simulated Power:</strong> {anomalyResult.simulatedPower?.toFixed(2)} kW</p>
                                <p><strong>ML-Calibrated Power:</strong> {anomalyResult.calibratedSimulatedPower?.toFixed(2)} kW</p>
                                <p><strong>Detection Method:</strong> Residual-based with Linear Regression</p>
                                <p><strong>Threshold:</strong> 25% of real power (min 5 kW)</p>
                            </div>
                        </details>
                    </div>
                )}
                
                {anomalyResult && anomalyResult.error && (
                    <div style={{ 
                        padding: '15px', 
                        background: '#f8d7da', 
                        border: '1px solid #f5c6cb',
                        borderRadius: '5px',
                        color: '#721c24'
                    }}>
                        <strong>⚠️ Detection Failed</strong>
                        <p style={{ margin: '10px 0 0 0', fontSize: '14px' }}>
                            {anomalyResult.message || 'Failed to perform anomaly detection'}
                        </p>
                    </div>
                )}
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
