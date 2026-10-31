import type { ReactNode } from 'react'
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

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <TemaProvider>
        <AuthProvider>{children}</AuthProvider>
      </TemaProvider>
    </QueryClientProvider>
  )
}
