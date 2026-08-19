import { DataTable, Td } from '@/components/DataTable'
import { Badge } from '@/components/Badge'
import { Card, CardTitle } from '@/components/ui/card'
import { versionFromReason } from '@/utils/investigationDisplay'
import type { Investigation } from '@/types/investigation'

export function AssetTable({ investigation }: { investigation: Investigation }) {
  const assets = investigation.assetInvestigation?.assets ?? []
  return (
    <Card>
      <CardTitle>Affected assets</CardTitle>
      <p className="mb-3 text-xs text-slate-500">
        Hosts come from the correlation engine. Version is shown only when it already appears in the match reason.
        CPE is not a field on the investigation asset DTO.
      </p>
      {assets.length === 0 ? (
        <p className="text-sm text-slate-400">Not available</p>
      ) : (
        <DataTable headers={['Asset', 'Environment', 'Version', 'CPE', 'Match type', 'Criticality', 'Internet exposure']}>
          {assets.map((asset, index) => (
            <tr key={asset.findingId ?? asset.assetId ?? `${asset.hostname}-${index}`}>
              <Td className="font-medium text-white">{asset.hostname ?? 'Not available'}</Td>
              <Td>{asset.environment ?? 'Not available'}</Td>
              <Td className="font-mono">{versionFromReason(asset.matchReason)}</Td>
              <Td>Not available</Td>
              <Td>{asset.matchType ?? 'Not available'}</Td>
              <Td><Badge value={asset.businessCriticality} /></Td>
              <Td>
                <Badge value={asset.internetExposure ? 'INTERNET-FACING' : 'INTERNAL'} />
              </Td>
            </tr>
          ))}
        </DataTable>
      )}
    </Card>
  )
}
