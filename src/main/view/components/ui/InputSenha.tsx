import { useState, type ComponentProps } from 'react'
import { LockKeyhole } from 'lucide-react'
import { Input } from './Input'

type Props = Omit<ComponentProps<typeof Input>, 'type' | 'acessorio'>

/** Campo de senha com o botão de mostrar/ocultar o que foi digitado. */
export function InputSenha({ icone = LockKeyhole, ...resto }: Props) {
  const [visivel, setVisivel] = useState(false)
  return (
    <Input
      {...resto}
      icone={icone}
      type={visivel ? 'text' : 'password'}
      acessorio={
        <button
          type="button"
          onClick={() => setVisivel((v) => !v)}
          aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
          aria-pressed={visivel}
          className="grid size-8 place-items-center rounded-md text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-semear-500 dark:text-slate-500 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        >
          <OlhoAnimado aberto={visivel} />
        </button>
      }
    />
  )
}

const TRANSICAO = 'transition-[transform,opacity] duration-300 ease-[cubic-bezier(0.65,0,0.35,1)] motion-reduce:transition-none'

/**
 * Olho que pisca: fechado (com cílios) enquanto a senha está oculta, abre quando ela fica visível.
 * O olho aberto "achata" verticalmente até virar a pálpebra fechada, e o contrário ao abrir.
 */
function OlhoAnimado({ aberto }: { aberto: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-4"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <g
        className={TRANSICAO}
        style={{ transformOrigin: '12px 12px', transform: aberto ? 'scaleY(1)' : 'scaleY(0.1)', opacity: aberto ? 1 : 0 }}
      >
        <path d="M2.06 12.35a1 1 0 0 1 0-.7 10.75 10.75 0 0 1 19.88 0 1 1 0 0 1 0 .7 10.75 10.75 0 0 1-19.88 0" />
        <circle cx="12" cy="12" r="3" />
      </g>
      <g
        className={TRANSICAO}
        style={{
          transformOrigin: '12px 12px',
          transform: aberto ? 'scaleY(0.4) translateY(-4px)' : 'none',
          opacity: aberto ? 0 : 1,
          transitionDelay: aberto ? '0ms' : '120ms',
        }}
      >
        <path d="M2 10c2.6 3.2 6 4.8 10 4.8s7.4-1.6 10-4.8" />
        <path d="m4.8 13.2-1.6 2.2M9.3 14.6l-.6 2.6M14.7 14.6l.6 2.6M19.2 13.2l1.6 2.2" />
      </g>
    </svg>
  )
}
