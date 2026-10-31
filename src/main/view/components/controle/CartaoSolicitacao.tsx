import { useState } from 'react'
import { Link } from 'react-router'
import { Check, X } from '@/components/ui/icones'
import { Badge, type CorBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { useDecidirAprovacao } from '@/hooks/useControle'
import { ApiError } from '@/lib/ApiError'
import { formatarDataHora, ROTULO_STATUS_SOLICITACAO, type Solicitacao, type StatusSolicitacao } from '@/models/controle'

const COR_STATUS: Record<StatusSolicitacao, CorBadge> = {
  PENDENTE: 'amarelo',
  APROVADA: 'verde',
  REJEITADA: 'vermelho',
  CANCELADA: 'cinza',
}

/** Um pedido de aprovação, com os botões que valem para quem está vendo. */
export function CartaoSolicitacao({ s, compacto = false }: { s: Solicitacao; compacto?: boolean }) {
  const { aprovar, rejeitar, cancelar } = useDecidirAprovacao()
  const [recusando, setRecusando] = useState(false)
  const [motivo, setMotivo] = useState('')
  const erro = aprovar.error ?? cancelar.error

  return (
    <li className="space-y-2 py-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium tracking-wide text-slate-500 uppercase">{s.tipoRotulo}</p>
          <p className="font-medium text-slate-900">{s.resumo}</p>
          <p className="text-sm text-slate-500">
            Pedido por {s.minha ? 'você' : s.solicitante} em {formatarDataHora(s.solicitadaEm)}
            {s.matriculaId && !compacto && (
              <>
                {' · '}
                <Link to={`/escola/matriculas/${s.matriculaId}`} className="text-semear-700 hover:underline">
                  ver matrícula
                </Link>
              </>
            )}
          </p>
          {s.status !== 'PENDENTE' && s.decididaEm && (
            <p className="text-sm text-slate-500">
              {ROTULO_STATUS_SOLICITACAO[s.status]} por {s.decisor} em {formatarDataHora(s.decididaEm)}
              {s.motivo && <> · motivo: “{s.motivo}”</>}
            </p>
          )}
        </div>
        <Badge cor={COR_STATUS[s.status]}>{ROTULO_STATUS_SOLICITACAO[s.status]}</Badge>
      </div>

      {s.status === 'PENDENTE' && (
        <div className="flex flex-wrap items-center gap-2">
          {s.podeDecidir && (
            <>
              <Button carregando={aprovar.isPending} onClick={() => aprovar.mutate(s.id)}>
                <Check className="size-4" aria-hidden /> Aprovar
              </Button>
              <Button variante="secundario" onClick={() => setRecusando(true)}>
                <X className="size-4" aria-hidden /> Recusar
              </Button>
            </>
          )}
          {s.minha && (
            <>
              <span className="text-sm text-slate-500">Aguardando outra pessoa aprovar.</span>
              <Button variante="fantasma" carregando={cancelar.isPending} onClick={() => cancelar.mutate(s.id)}>
                Cancelar pedido
              </Button>
            </>
          )}
        </div>
      )}
      {erro && (
        <p role="alert" className="text-sm text-red-600">
          {erro instanceof ApiError ? erro.message : 'Não foi possível concluir.'}
        </p>
      )}

      <Modal
        aberto={recusando}
        titulo="Recusar pedido"
        aoFechar={() => setRecusando(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setRecusando(false)}>
              Voltar
            </Button>
            <Button
              disabled={motivo.trim() === ''}
              carregando={rejeitar.isPending}
              onClick={() => rejeitar.mutate({ id: s.id, motivo }, { onSuccess: () => setRecusando(false) })}
            >
              Recusar
            </Button>
          </>
        }
      >
        <div className="space-y-2">
          <p className="text-sm text-slate-600">{s.resumo}</p>
          <label className="block text-sm font-medium text-slate-700" htmlFor={`motivo-${s.id}`}>
            Motivo (quem pediu vai ver)
          </label>
          <textarea
            id={`motivo-${s.id}`}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            rows={3}
            maxLength={500}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
          />
          {rejeitar.isError && (
            <p role="alert" className="text-sm text-red-600">
              {rejeitar.error instanceof ApiError ? rejeitar.error.message : 'Não foi possível recusar.'}
            </p>
          )}
        </div>
      </Modal>
    </li>
  )
}
