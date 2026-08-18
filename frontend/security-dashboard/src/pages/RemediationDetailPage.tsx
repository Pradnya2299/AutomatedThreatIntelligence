import { useParams } from 'react-router-dom'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

export function RemediationDetailPage() {
  const { id } = useParams()
  return (
    <PlaceholderPage
      title={`Remediation plan ${id}`}
      detail="Structured recommendation, policy citations, approve/reject (SECURITY_MANAGER), simulation status."
    />
  )
}
