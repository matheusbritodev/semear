import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { flushSync } from 'react-dom'
import { TemaContext, type PreferenciaTema, type Tema, type TemaContexto } from './TemaContext'

// A mesma chave é lida pelo script em index.html, que aplica o tema antes do React (evita piscar em branco).
const CHAVE = 'semear-tema'
const DURACAO_MS = 700

const consultaEscuro = () => window.matchMedia('(prefers-color-scheme: dark)')
const reduzirMovimento = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

function lerPreferencia(): PreferenciaTema {
  try {
    const salva = localStorage.getItem(CHAVE)
    return salva === 'claro' || salva === 'escuro' ? salva : 'sistema'
  } catch {
    return 'sistema'
  }
}

function salvarPreferencia(preferencia: PreferenciaTema) {
  try {
    if (preferencia === 'sistema') localStorage.removeItem(CHAVE)
    else localStorage.setItem(CHAVE, preferencia)
  } catch {
    // Navegação privada pode bloquear o armazenamento: o tema só não fica lembrado.
  }
}

const resolver = (preferencia: PreferenciaTema, sistemaEscuro: boolean): Tema =>
  preferencia === 'sistema' ? (sistemaEscuro ? 'escuro' : 'claro') : preferencia

/**
 * Tema claro/escuro com opção de seguir o sistema.
 * A troca usa a View Transitions API: o novo tema se revela num círculo que cresce a partir do botão clicado.
 * Navegadores sem suporte recebem uma transição suave de cores; quem pede menos movimento troca na hora.
 */
export function TemaProvider({ children }: { children: ReactNode }) {
  const [preferencia, setPreferencia] = useState<PreferenciaTema>(lerPreferencia)
  const [sistemaEscuro, setSistemaEscuro] = useState(() => consultaEscuro().matches)

  useEffect(() => {
    const consulta = consultaEscuro()
    const aoMudar = (e: MediaQueryListEvent) => setSistemaEscuro(e.matches)
    consulta.addEventListener('change', aoMudar)
    return () => consulta.removeEventListener('change', aoMudar)
  }, [])

  const tema = resolver(preferencia, sistemaEscuro)

  const alterarTema = useCallback(
    (nova: PreferenciaTema, origem?: HTMLElement | null) => {
      salvarPreferencia(nova)
      const mudaCor = resolver(nova, sistemaEscuro) !== tema

      if (!mudaCor || reduzirMovimento()) {
        setPreferencia(nova)
        return
      }

      if (!document.startViewTransition) {
        const raiz = document.documentElement
        raiz.classList.add('tema-em-transicao')
        setPreferencia(nova)
        window.setTimeout(() => raiz.classList.remove('tema-em-transicao'), DURACAO_MS)
        return
      }

      const transicao = document.startViewTransition(() => flushSync(() => setPreferencia(nova)))
      void transicao.ready.then(() => {
        const caixa = origem?.getBoundingClientRect()
        const x = caixa ? caixa.left + caixa.width / 2 : window.innerWidth / 2
        const y = caixa ? caixa.top + caixa.height / 2 : 0
        const raio = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
        document.documentElement.animate(
          { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${raio}px at ${x}px ${y}px)`] },
          { duration: DURACAO_MS, easing: 'cubic-bezier(0.65, 0, 0.35, 1)', pseudoElement: '::view-transition-new(root)' },
        )
      })
    },
    [sistemaEscuro, tema],
  )

  const valor = useMemo<TemaContexto>(() => ({ preferencia, tema, alterarTema }), [preferencia, tema, alterarTema])

  return <TemaContext.Provider value={valor}>{children}</TemaContext.Provider>
}
