import { Card } from '@/components/ui/card'
import { ConfidenceBadge, StatusBadge } from '@/components/StatusBadge'
import { formatWhen } from '@/utils/format'
import { notAvailable } from '@/utils/investigationDisplay'
import type { Investigation } from '@/types/investigation'

export function InvestigationHeader({ investigation }: { investigation: Investigation }) {
  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-accent">Security investigation</p>
          <h1 className="mt-1 font-mono text-3xl font-semibold text-white">{investigation.cveId}</h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-300">
            {investigation.threatIntelligence?.summary ?? 'Not available'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <StatusBadge value={investigation.status} />
          <ConfidenceBadge value={investigation.confidence} />
        </div>
      </div>
      <dl className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2 xl:grid-cols-4">
        <div>
          <dt className="text-slate-500">Current state</dt>
          <dd className="font-mono text-white">{notAvailable(investigation.currentState)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Started</dt>
          <dd className="text-white">{formatWhen(investigation.startedAt)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Investigation ID</dt>
          <dd className="break-all font-mono text-xs text-slate-200">{investigation.investigationId}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Overall confidence</dt>
          <dd><ConfidenceBadge value={investigation.confidence} /></dd>
        </div>
      </dl>
    </Card>
  )
}
