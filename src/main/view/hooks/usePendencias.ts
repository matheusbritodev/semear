import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/auth/useAuth'
import type { Modulo, PerfilUsuario } from '@/models/enums'
import { pendenciaService } from '@/services/PendenciaService'
import { permissaoService } from '@/services/PermissaoService'

export function usePendenciasDocumentos(habilitado = true) {
  return useQuery({ queryKey: ['pendencias', 'documentos'], queryFn: () => pendenciaService.documentos(), enabled: habilitado })
}

export function useResumoPendencias(habilitado = true) {
  return useQuery({ queryKey: ['pendencias', 'resumo'], queryFn: () => pendenciaService.resumo(), enabled: habilitado })
}

export function useMatrizPermissoes() {
  return useQuery({ queryKey: ['permissoes'], queryFn: () => permissaoService.matriz() })
}

/** Toda mudança atualiza a matriz e recarrega a sessão (o próprio menu pode mudar). */
export function useAlterarPermissoes() {
  const cliente = useQueryClient()
  const { recarregar } = useAuth()
  const aoMudar = (matriz: Awaited<ReturnType<typeof permissaoService.matriz>>) => {
    cliente.setQueryData(['permissoes'], matriz)
    void recarregar()
  }
  const alterar = useMutation({
    mutationFn: (p: { perfil: PerfilUsuario; modulo: Modulo; ler: boolean; gravar: boolean }) =>
      permissaoService.alterar(p.perfil, p.modulo, p.ler, p.gravar),
    onSuccess: aoMudar,
  })
  const restaurar = useMutation({ mutationFn: () => permissaoService.restaurarPadrao(), onSuccess: aoMudar })
  return { alterar, restaurar }
}
