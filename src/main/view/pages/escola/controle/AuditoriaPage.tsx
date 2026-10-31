import { useState } from 'react'
import { ShieldAlert, ShieldCheck } from '@/components/ui/icones'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Paginacao } from '@/components/comuns/Paginacao'
import { Badge, type CorBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useAuditoria, useOpcoesAuditoria, useVerificarAuditoria } from '@/hooks/useControle'
import { formatarDataHora, ROTULO_OPERACAO, type FiltroAuditoria, type RegistroAuditoria } from '@/models/controle'

const COR_OPERACAO: Record<RegistroAuditoria['operacao'], CorBadge> = { INSERT: 'verde', UPDATE: 'azul', DELETE: 'vermelho' }

/** Campos que dizem mais aparecem primeiro; o resto segue em ordem alfabética. */
const PRIORIDADE = ['resumo', 'status', 'tipo', 'nome', 'valor', 'valor_final', 'valor_mensal', 'valor_pago', 'perfil', 'ativo', 'motivo']
const peso = (campo: string) => {
  const i = PRIORIDADE.indexOf(campo)
  return i === -1 ? PRIORIDADE.length : i
}

/**
 * Trilha de auditoria (SOX): toda mudança em dinheiro, acessos e contratos, com quem fez, quando,
 * o antes e o depois. Ninguém altera a trilha; a verificação recalcula a cadeia de hashes para provar.
 */
export function AuditoriaPage() {
  const [filtro, setFiltro] = useState<FiltroAuditoria>({ pagina: 0 })
  const consulta = useAuditoria(filtro)
  const { autores, tabelas } = useOpcoesAuditoria()
  const verificar = useVerificarAuditoria()
  const mudar = (parcial: Partial<FiltroAuditoria>) => setFiltro((f) => ({ ...f, ...parcial, pagina: parcial.pagina ?? 0 }))
  const v = verificar.data

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Auditoria"
        descricao="Registro automático de toda mudança em cobranças, bolsas, preços, matrículas, contratos, logins e perfis de acesso. Ninguém consegue alterar ou apagar estes registros."
        acoes={
          <Button variante="secundario" carregando={verificar.isPending} onClick={() => verificar.mutate()}>
            <ShieldCheck className="size-4" aria-hidden /> Verificar integridade
          </Button>
        }
      />

      {v && (
        <div
          role="status"
          className={`mb-4 flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${v.integra ? 'border-semear-200 bg-semear-50 text-semear-900' : 'border-red-200 bg-red-50 text-red-900'}`}
        >
          {v.integra ? (
            <ShieldCheck className="size-5 shrink-0 text-semear-600" aria-hidden />
          ) : (
            <ShieldAlert className="size-5 shrink-0 text-red-600" aria-hidden />
          )}
          <div className="min-w-0">
            <p className="font-medium">{v.integra ? 'Trilha íntegra' : 'Trilha adulterada'}</p>
            <p>{v.mensagem}</p>
            <p className="mt-1 text-xs break-all opacity-80">
              Conferida em {formatarDataHora(v.verificadaEm)} · {v.ancorasConferidas} âncora(s) de revisões anteriores · último
              registro #{v.ultimoId} · hash {v.ultimoHash.slice(0, 16)}…
            </p>
          </div>
        </div>
      )}
      {verificar.isError && (
        <p role="alert" className="mb-4 text-sm text-red-600">
          Não foi possível verificar agora.
        </p>
      )}

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <Select rotulo="O quê" value={filtro.tabela ?? ''} onChange={(e) => mudar({ tabela: e.target.value || undefined })}>
          <option value="">Tudo</option>
          {(tabelas.data ?? []).map((t) => (
            <option key={t.tabela} value={t.tabela}>
              {t.rotulo}
            </option>
          ))}
        </Select>
        <Select rotulo="Quem" value={filtro.usuarioId ?? ''} onChange={(e) => mudar({ usuarioId: e.target.value || undefined })}>
          <option value="">Todos</option>
          {(autores.data ?? []).map((a) => (
            <option key={a.usuarioId} value={a.usuarioId}>
              {a.nome}
            </option>
          ))}
        </Select>
        <Input rotulo="De" type="date" value={filtro.de ?? ''} onChange={(e) => mudar({ de: e.target.value || undefined })} />
        <Input rotulo="Até" type="date" value={filtro.ate ?? ''} onChange={(e) => mudar({ ate: e.target.value || undefined })} />
        <Input
          rotulo="Código do registro"
          placeholder="id (uuid)"
          value={filtro.registroId ?? ''}
          onChange={(e) => mudar({ registroId: e.target.value.trim() || undefined })}
        />
      </div>

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.itens.length === 0 ? (
        <EstadoVazio titulo="Nenhum registro" descricao="Nada foi alterado com esses filtros." />
      ) : (
        <>
          <Card>
            <ul className="divide-y divide-slate-100">
              {consulta.data.itens.map((r) => (
                <Registro key={r.id} r={r} aoFiltrarRegistro={(id) => mudar({ registroId: id })} />
              ))}
            </ul>
          </Card>
          <Paginacao
            pagina={consulta.data.pagina}
            tamanho={consulta.data.tamanho}
            total={consulta.data.total}
            aoMudar={(pagina) => setFiltro((f) => ({ ...f, pagina }))}
          />
        </>
      )}
    </div>
  )
}

