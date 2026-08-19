import { AgentCard } from '@/components/AgentCard'
import { Card, CardTitle } from '@/components/ui/card'
import { PIPELINE_AGENTS } from '@/types/investigation'
import { evidenceCountForAgent } from '@/utils/investigationDisplay'
import type { Investigation } from '@/types/investigation'

function lastExecution(investigation: Investigation, agentName: string) {
  const rows = (investigation.executions ?? []).filter((e) => e.agentName === agentName)
  return rows[rows.length - 1] ?? null
}

function agentTrace(investigation: Investigation, agentName: string) {
  const rows = (investigation.executionTrace ?? []).filter((e) => e.agentName === agentName)
  return rows[rows.length - 1] ?? null
}

export function AgentPipeline({ investigation }: { investigation: Investigation }) {
  return (
    <section>
      <Card className="mb-4">
        <CardTitle>Agent investigation pipeline</CardTitle>
        <p className="text-xs text-slate-500">
          Orchestrator inspects evidence, then runs Threat → Asset → Risk → Remediation. Status comes from
          executionTrace / executions, not from a hardcoded demo sequence.
        </p>
        <p className="mt-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-accent">Orchestrator</p>
      </Card>
      <div className="flex flex-col gap-3 xl:flex-row xl:items-stretch">
        {PIPELINE_AGENTS.map((agent, index) => {
          const execution = lastExecution(investigation, agent.name)
          const trace = agentTrace(investigation, agent.name)
          const extra =
            agent.name === 'AssetInvestigationAgent' && investigation.assetInvestigation
              ? `Assets: ${investigation.assetInvestigation.affectedAssetCount}`
              : undefined
          return (
            <div key={agent.name} className="flex flex-1 flex-col items-stretch gap-2 xl:flex-row xl:items-center">
              <AgentCard
                label={agent.label}
                status={execution?.status ?? trace?.status ?? 'PENDING'}
                confidence={
                  agent.name === 'ThreatIntelligenceAgent'
                    ? investigation.threatIntelligence?.confidence ?? trace?.confidence ?? null
                    : agent.name === 'AssetInvestigationAgent'
                      ? investigation.assetInvestigation?.confidence ?? trace?.confidence ?? null
                      : agent.name === 'RiskAnalystAgent'
                        ? investigation.riskAnalysis?.confidence ?? trace?.confidence ?? null
                        : investigation.remediation?.confidence ?? trace?.confidence ?? null
                }
                durationMs={trace?.durationMs ?? null}
                evidenceCount={evidenceCountForAgent(investigation, agent.name)}
                startedAt={execution?.startedAt ?? trace?.startTime ?? null}
                endedAt={execution?.completedAt ?? trace?.endTime ?? null}
                extra={extra}
              />
              {index < PIPELINE_AGENTS.length - 1 && (
                <p className="text-center text-accent xl:px-1" aria-hidden>
                  ↓
                </p>
              )}
            </div>
          )
        })}
      </div>
      <p className="mt-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-slate-500">
        {investigation.status}
      </p>
    </section>
  )
}
