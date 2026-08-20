import { apiGet, apiPost } from '@/services/api/client'
import type { CodeRemediation } from '@/types/codeRemediation'

export function startCodeRemediation(investigationId: string, body: Record<string, string> = {}) {
  return apiPost<CodeRemediation>(
    `/api/v1/investigations/${encodeURIComponent(investigationId)}/remediation`,
    body,
  )
}

export function getCodeRemediationForInvestigation(investigationId: string) {
  return apiGet<CodeRemediation>(`/api/v1/investigations/${encodeURIComponent(investigationId)}/remediation`)
}

export function getCodeRemediation(id: string) {
  return apiGet<CodeRemediation>(`/api/v1/remediations/${encodeURIComponent(id)}`)
}

export function approveCodeRemediation(id: string, comment?: string) {
  return apiPost<CodeRemediation>(`/api/v1/remediations/${encodeURIComponent(id)}/approve`, { comment })
}

export function rejectCodeRemediation(id: string, comment?: string) {
  return apiPost<CodeRemediation>(`/api/v1/remediations/${encodeURIComponent(id)}/reject`, { comment })
}

export function requestCodeRemediationChanges(id: string, comment?: string) {
  return apiPost<CodeRemediation>(`/api/v1/remediations/${encodeURIComponent(id)}/request-changes`, { comment })
}
