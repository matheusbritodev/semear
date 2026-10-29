import { Fragment, useState } from 'react'
import { Link } from 'react-router'
import { ChevronDown, ChevronRight, MessageCircle } from 'lucide-react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { DetalheCobrancaModal } from '@/components/financeiro/DetalheCobrancaModal'
import { FinanceiroAbas } from '@/components/financeiro/FinanceiroAbas'
import { useInadimplencia } from '@/hooks/useFinanceiro'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_TIPO_COBRANCA, formatarCompetencia, type Inadimplente } from '@/models/financeiro'

/** Inadimplência (RF05): quem tem cobrança vencida, há quanto tempo e quanto deve hoje (com multa e juros). */
export function InadimplenciaPage() {
  const consulta = useInadimplencia()
  const [abertos, setAbertos] = useState<Set<string>>(new Set())
  const [cobranca, setCobranca] = useState<string | null>(null)

  function alternar(id: string) {
    setAbertos((atual) => {
      const novo = new Set(atual)
      if (novo.has(id)) novo.delete(id)
      else novo.add(id)
      return novo
    })
  }

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Financeiro"
        descricao="Alunos com cobrança vencida em aberto. Quem está aqui não pode rematricular (RN13) até quitar."
      />
      <FinanceiroAbas />

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.alunos === 0 ? (
        <EstadoVazio titulo="Nenhuma inadimplência" descricao="Todas as cobranças vencidas foram pagas." />
      ) : (
        <>
          <dl className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-4">
            <Indicador rotulo="Alunos" valor={String(consulta.data.alunos)} />
            <Indicador rotulo="Cobranças vencidas" valor={String(consulta.data.cobrancas)} />
            <Indicador rotulo="Valor original" valor={formatarMoeda(consulta.data.valorOriginal)} />
            <Indicador rotulo="Atualizado hoje" valor={formatarMoeda(consulta.data.valorAtualizado)} />
          </dl>

          <Tabela legenda="Alunos inadimplentes">
            <thead>
              <tr>
                <Th>Aluno</Th>
                <Th>Responsável financeiro</Th>
                <Th className="text-right">Cobranças</Th>
                <Th className="text-right">Maior atraso</Th>
                <Th className="text-right">Deve hoje</Th>
              </tr>
            </thead>
            <tbody>
              {consulta.data.inadimplentes.map((i) => {
                const aberto = abertos.has(i.alunoId)
                return (
                  <Fragment key={i.alunoId}>
                    <Tr>
                      <Td>
                        <button
                          type="button"
                          onClick={() => alternar(i.alunoId)}
                          aria-expanded={aberto}
                          className="inline-flex items-center gap-1 text-left font-medium text-slate-900 hover:text-semear-700"
                        >
                          {aberto ? <ChevronDown className="size-4" aria-hidden /> : <ChevronRight className="size-4" aria-hidden />}
                          {i.aluno}
                        </button>
                        <p className="pl-5 text-xs text-slate-500">
                          {i.turma} ·{' '}
                          <Link to={`/escola/alunos/${i.alunoId}`} className="hover:underline">
                            ficha do aluno
                          </Link>
                        </p>
                      </Td>
                      <Td>
                        <p className="text-slate-800">{i.responsavel}</p>
                        {i.telefone && <Contato inadimplente={i} />}
                      </Td>
                      <Td className="text-right tabular-nums">{i.cobrancas}</Td>
                      <Td className={`text-right tabular-nums ${i.maiorAtraso > 30 ? 'font-semibold text-red-700' : ''}`}>
                        {i.maiorAtraso} dias
                      </Td>
                      <Td className="text-right font-medium tabular-nums">{formatarMoeda(i.valorAtualizado)}</Td>
                    </Tr>
                    {aberto &&
                      i.itens.map((c) => (
                        <tr key={c.id} className="bg-slate-50 text-sm">
                          <td className="py-2 pr-3 pl-9" colSpan={2}>
                            <button type="button" onClick={() => setCobranca(c.id)} className="font-medium text-semear-700 hover:underline">
                              {ROTULO_TIPO_COBRANCA[c.tipo]} · {formatarCompetencia(c.competencia)}
                            </button>
                            <span className="text-slate-500"> · venceu em {formatarData(c.vencimento)}</span>
                          </td>
                          <td className="px-3 py-2 text-right tabular-nums text-slate-500">{formatarMoeda(c.valorFinal)}</td>
                          <td className="px-3 py-2 text-right tabular-nums text-slate-500">{c.diasDeAtraso} dias</td>
                          <td className="px-3 py-2 text-right tabular-nums">{formatarMoeda(c.valorHoje)}</td>
                        </tr>
                      ))}
                  </Fragment>
                )
              })}
            </tbody>
          </Tabela>
        </>
      )}
      <DetalheCobrancaModal id={cobranca} aoFechar={() => setCobranca(null)} />
    </div>
  )
}

function Contato({ inadimplente: i }: { inadimplente: Inadimplente }) {
  const digitos = (i.telefone ?? '').replace(/\D/g, '')
  const mensagem =
    `Olá, ${i.responsavel.split(' ')[0]}! Aqui é do financeiro do Centro Educacional Semeando. ` +
    `Consta em aberto ${i.cobrancas === 1 ? 'uma cobrança' : `${i.cobrancas} cobranças`} de ${i.aluno}, ` +
    `no total de ${formatarMoeda(i.valorAtualizado)} hoje. Você pode pagar pelo Portal do Responsável ou na secretaria. Qualquer dúvida, estamos à disposição.`
  return (
    <p className="mt-0.5 flex flex-wrap items-center gap-3 text-xs">
      <span className="text-slate-500">{i.telefone}</span>
      {digitos.length >= 10 && (
        <a
          href={`https://wa.me/55${digitos}?text=${encodeURIComponent(mensagem)}`}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 font-medium text-semear-700 hover:underline"
        >
          <MessageCircle className="size-3.5" aria-hidden /> Cobrar no WhatsApp
        </a>
      )}
    </p>
  )
}

function Indicador({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className="text-xl font-semibold text-slate-900 tabular-nums">{valor}</dd>
    </div>
  )
}
