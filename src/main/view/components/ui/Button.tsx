import type { ButtonHTMLAttributes } from 'react'
import { LoaderCircle } from 'lucide-react'

type Variante = 'primario' | 'secundario' | 'fantasma'

const ESTILOS: Record<Variante, string> = {
  primario: 'bg-semear-600 text-white hover:bg-semear-700 focus-visible:outline-semear-600',
  secundario: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 focus-visible:outline-slate-400',
  fantasma: 'text-slate-600 hover:bg-slate-100 focus-visible:outline-slate-400',
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
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${ESTILOS[variante]} ${className}`}
    >
      {carregando && <LoaderCircle className="size-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}
