import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { Pager } from '@/components/Pager'
import { PageSkeleton } from '@/components/Skeleton'
import { getRemediationPlans } from '@/services/api'
import { formatScore, formatWhen } from '@/utils/format'

export function RemediationPage() {
  const [params, setParams] = useSearchParams()
  const priority = params.get('priority') ?? ''
  const status = params.get('status') ?? ''
  const risk = params.get('risk') ?? ''
  const page = Number(params.get('page') ?? '0')
  const query = useQuery({
    queryKey: ['remediation-list', priority, status, risk, page],
    queryFn: () => getRemediationPlans({ priority, status, risk, page, size: 20 }),
  })
  const update = (patch: Record<string, string>) => {
    const next = new URLSearchParams(params)
    Object.entries(patch).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    if (!('page' in patch)) {
      next.delete('page')
    }
    setParams(next)
  }
  return (
    <div>
      <h1 className="mb-4 text-2xl font-semibold text-white">Remediation</h1>
      <div className="mb-4 flex flex-wrap gap-2">
        <select aria-label="Priority" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={priority} onChange={(e) => update({ priority: e.target.value })}>
          <option value="">All priority</option>
          {['IMMEDIATE', 'URGENT', 'SCHEDULED'].map((v) => <option key={v}>{v}</option>)}
        </select>
        <select aria-label="Status" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={status} onChange={(e) => update({ status: e.target.value })}>
          <option value="">All status</option>
          {['GENERATED', 'REVIEW_REQUIRED', 'FAILED'].map((v) => <option key={v}>{v}</option>)}
        </select>
        <select aria-label="Risk" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={risk} onChange={(e) => update({ risk: e.target.value })}>
          <option value="">All risk</option>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((v) => <option key={v}>{v}</option>)}
        </select>
      </div>
      {query.isLoading && <PageSkeleton />}
      {query.isError && <ErrorBanner message="Unable to load remediation plans. Please try again." onRetry={() => void query.refetch()} />}
      {query.data?.content.length === 0 && <EmptyState title="No AI remediation plans." />}
      {query.data && query.data.content.length > 0 && (
        <>
          <DataTable headers={['CVE', 'Asset', 'Risk', 'Priority', 'AI status', 'Created', 'Action']}>
            {query.data.content.map((row) => (
              <RowLink key={row.id} to={`/remediation/${row.id}`}>
                <Td className="text-white">{row.cveId}</Td>
                <Td>{row.hostname}</Td>
                <Td>
                  {formatScore(row.riskScore)} <Badge value={row.riskLevel} />
                </Td>
                <Td>
                  <Badge value={row.priority} />
                </Td>
                <Td>
                  <Badge value={row.status} />
                </Td>
                <Td>{formatWhen(row.createdAt)}</Td>
                <Td>View</Td>
              </RowLink>
            ))}
          </DataTable>
          <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={(next) => update({ page: String(next) })} />
        </>
      )}
    </div>
  )
}
