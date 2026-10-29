import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { OrigemPagamento, StatusPreMatricula } from '@/models/enums'
import { matriculaService, type FiltroMatriculas } from '@/services/MatriculaService'

/** Tudo que muda quando uma matrícula nasce ou muda de status. */
const afetadas = [['matriculas'], ['pre-matriculas'], ['turmas'], ['alunos'], ['alertas'], ['rematricula'], ['cobrancas'], ['inadimplencia'], ['resumo-financeiro']]

function useInvalidarMatriculas() {
  const cliente = useQueryClient()
  return () => afetadas.forEach((queryKey) => void cliente.invalidateQueries({ queryKey }))
}

export function usePreMatriculas(status?: StatusPreMatricula, ano?: number) {
  return useQuery({ queryKey: ['pre-matriculas', status, ano], queryFn: () => matriculaService.preMatriculas(status, ano) })
}

export function usePreMatricula(id: string) {
  return useQuery({ queryKey: ['pre-matriculas', 'detalhe', id], queryFn: () => matriculaService.preMatricula(id) })
}

export function useAprovarPreMatricula(id: string) {
  const invalidar = useInvalidarMatriculas()
  return useMutation({ mutationFn: (turmaId: string) => matriculaService.aprovar(id, turmaId), onSuccess: invalidar })
}

export function useRejeitarPreMatricula(id: string) {
  const invalidar = useInvalidarMatriculas()
  return useMutation({ mutationFn: (motivo: string) => matriculaService.rejeitar(id, motivo), onSuccess: invalidar })
}

export function useMatriculas(filtro: FiltroMatriculas, habilitado = true) {
  return useQuery({
    queryKey: ['matriculas', filtro],
    queryFn: () => matriculaService.pesquisar(filtro),
    placeholderData: keepPreviousData,
    enabled: habilitado,
  })
}

export function useMatricula(id: string) {
  return useQuery({ queryKey: ['matriculas', 'detalhe', id], queryFn: () => matriculaService.buscar(id) })
}

export function useCancelarMatricula(id: string) {
  const invalidar = useInvalidarMatriculas()
  return useMutation({ mutationFn: (motivo: string) => matriculaService.cancelar(id, motivo), onSuccess: invalidar })
}

export function useRegistrarPagamentoMatricula(id: string) {
  const invalidar = useInvalidarMatriculas()
  return useMutation({
    mutationFn: (origem: OrigemPagamento) => matriculaService.registrarPagamentoMatricula(id, origem),
    onSuccess: invalidar,
  })
}

export function useOpcaoRematricula(alunoId: string, habilitado = true) {
  return useQuery({
    queryKey: ['rematricula', alunoId],
    queryFn: () => matriculaService.opcaoRematricula(alunoId),
    enabled: habilitado,
  })
}

export function useRematricularPelaEscola() {
  const invalidar = useInvalidarMatriculas()
  return useMutation({
    mutationFn: (p: { alunoId: string; fardaUniforme: string; fardaEdFisica: string }) =>
      matriculaService.rematricular(p.alunoId, p.fardaUniforme, p.fardaEdFisica),
    onSuccess: invalidar,
  })
}
