import type { RiskDistribution } from '@/types/api'

export function RiskBars({ distribution }: { distribution: RiskDistribution }) {
  const rows = [
    { label: 'Critical', value: distribution.critical, color: '#ff5c7a' },
    { label: 'High', value: distribution.high, color: '#f5b942' },
    { label: 'Medium', value: distribution.medium, color: '#c9a227' },
    { label: 'Low', value: distribution.low, color: '#3ee0c5' },
  ]
  const max = Math.max(1, ...rows.map((row) => row.value))
  return (
    <div className="space-y-3" role="img" aria-label="Risk distribution">
      {rows.map((row) => (
        <div key={row.label}>
          <div className="mb-1 flex justify-between text-xs text-slate-400">
            <span>{row.label}</span>
            <span>{row.value}</span>
          </div>
          <div className="h-2 rounded-full bg-[#122038]">
            <div
              className="h-2 rounded-full"
              style={{ width: `${(row.value / max) * 100}%`, background: row.color }}
            />
          </div>
        </div>
      ))}
    </div>
  )
}
