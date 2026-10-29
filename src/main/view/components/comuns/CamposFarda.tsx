import { Select } from '@/components/ui/Select'
import { TAMANHOS_FARDA } from '@/models/enums'

interface Props {
  uniforme: string
  edFisica: string
  aoMudar: (campo: 'uniforme' | 'edFisica', valor: string) => void
  erros?: { uniforme?: string; edFisica?: string }
}

/** RN06: tamanhos de uniforme e de educação física, separados. */
export function CamposFarda({ uniforme, edFisica, aoMudar, erros }: Props) {
  const opcoes = (
    <>
      <option value="">Não sei ainda</option>
      {TAMANHOS_FARDA.map((t) => (
        <option key={t} value={t}>
          {t}
        </option>
      ))}
    </>
  )
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Select rotulo="Tamanho do uniforme" value={uniforme} onChange={(e) => aoMudar('uniforme', e.target.value)} erro={erros?.uniforme}>
        {opcoes}
      </Select>
      <Select
        rotulo="Tamanho da farda de educação física"
        value={edFisica}
        onChange={(e) => aoMudar('edFisica', e.target.value)}
        erro={erros?.edFisica}
      >
        {opcoes}
      </Select>
    </div>
  )
}
