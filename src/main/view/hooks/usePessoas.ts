import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { TipoFuncionario } from '@/models/enums'
import type { AtualizarFuncionario, NovoFuncionario } from '@/models/pessoa'
import { alunoService, type FiltroAlunos } from '@/services/AlunoService'
import { funcionarioService } from '@/services/FuncionarioService'
import { responsavelService } from '@/services/ResponsavelService'

/* ---- Alunos ---- */

export function useAlunos(filtro: FiltroAlunos, habilitado = true) {
  return useQuery({
    queryKey: ['alunos', filtro],
    queryFn: () => alunoService.pesquisar(filtro),
    placeholderData: keepPreviousData,
    enabled: habilitado,
  })
}

export function useAluno(id: string) {
  return useQuery({ queryKey: ['alunos', 'detalhe', id], queryFn: () => alunoService.buscar(id) })
}

export function useDefinirResponsavelFinanceiro(alunoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (responsavelId: string) => alunoService.definirResponsavelFinanceiro(alunoId, responsavelId),
    onSuccess: (aluno) => {
      cliente.setQueryData(['alunos', 'detalhe', alunoId], aluno)
      void cliente.invalidateQueries({ queryKey: ['alunos'] })
      void cliente.invalidateQueries({ queryKey: ['responsaveis'] })
    },
  })
}

/* ---- Responsáveis ---- */

export function useResponsaveis(busca: string, pagina: number) {
  return useQuery({
    queryKey: ['responsaveis', busca, pagina],
    queryFn: () => responsavelService.pesquisar(busca, pagina),
    placeholderData: keepPreviousData,
  })
}

export function useResponsavel(id: string) {
  return useQuery({ queryKey: ['responsaveis', 'detalhe', id], queryFn: () => responsavelService.buscar(id) })
}

export function useAtualizarContato(id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: ({ telefone, email }: { telefone: string; email: string }) =>
      responsavelService.atualizarContato(id, telefone, email),
    onSuccess: (responsavel) => {
      cliente.setQueryData(['responsaveis', 'detalhe', id], responsavel)
      void cliente.invalidateQueries({ queryKey: ['responsaveis'] })
      void cliente.invalidateQueries({ queryKey: ['alunos', 'detalhe'] })
    },
  })
}

/* ---- Funcionários ---- */

export function useFuncionarios(busca: string, tipo?: TipoFuncionario) {
  return useQuery({
    queryKey: ['funcionarios', busca, tipo],
    queryFn: () => funcionarioService.pesquisar(busca, tipo),
    placeholderData: keepPreviousData,
  })
}

export function useSalvarFuncionario() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (pedido: { id?: string; novo?: NovoFuncionario; dados?: AtualizarFuncionario }) =>
      pedido.id && pedido.dados
        ? funcionarioService.atualizar(pedido.id, pedido.dados)
        : funcionarioService.criar(pedido.novo!),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['funcionarios'] }),
  })
}

export function useAlterarAtivoFuncionario() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ativo }: { id: string; ativo: boolean }) => funcionarioService.alterarAtivo(id, ativo),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['funcionarios'] }),
  })
}
