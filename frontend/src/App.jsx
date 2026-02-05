import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import DigitalTwinScene from './DigitalTwinScene';
import { Factory, Clock, Thermometer, Zap, FlaskConical, TrendingUp, AlertTriangle, Loader2 } from 'lucide-react';

import TemperatureModal from './components/modals/TempModal';
import EnergyModal from './components/modals/EnergyModal';
import WhatIfModal from './components/modals/WhatIfModal';
import AnomalyModal from './components/modals/AnomalyModal';
import ChartModal from './components/modals/ChartModal';
import SelectedRoomPanel from './components/layout/SelectedRoomPanel';
import StatusBar from './components/layout/StatusBar';
import PowerTrendModal from './components/modals/PowerTrendModal';
import Tooltip from './components/ui/Tooltip';

function App() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [retryCount, setRetryCount] = useState(0);
  const maxRetries = 10;
  const retryCountRef = useRef(0);
  const isLoadingRef = useRef(true);
  const isRetryingRef = useRef(false);
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  // Track active manual control mode per room
  const [roomControlModes, setRoomControlModes] = useState({});

  // Modal states
  const [showTempModal, setShowTempModal] = useState(false);
  const [showEnergyModal, setShowEnergyModal] = useState(false);
  const [showWhatIfModal, setShowWhatIfModal] = useState(false);
  const [showAnomalyModal, setShowAnomalyModal] = useState(false);
  const [showPowerTrendModal, setShowPowerTrendModal] = useState(false);
  const [showChartModal, setShowChartModal] = useState(false);

  // What-If Analysis states
  const [whatIfParams, setWhatIfParams] = useState({
    targetTemp: 22,
    insulation: 0.03,
    baseLoad: 1.0,  
    hours: 24,
    investmentCost: null
  });
  const [whatIfResult, setWhatIfResult] = useState(null);
  const [isRunningWhatIf, setIsRunningWhatIf] = useState(false);

  const [baseLoadMode, setBaseLoadMode] = useState('all'); // 'all' or 'perRoom'
  const [roomBaseLoads, setRoomBaseLoads] = useState({}); // { roomName: value }

  //Power Trend
  const [powerTrendData, setPowerTrendData] = useState(null);
  const [isLoadingPowerTrend, setIsLoadingPowerTrend] = useState(false);
  // Anomaly Detection states
  const [anomalyResult, setAnomalyResult] = useState(null);
  const [isCheckingAnomaly, setIsCheckingAnomaly] = useState(false);
  const [anomalyWindowSize, setAnomalyWindowSize] = useState(32); // Default: 8 hours
  
  // Live Weather state (for display only)
  const [liveWeather, setLiveWeather] = useState(null);
  const [isLoadingWeather, setIsLoadingWeather] = useState(true);

  // Fetch Digital Twin data
  const fetchData = async () => {
    // Prevent multiple simultaneous retries
    if (isRetryingRef.current) {
      return;
    }
    
    try {
      const response = await axios.get('http://localhost:8080/api/dashboard');
      setData(response.data);
      setError(null);
      setIsLoading(false);
      isLoadingRef.current = false;
      isRetryingRef.current = false;
      setRetryCount(0);
      retryCountRef.current = 0;
    } catch (err) {
      console.error("Error fetching data:", err);
      
      // If still in initial loading and haven't exceeded max retries, keep retrying
      if (isLoadingRef.current && retryCountRef.current < maxRetries && !isRetryingRef.current) {
        isRetryingRef.current = true;
        retryCountRef.current += 1;
        setRetryCount(retryCountRef.current);
        console.log(`Retrying connection... (${retryCountRef.current}/${maxRetries})`);
        setTimeout(() => {
          isRetryingRef.current = false;
          fetchData();
        }, 2000); // Retry after 2 seconds
      } else if (retryCountRef.current >= maxRetries) {
        // Exceeded retries, show error
        setError("Failed to connect to Digital Twin. Backend server is not responding.");
        setIsLoading(false);
        isLoadingRef.current = false;
        isRetryingRef.current = false;
      }
    }
  };
  
  // Fetch Live Weather (for display only)
  const fetchLiveWeather = async () => {
    setIsLoadingWeather(true);
    try {
      const response = await axios.get('http://localhost:8080/api/weather/live');
      setLiveWeather(response.data);
    } catch (err) {
      console.error("Error fetching live weather:", err);
      // Silently fail - weather is optional
    } finally {
      setIsLoadingWeather(false);
    }
  };

  useEffect(() => {
    fetchData();
    fetchLiveWeather(); // Fetch once on mount
  }, []);
  
  // Start polling only after initial load is complete
  useEffect(() => {
    if (!isLoading && data) {
      const interval = setInterval(fetchData, 5000);
      const weatherInterval = setInterval(fetchLiveWeather, 300000); // Refresh every 5 minutes
      return () => {
        clearInterval(interval);
        clearInterval(weatherInterval);
      };
    }
  }, [isLoading, data]);

  // Reset What-If parameters when modal closes
  useEffect(() => {
    if (!showWhatIfModal) {
      // Reset all parameters to default values when modal is closed
      setWhatIfParams({
        targetTemp: 22,
        insulation: 0.03,
        baseLoad: 1.0,
        hours: 24,
        investmentCost: null
      });
      setWhatIfResult(null);
      setBaseLoadMode('all');
      setRoomBaseLoads({});
    }
  }, [showWhatIfModal]);

  
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
      if (whatIfParams.insulation !== 0.03) changes.insulation = parseFloat(whatIfParams.insulation);
      
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

  const handlePowerTrendFetch = async () => {
    setIsLoadingPowerTrend(true);
    setPowerTrendData(null);
    try {
      const response = await axios.get(`http://localhost:8080/api/anomaly?windowSize=${anomalyWindowSize || 32}`);
      setPowerTrendData(response.data);
    } catch (err) {
      console.error("Power trend fetch failed:", err);
      setPowerTrendData({ error: true, message: "Failed to load power trend data" });
    } finally {
      setIsLoadingPowerTrend(false);
    }
  };


  return (
    <div style={{ width: '100vw', height: '100vh', overflow: 'hidden', position: 'relative', fontFamily: 'Arial', background: '#0B0E14' }}>
      
      {/* LOADING SCREEN */}
      {isLoading && (
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 1000,
          background: 'rgba(11, 14, 20, 0.98)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <div style={{
            background: 'rgba(30, 30, 30, 0.95)',
            padding: '40px 60px',
            borderRadius: '12px',
            border: '2px solid rgba(59, 130, 246, 0.5)',
            textAlign: 'center',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)'
          }}>
            <div style={{ 
              marginBottom: '20px', 
              display: 'flex', 
              justifyContent: 'center',
              animation: 'spin 1s linear infinite'
            }}>
              <Loader2 size={48} color="#3B82F6" />
            </div>
            <h3 style={{ color: '#FFFFFF', margin: '0 0 10px 0', fontSize: 20 }}>Loading Digital Twin...</h3>
            <p style={{ color: '#B0B0B0', margin: '0 0 10px 0', fontSize: 14 }}>
              {retryCount === 0 
                ? 'Initializing simulation and fetching data' 
                : `Connecting to backend server...`}
            </p>
            {retryCount > 0 && (
              <p style={{ color: '#3B82F6', margin: 0, fontSize: 12 }}>
                Attempt {retryCount} of {maxRetries}
              </p>
            )}
          </div>
        </div>
      )}
      
      {/* FULL SCREEN 3D SCENE */}
      <div style={{ width: '100%', height: 'calc(100% - 60px)', paddingBottom: '60px' }}>
        <DigitalTwinScene data={data} onRoomSelect={setSelectedRoomId} />
      </div>

      {/* TOP LEFT - DASHBOARD & BUTTONS */}
      <div style={{ position: 'absolute', top: 15, left: 15, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{
          background: 'rgba(30, 30, 30, 0.95)',
          padding: '10px 18px',
          borderRadius: 6,
          boxShadow: '0 4px 20px rgba(0,0,0,0.5)',
          border: '1px solid rgba(59, 130, 246, 0.3)',
          minWidth: '480px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <Factory size={18} color="#3B82F6" />
            <h2 style={{ margin: 0, fontSize: 14, color: '#FFFFFF', lineHeight: 1.2 }}>Digital Twin Dashboard</h2>
          </div>
          
          {/* Simulation Context */}
          {data && (
            <div style={{ 
              fontSize: 10, 
              color: '#B0B0B0', 
              borderLeft: '2px solid rgba(59, 130, 246, 0.5)', 
              paddingLeft: 8,
              marginBottom: 6
            }}>
              <div style={{ color: '#3B82F6', fontWeight: 'bold', marginBottom: 1, fontSize: 9 }}>SIMULATION CONTEXT</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 1 }}>
                <Clock size={10} />
                <span>Time: {data.timestamp}</span>
              </div>
              {data.environment?.outdoorTemp && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Thermometer size={10} />
                  <span>Outdoor: {data.environment.outdoorTemp.toFixed(1)}°C (Historical)</span>
                </div>
              )}
            </div>
          )}
          
          {/* Live Reference */}
          <div style={{ 
            fontSize: 10, 
            color: '#B0B0B0', 
            borderLeft: '2px solid rgba(34, 197, 94, 0.5)', 
            paddingLeft: 8 
          }}>
            <div style={{ color: '#22C55E', fontWeight: 'bold', marginBottom: 1, fontSize: 9 }}>LIVE REFERENCE</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              {isLoadingWeather ? (
                <>
                  <Loader2 size={10} className="animate-spin" />
                  <span>Loading weather...</span>
                </>
              ) : liveWeather ? (
                <>
                  <Thermometer size={10} />
                  <span>Current: {liveWeather.temperature?.toFixed(1)}°C ({liveWeather.location})</span>
                </>
              ) : (
                <span style={{ color: '#888' }}>Weather unavailable</span>
              )}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, flexWrap: 'nowrap', minWidth: '480px' }}>
          <Tooltip text="View current temperatures and HVAC status" position="bottom">
            <button onClick={() => setShowTempModal(true)} style={buttonStyleGlass()}><Thermometer size={14} />View All Temps</button>
          </Tooltip>
          <Tooltip text="View energy breakdown by room" position="bottom">
            <button onClick={() => setShowEnergyModal(true)} style={buttonStyleGlass()}><Zap size={14} />View Energy</button>
          </Tooltip>
          <Tooltip text="Predict energy savings from changes" position="bottom">
            <button onClick={() => setShowWhatIfModal(true)} style={buttonStyleGlass()}><FlaskConical size={14} />What-If Analysis</button>
          </Tooltip>
          <Tooltip text="Compare power trends over time" position="bottom">
            <button onClick={() => { setShowPowerTrendModal(true); handlePowerTrendFetch(); }} style={buttonStyleGlass()}><TrendingUp size={14} />Power Trends</button>
          </Tooltip>
          <Tooltip text="Detect abnormal power patterns" position="bottom">
            <button onClick={() => { setShowAnomalyModal(true); handleAnomalyCheck(); }} style={buttonStyleAlert()} className="anomaly-button"><AlertTriangle size={14} />Anomaly Detection</button>
          </Tooltip>
        </div>
      </div>

      {/* OVERLAY - Click outside to dismiss room panel */}
      {selectedRoomId && (
        <div 
          onClick={() => setSelectedRoomId(null)}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.3)',
            zIndex: 10,
            cursor: 'pointer'
          }}
        />
      )}

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
      <PowerTrendModal
        showPowerTrend={showPowerTrendModal}
        setShowPowerTrend={setShowPowerTrendModal}
        windowSize={anomalyWindowSize}
        setWindowSize={setAnomalyWindowSize}
        trendData={powerTrendData}
        handleFetchTrends={handlePowerTrendFetch}
        isLoading={isLoadingPowerTrend}
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

      {/* SYSTEM MODE DISCLAIMER */}
      <div style={{
        position: 'absolute',
        bottom: 70,
        left: '50%',
        transform: 'translateX(-50%)',
        background: 'rgba(30, 30, 30, 0.85)',
        backdropFilter: 'blur(10px)',
        WebkitBackdropFilter: 'blur(10px)',
        padding: '8px 20px',
        borderRadius: 6,
        border: '1px solid rgba(59, 130, 246, 0.2)',
        boxShadow: '0 2px 10px rgba(0,0,0,0.3)',
        fontSize: 11,
        color: '#B0B0B0',
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        zIndex: 100
      }}>
        <div style={{ 
          width: 6, 
          height: 6, 
          borderRadius: '50%', 
          background: '#3B82F6',
          boxShadow: '0 0 8px rgba(59, 130, 246, 0.8)'
        }} />
        System operates in Historical Replay Mode. Live weather shown for demonstration of external API integration.
      </div>

      {/* BOTTOM BAR */}
      <StatusBar data={data} />

      {/* ERROR DISPLAY */}
      {error && (
        <div style={{
          position: 'absolute', top: '50%', left: '50%',
          transform: 'translate(-50%, -50%)',
          background: '#e74c3c', 
          color: 'white', 
          padding: 20, 
          borderRadius: 8,
          boxShadow: '0 8px 32px rgba(231, 76, 60, 0.4)'
        }}>
          {error}
        </div>
      )}
    </div>
  );
}

