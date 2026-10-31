import { Monitor, Moon, Sun, type TipoIcone } from '@/components/ui/icones'
import type { PreferenciaTema } from '@/tema/TemaContext'
import { useTema } from '@/tema/useTema'

const OPCOES: { valor: PreferenciaTema; rotulo: string; icone: TipoIcone }[] = [
  { valor: 'claro', rotulo: 'Tema claro', icone: Sun },
  { valor: 'escuro', rotulo: 'Tema escuro', icone: Moon },
  { valor: 'sistema', rotulo: 'Seguir o tema do sistema', icone: Monitor },
]

/** Seletor de tema: claro, escuro ou do sistema. A pílula verde desliza até a opção escolhida. */
export function AlternarTema({ className = '' }: { className?: string }) {
  const { preferencia, alterarTema } = useTema()
  const indice = OPCOES.findIndex((o) => o.valor === preferencia)

  return (
    <div
      role="group"
      aria-label="Tema da tela"
      className={`relative inline-grid grid-cols-3 rounded-full border border-slate-200 bg-white/80 p-1 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/80 ${className}`}
    >
      <span
        aria-hidden
        className="absolute inset-y-1 left-1 w-[calc((100%-0.5rem)/3)] rounded-full bg-semear-600 shadow-md shadow-semear-700/30 transition-transform duration-500 ease-[cubic-bezier(0.65,0,0.35,1)] dark:bg-semear-500"
        style={{ transform: `translateX(${indice * 100}%)` }}
      />
      {OPCOES.map(({ valor, rotulo, icone: Icone }) => {
        const ativo = valor === preferencia
        return (
          <button
            key={valor}
            type="button"
            title={rotulo}
            aria-label={rotulo}
            aria-pressed={ativo}
            onClick={(e) => alterarTema(valor, e.currentTarget)}
            className={`relative z-10 grid size-8 place-items-center rounded-full transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-semear-500 ${ativo ? 'text-white' : 'text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-100'}`}
          >
            <Icone
              className={`size-4 transition-transform duration-500 ${ativo ? 'scale-110' : 'scale-90'}`}
              aria-hidden
            />
          </button>
        )
      })}
    </div>
  )
}
