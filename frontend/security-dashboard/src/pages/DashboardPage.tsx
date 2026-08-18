import { useQuery } from '@tanstack/react-query'
import { Card, CardTitle } from '@/components/ui/card'

type Health = { status: string; service: string; timestamp: string }

export function DashboardPage() {
  const health = useQuery({
    queryKey: ['api-health'],
    queryFn: async (): Promise<Health> => {
      const res = await fetch('/api/health')
      if (!res.ok) {
        throw new Error('api-service unreachable')
      }
      return res.json() as Promise<Health>
    },
    retry: false,
  })

  const tiles = [
    { label: 'Critical vulnerabilities', value: '—' },
    { label: 'High vulnerabilities', value: '—' },
    { label: 'Affected assets', value: '—' },
    { label: 'Internet-facing vulnerable', value: '—' },
    { label: 'Actively exploited', value: '—' },
    { label: 'Remediation pending approval', value: '—' },
  ]

  return (
    <div>
      <h1 className="mb-2 text-2xl font-semibold text-white">Operations dashboard</h1>
      <p className="mb-6 text-sm text-slate-400">
        API health:{' '}
        {health.isLoading && 'checking…'}
        {health.isError && 'unavailable (start api-service)'}
        {health.data && `${health.data.status} · ${health.data.service}`}
      </p>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {tiles.map((tile) => (
          <Card key={tile.label}>
            <CardTitle>{tile.label}</CardTitle>
            <p className="text-3xl font-semibold text-white">{tile.value}</p>
          </Card>
        ))}
      </div>
      <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
        <Card>
          <CardTitle>Risk trends</CardTitle>
          <p className="text-sm text-slate-400">Placeholder. Bound to risk_assessments in Phase 3.</p>
        </Card>
        <Card>
          <CardTitle>Top vulnerabilities</CardTitle>
          <p className="text-sm text-slate-400">Placeholder. Bound after correlation + risk engines exist.</p>
        </Card>
      </div>
    </div>
  )
}
