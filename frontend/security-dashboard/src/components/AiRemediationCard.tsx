import { Link } from 'react-router-dom'
import { Badge } from '@/components/Badge'
import { Button } from '@/components/ui/button'
import type { RemediationPlan } from '@/types/api'

export function AiRemediationCard({
  plan,
  compact = false,
}: {
  plan: RemediationPlan
  compact?: boolean
}) {
  return (
    <section className="rounded-lg border border-[#1d4a5c] bg-[#0b1c28] p-5">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-accent">AI Remediation Advisor</p>
          <h2 className="mt-1 text-lg font-semibold text-white">Contextual plan for {plan.cveId}</h2>
        </div>
        <div className="flex gap-2">
          <Badge value={plan.priority} />
          <Badge value={plan.status} />
          {plan.demoAi && <Badge value="Demo AI" />}
        </div>
      </div>
      <p className="text-sm text-slate-300">{plan.summary}</p>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <div>
          <h3 className="text-xs uppercase tracking-wide text-slate-400">Recommended action</h3>
          <p className="mt-1 text-sm text-white">{plan.recommendedAction ?? '—'}</p>
        </div>
        <div>
          <h3 className="text-xs uppercase tracking-wide text-slate-400">Target version</h3>
          <p className="mt-1 text-sm text-white">{plan.targetVersion ?? 'Unavailable in supplied facts'}</p>
        </div>
      </div>
      {!compact && (
        <>
          <List title="Prerequisites" items={plan.prerequisites} />
          <List title="Implementation" items={plan.implementationSteps} numbered />
          <List title="Validation" items={plan.validationSteps} numbered />
          <div className="mt-4">
            <h3 className="text-xs uppercase tracking-wide text-slate-400">Rollback</h3>
            <p className="mt-1 text-sm text-slate-200">{plan.rollbackPlan ?? '—'}</p>
          </div>
          <div className="mt-4">
            <h3 className="text-xs uppercase tracking-wide text-slate-400">Why?</h3>
            <p className="mt-1 text-sm text-slate-300">{plan.reasoning ?? '—'}</p>
          </div>
          {plan.references.length > 0 && (
            <p className="mt-3 text-xs text-slate-500">References: {plan.references.join(' · ')}</p>
          )}
          <p className="mt-2 text-xs text-slate-500">
            Downtime expected: {plan.downtimeExpected == null ? 'unavailable' : plan.downtimeExpected ? 'yes' : 'no'}
            {plan.modelName ? ` · Model: ${plan.demoAi ? 'Demo AI' : plan.modelName}` : ''}
          </p>
        </>
      )}
      <div className="mt-5 flex flex-wrap gap-2">
        <Button type="button" disabled title="Coming in Phase 5">
          Review
        </Button>
        <Button type="button" disabled title="Coming in Phase 5">
          Approve remediation (Phase 5)
        </Button>
        <Link to={`/remediation/${plan.id}`} className="inline-flex items-center px-3 py-1.5 text-sm text-accent underline">
          View full remediation
        </Link>
      </div>
    </section>
  )
}

function List({ title, items, numbered }: { title: string; items: string[]; numbered?: boolean }) {
  if (!items.length) {
    return (
      <div className="mt-4">
        <h3 className="text-xs uppercase tracking-wide text-slate-400">{title}</h3>
        <p className="mt-1 text-sm text-slate-500">None recorded.</p>
      </div>
    )
  }
  return (
    <div className="mt-4">
      <h3 className="text-xs uppercase tracking-wide text-slate-400">{title}</h3>
      <ol className={`mt-1 space-y-1 text-sm text-slate-200 ${numbered ? 'list-decimal pl-5' : 'list-none'}`}>
        {items.map((item) => (
          <li key={item}>{numbered ? item : `✓ ${item}`}</li>
        ))}
      </ol>
    </div>
  )
}
