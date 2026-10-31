import type { CSSProperties, ReactNode } from 'react'
import { Link } from 'react-router'
import {
  AlertTriangle,
  ArrowRight,
  CalendarClock,
  Chalkboard,
  Check,
  FileWarning,
  Landmark,
  Minus,
  ShieldCheck,
  Stamp,
  Student,
  Tray,
  type TipoIcone,
} from '@/components/ui/icones'
import { useAuth } from '@/auth/useAuth'
import { pode } from '@/auth/permissions'
import { AlertasTurmas } from '@/components/comuns/AlertasTurmas'
import { Card } from '@/components/ui/Card'
import { useAnoCorrente, useTurmas } from '@/hooks/useAcademico'
import { useResumoAprovacoes } from '@/hooks/useControle'
import { useInadimplencia } from '@/hooks/useFinanceiro'
import { useAlertasObrigacoes } from '@/hooks/useObrigacoes'
import { useResumoPendencias } from '@/hooks/usePendencias'
import { formatarMoeda } from '@/lib/formatters'
import { MENU_ESCOLA } from '@/layouts/menuEscola'
import { ROTULO_MODULO, ROTULO_PERFIL } from '@/models/enums'

/** Atraso em cascata para os blocos do painel surgirem um após o outro. */
const emCascata = (ordem: number): CSSProperties => ({ animationDelay: `${ordem * 60}ms` })

function saudacao() {
  const hora = new Date().getHours()
  return hora < 12 ? 'Bom dia' : hora < 18 ? 'Boa tarde' : 'Boa noite'
}

