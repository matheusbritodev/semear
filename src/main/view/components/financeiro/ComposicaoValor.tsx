import { formatarData, formatarMoeda } from '@/lib/formatters'
import type { Cobranca, ComposicaoValor as Composicao } from '@/models/financeiro'

/**
 * Explica de onde vem o valor a pagar (RN08/RN09): valor da tabela, bolsa, desconto
 * de pontualidade, multa e juros. Para cobrança paga, mostra o que foi pago.
 */
export function ComposicaoValor({ cobranca, composicao }: { cobranca: Cobranca; composicao: Composicao }) {
  const paga = cobranca.status === 'PAGO'
  return (
    <dl className="divide-y divide-slate-100 rounded-lg border border-slate-200 text-sm">
      <Linha rotulo="Valor da tabela" valor={formatarMoeda(cobranca.valorBruto)} />
      {cobranca.descontoBolsa > 0 && <Linha rotulo="Bolsa" valor={`− ${formatarMoeda(cobranca.descontoBolsa)}`} cor="text-semear-700" />}
      {!paga && (
        <>
          {composicao.descontoPontualidade > 0 && (
            <Linha
              rotulo="Desconto de 10% (pago até o vencimento)"
              valor={`− ${formatarMoeda(composicao.descontoPontualidade)}`}
              cor="text-semear-700"
            />
          )}
          {composicao.multa > 0 && <Linha rotulo="Multa de 2%" valor={`+ ${formatarMoeda(composicao.multa)}`} cor="text-red-700" />}
          {composicao.juros > 0 && (
            <Linha
              rotulo={`Juros de 0,033% ao dia (${composicao.diasDeAtraso} dias)`}
              valor={`+ ${formatarMoeda(composicao.juros)}`}
              cor="text-red-700"
            />
          )}
        </>
      )}
      <div className="flex items-center justify-between gap-3 bg-slate-50 px-3 py-2.5">
        <dt className="font-semibold text-slate-800">
          {paga ? 'Valor pago' : `A pagar em ${formatarData(composicao.data)}`}
        </dt>
        <dd className="text-lg font-semibold text-slate-900 tabular-nums">{formatarMoeda(composicao.total)}</dd>
      </div>
    </dl>
  )
}

function Linha({ rotulo, valor, cor = 'text-slate-800' }: { rotulo: string; valor: string; cor?: string }) {
  return (
    <div className="flex items-center justify-between gap-3 px-3 py-2">
      <dt className="text-slate-600">{rotulo}</dt>
      <dd className={`tabular-nums ${cor}`}>{valor}</dd>
    </div>
  )
}
