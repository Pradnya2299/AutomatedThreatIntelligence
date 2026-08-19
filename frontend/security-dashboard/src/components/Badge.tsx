import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

const tones: Record<string, string> = {
  CRITICAL: 'bg-[#3a1018] text-[#ff8da0] border-[#5c1d2a]',
  HIGH: 'bg-[#3a2a10] text-[#f5c46b] border-[#5c4318]',
  MEDIUM: 'bg-[#2a2410] text-[#e6d27a] border-[#4a4018]',
  LOW: 'bg-[#102a22] text-[#7ee0c8] border-[#1d4a3c]',
  IMMEDIATE: 'bg-[#3a1018] text-[#ff8da0] border-[#5c1d2a]',
  URGENT: 'bg-[#3a2a10] text-[#f5c46b] border-[#5c4318]',
  SCHEDULED: 'bg-[#102033] text-[#8ec5ff] border-[#1d3a5c]',
  GENERATED: 'bg-[#102033] text-[#8ec5ff] border-[#1d3a5c]',
  OPEN: 'bg-[#102033] text-[#8ec5ff] border-[#1d3a5c]',
  FAILED: 'bg-[#3a1018] text-[#ff8da0] border-[#5c1d2a]',
  COMPLETED: 'bg-[#102a22] text-[#7ee0c8] border-[#1d4a3c]',
  REVIEW_REQUIRED: 'bg-[#3a2a10] text-[#f5c46b] border-[#5c4318]',
  RUNNING: 'bg-[#102033] text-[#8ec5ff] border-[#1d3a5c]',
  SKIPPED: 'bg-[#1a1f2a] text-slate-400 border-border',
  DETERMINISTIC: 'bg-[#102a22] text-[#7ee0c8] border-[#1d4a3c]',
  INTERPRETATION: 'bg-[#3a2a10] text-[#f5c46b] border-[#5c4318]',
  INTERNET: 'bg-[#3a1018] text-[#ff8da0] border-[#5c1d2a]',
  'INTERNET-FACING': 'bg-[#3a1018] text-[#ff8da0] border-[#5c1d2a]',
  INTERNAL: 'bg-[#102a22] text-[#7ee0c8] border-[#1d4a3c]',
}

export function Badge({ value, className, ...props }: { value?: string | null } & HTMLAttributes<HTMLSpanElement>) {
  if (!value) {
    return <span className="text-slate-500">—</span>
  }
  return (
    <span
      className={cn(
        'inline-flex rounded border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide',
        tones[value.toUpperCase()] ?? 'border-border bg-[#122038] text-slate-300',
        className,
      )}
      {...props}
    >
      {value}
    </span>
  )
}
