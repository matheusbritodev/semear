import { useState } from 'react'
import { Link } from 'react-router'
import { CalendarCheck, ClipboardCheck } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { useRegistrarRevisao, useRevisaoAcessos } from '@/hooks/useControle'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { formatarDataHora, type TipoAlertaAcesso } from '@/models/controle'
import { ROTULO_PERFIL } from '@/models/enums'

/** Alertas com texto longo: quebram linha (o Badge não quebra e estoura a tela no celular). */
const COR_ALERTA: Record<TipoAlertaAcesso, string> = {
  FUNCIONARIO_DESLIGADO: 'bg-red-50 text-red-700 ring-red-200',
  SEM_VINCULO: 'bg-red-50 text-red-700 ring-red-200',
  CONFLITO_SOD: 'bg-red-50 text-red-700 ring-red-200',
  NUNCA_ACESSOU: 'bg-amber-50 text-amber-800 ring-amber-200',
  SEM_ACESSO_RECENTE: 'bg-amber-50 text-amber-800 ring-amber-200',
  PERFIL_PRIVILEGIADO: 'bg-sky-50 text-sky-700 ring-sky-200',
}

/**
 * Revisão trimestral de acessos (SOX): o gestor confere quem tem login na Área da escola, corrige o que
 * for preciso em Usuários e registra que revisou. A cada 90 dias a revisão volta a vencer.
 */
