import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PerfilUsuario } from '@/models/enums'
import { configuracaoService } from '@/services/ConfiguracaoService'
import { chavesAcademico } from './useAcademico'

/* ---- Usuários ---- */

export function useContas() {
  return useQuery({ queryKey: ['usuarios'], queryFn: () => configuracaoService.contas() })
}

export function useFuncionariosSemLogin(habilitado: boolean) {
  return useQuery({
    queryKey: ['usuarios', 'funcionarios-sem-login'],
    queryFn: () => configuracaoService.funcionariosSemLogin(),
    enabled: habilitado,
  })
}

export function useEditarContas() {
  const cliente = useQueryClient()
  const atualizar = () => {
    void cliente.invalidateQueries({ queryKey: ['usuarios'] })
    void cliente.invalidateQueries({ queryKey: ['funcionarios'] })
  }
  return {
    criar: useMutation({
      mutationFn: (p: { funcionarioId: string; email: string; perfil: PerfilUsuario }) =>
        configuracaoService.criarAcesso(p.funcionarioId, p.email, p.perfil),
      onSuccess: atualizar,
    }),
    perfil: useMutation({
      mutationFn: (p: { id: string; perfil: PerfilUsuario }) => configuracaoService.alterarPerfil(p.id, p.perfil),
      onSuccess: atualizar,
    }),
    ativo: useMutation({
      mutationFn: (p: { id: string; ativo: boolean }) => configuracaoService.alterarAtivo(p.id, p.ativo),
      onSuccess: atualizar,
    }),
    senha: useMutation({ mutationFn: (id: string) => configuracaoService.senhaTemporaria(id) }),
  }
}

/* ---- Ano letivo, preços e turmas ---- */

export function usePrecos(ano?: number) {
  return useQuery({ queryKey: ['precos', ano], queryFn: () => configuracaoService.precos(ano!), enabled: ano !== undefined })
}

export function useEditarAnoLetivo() {
  const cliente = useQueryClient()
  const atualizar = () => void cliente.invalidateQueries({ queryKey: chavesAcademico.anos })
  return {
    criar: useMutation({
      mutationFn: (p: { ano: number; inicio: string; fim: string }) => configuracaoService.criarAno(p.ano, p.inicio, p.fim),
      onSuccess: atualizar,
    }),
    alterar: useMutation({
      mutationFn: (p: { ano: number; inicio: string; fim: string }) => configuracaoService.alterarAno(p.ano, p.inicio, p.fim),
      onSuccess: atualizar,
    }),
  }
}

export function useEditarPrecos(ano?: number) {
  const cliente = useQueryClient()
  const gravar = (lista: Awaited<ReturnType<typeof configuracaoService.precos>>) => cliente.setQueryData(['precos', ano], lista)
  return {
    salvar: useMutation({ mutationFn: configuracaoService.salvarPreco.bind(configuracaoService), onSuccess: gravar }),
    copiar: useMutation({
      mutationFn: (p: { deAno: number; reajuste: number }) => configuracaoService.copiarPrecos(p.deAno, ano!, p.reajuste),
      onSuccess: gravar,
    }),
  }
}

export function useEditarTurmas() {
  const cliente = useQueryClient()
  const atualizar = () => void cliente.invalidateQueries({ queryKey: ['turmas'] })
  return {
    criar: useMutation({
      mutationFn: (p: { ano: number; serieId: string; nome: string; capacidade: number }) =>
        configuracaoService.criarTurma(p.ano, p.serieId, p.nome, p.capacidade),
      onSuccess: atualizar,
    }),
    remover: useMutation({ mutationFn: (id: string) => configuracaoService.removerTurma(id), onSuccess: atualizar }),
  }
}

export function useOpcoesProfessores(habilitado: boolean) {
  return useQuery({
    queryKey: ['turmas', 'opcoes-professores'],
    queryFn: () => configuracaoService.opcoesProfessores(),
    enabled: habilitado,
    staleTime: 5 * 60_000,
  })
}

export function useProfessoresDaTurma(turmaId: string) {
  const cliente = useQueryClient()
  const atualizar = () => {
    void cliente.invalidateQueries({ queryKey: chavesAcademico.turma(turmaId) })
    void cliente.invalidateQueries({ queryKey: ['pedagogico'] })
  }
  return {
    vincular: useMutation({
      mutationFn: (p: { funcionarioId: string; disciplinaId: string | null }) =>
        configuracaoService.vincularProfessor(turmaId, p.funcionarioId, p.disciplinaId),
      onSuccess: atualizar,
    }),
    remover: useMutation({ mutationFn: (vinculoId: string) => configuracaoService.removerProfessor(turmaId, vinculoId), onSuccess: atualizar }),
  }
}
