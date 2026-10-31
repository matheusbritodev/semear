import type { ReactNode } from 'react'
import type { TipoIcone } from './icones'

interface Props {
  titulo?: string
  /** Ícone ao lado do título. */
  icone?: TipoIcone
  children: ReactNode
  className?: string
}

export function Card({ titulo, icone: Icone, children, className = '' }: Props) {
  return (
    <section className={`sombra-cartao rounded-2xl border border-slate-200/80 bg-white p-5 ${className}`}>
      {titulo && (
        <h2 className="mb-4 flex items-center gap-2 text-[15px] font-semibold text-slate-900">
          {Icone && (
            <span className="grid size-7 place-items-center rounded-lg bg-semear-50 text-semear-600">
              <Icone className="size-4" aria-hidden />
            </span>
          )}
          {titulo}
        </h2>
      )}
      {children}
    </section>
  )
}
