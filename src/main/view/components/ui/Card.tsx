import type { ReactNode } from 'react'

export function Card({ titulo, children, className = '' }: { titulo?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-xl border border-slate-200 bg-white p-5 shadow-xs ${className}`}>
      {titulo && <h2 className="mb-3 text-base font-semibold text-slate-800">{titulo}</h2>}
      {children}
    </section>
  )
}
