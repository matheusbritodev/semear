import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft, CheckCircle2, FlaskConical } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { StatusCobrancaBadge } from '@/components/comuns/StatusBadges'
import { ComposicaoValor } from '@/components/financeiro/ComposicaoValor'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { useMinhaCobranca, useMinhasCobrancas, useSimularPagamento } from '@/hooks/useFinanceiro'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { ROTULO_ORIGEM, ROTULO_TIPO_COBRANCA, formatarCompetencia } from '@/models/financeiro'

/**
 * Boleto da cobrança. Com o Cora simulado (projeto acadêmico), o botão faz o papel do banco
 * confirmando o pagamento — o mesmo caminho do webhook real.
 */
export function CobrancaPortalPage() {
  const { id = '' } = useParams()
  const consulta = useMinhaCobranca(id)
  const lista = useMinhasCobrancas()
  const simular = useSimularPagamento()
  const [confirmar, setConfirmar] = useState(false)

  const voltar = (
    <Link to="/portal/financeiro" className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
      <ArrowLeft className="size-4" aria-hidden /> Financeiro
    </Link>
  )

  if (consulta.isPending) return <Carregando />
  if (consulta.isError)
    return (
      <div>
        {voltar}
        <EstadoErro erro={consulta.error} />
      </div>
    )

  const { cobranca: c, composicao } = consulta.data
  const aberta = c.status === 'PENDENTE' || c.status === 'VENCIDO'
  const simulado = lista.data?.pagamentoSimulado ?? false

  return (
    <div className="mx-auto max-w-xl">
      {voltar}
      <Card>
        <div className="mb-4 flex items-start justify-between gap-2">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">
              {ROTULO_TIPO_COBRANCA[c.tipo]} · {formatarCompetencia(c.competencia)}
            </h1>
            <p className="text-sm text-slate-500">
              {c.aluno} · {c.turma} ({c.ano})
            </p>
          </div>
          <StatusCobrancaBadge status={c.status} />
        </div>

        <p className="mb-4 text-sm text-slate-600">
          Vencimento <strong>{formatarData(c.vencimento)}</strong>
          {c.pagoEm && (
            <>
              {' '}
              · pago em <strong>{formatarData(c.pagoEm)}</strong> ({c.origemPagamento && ROTULO_ORIGEM[c.origemPagamento]})
            </>
          )}
        </p>

        <ComposicaoValor cobranca={c} composicao={composicao} />

        {c.status === 'PAGO' && (
          <p role="status" className="mt-4 flex items-center gap-2 rounded-lg bg-semear-50 px-3 py-2 text-sm text-semear-800">
            <CheckCircle2 className="size-4" aria-hidden /> Pagamento confirmado. Obrigado!
          </p>
        )}

        {aberta && !c.temBoleto && (
          <p className="mt-4 rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-600">O boleto está sendo emitido. Volte em alguns minutos.</p>
        )}

        {aberta && c.temBoleto && simulado && (
          <div className="mt-4 space-y-3 rounded-lg border border-dashed border-amber-300 bg-amber-50 p-3">
            <p className="flex items-start gap-2 text-sm text-amber-900">
              <FlaskConical className="mt-0.5 size-4 shrink-0" aria-hidden />
              Ambiente de demonstração: o banco Cora é simulado. Use o botão para fazer de conta que o boleto foi pago; a escola recebe o aviso
              como se viesse do banco.
            </p>
            {confirmar ? (
              <div className="flex flex-wrap gap-2">
                <Button variante="secundario" onClick={() => setConfirmar(false)}>
                  Voltar
                </Button>
                <Button carregando={simular.isPending} onClick={() => simular.mutate(c.id, { onSuccess: () => setConfirmar(false) })}>
                  Confirmar pagamento simulado
                </Button>
              </div>
            ) : (
              <Button onClick={() => setConfirmar(true)}>Simular pagamento do boleto</Button>
            )}
          </div>
        )}

        {aberta && c.temBoleto && !simulado && c.linkPagamento && (
          <a
            href={c.linkPagamento}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-4 inline-flex items-center rounded-lg bg-semear-600 px-4 py-2 text-sm font-medium text-white hover:brightness-90"
          >
            Abrir boleto
          </a>
        )}

        {simular.isError && (
          <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {simular.error instanceof ApiError ? simular.error.message : 'Não foi possível concluir o pagamento.'}
          </p>
        )}
      </Card>
    </div>
  )
}
