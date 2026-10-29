import { useState } from 'react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { FinanceiroAbas } from '@/components/financeiro/FinanceiroAbas'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useResumoFinanceiro } from '@/hooks/useFinanceiro'
import { formatarMoeda } from '@/lib/formatters'
import { formatarCompetencia, type MesResumo } from '@/models/financeiro'

/**
 * Resumo mensal (vw_resumo_financeiro_mensal): previsto × recebido × em atraso.
 * "Recebido" inclui multas e juros e já desconta os 10% de pontualidade, por isso não fecha com o previsto.
 */
export function ResumoFinanceiroPage() {
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const resumo = useResumoFinanceiro(ano)

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Financeiro"
        descricao="Por competência (mês de referência), somando Taxa de Associado, Mensalidade e matrícula."
        acoes={<SeletorAno ano={ano} aoMudar={setAno} />}
      />
      <FinanceiroAbas />

      {resumo.isPending ? (
        <Carregando />
      ) : resumo.isError ? (
        <EstadoErro erro={resumo.error} aoTentarDeNovo={() => void resumo.refetch()} />
      ) : resumo.data.meses.length === 0 ? (
        <EstadoVazio titulo="Sem cobranças neste ano" descricao="Gere as cobranças do período na aba Cobranças." />
      ) : (
        <>
          <dl className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-4">
            <Indicador rotulo={`Previsto em ${resumo.data.ano}`} valor={formatarMoeda(resumo.data.total.previsto)} />
            <Indicador rotulo="Recebido" valor={formatarMoeda(resumo.data.total.recebido)} />
            <Indicador rotulo="Em atraso" valor={formatarMoeda(resumo.data.total.emAtraso)} destaque={resumo.data.total.emAtraso > 0} />
            <Indicador rotulo="Concedido em bolsas" valor={formatarMoeda(resumo.data.total.descontosBolsa)} />
          </dl>

          <Tabela legenda={`Resumo financeiro mensal de ${resumo.data.ano}`}>
            <thead>
              <tr>
                <Th>Competência</Th>
                <Th className="text-right">Cobranças</Th>
                <Th className="text-right">Previsto</Th>
                <Th className="text-right">Bolsas</Th>
                <Th className="text-right">Recebido</Th>
                <Th className="text-right">Em atraso</Th>
                <Th className="w-40">Recebido do previsto</Th>
              </tr>
            </thead>
            <tbody>
              {resumo.data.meses.map((m) => (
                <Linha key={m.competencia} mes={m} rotulo={formatarCompetencia(m.competencia!)} />
              ))}
              <Linha mes={resumo.data.total} rotulo="Total" negrito />
            </tbody>
          </Tabela>
          <p className="mt-3 text-xs text-slate-500">
            O recebido já desconta os 10% de quem pagou até o vencimento e soma multa e juros de quem pagou atrasado. Por isso pode ficar abaixo
            do previsto mesmo sem atraso. Meses futuros aparecem com o previsto e ainda sem recebimento.
          </p>
        </>
      )}
    </div>
  )
}

function Linha({ mes: m, rotulo, negrito = false }: { mes: MesResumo; rotulo: string; negrito?: boolean }) {
  const percentual = m.previsto > 0 ? Math.min(100, Math.round((m.recebido / m.previsto) * 100)) : 0
  return (
    <Tr>
      <Td className={negrito ? 'font-semibold' : 'font-medium'}>{rotulo}</Td>
      <Td className="text-right tabular-nums">{m.quantidade}</Td>
      <Td className="text-right tabular-nums">{formatarMoeda(m.previsto)}</Td>
      <Td className="text-right tabular-nums text-slate-500">{formatarMoeda(m.descontosBolsa)}</Td>
      <Td className={`text-right tabular-nums ${negrito ? 'font-semibold' : ''}`}>{formatarMoeda(m.recebido)}</Td>
      <Td className={`text-right tabular-nums ${m.emAtraso > 0 ? 'text-red-700' : 'text-slate-400'}`}>{formatarMoeda(m.emAtraso)}</Td>
      <Td>
        <div className="flex items-center gap-2" title={`${percentual}% do previsto recebido`}>
          <div className="h-1.5 flex-1 rounded-full bg-slate-100" aria-hidden>
            <div className="h-1.5 rounded-full bg-semear-600" style={{ width: `${percentual}%` }} />
          </div>
          <span className="w-9 text-right text-xs tabular-nums text-slate-600">{percentual}%</span>
        </div>
      </Td>
    </Tr>
  )
}

function Indicador({ rotulo, valor, destaque = false }: { rotulo: string; valor: string; destaque?: boolean }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className={`text-xl font-semibold tabular-nums ${destaque ? 'text-red-700' : 'text-slate-900'}`}>{valor}</dd>
    </div>
  )
}
