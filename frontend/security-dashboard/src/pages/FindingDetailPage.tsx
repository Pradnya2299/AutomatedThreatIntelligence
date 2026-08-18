import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AiRemediationCard } from '@/components/AiRemediationCard'
import { Badge } from '@/components/Badge'
import { EmptyState, ErrorBanner } from '@/components/States'
import { PageSkeleton } from '@/components/Skeleton'
import { RiskMeter } from '@/components/RiskMeter'
import { Card, CardTitle } from '@/components/ui/card'
import { getFinding } from '@/services/api'

export function FindingDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['finding', id],
    queryFn: () => getFinding(id),
    enabled: Boolean(id),
  })

  if (query.isLoading) {
    return <PageSkeleton />
  }
  if (query.isError) {
    return <ErrorBanner message="Unable to load finding data. Please try again." onRetry={() => void query.refetch()} />
  }
  const finding = query.data
  if (!finding) {
    return <EmptyState title="Finding not found." />
  }

  const factors = finding.risk
    ? [
        { label: 'CVSS', value: finding.risk.cvss },
        { label: 'Asset criticality', value: finding.risk.assetCriticality },
        { label: 'Internet exposure', value: finding.risk.internetExposure },
        { label: 'Exploitability', value: finding.risk.exploitability },
        { label: 'Active exploitation', value: finding.risk.activeExploitation },
      ]
    : []

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-accent">Finding</p>
        <h1 className="mt-1 text-2xl font-semibold text-white">{finding.cveId}</h1>
        <p className="text-sm text-slate-400">
          Asset <Link className="text-accent underline" to={`/assets/${finding.assetId}`}>{finding.hostname}</Link>
          {' · '}
          <Link className="text-accent underline" to={`/vulnerabilities/${finding.cveId}`}>{finding.cveId}</Link>
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardTitle>Match</CardTitle>
          <p className="text-white">{finding.matchType ?? '—'}</p>
          <p className="mt-2 text-xs uppercase text-slate-500">Confidence</p>
          <p>{finding.matchConfidence ?? '—'}</p>
        </Card>
        <Card>
          <CardTitle>Environment</CardTitle>
          <p>{finding.environment}</p>
          <p className="mt-2 text-sm text-slate-400">{finding.operatingSystem}</p>
          <Badge value={finding.status} />
        </Card>
        <Card>
          <CardTitle>Risk</CardTitle>
          {finding.risk ? <RiskMeter score={finding.risk.score} level={finding.risk.level} /> : <p>Not scored yet.</p>}
        </Card>
      </div>
      <Card>
        <CardTitle>Why is this affected?</CardTitle>
        <p className="text-sm leading-6 text-slate-200">
          {finding.matchExplanation || 'Match explanation is not available on this finding.'}
        </p>
      </Card>
      <Card>
        <CardTitle>Risk factors</CardTitle>
        {finding.risk ? (
          <>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
              {factors.map((factor) => (
                <div key={factor.label}>
                  <p className="text-xs uppercase text-slate-500">{factor.label}</p>
                  <p className="text-lg text-white">{factor.value ?? '—'}</p>
                </div>
              ))}
            </div>
            <p className="mt-4 text-sm text-slate-300">{finding.risk.explanation}</p>
          </>
        ) : (
          <EmptyState title="Risk has not been calculated." />
        )}
      </Card>
      {finding.remediation ? (
        <AiRemediationCard plan={finding.remediation} />
      ) : (
        <EmptyState title="No AI remediation plan has been generated." />
      )}
    </div>
  )
}
