import { useContext, useLayoutEffect } from 'react'
import { TemaContext, type TemaContexto } from './TemaContext'

export function useTema(): TemaContexto {
  const contexto = useContext(TemaContext)
  if (!contexto) throw new Error('useTema precisa estar dentro de <TemaProvider>')
  return contexto
}

/**
 * Classe que liga o modo escuro em cada área (a mesma regra está no script de index.html):
 * - telas públicas: `dark`, com as variantes `dark:` escritas à mão em cada componente;
 * - área interna (escola e portal): `tema-escuro`, que troca a paleta inteira em styles/globals.css.
 */
const CLASSE_ESCURA = { publico: 'dark', interno: 'tema-escuro' } as const

/** Aplica o tema escolhido na página enquanto o componente estiver montado. */
export function useAplicarTema(area: keyof typeof CLASSE_ESCURA) {
  const { tema } = useTema()

  // useLayoutEffect: a classe muda no mesmo quadro da troca, o que a View Transition precisa para capturar o "depois".
  useLayoutEffect(() => {
    const raiz = document.documentElement
    const classe = CLASSE_ESCURA[area]
    raiz.classList.toggle(classe, tema === 'escuro')
    raiz.style.colorScheme = tema === 'escuro' ? 'dark' : 'light'
    return () => {
      raiz.classList.remove(classe)
      raiz.style.colorScheme = ''
    }
  }, [area, tema])
}
