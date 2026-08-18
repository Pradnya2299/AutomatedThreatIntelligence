import { apiGet } from '@/services/api/client'
import type {
  AssetDetail,
  AssetListItem,
  DashboardSummary,
  FindingDetail,
  FindingListItem,
  PageResponse,
  RemediationListItem,
  RemediationPlan,
  VulnerabilityDetail,
  VulnerabilityListItem,
} from '@/types/api'

function qs(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === '') {
      continue
    }
    search.set(key, String(value))
  }
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}

export function getDashboardSummary() {
  return apiGet<DashboardSummary>('/api/dashboard/summary')
}

export function getVulnerabilities(params: {
  q?: string
  severity?: string
  risk?: string
  page?: number
  size?: number
}) {
  return apiGet<PageResponse<VulnerabilityListItem>>(`/api/vulnerabilities${qs(params)}`)
}

export function getVulnerability(cveId: string) {
  return apiGet<VulnerabilityDetail>(`/api/vulnerabilities/${encodeURIComponent(cveId)}`)
}

export function getFindings(params: {
  risk?: string
  status?: string
  asset?: string
  cve?: string
  page?: number
  size?: number
}) {
  return apiGet<PageResponse<FindingListItem>>(`/api/findings${qs(params)}`)
}

export function getFinding(id: string) {
  return apiGet<FindingDetail>(`/api/findings/${id}`)
}

export function getAssets(params: { page?: number; size?: number }) {
  return apiGet<PageResponse<AssetListItem>>(`/api/assets${qs(params)}`)
}

export function getAsset(id: string) {
  return apiGet<AssetDetail>(`/api/assets/${id}`)
}

export function getRemediationPlans(params: {
  priority?: string
  status?: string
  risk?: string
  page?: number
  size?: number
}) {
  return apiGet<PageResponse<RemediationListItem>>(`/api/remediation${qs(params)}`)
}

export function getRemediationPlan(id: string) {
  return apiGet<RemediationPlan>(`/api/remediation/${id}`)
}
