export function formatWhen(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

export function formatScore(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return Number(value).toFixed(1)
}

export function formatScoreOrUnavailable(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return 'Not available'
  }
  return Number(value).toFixed(1)
}
