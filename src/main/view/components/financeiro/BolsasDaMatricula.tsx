import { useState } from 'react'
import { Plus } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { CartaoSolicitacao } from '@/components/controle/CartaoSolicitacao'
import { useAprovacoes } from '@/hooks/useControle'
import { useBolsas, useBolsasDaMatricula, useConcederBolsa, useRevogarBolsa } from '@/hooks/useFinanceiro'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { descreverDesconto, type Concessao } from '@/models/financeiro'

/**
 * Bolsas da matrícula (RN11). Conceder vira pedido que outra pessoa aprova (segregação de funções);
 * revogar vale na hora. O banco recalcula as cobranças em aberto quando a bolsa entra ou sai.
 */
export function BolsasDaMatricula({ matriculaId, vigente }: { matriculaId: string; vigente: boolean }) {
  const podeVer = usePermissao('BOLSAS', 'LER')
  const podeEditar = usePermissao('BOLSAS', 'GRAVAR')
  const concessoes = useBolsasDaMatricula(matriculaId, podeVer)
  const tipos = useBolsas(podeEditar && vigente)
  const conceder = useConcederBolsa(matriculaId)
  const revogar = useRevogarBolsa(matriculaId)
  const [escolhendo, setEscolhendo] = useState(false)
  const [bolsaId, setBolsaId] = useState('')
  const [revogando, setRevogando] = useState<Concessao | null>(null)
  const podeVerPedidos = usePermissao('APROVACOES', 'LER') || podeEditar
  const pedidos = useAprovacoes('PENDENTE', podeVer && podeVerPedidos)
  const pendentes = (pedidos.data ?? []).filter((p) => p.matriculaId === matriculaId)

  if (!podeVer) return null

  const jaTem = new Set((concessoes.data ?? []).filter((c) => !c.vigenciaFim || c.vigente).map((c) => c.bolsaId))
  const disponiveis = (tipos.data ?? []).filter((b) => b.ativa && !jaTem.has(b.id))
  const erro = conceder.error ?? revogar.error

  return (
    <Card titulo="Bolsas" className="md:col-span-2">
      {concessoes.isPending ? (
        <Carregando />
      ) : concessoes.isError ? (
        <EstadoErro erro={concessoes.error} aoTentarDeNovo={() => void concessoes.refetch()} />
      ) : (
        <>
          {concessoes.data.length === 0 ? (
            <p className="text-sm text-slate-500">Sem bolsa nesta matrícula.</p>
          ) : (
            <ul className="divide-y divide-slate-100 text-sm">
              {concessoes.data.map((c) => (
                <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <span>
                    <strong className="font-medium">{c.bolsa}</strong>{' '}
                    <span className="text-slate-500">
                      ({descreverDesconto(c.tipoDesconto, c.valor)}) · {formatarData(c.vigenciaInicio)} –{' '}
                      {c.vigenciaFim ? formatarData(c.vigenciaFim) : 'sem fim'}
                    </span>
                  </span>
                  <span className="flex items-center gap-2">
                    {c.vigente ? <Badge cor="verde">Vigente</Badge> : <Badge>{c.bolsaAtiva ? 'Encerrada' : 'Bolsa desativada'}</Badge>}
                    {podeEditar && c.vigente && (
                      <Button variante="fantasma" onClick={() => setRevogando(c)}>
                        Revogar
                      </Button>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {pendentes.length > 0 && (
            <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50/60 px-3">
              <ul className="divide-y divide-amber-100">
                {pendentes.map((p) => (
                  <CartaoSolicitacao key={p.id} s={p} compacto />
                ))}
              </ul>
            </div>
          )}
          {conceder.isSuccess && (
            <p role="status" className="mt-3 text-sm text-semear-700">
              Pedido enviado. A bolsa entra nas cobranças quando outra pessoa aprovar.
            </p>
          )}
          {podeEditar && vigente && (
            <Button variante="secundario" className="mt-3" onClick={() => setEscolhendo(true)} disabled={tipos.isSuccess && disponiveis.length === 0}>
              <Plus className="size-4" aria-hidden /> Pedir bolsa
            </Button>
          )}
          {erro && (
            <p role="alert" className="mt-2 text-sm text-red-600">
              {erro instanceof ApiError ? erro.message : 'Não foi possível concluir.'}
            </p>
          )}
        </>
      )}

      <Modal
        aberto={escolhendo}
        titulo="Pedir bolsa"
        aoFechar={() => setEscolhendo(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setEscolhendo(false)}>
              Cancelar
            </Button>
            <Button
              disabled={!bolsaId}
              carregando={conceder.isPending}
              onClick={() => conceder.mutate(bolsaId, { onSuccess: () => (setEscolhendo(false), setBolsaId('')) })}
            >
              Enviar para aprovação
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <Select rotulo="Bolsa" value={bolsaId} onChange={(e) => setBolsaId(e.target.value)}>
            <option value="">Escolha…</option>
            {disponiveis.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome} ({descreverDesconto(b.tipoDesconto, b.valor)})
              </option>
            ))}
          </Select>
          <p className="text-sm text-slate-600">
            Outra pessoa da equipe precisa aprovar (quem pede não aprova). Depois de aprovada, vale de hoje (ou do início do ano letivo) até o fim
            do ano letivo; as cobranças mensais em aberto são recalculadas e as pagas não mudam.
          </p>
        </div>
      </Modal>

      <Modal
        aberto={revogando !== null}
        titulo="Revogar bolsa"
        aoFechar={() => setRevogando(null)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setRevogando(null)}>
              Voltar
            </Button>
            <Button
              carregando={revogar.isPending}
              onClick={() => revogando && revogar.mutate(revogando.id, { onSuccess: () => setRevogando(null) })}
            >
              Revogar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          A bolsa <strong>{revogando?.bolsa}</strong> deixa de valer a partir de hoje. As cobranças em aberto dos meses seguintes voltam ao valor
          cheio.
        </p>
      </Modal>
    </Card>
  )
}
