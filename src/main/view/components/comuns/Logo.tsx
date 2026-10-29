import { Sprout } from 'lucide-react'

export function Logo({ claro = false }: { claro?: boolean }) {
  return (
    <span className={`flex items-center gap-2 font-semibold tracking-tight ${claro ? 'text-white' : 'text-semear-700'}`}>
      <span className={`grid size-8 place-items-center rounded-lg ${claro ? 'bg-white/15' : 'bg-semear-600 text-white'}`}>
        <Sprout className="size-5" aria-hidden />
      </span>
      Semear
    </span>
  )
}
