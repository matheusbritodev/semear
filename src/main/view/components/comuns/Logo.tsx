import { Sprout } from '@/components/ui/icones'

interface Props {
  claro?: boolean
  /** "grande": logo em destaque, com o nome embaixo (telas públicas). */
  tamanho?: 'normal' | 'grande'
  /** Classes extras para o nome "Semear" (ex.: esmaecer quando o menu lateral está recolhido). */
  classeNome?: string
}

export function Logo({ claro = false, tamanho = 'normal', classeNome = '' }: Props) {
  if (tamanho === 'grande') {
    return (
      <span className="flex flex-col items-center gap-3 text-center">
        <span className="relative grid size-16 place-items-center rounded-2xl bg-linear-to-br from-semear-500 to-semear-700 text-white shadow-lg shadow-semear-700/30 ring-4 ring-semear-100 dark:shadow-semear-950/60 dark:ring-semear-900/60">
          <Sprout className="size-9" aria-hidden />
        </span>
        <span className="leading-tight">
          <span className="block text-2xl font-bold tracking-tight text-semear-700 dark:text-semear-300">Semear</span>
          <span className="block text-xs font-medium tracking-[0.2em] text-slate-500 uppercase dark:text-slate-400">
            Gestão Escolar
          </span>
        </span>
      </span>
    )
  }

  return (
    <span className={`flex items-center gap-2 font-semibold tracking-tight ${claro ? 'text-white' : 'text-semear-700 dark:text-semear-300'}`}>
      <span className={`grid size-8 shrink-0 place-items-center rounded-lg ${claro ? 'bg-white/15' : 'bg-semear-600 text-white'}`}>
        <Sprout className="size-5" aria-hidden />
      </span>
      <span className={classeNome}>Semear</span>
    </span>
  )
}
