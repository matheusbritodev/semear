import type { ReactNode } from 'react'
import { AlertTriangle, Inbox, LoaderCircle } from 'lucide-react'
import { ApiError } from '@/lib/ApiError'
import { Button } from '@/components/ui/Button'

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-slate-500" role="status">
      <LoaderCircle className="size-4 animate-spin text-semear-600" aria-hidden /> {texto}
    </div>
  )
}

export function EstadoVazio({ titulo, descricao }: { titulo: string; descricao?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center">
      <Inbox className="size-8 text-slate-300" aria-hidden />
      <p className="font-medium text-slate-700">{titulo}</p>
      {descricao && <p className="text-sm text-slate-500">{descricao}</p>}
    </div>
  )
}

export function EstadoErro({ erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo?: () => void }) {
  const mensagem = erro instanceof ApiError ? erro.mensagemAmigavel : 'Não foi possível carregar os dados.'
  return (
    <div role="alert" className="flex flex-col items-center gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-8 text-center">
      <AlertTriangle className="size-6 text-red-500" aria-hidden />
      <p className="text-sm text-red-700">{mensagem}</p>
      {aoTentarDeNovo && (
        <Button variante="secundario" onClick={aoTentarDeNovo}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}
