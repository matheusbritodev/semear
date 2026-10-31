import { Search } from '@/components/ui/icones'

interface Props {
  valor: string
  aoMudar: (valor: string) => void
  placeholder: string
  rotulo?: string
}

export function CampoBusca({ valor, aoMudar, placeholder, rotulo = 'Buscar' }: Props) {
  return (
    <label className="group relative block w-full sm:max-w-sm">
      <span className="sr-only">{rotulo}</span>
      <Search
        className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-semear-600"
        aria-hidden
      />
      <input
        type="search"
        value={valor}
        onChange={(e) => aoMudar(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-slate-300 bg-white py-2 pr-3 pl-9 text-sm shadow-xs outline-none transition placeholder:text-slate-400 focus:border-semear-500 focus:ring-2 focus:ring-semear-200"
      />
    </label>
  )
}
