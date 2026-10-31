import type { ReactNode } from 'react'

export type CorBadge = 'verde' | 'amarelo' | 'vermelho' | 'cinza' | 'azul'

const CORES: Record<CorBadge, string> = {
  verde: 'bg-semear-50 text-semear-700 ring-semear-200/80',
  amarelo: 'bg-amber-50 text-amber-800 ring-amber-200/80',
  vermelho: 'bg-red-50 text-red-700 ring-red-200/80',
  cinza: 'bg-slate-100 text-slate-600 ring-slate-200/80',
  azul: 'bg-sky-50 text-sky-700 ring-sky-200/80',
}

const PONTOS: Record<CorBadge, string> = {
  verde: 'bg-semear-500',
  amarelo: 'bg-amber-500',
  vermelho: 'bg-red-500',
  cinza: 'bg-slate-400',
  azul: 'bg-sky-500',
}

/** Etiqueta de status. O ponto colorido ajuda a ler a cor mesmo com pouco contraste. */
export function Badge({ cor = 'cinza', children }: { cor?: CorBadge; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset ${CORES[cor]}`}
    >
      <span className={`size-1.5 shrink-0 rounded-full ${PONTOS[cor]}`} aria-hidden />
      {children}
    </span>
  )
}
