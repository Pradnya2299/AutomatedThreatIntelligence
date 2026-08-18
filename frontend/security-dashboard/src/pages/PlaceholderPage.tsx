import { Card, CardTitle } from '@/components/ui/card'

export function PlaceholderPage({ title, detail }: { title: string; detail: string }) {
  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-white">{title}</h1>
      <Card>
        <CardTitle>Phase 1 skeleton</CardTitle>
        <p className="text-sm text-slate-300">{detail}</p>
        <p className="mt-3 text-xs text-slate-500">
          Live data, risk scores, and AI recommendations land in later phases. This page is routed and styled for the SOC
          console.
        </p>
      </Card>
    </div>
  )
}
