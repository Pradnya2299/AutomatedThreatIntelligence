import { Card } from '@/components/ui/card'
import { Badge } from '@/components/Badge'
import type { Investigation } from '@/types/investigation'

export function HumanReviewBanner({ investigation }: { investigation: Investigation }) {
  if (investigation.status === 'REVIEW_REQUIRED') {
    const reason =
      investigation.recommendation?.summary ??
      investigation.decisionHistory?.find((d) => d.action === 'REVIEW_REQUIRED')?.reason ??
      'Not available'
    return (
      <Card className="border-[#5c4318] bg-[#2a1f0c]" role="alert">
        <p className="text-sm font-semibold uppercase tracking-wide text-[#f5c46b]">Human review required</p>
        <p className="mt-2 text-sm text-slate-200">
          The system does not have sufficient evidence to safely produce a remediation decision.
        </p>
        <p className="mt-2 text-sm text-white">Reason: {reason}</p>
        <div className="mt-2">
          <Badge value={investigation.confidence} />
        </div>
        <p className="mt-3 text-sm text-slate-300">Remediation generation was intentionally withheld.</p>
      </Card>
    )
  }
  if (investigation.status === 'FAILED') {
    const reason =
      investigation.errors?.[investigation.errors.length - 1]?.message ??
      investigation.decisionHistory?.find((d) => d.action === 'FAIL')?.reason ??
      'Not available'
    return (
      <Card className="border-[#5c1d2a] bg-[#2a1016]" role="alert">
        <p className="text-sm font-semibold uppercase tracking-wide text-[#ff8da0]">Investigation failed</p>
        <p className="mt-2 text-sm text-white">{reason}</p>
        <p className="mt-2 text-sm text-slate-300">Prior deterministic evidence is preserved. No fabricated plan is shown.</p>
      </Card>
    )
  }
  return null
}
