import { Card, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import type { Investigation } from '@/types/investigation'

export function DecisionTimeline({ investigation }: { investigation: Investigation }) {
  const history = investigation.decisionHistory ?? []
  return (
    <Card>
      <CardTitle>Agent decision history</CardTitle>
      <p className="mb-4 text-xs text-slate-500">
        These reasons are returned by the backend orchestrator. The UI does not invent explanations.
      </p>
      {history.length === 0 ? (
        <p className="text-sm text-slate-400">Not available</p>
      ) : (
        <ol className="space-y-0">
          {history.map((row, index) => (
            <li key={row.id ?? `${row.action}-${index}`}>
              <div className="flex gap-4">
                <div className="flex w-8 flex-col items-center">
                  <span className="flex h-8 w-8 items-center justify-center rounded-full border border-border text-xs text-accent">
                    {index + 1}
                  </span>
                  {index < history.length - 1 && <span className="min-h-8 w-px flex-1 bg-border" />}
                </div>
                <div className="pb-6">
                  <p className="font-mono text-sm text-white">{row.action}</p>
                  <p className="mt-1 text-sm text-slate-300">Reason: {row.reason ?? 'Not available'}</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {row.confidence && <Badge value={row.confidence} />}
                    {row.result && <Badge value={row.result} />}
                  </div>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}
    </Card>
  )
}
