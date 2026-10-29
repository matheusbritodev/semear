import { useContext } from 'react'
import { AuthContext, type AuthContexto } from './AuthContext'

export function useAuth(): AuthContexto {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  return contexto
}
