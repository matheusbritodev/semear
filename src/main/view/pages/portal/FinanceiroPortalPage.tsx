import { Link } from 'react-router'
import { ArrowRight } from 'lucide-react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { StatusCobrancaBadge } from '@/components/comuns/StatusBadges'
import { Card } from '@/components/ui/Card'
import { useMinhasCobrancas } from '@/hooks/useFinanceiro'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_TIPO_COBRANCA, formatarCompetencia, type Cobranca } from '@/models/financeiro'

/** Cobranças em que o responsável logado é o pagador: em aberto primeiro, depois o histórico. */
export function FinanceiroPortalPage() {
  const consulta = useMinhasCobrancas()

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const todas = consulta.data.cobrancas
  const abertas = todas
    .filter((c) => c.status === 'PENDENTE' || c.status === 'VENCIDO')
    .sort((a, b) => a.vencimento.localeCompare(b.vencimento))
  const historico = todas.filter((c) => c.status === 'PAGO' || c.status === 'CANCELADO')
  const vencidas = abertas.filter((c) => c.status === 'VENCIDO')

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Financeiro</h1>
        <p className="text-slate-500">
          Mensalidades pagas até o vencimento têm 10% de desconto. Depois do vencimento, há multa de 2% e juros de 0,033% ao dia.
        </p>
      </div>

      {vencidas.length > 0 && (
        <p role="status" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900">
          Você tem <strong>{vencidas.length}</strong> {vencidas.length === 1 ? 'cobrança vencida' : 'cobranças vencidas'}. Com débito em aberto não é
          possível fazer a rematrícula. Em caso de dúvida, fale com a secretaria.
        </p>
      )}

      <section aria-labelledby="titulo-abertas">
        <h2 id="titulo-abertas" className="mb-3 text-base font-semibold text-slate-800">
          Em aberto
        </h2>
        {abertas.length === 0 ? (
          <Card>
            <p className="text-sm text-slate-600">Nenhuma cobrança em aberto. Tudo em dia!</p>
          </Card>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {abertas.map((c) => (
              <CartaoCobranca key={c.id} cobranca={c} />
            ))}
          </div>
        )}
      </section>

      {historico.length > 0 && (
        <section aria-labelledby="titulo-historico">
          <h2 id="titulo-historico" className="mb-3 text-base font-semibold text-slate-800">
            Histórico
          </h2>
          <Card>
            <ul className="divide-y divide-slate-100 text-sm">
              {historico.map((c) => (
                <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <span>
                    <span className="font-medium">{ROTULO_TIPO_COBRANCA[c.tipo]}</span>{' '}
                    <span className="text-slate-500">
                      {formatarCompetencia(c.competencia)} · {c.aluno.split(' ')[0]}
                    </span>
                  </span>
                  <span className="flex items-center gap-3">
                    <span className="tabular-nums text-slate-600">
                      {c.pagoEm ? `${formatarMoeda(c.valorPago ?? 0)} em ${formatarData(c.pagoEm)}` : formatarMoeda(c.valorFinal)}
                    </span>
                    <StatusCobrancaBadge status={c.status} />
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </section>
      )}
    </div>
  )
}

function CartaoCobranca({ cobranca: c }: { cobranca: Cobranca }) {
  return (
    <Link
      to={`/portal/cobrancas/${c.id}`}
      className="group block rounded-xl border border-slate-200 bg-white p-4 shadow-xs hover:border-semear-200"
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold text-slate-900">
            {ROTULO_TIPO_COBRANCA[c.tipo]} · {formatarCompetencia(c.competencia)}
          </p>
          <p className="text-sm text-slate-500">{c.aluno}</p>
        </div>
        <StatusCobrancaBadge status={c.status} />
      </div>
      <p className="mt-3 text-2xl font-semibold text-slate-900 tabular-nums">{formatarMoeda(c.valorHoje)}</p>
      <p className="text-sm text-slate-500">
        {c.status === 'VENCIDO' ? `Venceu em ${formatarData(c.vencimento)} · ${c.diasDeAtraso} dias` : `Vence em ${formatarData(c.vencimento)}`}
      </p>
      <span className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-semear-700">
        Ver boleto <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden />
      </span>
    </Link>
  )
}
