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

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
        <Route path="/vulnerabilities/:id" element={<VulnerabilityDetailPage />} />
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
