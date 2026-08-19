const KEY = 'threat-advisor.recent-investigations'

export type RecentInvestigation = {
  investigationId: string
  cveId: string
  status: string
  openedAt: string
}

export function readRecentInvestigations(): RecentInvestigation[] {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw) as RecentInvestigation[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function rememberInvestigation(entry: RecentInvestigation): void {
  const next = [entry, ...readRecentInvestigations().filter((row) => row.investigationId !== entry.investigationId)]
  sessionStorage.setItem(KEY, JSON.stringify(next.slice(0, 20)))
}
