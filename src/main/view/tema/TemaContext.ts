import { createContext } from 'react'

/** O que a pessoa escolheu: claro, escuro ou acompanhar o sistema operacional. */
export type PreferenciaTema = 'claro' | 'escuro' | 'sistema'

/** O tema efetivamente aplicado na tela. */
export type Tema = 'claro' | 'escuro'

export interface TemaContexto {
  preferencia: PreferenciaTema
  tema: Tema
  /** Troca o tema; com `origem`, a transição se espalha a partir desse elemento (ex.: o botão clicado). */
  alterarTema: (preferencia: PreferenciaTema, origem?: HTMLElement | null) => void
}

export const TemaContext = createContext<TemaContexto | null>(null)
