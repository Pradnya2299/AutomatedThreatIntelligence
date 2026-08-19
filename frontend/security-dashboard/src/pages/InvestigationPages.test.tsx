import { screen, waitFor } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi, afterEach, beforeEach } from 'vitest'
import { fireEvent } from '@testing-library/react'
import { InvestigationDetailPage } from '@/pages/InvestigationDetailPage'
import { NewInvestigationPage } from '@/pages/NewInvestigationPage'
import { InvestigationListPage } from '@/pages/InvestigationListPage'
import { renderApp } from '@/test/render'
import type { Investigation } from '@/types/investigation'

const completed: Investigation = {
  investigationId: '2daf8436-684a-4519-8e7e-29dc60b15a83',
  cveId: 'CVE-2021-44228',
  status: 'COMPLETED',
  correlationId: 'cac003d7-9f0a-48e8-a1d3-3b42fbcda711',
  startedAt: '2026-08-19T09:27:57Z',
  completedAt: '2026-08-19T09:27:58Z',
  currentState: 'COMPLETED',
  confidence: 'HIGH',
  threatIntelligence: {
    cveId: 'CVE-2021-44228',
    vulnerabilityId: 'e0000000-0000-0000-0000-000000000001',
    severity: 'CRITICAL',
    cvssScore: 10,
    exploitability: 'SOURCE_FLAGS_ACTIVE_EXPLOITATION',
    exploitAvailable: true,
    activelyExploited: true,
    affectedProducts: [],
    summary: 'CVE CVE-2021-44228 is recorded as CRITICAL (CVSS 10.0).',
    evidence: [],
    confidence: 'HIGH',
  },
  assetInvestigation: {
    affected: true,
    affectedAssetCount: 3,
    findingsCreated: 3,
    findingsUpdated: 0,
    matchesEvaluated: 3,
    assets: [
      {
        findingId: 'f1',
        assetId: 'a1',
        hostname: 'nw-prod-app-01',
        environment: 'PRODUCTION',
        businessCriticality: 'CRITICAL',
        internetExposure: false,
        matchType: 'VERSION_RANGE_MATCH',
        matchConfidence: 'HIGH',
        matchReason: 'Asset nw-prod-app-01 runs Apache Log4j 2.14.1, which falls within the vulnerable range.',
      },
    ],
    matchReasons: [],
    confidence: 'HIGH',
    detailsEnriched: false,
  },
  riskAnalysis: {
    primaryFindingId: 'f1',
    primaryRiskAssessmentId: 'r1',
    riskScore: 97.5,
    riskLevel: 'CRITICAL',
    factors: [{ name: 'cvss', score: 100, detail: 'from risk engine v1' }],
    explanation: 'Organizational urgency is CRITICAL (97.50) from the deterministic risk engine.',
    findingScores: [],
    confidence: 'HIGH',
  },
  remediation: {
    findingId: 'f1',
    riskAssessmentId: 'r1',
    remediationPlanId: 'p1',
    generationStatus: 'COMPLETED',
    priority: 'IMMEDIATE',
    targetVersion: '2.17.1',
    prerequisites: ['backup'],
    implementationSteps: ['patch'],
    validationSteps: ['verify'],
    rollback: 'restore previous package',
    references: ['policy.md'],
    ragSources: ['policy.md'],
    ragContextUsed: true,
    summary: '[DEMO MODE] IMMEDIATE remediation for CVE-2021-44228',
    confidence: 'HIGH',
  },
  recommendation: {
    exposed: true,
    organizationalRiskScore: 97.5,
    riskLevel: 'CRITICAL',
    priority: 'IMMEDIATE',
    summary: '[DEMO MODE] IMMEDIATE remediation for CVE-2021-44228',
  },
  executions: [
    { agentName: 'ThreatIntelligenceAgent', status: 'COMPLETED', startedAt: '2026-08-19T09:27:57Z', completedAt: '2026-08-19T09:27:57Z', failureReason: null },
    { agentName: 'AssetInvestigationAgent', status: 'COMPLETED', startedAt: '2026-08-19T09:27:57Z', completedAt: '2026-08-19T09:27:57Z', failureReason: null },
    { agentName: 'RiskAnalystAgent', status: 'COMPLETED', startedAt: '2026-08-19T09:27:57Z', completedAt: '2026-08-19T09:27:58Z', failureReason: null },
    { agentName: 'RemediationAgent', status: 'COMPLETED', startedAt: '2026-08-19T09:27:58Z', completedAt: '2026-08-19T09:27:58Z', failureReason: null },
  ],
  errors: [],
  evidence: [
    {
      source: 'CVE_DATABASE',
      type: 'cve_row',
      detail: 'Canonical CVE row',
      description: 'Canonical CVE row',
      value: 'CVE-2021-44228',
      confidence: 'HIGH',
      timestamp: '2026-08-19T09:27:57Z',
      authority: 'DETERMINISTIC',
    },
    {
      source: 'CORRELATION_ENGINE',
      type: 'match',
      detail: 'deterministic match',
      description: 'deterministic match',
      value: 'nw-prod-app-01',
      confidence: 'HIGH',
      timestamp: '2026-08-19T09:27:57Z',
      authority: 'DETERMINISTIC',
    },
    {
      source: 'RISK_ENGINE',
      type: 'risk_score',
      detail: 'Deterministic risk engine v1',
      description: 'Deterministic risk engine v1',
      value: '97.5 CRITICAL',
      confidence: 'HIGH',
      timestamp: '2026-08-19T09:27:58Z',
      authority: 'DETERMINISTIC',
    },
  ],
  decisionHistory: [
    { id: '1', action: 'RUN_THREAT_AGENT', reason: 'Threat intelligence is missing', evidence: [], result: 'RUNNING', confidence: 'MEDIUM', decidedAt: null },
    { id: '2', action: 'RUN_ASSET_AGENT', reason: 'Affected asset evidence is missing', evidence: [], result: 'RUNNING', confidence: 'MEDIUM', decidedAt: null },
    { id: '3', action: 'RUN_RISK_AGENT', reason: 'Threat and asset evidence are sufficient for deterministic risk', evidence: [], result: 'RUNNING', confidence: 'HIGH', decidedAt: null },
    { id: '4', action: 'RUN_REMEDIATION_AGENT', reason: 'Risk assessment completed', evidence: [], result: 'RUNNING', confidence: 'HIGH', decidedAt: null },
    { id: '5', action: 'COMPLETE', reason: 'Required evidence is present and investigation is complete', evidence: [], result: 'COMPLETED', confidence: 'HIGH', decidedAt: null },
  ],
  executionTrace: [
    {
      executionId: 't1',
      investigationId: '2daf8436-684a-4519-8e7e-29dc60b15a83',
      agentName: 'ThreatIntelligenceAgent',
      action: 'RUN_THREAT_AGENT',
      status: 'COMPLETED',
      startTime: '2026-08-19T09:27:57Z',
      endTime: '2026-08-19T09:27:57Z',
      durationMs: 49,
      reason: null,
      confidence: 'HIGH',
      evidenceProduced: [],
      error: null,
    },
  ],
}

