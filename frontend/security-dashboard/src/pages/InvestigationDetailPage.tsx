import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { InvestigationHeader } from '@/components/InvestigationHeader'
import { RiskCard } from '@/components/RiskCard'
import { AgentPipeline } from '@/components/AgentPipeline'
import { DecisionTimeline } from '@/components/DecisionTimeline'
import { EvidencePanel } from '@/components/EvidencePanel'
import { AssetTable } from '@/components/AssetTable'
import { RemediationPanel } from '@/components/RemediationPanel'
import { HumanReviewBanner } from '@/components/HumanReviewBanner'
import { FactsVsAi } from '@/components/FactsVsAi'
import { ErrorState, LoadingState } from '@/components/LoadingState'
import { Card } from '@/components/ui/card'
import { getInvestigation } from '@/services/api/investigations'
import { ApiError } from '@/services/api/client'
import { isTerminalStatus } from '@/types/investigation'
import { rememberInvestigation } from '@/utils/recentInvestigations'

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export function InvestigationDetailPage() {
  const { investigationId = '' } = useParams()
  const valid = UUID_RE.test(investigationId)
  const query = useQuery({
    queryKey: ['investigation', investigationId],
    queryFn: () => getInvestigation(investigationId),
    enabled: valid,
    refetchInterval: (q) => {
      const status = q.state.data?.status
      return status && !isTerminalStatus(status) ? 2000 : false
    },
  })

  useEffect(() => {
    if (query.data) {
      rememberInvestigation({
        investigationId: query.data.investigationId,
        cveId: query.data.cveId,
        status: query.data.status,
        openedAt: new Date().toISOString(),
      })
    }
  }, [query.data])

  if (!valid) {
    return <ErrorState message="That investigation ID is not a valid UUID." />
  }
  if (query.isLoading) {
    return <LoadingState />
  }
  if (query.isError) {
    const message = query.error instanceof ApiError ? query.error.message : 'Unable to load investigation.'
    return <ErrorState message={message} onRetry={() => void query.refetch()} />
  }
  const investigation = query.data
  if (!investigation) {
    return <ErrorState message="Unable to load investigation." onRetry={() => void query.refetch()} />
  }

  const rec = investigation.recommendation
  const showRemediation = Boolean(investigation.remediation) && investigation.status !== 'REVIEW_REQUIRED'

  return (
    <div className="space-y-4">
      <InvestigationHeader investigation={investigation} />
      <HumanReviewBanner investigation={investigation} />
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-6">
        <SummaryTile label="Severity" value={investigation.threatIntelligence?.severity} />
        <SummaryTile label="CVSS" value={investigation.threatIntelligence?.cvssScore} />
        <SummaryTile label="Risk score" value={investigation.riskAnalysis?.riskScore ?? rec?.organizationalRiskScore} />
        <SummaryTile label="Affected assets" value={investigation.assetInvestigation?.affectedAssetCount} />
        <SummaryTile label="Confidence" value={investigation.confidence} />
        <SummaryTile label="Priority" value={rec?.priority} />
      </div>
      {rec?.summary && investigation.status === 'COMPLETED' && (
        <Card>
          <p className="text-xs uppercase tracking-wide text-slate-500">Recommendation</p>
          <p className="mt-1 text-sm text-white">{rec.summary}</p>
        </Card>
      )}
      <FactsVsAi />
      <RiskCard investigation={investigation} />
      <AgentPipeline investigation={investigation} />
      <DecisionTimeline investigation={investigation} />
      <EvidencePanel investigation={investigation} />
      <AssetTable investigation={investigation} />
      {showRemediation ? <RemediationPanel investigation={investigation} /> : null}
    </div>
  )
}

function SummaryTile({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <Card>
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 font-mono text-lg text-white">
        {value === null || value === undefined || value === '' ? 'Not available' : String(value)}
      </p>
    </Card>
  )
}
