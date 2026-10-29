import { useId, type SelectHTMLAttributes } from 'react'

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  rotulo: string
  ocultarRotulo?: boolean
  erro?: string
}

export function Select({ rotulo, ocultarRotulo = false, erro, className = '', children, ...resto }: Props) {
  const id = useId()
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className={ocultarRotulo ? 'sr-only' : 'text-sm font-medium text-slate-700'}>
        {rotulo}
      </label>
      <select
        id={id}
        aria-invalid={erro ? true : undefined}
        className={`rounded-lg border bg-white px-3 py-2 text-sm shadow-xs outline-none focus:border-semear-500 focus:ring-2 focus:ring-semear-200 ${erro ? 'border-red-400' : 'border-slate-300'} ${className}`}
        {...resto}
      >
        {children}
      </select>
      {erro && <p className="text-xs text-red-600">{erro}</p>}
    </div>
  )
}
