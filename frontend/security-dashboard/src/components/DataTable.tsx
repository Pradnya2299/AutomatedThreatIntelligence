import type { KeyboardEvent, ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/lib/utils'

export function DataTable({
  headers,
  children,
  className,
}: {
  headers: string[]
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('overflow-x-auto rounded-lg border border-border', className)}>
      <table className="w-full min-w-[720px] border-collapse text-left text-sm">
        <thead className="bg-[#0b1422] text-xs uppercase tracking-wide text-slate-400">
          <tr>
            {headers.map((header) => (
              <th key={header} scope="col" className="px-3 py-2 font-medium">
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">{children}</tbody>
      </table>
    </div>
  )
}

export function Td({ children, className }: { children: ReactNode; className?: string }) {
  return <td className={cn('px-3 py-2.5 align-middle text-slate-200', className)}>{children}</td>
}

export function RowLink({ to, children }: { to: string; children: ReactNode }) {
  const navigate = useNavigate()
  const go = () => navigate(to)
  const onKey = (event: KeyboardEvent<HTMLTableRowElement>) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      go()
    }
  }
  return (
    <tr
      tabIndex={0}
      role="link"
      className="cursor-pointer hover:bg-[#122038] focus:bg-[#122038] focus:outline-none"
      onClick={go}
      onKeyDown={onKey}
    >
      {children}
    </tr>
  )
}
