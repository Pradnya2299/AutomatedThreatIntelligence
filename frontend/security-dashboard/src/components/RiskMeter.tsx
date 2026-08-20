export function RiskMeter({ score, level }: { score?: number | null; level?: string | null }) {
  const value = score ?? 0
  return (
    <div>
      <div className="flex items-end justify-between">
        <p className="text-3xl font-semibold text-white">{score == null ? '—' : Number(score).toFixed(2)}</p>
        <p className="text-sm uppercase tracking-wide text-slate-400">{level ?? 'unscored'}</p>
      </div>
      <div className="mt-3 h-2 rounded-full bg-[#122038]">
        <div
          className="h-2 rounded-full bg-danger"
          style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
        />
      </div>
    </div>
  )
}
