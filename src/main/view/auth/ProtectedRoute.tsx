import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { TelaCarregando } from '@/components/comuns/TelaCarregando'
import { Button } from '@/components/ui/Button'
import { usaPortal } from '@/models/enums'
import { useAuth } from './useAuth'

interface Props {
  area: 'escola' | 'portal'
  children: ReactNode
}

/** Exige login e envia cada perfil para a sua área (escola ou portal). */
export function ProtectedRoute({ area, children }: Props) {
  const { carregando, session, sessao, erro, recarregar } = useAuth()
  const local = useLocation()

  if (carregando) return <TelaCarregando />
  if (!session) return <Navigate to="/login" replace state={{ de: local.pathname }} />

  if (erro) {
    return (
      <div className="mx-auto mt-24 max-w-md space-y-4 px-4 text-center">
        <p className="text-slate-700">{erro.mensagemAmigavel}</p>
        <Button variante="secundario" onClick={() => void recarregar()}>
          Tentar novamente
        </Button>
      </div>
    )
  }

  const perfil = sessao?.perfil ?? null
  if (area === 'escola' && (!perfil || usaPortal(perfil))) return <Navigate to="/portal" replace />
  if (area === 'portal' && perfil && !usaPortal(perfil)) return <Navigate to="/escola" replace />

  return <>{children}</>
}

/** Rota "/": manda para o login ou para a área do perfil. */
export function RedirecionarPorPerfil() {
  const { carregando, session, sessao } = useAuth()
  if (carregando) return <TelaCarregando />
  if (!session) return <Navigate to="/login" replace />
  const perfil = sessao?.perfil
  return <Navigate to={perfil && !usaPortal(perfil) ? '/escola' : '/portal'} replace />
}
