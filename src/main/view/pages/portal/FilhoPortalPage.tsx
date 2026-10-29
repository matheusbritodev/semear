import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft, ArrowRight, FileSignature, FileText } from 'lucide-react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { StatusContratoBadge } from '@/components/comuns/StatusBadges'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Badge } from '@/components/ui/Badge'
import { Card } from '@/components/ui/Card'
import { Select } from '@/components/ui/Select'
import { useBoletimDoFilho, useContratosDoFilho, useMateriaisDoFilho } from '@/hooks/useFormalizacao'
import { useFilhos } from '@/hooks/usePortal'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import type { Boletim } from '@/models/formalizacao'

/** Tudo de um filho no portal (RF10): contratos, boletim, materiais e documentos. */
export function FilhoPortalPage() {
  const { id = '' } = useParams()
  const filhos = useFilhos()
  const filho = filhos.data?.find((f) => f.alunoId === id)

  if (filhos.isPending) return <Carregando />
  if (filhos.isError) return <EstadoErro erro={filhos.error} aoTentarDeNovo={() => void filhos.refetch()} />
  if (!filho) return <EstadoErro erro={new Error('Aluno não encontrado entre os seus filhos.')} />

  return (
    <div className="space-y-8">
      <div>
        <Link to="/portal" className="mb-3 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
          <ArrowLeft className="size-4" aria-hidden /> Início
        </Link>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">{filho.nome}</h1>
            <p className="text-slate-500">{filho.turmaAtual ?? 'Sem matrícula'}</p>
          </div>
          {filho.statusMatricula && <StatusMatriculaBadge status={filho.statusMatricula} />}
        </div>
      </div>

      <Contratos alunoId={id} />
      <SecaoBoletim alunoId={id} />
      <Materiais alunoId={id} />

      <Link
        to={`/portal/alunos/${id}/documentos`}
        className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 text-sm font-medium text-slate-800 hover:border-semear-200"
      >
        <span className="flex items-center gap-2">
          <FileText className="size-5 text-semear-600" aria-hidden /> Documentos da matrícula
          {filho.documentosPendentes > 0 && <Badge cor="amarelo">{filho.documentosPendentes} pendentes</Badge>}
        </span>
        <ArrowRight className="size-4 text-slate-400" aria-hidden />
      </Link>
    </div>
  )
}

