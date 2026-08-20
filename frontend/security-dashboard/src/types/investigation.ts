export type InvestigationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'REVIEW_REQUIRED'

export type InvestigationState =
  | 'INITIALIZED'
  | 'INVESTIGATING'
  | 'THREAT_ANALYZED'
  | 'ASSETS_ANALYZED'
  | 'RISK_ANALYZED'
  | 'REMEDIATION_GENERATED'
  | 'COMPLETED'
  | 'REVIEW_REQUIRED'
  | 'FAILED'

export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW'

export type AgentStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export type AgentAction =
  | 'RUN_THREAT_AGENT'
  | 'RUN_ASSET_AGENT'
  | 'RUN_RISK_AGENT'
  | 'RUN_REMEDIATION_AGENT'
  | 'REQUEST_MORE_EVIDENCE'
  | 'COMPLETE'
  | 'REVIEW_REQUIRED'
  | 'FAIL'

export type EvidenceSource =
  | 'CVE_DATABASE'
  | 'CPE_LOOKUP'
  | 'CORRELATION_ENGINE'
  | 'ASSET_INVENTORY'
  | 'RISK_ENGINE'
  | 'KNOWLEDGE_BASE'
  | 'LLM'

export type EvidenceAuthority = 'DETERMINISTIC' | 'INTERPRETATION'

export type EvidenceItem = {
  source: string
  type: string | null
  detail: string | null
  description: string | null
  value: string | null
  confidence: Confidence | null
  timestamp: string | null
  authority: EvidenceAuthority | string | null
}

export type AgentExecution = {
  agentName: string
  status: AgentStatus | string
  startedAt: string | null
  completedAt: string | null
  failureReason: string | null
}

export type AgentError = {
  agentName: string
  code: string | null
  message: string | null
  occurredAt: string | null
}

export type DecisionRecord = {
  id: string | null
  action: AgentAction | string
  reason: string | null
  evidence: string[] | null
  result: string | null
  confidence: Confidence | null
  decidedAt: string | null
}

export type ExecutionTraceEntry = {
  executionId: string | null
  investigationId: string | null
  agentName: string
  action: AgentAction | string | null
  status: AgentStatus | string | null
  startTime: string | null
  endTime: string | null
  durationMs: number | null
  reason: string | null
  confidence: Confidence | null
  evidenceProduced: string[] | null
  error: string | null
}

export type AffectedProduct = {
  vendor: string | null
  product: string | null
  versionStartIncluding: string | null
  versionEndExcluding: string | null
  cpe: string | null
}

export type ThreatIntelligence = {
  cveId: string
  vulnerabilityId: string | null
  severity: string | null
  cvssScore: number | null
  exploitability: string | null
  exploitAvailable: boolean | null
  activelyExploited: boolean | null
  affectedProducts: AffectedProduct[] | null
  summary: string | null
  evidence: EvidenceItem[] | null
  confidence: Confidence | null
}

export type AffectedAssetMatch = {
  findingId: string | null
  assetId: string | null
  hostname: string | null
  environment: string | null
  businessCriticality: string | null
  internetExposure: boolean
  matchType: string | null
  matchConfidence: string | null
  matchReason: string | null
}

export type AssetInvestigation = {
  affected: boolean
  affectedAssetCount: number
  findingsCreated: number
  findingsUpdated: number
  matchesEvaluated: number
  assets: AffectedAssetMatch[] | null
  matchReasons: string[] | null
  confidence: Confidence | null
  detailsEnriched: boolean
}

export type RiskFactor = {
  name: string
  score: number | null
  detail: string | null
}

export type FindingRiskScore = {
  findingId: string | null
  assetId: string | null
  riskAssessmentId: string | null
  riskScore: number | null
  riskLevel: string | null
  factors: RiskFactor[] | null
  calculationDetails: string | null
}

export type RiskAnalysis = {
  primaryFindingId: string | null
  primaryRiskAssessmentId: string | null
  riskScore: number | null
  riskLevel: string | null
  factors: RiskFactor[] | null
  explanation: string | null
  findingScores: FindingRiskScore[] | null
  confidence: Confidence | null
}

export type Remediation = {
  findingId: string | null
  riskAssessmentId: string | null
  remediationPlanId: string | null
  generationStatus: string | null
  priority: string | null
  targetVersion: string | null
  prerequisites: string[] | null
  implementationSteps: string[] | null
  validationSteps: string[] | null
  rollback: string | null
  references: string[] | null
  ragSources: string[] | null
  ragContextUsed: boolean
  summary: string | null
  confidence: Confidence | null
}

export type Recommendation = {
  exposed: boolean
  organizationalRiskScore: number | null
  riskLevel: string | null
  priority: string | null
  summary: string | null
}

export type Investigation = {
  investigationId: string
  cveId: string
  status: InvestigationStatus | string
  correlationId: string | null
  startedAt: string | null
  completedAt: string | null
  threatIntelligence: ThreatIntelligence | null
  assetInvestigation: AssetInvestigation | null
  riskAnalysis: RiskAnalysis | null
  remediation: Remediation | null
  recommendation: Recommendation | null
  executions: AgentExecution[] | null
  errors: AgentError[] | null
  evidence: EvidenceItem[] | null
  currentState: InvestigationState | string | null
  confidence: Confidence | null
  decisionHistory: DecisionRecord[] | null
  executionTrace: ExecutionTraceEntry[] | null
}

export const PIPELINE_AGENTS = [
  { name: 'ThreatIntelligenceAgent', label: 'Threat Intelligence Agent', action: 'RUN_THREAT_AGENT' as AgentAction },
  { name: 'AssetInvestigationAgent', label: 'Asset Investigation Agent', action: 'RUN_ASSET_AGENT' as AgentAction },
  { name: 'RiskAnalystAgent', label: 'Risk Analyst Agent', action: 'RUN_RISK_AGENT' as AgentAction },
  { name: 'RemediationAgent', label: 'Remediation Agent', action: 'RUN_REMEDIATION_AGENT' as AgentAction },
] as const

export function isTerminalStatus(status: string | null | undefined): boolean {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'REVIEW_REQUIRED'
}