function Registro({ r, aoFiltrarRegistro }: { r: RegistroAuditoria; aoFiltrarRegistro: (id: string) => void }) {
  const [aberto, setAberto] = useState(false)
  const ordenadas = [...r.alteracoes].sort((a, b) => peso(a.campo) - peso(b.campo))
  const visiveis = aberto ? ordenadas : ordenadas.slice(0, 4)

  return (
    <li className="py-3 text-sm">
      <div className="flex flex-wrap items-center gap-2">
        <Badge cor={COR_OPERACAO[r.operacao]}>{ROTULO_OPERACAO[r.operacao]}</Badge>
        <span className="font-medium text-slate-900">{r.tabelaRotulo}</span>
        <span className="text-slate-500">
          por <strong className="font-medium text-slate-700">{r.autor}</strong> em {formatarDataHora(r.ocorridoEm)}
        </span>
        <span className="ml-auto text-xs text-slate-400">#{r.id}</span>
      </div>
      {r.registroId && (
        <button
          type="button"
          className="mt-1 text-xs break-all text-slate-500 hover:text-semear-700 hover:underline"
          onClick={() => aoFiltrarRegistro(r.registroId!)}
          title="Ver todo o histórico deste registro"
        >
          registro {r.registroId}
        </button>
      )}
      {visiveis.length > 0 && (
        <dl className="mt-2 grid gap-x-4 gap-y-1 rounded-lg bg-slate-50 p-2 text-xs sm:grid-cols-[minmax(8rem,auto)_1fr]">
          {visiveis.map((a) => (
            <div key={a.campo} className="contents">
              <dt className="font-mono text-slate-500">{a.campo}</dt>
              <dd className="min-w-0 break-words text-slate-700">
                {r.operacao === 'UPDATE' ? (
                  <>
                    <span className="text-red-700 line-through decoration-red-300">{a.antes}</span>{' '}
                    <span aria-hidden>→</span> <span className="text-semear-800">{a.depois}</span>
                  </>
                ) : (
                  (a.depois ?? a.antes)
                )}
              </dd>
            </div>
          ))}
        </dl>
      )}
      {r.alteracoes.length > 4 && (
        <button type="button" className="mt-1 text-xs font-medium text-semear-700 hover:underline" onClick={() => setAberto(!aberto)}>
          {aberto ? 'Mostrar menos' : `Mostrar todos os ${r.alteracoes.length} campos`}
        </button>
      )}
    </li>
  )
}
