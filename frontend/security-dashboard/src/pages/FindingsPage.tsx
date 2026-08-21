import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { Pager } from '@/components/Pager'
import { PageSkeleton } from '@/components/Skeleton'
import { getFindings } from '@/services/api'
import { apiErrorMessage } from '@/services/api/client'
import { formatScore, formatWhen } from '@/utils/format'

export function FindingsPage() {
  const [params, setParams] = useSearchParams()
  const risk = params.get('risk') ?? ''
  const status = params.get('status') ?? ''
  const asset = params.get('asset') ?? ''
  const cve = params.get('cve') ?? ''
  const page = Number(params.get('page') ?? '0')
  const query = useQuery({
    queryKey: ['findings', risk, status, asset, cve, page],
    queryFn: () => getFindings({ risk, status, asset, cve, page, size: 20 }),
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
      <h1 className="mb-4 text-2xl font-semibold text-white">Findings</h1>
      <div className="mb-4 flex flex-wrap gap-2">
        <input aria-label="Filter by CVE" placeholder="CVE" className="rounded-md border border-border bg-[#0b1422] px-3 py-1.5 text-sm" defaultValue={cve} onKeyDown={(e) => e.key === 'Enter' && update({ cve: (e.target as HTMLInputElement).value })} />
        <input aria-label="Filter by asset" placeholder="Asset" className="rounded-md border border-border bg-[#0b1422] px-3 py-1.5 text-sm" defaultValue={asset} onKeyDown={(e) => e.key === 'Enter' && update({ asset: (e.target as HTMLInputElement).value })} />
        <select aria-label="Risk" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={risk} onChange={(e) => update({ risk: e.target.value })}>
          <option value="">All risk</option>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((v) => <option key={v}>{v}</option>)}
        </select>
        <select aria-label="Status" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={status} onChange={(e) => update({ status: e.target.value })}>
          <option value="">All status</option>
          {['OPEN', 'CLOSED'].map((v) => <option key={v}>{v}</option>)}
        </select>
      </div>
      {query.isLoading && <PageSkeleton />}
      {query.isError && <ErrorBanner message={apiErrorMessage(query.error, 'Unable to load findings. Please try again.')} onRetry={() => void query.refetch()} />}
      {query.data?.content.length === 0 && (
        <EmptyState
          title="No findings found."
          detail="Seed demo_seed.sql (catalog rows) or run correlation for a CVE such as CVE-2021-44228."
        />
      )}
      {query.data && query.data.content.length > 0 && (
        <>
          <DataTable headers={['Asset', 'CVE', 'Match type', 'Confidence', 'Risk', 'Status', 'Detected']}>
            {query.data.content.map((row) => (
              <RowLink key={row.id} to={`/findings/${row.id}`}>
                <Td className="text-white">{row.hostname}</Td>
                <Td>{row.cveId}</Td>
                <Td>{row.matchType ?? '—'}</Td>
                <Td>{row.matchConfidence ?? '—'}</Td>
                <Td>
                  {formatScore(row.riskScore)} <Badge value={row.riskLevel} />
                </Td>
                <Td>
                  <Badge value={row.status} />
                </Td>
                <Td>{formatWhen(row.detectedAt)}</Td>
              </RowLink>
            ))}
          </DataTable>
          <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={(next) => update({ page: String(next) })} />
        </>
      )}
    </div>
  )
}
