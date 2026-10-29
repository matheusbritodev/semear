import { Link } from 'react-router'
import { BookOpen, CalendarCheck, FileText, Plus, Wallet } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { StatusPreMatriculaBadge } from '@/components/comuns/StatusBadges'
import { Badge } from '@/components/ui/Badge'
import { Card } from '@/components/ui/Card'
import { useFilhos, useMinhasPreMatriculas } from '@/hooks/usePortal'
import { formatarData } from '@/lib/formatters'
import { CompletarCadastro } from './CompletarCadastro'

export function InicioPortalPage() {
  const { sessao } = useAuth()
  const completo = sessao?.cadastroCompleto ?? false
  const filhos = useFilhos(completo)
  const pedidos = useMinhasPreMatriculas(completo)

  if (!completo) return <CompletarCadastro />

  const primeiroNome = sessao?.nome?.split(' ')[0]

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-semibold text-slate-900">Olá{primeiroNome ? `, ${primeiroNome}` : ''}!</h1>

      <section aria-labelledby="titulo-filhos">
        <h2 id="titulo-filhos" className="mb-3 text-base font-semibold text-slate-800">
          Seus filhos
        </h2>
        {filhos.isPending ? (
          <Carregando />
        ) : filhos.isError ? (
          <EstadoErro erro={filhos.error} aoTentarDeNovo={() => void filhos.refetch()} />
        ) : filhos.data.length === 0 ? (
          <Card>
            <p className="text-sm text-slate-600">Nenhum aluno ligado ao seu cadastro ainda.</p>
          </Card>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {filhos.data.map((f) => (
              <Card key={f.alunoId}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <Link to={`/portal/alunos/${f.alunoId}`} className="font-semibold text-slate-900 hover:text-semear-700 hover:underline">
                      {f.nome}
                    </Link>
                    <p className="text-sm text-slate-500">{f.turmaAtual ?? 'Sem matrícula'}</p>
                  </div>
                  {f.statusMatricula && <StatusMatriculaBadge status={f.statusMatricula} />}
                </div>
                {f.financeiro && (
                  <p className="mt-3">
                    <Badge cor="azul">
                      <Wallet className="size-3" aria-hidden /> Você é o responsável financeiro
                    </Badge>
                  </p>
                )}
                {f.contratosPendentes > 0 && (
                  <Link
                    to={`/portal/alunos/${f.alunoId}#titulo-contratos`}
                    className="mt-3 block rounded-lg bg-semear-50 px-3 py-2 text-sm font-medium text-semear-800 hover:bg-semear-100"
                  >
                    {f.contratosPendentes === 1 ? 'Há um documento' : `Há ${f.contratosPendentes} documentos`} da matrícula para assinar →
                  </Link>
                )}
                {f.documentosPendentes > 0 && (
                  <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
                    Faltam <strong>{f.documentosPendentes}</strong> {f.documentosPendentes === 1 ? 'documento' : 'documentos'} da
                    matrícula. A vaga está garantida, mas a escola precisa deles.
                  </p>
                )}
                <div className="mt-3 flex flex-col gap-1.5">
                  <Link
                    to={`/portal/alunos/${f.alunoId}`}
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-semear-700 hover:underline"
                  >
                    <BookOpen className="size-4" aria-hidden /> Boletim, contrato e materiais
                  </Link>
                  <Link
                    to={`/portal/alunos/${f.alunoId}/documentos`}
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-semear-700 hover:underline"
                  >
                    <FileText className="size-4" aria-hidden /> Documentos da matrícula
                  </Link>
                </div>
              </Card>
            ))}
          </div>
        )}
      </section>

      <div className="grid gap-3 sm:grid-cols-2">
        <Link
          to="/portal/pre-matricula/nova"
          className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-xs hover:border-semear-200"
        >
          <Plus className="size-5 text-semear-600" aria-hidden />
          <span>
            <span className="block font-medium text-slate-900">Nova pré-matrícula</span>
            <span className="text-sm text-slate-500">Para uma criança que ainda não estuda na escola</span>
          </span>
        </Link>
        <Link
          to="/portal/rematricula"
          className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-xs hover:border-semear-200"
        >
          <CalendarCheck className="size-5 text-semear-600" aria-hidden />
          <span>
            <span className="block font-medium text-slate-900">Rematrícula</span>
            <span className="text-sm text-slate-500">Garanta a vaga dos seus filhos no próximo ano</span>
          </span>
        </Link>
      </div>

      {pedidos.data && pedidos.data.length > 0 && (
        <section aria-labelledby="titulo-pedidos">
          <h2 id="titulo-pedidos" className="mb-3 text-base font-semibold text-slate-800">
            Pré-matrículas enviadas
          </h2>
          <ul className="space-y-2">
            {pedidos.data.map((p) => (
              <li key={p.id}>
                <Card>
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-medium text-slate-900">{p.alunoNome}</p>
                      <p className="text-sm text-slate-500">
                        {p.serie} · {p.ano} · enviado em {formatarData(p.criadoEm)}
                      </p>
                    </div>
                    <StatusPreMatriculaBadge status={p.status} />
                  </div>
                  {p.status === 'REJEITADA' && p.motivoRejeicao && (
                    <p className="mt-2 text-sm text-slate-600">
                      <strong>Motivo:</strong> {p.motivoRejeicao}
                    </p>
                  )}
                  {p.status === 'APROVADA' && (
                    <p className="mt-2 text-sm text-slate-600">Aprovada! A vaga é confirmada com o pagamento da matrícula.</p>
                  )}
                </Card>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
