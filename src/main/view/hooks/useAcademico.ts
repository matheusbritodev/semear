import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { turmaService } from '@/services/TurmaService'

export const chavesAcademico = {
  anos: ['anos-letivos'] as const,
  anoCorrente: ['anos-letivos', 'corrente'] as const,
  turmas: (ano?: number) => ['turmas', ano ?? 'corrente'] as const,
  turma: (id: string) => ['turmas', 'detalhe', id] as const,
  alertas: ['alertas'] as const,
}

export function useAnosLetivos() {
  return useQuery({ queryKey: chavesAcademico.anos, queryFn: () => turmaService.anosLetivos(), staleTime: 5 * 60_000 })
}

export function useAnoCorrente() {
  return useQuery({ queryKey: chavesAcademico.anoCorrente, queryFn: () => turmaService.anoCorrente(), staleTime: 5 * 60_000 })
}

export function useTurmas(ano?: number, habilitado = true) {
  return useQuery({ queryKey: chavesAcademico.turmas(ano), queryFn: () => turmaService.listar(ano), enabled: habilitado })
}

export function useTurma(id: string) {
  return useQuery({ queryKey: chavesAcademico.turma(id), queryFn: () => turmaService.buscar(id) })
}

export function useAlertas(habilitado = true) {
  return useQuery({ queryKey: chavesAcademico.alertas, queryFn: () => turmaService.alertas(true), enabled: habilitado })
}

export function useAlterarCapacidade(id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (capacidade: number) => turmaService.alterarCapacidade(id, capacidade),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['turmas'] }),
  })
}

export function useMarcarAlertaLido() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => turmaService.marcarAlertaComoLido(id),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chavesAcademico.alertas }),
  })
}
