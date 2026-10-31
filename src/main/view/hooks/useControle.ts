import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { FiltroAuditoria, StatusSolicitacao } from '@/models/controle'
import { controleService } from '@/services/ControleService'

/* ---- Aprovações ---- */

export function useAprovacoes(status?: StatusSolicitacao, habilitado = true) {
  return useQuery({
    queryKey: ['aprovacoes', status ?? 'todas'],
    queryFn: () => controleService.aprovacoes(status),
    enabled: habilitado,
  })
}

export function useResumoAprovacoes(habilitado = true) {
  return useQuery({ queryKey: ['aprovacoes', 'resumo'], queryFn: () => controleService.resumoAprovacoes(), enabled: habilitado })
}

/** Aprovar muda bolsas, cobranças em aberto e preços: recarrega tudo isso. */
export function useDecidirAprovacao() {
  const cliente = useQueryClient()
  const atualizar = () => {
    for (const chave of ['aprovacoes', 'bolsas', 'cobrancas', 'matriculas', 'precos', 'inadimplencia', 'resumo-financeiro', 'auditoria']) {
      void cliente.invalidateQueries({ queryKey: [chave] })
    }
  }
  return {
    aprovar: useMutation({ mutationFn: (id: string) => controleService.aprovar(id), onSuccess: atualizar }),
    rejeitar: useMutation({
      mutationFn: (p: { id: string; motivo: string }) => controleService.rejeitar(p.id, p.motivo),
      onSuccess: atualizar,
    }),
    cancelar: useMutation({ mutationFn: (id: string) => controleService.cancelar(id), onSuccess: atualizar }),
  }
}

/* ---- SoD ---- */

export function usePoliticaSod(habilitado = true) {
  return useQuery({ queryKey: ['permissoes', 'segregacao'], queryFn: () => controleService.politicaSod(), enabled: habilitado })
}

/* ---- Auditoria ---- */

export function useAuditoria(filtro: FiltroAuditoria) {
  return useQuery({
    queryKey: ['auditoria', filtro],
    queryFn: () => controleService.auditoria(filtro),
    placeholderData: keepPreviousData,
  })
}

export function useOpcoesAuditoria() {
  const autores = useQuery({ queryKey: ['auditoria', 'autores'], queryFn: () => controleService.autoresAuditoria() })
  const tabelas = useQuery({
    queryKey: ['auditoria', 'tabelas'],
    queryFn: () => controleService.tabelasAuditadas(),
    staleTime: Infinity,
  })
  return { autores, tabelas }
}

/** Verificação da cadeia de hashes: só roda quando o gestor pede. */
export function useVerificarAuditoria() {
  return useMutation({ mutationFn: () => controleService.verificar() })
}

/* ---- Revisão de acessos ---- */

export function useRevisaoAcessos() {
  return useQuery({ queryKey: ['revisao-acessos'], queryFn: () => controleService.revisaoAcessos() })
}

export function useRegistrarRevisao() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (observacao: string) => controleService.registrarRevisao(observacao),
    onSuccess: (relatorio) => {
      cliente.setQueryData(['revisao-acessos'], relatorio)
      void cliente.invalidateQueries({ queryKey: ['auditoria'] })
    },
  })
}