function Contratos({ alunoId }: { alunoId: string }) {
  const contratos = useContratosDoFilho(alunoId)
  return (
    <section aria-labelledby="titulo-contratos">
      <h2 id="titulo-contratos" className="mb-3 text-base font-semibold text-slate-800">
        Contrato e Ficha de Sócio
      </h2>
      {contratos.isPending ? (
        <Carregando />
      ) : contratos.isError ? (
        <EstadoErro erro={contratos.error} />
      ) : contratos.data.length === 0 ? (
        <Card>
          <p className="text-sm text-slate-600">Nenhum documento ainda.</p>
        </Card>
      ) : (
        <Card>
          <ul className="divide-y divide-slate-100 text-sm">
            {contratos.data.map((c) => (
              <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                <span>
                  <span className="font-medium">{c.titulo}</span> <span className="text-slate-500">{c.ano} · {c.serie}</span>
                  {c.assinadoEm && <span className="block text-xs text-slate-500">assinado por {c.assinadoPor} em {formatarData(c.assinadoEm)}</span>}
                </span>
                <span className="flex items-center gap-2">
                  <StatusContratoBadge status={c.status} />
                  <Link
                    to={`/portal/contratos/${c.id}`}
                    className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 font-medium ${
                      c.podeAssinar ? 'bg-semear-600 text-white hover:bg-semear-700' : 'text-semear-700 hover:underline'
                    }`}
                  >
                    {c.podeAssinar ? (
                      <>
                        <FileSignature className="size-4" aria-hidden /> Ler e assinar
                      </>
                    ) : (
                      'Ver'
                    )}
                  </Link>
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </section>
  )
}

function SecaoBoletim({ alunoId }: { alunoId: string }) {
  const [ano, setAno] = useState<number>()
  const boletim = useBoletimDoFilho(alunoId, ano)
  const semBoletim = boletim.error instanceof ApiError && boletim.error.status === 404

  return (
    <section aria-labelledby="titulo-boletim">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h2 id="titulo-boletim" className="text-base font-semibold text-slate-800">
          Boletim e frequência
        </h2>
        {boletim.data && boletim.data.anosDisponiveis.length > 1 && (
          <div className="w-32">
            <Select rotulo="Ano" ocultarRotulo value={boletim.data.ano} onChange={(e) => setAno(Number(e.target.value))}>
              {boletim.data.anosDisponiveis.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </Select>
          </div>
        )}
      </div>
      {boletim.isPending ? (
        <Carregando />
      ) : semBoletim ? (
        <Card>
          <p className="text-sm text-slate-600">Ainda não há boletim.</p>
        </Card>
      ) : boletim.isError ? (
        <EstadoErro erro={boletim.error} />
      ) : (
        <TabelaBoletim boletim={boletim.data} />
      )}
    </section>
  )
}

function TabelaBoletim({ boletim: b }: { boletim: Boletim }) {
  const f = b.frequencia
  return (
    <Card>
      <p className="mb-3 text-sm text-slate-500">
        {b.ano} · {b.serie} {b.turma}
      </p>
      <dl className="mb-4 grid grid-cols-3 gap-3 text-center">
        <div className="rounded-lg bg-slate-50 px-2 py-2">
          <dt className="text-xs text-slate-500">Presença</dt>
          <dd className="text-lg font-semibold text-slate-900">{f.percentual === null ? '—' : `${String(f.percentual).replace('.', ',')}%`}</dd>
        </div>
        <div className="rounded-lg bg-slate-50 px-2 py-2">
          <dt className="text-xs text-slate-500">Aulas registradas</dt>
          <dd className="text-lg font-semibold text-slate-900">{f.aulas}</dd>
        </div>
        <div className="rounded-lg bg-slate-50 px-2 py-2">
          <dt className="text-xs text-slate-500">Faltas</dt>
          <dd className="text-lg font-semibold text-slate-900">{f.faltas}</dd>
        </div>
      </dl>

      {b.nivel === 'EDUCACAO_INFANTIL' ? (
        <p className="text-sm text-slate-600">Na Educação Infantil não há notas: o acompanhamento é feito pela professora e pela frequência.</p>
      ) : b.disciplinas.length === 0 ? (
        <p className="text-sm text-slate-600">Nenhuma nota lançada ainda.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[28rem] text-sm">
            <caption className="sr-only">Notas por bimestre</caption>
            <thead>
              <tr className="border-b border-slate-200 text-left text-slate-500">
                <th className="py-2 font-medium">Disciplina</th>
                {[1, 2, 3, 4].map((n) => (
                  <th key={n} className="w-14 py-2 text-center font-medium">
                    {n}º bim.
                  </th>
                ))}
                <th className="w-16 py-2 text-center font-medium">Média</th>
              </tr>
            </thead>
            <tbody>
              {b.disciplinas.map((d) => (
                <tr key={d.disciplina} className="border-b border-slate-100 last:border-0">
                  <td className="py-2 text-slate-800">{d.disciplina}</td>
                  {d.notas.map((n, i) => (
                    <td key={i} className="py-2 text-center tabular-nums text-slate-700">
                      {n === null ? <span className="text-slate-300">—</span> : decimal(n)}
                    </td>
                  ))}
                  <td className="py-2 text-center font-semibold tabular-nums text-slate-900">{d.mediaParcial === null ? '—' : decimal(d.mediaParcial)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="mt-3 text-xs text-slate-500">
            Média parcial dos {b.bimestresLancados} bimestres lançados
            {b.mediaGeral !== null && <> · média geral {decimal(b.mediaGeral)}</>}.
          </p>
        </div>
      )}
    </Card>
  )
}

function Materiais({ alunoId }: { alunoId: string }) {
  const materiais = useMateriaisDoFilho(alunoId)
  const semMatricula = materiais.error instanceof ApiError && materiais.error.status === 404
  return (
    <section aria-labelledby="titulo-materiais">
      <h2 id="titulo-materiais" className="mb-3 text-base font-semibold text-slate-800">
        Lista de materiais
      </h2>
      {materiais.isPending ? (
        <Carregando />
      ) : semMatricula ? (
        <Card>
          <p className="text-sm text-slate-600">Disponível quando houver matrícula vigente.</p>
        </Card>
      ) : materiais.isError ? (
        <EstadoErro erro={materiais.error} />
      ) : (
        <Card>
          <p className="mb-2 text-sm text-slate-500">
            {materiais.data.serie} · {materiais.data.ano}
          </p>
          {materiais.data.itens.length === 0 ? (
            <p className="text-sm text-slate-600">A escola ainda não publicou a lista desta série.</p>
          ) : (
            <ul className="grid gap-x-6 sm:grid-cols-2">
              {materiais.data.itens.map((i) => (
                <li key={i.id} className="flex justify-between gap-3 border-b border-slate-100 py-1.5 text-sm">
                  <span className="text-slate-800">{i.descricao}</span>
                  <span className="tabular-nums text-slate-500">{i.quantidade}×</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      )}
    </section>
  )
}

function decimal(n: number) {
  return Number(n).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}
