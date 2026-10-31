import { useEffect, useRef, type ReactNode } from 'react'
import { X } from './icones'

interface Props {
  aberto: boolean
  titulo: string
  aoFechar: () => void
  children: ReactNode
  rodape?: ReactNode
}

/** Janela modal com <dialog> nativo: foco preso no diálogo e Esc para fechar. Abre com um leve zoom. */
export function Modal({ aberto, titulo, aoFechar, children, rodape }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const elemento = dialogo.current
    if (!elemento) return
    if (aberto && !elemento.open) elemento.showModal()
    if (!aberto && elemento.open) elemento.close()
  }, [aberto])

  return (
    <dialog
      ref={dialogo}
      onCancel={(e) => {
        e.preventDefault()
        aoFechar()
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-2xl border border-slate-200 bg-white p-0 shadow-2xl shadow-black/20 backdrop:bg-black/40 backdrop:backdrop-blur-[2px] motion-safe:open:animate-modal motion-safe:backdrop:animate-esmaecer"
    >
      {aberto && (
        <div className="flex max-h-[85vh] flex-col">
          <header className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
            <h2 className="text-base font-semibold text-slate-900">{titulo}</h2>
            <button
              type="button"
              onClick={aoFechar}
              className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
              aria-label="Fechar"
            >
              <X className="size-4" aria-hidden />
            </button>
          </header>
          <div className="overflow-y-auto px-5 py-4">{children}</div>
          {rodape && <footer className="flex justify-end gap-2 border-t border-slate-100 bg-slate-50/60 px-5 py-3">{rodape}</footer>}
        </div>
      )}
    </dialog>
  )
}
