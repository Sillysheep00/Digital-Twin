
design a clean diagram for Live Twin vs What-If Twin vs Prediction Twin for your report   
enhance the cost analysis logic so ROI higher than how many percentage then you only recoommedn the user to apply the setting otherwise do not recommend it 
Remove 12 hour horizon in whatifanalysis

Week 5-6: Error Messages
创建ErrorMessage组件
替换所有error显示
测试各种error scenarios
添加recovery建议

Note 
always run mvn clean compile when make any changes on the .ecore and .smartoffice file

Git Best Practice
Create a new branch when implementing a feature (git checkout -b branch name)
Add,commit and push( git push origin branch name) to that branch ( if will go into an editor , press esc and type :wq to exit)
Merge it to main   ( git merge branch name)
Delete branch (Git commits are the checkpoint not the branch)


Note:
Power(kW) = instantaneous rate(current consumption ) 
Energy(kWh) = cumulative consumption over time 

manual override will affect graph (checked)

Answer for Question
1. dataManager and predictiveModel and digital twin in the metamodel 的用处是什么 because seems like we are not using them
metamodel defines the concept and structure of the system (like a system blueprint)
You then choose how to implement those in the code

metamodel is not:
 -a to-do list, 
 -promise that everything must run in emf 
 -and a requirement that every element must have code 

meaning it does not say you must implement a feature inside emf, it say the system contain this feature.
How you implement it is on your choice

can explain like this in the report 
The metamodel includes concepts such as DataManager and PredictiveModel to represent data flow and analytics at 
a conceptual level. In the current prototype, these elements are not directly executed by the EMF engine. 
Instead, their responsibilities are implemented through Spring Boot services and machine learning components. 
This separation allows the model to remain technology-agnostic while enabling flexible backend implementation.

2. Differences between springboot api test and powershell api test
Springboot api test check if the API endpoint behave correctly inside the application
Springboot api test runs inside spring, it does not required backend to be run, faster speed and best for development and CI(Continous Integration)
Powershell api test check if the deployed system work end to end
Powershell api test runs outside the system, required backend to be run, slower speed and best for validation and demo

In a project we need to use both of it.

3.What is a unit test and integration test
A unit test test one small piece of logic in isolation, one class one method no database and no web server
Goal is to check if the method work correctly by itself

An integration test check that multiple components works together.
Goal is to check if part integrate correctly
For example:
Service + Repository 
Controller + Service

Service level documentation explains what a service does and why, not whether it works

4. What is pagination
Pagination is a techinqiue to retrieve data in small chunks(pages) instead of loading everything at once from the database. think
of it like reading a book page by page instead of trying to read all 1000 pages at once.

Example:
1. PageRequest.of(0, trainingSampleSize, Sort.by(Sort.Direction.ASC, "date"))
  - this mean give me the first page, but make the page size big enough to hold 4252 records(20% of the dataset)

2. PageRequest.of(0, stepsNeeded, Sort.by(Sort.Direction.ASC, "date"))
  - this mean give me the first 96(stepsNeeded) records that come after a date

Why do you use pagination?
Pagination allows me to efficiently query large datasets without loading everything into memory.

For example, my dataset has over 21,000 sensor readings. During simulation, I only need ONE record at a 
time, so I use PageRequest.of(index, 1, ...) to fetch just that single record.

For predictions, I only need the next 96 records (24 hours × 4 steps/hour), so I use 
PageRequest.of(0, 96, ...) to get exactly those records.

This approach is memory-efficient and follows best practices for working with large datasets in Spring Boot and MongoDB.

5. Why the ML Prediction Value in the Anomaly Detection Appears Constant

In the current implementation, the anomaly detection model uses HVAC power consumption and a base load as its primary input features. 
Under normal operating conditions, the HVAC system reaches a steady-state once the indoor temperature approaches the target setpoint. 
As a result, HVAC power consumption remains nearly constant over time. Since the Linear Regression model learns a direct relationship 
between these input features and the predicted power, the predicted value also remains approximately constant.

This behaviour does not indicate a fault in the machine learning model. Instead, it reflects the limited variability of the input features
provided to the model. When the inputs do not change significantly, the model output naturally remains stable.

To demonstrate that the machine learning prediction is dynamic and responsive to different operating conditions, additional debug statements were introduced. 
These experiments show that when the target temperature is modified, the HVAC system requires different amounts of power to reach the new setpoint. 
Consequently, the Linear Regression model scales its prediction proportionally to the change in HVAC power, confirming that the model responds correctly to variations in input data.

