import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { DashboardPage } from '@/pages/DashboardPage'
import { renderApp } from '@/test/render'

const summary = {
  criticalVulnerabilities: 4,
  highVulnerabilities: 12,
  mediumVulnerabilities: 27,
  lowVulnerabilities: 41,
  affectedAssets: 18,
  criticalFindings: 7,
  pendingRemediations: 9,
  aiRemediationPlans: 9,
  riskDistribution: { critical: 7, high: 5, medium: 3, low: 1 },
  topVulnerabilities: [
    {
      cveId: 'CVE-2021-44228',
      severity: 'CRITICAL',
      cvss: 10,
      affectedAssets: 2,
      riskScore: 97.5,
      riskLevel: 'CRITICAL',
      status: 'Open',
      aiRecommendation: 'GENERATED',
    },
  ],
  recentActivity: [
    {
      type: 'FINDING',
      message: 'CVE CVE-2021-44228 correlated with asset nw-prod-app-01',
      cveId: 'CVE-2021-44228',
      findingId: 'f1',
      occurredAt: '2026-08-18T12:00:00Z',
    },
  ],
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: true,
        json: async () => summary,
      })),
    )
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders KPI values from the API', async () => {
    renderApp(<DashboardPage />)
    expect(screen.getByLabelText('Loading')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('4')).toBeInTheDocument())
    expect(screen.getByText('Critical vulnerabilities')).toBeInTheDocument()
    expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument()
    expect(screen.getByText(/correlated with asset/)).toBeInTheDocument()
  })

  it('shows an error state', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: false,
        status: 500,
        json: async () => ({ message: 'Unable to load vulnerability data. Please try again.' }),
      })),
    )
    renderApp(<DashboardPage />)
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  })
})
