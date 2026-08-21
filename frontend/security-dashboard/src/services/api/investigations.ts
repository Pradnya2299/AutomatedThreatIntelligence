import { apiGet, apiPost } from '@/services/api/client'
import type { Investigation } from '@/types/investigation'

export function getInvestigation(id: string) {
  return apiGet<Investigation>(`/api/v1/investigations/${encodeURIComponent(id)}`)
}

export function createInvestigation(cveId: string) {
  return apiPost<Investigation>('/api/v1/investigations', { cveId })
}
