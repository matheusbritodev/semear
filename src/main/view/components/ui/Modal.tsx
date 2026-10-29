import { useEffect, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface Props {
  aberto: boolean
  titulo: string
  aoFechar: () => void
  children: ReactNode
  rodape?: ReactNode
}

/** Janela modal com <dialog> nativo: foco preso no diálogo e Esc para fechar. */
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
      className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-xl border border-slate-200 bg-white p-0 shadow-xl backdrop:bg-slate-900/40"
    >
      {aberto && (
        <div className="flex max-h-[85vh] flex-col">
          <header className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
            <h2 className="text-base font-semibold text-slate-900">{titulo}</h2>
            <button type="button" onClick={aoFechar} className="rounded p-1 text-slate-500 hover:bg-slate-100" aria-label="Fechar">
              <X className="size-4" aria-hidden />
            </button>
          </header>
          <div className="overflow-y-auto px-5 py-4">{children}</div>
          {rodape && <footer className="flex justify-end gap-2 border-t border-slate-200 px-5 py-3">{rodape}</footer>}
        </div>
      )}
    </dialog>
  )
}
