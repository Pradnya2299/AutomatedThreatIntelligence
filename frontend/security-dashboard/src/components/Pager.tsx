import { Button } from '@/components/ui/button'

export function Pager({
  page,
  totalPages,
  onPage,
}: {
  page: number
  totalPages: number
  onPage: (page: number) => void
}) {
  return (
    <div className="mt-4 flex items-center justify-between text-sm text-slate-400">
      <span>
        Page {page + 1} of {Math.max(totalPages, 1)}
      </span>
      <div className="flex gap-2">
        <Button type="button" disabled={page <= 0} onClick={() => onPage(page - 1)}>
          Previous
        </Button>
        <Button type="button" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  )
}
