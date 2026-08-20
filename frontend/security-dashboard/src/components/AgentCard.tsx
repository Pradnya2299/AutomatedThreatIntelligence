import { Card } from '@/components/ui/card'
import { ConfidenceBadge, StatusBadge } from '@/components/StatusBadge'
import { formatWhen } from '@/utils/format'
import { formatDuration, notAvailable } from '@/utils/investigationDisplay'

export function AgentCard({
  label,
  status,
  confidence,
  durationMs,
  evidenceCount,
  startedAt,
  endedAt,
  extra,
}: {
  label: string
  status: string | null
  confidence: string | null
  durationMs: number | null
  evidenceCount: number | null
  startedAt: string | null
  endedAt: string | null
  extra?: string
}) {
  const done = status === 'COMPLETED'
  return (
    <Card className="min-w-[220px] flex-1">
      <p className="text-sm font-semibold text-white">
        {done ? '✓ ' : status === 'FAILED' ? '✕ ' : ''}
        {label}
      </p>
      <dl className="mt-3 space-y-1 text-xs text-slate-400">
        <div className="flex justify-between gap-2">
          <dt>Status</dt>
          <dd><StatusBadge value={status} /></dd>
        </div>
        <div className="flex justify-between gap-2">
          <dt>Confidence</dt>
          <dd><ConfidenceBadge value={confidence} /></dd>
        </div>
        <div className="flex justify-between gap-2">
          <dt>Evidence</dt>
          <dd className="text-white">{evidenceCount === null ? 'Not available' : evidenceCount}</dd>
        </div>
        <div className="flex justify-between gap-2">
          <dt>Duration</dt>
          <dd className="text-white">{formatDuration(durationMs)}</dd>
        </div>
        <div className="flex justify-between gap-2">
          <dt>Start</dt>
          <dd className="text-right text-white">{formatWhen(startedAt)}</dd>
        </div>
        <div className="flex justify-between gap-2">
          <dt>End</dt>
          <dd className="text-right text-white">{formatWhen(endedAt)}</dd>
        </div>
        {extra && (
          <div className="flex justify-between gap-2">
            <dt>Detail</dt>
            <dd className="text-white">{notAvailable(extra)}</dd>
          </div>
        )}
      </dl>
    </Card>
  )
}
