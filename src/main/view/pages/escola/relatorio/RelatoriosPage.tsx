import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Download, Printer } from '@/components/ui/icones'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente, useTurmas } from '@/hooks/useAcademico'
import { formatarData } from '@/lib/formatters'
import { formatarDataHora } from '@/models/controle'
import { ROTULO_STATUS_MATRICULA, type StatusMatricula } from '@/models/enums'
import type { RelatorioAlunos } from '@/models/relatorio'
import { relatorioService } from '@/services/RelatorioService'

/**
 * Relatório de alunos matriculados (dados cadastrados no banco): filtros, totais e a lista completa.
 * "Imprimir / PDF" usa a impressão do navegador (o menu some na impressão); "Planilha" baixa um CSV.
 */
export function RelatoriosPage() {
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const [turmaId, setTurmaId] = useState('')
  const turmas = useTurmas(ano, ano !== undefined)
  const relatorio = useQuery({
    queryKey: ['relatorios', 'alunos', ano, turmaId],
    queryFn: () => relatorioService.alunos(ano!, turmaId || undefined),
    enabled: ano !== undefined,
  })
  const r = relatorio.data

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Relatório de alunos"
        descricao="Alunos matriculados no ano letivo, com situação da matrícula, responsável financeiro e documentos pendentes."
        acoes={
          r && r.total > 0 && (
            <div className="flex flex-wrap gap-2 print:hidden">
              <Button variante="secundario" onClick={() => baixarCsv(r)}>
                <Download className="size-4" aria-hidden /> Planilha (CSV)
              </Button>
              <Button onClick={() => window.print()}>
                <Printer className="size-4" aria-hidden /> Imprimir / PDF
              </Button>
            </div>
          )
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3 print:hidden">
        <SeletorAno
          ano={ano}
          aoMudar={(novo) => {
            setAno(novo)
            setTurmaId('')
          }}
        />
        <div className="w-full sm:w-56">
          <Select rotulo="Turma" ocultarRotulo value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
            <option value="">Todas as turmas</option>
            {(turmas.data ?? []).map((t) => (
              <option key={t.id} value={t.id}>
                {t.nomeCompleto}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {relatorio.isPending ? (
        <Carregando />
      ) : relatorio.isError ? (
        <EstadoErro erro={relatorio.error} aoTentarDeNovo={() => void relatorio.refetch()} />
      ) : r!.total === 0 ? (
        <EstadoVazio titulo="Nenhum aluno neste filtro" />
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-slate-500">
            Centro Educacional Semeando · ano letivo {r!.ano}
            {r!.turma ? ` · turma ${r!.turma}` : ' · todas as turmas'} · emitido em {formatarDataHora(r!.emitidoEm)} por {r!.emitidoPor}
          </p>

          <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Total rotulo="Matrículas" valor={r!.total} />
            <Total rotulo="Vigentes" valor={r!.vigentes} />
            <Total rotulo="Aguardando pagamento" valor={r!.porSituacao.AGUARDANDO_PAGAMENTO} />
            <Total rotulo="Com documentos pendentes" valor={r!.comDocumentosPendentes} />
          </dl>

          {r!.porTurma.length > 1 && (
            <Card titulo="Por turma">
              <ul className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
                {r!.porTurma.map((t) => (
                  <li key={t.turma} className="flex justify-between border-b border-slate-100 py-1">
                    <span className="text-slate-700">{t.turma}</span>
                    <span className="text-slate-500 tabular-nums">
                      {t.vigentes} vigentes{t.alunos !== t.vigentes ? ` de ${t.alunos}` : ''}
                    </span>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          <Tabela legenda={`Relatório de alunos ${r!.ano}`}>
            <thead>
              <tr>
                <Th className="w-16">Nº</Th>
                <Th>Aluno</Th>
                <Th>Nascimento</Th>
                <Th>Turma</Th>
                <Th>Situação</Th>
                <Th>Responsável financeiro</Th>
                <Th>Documentos</Th>
              </tr>
            </thead>
            <tbody>
              {r!.linhas.map((l) => (
                <Tr key={`${l.numeroCadastro}-${l.turma}`}>
                  <Td className="text-slate-500 tabular-nums">{l.numeroCadastro}</Td>
                  <Td className="font-medium text-slate-900">{l.aluno}</Td>
                  <Td className="tabular-nums">
                    {formatarData(l.dataNascimento)} <span className="text-slate-500">({l.idade} anos)</span>
                  </Td>
                  <Td>{l.turma}</Td>
                  <Td>
                    <StatusMatriculaBadge status={l.situacao} />
                  </Td>
                  <Td className="text-slate-600">{l.responsavelFinanceiro ?? '—'}</Td>
                  <Td>
                    {l.documentosPendentes > 0 ? (
                      <Badge cor="amarelo">{l.documentosPendentes} pendente(s)</Badge>
                    ) : (
                      <span className="text-slate-500">Completos</span>
                    )}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Tabela>
        </div>
      )}
    </div>
  )
}

function Total({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="sombra-cartao rounded-2xl border border-slate-200/80 bg-white p-4">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className="text-2xl font-semibold text-slate-900 tabular-nums">{valor}</dd>
    </div>
  )
}

/** CSV com ";" (o Excel em português abre direto) e BOM para os acentos. */
function baixarCsv(r: RelatorioAlunos) {
  const celula = (v: string | number | null) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const linhas = [
    ['Nº', 'Aluno', 'Nascimento', 'Idade', 'Turma', 'Situação', 'Responsável financeiro', 'Documentos pendentes'],
    ...r.linhas.map((l) => [
      l.numeroCadastro,
      l.aluno,
      formatarData(l.dataNascimento),
      l.idade,
      l.turma,
      ROTULO_STATUS_MATRICULA[l.situacao as StatusMatricula],
      l.responsavelFinanceiro,
      l.documentosPendentes,
    ]),
  ]
  const conteudo = '﻿' + linhas.map((l) => l.map(celula).join(';')).join('\r\n')
  const url = URL.createObjectURL(new Blob([conteudo], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `relatorio-alunos-${r.ano}${r.turma ? '-' + r.turma.replace(/\s+/g, '-') : ''}.csv`
  link.click()
  URL.revokeObjectURL(url)
}
