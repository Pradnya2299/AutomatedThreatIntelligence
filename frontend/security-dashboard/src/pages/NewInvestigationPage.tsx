import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { ErrorState } from '@/components/LoadingState'
import { createInvestigation } from '@/services/api/investigations'
import { ApiError } from '@/services/api/client'
import { rememberInvestigation } from '@/utils/recentInvestigations'

export function NewInvestigationPage() {
  const navigate = useNavigate()
  const [cveId, setCveId] = useState('CVE-2021-44228')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const result = await createInvestigation(cveId.trim())
      rememberInvestigation({
        investigationId: result.investigationId,
        cveId: result.cveId,
        status: result.status,
        openedAt: new Date().toISOString(),
      })
      navigate(`/investigations/${result.investigationId}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to start investigation.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <h1 className="text-2xl font-semibold text-white">Start investigation</h1>
      <Card>
        <CardTitle>CVE ID</CardTitle>
        <form onSubmit={onSubmit} className="space-y-3">
          <input
            value={cveId}
            onChange={(e) => setCveId(e.target.value)}
            className="w-full rounded border border-border bg-[#0b1422] px-3 py-2 font-mono text-white"
            placeholder="CVE-2021-44228"
            required
          />
          <p className="text-xs text-slate-500">
            Calls POST /api/v1/investigations on api-service. Uses seeded CVE data when present. No fake investigation
            is created in the browser.
          </p>
          {error && <ErrorState message={error} onRetry={() => setError(null)} />}
          <Button type="submit" disabled={busy}>
            {busy ? 'Starting investigation...' : 'Start investigation'}
          </Button>
        </form>
      </Card>
    </div>
  )
}
