import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { ArrowLeft } from 'lucide-react'

interface Props {
  titulo: string
  descricao?: ReactNode
  acoes?: ReactNode
  voltarPara?: { rota: string; rotulo: string }
}

export function PageHeader({ titulo, descricao, acoes, voltarPara }: Props) {
  return (
    <div className="mb-6 space-y-2">
      {voltarPara && (
        <Link to={voltarPara.rota} className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-semear-700">
          <ArrowLeft className="size-4" aria-hidden /> {voltarPara.rotulo}
        </Link>
      )}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">{titulo}</h1>
          {descricao && <p className="mt-1 text-sm text-slate-500">{descricao}</p>}
        </div>
        {acoes && <div className="flex flex-wrap items-center gap-2">{acoes}</div>}
      </div>
    </div>
  )
}
