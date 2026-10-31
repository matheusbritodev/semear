import type { ButtonHTMLAttributes } from 'react'
import { LoaderCircle } from '@/components/ui/icones'

type Variante = 'primario' | 'secundario' | 'fantasma'

const ESTILOS: Record<Variante, string> = {
  primario:
    'bg-semear-600 text-white shadow-sm shadow-black/10 hover:brightness-90 focus-visible:outline-semear-600 dark:bg-semear-500 dark:focus-visible:outline-semear-400',
  secundario:
    'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 focus-visible:outline-slate-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
  fantasma: 'text-slate-600 hover:bg-slate-100 focus-visible:outline-slate-400 dark:text-slate-300 dark:hover:bg-slate-800',
}

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  carregando?: boolean
}

export function Button({ variante = 'primario', carregando = false, className = '', children, disabled, ...resto }: Props) {
  return (
    <button
      {...resto}
      disabled={disabled || carregando}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-[color,background-color,border-color,transform,filter] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${ESTILOS[variante]} ${className}`}
    >
      {carregando && <LoaderCircle className="size-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}
