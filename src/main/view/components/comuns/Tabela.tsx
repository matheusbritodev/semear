import type { ReactNode, ThHTMLAttributes, TdHTMLAttributes } from 'react'

/** Tabela com rolagem horizontal no celular. */
export function Tabela({ children, legenda }: { children: ReactNode; legenda: string }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-xs">
      <table className="w-full text-left text-sm">
        <caption className="sr-only">{legenda}</caption>
        {children}
      </table>
    </div>
  )
}

export function Th({ className = '', ...resto }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      scope="col"
      className={`border-b border-slate-200 bg-slate-50 px-4 py-2.5 text-xs font-semibold tracking-wide text-slate-500 uppercase ${className}`}
      {...resto}
    />
  )
}

export function Td({ className = '', ...resto }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={`border-b border-slate-100 px-4 py-3 align-middle ${className}`} {...resto} />
}

export function Tr({ children, destacar = false }: { children: ReactNode; destacar?: boolean }) {
  return <tr className={`last:[&>td]:border-0 hover:bg-slate-50/70 ${destacar ? 'bg-slate-50/50' : ''}`}>{children}</tr>
}
