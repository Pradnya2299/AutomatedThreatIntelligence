import { describe, expect, it } from 'vitest'
import { resolveApiBase } from '@/services/api/client'

describe('resolveApiBase', () => {
  it('uses the Vite proxy for empty and local api-service URLs in dev', () => {
    expect(resolveApiBase('', true)).toBe('')
    expect(resolveApiBase('http://localhost:8080', true)).toBe('')
    expect(resolveApiBase('http://localhost:8080/api', true)).toBe('')
    expect(resolveApiBase('http://127.0.0.1:8080/', true)).toBe('')
  })

  it('keeps a remote origin', () => {
    expect(resolveApiBase('https://api.example.test', true)).toBe('https://api.example.test')
    expect(resolveApiBase('http://localhost:8080', false)).toBe('http://localhost:8080')
  })
})
