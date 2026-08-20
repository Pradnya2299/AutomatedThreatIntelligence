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
  const [example, setExample] = useState<'mapped' | 'maven' | 'docker' | 'source' | 'empty'>('mapped')
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
    mutationFn: () => startCodeRemediation(investigation.investigationId, startBody(example)),
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
            Pick a demo target. Maven, Docker, and source-hash are isolated fixture workspaces. Empty shows
            REVIEW_REQUIRED.
          </p>
          <label className="block text-xs text-slate-500">
            Example
            <select
              className="mt-1 block w-full rounded-md border border-border bg-[#08101c] px-2 py-1.5 text-sm text-white"
              value={example}
              onChange={(event) => setExample(event.target.value as typeof example)}
            >
              <option value="mapped">Mapped asset repo (seed / GitHub binding)</option>
              <option value="maven">Example: Maven Log4j (pom.xml)</option>
              <option value="docker">Example: Docker base image (Dockerfile)</option>
              <option value="source">Example: insecure hash (Java source)</option>
              <option value="empty">Example: cannot safely fix</option>
            </select>
          </label>
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

function startBody(example: 'mapped' | 'maven' | 'docker' | 'source' | 'empty'): Record<string, string> {
  if (example === 'maven') {
    return {
      initiatedBy: 'analyst',
      provider: 'LOCAL_WORKSPACE',
      repository: 'payment-service',
      repositoryUrl: 'local://payment-service',
      defaultBranch: 'main',
    }
  }
  if (example === 'docker') {
    return {
      initiatedBy: 'analyst',
      provider: 'LOCAL_WORKSPACE',
      repository: 'container-service',
      repositoryUrl: 'local://container-service',
      defaultBranch: 'main',
    }
  }
  if (example === 'source') {
    return {
      initiatedBy: 'analyst',
      provider: 'LOCAL_WORKSPACE',
      repository: 'source-service',
      repositoryUrl: 'local://source-service',
      defaultBranch: 'main',
    }
  }
  if (example === 'empty') {
    return {
      initiatedBy: 'analyst',
      provider: 'LOCAL_WORKSPACE',
      repository: 'empty-service',
      repositoryUrl: 'local://empty-service',
      defaultBranch: 'main',
    }
  }
  return { initiatedBy: 'analyst' }
}
