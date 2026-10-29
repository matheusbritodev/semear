import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/Button'

interface Props {
  pagina: number
  tamanho: number
  total: number
  aoMudar: (pagina: number) => void
}

export function Paginacao({ pagina, tamanho, total, aoMudar }: Props) {
  const totalPaginas = Math.max(1, Math.ceil(total / tamanho))
  const inicio = total === 0 ? 0 : pagina * tamanho + 1
  const fim = Math.min(total, (pagina + 1) * tamanho)

  return (
    <nav className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-slate-500" aria-label="Paginação">
      <span>
        {inicio}–{fim} de {total}
      </span>
      <div className="flex items-center gap-1">
        <Button variante="secundario" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)} aria-label="Página anterior">
          <ChevronLeft className="size-4" aria-hidden />
        </Button>
        <span className="px-2">
          {pagina + 1} / {totalPaginas}
        </span>
        <Button
          variante="secundario"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => aoMudar(pagina + 1)}
          aria-label="Próxima página"
        >
          <ChevronRight className="size-4" aria-hidden />
        </Button>
      </div>
    </nav>
  )
}
