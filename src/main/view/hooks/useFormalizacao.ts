import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ListaMaterial } from '@/models/formalizacao'
import { formalizacaoService } from '@/services/FormalizacaoService'
import { portalService } from '@/services/PortalService'

/** Assinar muda o contrato, a matrícula e os avisos do portal. */
function useInvalidarContratos() {
  const cliente = useQueryClient()
  return () => {
    for (const queryKey of [['contratos'], ['matriculas'], ['portal', 'filhos'], ['portal', 'contratos']]) {
      void cliente.invalidateQueries({ queryKey })
    }
  }
}

/* ---- Escola ---- */

export function useContrato(id: string) {
  return useQuery({ queryKey: ['contratos', id], queryFn: () => formalizacaoService.contrato(id) })
}

export function useRegistrarAssinatura(id: string) {
  const invalidar = useInvalidarContratos()
  return useMutation({
    mutationFn: (responsavelId: string) => formalizacaoService.registrarAssinatura(id, responsavelId),
    onSuccess: invalidar,
  })
}

export function useSeries() {
  return useQuery({ queryKey: ['series'], queryFn: () => formalizacaoService.series(), staleTime: 60 * 60_000 })
}

export function useListaMaterial(serieId?: string, ano?: number) {
  return useQuery({
    queryKey: ['materiais', serieId, ano],
    queryFn: () => formalizacaoService.materiais(serieId!, ano!),
    enabled: Boolean(serieId) && ano !== undefined,
    placeholderData: keepPreviousData,
  })
}

/** Cada operação devolve a lista atualizada: grava direto no cache. */
export function useEditarLista(serieId?: string, ano?: number) {
  const cliente = useQueryClient()
  const atualizar = (lista: ListaMaterial) => {
    cliente.setQueryData(['materiais', serieId, ano], lista)
    void cliente.invalidateQueries({ queryKey: ['portal', 'materiais'] })
  }
  return {
    adicionar: useMutation({
      mutationFn: (p: { descricao: string; quantidade: number }) =>
        formalizacaoService.adicionarItem(serieId!, ano!, p.descricao, p.quantidade),
      onSuccess: atualizar,
    }),
    alterar: useMutation({
      mutationFn: (p: { id: string; descricao: string; quantidade: number }) =>
        formalizacaoService.alterarItem(p.id, p.descricao, p.quantidade),
      onSuccess: atualizar,
    }),
    remover: useMutation({ mutationFn: (id: string) => formalizacaoService.removerItem(id), onSuccess: atualizar }),
    copiar: useMutation({
      mutationFn: (deAno: number) => formalizacaoService.copiarLista(serieId!, deAno, ano!),
      onSuccess: atualizar,
    }),
  }
}

/* ---- Portal ---- */

export function useContratosDoFilho(alunoId: string) {
  return useQuery({ queryKey: ['portal', 'contratos', 'aluno', alunoId], queryFn: () => portalService.contratosDoFilho(alunoId) })
}

export function useContratoPortal(id: string) {
  return useQuery({ queryKey: ['portal', 'contratos', id], queryFn: () => portalService.contrato(id) })
}

export function useAssinarContrato() {
  const invalidar = useInvalidarContratos()
  return useMutation({ mutationFn: (id: string) => portalService.assinarContrato(id), onSuccess: invalidar })
}

export function useMateriaisDoFilho(alunoId: string) {
  return useQuery({ queryKey: ['portal', 'materiais', alunoId], queryFn: () => portalService.materiaisDoFilho(alunoId), retry: false })
}

export function useBoletimDoFilho(alunoId: string, ano?: number) {
  return useQuery({
    queryKey: ['portal', 'boletim', alunoId, ano],
    queryFn: () => portalService.boletimDoFilho(alunoId, ano),
    placeholderData: keepPreviousData,
    retry: false,
  })
}
