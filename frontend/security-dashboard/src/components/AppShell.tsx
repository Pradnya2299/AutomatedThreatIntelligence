import { NavLink, Outlet } from 'react-router-dom'

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/vulnerabilities', label: 'Vulnerabilities' },
  { to: '/assets', label: 'Assets' },
  { to: '/findings', label: 'Findings' },
  { to: '/remediation', label: 'Remediation' },
  { to: '/settings', label: 'Settings' },
]

export function AppShell() {
  return (
    <div className="flex min-h-screen">
      <aside className="w-60 border-r border-border bg-[#08101c] px-4 py-6">
        <div className="mb-8 px-2">
          <p className="text-xs uppercase tracking-[0.2em] text-accent">Threat Advisor</p>
          <h1 className="mt-1 text-lg font-semibold text-white">SOC Console</h1>
        </div>
        <nav className="flex flex-col gap-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm ${isActive ? 'bg-[#122038] text-accent' : 'text-slate-300 hover:bg-[#0f1a2c]'}`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  )
}
