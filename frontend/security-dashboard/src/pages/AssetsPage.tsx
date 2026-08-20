import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { Pager } from '@/components/Pager'
import { PageSkeleton } from '@/components/Skeleton'
import { getAssets } from '@/services/api'
import { apiErrorMessage } from '@/services/api/client'

export function AssetsPage() {
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? '0')
  const query = useQuery({
    queryKey: ['assets', page],
    queryFn: () => getAssets({ page, size: 20 }),
  })
  return (
    <div>
      <h1 className="mb-4 text-2xl font-semibold text-white">Assets</h1>
      {query.isLoading && <PageSkeleton />}
      {query.isError && <ErrorBanner message={apiErrorMessage(query.error, 'Unable to load assets. Please try again.')} onRetry={() => void query.refetch()} />}
      {query.data?.content.length === 0 && <EmptyState title="No assets found." />}
      {query.data && query.data.content.length > 0 && (
        <>
          <DataTable headers={['Asset', 'Hostname', 'Environment', 'OS', 'Criticality', 'Internet exposure', 'Installed software', 'Open findings']}>
            {query.data.content.map((row) => (
              <RowLink key={row.id} to={`/assets/${row.id}`}>
                <Td className="text-white">{row.hostname}</Td>
                <Td>{row.hostname}</Td>
                <Td>{row.environment}</Td>
                <Td>{row.operatingSystem}</Td>
                <Td>
                  <Badge value={row.criticality} />
                </Td>
                <Td>
                  <Badge value={row.internetExposure ? 'INTERNET' : 'INTERNAL'} />
                </Td>
                <Td className="max-w-xs truncate">{row.installedSoftware ?? '—'}</Td>
                <Td>{row.openFindings}</Td>
              </RowLink>
            ))}
          </DataTable>
          <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={(next) => setParams({ page: String(next) })} />
        </>
      )}
    </div>
  )
}
