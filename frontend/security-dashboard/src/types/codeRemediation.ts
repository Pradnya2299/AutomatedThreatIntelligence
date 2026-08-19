export type CodeRemediationState =
  | 'DISCOVERING_REPOSITORY'
  | 'REPOSITORY_FOUND'
  | 'ANALYZING_CODE'
  | 'PLAN_CREATED'
  | 'PATCH_GENERATED'
  | 'VALIDATING'
  | 'PATCH_FAILED'
  | 'PATCH_VALIDATED'
  | 'SECURITY_VERIFIED'
  | 'AWAITING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'CHANGES_REQUESTED'
  | 'PR_CREATING'
  | 'PR_CREATED'
  | 'REVIEW_REQUIRED'
  | 'FAILED'

export type CodeRemediation = {
  remediationId: string
  investigationId: string
  cveId: string
  status: string
  currentState: CodeRemediationState | string
  confidence: string | null
  reviewReason: string | null
  attemptCount: number
  createdAt: string | null
  updatedAt: string | null
  repository: {
    provider: string | null
    organization: string | null
    repository: string | null
    defaultBranch: string | null
    aiBranch: string | null
    technology: string | null
    buildSystem: string | null
    repositoryUrl: string | null
    workspacePath: string | null
    confidence: string | null
    evidence: string[] | null
  } | null
  strategy: {
    strategyType: string
    rationale: string | null
    affectedFiles: string[] | null
    expectedChanges: string[] | null
    confidence: string | null
    prerequisites: string[] | null
    risks: string[] | null
    rollbackPlan: string | null
  } | null
  plan: {
    id: string
    strategyType: string
    rationale: string | null
    affectedFiles: string[] | null
    expectedChanges: string[] | null
    prerequisites: string[] | null
    risks: string[] | null
    rollbackPlan: string | null
    confidence: string | null
  } | null
  patch: {
    id: string
    attemptNumber: number
    status: string
    unifiedDiff: string | null
    filesChanged: number
    linesAdded: number
    linesDeleted: number
    safetyStatus: string | null
    safetyViolations: string[] | null
    files: { file: string; changeType: string; beforeExcerpt: string | null; afterExcerpt: string | null }[]
  } | null
  validations: {
    command: string
    exitCode: number | null
    durationMs: number
    stdoutSummary: string | null
    stderrSummary: string | null
    status: string
  }[]
  securityVerification: { result: string; details: string | null; evidence: string[] | null } | null
  approval: { status: string; decidedBy: string | null; comment: string | null; decidedAt: string | null } | null
  pullRequest: {
    provider: string
    repository: string
    branch: string
    commitSha: string | null
    pullRequestUrl: string | null
    pullRequestNumber: number | null
    skippedReason: string | null
  } | null
  githubEnabled: boolean
}

export function isCodeRemediationTerminal(state: string | null | undefined): boolean {
  return (
    state === 'AWAITING_APPROVAL' ||
    state === 'APPROVED' ||
    state === 'REJECTED' ||
    state === 'CHANGES_REQUESTED' ||
    state === 'PR_CREATED' ||
    state === 'REVIEW_REQUIRED' ||
    state === 'FAILED'
  )
}
