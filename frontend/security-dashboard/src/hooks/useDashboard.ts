import { useQuery } from '@tanstack/react-query'
import { getDashboardSummary } from '@/services/api'

export function useDashboardSummary(refetchInterval?: number) {
  return useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: getDashboardSummary,
    refetchInterval,
  })
}
