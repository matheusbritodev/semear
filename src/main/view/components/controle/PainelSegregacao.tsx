import { ShieldCheck } from '@/components/ui/icones'
import { Badge } from '@/components/ui/Badge'
import { Card } from '@/components/ui/Card'
import { usePoliticaSod } from '@/hooks/useControle'
import { ROTULO_MODULO, ROTULO_PERFIL } from '@/models/enums'

/** Matriz de funções incompatíveis (SoD), mostrada na tela de perfis de acesso. */
export function PainelSegregacao() {
  const politica = usePoliticaSod()
  if (!politica.data) return null
  const p = politica.data

  return (
    <Card titulo="Funções que não podem ficar juntas (segregação de funções)" className="mt-6">
      <p className="mb-3 text-sm text-slate-600">
        Um mesmo perfil não pode <strong>alterar</strong> os dois módulos de cada linha: uma pessoa sozinha conseguiria fazer e esconder um
        desvio. O sistema recusa a combinação. A Administração ({ROTULO_PERFIL[p.perfilComExcecao]}) é a exceção aceita, coberta por:{' '}
        {p.controlesCompensatorios.toLowerCase()}.
      </p>
      <ul className="divide-y divide-slate-100 text-sm">
        {p.regras.map((r) => {
          const excecao = p.conflitos.some((c) => c.codigo === r.codigo && c.excecaoDocumentada)
          const violada = p.conflitos.some((c) => c.codigo === r.codigo && !c.excecaoDocumentada)
          return (
            <li key={r.codigo} className="flex flex-col gap-1 py-2 sm:flex-row sm:items-start sm:gap-3">
              <span className="flex shrink-0 items-center gap-2 sm:w-64">
                <Badge cor={violada ? 'vermelho' : 'cinza'}>{r.codigo}</Badge>
                <span className="font-medium text-slate-800">
                  {ROTULO_MODULO[r.moduloA]} × {ROTULO_MODULO[r.moduloB]}
                </span>
              </span>
              <span className="flex-1 text-slate-600">
                {r.risco}
                {excecao && (
                  <span className="ml-1 inline-flex items-center gap-1 text-xs text-sky-700">
                    <ShieldCheck className="size-3.5" aria-hidden /> exceção do gestor
                  </span>
                )}
              </span>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}
