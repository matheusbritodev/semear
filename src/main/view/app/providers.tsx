import type { ReactNode } from 'react'
import { IconContext, type IconProps } from '@phosphor-icons/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/auth/AuthProvider'
import { ApiError } from '@/lib/ApiError'
import { TemaProvider } from '@/tema/TemaProvider'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Não insiste em erros de regra/permissão (4xx); só em falhas de rede/servidor.
      retry: (tentativas, erro) => !(erro instanceof ApiError && erro.status >= 400 && erro.status < 500) && tentativas < 2,
    },
  },
})

// Estilo padrão dos ícones (components/ui/icones.ts): contorno com preenchimento suave.
const ICONES: IconProps = { weight: 'duotone' }

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <IconContext.Provider value={ICONES}>
        <TemaProvider>
          <AuthProvider>{children}</AuthProvider>
        </TemaProvider>
      </IconContext.Provider>
    </QueryClientProvider>
  )
}
