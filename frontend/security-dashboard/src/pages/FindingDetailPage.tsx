import { useParams } from 'react-router-dom'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function FindingDetailPage() {
  const { id } = useParams()
  return (
    <PlaceholderPage
      title={`Finding ${id}`}
      detail="Why the asset matched, deterministic risk breakdown, and linked remediation plan."
    />
  )
}
