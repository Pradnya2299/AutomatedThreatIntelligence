import { useState } from 'react'
import { Card, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import { notAvailable } from '@/utils/investigationDisplay'
import type { Investigation } from '@/types/investigation'

function Steps({ title, items }: { title: string; items: string[] | null | undefined }) {
  const [open, setOpen] = useState(true)
  return (
    <div className="border-t border-border pt-3">
      <button type="button" className="text-sm font-medium text-white" onClick={() => setOpen((v) => !v)}>
        {open ? '▾' : '▸'} {title}
      </button>
      {open && (
        <ol className="mt-2 list-decimal space-y-1 pl-5 text-sm text-slate-300">
          {(items && items.length > 0 ? items : ['Not available']).map((step, index) => (
            <li key={`${title}-${index}`}>{step}</li>
          ))}
        </ol>
      )}
    </div>
  )
}

export function RemediationPanel({ investigation }: { investigation: Investigation }) {
  const rem = investigation.remediation
  if (!rem) {
    return null
  }
  return (
    <Card>
      <CardTitle>Remediation recommendation</CardTitle>
      <div className="mb-3 flex flex-wrap gap-2">
        <Badge value={rem.priority} />
        <Badge value={rem.generationStatus} />
      </div>
      <p className="text-sm text-white">{rem.summary ?? 'Not available'}</p>
      <dl className="mt-4 grid grid-cols-1 gap-3 text-sm md:grid-cols-2">
        <div>
          <dt className="text-slate-500">Target version</dt>
          <dd className="font-mono text-white">{notAvailable(rem.targetVersion)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Affected components</dt>
          <dd className="text-white">Not available</dd>
        </div>
        <div>
          <dt className="text-slate-500">Downtime expected</dt>
          <dd className="text-white">Not available</dd>
        </div>
        <div>
          <dt className="text-slate-500">RAG context used</dt>
          <dd className="text-white">{rem.ragContextUsed ? 'Yes' : 'No'}</dd>
        </div>
      </dl>
      <div className="mt-4 space-y-3">
        <Steps title="Prerequisites" items={rem.prerequisites} />
        <Steps title="Implementation" items={rem.implementationSteps} />
        <Steps title="Validation" items={rem.validationSteps} />
        <Steps title="Rollback" items={rem.rollback ? [rem.rollback] : []} />
        <Steps title="References" items={rem.references} />
      </div>
    </Card>
  )
}
