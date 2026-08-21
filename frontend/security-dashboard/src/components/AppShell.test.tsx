import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppShell } from '@/components/AppShell'

describe('navigation', () => {
  it('exposes primary SOC destinations', () => {
    render(
      <MemoryRouter>
        <AppShell />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Investigations' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Vulnerabilities' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Findings' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Assets' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Remediation' })).toBeInTheDocument()
  })
})