// Unified Glass Button Style - Command Center Aesthetic
const buttonStyleGlass = (isActive = false) => ({
  padding: '10px 16px',
  borderRadius: 5,
  border: isActive 
    ? '1px solid #3B82F6' 
    : '1px solid rgba(59, 130, 246, 0.3)',
  cursor: 'pointer',
  background: isActive 
    ? 'rgba(59, 130, 246, 0.2)' 
    : 'rgba(30, 30, 30, 0.6)',
  backdropFilter: 'blur(10px)',
  WebkitBackdropFilter: 'blur(10px)',
  color: isActive ? '#FFFFFF' : '#3B82F6',
  boxShadow: isActive 
    ? '0 0 20px rgba(59, 130, 246, 0.4), 0 2px 8px rgba(0,0,0,0.4)' 
    : '0 2px 8px rgba(0,0,0,0.4)',
  fontWeight: 'bold',
  transition: 'all 0.3s ease',
  display: 'flex',
  alignItems: 'center',
  gap: 6
});

// Alert Button Style - Anomaly Detection Only
const buttonStyleAlert = () => ({
  padding: '10px 16px',
  borderRadius: 5,
  border: '1px solid rgba(231, 76, 60, 0.5)',
  cursor: 'pointer',
  background: 'rgba(231, 76, 60, 0.15)',
  backdropFilter: 'blur(10px)',
  WebkitBackdropFilter: 'blur(10px)',
  color: '#ff6b6b',
  boxShadow: '0 2px 8px rgba(0,0,0,0.4)',
  fontWeight: 'bold',
  transition: 'all 0.3s ease',
  display: 'flex',
  alignItems: 'center',
  gap: 6
});

export default App;