export function InicioEscolaPage() {
  const { sessao } = useAuth()
  const veTurmas = pode(sessao, 'TURMAS', 'LER')
  const corrente = useAnoCorrente()
  const turmas = useTurmas(corrente.data?.ano, veTurmas && corrente.data !== undefined)
  const vePendencias = pode(sessao, 'PENDENCIAS', 'LER')
  const pendencias = useResumoPendencias(vePendencias)
  const veFinanceiro = pode(sessao, 'FINANCEIRO', 'LER')
  const inadimplencia = useInadimplencia(veFinanceiro)
  const veObrigacoes = pode(sessao, 'OBRIGACOES', 'LER')
  const obrigacoes = useAlertasObrigacoes(veObrigacoes)
  const decideAprovacoes = pode(sessao, 'APROVACOES', 'GRAVAR')
  const aprovacoes = useResumoAprovacoes(decideAprovacoes)

  if (!sessao?.perfil) return null
  const primeiroNome = sessao.nome?.split(' ')[0]
  const lista = turmas.data ?? []
  const atalhos = MENU_ESCOLA.filter((item) => item.rota && pode(sessao, item.modulo, 'LER'))

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-[28px]">
            {saudacao()}
            {primeiroNome ? `, ${primeiroNome}` : ''}!
          </h1>
          <p className="mt-1 text-slate-500">Veja o que precisa da sua atenção hoje.</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-medium">
          <span className="rounded-full bg-semear-50 px-3 py-1 text-semear-700 ring-1 ring-semear-200/70 ring-inset">
            {ROTULO_PERFIL[sessao.perfil]}
          </span>
          {corrente.data && (
            <span className="rounded-full bg-white px-3 py-1 text-slate-600 ring-1 ring-slate-200 ring-inset">
              Ano letivo {corrente.data.ano}
            </span>
          )}
        </div>
      </div>

      {veTurmas && lista.length > 0 && (
        <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Indicador
            ordem={0}
            icone={Chalkboard}
            rotulo={sessao.perfil === 'PROFESSOR' ? 'Minhas turmas' : 'Turmas'}
            valor={lista.length}
          />
          <Indicador ordem={1} icone={Student} rotulo="Alunos" valor={lista.reduce((s, t) => s + t.ocupadas, 0)} />
          <Indicador ordem={2} icone={Tray} rotulo="Vagas livres" valor={lista.reduce((s, t) => s + t.vagas, 0)} />
          <Indicador
            ordem={3}
            icone={AlertTriangle}
            rotulo="Turmas lotadas"
            valor={lista.filter((t) => t.lotada).length}
            alerta={lista.some((t) => t.lotada)}
          />
        </dl>
      )}

      <AlertasTurmas />

      {decideAprovacoes && aprovacoes.data && aprovacoes.data.pendentes > 0 && (
        <Aviso tom="neutro" icone={Stamp} rota="/escola/aprovacoes" acao="Ver aprovações">
          <strong className="font-semibold text-slate-900">
            {aprovacoes.data.pendentes} {aprovacoes.data.pendentes === 1 ? 'pedido aguardando' : 'pedidos aguardando'} aprovação
          </strong>{' '}
          (bolsas e preços só valem depois que uma segunda pessoa aprova).
        </Aviso>
      )}

      {vePendencias && pendencias.data && pendencias.data.alunosComPendencia > 0 && (
        <Aviso tom="vermelho" icone={FileWarning} rota="/escola/pendencias" acao="Ver pendências">
          <strong className="font-semibold">
            {pendencias.data.alunosComPendencia} {pendencias.data.alunosComPendencia === 1 ? 'aluno' : 'alunos'} com documentos pendentes
          </strong>{' '}
          ({pendencias.data.documentosFaltando} {pendencias.data.documentosFaltando === 1 ? 'documento' : 'documentos'} no total, de{' '}
          {pendencias.data.alunosConsiderados} matrículas vigentes). Cobre as famílias pela lista de pendências.
        </Aviso>
      )}

      {veObrigacoes && obrigacoes.data && obrigacoes.data.length > 0 && (
        <Aviso tom="amarelo" icone={CalendarClock} rota="/escola/obrigacoes" acao="Ver obrigações">
          <strong className="font-semibold">Obrigações com prazo próximo:</strong>{' '}
          {obrigacoes.data
            .map(
              (o) =>
                `${o.orgaoRotulo} (${o.diasRestantes < 0 ? `atrasada há ${-o.diasRestantes} dias` : o.diasRestantes === 0 ? 'vence hoje' : `em ${o.diasRestantes} dias`})`,
            )
            .join(' · ')}
        </Aviso>
      )}

      {veFinanceiro && inadimplencia.data && inadimplencia.data.alunos > 0 && (
        <Aviso tom="neutro" icone={Landmark} rota="/escola/financeiro/inadimplencia" acao="Ver inadimplência">
          <strong className="font-semibold text-slate-900">
            {inadimplencia.data.alunos} {inadimplencia.data.alunos === 1 ? 'aluno inadimplente' : 'alunos inadimplentes'}
          </strong>{' '}
          · {inadimplencia.data.cobrancas} cobranças vencidas · {formatarMoeda(inadimplencia.data.valorAtualizado)} atualizado hoje
        </Aviso>
      )}

      {atalhos.length > 0 && (
        <section aria-labelledby="titulo-atalhos" className="space-y-3">
          <h2 id="titulo-atalhos" className="text-sm font-semibold text-slate-500">
            Acesso rápido
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {atalhos.map((item, i) => (
              <Link
                key={item.rotulo}
                to={item.rota!}
                style={emCascata(i + 4)}
                className="group sombra-cartao flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-white p-4 transition-[border-color,box-shadow,transform] duration-200 hover:-translate-y-0.5 hover:border-semear-200 hover:shadow-md motion-safe:animate-entrar"
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-semear-50 text-semear-600 transition-colors duration-200 group-hover:bg-semear-600 group-hover:text-white">
                  <item.icone className="size-5" aria-hidden />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-slate-800">{item.rotulo}</span>
                  <span className="block text-xs text-slate-400">{item.grupo}</span>
                </span>
                <ArrowRight
                  className="size-4 text-slate-300 transition-[color,transform] duration-200 group-hover:translate-x-0.5 group-hover:text-semear-600"
                  aria-hidden
                />
              </Link>
            ))}
          </div>
        </section>
      )}

      <details className="group">
        <summary className="inline-flex cursor-pointer list-none items-center gap-2 rounded-lg text-sm font-medium text-slate-500 transition-colors hover:text-slate-900">
          <ShieldCheck className="size-4" aria-hidden /> Ver minhas permissões
        </summary>
        <Card className="mt-3 motion-safe:animate-entrar">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-slate-500">
                <th className="py-2 font-medium">Módulo</th>
                <th className="w-24 py-2 text-center font-medium">Consultar</th>
                <th className="w-24 py-2 text-center font-medium">Alterar</th>
              </tr>
            </thead>
            <tbody>
              {sessao.permissoes.map((p) => (
                <tr key={p.modulo} className="border-b border-slate-100 last:border-0">
                  <td className="py-2">{ROTULO_MODULO[p.modulo]}</td>
                  <td className="py-2 text-center">{marca(p.ler)}</td>
                  <td className="py-2 text-center">{marca(p.gravar)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      </details>
    </div>
  )
}

function Indicador({
  rotulo,
  valor,
  icone: Icone,
  ordem,
  alerta = false,
}: {
  rotulo: string
  valor: number
  icone: TipoIcone
  ordem: number
  alerta?: boolean
}) {
  return (
    <div
      style={emCascata(ordem)}
      className="sombra-cartao flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-white p-4 motion-safe:animate-entrar"
    >
      <span
        className={`grid size-11 shrink-0 place-items-center rounded-xl ${alerta ? 'bg-amber-50 text-amber-600' : 'bg-semear-50 text-semear-600'}`}
      >
        <Icone className="size-6" aria-hidden />
      </span>
      <div className="min-w-0">
        <dt className="truncate text-xs font-medium text-slate-500">{rotulo}</dt>
        <dd className="text-2xl font-semibold tracking-tight text-slate-900 tabular-nums">{valor}</dd>
      </div>
    </div>
  )
}

const TONS = {
  vermelho: {
    caixa: 'border-red-200/80 bg-red-50/70 hover:border-red-300',
    icone: 'bg-red-100 text-red-600',
    texto: 'text-red-900',
    acao: 'text-red-700',
  },
  amarelo: {
    caixa: 'border-amber-200/80 bg-amber-50/70 hover:border-amber-300',
    icone: 'bg-amber-100 text-amber-600',
    texto: 'text-amber-900',
    acao: 'text-amber-800',
  },
  neutro: {
    caixa: 'border-slate-200/80 bg-white hover:border-semear-200',
    icone: 'bg-red-50 text-red-600',
    texto: 'text-slate-700',
    acao: 'text-semear-700',
  },
} as const

/** Aviso clicável do painel (pendências, prazos, inadimplência). */
function Aviso({
  tom,
  icone: Icone,
  rota,
  acao,
  children,
}: {
  tom: keyof typeof TONS
  icone: TipoIcone
  rota: string
  acao: string
  children: ReactNode
}) {
  const cores = TONS[tom]
  return (
    <Link
      to={rota}
      className={`group flex flex-col items-start gap-3 rounded-2xl border px-4 py-3.5 text-sm transition-colors duration-200 motion-safe:animate-entrar sm:flex-row sm:items-center ${cores.caixa}`}
    >
      <span className={`grid size-9 shrink-0 place-items-center rounded-xl ${cores.icone}`}>
        <Icone className="size-5" aria-hidden />
      </span>
      <p className={`flex-1 ${cores.texto}`}>{children}</p>
      <span className={`inline-flex shrink-0 items-center gap-1 font-medium ${cores.acao}`}>
        {acao} <ArrowRight className="size-4 transition-transform duration-200 group-hover:translate-x-0.5" aria-hidden />
      </span>
    </Link>
  )
}

function marca(permitido: boolean) {
  return permitido ? (
    <Check className="mx-auto size-4 text-semear-600" aria-label="sim" />
  ) : (
    <Minus className="mx-auto size-4 text-slate-300" aria-label="não" />
  )
}
