import { Card, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import { formatScoreOrUnavailable } from '@/utils/format'
import { factorScore, notAvailable } from '@/utils/investigationDisplay'
import type { Investigation } from '@/types/investigation'

export function RiskCard({ investigation }: { investigation: Investigation }) {
  const risk = investigation.riskAnalysis
  const threat = investigation.threatIntelligence
  const primary = investigation.assetInvestigation?.assets?.find(
    (a) => a.findingId === risk?.primaryFindingId,
  ) ?? investigation.assetInvestigation?.assets?.[0]
  const internet = primary
    ? primary.internetExposure
      ? 'INTERNET-FACING'
      : 'INTERNAL'
    : 'Not available'

  return (
    <Card className="border-accent/30">
      <CardTitle>Organizational risk (deterministic engine)</CardTitle>
      <p className="mb-4 text-xs text-slate-500">
        Score and factors come from risk-service. The UI does not recalculate risk.
      </p>
      <div className="flex flex-wrap items-end gap-8">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Risk score</p>
          <p className="font-mono text-5xl font-semibold text-white">{formatScoreOrUnavailable(risk?.riskScore)}</p>
        </div>
        <div className="space-y-1">
          <Badge value={risk?.riskLevel} />
          <p className="text-sm text-slate-400">{risk?.explanation ?? 'Not available'}</p>
        </div>
      </div>
      <dl className="mt-6 grid grid-cols-2 gap-3 text-sm md:grid-cols-3 xl:grid-cols-6">
        <div>
          <dt className="text-slate-500">Severity</dt>
          <dd><Badge value={threat?.severity} /></dd>
        </div>
        <div>
          <dt className="text-slate-500">CVSS</dt>
          <dd className="font-mono text-white">{formatScoreOrUnavailable(threat?.cvssScore)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Exploitability</dt>
          <dd className="text-white">{notAvailable(threat?.exploitability)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Asset criticality</dt>
          <dd className="text-white">
            {primary?.businessCriticality ?? factorScore(risk?.factors, 'assetCriticality')}
          </dd>
        </div>
        <div>
          <dt className="text-slate-500">Internet exposure</dt>
          <dd><Badge value={internet === 'Not available' ? null : internet} /></dd>
        </div>
        <div>
          <dt className="text-slate-500">Active exploitation</dt>
          <dd className="text-white">
            {threat?.activelyExploited === null || threat?.activelyExploited === undefined
              ? 'Not available'
              : threat.activelyExploited
                ? 'Yes'
                : 'No'}
          </dd>
        </div>
      </dl>
    </Card>
  )
}
