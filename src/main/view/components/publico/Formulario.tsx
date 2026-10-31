import type { CSSProperties, ReactNode } from 'react'
import { AlertCircle } from '@/components/ui/icones'

/** Atraso em cascata para os itens do formulário surgirem um após o outro. */
export const emCascata = (ordem: number): CSSProperties => ({ animationDelay: `${150 + ordem * 70}ms` })

export function CabecalhoPublico({ titulo, descricao }: { titulo: string; descricao?: ReactNode }) {
  return (
    <div className="space-y-1.5 text-center motion-safe:animate-surgir" style={emCascata(0)}>
      <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">{titulo}</h1>
      {descricao && <p className="text-sm text-slate-500 dark:text-slate-400">{descricao}</p>}
    </div>
  )
}

/** Erro do formulário (ex.: senha incorreta), com um leve "balanço" ao aparecer. */
export function AlertaFormulario({ children }: { children: ReactNode }) {
  return (
    <p
      role="alert"
      className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700 motion-safe:animate-balancar dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300"
    >
      <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden />
      {children}
    </p>
  )
}
