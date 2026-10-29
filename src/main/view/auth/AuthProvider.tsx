import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { Session } from '@supabase/supabase-js'
import { ApiError } from '@/lib/ApiError'
import { supabase } from '@/lib/supabase'
import type { Sessao } from '@/models/sessao'
import { contaService } from '@/services/ContaService'
import { meService } from '@/services/MeService'
import { AuthContext, type AuthContexto } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [sessao, setSessao] = useState<Sessao | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<ApiError | null>(null)

  const carregarSessao = useCallback(async (atual: Session | null) => {
    setSession(atual)
    if (!atual) {
      setSessao(null)
      setErro(null)
      setCarregando(false)
      return
    }
    try {
      setSessao(await meService.sessao())
      setErro(null)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        await supabase.auth.signOut()
        setSessao(null)
      } else {
        setErro(e instanceof ApiError ? e : ApiError.rede())
      }
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    // INITIAL_SESSION, SIGNED_IN, SIGNED_OUT... O setTimeout evita chamar o Supabase
    // de dentro do próprio callback (recomendação da biblioteca).
    const { data } = supabase.auth.onAuthStateChange((evento, atual) => {
      if (evento === 'TOKEN_REFRESHED') {
        setSession(atual)
        return
      }
      setTimeout(() => void carregarSessao(atual), 0)
    })
    return () => data.subscription.unsubscribe()
  }, [carregarSessao])

  const entrar = useCallback(async (email: string, senha: string) => {
    const { error } = await supabase.auth.signInWithPassword({ email, password: senha })
    if (error) throw error
  }, [])

  const sair = useCallback(async () => {
    await supabase.auth.signOut()
  }, [])

  const recarregar = useCallback(async () => {
    setCarregando(true)
    const { data } = await supabase.auth.getSession()
    await carregarSessao(data.session)
  }, [carregarSessao])

  /**
   * A conta é criada pela aplicação Java já confirmada (decisão D10) e o responsável entra em seguida.
   * Em produção, com servidor de e-mail configurado, voltar para supabase.auth.signUp com confirmação.
   */
  const criarConta = useCallback(async (email: string, senha: string) => {
    await contaService.criar(email, senha)
    const { data, error } = await supabase.auth.signInWithPassword({ email, password: senha })
    if (error) throw error
    return data.session !== null
  }, [])

  const recuperarSenha = useCallback(async (email: string) => {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/redefinir-senha`,
    })
    if (error) throw error
  }, [])

  const redefinirSenha = useCallback(async (novaSenha: string) => {
    const { error } = await supabase.auth.updateUser({ password: novaSenha })
    if (error) throw error
  }, [])

  const valor = useMemo<AuthContexto>(
    () => ({ carregando, session, sessao, erro, entrar, sair, recarregar, criarConta, recuperarSenha, redefinirSenha }),
    [carregando, session, sessao, erro, entrar, sair, recarregar, criarConta, recuperarSenha, redefinirSenha],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