export function RevisaoAcessosPage() {
  const consulta = useRevisaoAcessos()
  const registrar = useRegistrarRevisao()
  const podeRegistrar = usePermissao('USUARIOS', 'GRAVAR')
  const [confirmando, setConfirmando] = useState(false)
  const [observacao, setObservacao] = useState('')

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
  const r = consulta.data

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <PageHeader
        titulo="Revisão de acessos"
        descricao="A cada trimestre, confira se cada pessoa com login da equipe ainda precisa do acesso que tem. Corrija na tela de Usuários e registre a revisão aqui."
        acoes={
          podeRegistrar && (
            <Button onClick={() => setConfirmando(true)}>
              <ClipboardCheck className="size-4" aria-hidden /> Registrar revisão
            </Button>
          )
        }
      />

      <div
        className={`flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${r.atrasada ? 'border-amber-200 bg-amber-50 text-amber-900' : 'border-semear-200 bg-semear-50 text-semear-900'}`}
      >
        <CalendarCheck className="size-5 shrink-0" aria-hidden />
        <p>
          {r.ultimaRevisao ? <>Última revisão em {formatarData(r.ultimaRevisao)}. </> : <>Nenhuma revisão registrada ainda. </>}
          {r.atrasada ? (
            <strong>A revisão está vencida: faça hoje.</strong>
          ) : (
            <>Próxima até {formatarData(r.proximaRevisao)}.</>
          )}
        </p>
      </div>

      <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Indicador rotulo="Logins da equipe" valor={r.contas.filter((c) => c.ativo).length} />
        <Indicador rotulo="Com alerta" valor={r.contasComAlerta} />
        <Indicador rotulo="Alertas" valor={r.totalAlertas} />
        <Indicador rotulo="Famílias com login" valor={r.contasPortalAtivas} />
      </dl>

      <Card titulo="Contas da equipe">
        <ul className="divide-y divide-slate-100">
          {r.contas.map((c) => (
            <li key={c.usuarioId} className={`py-3 text-sm ${c.ativo ? '' : 'opacity-60'}`}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="min-w-0">
                  <strong className="font-medium text-slate-900">{c.nome ?? c.email}</strong>
                  <span className="block text-slate-500 break-all">
                    {c.email} · {ROTULO_PERFIL[c.perfil]}
                  </span>
                </span>
                <span className="text-right text-xs text-slate-500">
                  {c.ativo ? (c.ultimoAcesso ? <>Último acesso {formatarDataHora(c.ultimoAcesso)}</> : 'Nunca acessou') : 'Desativado'}
                </span>
              </div>
              {c.alertas.length > 0 && (
                <ul className="mt-2 flex flex-wrap gap-2">
                  {c.alertas.map((a) => (
                    <li key={a.tipo + a.detalhe} className={`rounded-lg px-2 py-1 text-xs font-medium ring-1 ring-inset ${COR_ALERTA[a.tipo]}`}>
                      {a.detalhe}
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
        <p className="mt-3 text-xs text-slate-500">
          Para desativar um login ou trocar o perfil, use{' '}
          <Link to="/escola/config/usuarios" className="text-semear-700 hover:underline">
            Usuários
          </Link>
          . Funções incompatíveis entre perfis ficam em{' '}
          <Link to="/escola/config/perfis" className="text-semear-700 hover:underline">
            Perfis de acesso
          </Link>
          .
        </p>
      </Card>

      {r.conflitosPerfis.length > 0 && (
        <Card titulo="Funções acumuladas por perfil">
          {r.conflitosPerfis.some((c) => c.excecaoDocumentada) && (
            <p className="mb-3 text-sm text-slate-600">
              A Administração acumula funções por precisar gerir a escola inteira: exceção aceita, coberta pela dupla aprovação, pela trilha de
              auditoria e por esta revisão.
            </p>
          )}
          <ul className="space-y-3 text-sm">
            {r.conflitosPerfis.map((c) => (
              <li key={c.perfil + c.codigo} className="space-y-1">
                <Badge cor={c.excecaoDocumentada ? 'azul' : 'vermelho'}>
                  {ROTULO_PERFIL[c.perfil]} · {c.codigo}
                  {c.excecaoDocumentada ? ' · exceção aceita' : ' · corrigir'}
                </Badge>
                <p className="text-slate-600">{c.risco}</p>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Card titulo="Histórico de revisões">
        {r.historico.length === 0 ? (
          <p className="text-sm text-slate-500">Nenhuma revisão registrada.</p>
        ) : (
          <ul className="divide-y divide-slate-100 text-sm">
            {r.historico.map((h) => (
              <li key={h.id} className="py-2">
                <strong className="font-medium">{formatarDataHora(h.realizadaEm)}</strong> · {h.realizadaPor} · {h.contasRevisadas} contas,{' '}
                {h.alertas} alertas
                {h.observacao && <p className="text-slate-500">“{h.observacao}”</p>}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal
        aberto={confirmando}
        titulo="Registrar revisão de acessos"
        aoFechar={() => setConfirmando(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setConfirmando(false)}>
              Voltar
            </Button>
            <Button
              carregando={registrar.isPending}
              onClick={() =>
                registrar.mutate(observacao, {
                  onSuccess: () => {
                    setConfirmando(false)
                    setObservacao('')
                  },
                })
              }
            >
              Registrar
            </Button>
          </>
        }
      >
        <div className="space-y-2 text-sm">
          <p className="text-slate-600">
            Você confirma que conferiu as {r.contas.length} contas da equipe e corrigiu o que era preciso. Fica guardada uma foto das contas e
            dos alertas de hoje.
          </p>
          <label htmlFor="obs-revisao" className="block font-medium text-slate-700">
            Observação (opcional)
          </label>
          <textarea
            id="obs-revisao"
            rows={3}
            maxLength={1000}
            className="w-full rounded-lg border border-slate-300 px-3 py-2"
            placeholder="Ex.: desativado o login de quem saiu em setembro."
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
          />
          {registrar.isError && (
            <p role="alert" className="text-red-600">
              {registrar.error instanceof ApiError ? registrar.error.message : 'Não foi possível registrar.'}
            </p>
          )}
        </div>
      </Modal>
    </div>
  )
}

function Indicador({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className="text-2xl font-semibold text-slate-900 tabular-nums">{valor}</dd>
    </div>
  )
}
