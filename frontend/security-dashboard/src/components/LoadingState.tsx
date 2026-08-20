import { apiOrigin } from '@/services/api/client'
import { ErrorBanner } from '@/components/States'
import { PageSkeleton } from '@/components/Skeleton'

export function LoadingState({ label = 'Loading investigation...' }: { label?: string }) {
  return (
    <div>
      <p className="mb-3 text-sm text-slate-400">{label}</p>
      <PageSkeleton />
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <ErrorBanner
      message={`${message} Backend: ${apiOrigin()}`}
      onRetry={onRetry}
    />
  )
}
