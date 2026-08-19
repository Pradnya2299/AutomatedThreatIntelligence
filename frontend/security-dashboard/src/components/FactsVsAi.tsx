import { Card, CardTitle } from '@/components/ui/card'

export function FactsVsAi() {
  return (
    <Card>
      <CardTitle>Facts vs recommendation</CardTitle>
      <div className="grid grid-cols-1 gap-3 text-center text-xs md:grid-cols-5">
        <div className="rounded border border-border p-3">
          <p className="font-semibold text-white">CVE</p>
          <p className="mt-1 text-slate-500">Database lookup</p>
        </div>
        <div className="rounded border border-border p-3">
          <p className="font-semibold text-white">Assets</p>
          <p className="mt-1 text-slate-500">Correlation engine</p>
        </div>
        <div className="rounded border border-border p-3">
          <p className="font-semibold text-white">Risk</p>
          <p className="mt-1 text-slate-500">Deterministic formula v1</p>
        </div>
        <div className="rounded border border-accent/40 p-3">
          <p className="font-semibold text-accent">AI agents</p>
          <p className="mt-1 text-slate-500">Orchestrate investigation</p>
        </div>
        <div className="rounded border border-border p-3">
          <p className="font-semibold text-white">Recommendation</p>
          <p className="mt-1 text-slate-500">RAG / demo plan</p>
        </div>
      </div>
    </Card>
  )
}
