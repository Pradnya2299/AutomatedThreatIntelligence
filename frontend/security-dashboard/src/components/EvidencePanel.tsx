import { Card, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import { formatWhen } from '@/utils/format'
import { notAvailable } from '@/utils/investigationDisplay'
import type { EvidenceItem, Investigation } from '@/types/investigation'

const GROUPS = [
  'CVE_DATABASE',
  'CPE_LOOKUP',
  'CORRELATION_ENGINE',
  'ASSET_INVENTORY',
  'RISK_ENGINE',
  'KNOWLEDGE_BASE',
  'LLM',
] as const

function groupItems(items: EvidenceItem[]) {
  const map = new Map<string, EvidenceItem[]>()
  for (const source of GROUPS) {
    map.set(source, [])
  }
  for (const item of items) {
    const key = GROUPS.includes(item.source as (typeof GROUPS)[number]) ? item.source : item.source || 'OTHER'
    if (!map.has(key)) {
      map.set(key, [])
    }
    map.get(key)!.push(item)
  }
  return map
}

export function EvidencePanel({ investigation }: { investigation: Investigation }) {
  const items = investigation.evidence ?? []
  const grouped = groupItems(items)
  return (
    <Card>
      <CardTitle>Evidence</CardTitle>
      <p className="mb-4 text-sm text-slate-300">
        Security facts come from deterministic systems. AI interprets evidence rather than inventing security facts.
      </p>
      {items.length === 0 ? (
        <p className="text-sm text-slate-400">Not available</p>
      ) : (
        <div className="space-y-4">
          {[...grouped.entries()].map(([source, rows]) => (
            <div key={source}>
              <h3 className="font-mono text-xs uppercase tracking-wide text-accent">{source}</h3>
              {rows.length === 0 ? (
                <p className="mt-1 text-xs text-slate-500">No items from this source.</p>
              ) : (
                <ul className="mt-2 space-y-2">
                  {rows.map((row, index) => (
                    <li key={`${source}-${index}`} className="rounded border border-border bg-[#0b1422] p-3 text-sm">
                      <div className="flex flex-wrap gap-2">
                        <Badge value={row.authority} />
                        <Badge value={row.confidence} />
                        <span className="text-xs text-slate-500">{notAvailable(row.type)}</span>
                      </div>
                      <p className="mt-2 text-white">{row.description ?? row.detail ?? 'Not available'}</p>
                      {row.value && <p className="mt-1 font-mono text-xs text-slate-400">{row.value}</p>}
                      <p className="mt-1 text-xs text-slate-500">{formatWhen(row.timestamp)}</p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
