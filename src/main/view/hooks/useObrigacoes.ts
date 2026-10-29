import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { DadosObrigacao } from '@/models/obrigacao'
import { obrigacaoService } from '@/services/ObrigacaoService'

export function useObrigacoes() {
  return useQuery({ queryKey: ['obrigacoes'], queryFn: () => obrigacaoService.listar() })
}

export function useAlertasObrigacoes(habilitado = true) {
  return useQuery({ queryKey: ['obrigacoes', 'alertas'], queryFn: () => obrigacaoService.alertas(), enabled: habilitado })
}

export function useResponsaveisObrigacao(habilitado = true) {
  return useQuery({
    queryKey: ['obrigacoes', 'responsaveis'],
    queryFn: () => obrigacaoService.responsaveis(),
    enabled: habilitado,
    staleTime: 10 * 60_000,
  })
}

/** Toda mudança atualiza a lista e o card do painel. */
export function useEditarObrigacoes() {
  const cliente = useQueryClient()
  const atualizar = () => void cliente.invalidateQueries({ queryKey: ['obrigacoes'] })
  return {
    salvar: useMutation({
      mutationFn: ({ id, dados }: { id?: string; dados: DadosObrigacao }) =>
        id ? obrigacaoService.alterar(id, dados) : obrigacaoService.criar(dados),
      onSuccess: atualizar,
    }),
    remover: useMutation({ mutationFn: (id: string) => obrigacaoService.remover(id), onSuccess: atualizar }),
    cumprir: useMutation({ mutationFn: (id: string) => obrigacaoService.cumprir(id), onSuccess: atualizar }),
    enviar: useMutation({
      mutationFn: ({ id, arquivo }: { id: string; arquivo: File }) => obrigacaoService.enviarComprovante(id, arquivo),
      onSuccess: atualizar,
    }),
    removerComprovante: useMutation({
      mutationFn: (comprovanteId: string) => obrigacaoService.removerComprovante(comprovanteId),
      onSuccess: atualizar,
    }),
  }
}

/** Abre o comprovante em nova aba com link temporário (a aba abre antes, para não ser bloqueada). */
export async function abrirComprovante(comprovanteId: string) {
  const aba = window.open('', '_blank')
  try {
    const { url } = await obrigacaoService.link(comprovanteId)
    if (aba) {
      aba.opener = null
      aba.location.href = url
    } else {
      window.location.href = url
    }
  } catch (e) {
    aba?.close()
    throw e
  }
}
