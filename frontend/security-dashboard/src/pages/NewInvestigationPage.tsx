import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { ErrorState } from '@/components/LoadingState'
import { createInvestigation } from '@/services/api/investigations'
import { ApiError } from '@/services/api/client'
import { rememberInvestigation } from '@/utils/recentInvestigations'
import { DEMO_CVES } from '@/data/demoCves'

export function NewInvestigationPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [cveId, setCveId] = useState(params.get('cve') || 'CVE-2021-44228')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const selected = useMemo(() => DEMO_CVES.find((row) => row.id === cveId), [cveId])

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
        <CardTitle>Pick a seeded CVE</CardTitle>
        <div className="mb-3 grid grid-cols-1 gap-2">
          {DEMO_CVES.map((row) => (
            <button
              key={row.id}
              type="button"
              onClick={() => setCveId(row.id)}
              className={`rounded-md border px-3 py-2 text-left text-sm ${
                cveId === row.id ? 'border-accent bg-[#122038] text-white' : 'border-border text-slate-300'
              }`}
            >
              <span className="font-mono">{row.id}</span>
              <span className="mt-0.5 block text-xs text-slate-400">{row.title}</span>
            </button>
          ))}
        </div>
        <form onSubmit={onSubmit} className="space-y-3">
          <input
            value={cveId}
            onChange={(e) => setCveId(e.target.value)}
            className="w-full rounded border border-border bg-[#0b1422] px-3 py-2 font-mono text-white"
            placeholder="CVE-2021-44228"
            required
          />
          <p className="text-xs text-slate-500">
            {selected?.detail ||
              'Calls POST /api/v1/investigations. Uses seeded or NVD data. The browser does not invent CVEs.'}
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
