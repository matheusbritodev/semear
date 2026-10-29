import { useState } from 'react'
import { Link } from 'react-router'
import { StatusCobrancaBadge } from '@/components/comuns/StatusBadges'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { useCobranca, usePagarPresencial } from '@/hooks/useFinanceiro'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_ORIGEM, ROTULO_TIPO_COBRANCA, formatarCompetencia } from '@/models/financeiro'
import { ComposicaoValor } from './ComposicaoValor'

/**
 * Detalhe da cobrança: composição do valor numa data escolhida (simulação para atender a família)
 * e baixa presencial pelo valor de hoje.
 */
export function DetalheCobrancaModal({ id, aoFechar }: { id: string | null; aoFechar: () => void }) {
  const [data, setData] = useState('')
  const consulta = useCobranca(id, data || undefined)
  const pagar = usePagarPresencial()
  const podeBaixar = usePermissao('FINANCEIRO', 'GRAVAR')
  const [confirmar, setConfirmar] = useState(false)

  function fechar() {
    setData('')
    setConfirmar(false)
    pagar.reset()
    aoFechar()
  }

  const c = consulta.data?.cobranca
  const aberta = c && (c.status === 'PENDENTE' || c.status === 'VENCIDO')

  return (
    <Modal
      aberto={id !== null}
      titulo={c ? `${ROTULO_TIPO_COBRANCA[c.tipo]} · ${formatarCompetencia(c.competencia)}` : 'Cobrança'}
      aoFechar={fechar}
      rodape={
        c &&
        aberta &&
        podeBaixar && (
          <>
            {confirmar ? (
              <>
                <Button variante="secundario" onClick={() => setConfirmar(false)}>
                  Voltar
                </Button>
                <Button onClick={() => void pagar.mutateAsync(c.id).then(() => setConfirmar(false))} carregando={pagar.isPending}>
                  Confirmar recebimento de {formatarMoeda(c.valorHoje)}
                </Button>
              </>
            ) : (
              <Button onClick={() => setConfirmar(true)}>Registrar pagamento presencial</Button>
            )}
          </>
        )
      }
    >
      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : (
        c && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <Link to={`/escola/alunos/${c.alunoId}`} className="font-semibold text-slate-900 hover:text-semear-700 hover:underline">
                  {c.aluno}
                </Link>
                <p className="text-sm text-slate-500">
                  {c.turma} ({c.ano}) · responsável: {c.responsavel}
                  {c.telefone && <> · {c.telefone}</>}
                </p>
              </div>
              <StatusCobrancaBadge status={c.status} />
            </div>

            <p className="text-sm text-slate-600">
              Vencimento <strong>{formatarData(c.vencimento)}</strong>
              {c.diasDeAtraso > 0 && <span className="text-red-700"> · {c.diasDeAtraso} dias em atraso</span>}
              {c.pagoEm && (
                <>
                  {' '}
                  · pago em <strong>{formatarData(c.pagoEm)}</strong> ({c.origemPagamento && ROTULO_ORIGEM[c.origemPagamento]}
                  {c.baixaRegistradaPor && `, recebido por ${c.baixaRegistradaPor}`})
                </>
              )}
              {!c.temBoleto && aberta && <> · boleto ainda não emitido</>}
            </p>

            <ComposicaoValor cobranca={c} composicao={consulta.data.composicao} />

            {aberta && (
              <div className="max-w-xs">
                <Input
                  rotulo="Simular pagamento em outra data"
                  type="date"
                  value={data}
                  onChange={(e) => setData(e.target.value)}
                />
              </div>
            )}

            {pagar.isSuccess && (
              <p role="status" className="rounded-lg bg-semear-50 px-3 py-2 text-sm text-semear-800">
                Pagamento registrado.
              </p>
            )}
            {pagar.isError && (
              <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                {pagar.error instanceof ApiError ? pagar.error.message : 'Não foi possível registrar o pagamento.'}
              </p>
            )}
          </div>
        )
      )}
    </Modal>
  )
}