Although the target temperature may remain fixed during normal operation, HVAC power consumption varies dynamically based on the instantaneous temperature error between the current indoor temperature
and the desired setpoint. As the room approaches the target temperature, the required heating or cooling load decreases, resulting in lower HVAC power consumption. Once thermal equilibrium is achieved, 
the system enters a steady-state phase where only maintenance energy is required.

6.Why use a linear regression based calibration ?
Before going into this question, let's first answer what is a calibration.
Calibration aligns the simulated system with the real world measurement. For example i am a student from malaysia
to obtain Malaysia time from UK time, a fixed offset is added in my case it will be +8.

In this project, a linear regression-based calibration is used for the following reason:
There is a systematic difference between simulation and real data. My model only handles HVAC and minimal base load,
while the real dataset includes additional constant energy consumer.This causes the trend of energy usage is similar but 
the absolute values are consistently different.

Linear regression is suitable because it can correct a scaling difference (by the slope) and a constant offset (by the intercept).

Why did you use linear regression instead of a decision tree or other ML methods?
I chose linear regression because the relationship between simulated HVAC power and real building power is approximately linear. 
The main difference comes from unmodeled constant loads such as lighting and equipment, which appear as a linear offset rather than nonlinear behavior.

In my system, the physics-based model already captures the main dynamics of HVAC energy. The real building
power differs mainly by a proportional scaling and a constant base load. This fits naturally into a linear relationship.

Decision trees are more suitable when:
  -Relationships are highly nonlinear
  -There are many interacting categorical features

Linear regression provides explicit parameters—a slope and an intercept—that have physical meaning. 
This allows me to explain exactly how the simulation is being calibrated.

For example:
  -Slope → HVAC scaling mismatch
  -Intercept → constant background loads
Decision trees do not offer this level of physical interpretability.

Decision trees can overfit when the feature set is small or when the input signal is relatively constant,
which is the case for HVAC power under steady operation. Linear regression provides smoother and more stable predictions.

The goal of this project is not to compare machine learning algorithms, but to demonstrate a digital twin system that integrates
physics-based simulation with data-driven calibration and anomaly detection.Linear regression achieves this with minimal complexity.

Could decision tree work?
Yes, decision trees could be used if additional nonlinear features such as weather conditions, or equipment schedules were included. 
However, given the current feature set, linear regression is the most suitable choice.


7.Explain how your cost analysis feature work
Cost analysis is not a standalone feature. It is a derived interpretation of a what if scenario

Example scenario
Building Manager ask should we upgrade insulation in this building
The cost analysis goal is to decide whether an isulation upgrade is worth the money

Step 1:
The building currently operates with target temperature 22 degree celsius and  insulation value of 0.04
From this the system know the baseline cost of operation

Step 2:
The building manager ask what if we improve simulation to 0.06?
We adjust the insulation value to 0.06 and set the analysis period(1 month) and run the what if analysis
After running we can know whether the energy is saved(let say 145kWh) with the setting. 
But this is not enough to make a decision.

Step3:
Now building manager may ask the question what does 145kWh mean in money?
I follow the UK electricity prices which 0.3 pound per kwH

From this the system can calculate the monthly and anual savings.
Monthy saving = 145 * 0.4 = 43.5
Annual saving = 43.5 * 12 = 522

Important!: If saving is negative the system will not display the ROI and payback, as negative ROI and payback
            has no meaningful interpretation and maybe misleading for non technical user

Step 4: The ROI & Payback is another supporting evidence for the decision
Building manager enter the insulation upgrade cost 2000 pound 
  - Insulation upgrade cost is like a budget for the upgrade

The system will calculates the payback period
Payback period = 2000 / 522 = 3.8 years
  - payback period（回本时长） = how long it takes for savings to payback initial cost.
  - think like this : You spend money first, then save money every month.
  - so 3.8 years your savings will equal to your initial cost and after that will be pure profit
  - Payback Period	Interpretation
    < 3 years	Excellent
    3–5 years	Good
    5–10 years	Acceptable
    > 10 years	Usually not attractive

