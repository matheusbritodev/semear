import { useState } from 'react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { StatusCobrancaBadge } from '@/components/comuns/StatusBadges'
import { Card } from '@/components/ui/Card'
import { useCobrancas } from '@/hooks/useFinanceiro'
import { usePermissao } from '@/hooks/useUtilitarios'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_TIPO_COBRANCA, formatarCompetencia } from '@/models/financeiro'
import { DetalheCobrancaModal } from './DetalheCobrancaModal'

/** Todas as cobranças da matrícula (para quem pode ver o financeiro), com o valor de hoje. */
export function CobrancasDaMatricula({ matriculaId }: { matriculaId: string }) {
  const podeVer = usePermissao('FINANCEIRO', 'LER')
  const cobrancas = useCobrancas({ matriculaId, tamanho: 100 }, podeVer)
  const [aberta, setAberta] = useState<string | null>(null)

  if (!podeVer) return null

  return (
    <Card titulo="Cobranças" className="md:col-span-2">
      {cobrancas.isPending ? (
        <Carregando />
      ) : cobrancas.isError ? (
        <EstadoErro erro={cobrancas.error} aoTentarDeNovo={() => void cobrancas.refetch()} />
      ) : cobrancas.data.itens.length === 0 ? (
        <p className="text-sm text-slate-500">Nenhuma cobrança gerada para esta matrícula.</p>
      ) : (
        <ul className="divide-y divide-slate-100 text-sm">
          {[...cobrancas.data.itens]
            .sort((a, b) => a.vencimento.localeCompare(b.vencimento))
            .map((c) => (
              <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                <button type="button" onClick={() => setAberta(c.id)} className="text-left hover:text-semear-700">
                  <span className="font-medium">{ROTULO_TIPO_COBRANCA[c.tipo]}</span>{' '}
                  <span className="text-slate-500">
                    {formatarCompetencia(c.competencia)} · vence {formatarData(c.vencimento)}
                  </span>
                </button>
                <span className="flex items-center gap-3">
                  <span className="tabular-nums">
                    {formatarMoeda(c.status === 'PAGO' ? (c.valorPago ?? c.valorHoje) : c.status === 'CANCELADO' ? c.valorFinal : c.valorHoje)}
                  </span>
                  <StatusCobrancaBadge status={c.status} />
                </span>
              </li>
            ))}
        </ul>
      )}
      <DetalheCobrancaModal id={aberta} aoFechar={() => setAberta(null)} />
    </Card>
  )
}