function jsonOk(body: unknown) {
  return vi.fn(async () => ({
    ok: true,
    json: async () => body,
  }))
}

function jsonErr(status: number, body: unknown) {
  return vi.fn(async () => ({
    ok: false,
    status,
    json: async () => body,
  }))
}

describe('investigation pages', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders completed investigation pipeline and decisions', async () => {
    vi.stubGlobal('fetch', jsonOk(completed))
    renderApp(
      <Routes>
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
      </Routes>,
      '/investigations/2daf8436-684a-4519-8e7e-29dc60b15a83',
    )
    await waitFor(() => expect(screen.getAllByText('CVE-2021-44228').length).toBeGreaterThan(0))
    expect(screen.getByText(/Threat Intelligence Agent/)).toBeInTheDocument()
    expect(screen.getByText(/Asset Investigation Agent/)).toBeInTheDocument()
    expect(screen.getByText(/Risk Analyst Agent/)).toBeInTheDocument()
    expect(screen.getByText(/Remediation Agent/)).toBeInTheDocument()
    expect(screen.getByText('RUN_THREAT_AGENT')).toBeInTheDocument()
    expect(screen.getByText(/Threat intelligence is missing/)).toBeInTheDocument()
    expect(screen.getByText('Canonical CVE row')).toBeInTheDocument()
    expect(screen.getAllByText('nw-prod-app-01').length).toBeGreaterThan(0)
    expect(screen.getAllByText('97.5').length).toBeGreaterThan(0)
    expect(screen.getAllByText('[DEMO MODE] IMMEDIATE remediation for CVE-2021-44228').length).toBeGreaterThan(0)
    expect(screen.queryByText('Human review required')).not.toBeInTheDocument()
  })

  it('renders REVIEW_REQUIRED without a remediation card', async () => {
    vi.stubGlobal(
      'fetch',
      jsonOk({
        ...completed,
        status: 'REVIEW_REQUIRED',
        currentState: 'REVIEW_REQUIRED',
        confidence: 'LOW',
        threatIntelligence: null,
        assetInvestigation: null,
        riskAnalysis: null,
        remediation: null,
        recommendation: {
          exposed: false,
          organizationalRiskScore: null,
          riskLevel: null,
          priority: null,
          summary: 'REVIEW_REQUIRED: Threat intelligence failed; unknown CVE or lookup error',
        },
        decisionHistory: [
          { id: '1', action: 'RUN_THREAT_AGENT', reason: 'Threat intelligence is missing', evidence: [], result: 'RUNNING', confidence: 'MEDIUM', decidedAt: null },
          { id: '2', action: 'REVIEW_REQUIRED', reason: 'Threat intelligence failed; unknown CVE or lookup error', evidence: [], result: 'REVIEW_REQUIRED', confidence: 'LOW', decidedAt: null },
        ],
      }),
    )
    renderApp(
      <Routes>
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
      </Routes>,
      '/investigations/2daf8436-684a-4519-8e7e-29dc60b15a83',
    )
    await waitFor(() => expect(screen.getByText(/Human review required/i)).toBeInTheDocument())
    expect(screen.queryByText('Remediation recommendation')).not.toBeInTheDocument()
    expect(screen.getAllByText(/unknown CVE/i).length).toBeGreaterThan(0)
  })

  it('renders FAILED state', async () => {
    vi.stubGlobal(
      'fetch',
      jsonOk({
        ...completed,
        status: 'FAILED',
        currentState: 'FAILED',
        remediation: null,
        riskAnalysis: null,
        errors: [{ agentName: 'RiskAnalystAgent', code: 'RISK_ENGINE_UNAVAILABLE', message: 'Risk engine call failed; score is unknown', occurredAt: null }],
        decisionHistory: [{ id: 'x', action: 'FAIL', reason: 'Risk engine failed; remediation withheld', evidence: [], result: 'FAILED', confidence: 'LOW', decidedAt: null }],
      }),
    )
    renderApp(
      <Routes>
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
      </Routes>,
      '/investigations/2daf8436-684a-4519-8e7e-29dc60b15a83',
    )
    await waitFor(() => expect(screen.getByText(/Investigation failed/i)).toBeInTheDocument())
    expect(screen.queryByText('Remediation recommendation')).not.toBeInTheDocument()
  })

  it('shows API authentication failure', async () => {
    vi.stubGlobal('fetch', jsonErr(401, { code: 'UNAUTHORIZED', message: 'bad' }))
    renderApp(
      <Routes>
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
      </Routes>,
      '/investigations/2daf8436-684a-4519-8e7e-29dc60b15a83',
    )
    await waitFor(() => expect(screen.getByText(/API authentication failed/)).toBeInTheDocument())
    expect(screen.getByText(/http:\/\/localhost:8080/)).toBeInTheDocument()
  })

  it('starts an investigation via POST', async () => {
    vi.stubGlobal('fetch', jsonOk(completed))
    renderApp(
      <Routes>
        <Route path="/investigations/new" element={<NewInvestigationPage />} />
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
      </Routes>,
      '/investigations/new',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Start investigation' }))
    await waitFor(() => expect(screen.getByText(/Threat Intelligence Agent/)).toBeInTheDocument())
  })

  it('list page does not invent investigations', async () => {
    renderApp(<InvestigationListPage />, '/investigations')
    expect(screen.getByText(/No investigations opened in this session/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start investigation' })).toBeInTheDocument()
  })
})
