import { useId, type InputHTMLAttributes, type ReactNode } from 'react'
import type { TipoIcone } from '@/components/ui/icones'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  rotulo: string
  erro?: string
  /** Ícone à esquerda, dentro do campo. */
  icone?: TipoIcone
  /** Elemento à direita, dentro do campo (ex.: botão de mostrar senha). */
  acessorio?: ReactNode
}

export function Input({ rotulo, erro, icone: Icone, acessorio, className = '', ...resto }: Props) {
  const id = useId()
  const idErro = `${id}-erro`
  return (
    <div className="group flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-300">
        {rotulo}
      </label>
      <div className="relative">
        {Icone && (
          <Icone
            className={`pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 transition-colors ${erro ? 'text-red-400' : 'text-slate-400 group-focus-within:text-semear-600 dark:text-slate-500 dark:group-focus-within:text-semear-400'}`}
            aria-hidden
          />
        )}
        <input
          id={id}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? idErro : undefined}
          className={`w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 shadow-xs outline-none transition placeholder:text-slate-400 focus:border-semear-500 focus:ring-2 focus:ring-semear-200 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-semear-400 dark:focus:ring-semear-400/25 ${erro ? 'border-red-400 dark:border-red-500/70' : 'border-slate-300 dark:border-slate-700'} ${Icone ? 'pl-9' : ''} ${acessorio ? 'pr-11' : ''} ${className}`}
          {...resto}
        />
        {acessorio && <div className="absolute inset-y-0 right-1 flex items-center">{acessorio}</div>}
      </div>
      {erro && (
        <p id={idErro} className="text-xs text-red-600 motion-safe:animate-surgir dark:text-red-400">
          {erro}
        </p>
      )}
    </div>
  )
}
