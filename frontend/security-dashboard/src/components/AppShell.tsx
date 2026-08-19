import { NavLink, Outlet } from 'react-router-dom'
import { LayoutDashboard, Bug, FileSearch, Server, Shield, Settings, GitBranch } from 'lucide-react'

const links = [
  { to: '/investigations', label: 'Investigations', icon: GitBranch },
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/vulnerabilities', label: 'Vulnerabilities', icon: Bug },
  { to: '/findings', label: 'Findings', icon: FileSearch },
  { to: '/assets', label: 'Assets', icon: Server },
  { to: '/remediation', label: 'Remediation', icon: Shield },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export function AppShell() {
  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 flex-col border-r border-border bg-[#08101c] px-4 py-6">
        <div className="mb-8 px-2">
          <p className="text-xs uppercase tracking-[0.2em] text-accent">Threat Advisor</p>
          <h1 className="mt-1 text-lg font-semibold text-white">SOC Console</h1>
        </div>
        <nav aria-label="Primary" className="flex flex-col gap-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-md px-3 py-2 text-sm ${isActive ? 'bg-[#122038] text-accent' : 'text-slate-300 hover:bg-[#0f1a2c]'}`
              }
            >
              <link.icon size={16} aria-hidden />
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-border px-8 py-3">
          <p className="text-sm text-slate-400">CVE → Investigation → Isolated patch → Human approval → PR</p>
          <p className="text-xs uppercase tracking-wide text-slate-500">Local · analyst via API proxy</p>
        </header>
        <main className="flex-1 overflow-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
