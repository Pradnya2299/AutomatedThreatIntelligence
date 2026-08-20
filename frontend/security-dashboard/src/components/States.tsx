export function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="rounded-lg border border-[#5c1d2a] bg-[#2a1016] p-4 text-sm text-[#ffb3c0]">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="mt-2 text-accent underline" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}

export function EmptyState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="rounded-lg border border-dashed border-border p-8 text-center">
      <p className="text-sm font-medium text-white">{title}</p>
      {detail && <p className="mt-1 text-sm text-slate-400">{detail}</p>}
    </div>
  )
}
