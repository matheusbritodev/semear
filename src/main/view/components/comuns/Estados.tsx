import type { ReactNode } from 'react'
import { AlertTriangle, Inbox, LoaderCircle } from '@/components/ui/icones'
import { ApiError } from '@/lib/ApiError'
import { Button } from '@/components/ui/Button'

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-slate-500 motion-safe:animate-esmaecer" role="status">
      <LoaderCircle className="size-5 animate-spin text-semear-600" aria-hidden /> {texto}
    </div>
  )
}

export function EstadoVazio({ titulo, descricao }: { titulo: string; descricao?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-slate-300 bg-white/70 px-4 py-12 text-center motion-safe:animate-entrar">
      <span className="mb-1 grid size-12 place-items-center rounded-2xl bg-slate-100 text-slate-400">
        <Inbox className="size-6" aria-hidden />
      </span>
      <p className="font-medium text-slate-700">{titulo}</p>
      {descricao && <p className="max-w-md text-sm text-slate-500">{descricao}</p>}
    </div>
  )
}

export function EstadoErro({ erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo?: () => void }) {
  const mensagem = erro instanceof ApiError ? erro.mensagemAmigavel : 'Não foi possível carregar os dados.'
  return (
    <div
      role="alert"
      className="flex flex-col items-center gap-3 rounded-2xl border border-red-200 bg-red-50/70 px-4 py-8 text-center motion-safe:animate-entrar"
    >
      <span className="grid size-11 place-items-center rounded-2xl bg-red-100 text-red-600">
        <AlertTriangle className="size-6" aria-hidden />
      </span>
      <p className="text-sm text-red-700">{mensagem}</p>
      {aoTentarDeNovo && (
        <Button variante="secundario" onClick={aoTentarDeNovo}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}
