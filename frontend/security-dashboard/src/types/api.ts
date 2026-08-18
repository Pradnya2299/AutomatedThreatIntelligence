export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type RiskDistribution = {
  critical: number
  high: number
  medium: number
  low: number
}

export type TopVulnerabilityRow = {
  cveId: string
  severity: string | null
  cvss: number | null
  affectedAssets: number
  riskScore: number | null
  riskLevel: string | null
  status: string | null
  aiRecommendation: string | null
}

export type ActivityItem = {
  type: string
  message: string
  cveId: string | null
  findingId: string | null
  occurredAt: string | null
}

export type DashboardSummary = {
  criticalVulnerabilities: number
  highVulnerabilities: number
  mediumVulnerabilities: number
  lowVulnerabilities: number
  affectedAssets: number
  criticalFindings: number
  pendingRemediations: number
  aiRemediationPlans: number
  riskDistribution: RiskDistribution
  topVulnerabilities: TopVulnerabilityRow[]
  recentActivity: ActivityItem[]
}

export type VulnerabilityListItem = {
  id: string
  cveId: string
  description: string | null
  severity: string | null
  cvss: number | null
  affectedAssets: number
  riskScore: number | null
  riskLevel: string | null
  publishedAt: string | null
  status: string | null
}

export type CpeRange = {
  id: string
  cpe: string | null
  vendor: string | null
  product: string | null
  versionStartIncluding: string | null
  versionEndExcluding: string | null
}

export type AffectedAssetRow = {
  assetId: string
  findingId: string
  hostname: string
  environment: string | null
  installedVersion: string | null
  criticality: string | null
  internetExposure: boolean
  riskScore: number | null
  riskLevel: string | null
}

export type VulnerabilityDetail = {
  id: string
  cveId: string
  description: string | null
  severity: string | null
  cvss: number | null
  cvssVector: string | null
  publishedAt: string | null
  modifiedAt: string | null
  vendor: string | null
  product: string | null
  affectedVersions: CpeRange[]
  affectedAssets: AffectedAssetRow[]
  highestRiskScore: number | null
  highestRiskLevel: string | null
  remediationPlanId: string | null
  remediationStatus: string | null
  demoAi: boolean
}

export type FindingListItem = {
  id: string
  assetId: string
  hostname: string
  vulnerabilityId: string
  cveId: string
  matchType: string | null
  matchConfidence: string | null
  riskScore: number | null
  riskLevel: string | null
  status: string | null
  detectedAt: string | null
}

export type RiskBreakdown = {
  id: string
  score: number | null
  level: string | null
  cvss: number | null
  assetCriticality: number | null
  internetExposure: number | null
  exploitability: number | null
  activeExploitation: number | null
  explanation: string | null
}

export type RemediationPlan = {
  id: string
  findingId: string
  riskAssessmentId: string | null
  cveId: string
  hostname: string
  status: string
  priority: string | null
  summary: string | null
  recommendedAction: string | null
  targetVersion: string | null
  affectedComponents: string[]
  prerequisites: string[]
  implementationSteps: string[]
  validationSteps: string[]
  rollbackPlan: string | null
  downtimeExpected: boolean | null
  reasoning: string | null
  references: string[]
  modelName: string | null
  demoAi: boolean
  createdAt: string | null
}

export type FindingDetail = {
  id: string
  status: string
  cveId: string
  vulnerabilityId: string
  assetId: string
  hostname: string
  environment: string | null
  operatingSystem: string | null
  matchType: string | null
  matchConfidence: string | null
  matchExplanation: string | null
  risk: RiskBreakdown | null
  remediation: RemediationPlan | null
}

export type AssetListItem = {
  id: string
  hostname: string
  environment: string | null
  operatingSystem: string | null
  criticality: string | null
  internetExposure: boolean
  installedSoftware: string | null
  openFindings: number
}

export type AssetDetail = {
  id: string
  hostname: string
  environment: string | null
  operatingSystem: string | null
  osVersion: string | null
  criticality: string | null
  internetExposure: boolean
  status: string | null
  installedSoftware: { vendor: string; product: string; version: string | null }[]
  findings: FindingListItem[]
}

export type RemediationListItem = {
  id: string
  cveId: string
  hostname: string
  riskScore: number | null
  riskLevel: string | null
  priority: string | null
  status: string
  createdAt: string | null
}
