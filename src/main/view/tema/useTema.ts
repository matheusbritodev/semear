import { useContext, useLayoutEffect } from 'react'
import { TemaContext, type TemaContexto } from './TemaContext'

export function useTema(): TemaContexto {
  const contexto = useContext(TemaContext)
  if (!contexto) throw new Error('useTema precisa estar dentro de <TemaProvider>')
  return contexto
}

/**
 * Aplica o tema na página enquanto o componente estiver montado.
 * Por enquanto só as telas públicas têm modo escuro; ao sair delas, a página volta ao claro.
 */
export function useAplicarTema() {
  const { tema } = useTema()

  // useLayoutEffect: a classe muda no mesmo quadro da troca, o que a View Transition precisa para capturar o "depois".
  useLayoutEffect(() => {
    const raiz = document.documentElement
    raiz.classList.toggle('dark', tema === 'escuro')
    raiz.style.colorScheme = tema === 'escuro' ? 'dark' : 'light'
    return () => {
      raiz.classList.remove('dark')
      raiz.style.colorScheme = ''
    }
  }, [tema])
}
