import { Link } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { PageSkeleton } from '@/components/Skeleton'
import { RiskBars } from '@/components/RiskBars'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { useDashboardSummary } from '@/hooks/useDashboard'
import { formatScore, formatWhen } from '@/utils/format'

export function DashboardPage() {
  const queryClient = useQueryClient()
  const summary = useDashboardSummary(30_000)

  if (summary.isLoading) {
    return <PageSkeleton />
  }
  if (summary.isError) {
    return (
      <ErrorBanner
        message="Unable to load dashboard data. Please try again."
        onRetry={() => void summary.refetch()}
      />
    )
  }
  const data = summary.data
  if (!data) {
    return <EmptyState title="No dashboard data." />
  }

  const kpis = [
    { label: 'Critical vulnerabilities', value: data.criticalVulnerabilities, to: '/vulnerabilities?severity=CRITICAL' },
    { label: 'High vulnerabilities', value: data.highVulnerabilities, to: '/vulnerabilities?severity=HIGH' },
    { label: 'Affected assets', value: data.affectedAssets, to: '/assets' },
    { label: 'Critical findings', value: data.criticalFindings, to: '/findings?risk=CRITICAL' },
    { label: 'Pending remediations', value: data.pendingRemediations, to: '/remediation' },
    { label: 'AI remediation plans', value: data.aiRemediationPlans, to: '/remediation?status=GENERATED' },
  ]

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-white">Operations dashboard</h1>
          <p className="mt-1 text-sm text-slate-400">Live inventory, findings, risk, and AI plans from api-service.</p>
        </div>
        <Button type="button" onClick={() => void queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] })}>
          Refresh
        </Button>
      </div>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {kpis.map((kpi) => (
          <Link key={kpi.label} to={kpi.to} className="block">
            <Card className="h-full hover:border-accent">
              <CardTitle>{kpi.label}</CardTitle>
              <p className="text-3xl font-semibold text-white">{kpi.value}</p>
            </Card>
          </Link>
        ))}
      </div>
      <div className="mt-6 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardTitle>Top priority vulnerabilities</CardTitle>
          {data.topVulnerabilities.length === 0 ? (
            <EmptyState title="No correlated vulnerabilities yet." detail="Run correlation and risk engines, then refresh." />
          ) : (
            <DataTable headers={['CVE', 'Severity', 'CVSS', 'Affected assets', 'Risk', 'Status', 'AI recommendation']}>
              {data.topVulnerabilities.map((row) => (
                <RowLink key={row.cveId} to={`/vulnerabilities/${row.cveId}`}>
                  <Td className="font-medium text-white">{row.cveId}</Td>
                  <Td>
                    <Badge value={row.severity} />
                  </Td>
                  <Td>{formatScore(row.cvss)}</Td>
                  <Td>{row.affectedAssets}</Td>
                  <Td>
                    <span className="mr-2">{formatScore(row.riskScore)}</span>
                    <Badge value={row.riskLevel} />
                  </Td>
                  <Td>{row.status}</Td>
                  <Td>{row.aiRecommendation}</Td>
                </RowLink>
              ))}
            </DataTable>
          )}
        </Card>
        <Card>
          <CardTitle>Risk distribution</CardTitle>
          <RiskBars distribution={data.riskDistribution} />
        </Card>
      </div>
      <Card className="mt-6">
        <CardTitle>Recent activity</CardTitle>
        {data.recentActivity.length === 0 ? (
          <EmptyState title="No activity yet." />
        ) : (
          <ul className="space-y-3">
            {data.recentActivity.map((item, index) => (
              <li key={`${item.type}-${item.occurredAt}-${index}`} className="flex justify-between gap-4 text-sm">
                <span className="text-slate-200">{item.message}</span>
                <span className="shrink-0 text-slate-500">{formatWhen(item.occurredAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
