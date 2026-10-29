import { createContext } from 'react'
import type { Session } from '@supabase/supabase-js'
import type { ApiError } from '@/lib/ApiError'
import type { Sessao } from '@/models/sessao'

export interface AuthContexto {
  /** true enquanto descobre se há login e carrega GET /api/me */
  carregando: boolean
  /** Sessão do Supabase Auth (tem o JWT) */
  session: Session | null
  /** Perfil e permissões vindos da aplicação Java */
  sessao: Sessao | null
  /** Falha ao falar com a aplicação Java (ex.: servidor fora do ar) */
  erro: ApiError | null
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => Promise<void>
  recarregar: () => Promise<void>
  /** Cria a conta pela aplicação Java (já confirmada) e entra. Devolve true se a sessão foi aberta. */
  criarConta: (email: string, senha: string) => Promise<boolean>
  recuperarSenha: (email: string) => Promise<void>
  redefinirSenha: (novaSenha: string) => Promise<void>
}

export const AuthContext = createContext<AuthContexto | null>(null)
