import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Card, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/Badge'
import { ApiError } from '@/services/api/client'
import {
  approveCodeRemediation,
  getCodeRemediationForInvestigation,
  rejectCodeRemediation,
  requestCodeRemediationChanges,
  startCodeRemediation,
} from '@/services/api/codeRemediation'
import type { Investigation } from '@/types/investigation'
import type { CodeRemediation } from '@/types/codeRemediation'

export function AutonomousRemediationPanel({ investigation }: { investigation: Investigation }) {
  const queryClient = useQueryClient()
  const [showDiff, setShowDiff] = useState(true)
  const query = useQuery({
    queryKey: ['code-remediation', investigation.investigationId],
    queryFn: async () => {
      try {
        return await getCodeRemediationForInvestigation(investigation.investigationId)
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null
        }
        throw error
      }
    },
    enabled: investigation.status === 'COMPLETED',
  })

  const start = useMutation({
    mutationFn: () => startCodeRemediation(investigation.investigationId, { initiatedBy: 'analyst' }),
    onSuccess: (data) => {
      void queryClient.setQueryData(['code-remediation', investigation.investigationId], data)
    },
  })
  const approve = useMutation({
    mutationFn: () => approveCodeRemediation(query.data?.remediationId ?? ''),
    onSuccess: (data) => {
      void queryClient.setQueryData(['code-remediation', investigation.investigationId], data)
    },
  })
  const reject = useMutation({
    mutationFn: () => rejectCodeRemediation(query.data?.remediationId ?? '', 'Rejected from SOC dashboard'),
    onSuccess: (data) => {
      void queryClient.setQueryData(['code-remediation', investigation.investigationId], data)
    },
  })
  const changes = useMutation({
    mutationFn: () => requestCodeRemediationChanges(query.data?.remediationId ?? '', 'Request changes'),
    onSuccess: (data) => {
      void queryClient.setQueryData(['code-remediation', investigation.investigationId], data)
    },
  })

  if (investigation.status !== 'COMPLETED') {
    return (
      <Card>
        <CardTitle>Autonomous remediation</CardTitle>
        <p className="text-sm text-slate-400">
          Code patching is available only after a completed investigation with threat, asset, and risk evidence.
        </p>
      </Card>
    )
  }

  const job = query.data
  return (
    <Card>
      <CardTitle>Autonomous remediation</CardTitle>
      <p className="mb-3 text-xs text-slate-500">
        Isolated workspace only. Default branches are never modified. Pull requests require human APPROVE.
        GitHub push is off unless GITHUB_ENABLED=true.
      </p>
      {query.isLoading ? <p className="text-sm text-slate-400">Loading remediation...</p> : null}
      {query.isError ? (
        <p className="text-sm text-rose-300">
          {query.error instanceof ApiError ? query.error.message : 'Unable to load code remediation.'}
        </p>
      ) : null}
      {!job && !query.isLoading ? (
        <div className="space-y-3">
          <p className="text-sm text-slate-300">
            No code remediation job yet. This uses the mapped repository (or REVIEW_REQUIRED if none is mapped).
          </p>
          <Button type="button" onClick={() => start.mutate()} disabled={start.isPending}>
            {start.isPending ? 'Starting…' : 'Start code remediation'}
          </Button>
          {start.isError ? (
            <p className="text-sm text-rose-300">
              {start.error instanceof ApiError ? start.error.message : 'Unable to start code remediation.'}
            </p>
          ) : null}
        </div>
      ) : null}
      {job ? <JobView job={job} showDiff={showDiff} onToggleDiff={() => setShowDiff((v) => !v)} /> : null}
      {job?.currentState === 'AWAITING_APPROVAL' ? (
        <div className="mt-4 flex flex-wrap gap-2">
          <Button type="button" onClick={() => approve.mutate()} disabled={approve.isPending}>
            Approve & create PR
          </Button>
          <Button type="button" onClick={() => reject.mutate()} disabled={reject.isPending}>
            Reject
          </Button>
          <Button type="button" onClick={() => changes.mutate()} disabled={changes.isPending}>
            Request changes
          </Button>
        </div>
      ) : null}
    </Card>
  )
}

function JobView({
  job,
  showDiff,
  onToggleDiff,
}: {
  job: CodeRemediation
  showDiff: boolean
  onToggleDiff: () => void
}) {
  const repo = job.repository
  return (
    <div className="space-y-3 text-sm">
      <div className="flex flex-wrap gap-2">
        <Badge value={job.currentState} />
        <Badge value={job.status} />
        {job.securityVerification?.result ? <Badge value={job.securityVerification.result} /> : null}
        <Badge value={job.githubEnabled ? 'GITHUB_ENABLED' : 'GITHUB_DISABLED'} />
      </div>
      <p className="font-mono text-white">{job.cveId}</p>
      <dl className="grid grid-cols-1 gap-2 md:grid-cols-2">
        <div>
          <dt className="text-slate-500">Repository</dt>
          <dd className="text-white">{repo?.repository ?? 'Not available'}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Base</dt>
          <dd className="text-white">{repo?.defaultBranch ?? 'Not available'}</dd>
        </div>
        <div>
          <dt className="text-slate-500">AI branch</dt>
          <dd className="text-white">{repo?.aiBranch ?? 'Not available'}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Strategy</dt>
          <dd className="text-white">{job.strategy?.strategyType ?? job.plan?.strategyType ?? 'Not available'}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Files changed</dt>
          <dd className="text-white">{job.patch?.filesChanged ?? 'Not available'}</dd>
        </div>
        <div>
          <dt className="text-slate-500">AI confidence</dt>
          <dd className="text-white">{job.confidence ?? job.strategy?.confidence ?? 'Not available'}</dd>
        </div>
      </dl>
      {job.reviewReason ? <p className="text-amber-200">{job.reviewReason}</p> : null}
      {job.strategy?.rationale ? <p className="text-slate-300">{job.strategy.rationale}</p> : null}
      {job.securityVerification ? (
        <p className="text-slate-300">
          Security verification: {job.securityVerification.result}. {job.securityVerification.details}
        </p>
      ) : null}
      {job.validations?.length ? (
        <div>
          <p className="text-slate-500">Validation</p>
          <ul className="mt-1 space-y-1 font-mono text-xs text-slate-300">
            {job.validations.map((run) => (
              <li key={run.command}>
                {run.status} {run.command} {run.exitCode === null ? '' : `(exit ${run.exitCode})`}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      <Button type="button" onClick={onToggleDiff}>
        {showDiff ? 'Hide diff' : 'View diff'}
      </Button>
      {showDiff ? (
        <pre className="max-h-96 overflow-auto rounded-md border border-border bg-black/40 p-3 font-mono text-xs text-slate-200">
          {job.patch?.unifiedDiff || 'Not available'}
        </pre>
      ) : null}
      {job.pullRequest ? (
        <p className="text-slate-300">
          PR: {job.pullRequest.pullRequestUrl ?? 'Not created'}{' '}
          {job.pullRequest.skippedReason ? `(${job.pullRequest.skippedReason})` : ''}
        </p>
      ) : null}
    </div>
  )
}
