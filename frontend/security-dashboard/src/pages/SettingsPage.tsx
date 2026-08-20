import { useQuery } from '@tanstack/react-query'
import { Card, CardTitle } from '@/components/ui/card'
import { ErrorBanner } from '@/components/States'
import { apiGet } from '@/services/api/client'

type Me = { username: string; authorities: string[] }

export function SettingsPage() {
  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => apiGet<Me>('/api/me'),
    retry: false,
  })
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold text-white">Settings</h1>
      <Card>
        <CardTitle>Local authentication</CardTitle>
        <p className="text-sm text-slate-300">
          The dashboard talks only to api-service. Vite proxies <code>/api</code> and attaches HTTP Basic for local
          development (default analyst / analyst_change_me). Production identity is unchanged.
        </p>
        {me.isError && <div className="mt-3"><ErrorBanner message="Unable to load the current API user. Start api-service." /></div>}
        {me.data && (
          <p className="mt-3 text-sm text-white">
            Signed in as {me.data.username} ({me.data.authorities.join(', ')})
          </p>
        )}
      </Card>
      <Card>
        <CardTitle>Demo AI</CardTitle>
        <p className="text-sm text-slate-300">
          When OPENAI_API_KEY is set, Start code remediation shows LLM POWERED. An empty key uses DEMO MODE
          (structured fixture patches, not OpenAI). Restart ai-service after changing .env.
        </p>
      </Card>
    </div>
  )
}
