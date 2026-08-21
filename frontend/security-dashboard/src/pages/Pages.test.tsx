import { screen, waitFor } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { FindingDetailPage } from '@/pages/FindingDetailPage'
import { VulnerabilitiesPage } from '@/pages/VulnerabilitiesPage'
import { RemediationDetailPage } from '@/pages/RemediationDetailPage'
import { renderApp } from '@/test/render'

describe('core pages', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders a vulnerability list', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: true,
        json: async () => ({
          content: [
            {
              id: '1',
              cveId: 'CVE-2021-44228',
              description: 'Log4Shell',
              severity: 'CRITICAL',
              cvss: 10,
              affectedAssets: 2,
              riskScore: 97.5,
              riskLevel: 'CRITICAL',
              publishedAt: null,
              status: 'Open',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        }),
      })),
    )
    renderApp(<VulnerabilitiesPage />, '/vulnerabilities')
    await waitFor(() => expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument())
    expect(screen.getByText('Log4Shell')).toBeInTheDocument()
  })

  it('shows the API error when vulnerability list fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )
    renderApp(<VulnerabilitiesPage />, '/vulnerabilities')
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(screen.getByRole('alert').textContent).toMatch(/api-service/i)
  })

  it('renders finding detail including match explanation and risk', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: true,
        json: async () => ({
          id: 'f1',
          status: 'OPEN',
          cveId: 'CVE-2021-44228',
          vulnerabilityId: 'v1',
          assetId: 'a1',
          hostname: 'web-prod-01',
          environment: 'PRODUCTION',
          operatingSystem: 'Linux',
          matchType: 'EXACT_VERSION_MATCH',
          matchConfidence: 'HIGH',
          matchExplanation: 'Asset web-prod-01 is affected because it runs Apache HTTP Server 2.4.49.',
          risk: {
            id: 'r1',
            score: 97.5,
            level: 'CRITICAL',
            cvss: 100,
            assetCriticality: 100,
            internetExposure: 100,
            exploitability: 75,
            activeExploitation: 100,
            explanation: 'Risk is CRITICAL (97.50)',
          },
          remediation: {
            id: 'p1',
            findingId: 'f1',
            riskAssessmentId: 'r1',
            cveId: 'CVE-2021-44228',
            hostname: 'web-prod-01',
            status: 'GENERATED',
            priority: 'IMMEDIATE',
            summary: '[DEMO MODE] emergency patch',
            recommendedAction: 'Follow emergency procedure',
            targetVersion: 'unavailable',
            affectedComponents: ['httpd'],
            prerequisites: ['backup'],
            implementationSteps: ['patch'],
            validationSteps: ['health'],
            rollbackPlan: 'restore previous package',
            downtimeExpected: true,
            reasoning: 'policy',
            references: ['Emergency Security Patch Procedure'],
            modelName: 'demo-deterministic',
            demoAi: true,
            createdAt: '2026-08-18T12:00:00Z',
          },
        }),
      })),
    )
    renderApp(
      <Routes>
        <Route path="/findings/:id" element={<FindingDetailPage />} />
      </Routes>,
      '/findings/f1',
    )
    await waitFor(() => expect(screen.getByText(/Apache HTTP Server 2.4.49/)).toBeInTheDocument())
    expect(screen.getByText('AI Remediation Advisor')).toBeInTheDocument()
    expect(screen.getByText('Demo AI')).toBeInTheDocument()
  })

  it('renders a remediation plan', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: true,
        json: async () => ({
          id: 'p1',
          findingId: 'f1',
          riskAssessmentId: 'r1',
          cveId: 'CVE-2021-44228',
          hostname: 'web-prod-01',
          status: 'GENERATED',
          priority: 'IMMEDIATE',
          summary: 'Upgrade httpd',
          recommendedAction: 'Apply vendor patch',
          targetVersion: '2.4.62',
          affectedComponents: [],
          prerequisites: ['backup'],
          implementationSteps: ['install'],
          validationSteps: ['health'],
          rollbackPlan: 'restore previous package',
          downtimeExpected: true,
          reasoning: 'internet-facing critical asset',
          references: [],
          modelName: 'gpt-4o-mini',
          demoAi: false,
          createdAt: '2026-08-18T12:00:00Z',
        }),
      })),
    )
    renderApp(
      <Routes>
        <Route path="/remediation/:id" element={<RemediationDetailPage />} />
      </Routes>,
      '/remediation/p1',
    )
    await waitFor(() => expect(screen.getByText('Apply vendor patch')).toBeInTheDocument())
    expect(screen.getByText('Approve remediation (Phase 5)')).toBeInTheDocument()
  })
})