ROI  = (522/2000) * 100 = 26.1 %
  - ROI = Return on Investment
  - It tells how efficient your investment is, 
  basically it answer how much money do you earn back for the amount of money you spent
  - Formula : ROI (%) = (Annual Savings ÷ Investment Cost) × 100
  From our example 26.1 % meaning every year we will recover 26.1 of our investment , so for the 2000 pound
  it takes roughly 3.8 years to fully recover 
  -Higher ROI meaning better investment


By having all this the building manager can make decision
Payback period < 5 years   worth considering
Positive ROI  financially sensible

Cost Analysis alone cannot answer:
  “What happens if insulation changes?”
  “What if target temperature is lowered?”
  “What if operating hours change?”

Cost Analysis WITH What-If can answer:
  “Is this change worth investing in?”
  “How long until it pays for itself?”
  “What is the financial impact of my design choice?”

8. why when selected 24 hour analysis window for power trend and residual graph, the result does not show full 24 hour?
The power trend and anomaly detection graphs are data-driven and depend on historical observations. 
Therefore, at system startup, only partial windows are available. This reflects realistic monitoring 
constraints and avoids misleading analysis.

9. Why not display ML-calibrated energy in the energy consumption report?
The energy consumption report reflects the Digital Twin’s internal state based on physical and rule-based simulation. 
Machine learning is used only for calibration, comparison, and anomaly detection, not as a source of truth for operational 
energy reporting

10. Explanation on the threshold implementation.
Previous implmentation : I uses fixed percentage(25%)of real power as the threshold, threshold will change every time steps as i take 25% of the
real power of the current step as the threshold so the threshold will look different every timestep. I use simple comparison residual > comparison and no statistical
analysis(no rolling mean / std).This causes the simulated power always exceed the threshold in the graph and showing office is in abnormal condition.

Reason of replacing it to new implementation:
Always anomalous: when predicted power was small, even tiny absolute errors looked large in percentage terms
No statistical context: didn't account for normal variation
Biased model: systematic under/over-prediction caused constant false positives

I change the implmentation to :
Rolling mean and standard deviation over last 32 steps (8 hours)
Z-score calculation: zScore = (residual - meanResidual) / stdResidual
Threshold: meanResidual + 3.0 * stdResidual (3-sigma rule)
Fallback: Still uses predictedPower * 0.25 when insufficient data (line 84, 103)

The anomaly decision will have 3 rule:
1. Ignore tiny residuals for noise filterring
2. In Z-score threshold
3. fallback for insufficient data

This implementation is better is because the threshold is adjust based on recent behavior,with this implementation
it solves always anomalous problem(reduce false positives), adaptive threshold (adjust to system behaviour), works at 
all power levels(handles low power scenario(by the rule 1 in the anomay decision)), better diagonistics (z-score show how unusual
values are).

11.When lecturer ask why didnt you train the ML model on the full simulation test?
Because the full simulation includes complex thermal dynamics and state-dependent behaviour, the training data distribution
 becomes unstable with limited samples. To ensure the ML model learns a clean and consistent relationship,
 I calibrated it on the fast estimation layer where the input-output relationship is well-defined. 
This allowed me to clearly demonstrate the effectiveness of ML-based calibration without introducing confounding physical noise.

Clonning isse explanation
During development, an architectural issue was identified where runtime HVAC simulation logic persisted energy consumption values into 
the base EMF model via EOL store operations. This resulted in contamination of the design-time model and incorrect behaviour in prediction
and what-if simulations. The architecture was refactored to enforce strict separation between the immutable base model, runtime digital twin 
instance, and predictive twin clones, ensuring timeline isolation and correctness of energy analytics(guarantees that all prediction and what-if analysis
start from a clean semantic state).

Timeline	Model Source	Energy State
Live DT	in-memory model	accumulated
Prediction	cloneModel()	reset → accumulate
What-If	loadBaseModel()	reset → accumulate
Energy Report	live model	accumulated

Model Isolation and Timeline Separation
To ensure correctness and prevent state contamination between live simulation, prediction, and what-if analysis, a strict model isolation strategy was implemented. 
The base EMF model is loaded once as a read-only template and is never mutated. At system initialisation, a deep clone of the base model is created to serve as the 
live runtime digital twin. This cloning process constructs a completely independent ResourceSet and EObject graph, ensuring that all model elements are isolated at memory level.

