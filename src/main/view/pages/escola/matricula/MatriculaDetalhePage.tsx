import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { Ban, Banknote } from 'lucide-react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { StatusCobrancaBadge, StatusContratoBadge } from '@/components/comuns/StatusBadges'
import { BolsasDaMatricula } from '@/components/financeiro/BolsasDaMatricula'
import { CobrancasDaMatricula } from '@/components/financeiro/CobrancasDaMatricula'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { useCancelarMatricula, useMatricula, useRegistrarPagamentoMatricula } from '@/hooks/useMatricula'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_TIPO_CONTRATO, ROTULO_TIPO_MATRICULA } from '@/models/enums'

export function MatriculaDetalhePage() {
  const { id = '' } = useParams()
  const consulta = useMatricula(id)
  const podeCancelar = usePermissao('MATRICULA', 'GRAVAR')
  const podeReceber = usePermissao('FINANCEIRO', 'GRAVAR')
  const cancelar = useCancelarMatricula(id)
  const pagar = useRegistrarPagamentoMatricula(id)
  const [cancelando, setCancelando] = useState(false)
  const [recebendo, setRecebendo] = useState(false)
  const [motivo, setMotivo] = useState('')

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const d = consulta.data
  const m = d.matricula
  const cobranca = d.cobrancaMatricula
  const cobrancaAberta = cobranca && (cobranca.status === 'PENDENTE' || cobranca.status === 'VENCIDO')
  const podeSerCancelada = m.status === 'ATIVA' || m.status === 'AGUARDANDO_PAGAMENTO'
  const erroCancelar = cancelar.error instanceof ApiError ? (cancelar.error.campos[0]?.mensagem ?? cancelar.error.mensagemAmigavel) : null
  const erroPagar = pagar.error instanceof ApiError ? pagar.error.mensagemAmigavel : null

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo={m.aluno}
        descricao={
          <>
            Matrícula {m.ano} · {ROTULO_TIPO_MATRICULA[m.tipo]} · feita em {formatarData(m.dataMatricula)}
          </>
        }
        voltarPara={{ rota: `/escola/matriculas?ano=${m.ano}`, rotulo: 'Matrículas' }}
        acoes={
          <>
            <StatusMatriculaBadge status={m.status} />
            {podeCancelar && podeSerCancelada && (
              <Button variante="secundario" onClick={() => setCancelando(true)}>
                <Ban className="size-4" aria-hidden /> Cancelar matrícula
              </Button>
            )}
          </>
        }
      />

      {m.status === 'AGUARDANDO_PAGAMENTO' && (
        <p className="mb-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          A vaga está reservada e é confirmada quando a cobrança da matrícula for paga (RN12).
        </p>
      )}
      {d.motivoCancelamento && (
        <p className="mb-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
          <strong>Motivo do cancelamento:</strong> {d.motivoCancelamento}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <Card titulo="Aluno e turma">
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <dt className="text-slate-500">Aluno</dt>
            <dd>
              <Link to={`/escola/alunos/${m.alunoId}`} className="hover:text-semear-700 hover:underline">
                {m.aluno}
              </Link>{' '}
              (nº {m.numeroCadastro})
            </dd>
            <dt className="text-slate-500">Turma</dt>
            <dd>
              <Link to={`/escola/turmas/${m.turmaId}`} className="hover:text-semear-700 hover:underline">
                {m.turma}
              </Link>
            </dd>
            <dt className="text-slate-500">Financeiro</dt>
            <dd>{d.responsavelFinanceiro ?? <Badge cor="vermelho">Não definido</Badge>}</dd>
            <dt className="text-slate-500">Fardas</dt>
            <dd>{d.fardas.length ? d.fardas.map((f) => `${f.tipo === 'UNIFORME' ? 'Uniforme' : 'Ed. física'} ${f.tamanho}`).join(' · ') : '—'}</dd>
            <dt className="text-slate-500">Documentos</dt>
            <dd>
              <Link to={`/escola/alunos/${m.alunoId}#documentos`} className="hover:underline">
                {d.documentacaoCompleta ? (
                  <Badge cor="verde">Completos</Badge>
                ) : (
                  <Badge cor="amarelo">
                    {d.documentosPendentes} pendente{d.documentosPendentes === 1 ? '' : 's'} (RN12)
                  </Badge>
                )}
              </Link>
            </dd>
          </dl>
          {d.possuiDebitosVencidos && (
            <p className="mt-3">
              <Badge cor="vermelho">Há cobranças vencidas</Badge>
            </p>
          )}
        </Card>

        <Card titulo="Cobrança da matrícula">
          {cobranca ? (
            <>
              <div className="mb-3 flex items-center justify-between">
                <span className="text-2xl font-semibold text-slate-900">{formatarMoeda(cobranca.valorFinal)}</span>
                <StatusCobrancaBadge status={cobranca.status} />
              </div>
              <p className="text-sm text-slate-600">
                Vencimento {formatarData(cobranca.vencimento)}
                {cobranca.pagoEm && (
                  <>
                    {' '}
                    · pago em {formatarData(cobranca.pagoEm)} ({cobranca.origemPagamento === 'SITE' ? 'site' : 'na secretaria'})
                  </>
                )}
              </p>
              {cobrancaAberta && podeReceber && (
                <Button className="mt-4" onClick={() => setRecebendo(true)}>
                  <Banknote className="size-4" aria-hidden /> Registrar pagamento na secretaria
                </Button>
              )}
            </>
          ) : (
            <p className="text-sm text-slate-500">Sem cobrança de matrícula.</p>
          )}
        </Card>

        <Card titulo="Documentos de formalização" className="md:col-span-2">
          <ul className="divide-y divide-slate-100 text-sm">
            {d.contratos.map((c) => (
              <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                <Link to={`/escola/contratos/${c.id}`} className="font-medium text-semear-700 hover:underline">
                  {ROTULO_TIPO_CONTRATO[c.tipo]}
                </Link>
                <span className="flex items-center gap-2 text-slate-500">
                  {c.assinadoPor && (
                    <>
                      por {c.assinadoPor} em {c.assinadoEm ? formatarData(c.assinadoEm) : ''}
                    </>
                  )}
                  <StatusContratoBadge status={c.status} />
                </span>
              </li>
            ))}
          </ul>
        </Card>

        <BolsasDaMatricula matriculaId={id} vigente={podeSerCancelada} />
        <CobrancasDaMatricula matriculaId={id} />
      </div>

      <Modal
        aberto={cancelando}
        titulo="Cancelar matrícula"
        aoFechar={() => setCancelando(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setCancelando(false)}>
              Voltar
            </Button>
            <Button carregando={cancelar.isPending} onClick={() => cancelar.mutate(motivo, { onSuccess: () => setCancelando(false) })}>
              Confirmar cancelamento
            </Button>
          </>
        }
      >
        <p className="mb-3 text-sm text-slate-600">
          A vaga é liberada e as cobranças ainda não vencidas são canceladas. Pelo contrato, só é possível cancelar sem débitos
          vencidos (RN13).
        </p>
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-slate-700">Motivo</span>
          <textarea
            rows={3}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-semear-500 focus:ring-2 focus:ring-semear-200"
          />
        </label>
        {erroCancelar && (
          <p role="alert" className="mt-2 text-sm text-red-600">
            {erroCancelar}
          </p>
        )}
      </Modal>

      <Modal
        aberto={recebendo}
        titulo="Registrar pagamento da matrícula"
        aoFechar={() => setRecebendo(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setRecebendo(false)}>
              Voltar
            </Button>
            <Button carregando={pagar.isPending} onClick={() => pagar.mutate('PRESENCIAL', { onSuccess: () => setRecebendo(false) })}>
              Confirmar recebimento
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Confirma o recebimento de <strong>{cobranca ? formatarMoeda(cobranca.valorFinal) : ''}</strong> na secretaria? A matrícula
          passa a ativa (RD02) e o pagamento não pode ser alterado depois (RD05).
        </p>
        {erroPagar && (
          <p role="alert" className="mt-2 text-sm text-red-600">
            {erroPagar}
          </p>
        )}
      </Modal>
    </div>
  )
}
