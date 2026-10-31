import { useState } from 'react'
import { Plus } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useBolsas, useBolsasDaMatricula, useConcederBolsa, useRevogarBolsa } from '@/hooks/useFinanceiro'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { descreverDesconto, type Concessao } from '@/models/financeiro'

/** Bolsas da matrícula (RN11): conceder e revogar. O banco recalcula as cobranças em aberto na hora. */
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
          {podeEditar && vigente && (
            <Button variante="secundario" className="mt-3" onClick={() => setEscolhendo(true)} disabled={tipos.isSuccess && disponiveis.length === 0}>
              <Plus className="size-4" aria-hidden /> Conceder bolsa
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
        titulo="Conceder bolsa"
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
              Conceder
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
            Vale de hoje (ou do início do ano letivo) até o fim do ano letivo. As cobranças mensais em aberto são recalculadas na hora; as pagas
            não mudam.
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
