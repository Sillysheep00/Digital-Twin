What If Analysis 
1. Controller Layer
DigitalTwinController.java
接收 HTTP 请求
验证 @Valid WhatIfRequest
委托给 DigitalTwinEngine
2. DTO Layer
WhatIfRequest.java
changes: Map<String, Object>
hours: Integer (预测时长)
investmentCost: Double (可选)
3. Engine Layer
DigitalTwinEngine.java
predictWithWhatIf(): 同步状态并委托
4. Service Layer (核心)
WhatIfAnalysisService.java
runAnalysis(): 主流程
applyChangesToModel(): EOL 模型变换
extractAverageTargetTemp(): 提取目标温度
extractAverageInsulation(): 提取隔热值
extractBaseLoad(): 提取基础负载
calculateSavingsWithChartData(): 计算节省与图表数据
buildChartData(): 构建图表数据点
PredictionService.java
predictFutureEnergyWithSteps(): Baseline 预测
predictOnModelWithSteps(): Scenario 预测
runSimulationStepOnModel(): 单步模拟
ModelService.java
deepCloneModel(): EMF 深拷贝
createEmfModelFromResource(): Resource → EmfModel
runSimpleEolScript(): 执行 EOL 脚本
CostAnalysisService.java
analyzeCosts(): 成本分析与 ROI
5. DTO Layer (Results)
CostAnalysisResult.java
封装成本分析结果