For prediction and what-if analysis, the current live model is deep-cloned again to create separate scenario models. These scenario models are used exclusively for
 fast-forward simulation and hypothetical modifications, and are disposed of after use. This guarantees that neither predictions nor what-if experiments can affect the live simulation
  state. In addition, energy meters are explicitly reset at the start of each prediction or scenario run, ensuring that energy accumulation is strictly scoped to the relevant timeline.

This design enforces a clear separation between historical simulation data, future prediction timelines, and hypothetical what-if scenarios. By avoiding shared references and using 
deep cloning rather than shallow wrappers, the system eliminates hidden coupling and prevents cross-timeline contamination. As a result, all simulation outputs remain deterministic, 
reproducible, and architecturally sound.

🎤 Viva Version (Short, Natural Answer)

If the examiner asks “How do you prevent predictions from affecting the live simulation?”, you can say:

“I use deep cloning at the EMF Resource level. The live digital twin is already a clone of the base model, and every prediction or 
what-if analysis is run on a further deep-cloned copy. That means each timeline – live, prediction, and scenario – has its own isolated EObject graph.
 There is no shared state, so contamination is structurally impossible.”


Dual Simulation Representation: Fast-Estimation vs Physics-Based Power
The system implements two distinct simulated power representations to balance stability and fidelity. The fast-estimation approach computes a constant average power baseline using statistical averages (e.g., 35% HVAC duty cycle, occupancy-based plug load), which serves as input to the Linear Regression calibration model. This design choice ensures a stable prediction baseline for anomaly detection, as the ML model learns to correct systematic offsets between a consistent estimate and real sensor data. The statistical threshold (Z-score) for anomaly detection benefits from this stability, reducing false positives caused by normal HVAC cycling.
In parallel, a physics-based simulation tracks the actual HVAC power consumption and plug loads computed by the hvac.eol thermal model, which responds dynamically to temperature errors, comfort zones, and occupancy-driven control logic. This representation provides high-fidelity feedback for What-If analysis and validates that parameter changes (e.g., target temperature, insulation) produce the expected directional impact on energy consumption.
This dual-layer architecture avoids the trade-off between ML stability and simulation responsiveness. The fast-estimation ensures robust anomaly detection with minimal noise, while the physics-based layer demonstrates the Digital Twin's ability to predict realistic operational behavior under hypothetical scenarios. This separation of concerns aligns with the principle of fitness-for-purpose: each representation serves its intended analytical role without compromising the other.

Viva Defense Script (If Asked):
Q: "Why do you have two simulated power values? Isn't that redundant?"
Your Answer:
"No, they serve different purposes. The fast-estimation is designed for machine learning stability—it provides a consistent input so the Linear Regression model learns a clean mapping from simulation to reality. This makes anomaly detection reliable because the threshold is based on stable residuals.
The physics-based simulation, on the other hand, reflects the actual thermal dynamics from the HVAC control logic. It responds to temperature setpoints, insulation changes, and occupancy patterns in real-time. This is essential for What-If analysis validation—when a facility manager asks, 'What if I raise the temperature to 25°C?', they need to see realistic HVAC power variation, not a flat average.
If I had used only the physics-based power for ML training, the model would learn from noisy HVAC cycling, making anomaly detection less reliable. If I had used only the fast-estimation, the What-If analysis wouldn't reflect real operational behavior. The dual approach gives me the best of both worlds."
Q: "But doesn't training on a constant estimate make your ML model less accurate?"
Your Answer:
"The ML model's job is not to simulate physics—it's to calibrate the simulation output to match real building data. The fast-estimation already captures the major energy consumers (HVAC average + plug loads). The Linear Regression learns the systematic scaling and offset between that estimate and reality.
For example, my model learned a slope of ~X and an intercept of ~Y kW, which corrects for unmodeled constant loads like lighting and equipment. Because the input is stable, the calibration is consistent and interpretable. The R² of ~0.XX and RMSE of ~X kW confirm the model generalizes well despite using a simplified input.
The physics-based simulation handles the dynamic fidelity, while the ML handles the data-driven correction. It's a classic separation of concerns in hybrid modeling."
Q: "Why not just use the physics-based power for everything and retrain the ML model?"
Your Answer:
"That's a valid alternative, and I considered it. However, retraining on physics-based power introduces two risks:
The training data would include HVAC cycling noise (ON/OFF states, load factor modulation), which could cause the ML model to overfit to short-term fluctuations rather than learning the systematic bias.
The anomaly detection threshold would need to be adaptive to HVAC state, which adds complexity and may increase false positives during legitimate HVAC ramp-up periods (e.g., morning warm-up).
Given the time constraints of this project and the need for demonstrable reliability in anomaly detection, I chose the dual-layer approach. It's architecturally sound, well-justified, and achieves both stability and responsiveness. If I were to extend this work post-submission, training on physics-based power with an adaptive threshold would be an interesting research direction."


