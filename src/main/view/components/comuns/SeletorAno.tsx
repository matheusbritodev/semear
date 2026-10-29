import { Select } from '@/components/ui/Select'
import { useAnosLetivos } from '@/hooks/useAcademico'

/** Escolha do ano letivo; o ano em andamento aparece marcado. */
export function SeletorAno({ ano, aoMudar }: { ano?: number; aoMudar: (ano: number) => void }) {
  const anos = useAnosLetivos()
  return (
    <div className="w-full sm:w-40">
      <Select rotulo="Ano letivo" ocultarRotulo value={ano ?? ''} onChange={(e) => aoMudar(Number(e.target.value))}>
        {ano === undefined && <option value="">Ano letivo…</option>}
        {anos.data?.map((a) => (
          <option key={a.id} value={a.ano}>
            {a.ano}
            {a.emAndamento ? ' (atual)' : ''}
          </option>
        ))}
      </Select>
    </div>
  )
}
