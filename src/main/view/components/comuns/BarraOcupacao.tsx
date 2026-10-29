/** Ocupação da turma (RN04): verde até 80%, âmbar até 99%, vermelho quando lotada. */
export function BarraOcupacao({ ocupadas, capacidade }: { ocupadas: number; capacidade: number }) {
  const percentual = Math.min(100, Math.round((100 * ocupadas) / capacidade))
  const cor = percentual >= 100 ? 'bg-red-500' : percentual >= 80 ? 'bg-amber-500' : 'bg-semear-500'
  return (
    <div className="space-y-1">
      <div
        className="h-2 overflow-hidden rounded-full bg-slate-100"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={capacidade}
        aria-valuenow={ocupadas}
        aria-label={`${ocupadas} de ${capacidade} vagas ocupadas`}
      >
        <div className={`h-full rounded-full ${cor}`} style={{ width: `${percentual}%` }} />
      </div>
      <p className="text-xs text-slate-500">
        <strong className="font-semibold text-slate-700">{ocupadas}</strong> de {capacidade} vagas
      </p>
    </div>
  )
}