Layer 1: Fast-Estimation (for ML)
In json.eol: simulatedTotalPower = estimatedHvacPower + estimatedPlugPower (constant)
Used for: ML training (RegressionTrainingService) and calibration baseline
Saved in: SimulationResult.simulatedPower
Displayed in: Power Trend graph (red line)
Layer 2: Physics-Based (for simulation)
In hvac.eol: Actual room.hvac.powerUsage + room.plugLoad computed every timestep
Used for: Live simulation, What-If energy predictions (via energy meters)
Saved in: Energy meters (EnergyMeter.energyConsumed)
Displayed in: What-If energy comparison chart


why changing target temperature in what if analysis have no obvious difference in the energy usage and both scenario use the same energy usage most of the time Because in your What‑If the chart is plotting step energy (15‑min kWh) aggregated to hourly points, and target temperature only affects the HVAC part—but most of the time your step energy is dominated by things that don’t change, or the HVAC control saturates / gets overridden. The main reasons (based on your actual code): 
1) Your energy includes plug/base load, which is unchanged hvac.eol adds energy each step as: totalRoomPower = hvacPower + plugPower plugPower comes from room.baseLoad (and standby ratio when empty) So if plug load is a big portion, changing target temp may only change a small slice → curves look similar.

 2) Your HVAC power can saturate, so both scenarios consume the same when “fully on” When heating/cooling, hvac.eol computes a loadFactor = gap/2 and then caps it to max 1.0. If the temperature gap is large in both scenarios, both hit 100% loadFactor, so powerUsage is the same; only runtime duration changes slightly (often small visually).

 3) Night/occupancy logic can override your setpoint Even if you set all HVAC setpoints to 25°C in What‑If, your script can override targetTemp during night/low occupancy (e.g., 22/20/16). That reduces the effective difference across the 24h window. 

 4) The building may already be near the comfort band HVAC only turns on outside targetTemp ± comfortZone. If the room temps stay inside the band for long periods, HVAC stays off → both scenarios show the same energy (mostly plug load). where should i write this in my report , limitation or design decision





Digital twin 
Digital Twin ≠ 必须用live data
Digital Twin的定义：
✅ 有physical asset的virtual model
✅ 可以模拟行为和预测结果
✅ 可以run scenarios和analysis
❌ 不一定要real-time data

Examples:
NASA的spacecraft digital twin用历史数据做training
Manufacturing digital twin用recorded sensor logs
Building digital twin可以用historical data + simulation
你的系统是digital twin吗？是的，因为：
✅ 有building的virtual model（EMF .ecore）
✅ 可以simulate HVAC behavior
✅ 有What-If Analysis
✅ 有anomaly detection
✅ 有predictive capability


Power Type详细说明
1. Real Power (真实功率)
来源: CSV historical data (May 2018)公式: data.powerConsumption范围: 10-20 kW (typical)
Use Cases:
Anomaly Detection: 作为ground truth对比
Power Trend: 显示历史真实值
ML Training: 训练线性回归模型的target value
Limitation: 只有历史数据，不反映当前live weather
2. Simulated Power - Fast Estimation (快速估算)
来源: json.eol (lines 21-35)公式:   hvacPower = hvacCount × 5.0 kW × 0.35 (duty cycle)  plugPower = baseLoad (if occupied) or baseLoad × 0.1 (standby)  total = hvacPower + plugPower
Use Cases:
ML Model Training: 作为input feature (X)
快速更新: 不需要运行完整物理模拟
Dashboard display (之前用这个，现在改用physics-based了)
Advantages:
计算速度快
与ML训练一致
Limitation:
不够精确
不反映实际HVAC状态
3. Simulated Power - Physics-based (物理模型)
来源: hvac.eol + json.eol (lines 37-57)公式:  hvacPower = Σ(r.hvac.powerUsage) for all ON HVACs  plugPower = Σ(r.plugLoad) for all rooms  total = hvacPower + plugPower  hvac.eol计算powerUsage考虑:- Heat transfer (Q = U × A × ΔT)- Temperature difference (indoor - outdoor)- HVAC ON/OFF状态- Live outdoor temperature影响
Use Cases:
StatusBar "Total Power": 显示当前物理模拟功率
Energy Modal: 显示room-level真实功率
Room display: 每个房间的实际功率
Advantages:
物理精确
反映live weather影响
动态变化 (响应HVAC控制)
Limitation:
计算复杂
需要完整hvac.eol执行
4. Predicted Power - ML-calibrated (ML校准)
来源: json.eol (lines 99-112)公式: predictedPower = (FastEstimation × mlSlope) + mlInterceptML模型: Linear Regression (trained on historical data)  mlSlope ≈ 1.0 (learned from training)  mlIntercept ≈ 0.0
Use Cases:
Anomaly Detection:
residual = |realPower - predictedPower|
Z-score analysis for threshold
Power Trend Graph: 显示ML预测线
What-If Analysis: 预测scenario结果
Advantages:
结合快速估算和ML精度
自动校准 (从历史数据学习)
适合预测和异常检测
When is it calculated:
每次simulation step (json.eol执行时)
使用当前mlSlope和mlIntercept



