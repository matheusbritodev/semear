import type { ReactNode, ThHTMLAttributes, TdHTMLAttributes } from 'react'

/** Tabela com rolagem horizontal no celular. */
export function Tabela({ children, legenda }: { children: ReactNode; legenda: string }) {
  return (
    <div className="sombra-cartao overflow-x-auto rounded-2xl border border-slate-200/80 bg-white">
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
      className={`border-b border-slate-200/80 bg-slate-50/80 px-4 py-3 text-[11px] font-semibold tracking-wider text-slate-500 uppercase ${className}`}
      {...resto}
    />
  )
}

export function Td({ className = '', ...resto }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={`border-b border-slate-100 px-4 py-3 align-middle ${className}`} {...resto} />
}

export function Tr({ children, destacar = false }: { children: ReactNode; destacar?: boolean }) {
  return (
    <tr className={`transition-colors duration-150 last:[&>td]:border-0 hover:bg-semear-50/40 ${destacar ? 'bg-slate-50/60' : ''}`}>
      {children}
    </tr>
  )
}
