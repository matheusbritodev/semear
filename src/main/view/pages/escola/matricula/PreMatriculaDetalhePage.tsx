import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Check, X } from 'lucide-react'
import { BarraOcupacao } from '@/components/comuns/BarraOcupacao'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusPreMatriculaBadge } from '@/components/comuns/StatusBadges'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { useAprovarPreMatricula, usePreMatricula, useRejeitarPreMatricula } from '@/hooks/useMatricula'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'

export function PreMatriculaDetalhePage() {
  const { id = '' } = useParams()
  const navegar = useNavigate()
  const consulta = usePreMatricula(id)
  const podeDecidir = usePermissao('MATRICULA', 'GRAVAR')
  const aprovar = useAprovarPreMatricula(id)
  const rejeitar = useRejeitarPreMatricula(id)
  const [turmaId, setTurmaId] = useState<string | null>(null)
  const [rejeitando, setRejeitando] = useState(false)
  const [motivo, setMotivo] = useState('')

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const { preMatricula: p, turmasDisponiveis } = consulta.data
  const detalhe = consulta.data
  const pendente = p.status === 'PENDENTE'
  const erroAprovar = aprovar.error instanceof ApiError ? aprovar.error.mensagemAmigavel : null
  const erroRejeitar = rejeitar.error instanceof ApiError ? (rejeitar.error.campos[0]?.mensagem ?? rejeitar.error.mensagemAmigavel) : null

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo={p.alunoNome}
        descricao={`Pedido de ${formatarData(p.criadoEm)} · ${p.serie} · ${p.ano}`}
        voltarPara={{ rota: '/escola/pre-matriculas', rotulo: 'Pré-matrículas' }}
        acoes={<StatusPreMatriculaBadge status={p.status} />}
      />

      <div className="mb-6 grid gap-4 md:grid-cols-2">
        <Card titulo="Criança">
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <dt className="text-slate-500">Nascimento</dt>
            <dd>
              {formatarData(p.alunoNascimento)} ({p.idade} anos)
            </dd>
            <dt className="text-slate-500">CPF</dt>
            <dd>{detalhe.alunoCpf ?? 'Não informado'}</dd>
            <dt className="text-slate-500">Uniforme</dt>
            <dd>{detalhe.fardaUniforme ?? '—'}</dd>
            <dt className="text-slate-500">Ed. física</dt>
            <dd>{detalhe.fardaEdFisica ?? '—'}</dd>
          </dl>
        </Card>
        <Card titulo="Responsável">
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <dt className="text-slate-500">Nome</dt>
            <dd>
              <Link to={`/escola/responsaveis/${detalhe.responsavelId}`} className="hover:text-semear-700 hover:underline">
                {p.responsavel}
              </Link>{' '}
              ({detalhe.parentesco ?? 'Responsável'})
            </dd>
            <dt className="text-slate-500">Telefone</dt>
            <dd>{p.telefone ?? '—'}</dd>
            <dt className="text-slate-500">E-mail</dt>
            <dd>{detalhe.responsavelEmail ?? '—'}</dd>
          </dl>
        </Card>
      </div>

      {p.status === 'REJEITADA' && (
        <Card titulo="Motivo da recusa">
          <p className="text-sm text-slate-700">{p.motivoRejeicao}</p>
        </Card>
      )}

      {p.status === 'APROVADA' && p.matriculaId && (
        <Card>
          <p className="text-sm text-slate-700">
            Pedido aprovado.{' '}
            <Link to={`/escola/matriculas/${p.matriculaId}`} className="font-medium text-semear-700 underline">
              Ver matrícula
            </Link>
          </p>
        </Card>
      )}

      {pendente && podeDecidir && (
        <section aria-labelledby="titulo-turma">
          <h2 id="titulo-turma" className="mb-3 text-base font-semibold text-slate-800">
            Escolha a turma
          </h2>
          {turmasDisponiveis.length === 0 ? (
            <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              Ainda não há turma de {p.serie} aberta para {p.ano}.
            </p>
          ) : (
            <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3" role="radiogroup" aria-label="Turmas">
              {turmasDisponiveis.map((t) => {
                const selecionada = turmaId === t.id
                return (
                  <button
                    key={t.id}
                    type="button"
                    role="radio"
                    aria-checked={selecionada}
                    disabled={t.lotada}
                    onClick={() => setTurmaId(t.id)}
                    className={`rounded-xl border bg-white p-4 text-left transition disabled:cursor-not-allowed disabled:opacity-60 ${selecionada ? 'border-semear-600 ring-2 ring-semear-200' : 'border-slate-200 hover:border-semear-200'}`}
                  >
                    <div className="mb-3 flex items-center justify-between">
                      <span className="font-semibold">{t.nomeCompleto}</span>
                      {t.lotada ? <Badge cor="vermelho">Lotada</Badge> : <Badge cor="verde">{t.vagas} vagas</Badge>}
                    </div>
                    <BarraOcupacao ocupadas={t.ocupadas} capacidade={t.capacidadeMax} />
                  </button>
                )
              })}
            </div>
          )}
          {erroAprovar && (
            <p role="alert" className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {erroAprovar}
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!turmaId}
              carregando={aprovar.isPending}
              onClick={() =>
                turmaId &&
                aprovar.mutate(turmaId, { onSuccess: (r) => r.matriculaId && navegar(`/escola/matriculas/${r.matriculaId}`) })
              }
            >
              <Check className="size-4" aria-hidden /> Aprovar e matricular
            </Button>
            <Button variante="secundario" onClick={() => setRejeitando(true)}>
              <X className="size-4" aria-hidden /> Não aprovar
            </Button>
          </div>
          <p className="mt-2 text-xs text-slate-500">
            Ao aprovar, o sistema cria o cadastro do aluno, a matrícula,{' '}
            {p.nivel === 'EDUCACAO_INFANTIL' ? 'a Ficha de Sócio' : 'o Contrato de Matrícula'} e a cobrança da matrícula. A vaga é
            confirmada com o pagamento.
          </p>
        </section>
      )}

      <Modal
        aberto={rejeitando}
        titulo="Não aprovar o pedido"
        aoFechar={() => setRejeitando(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setRejeitando(false)}>
              Voltar
            </Button>
            <Button carregando={rejeitar.isPending} onClick={() => rejeitar.mutate(motivo, { onSuccess: () => setRejeitando(false) })}>
              Confirmar
            </Button>
          </>
        }
      >
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-slate-700">Motivo (a família verá este texto)</span>
          <textarea
            rows={4}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-semear-500 focus:ring-2 focus:ring-semear-200"
          />
        </label>
        {erroRejeitar && (
          <p role="alert" className="mt-2 text-sm text-red-600">
            {erroRejeitar}
          </p>
        )}
        <p className="mt-2 text-xs text-slate-500">A decisão é definitiva (RD04).</p>
      </Modal>
    </div>
  )
}