Table 5: Live Weather Integration Design
Aspect	Design Decision	Rationale
API Provider	OpenWeatherMap API	Free tier, reliable, well-documented
Location	Fixed coordinates (London: 51.5085, -0.1257)	Consistent testing, matches CSV timezone
Caching Strategy	10-minute cache	Reduce API calls from 5,760/day to 144/day (97.5% reduction)
Fallback Mechanism	Automatic switch to CSV historical data	System continues if API fails
Display Method	Dashboard "LIVE REFERENCE" section	Clear separation from historical simulation
Simulation Mode	Historical Replay (uses CSV outdoor temp)	Maintains consistency, live temp for display only

Design Decision Summary for Report
选择Scenario A (Display Only) 的原因:
Academic Integrity (学术诚信)
清楚区分historical simulation和live data
避免混合2018和2026 data造成confusion
System Consistency (系统一致性)
ML model trained on historical data
What-If analysis基于consistent baseline
Anomaly detection用historical patterns
Demonstration Value (展示价值)
证明API integration能力
显示system扩展性 (可以接入任何external data)
用户可看到live vs historical对比
Future Extensibility (未来扩展性)
架构支持切换到live simulation mode
只需修改DigitalTwinEngine.simulateStep()
WeatherService已经ready
Footer Disclaimer:
"System operates in Historical Replay Mode. 
Live weather shown for demonstration of external API integration."


Calibration 
 Producing a correction factor that is applied post-simulation
 关键点：
物理模型不变：hvac.eol 中的热力学系数（U-value, Q = U×A×ΔT）保持原样
回归是后处理：学习到的 slope 和 intercept 只在模拟完成后应用
用途是异常检测：不是为了改进模拟精度，而是为了预测"正常情况下真实功率应该是多少"
为什么这样设计：
保持物理完整性：不破坏经过验证的物理模型
可解释性：物理模拟和ML校正分离，便于调试
灵活性：可以随时重新训练回归模型，不影响物理引擎
简答：B，回归是后处理校正，不调整物理系数。

How RMSE and R square value is calculated 
项目	Baseline (未校准)	Calibrated (校准后)
预测值	yPred = simulatedPower	yPred = slope × simulatedPower + intercept
残差	realPower - simulatedPower	realPower - (slope × simulatedPower + intercept)
R²公式	1 - (Σ(real-simulated)² / SS_total)	1 - (Σ(real-predicted_calibrated)² / SS_total)
RMSE公式	√(Σ(real-simulated)² / n)	√(Σ(real-predicted_calibrated)² / n)


 * Why Linear Regression for FYP:
 * - Simple and explainable (not a black box)
 * - Qualifies as machine learning (learns from data)
 * - No external libraries needed (pure Java implementation)
 * - Provides interpretable coefficients
 * 
 * Machine Learning Justification:
 * - Learns parameters (slope, intercept) from historical data
 * - Generalizes to predict future power consumption
 * - Uses statistical optimization (least squares)
 * - Improves prediction compared to simple averaging


 Global exception reason
 Provides consistent error responses across the API.
 Best Practice: Centralized exception handling makes the API
 more maintainable and provides better error messages to clients.