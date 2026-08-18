import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AiRemediationCard } from '@/components/AiRemediationCard'
import { EmptyState, ErrorBanner } from '@/components/States'
import { PageSkeleton } from '@/components/Skeleton'
import { getRemediationPlan } from '@/services/api'

export function RemediationDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['remediation', id],
    queryFn: () => getRemediationPlan(id),
    enabled: Boolean(id),
  })
  if (query.isLoading) {
    return <PageSkeleton />
  }
  if (query.isError) {
    return <ErrorBanner message="Unable to load the remediation plan. Please try again." onRetry={() => void query.refetch()} />
  }
  if (!query.data) {
    return <EmptyState title="No AI remediation plan has been generated." />
  }
  return (
    <div>
      <h1 className="mb-4 text-2xl font-semibold text-white">Remediation plan</h1>
      <AiRemediationCard plan={query.data} />
    </div>
  )
}
