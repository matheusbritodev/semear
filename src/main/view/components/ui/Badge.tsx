import type { ReactNode } from 'react'

export type CorBadge = 'verde' | 'amarelo' | 'vermelho' | 'cinza' | 'azul'

const CORES: Record<CorBadge, string> = {
  verde: 'bg-semear-50 text-semear-700 ring-semear-200',
  amarelo: 'bg-amber-50 text-amber-800 ring-amber-200',
  vermelho: 'bg-red-50 text-red-700 ring-red-200',
  cinza: 'bg-slate-100 text-slate-600 ring-slate-200',
  azul: 'bg-sky-50 text-sky-700 ring-sky-200',
}

export function Badge({ cor = 'cinza', children }: { cor?: CorBadge; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset ${CORES[cor]}`}>
      {children}
    </span>
  )
}
