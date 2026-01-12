Calibration test revealed a training runtime mismatch
The system architecture was deliberately refactored so that both the fast estimation layer and the machine learning calibration pipeline are based on the same simplified power calculation model. This ensures architectural consistency between the data used for training and the data used for real-time prediction, preventing distribution mismatch and unstable calibration behaviour. As a result, the regression model is trained on fast-estimated power values derived primarily from occupancy-based base loads, which leads to limited variance in the simulated data. Consequently, the model achieves a relatively low coefficient of determination (R² ≈ 0.03), as real building power consumption exhibits significantly greater variability (approximately 25–55 kW) due to factors such as outdoor temperature, weather conditions, equipment cycling, and other unmodelled dynamics. This design represents a conscious trade-off: prioritising system stability, real-time performance, and architectural correctness over physical fidelity in the fast estimation layer. The approach ensures reliable calibration behaviour, consistent anomaly detection, and predictable What-If analysis results. Future work could enhance the fast estimation model by incorporating time-of-day profiles, temperature-dependent HVAC behaviour, and stochastic variability to improve regression fit while maintaining computational efficiency.

2. In report can talk about what api test you failed and how do you correct it ( not code bug. We frame it as  experimental finding -> root cause analysis -> design correction. Put in Testing & Validation section)
Test failed 
    - Initial calibration test revealed a training runtime mismatch 



