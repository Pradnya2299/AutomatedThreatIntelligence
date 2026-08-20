import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import { readRecentInvestigations } from '@/utils/recentInvestigations'
import { formatWhen } from '@/utils/format'
import { DEMO_CVES } from '@/data/demoCves'

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export function InvestigationListPage() {
  const navigate = useNavigate()
  const [lookup, setLookup] = useState('')
  const [error, setError] = useState<string | null>(null)
  const recent = useMemo(() => readRecentInvestigations(), [])

  const onLookup = (event: FormEvent) => {
    event.preventDefault()
    const id = lookup.trim()
    if (!UUID_RE.test(id)) {
      setError('Enter a valid investigation UUID from a previous POST/GET.')
      return
    }
    navigate(`/investigations/${id}`)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-white">Investigations</h1>
          <p className="mt-1 text-sm text-slate-400">
            Multi-agent CVE investigation. The backend has no list endpoint; open by ID or start a new run.
          </p>
        </div>
        <Link to="/investigations/new">
          <Button type="button">Start investigation</Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {DEMO_CVES.map((row) => (
          <Card key={row.id}>
            <CardTitle>{row.title}</CardTitle>
            <p className="font-mono text-lg text-white">{row.id}</p>
            <p className="mt-2 text-sm text-slate-400">{row.detail}</p>
            <Link to={`/investigations/new?cve=${encodeURIComponent(row.id)}`} className="mt-3 inline-block">
              <Button type="button">Investigate</Button>
            </Link>
          </Card>
        ))}
        <Card>
          <CardTitle>Open by ID</CardTitle>
          <form onSubmit={onLookup} className="flex flex-col gap-2">
            <label className="text-xs text-slate-500" htmlFor="investigation-id">
              Investigation ID
            </label>
            <input
              id="investigation-id"
              value={lookup}
              onChange={(e) => setLookup(e.target.value)}
              className="rounded border border-border bg-[#0b1422] px-3 py-2 font-mono text-sm text-white"
              placeholder="2daf8436-684a-4519-8e7e-29dc60b15a83"
            />
            {error && <p className="text-xs text-[#ff8da0]">{error}</p>}
            <Button type="submit">Open</Button>
          </form>
        </Card>
      </div>

      <Card>
        <CardTitle>Opened in this browser session</CardTitle>
        {recent.length === 0 ? (
          <p className="text-sm text-slate-400">
            No investigations opened in this session. Start one or paste an ID. This is not a server-side catalog.
          </p>
        ) : (
          <ul className="space-y-2">
            {recent.map((row) => (
              <li key={row.investigationId}>
                <Link
                  to={`/investigations/${row.investigationId}`}
                  className="flex flex-wrap items-center justify-between gap-2 rounded border border-border px-3 py-2 hover:border-accent"
                >
                  <span className="font-mono text-sm text-white">{row.cveId}</span>
                  <Badge value={row.status} />
                  <span className="font-mono text-xs text-slate-500">{row.investigationId}</span>
                  <span className="text-xs text-slate-500">{formatWhen(row.openedAt)}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
