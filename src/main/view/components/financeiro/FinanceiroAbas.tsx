import { NavLink } from 'react-router'
import { usePermissao } from '@/hooks/useUtilitarios'

/** Navegação entre as telas do financeiro. Bolsas aparece só para quem pode ver o módulo BOLSAS. */
export function FinanceiroAbas() {
  const veFinanceiro = usePermissao('FINANCEIRO', 'LER')
  const veBolsas = usePermissao('BOLSAS', 'LER')
  const abas = [
    veFinanceiro && { rota: '/escola/financeiro/cobrancas', rotulo: 'Cobranças' },
    veFinanceiro && { rota: '/escola/financeiro/inadimplencia', rotulo: 'Inadimplência' },
    veFinanceiro && { rota: '/escola/financeiro/resumo', rotulo: 'Resumo mensal' },
    veBolsas && { rota: '/escola/financeiro/bolsas', rotulo: 'Bolsas' },
  ].filter((a): a is { rota: string; rotulo: string } => Boolean(a))

  if (abas.length < 2) return null
  return (
    <nav aria-label="Financeiro" className="mb-6 flex gap-1 overflow-x-auto border-b border-slate-200">
      {abas.map((a) => (
        <NavLink
          key={a.rota}
          to={a.rota}
          className={({ isActive }) =>
            `-mb-px shrink-0 border-b-2 px-3 py-2 text-sm font-medium ${
              isActive ? 'border-semear-600 text-semear-700' : 'border-transparent text-slate-500 hover:text-slate-800'
            }`
          }
        >
          {a.rotulo}
        </NavLink>
      ))}
    </nav>
  )
}
