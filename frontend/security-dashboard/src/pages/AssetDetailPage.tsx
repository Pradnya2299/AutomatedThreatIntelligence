import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Badge } from '@/components/Badge'
import { DataTable, RowLink, Td } from '@/components/DataTable'
import { EmptyState, ErrorBanner } from '@/components/States'
import { PageSkeleton } from '@/components/Skeleton'
import { Card, CardTitle } from '@/components/ui/card'
import { getAsset } from '@/services/api'
import { formatScore, formatWhen } from '@/utils/format'

export function AssetDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['asset', id],
    queryFn: () => getAsset(id),
    enabled: Boolean(id),
  })
  if (query.isLoading) {
    return <PageSkeleton />
  }
  if (query.isError) {
    return <ErrorBanner message="Unable to load asset data. Please try again." onRetry={() => void query.refetch()} />
  }
  const asset = query.data
  if (!asset) {
    return <EmptyState title="Asset not found." />
  }
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">{asset.hostname}</h1>
        <p className="text-sm text-slate-400">
          {asset.environment} · {asset.operatingSystem} {asset.osVersion}
        </p>
        <div className="mt-2 flex gap-2">
          <Badge value={asset.criticality} />
          <Badge value={asset.internetExposure ? 'INTERNET' : 'INTERNAL'} />
          <Badge value={asset.status} />
        </div>
      </div>
      <Card>
        <CardTitle>Installed software</CardTitle>
        {asset.installedSoftware.length === 0 ? (
          <EmptyState title="No installed software recorded." />
        ) : (
          <ul className="text-sm">
            {asset.installedSoftware.map((row) => (
              <li key={`${row.vendor}-${row.product}-${row.version}`}>
                {row.vendor} {row.product} {row.version}
              </li>
            ))}
          </ul>
        )}
      </Card>
      <Card>
        <CardTitle>Findings</CardTitle>
        {asset.findings.length === 0 ? (
          <EmptyState title="No findings on this asset." />
        ) : (
          <DataTable headers={['CVE', 'Risk', 'Status', 'Detected']}>
            {asset.findings.map((row) => (
              <RowLink key={row.id} to={`/findings/${row.id}`}>
                <Td>{row.cveId}</Td>
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
        )}
      </Card>
      <Link to="/assets" className="text-sm text-accent underline">
        Back to assets
      </Link>
    </div>
  )
}
