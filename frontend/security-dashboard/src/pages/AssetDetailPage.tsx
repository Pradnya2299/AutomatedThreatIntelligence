import { useParams } from 'react-router-dom'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function AssetDetailPage() {
  const { id } = useParams()
  return (
    <PlaceholderPage
      title={`Asset ${id}`}
      detail="Installed software, findings, and business context for a single CMDB-style asset."
    />
  )
}
