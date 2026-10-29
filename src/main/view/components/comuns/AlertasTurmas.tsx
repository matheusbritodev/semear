import { Link } from 'react-router'
import { BellRing, Check } from 'lucide-react'
import { useAlertas, useMarcarAlertaLido } from '@/hooks/useAcademico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { Button } from '@/components/ui/Button'

/** Avisos de turma lotada (RF03). Só aparece para quem pode vê-los e se houver algum não lido. */
export function AlertasTurmas() {
  const podeGravarTurmas = usePermissao('TURMAS', 'GRAVAR')
  const podeLerMatricula = usePermissao('MATRICULA', 'LER')
  const alertas = useAlertas(podeGravarTurmas || podeLerMatricula)
  const marcar = useMarcarAlertaLido()

  if (!alertas.data?.length) return null

  return (
    <section aria-label="Alertas para a secretaria" className="mb-6 space-y-2">
      {alertas.data.map((alerta) => (
        <div
          key={alerta.id}
          className="flex flex-col items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm sm:flex-row sm:items-center sm:gap-3"
        >
          <BellRing className="size-4 shrink-0 text-amber-600" aria-hidden />
          <p className="flex-1 text-amber-900">
            <strong className="font-semibold">Turma lotada.</strong> {alerta.mensagem}{' '}
            {alerta.turmaId && (
              <Link to={`/escola/turmas/${alerta.turmaId}`} className="font-medium underline">
                Ver turma
              </Link>
            )}
          </p>
          {podeGravarTurmas && (
            <Button variante="fantasma" onClick={() => marcar.mutate(alerta.id)} carregando={marcar.isPending && marcar.variables === alerta.id}>
              <Check className="size-4" aria-hidden /> Marcar como lido
            </Button>
          )}
        </div>
      ))}
    </section>
  )
}
