import type { Investigation, RiskFactor } from '@/types/investigation'

export function notAvailable(value: string | number | boolean | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return 'Not available'
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return String(value)
}

export function formatDuration(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) {
    return 'Not available'
  }
  return `${ms} ms`
}

export function factorScore(factors: RiskFactor[] | null | undefined, name: string): string {
  const match = factors?.find((f) => f.name === name)
  if (!match || match.score === null || match.score === undefined) {
    return 'Not available'
  }
  return String(match.score)
}

export function versionFromReason(reason: string | null | undefined): string {
  if (!reason) {
    return 'Not available'
  }
  const match = reason.match(/\d+\.\d+(?:\.\d+)*/)
  return match ? match[0] : 'Not available'
}

export function evidenceCountForAgent(investigation: Investigation, agentName: string): number {
  const evidence = investigation.evidence ?? []
  if (agentName === 'ThreatIntelligenceAgent') {
    return evidence.filter((e) => e.source === 'CVE_DATABASE' || e.source === 'CPE_LOOKUP').length
  }
  if (agentName === 'AssetInvestigationAgent') {
    return evidence.filter((e) => e.source === 'CORRELATION_ENGINE' || e.source === 'ASSET_INVENTORY').length
  }
  if (agentName === 'RiskAnalystAgent') {
    return evidence.filter((e) => e.source === 'RISK_ENGINE').length
  }
  if (agentName === 'RemediationAgent') {
    return evidence.filter((e) => e.source === 'KNOWLEDGE_BASE' || e.source === 'LLM').length
  }
  return 0
}
