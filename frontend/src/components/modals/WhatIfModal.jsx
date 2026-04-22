import ModalWrapper from '../ui/ModalWrapper';
import MetricCard from '../ui/MetricCard';
import Tooltip from '../ui/Tooltip';
import ErrorMessage from '../ui/ErrorMessage';
import { FlaskConical, Play, Loader2, ArrowDown, ArrowUp, BarChart3, DollarSign, CheckCircle, AlertCircle } from 'lucide-react';

function WhatIfModal({
  showWhatIfModal,
  setShowWhatIfModal,
  whatIfParams,
  setWhatIfParams,
  handleWhatIfAnalysis,
  isRunningWhatIf,
  whatIfResult,
  setShowChartModal,
  data,  
  baseLoadMode,  
  setBaseLoadMode,  
  roomBaseLoads,
  setRoomBaseLoads 
}) {
  if (!showWhatIfModal) return null;

  const costAnalysis = whatIfResult?.costAnalysis;
  // Helper function to format payback period
  const formatPaybackPeriod = (months) => {
    if (!months || months <= 0) return null;
    
    if (months < 12) {
      // Less than 1 year: display in months
      return `${months.toFixed(1)} month${months !== 1 ? 's' : ''}`;
    } else {
      // 1 year or more: display as "X year(s) Y month(s)"
      const years = Math.floor(months / 12);
      const remainingMonths = Math.round((months % 12) * 10) / 10; // Round to 1 decimal
      
      const yearText = years === 1 ? 'year' : 'years';
      
      if (remainingMonths < 0.1) {
        // If less than 0.1 months remaining, just show years
        return `${years} ${yearText}`;
      } else {
        const monthText = remainingMonths === 1 ? 'month' : 'months';
        return `${years} ${yearText} ${remainingMonths.toFixed(1)} ${monthText}`;
      }
    }
  };


  return (
    <ModalWrapper onClose={() => setShowWhatIfModal(false)} title={<span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><FlaskConical size={18} />What-If Analysis</span>}>
      {/* Input Control */}
      <div style={{ background: 'rgba(59, 130, 246, 0.1)', padding: '20px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(59, 130, 246, 0.3)' }}>
        <h3 style={{ marginTop: 0, color: '#FFFFFF' }}>Scenario Parameters</h3>

        {/* Target Temperature */}
        <div style={{ marginBottom: '15px' }}>
          <Tooltip text="Desired indoor temperature. Higher temperatures increase heating costs and energy consumption." position="right">
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
              Target Temperature: {whatIfParams.targetTemp}°C
            </label>
          </Tooltip>
          <input
            type="range"
            min="18"
            max="25"
            step="0.5"
            value={whatIfParams.targetTemp}
            onChange={(e) => setWhatIfParams({ ...whatIfParams, targetTemp: parseFloat(e.target.value) })}
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
          <Tooltip text="Wall insulation coefficient (W/m²·K). Lower values mean better insulation and less heat loss, reducing HVAC energy consumption." position="right">
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
              Insulation Quality: {whatIfParams.insulation} (Lower = Better)
            </label>
          </Tooltip>
          <input
            type="range"
            min="0.01"
            max="0.08"
            step="0.005"
            value={whatIfParams.insulation}
            onChange={(e) => setWhatIfParams({ ...whatIfParams, insulation: parseFloat(e.target.value) })}
            style={{ width: '100%' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666' }}>
            <span>0.01 (Excellent)</span>
            <span>0.045 (Medium)</span>
            <span>0.08 (Poor)</span>
          </div>
        </div>

        {/* Base Load - Toggle Mode */}
        <div style={{ marginBottom: '15px' }}>
          <Tooltip text="Baseline power consumption from equipment (computers, lights, etc.) excluding HVAC. Can be adjusted globally or per-room." position="right">
            <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
              Base Load (Equipment Power)
            </label>
          </Tooltip>
          
          {/* Mode Toggle */}
          <div style={{ marginBottom: '10px', display: 'flex', gap: '10px' }}>
            <button
              onClick={() => setBaseLoadMode('all')}
              style={{
                padding: '8px 16px',
                borderRadius: '4px',
                border: 'none',
                background: baseLoadMode === 'all' ? '#00b894' : '#ddd',
                color: baseLoadMode === 'all' ? 'white' : '#333',
                cursor: 'pointer',
                fontWeight: 'bold',
                fontSize: '13px',
                transition: 'all 0.2s'
              }}
            >
              Apply to All Rooms
            </button>
            <button
              onClick={() => setBaseLoadMode('perRoom')}
              style={{
                padding: '8px 16px',
                borderRadius: '4px',
                border: 'none',
                background: baseLoadMode === 'perRoom' ? '#00b894' : '#ddd',
                color: baseLoadMode === 'perRoom' ? 'white' : '#333',
                cursor: 'pointer',
                fontWeight: 'bold',
                fontSize: '13px',
                transition: 'all 0.2s'
              }}
            >
              Per-Room Control
            </button>
          </div>

          {/* All Rooms Mode */}
          {baseLoadMode === 'all' && (
            <>
              <input
                type="range"
                min="0"
                max="2"
                step="0.1"
                value={whatIfParams.baseLoad ?? 1.0}
                onChange={(e) => setWhatIfParams({ ...whatIfParams, baseLoad: parseFloat(e.target.value) })}
                style={{ width: '100%' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666' }}>
                <span>0 kW (Minimal)</span>
                <span><b>{whatIfParams.baseLoad ?? 1.0} kW</b></span>
                <span>2 kW (High)</span>
              </div>
            </>
          )}

          {/* Per-Room Mode - Expandable */}
          {baseLoadMode === 'perRoom' && (
            <div>
              {/* Global Slider*/}
              <div style={{ marginBottom: '10px', padding: '10px', background: 'rgba(30, 30, 30, 0.5)', borderRadius: '4px', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '5px' }}>
                  <span style={{ fontSize: '12px', fontWeight: '500', color: '#FFFFFF' }}>Quick Set All Rooms:</span>
                  <span style={{ fontSize: '12px', color: '#666' }}>
                    {whatIfParams.baseLoad ?? 1.0} kW
                  </span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  value={whatIfParams.baseLoad ?? 1.0}
                  onChange={(e) => {
                    const newValue = parseFloat(e.target.value);
                    setWhatIfParams({ ...whatIfParams, baseLoad: newValue });
                    // Optionally update all room values when dragging
                    if (data?.rooms) {
                      const updates = {};
                      data.rooms.forEach(room => {
                        updates[room.name] = newValue;
                      });
                      setRoomBaseLoads(prev => ({ ...prev, ...updates }));
                    }
                  }}
                  style={{ width: '100%' }}
                />
                <div style={{ fontSize: '11px', color: '#666', marginTop: '5px', fontStyle: 'italic' }}>
                  Drag to set all rooms, then adjust individual rooms below
                </div>
              </div>

               {/* Expandable Room List */}
              <div style={{ 
                border: '1px solid rgba(59, 130, 246, 0.3)',
                borderRadius: '4px',
                background: 'rgba(30, 30, 30, 0.5)',
                maxHeight: '300px',
                overflowY: 'auto'
              }}>
                {data?.rooms && data.rooms.length > 0 ? (
                  data.rooms.map(room => (
                    <div 
                      key={room.id} 
                      style={{ 
                        padding: '12px',
                        borderBottom: '1px solid rgba(59, 130, 246, 0.2)',
                        ':last-child': { borderBottom: 'none' }
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <label style={{ fontWeight: '500', fontSize: '13px', color: '#FFFFFF' }}>{room.name}</label>
                        <span style={{ color: '#B0B0B0', fontSize: '13px', fontWeight: 'bold' }}>
                          {roomBaseLoads[room.name] ?? whatIfParams.baseLoad ?? 1.0} kW
                        </span>
                      </div>
                      <input
                        type="range"
                        min="0"
                        max="2"
                        step="0.1"
                        value={roomBaseLoads[room.name] ?? whatIfParams.baseLoad ?? 1.0}
                        onChange={(e) => {
                          setRoomBaseLoads(prev => ({
                            ...prev,
                            [room.name]: parseFloat(e.target.value)
                          }));
                        }}
                        style={{ width: '100%' }}
                      />
                    </div>
                  ))  
                ) : (
                  <div style={{ padding: '20px', textAlign: 'center', color: '#B0B0B0', fontSize: '13px' }}>
                    No room data available. Please wait for simulation to load.
                  </div>
                )}
              </div>
            </div>
          )}
        </div>  

        {/* Prediction Horizon */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold', color: '#FFFFFF' }}>
            Prediction Horizon: {whatIfParams.hours} hours
          </label>
          <select
            value={whatIfParams.hours}
            onChange={(e) => setWhatIfParams({ ...whatIfParams, hours: parseInt(e.target.value) })}
            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid rgba(59, 130, 246, 0.3)', background: 'rgba(30, 30, 30, 0.5)', color: '#FFFFFF' }}
          >
            <option value="12">12 hours</option>
            <option value="24">24 hours (1 day)</option>
            <option value="48">48 hours (2 days)</option>
            <option value="72">72 hours (3 days)</option>
          </select>
        </div>
      
        {/* Investment Cost*/}
        <div style={{ marginBottom: '15px' }}>
          <Tooltip text="Initial cost for implementing the scenario changes (e.g., insulation upgrade, equipment replacement). Used to calculate ROI and payback period." position="right">
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold', color: '#FFFFFF' }}>
              Investment Cost (Optional): £
            </label>
          </Tooltip>
          <input
            type="number"
            min="0"
            step="100"
            value={whatIfParams.investmentCost || ''}
            onChange={(e) => setWhatIfParams({ 
              ...whatIfParams, 
              investmentCost: e.target.value ? parseFloat(e.target.value) : null 
            })}
            placeholder="e.g., 5000 for insulation upgrade"
            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid rgba(59, 130, 246, 0.3)', background: 'rgba(30, 30, 30, 0.5)', color: '#FFFFFF' }}
          />
          <div style={{ fontSize: '11px', color: '#B0B0B0', marginTop: '5px' }}>
            Enter capital investment to calculate ROI and payback period
          </div>
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
          {isRunningWhatIf ? (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Loader2 size={16} className="animate-spin" />Running Analysis...
            </span>
          ) : (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Play size={16} />Run What-If Analysis
            </span>
          )}
        </button>
      </div>

      {/* Results */}
      {whatIfResult && !whatIfResult.error && (
        <div style={{ border: '2px solid #00b894', borderRadius: '8px', padding: '20px', background: 'rgba(0, 184, 148, 0.15)' }}>
          <h3 style={{ marginTop: 0, color: '#51cf66', display: 'flex', alignItems: 'center', gap: 8 }}>
            <BarChart3 size={20} /> Analysis Results
          </h3>

          {/* Comparison Table */}
          <table style={{ width: '100%', marginBottom: '20px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #00b894' }}>
                <th style={{ textAlign: 'left', padding: '10px', color: '#FFFFFF' }}>Metric</th>
                <th style={{ textAlign: 'center', padding: '10px', color: '#FFFFFF' }}>Current Scenario</th>
                <th style={{ textAlign: 'center', padding: '10px', color: '#FFFFFF' }}>What-If Scenario</th>
                <th style={{ textAlign: 'center', padding: '10px', color: '#FFFFFF' }}>Difference</th>
              </tr>
            </thead>
            <tbody>
              {/* Energy Usage - Always shown */}
              <tr style={{ borderBottom: '1px solid rgba(0, 184, 148, 0.3)' }}>
                <td style={{ padding: '10px', color: '#FFFFFF' }}><strong>Energy Usage</strong></td>
                <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                  {whatIfResult.baseline.predictedEnergy?.toFixed(2)} kWh
                </td>
                <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                  {whatIfResult.scenario.predictedEnergy?.toFixed(2)} kWh
                </td>
                <td
                  style={{
                    textAlign: 'center',
                    padding: '10px',
                    fontWeight: 'bold',
                    color: whatIfResult.energySaved > 0 ? '#00b894' : '#d63031'
                  }}
                >
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    {whatIfResult.energySaved > 0 ? <ArrowDown size={14} /> : <ArrowUp size={14} />}
                    {Math.abs(whatIfResult.energySaved).toFixed(2)} kWh
                  </span>
                  <br />
                  <span style={{ fontSize: '12px' }}>
                    ({whatIfResult.energySaved > 0 ? '-' : '+'}{Math.abs(whatIfResult.percentSaved).toFixed(1)}%)
                  </span>
                </td>
              </tr>

                {/* Target Temperature - Only show if changed */}
                {whatIfResult.baseline?.targetTemp !== undefined && whatIfResult.scenario?.targetTemp !== undefined && (
                <tr style={{ borderBottom: '1px solid rgba(0, 184, 148, 0.3)' }}>
                  <td style={{ padding: '10px', color: '#FFFFFF' }}><strong>Target Temperature</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.baseline.targetTemp?.toFixed(1)}°C
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.scenario.targetTemp?.toFixed(1)}°C
                  </td>
                  <td
                    style={{
                      textAlign: 'center',
                      padding: '10px',
                      fontWeight: 'bold',
                      color: whatIfResult.targetTempDifference < 0 ? '#00b894' : '#d63031'
                    }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                      {whatIfResult.targetTempDifference < 0 ? <ArrowDown size={12} /> : <ArrowUp size={12} />}
                      {Math.abs(whatIfResult.targetTempDifference || 0).toFixed(1)}°C
                    </span>
                    <br />
                    <span style={{ fontSize: '12px', color: '#B0B0B0' }}>
                      {whatIfResult.targetTempDifference < 0 ? 'Reduced' : 'Increased'} target temperature
                    </span>
                  </td>
                </tr>
              )}

              {/* Insulation - Only show if changed */}
              {whatIfResult.baseline?.insulation !== undefined && whatIfResult.scenario?.insulation !== undefined && (
                <tr style={{ borderBottom: '1px solid rgba(0, 184, 148, 0.3)' }}>
                  <td style={{ padding: '10px', color: '#FFFFFF' }}><strong>Insulation</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.baseline.insulation?.toFixed(3)}
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.scenario.insulation?.toFixed(3)}
                  </td>
                  <td
                    style={{
                      textAlign: 'center',
                      padding: '10px',
                      fontWeight: 'bold',
                      color: whatIfResult.insulationDifference < 0 ? '#00b894' : '#d63031'
                    }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                      {whatIfResult.insulationDifference < 0 ? <ArrowDown size={12} /> : <ArrowUp size={12} />}
                      {Math.abs(whatIfResult.insulationDifference || 0).toFixed(3)}
                    </span>
                    <br />
                    <span style={{ fontSize: '12px', color: '#B0B0B0' }}>
                      {whatIfResult.insulationDifference < 0 ? 'Improved' : 'Worsened'} insulation
                    </span>
                  </td>
                </tr>
              )}
              
               {/* Base Load - Only show if changed */}
               {whatIfResult.baseline?.baseLoad !== undefined && whatIfResult.scenario?.baseLoad !== undefined && whatIfResult.scenario?.baseLoad !== undefined &&(
                <tr style={{ borderBottom: '1px solid rgba(0, 184, 148, 0.3)' }}>
                  <td style={{ padding: '10px', color: '#FFFFFF' }}><strong>Base Load</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.baseline.baseLoad?.toFixed(2)} kW
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px', color: '#B0B0B0' }}>
                    {whatIfResult.scenario.baseLoad?.toFixed(2)} kW
                  </td>
                  <td
                    style={{
                      textAlign: 'center',
                      padding: '10px',
                      fontWeight: 'bold',
                      color: whatIfResult.baseLoadDifference < 0 ? '#00b894' : '#d63031'
                    }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                      {whatIfResult.baseLoadDifference < 0 ? <ArrowDown size={12} /> : <ArrowUp size={12} />}
                      {Math.abs(whatIfResult.baseLoadDifference || 0).toFixed(2)} kW
                    </span>
                    <br />
                    <span style={{ fontSize: '12px', color: '#B0B0B0' }}>
                      {whatIfResult.baseLoadDifference < 0 ? 'Reduced' : 'Increased'} equipment load
                    </span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          
          {/*Cost Savings */}
          {costAnalysis && (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, 1fr)',
                gap: '15px',
                marginTop: '15px'
              }}
            >
              <MetricCard
                label="Daily Savings"
                prefix="£"
                value={whatIfResult.costAnalysis?.dailyCostSaved?.toFixed(2)}
                color={whatIfResult.costAnalysis?.dailyCostSaved >= 0 ? 'green' : 'red'}
              />
              <MetricCard
                label="Monthly Savings"
                prefix="£"
                value={whatIfResult.costAnalysis?.monthlyCostSaved?.toFixed(2)}
                color={whatIfResult.costAnalysis?.dailyCostSaved >= 0 ? 'green' : 'red'}
              />
              <MetricCard
                label="Annual Savings"
                prefix="£"
                value={whatIfResult.costAnalysis?.annualCostSaved?.toFixed(2)}
                color={whatIfResult.costAnalysis?.dailyCostSaved >= 0 ? 'green' : 'red'}
              />
              <MetricCard
                label="Period Savings"
                prefix="£"
                value={whatIfResult.costAnalysis?.periodCostSaved?.toFixed(2)}
                color={whatIfResult.costAnalysis?.dailyCostSaved >= 0 ? 'green' : 'red'}
              />
            </div>
          )}

          {costAnalysis?.investmentCost && (
            <div
              style={{
                marginTop: '20px',
                padding: '20px',
                background: 'rgba(52, 152, 219, 0.15)',
                borderRadius: '8px',
                border: '2px solid #3498db'
              }}
            >
              <h4 style={{ marginTop: 0, color: '#FFFFFF', display: 'flex', alignItems: 'center', gap: 8 }}>
                <DollarSign size={18} /> Return on Investment (ROI) Analysis
              </h4>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px' }}>
                <MetricCard
                  label="Investment Cost"
                  prefix="£"
                  value={costAnalysis?.investmentCost.toFixed(2)}
                  color="#e74c3c"
                />
                
                {/*Payback Period or Negative Savings Message */}
                {costAnalysis?.annualCostSaved >0  && costAnalysis?.paybackPeriodMonths ?(
                  <MetricCard
                  label="Payback Period"
                  value={formatPaybackPeriod(costAnalysis.paybackPeriodMonths)}
                  color="#3498db"
                />
                ) : costAnalysis?.annualCostSaved < 0 ? (
                <div style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  padding: '15px',
                  borderRadius: '8px',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  textAlign: 'center'
                }}>
                  <div style={{ fontSize: '12px', color: '#B0B0B0', marginBottom: '5px' }}>
                    Payback Period
                  </div>
                  <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#e74c3c' }}>
                    No payback — costs exceed savings
                  </div>
                </div>
                ) : (
                <MetricCard
                  label="Payback Period"
                  value="—"
                  color="#95a5a6"
                />
                )}

                {/* Annual ROI or Cost Increase or Not Applicable */}
                {costAnalysis?.annualCostSaved >= 0 && costAnalysis?.roiPercentage ? (
                  <MetricCard
                    label="Annual ROI"
                    value={costAnalysis?.roiPercentage?.toFixed(1)}
                    suffix="%"
                    color="#27ae60"
                  />
                ) : costAnalysis?.annualCostSaved < 0 ? (
                  <div style={{
                    background: 'rgba(255, 255, 255, 0.05)',
                    padding: '15px',
                    borderRadius: '8px',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    textAlign: 'center'
                  }}>
                    <div style={{ fontSize: '12px', color: '#B0B0B0', marginBottom: '5px' }}>
                      Annual Cost Impact
                    </div>
                    <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#e74c3c' }}>
                      Annual cost increase: £{Math.abs(costAnalysis?.annualCostSaved || 0).toFixed(2)}
                    </div>
                    <div style={{ fontSize: '12px', color: '#e74c3c', marginTop: '5px' }}>
                      ROI not applicable
                    </div>
                  </div>
                ) : (
                  <MetricCard
                    label="Annual ROI"
                    value="—"
                    color="#95a5a6"
                  />
                )}
              
              </div>
            </div>
          )}
          

          {/* Chart Button */}
          {whatIfResult.chartData && whatIfResult.chartData.length > 0 && (
            <button
              onClick={() => setShowChartModal(true)}
              style={{
                width: '100%',
                padding: '12px',
                marginTop: '20px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontWeight: 'bold',
                fontSize: '16px',
                cursor: 'pointer',
                boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                transition: 'transform 0.2s'
              }}
              onMouseOver={(e) => (e.target.style.transform = 'translateY(-2px)')}
              onMouseOut={(e) => (e.target.style.transform = 'translateY(0)')}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: 8, justifyContent: 'center' }}>
                <BarChart3 size={18} /> View Energy Comparison Chart
              </span>
            </button>
          )}

          {/* Recommendation */}
          {whatIfResult.energySaved > 0 ? (
            <div
              style={{
                marginTop: '20px',
                padding: '15px',
                background: 'rgba(81, 207, 102, 0.15)',
                border: '1px solid rgba(81, 207, 102, 0.4)',
                borderRadius: '5px',
                color: '#22C55E'
              }}
            >
              <strong style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                <CheckCircle size={16} /> Recommendation:
              </strong> This scenario would save energy and reduce costs. Consider implementing these changes!
            </div>
          ) : (
            <div
              style={{
                marginTop: '20px',
                padding: '15px',
                background: 'rgba(255, 107, 107, 0.15)',
                border: '1px solid rgba(255, 107, 107, 0.4)',
                borderRadius: '5px',
                color: '#ff6b6b'
              }}
            >
              <strong style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                <AlertCircle size={16} /> Note:
              </strong> This scenario would increase energy consumption. Current settings are more efficient.
            </div>
          )}
        </div>
      )}

      {/* Error */}
      {whatIfResult && whatIfResult.error && (
        <ErrorMessage
          type="critical"
          title="ANALYSIS FAILED"
          message={whatIfResult.message || 'What-If analysis could not be completed.'}
          recovery={[
            "Ensure backend is running and responsive",
            "Check if prediction data is available (requires ML model training)",
            "Verify parameter values are within valid ranges",
            "Try resetting parameters to defaults"
          ]}
        />
      )}
    </ModalWrapper>
  );
}

export default WhatIfModal;
