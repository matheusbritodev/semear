import { useId, type InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  rotulo: string
  erro?: string
}

export function Input({ rotulo, erro, className = '', ...resto }: Props) {
  const id = useId()
  const idErro = `${id}-erro`
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {rotulo}
      </label>
      <input
        id={id}
        aria-invalid={erro ? true : undefined}
        aria-describedby={erro ? idErro : undefined}
        className={`rounded-lg border bg-white px-3 py-2 text-sm shadow-xs outline-none transition focus:border-semear-500 focus:ring-2 focus:ring-semear-200 ${erro ? 'border-red-400' : 'border-slate-300'} ${className}`}
        {...resto}
      />
      {erro && (
        <p id={idErro} className="text-xs text-red-600">
          {erro}
        </p>
      )}
    </div>
  )
}
