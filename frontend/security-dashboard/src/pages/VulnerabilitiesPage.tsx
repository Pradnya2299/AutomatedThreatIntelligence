import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { Pager } from '@/components/Pager'
import { PageSkeleton } from '@/components/Skeleton'
import { getVulnerabilities } from '@/services/api'
import { formatScore, formatWhen } from '@/utils/format'

export function VulnerabilitiesPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const severity = params.get('severity') ?? ''
  const risk = params.get('risk') ?? ''
  const page = Number(params.get('page') ?? '0')
  const query = useQuery({
    queryKey: ['vulnerabilities', q, severity, risk, page],
    queryFn: () => getVulnerabilities({ q, severity, risk, page, size: 20 }),
  })

  const update = (patch: Record<string, string>) => {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(patch)) {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    }
    if (!('page' in patch)) {
      next.delete('page')
    }
    setParams(next)
  }

  const filters = useMemo(
    () => (
      <div className="mb-4 flex flex-wrap gap-2">
        <input
          aria-label="Search CVE, vendor, or product"
          className="rounded-md border border-border bg-[#0b1422] px-3 py-1.5 text-sm"
          placeholder="Search CVE / vendor / product"
          defaultValue={q}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              update({ q: (event.target as HTMLInputElement).value })
            }
          }}
        />
        <select aria-label="Severity" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={severity} onChange={(e) => update({ severity: e.target.value })}>
          <option value="">All severities</option>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <select aria-label="Risk" className="rounded-md border border-border bg-[#0b1422] px-2 py-1.5 text-sm" value={risk} onChange={(e) => update({ risk: e.target.value })}>
          <option value="">All risk levels</option>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
      </div>
    ),
    [q, severity, risk],
  )

  return (
    <div>
      <h1 className="mb-2 text-2xl font-semibold text-white">Vulnerabilities</h1>
      <p className="mb-4 text-sm text-slate-400">CVE inventory with affected-asset and risk rollups.</p>
      {filters}
      {query.isLoading && <PageSkeleton />}
      {query.isError && (
        <ErrorBanner message="Unable to load vulnerability data. Please try again." onRetry={() => void query.refetch()} />
      )}
      {query.data && query.data.content.length === 0 && <EmptyState title="No vulnerabilities found." />}
      {query.data && query.data.content.length > 0 && (
        <>
          <DataTable headers={['CVE', 'Description', 'Severity', 'CVSS', 'Affected assets', 'Risk', 'Published', 'Status']}>
            {query.data.content.map((row) => (
              <RowLink key={row.id} to={`/vulnerabilities/${row.cveId}`}>
                <Td className="font-medium text-white">{row.cveId}</Td>
                <Td className="max-w-xs truncate">{row.description}</Td>
                <Td>
                  <Badge value={row.severity} />
                </Td>
                <Td>{formatScore(row.cvss)}</Td>
                <Td>{row.affectedAssets}</Td>
                <Td>
                  {formatScore(row.riskScore)} <Badge value={row.riskLevel} />
                </Td>
                <Td>{formatWhen(row.publishedAt)}</Td>
                <Td>{row.status}</Td>
              </RowLink>
            ))}
          </DataTable>
          <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={(next) => update({ page: String(next) })} />
        </>
      )}
    </div>
  )
}
