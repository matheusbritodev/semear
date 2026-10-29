import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { DadosPlano, TipoCalendario } from '@/models/pedagogico'
import { pedagogicoService, type Presenca } from '@/services/PedagogicoService'

export function useTurmasPedagogicas(ano?: number) {
  return useQuery({
    queryKey: ['pedagogico', 'turmas', ano],
    queryFn: () => pedagogicoService.turmas(ano),
    enabled: ano !== undefined,
  })
}

export function useChamada(turmaId: string, data: string) {
  return useQuery({
    queryKey: ['pedagogico', 'chamada', turmaId, data],
    queryFn: () => pedagogicoService.chamada(turmaId, data),
    placeholderData: keepPreviousData,
  })
}

/** Gravar o diário muda chamada, lista de diários, frequência e o boletim do portal. */
export function useRegistrarDiario(turmaId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (p: { data: string; conteudo: string; presencas: Presenca[] }) =>
      pedagogicoService.registrarDiario(turmaId, p.data, p.conteudo, p.presencas),
    onSuccess: (chamada) => {
      cliente.setQueryData(['pedagogico', 'chamada', turmaId, chamada.data], chamada)
      for (const queryKey of [['pedagogico', 'diarios', turmaId], ['pedagogico', 'frequencia', turmaId], ['pedagogico', 'turmas']]) {
        void cliente.invalidateQueries({ queryKey })
      }
    },
  })
}

export function useDiarios(turmaId: string, mes: string) {
  return useQuery({
    queryKey: ['pedagogico', 'diarios', turmaId, mes],
    queryFn: () => pedagogicoService.diarios(turmaId, mes),
    placeholderData: keepPreviousData,
  })
}

export function useFrequenciaTurma(turmaId: string) {
  return useQuery({ queryKey: ['pedagogico', 'frequencia', turmaId], queryFn: () => pedagogicoService.frequencia(turmaId) })
}

export function usePlanos(turmaId: string, de: string, ate: string) {
  return useQuery({
    queryKey: ['pedagogico', 'planos', turmaId, de, ate],
    queryFn: () => pedagogicoService.planos(turmaId, de, ate),
    placeholderData: keepPreviousData,
  })
}

export function useSalvarPlano(turmaId: string) {
  const cliente = useQueryClient()
  const atualizar = () => void cliente.invalidateQueries({ queryKey: ['pedagogico', 'planos', turmaId] })
  return {
    salvar: useMutation({
      mutationFn: ({ id, dados }: { id?: string; dados: DadosPlano }) =>
        id ? pedagogicoService.alterarPlano(id, dados) : pedagogicoService.criarPlano(turmaId, dados).then(() => undefined),
      onSuccess: atualizar,
    }),
    remover: useMutation({ mutationFn: (id: string) => pedagogicoService.removerPlano(id), onSuccess: atualizar }),
  }
}

export function useNotasTurma(turmaId: string, disciplinaId?: string, bimestre?: number) {
  return useQuery({
    queryKey: ['pedagogico', 'notas', turmaId, disciplinaId, bimestre],
    queryFn: () => pedagogicoService.notas(turmaId, disciplinaId, bimestre),
    placeholderData: keepPreviousData,
  })
}

export function useLancarNotas(turmaId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (p: { disciplinaId: string; bimestre: number; notas: { matriculaId: string; valor: number | null }[] }) =>
      pedagogicoService.lancarNotas(turmaId, p.disciplinaId, p.bimestre, p.notas),
    onSuccess: (notas) => {
      cliente.setQueryData(['pedagogico', 'notas', turmaId, notas.disciplinaId, notas.bimestre], notas)
      void cliente.invalidateQueries({ queryKey: ['portal', 'boletim'] })
    },
  })
}

export function useCalendario(ano?: number) {
  return useQuery({
    queryKey: ['calendario', ano],
    queryFn: () => pedagogicoService.calendario(ano),
    enabled: ano !== undefined,
  })
}

export function useEditarCalendario(ano?: number) {
  const cliente = useQueryClient()
  const gravar = (eventos: Awaited<ReturnType<typeof pedagogicoService.calendario>>) => {
    cliente.setQueryData(['calendario', ano], eventos)
    void cliente.invalidateQueries({ queryKey: ['pedagogico', 'chamada'] })
  }
  return {
    criar: useMutation({
      mutationFn: (p: { data: string; tipo: TipoCalendario; descricao: string }) =>
        pedagogicoService.criarEvento(ano!, p.data, p.tipo, p.descricao),
      onSuccess: gravar,
    }),
    remover: useMutation({ mutationFn: (id: string) => pedagogicoService.removerEvento(id), onSuccess: gravar }),
  }
}

export function useFormacoes(funcionarioId: string) {
  return useQuery({ queryKey: ['formacoes', funcionarioId], queryFn: () => pedagogicoService.formacoes(funcionarioId) })
}

export function useEditarFormacoes(funcionarioId: string) {
  const cliente = useQueryClient()
  const gravar = (lista: Awaited<ReturnType<typeof pedagogicoService.formacoes>>) =>
    cliente.setQueryData(['formacoes', funcionarioId], lista)
  return {
    registrar: useMutation({
      mutationFn: (p: { curso: string; cargaHoraria: number; concluidaEm: string | null }) =>
        pedagogicoService.registrarFormacao(funcionarioId, p.curso, p.cargaHoraria, p.concluidaEm),
      onSuccess: gravar,
    }),
    remover: useMutation({ mutationFn: (id: string) => pedagogicoService.removerFormacao(funcionarioId, id), onSuccess: gravar }),
  }
}
