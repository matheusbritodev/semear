import { Link } from 'react-router'
import { ArrowRight, CalendarClock, Check, FileWarning, Landmark, Minus } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { pode } from '@/auth/permissions'
import { AlertasTurmas } from '@/components/comuns/AlertasTurmas'
import { Card } from '@/components/ui/Card'
import { useAnoCorrente, useTurmas } from '@/hooks/useAcademico'
import { useInadimplencia } from '@/hooks/useFinanceiro'
import { useAlertasObrigacoes } from '@/hooks/useObrigacoes'
import { useResumoPendencias } from '@/hooks/usePendencias'
import { formatarMoeda } from '@/lib/formatters'
import { MENU_ESCOLA } from '@/layouts/menuEscola'
import { ROTULO_MODULO, ROTULO_PERFIL } from '@/models/enums'

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

  if (!sessao?.perfil) return null
  const primeiroNome = sessao.nome?.split(' ')[0]
  const lista = turmas.data ?? []
  const atalhos = MENU_ESCOLA.filter((item) => item.rota && pode(sessao, item.modulo, 'LER'))

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Olá{primeiroNome ? `, ${primeiroNome}` : ''}!</h1>
        <p className="text-slate-500">
          {ROTULO_PERFIL[sessao.perfil]}
          {corrente.data && <> · ano letivo {corrente.data.ano}</>}
        </p>
      </div>

      <AlertasTurmas />

      {vePendencias && pendencias.data && pendencias.data.alunosComPendencia > 0 && (
        <Link
          to="/escola/pendencias"
          className="group flex flex-wrap items-center gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm hover:border-red-300"
        >
          <FileWarning className="size-5 shrink-0 text-red-600" aria-hidden />
          <p className="flex-1 text-red-900">
            <strong className="font-semibold">
              {pendencias.data.alunosComPendencia} {pendencias.data.alunosComPendencia === 1 ? 'aluno' : 'alunos'} com documentos pendentes
            </strong>{' '}
            ({pendencias.data.documentosFaltando} {pendencias.data.documentosFaltando === 1 ? 'documento' : 'documentos'} no total, de{' '}
            {pendencias.data.alunosConsiderados} matrículas vigentes). Cobre as famílias pela lista de pendências.
          </p>
          <span className="inline-flex items-center gap-1 font-medium text-red-700">
            Ver pendências <ArrowRight className="size-4" aria-hidden />
          </span>
        </Link>
      )}

      {veTurmas && lista.length > 0 && (
        <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <Indicador rotulo={sessao.perfil === 'PROFESSOR' ? 'Minhas turmas' : 'Turmas'} valor={lista.length} />
          <Indicador rotulo="Alunos" valor={lista.reduce((s, t) => s + t.ocupadas, 0)} />
          <Indicador rotulo="Vagas livres" valor={lista.reduce((s, t) => s + t.vagas, 0)} />
          <Indicador rotulo="Turmas lotadas" valor={lista.filter((t) => t.lotada).length} />
        </dl>
      )}

      {veObrigacoes && obrigacoes.data && obrigacoes.data.length > 0 && (
        <Link
          to="/escola/obrigacoes"
          className="group flex flex-wrap items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm hover:border-amber-300"
        >
          <CalendarClock className="size-5 shrink-0 text-amber-600" aria-hidden />
          <p className="flex-1 text-amber-900">
            <strong className="font-semibold">Obrigações com prazo próximo:</strong>{' '}
            {obrigacoes.data
              .map((o) => `${o.orgaoRotulo} (${o.diasRestantes < 0 ? `atrasada há ${-o.diasRestantes} dias` : o.diasRestantes === 0 ? 'vence hoje' : `em ${o.diasRestantes} dias`})`)
              .join(' · ')}
          </p>
          <span className="inline-flex items-center gap-1 font-medium text-amber-800">
            Ver obrigações <ArrowRight className="size-4" aria-hidden />
          </span>
        </Link>
      )}

      {veFinanceiro && inadimplencia.data && inadimplencia.data.alunos > 0 && (
        <Link
          to="/escola/financeiro/inadimplencia"
          className="group flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm hover:border-semear-200"
        >
          <Landmark className="size-5 shrink-0 text-red-600" aria-hidden />
          <p className="flex-1 text-slate-700">
            <strong className="font-semibold text-slate-900">
              {inadimplencia.data.alunos} {inadimplencia.data.alunos === 1 ? 'aluno inadimplente' : 'alunos inadimplentes'}
            </strong>{' '}
            · {inadimplencia.data.cobrancas} cobranças vencidas · {formatarMoeda(inadimplencia.data.valorAtualizado)} atualizado hoje
          </p>
          <span className="inline-flex items-center gap-1 font-medium text-semear-700">
            Ver inadimplência <ArrowRight className="size-4" aria-hidden />
          </span>
        </Link>
      )}

      {atalhos.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {atalhos.map((item) => (
            <Link
              key={item.rotulo}
              to={item.rota!}
              className="group flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-xs hover:border-semear-200"
            >
              <span className="flex items-center gap-2 font-medium text-slate-800">
                <item.icone className="size-5 text-semear-600" aria-hidden /> {item.rotulo}
              </span>
              <ArrowRight className="size-4 text-slate-300 group-hover:text-semear-600" aria-hidden />
            </Link>
          ))}
        </div>
      )}

      <details className="group">
        <summary className="cursor-pointer text-sm font-medium text-slate-600 hover:text-slate-900">
          Ver minhas permissões
        </summary>
        <Card className="mt-3">
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

function Indicador({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className="text-2xl font-semibold text-slate-900">{valor}</dd>
    </div>
  )
}

function marca(permitido: boolean) {
  return permitido ? (
    <Check className="mx-auto size-4 text-semear-600" aria-label="sim" />
  ) : (
    <Minus className="mx-auto size-4 text-slate-300" aria-label="não" />
  )
}
