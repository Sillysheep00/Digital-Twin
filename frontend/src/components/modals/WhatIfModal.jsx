import ModalWrapper from '../ui/ModalWrapper';
import MetricCard from '../ui/MetricCard';

function WhatIfModal({
  showWhatIfModal,
  setShowWhatIfModal,
  whatIfParams,
  setWhatIfParams,
  handleWhatIfAnalysis,
  isRunningWhatIf,
  whatIfResult,
  setShowChartModal
}) {
  if (!showWhatIfModal) return null;

  const costAnalysis = whatIfResult?.costAnalysis;


  return (
    <ModalWrapper onClose={() => setShowWhatIfModal(false)} title="🔬 What-If Analysis">
      {/* Description */}
      <p style={{ color: '#666', marginBottom: '20px' }}>
        Test different scenarios to optimize energy usage and costs. Adjust parameters below and see the impact!
      </p>

      {/* Input Control */}
      <div style={{ background: '#f8f9fa', padding: '20px', borderRadius: '8px', marginBottom: '20px' }}>
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
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
            Insulation Quality: {whatIfParams.insulation} (Lower = Better)
          </label>
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
            <span>0.04 (Current)</span>
            <span>0.08 (Poor)</span>
          </div>
        </div>

         {/* Base Load*/}
         <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
            Base Load (Equipment Power): {whatIfParams.baseLoad} kW per room
          </label>
          <input
            type="range"
            min="0"
            max="2"
            step="0.1"
            value={whatIfParams.baseLoad }
            onChange={(e) => setWhatIfParams({ ...whatIfParams, baseLoad: parseFloat(e.target.value) })}
            style={{ width: '100%' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666' }}>
            <span>0 kW (Minimal)</span>
            <span>2 kW (High)</span>
          </div>
        </div>


        {/* Prediction Horizon */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
            Prediction Horizon: {whatIfParams.hours} hours
          </label>
          <select
            value={whatIfParams.hours}
            onChange={(e) => setWhatIfParams({ ...whatIfParams, hours: parseInt(e.target.value) })}
            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
          >
            <option value="12">12 hours</option>
            <option value="24">24 hours (1 day)</option>
            <option value="48">48 hours (2 days)</option>
            <option value="72">72 hours (3 days)</option>
          </select>
        </div>
      
        {/* Investment Cost (Optional) */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
            Investment Cost (Optional): £
          </label>
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
            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
          />
          <div style={{ fontSize: '11px', color: '#666', marginTop: '5px' }}>
            Enter capital investment to calculate ROI and payback period
          </div>
        </div>
        
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
            background: '#e8f4f8',
            borderRadius: '8px',
            border: '2px solid #3498db'
          }}
        >
          <h4 style={{ marginTop: 0, color: '#2c3e50' }}>
            💰 Return on Investment (ROI) Analysis
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
               value={costAnalysis?.paybackPeriodMonths?.toFixed(1)}
               suffix=" months"
               color="#3498db"
             />
            ) : costAnalysis?.annualCostSaved < 0 ? (
             <div style={{
               background: 'white',
               padding: '15px',
               borderRadius: '8px',
               boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
               textAlign: 'center'
             }}>
               <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
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
                background: 'white',
                padding: '15px',
                borderRadius: '8px',
                boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
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
        <div style={{ border: '2px solid #00b894', borderRadius: '8px', padding: '20px', background: '#e8f8f5' }}>
          <h3 style={{ marginTop: 0, color: '#00b894' }}>📊 Analysis Results</h3>

          {/* Comparison Table */}
          <table style={{ width: '100%', marginBottom: '20px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #00b894' }}>
                <th style={{ textAlign: 'left', padding: '10px' }}>Metric</th>
                <th style={{ textAlign: 'center', padding: '10px' }}>Current Scenario</th>
                <th style={{ textAlign: 'center', padding: '10px' }}>What-If Scenario</th>
                <th style={{ textAlign: 'center', padding: '10px' }}>Difference</th>
              </tr>
            </thead>
            <tbody>
              {/* Energy Usage - Always shown */}
              <tr style={{ borderBottom: '1px solid #ddd' }}>
                <td style={{ padding: '10px' }}><strong>Energy Usage</strong></td>
                <td style={{ textAlign: 'center', padding: '10px' }}>
                  {whatIfResult.baseline.predictedEnergy?.toFixed(2)} kWh
                </td>
                <td style={{ textAlign: 'center', padding: '10px' }}>
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
                  {whatIfResult.energySaved > 0 ? '▼' : '▲'} {Math.abs(whatIfResult.energySaved).toFixed(2)} kWh
                  <br />
                  <span style={{ fontSize: '12px' }}>
                    ({whatIfResult.energySaved > 0 ? '-' : '+'}{Math.abs(whatIfResult.percentSaved).toFixed(1)}%)
                  </span>
                </td>
              </tr>

                {/* Target Temperature - Only show if changed */}
                {whatIfResult.baseline?.targetTemp !== undefined && whatIfResult.scenario?.targetTemp !== undefined && (
                <tr style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: '10px' }}><strong>Target Temperature</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
                    {whatIfResult.baseline.targetTemp?.toFixed(1)}°C
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
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
                    {whatIfResult.targetTempDifference < 0 ? '▼' : '▲'} {Math.abs(whatIfResult.targetTempDifference || 0).toFixed(1)}°C
                    <br />
                    <span style={{ fontSize: '12px', color: '#666' }}>
                      {whatIfResult.targetTempDifference < 0 ? 'Reduced' : 'Increased'} target temperature
                    </span>
                  </td>
                </tr>
              )}

              {/* Insulation - Only show if changed */}
              {whatIfResult.baseline?.insulation !== undefined && whatIfResult.scenario?.insulation !== undefined && (
                <tr style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: '10px' }}><strong>Insulation</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
                    {whatIfResult.baseline.insulation?.toFixed(3)}
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
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
                    {whatIfResult.insulationDifference < 0 ? '▼' : '▲'} {Math.abs(whatIfResult.insulationDifference || 0).toFixed(3)}
                    <br />
                    <span style={{ fontSize: '12px', color: '#666' }}>
                      {whatIfResult.insulationDifference < 0 ? 'Improved' : 'Worsened'} insulation
                    </span>
                  </td>
                </tr>
              )}
              
               {/* Base Load - Only show if changed */}
               {whatIfResult.baseline?.baseLoad !== undefined && whatIfResult.scenario?.baseLoad !== undefined && (
                <tr style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: '10px' }}><strong>Base Load</strong></td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
                    {whatIfResult.baseline.baseLoad?.toFixed(2)} kW
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px' }}>
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
                    {whatIfResult.baseLoadDifference < 0 ? '▼' : '▲'} {Math.abs(whatIfResult.baseLoadDifference || 0).toFixed(2)} kW
                    <br />
                    <span style={{ fontSize: '12px', color: '#666' }}>
                      {whatIfResult.baseLoadDifference < 0 ? 'Reduced' : 'Increased'} equipment load
                    </span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Savings Summary */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginTop: '15px' }}>
            <div style={{ background: 'white', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
              <div style={{ fontSize: '12px', color: '#666' }}>Cost Saved (Period)</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', color: (whatIfResult.costSaved >= 0 ? 'green' : 'red') }}>
                ${whatIfResult.costSaved?.toFixed(2)}
              </div>
            </div>
            <div style={{ background: 'white', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
              <div style={{ fontSize: '12px', color: '#666' }}>Annual Savings</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold', color: (whatIfResult.costSaved >= 0 ? 'green' : 'red') }}>
                ${whatIfResult.annualCostSaved?.toFixed(2)}
              </div>
            </div>
          </div>

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
              📊 View Energy Comparison Chart
            </button>
          )}

          {/* Recommendation */}
          {whatIfResult.energySaved > 0 ? (
            <div
              style={{
                marginTop: '20px',
                padding: '15px',
                background: '#d4edda',
                border: '1px solid #c3e6cb',
                borderRadius: '5px',
                color: '#155724'
              }}
            >
              <strong>✅ Recommendation:</strong> This scenario would save energy and reduce costs. Consider implementing these changes!
            </div>
          ) : (
            <div
              style={{
                marginTop: '20px',
                padding: '15px',
                background: '#f8d7da',
                border: '1px solid #f5c6cb',
                borderRadius: '5px',
                color: '#721c24'
              }}
            >
              <strong>⚠️ Note:</strong> This scenario would increase energy consumption. Current settings are more efficient.
            </div>
          )}
        </div>
      )}

      {/* Error */}
      {whatIfResult && whatIfResult.error && (
        <div style={{ padding: '15px', background: '#f8d7da', border: '1px solid #f5c6cb', borderRadius: '5px', color: '#721c24' }}>
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
    </ModalWrapper>
  );
}

export default WhatIfModal;
