import { LoaderCircle } from 'lucide-react'

export function TelaCarregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div className="flex h-full min-h-[60vh] items-center justify-center gap-3 text-slate-500" role="status">
      <LoaderCircle className="size-5 animate-spin text-semear-600" aria-hidden />
      <span>{texto}</span>
    </div>
  )
}
