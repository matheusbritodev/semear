/** Círculo com as iniciais da pessoa (ex.: "Ana Paula Costa" → "AC"). */
export function Avatar({ nome, className = '' }: { nome?: string | null; className?: string }) {
  const partes = (nome ?? '').trim().split(/\s+/).filter(Boolean)
  const iniciais = partes.length === 0 ? '?' : (partes[0]![0]! + (partes.length > 1 ? partes[partes.length - 1]![0]! : '')).toUpperCase()
  return (
    <span
      aria-hidden
      className={`grid size-9 shrink-0 place-items-center rounded-full bg-linear-to-br from-semear-100 to-semear-200 text-xs font-semibold text-semear-800 ring-2 ring-white ${className}`}
    >
      {iniciais}
    </span>
  )
}
