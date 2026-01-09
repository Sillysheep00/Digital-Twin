import { useState, useEffect } from 'react';
import axios from 'axios';
import DigitalTwinScene from './DigitalTwinScene';

import TemperatureModal from './components/modals/TempModal';
import EnergyModal from './components/modals/EnergyModal';
import WhatIfModal from './components/modals/WhatIfModal';
import AnomalyModal from './components/modals/AnomalyModal';
import ChartModal from './components/modals/ChartModal';
import SelectedRoomPanel from './components/layout/SelectedRoomPanel';
import StatusBar from './components/layout/StatusBar';

function App() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  // Track active manual control mode per room
  const [roomControlModes, setRoomControlModes] = useState({});

  // Modal states
  const [showTempModal, setShowTempModal] = useState(false);
  const [showEnergyModal, setShowEnergyModal] = useState(false);
  const [showWhatIfModal, setShowWhatIfModal] = useState(false);
  const [showAnomalyModal, setShowAnomalyModal] = useState(false);
  const [showChartModal, setShowChartModal] = useState(false);

  // What-If Analysis states
  const [whatIfParams, setWhatIfParams] = useState({
    targetTemp: 22,
    insulation: 0.04,
    baseLoad: 0.5,  
    hours: 24,
    investmentCost: null
  });
  const [whatIfResult, setWhatIfResult] = useState(null);
  const [isRunningWhatIf, setIsRunningWhatIf] = useState(false);

  const [baseLoadMode, setBaseLoadMode] = useState('all'); // 'all' or 'perRoom'
  const [roomBaseLoads, setRoomBaseLoads] = useState({}); // { roomName: value }

  // Anomaly Detection states
  const [anomalyResult, setAnomalyResult] = useState(null);
  const [isCheckingAnomaly, setIsCheckingAnomaly] = useState(false);
  const [anomalyWindowSize, setAnomalyWindowSize] = useState(32); // Default: 8 hours

  // Fetch Digital Twin data
  const fetchData = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/dashboard');
      setData(response.data);
      setError(null);
    } catch (err) {
      console.error("Error fetching data:", err);
      setError("Failed to connect to Digital Twin. Is the server running?");
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  
  const selectedRoom = data?.rooms?.find(r => r.id === selectedRoomId) || null;

  const handleControl = async (action) => {
    if (!selectedRoomId) return;
    try {
      await axios.post(`http://localhost:8080/api/control?roomId=${selectedRoomId}&action=${action}`);
      console.log(`Sent command: ${action} to ${selectedRoomId}`);
        // Update local state to track active mode
        setRoomControlModes(prev => ({
          ...prev,
          [selectedRoomId]: action
        }));
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
      if (whatIfParams.targetTemp !== 22) changes.targetTemp = parseFloat(whatIfParams.targetTemp);
      if (whatIfParams.insulation !== 0.04) changes.insulation = parseFloat(whatIfParams.insulation);
      
      // Base Load Logic - Handle both modes
      if (baseLoadMode === 'all') {
        // Use global baseLoad if different from default
        if (whatIfParams.baseLoad !== 1.0) {
          changes.baseLoad = parseFloat(whatIfParams.baseLoad);
        }
      } else {
        // Per-room mode: check if any room has a different value
        const roomBaseLoadChanges = {};
        const defaultBaseLoad = whatIfParams.baseLoad ?? 1.0;
        let hasChanges = false;
        
        data?.rooms?.forEach(room => {
          const roomValue = roomBaseLoads[room.name];
          // Only include if explicitly set and different from default
          if (roomValue !== undefined && roomValue !== defaultBaseLoad) {
            roomBaseLoadChanges[room.name] = roomValue;
            hasChanges = true;
          }
        });
        
        if (hasChanges) {
          changes.roomBaseLoad = roomBaseLoadChanges;
        } else if (whatIfParams.baseLoad !== 1.0) {
          // If no per-room changes but global is different, use global
          changes.baseLoad = parseFloat(whatIfParams.baseLoad);
        }
      }

      const response = await axios.post('http://localhost:8080/api/what-if', {
        changes,
        hours: parseInt(whatIfParams.hours),
        investmentCost: whatIfParams.investmentCost || null  // Add investment cost
      });
      setWhatIfResult(response.data);
    } catch (err) {
      console.error("What-If analysis failed:", err);
      const errorMsg = err.response?.data?.message ||
        "What-If analysis failed. Ensure backend is running and prediction data is available.";
      setWhatIfResult({ error: true, message: errorMsg });
      alert(errorMsg);
    } finally {
      setIsRunningWhatIf(false);
    }
  };

  const handleAnomalyCheck = async () => {
    setIsCheckingAnomaly(true);
    setAnomalyResult(null);
    try {
      const response = await axios.get(`http://localhost:8080/api/anomaly?windowSize=${anomalyWindowSize}`);
      setAnomalyResult(response.data);
    } catch (err) {
      console.error("Anomaly detection failed:", err);
      setAnomalyResult({ error: true, message: "Failed to connect to anomaly detection service" });
      alert("Failed to check for anomalies. Ensure backend is running.");
    } finally {
      setIsCheckingAnomaly(false);
    }
  };

  return (
    <div style={{ width: '100vw', height: '100vh', overflow: 'hidden', position: 'relative', fontFamily: 'Arial' }}>
      
      {/* FULL SCREEN 3D SCENE */}
      <div style={{ width: '100%', height: 'calc(100% - 100px)', paddingBottom: '100px' }}>
        <DigitalTwinScene data={data} onRoomSelect={setSelectedRoomId} />
      </div>

      {/* TOP LEFT - DASHBOARD & BUTTONS */}
      <div style={{ position: 'absolute', top: 20, left: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{
          background: 'rgba(255, 255, 255, 0.95)',
          padding: '15px 25px',
          borderRadius: 8,
          boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
          display: 'flex', alignItems: 'center', gap: 10
        }}>
          <span style={{ fontSize: 24 }}>🏭</span>
          <div>
            <h2 style={{ margin: 0, fontSize: 18 }}>Digital Twin Dashboard</h2>
            {data && <span style={{ fontSize: 12, color: '#666' }}>🕒 {data.timestamp}</span>}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button onClick={() => setShowTempModal(true)} style={buttonStyle('#fff')}>🌡️ View All Temps</button>
          <button onClick={() => setShowEnergyModal(true)} style={buttonStyle('#fff')}>⚡ View Energy</button>
          <button onClick={() => setShowWhatIfModal(true)} style={buttonStyle('#00b894', 'white')}>🔬 What-If Analysis</button>
          <button onClick={() => { setShowAnomalyModal(true); handleAnomalyCheck(); }} style={buttonStyle('#e74c3c', 'white')}>🚨 Anomaly Detection</button>
        </div>
      </div>

      {/* TOP RIGHT - SELECTED ROOM PANEL */}
      <SelectedRoomPanel selectedRoom={selectedRoom} 
      handleControl={handleControl} 
      activeMode={selectedRoomId ? roomControlModes[selectedRoomId] : null}
      />

      {/* MODALS */}
      <TemperatureModal show={showTempModal} data={data} onClose={() => setShowTempModal(false)} />
      <EnergyModal show={showEnergyModal} data={data} onClose={() => setShowEnergyModal(false)} />
      <WhatIfModal
        showWhatIfModal={showWhatIfModal}
        setShowWhatIfModal={setShowWhatIfModal}
        whatIfParams={whatIfParams}
        setWhatIfParams={setWhatIfParams}
        handleWhatIfAnalysis={handleWhatIfAnalysis}
        isRunningWhatIf={isRunningWhatIf}
        whatIfResult={whatIfResult}
        setShowChartModal={setShowChartModal}
        data={data}  
        baseLoadMode={baseLoadMode} 
        setBaseLoadMode={setBaseLoadMode}  
        roomBaseLoads={roomBaseLoads}  
        setRoomBaseLoads={setRoomBaseLoads}  
      />
      <AnomalyModal
        showAnomaly={showAnomalyModal}
        setShowAnomaly={setShowAnomalyModal}
        anomalyResult={anomalyResult}
        handleAnomalyCheck={handleAnomalyCheck}
        isCheckingAnomaly={isCheckingAnomaly}
        windowSize={anomalyWindowSize}
        setWindowSize={setAnomalyWindowSize}
      />
      <ChartModal show={showChartModal} whatIfResult={whatIfResult} whatIfParams={whatIfParams} onClose={() => setShowChartModal(false)} />

      {/* BOTTOM BAR */}
      <StatusBar data={data} />

      {/* ERROR DISPLAY */}
      {error && (
        <div style={{
          position: 'absolute', top: '50%', left: '50%',
          transform: 'translate(-50%, -50%)',
          background: 'red', color: 'white', padding: 20, borderRadius: 8
        }}>
          {error}
        </div>
      )}
    </div>
  );
}

// reusable button style
const buttonStyle = (bgColor, color = 'black') => ({
  padding: 10,
  borderRadius: 5,
  border: 'none',
  cursor: 'pointer',
  background: bgColor,
  color: color,
  boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
  fontWeight: 'bold'
});

export default App;
