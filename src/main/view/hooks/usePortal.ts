import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { NovaPreMatricula } from '@/models/matricula'
import { portalService } from '@/services/PortalService'

export function useFilhos(habilitado = true) {
  return useQuery({ queryKey: ['portal', 'filhos'], queryFn: () => portalService.filhos(), enabled: habilitado })
}

export function useMinhasPreMatriculas(habilitado = true) {
  return useQuery({ queryKey: ['portal', 'pre-matriculas'], queryFn: () => portalService.preMatriculas(), enabled: habilitado })
}

export function useOpcoesMatricula() {
  return useQuery({ queryKey: ['portal', 'opcoes'], queryFn: () => portalService.opcoesMatricula(), staleTime: 5 * 60_000 })
}

export function useNovaPreMatricula() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (dados: NovaPreMatricula) => portalService.novaPreMatricula(dados),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['portal'] }),
  })
}

export function useOpcoesRematricula() {
  return useQuery({ queryKey: ['portal', 'rematricula'], queryFn: () => portalService.opcoesRematricula() })
}

export function useRematricular() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (p: { alunoId: string; fardaUniforme: string; fardaEdFisica: string }) =>
      portalService.rematricular(p.alunoId, p.fardaUniforme, p.fardaEdFisica),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['portal'] }),
  })
}
