
design a clean diagram for Live Twin vs What-If Twin vs Prediction Twin for your report   
enhance the cost analysis logic so ROI higher than how many percentage then you only recoommedn the user to apply the setting otherwise do not recommend it 
Fix per room bug: When i adjust the base load in per room control and  it does not display in the result section but all when i change the apply to all room it will display
The payback period display in this  1 year how many months if less than 1 year display in months
Power trend graph show two line only 
Remove 12 hour horizon in whatifanalysis


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