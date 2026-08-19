import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import { DashboardPage } from '@/pages/DashboardPage'
import { VulnerabilitiesPage } from '@/pages/VulnerabilitiesPage'
import { VulnerabilityDetailPage } from '@/pages/VulnerabilityDetailPage'
import { AssetsPage } from '@/pages/AssetsPage'
import { AssetDetailPage } from '@/pages/AssetDetailPage'
import { FindingsPage } from '@/pages/FindingsPage'
import { FindingDetailPage } from '@/pages/FindingDetailPage'
import { RemediationPage } from '@/pages/RemediationPage'
import { RemediationDetailPage } from '@/pages/RemediationDetailPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { InvestigationListPage } from '@/pages/InvestigationListPage'
import { NewInvestigationPage } from '@/pages/NewInvestigationPage'
import { InvestigationDetailPage } from '@/pages/InvestigationDetailPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/investigations" replace />} />
        <Route path="/investigations" element={<InvestigationListPage />} />
        <Route path="/investigations/new" element={<NewInvestigationPage />} />
        <Route path="/investigations/:investigationId" element={<InvestigationDetailPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
        <Route path="/vulnerabilities/:cveId" element={<VulnerabilityDetailPage />} />
        <Route path="/assets" element={<AssetsPage />} />
        <Route path="/assets/:id" element={<AssetDetailPage />} />
        <Route path="/findings" element={<FindingsPage />} />
        <Route path="/findings/:id" element={<FindingDetailPage />} />
        <Route path="/remediation" element={<RemediationPage />} />
        <Route path="/remediation/:id" element={<RemediationDetailPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  )
}
