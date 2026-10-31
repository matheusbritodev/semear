import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AlteracaoBolsa, Bolsa, DadosBolsa } from '@/models/financeiro'
import { financeiroService, type FiltroCobrancas } from '@/services/FinanceiroService'
import { portalService } from '@/services/PortalService'

/** Tudo que muda quando uma cobrança é paga, gerada ou recalculada por bolsa. */
const afetadas = [
  ['cobrancas'],
  ['inadimplencia'],
  ['resumo-financeiro'],
  ['matriculas'],
  ['rematricula'],
  ['bolsas'],
  ['portal', 'cobrancas'],
]

function useInvalidarFinanceiro() {
  const cliente = useQueryClient()
  return () => afetadas.forEach((queryKey) => void cliente.invalidateQueries({ queryKey }))
}

export function useCobrancas(filtro: FiltroCobrancas, habilitado = true) {
  return useQuery({
    queryKey: ['cobrancas', filtro],
    queryFn: () => financeiroService.cobrancas(filtro),
    placeholderData: keepPreviousData,
    enabled: habilitado,
  })
}

export function useCobranca(id: string | null, data?: string) {
  return useQuery({
    queryKey: ['cobrancas', 'detalhe', id, data],
    queryFn: () => financeiroService.cobranca(id!, data),
    enabled: id !== null,
  })
}

export function usePagarPresencial() {
  const invalidar = useInvalidarFinanceiro()
  return useMutation({ mutationFn: (id: string) => financeiroService.pagarPresencial(id), onSuccess: invalidar })
}

export function useGerarCobrancas() {
  const invalidar = useInvalidarFinanceiro()
  return useMutation({
    mutationFn: (p: { ano: number; mesInicio: number; mesFim: number }) =>
      financeiroService.gerar(p.ano, p.mesInicio, p.mesFim),
    onSuccess: invalidar,
  })
}

export function useInadimplencia(habilitado = true) {
  return useQuery({ queryKey: ['inadimplencia'], queryFn: () => financeiroService.inadimplencia(), enabled: habilitado })
}

export function useResumoFinanceiro(ano?: number) {
  return useQuery({
    queryKey: ['resumo-financeiro', ano],
    queryFn: () => financeiroService.resumo(ano),
    enabled: ano !== undefined,
    placeholderData: keepPreviousData,
  })
}

/* ---- Bolsas ---- */

export function useBolsas(habilitado = true) {
  return useQuery({ queryKey: ['bolsas'], queryFn: () => financeiroService.bolsas(), enabled: habilitado })
}

export function useSalvarBolsa() {
  const invalidar = useInvalidarFinanceiro()
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dados }: { id?: string; dados: DadosBolsa }): Promise<Bolsa | AlteracaoBolsa> =>
      id ? financeiroService.alterarBolsa(id, dados) : financeiroService.criarBolsa(dados),
    onSuccess: () => {
      invalidar()
      void cliente.invalidateQueries({ queryKey: ['aprovacoes'] })
    },
  })
}

export function useBolsistas(ano?: number) {
  return useQuery({
    queryKey: ['bolsas', 'bolsistas', ano],
    queryFn: () => financeiroService.bolsistas(ano),
    enabled: ano !== undefined,
  })
}

export function useBolsasDaMatricula(matriculaId: string, habilitado = true) {
  return useQuery({
    queryKey: ['bolsas', 'matricula', matriculaId],
    queryFn: () => financeiroService.bolsasDaMatricula(matriculaId),
    enabled: habilitado,
  })
}

/** Pedir bolsa cria um pedido de aprovação (nada muda nas cobranças até alguém aprovar). */
export function useConcederBolsa(matriculaId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (bolsaId: string) => financeiroService.conceder(matriculaId, bolsaId),
    onSuccess: () => void cliente.invalidateQueries({ queryKey: ['aprovacoes'] }),
  })
}

export function useRevogarBolsa(matriculaId: string) {
  const invalidar = useInvalidarFinanceiro()
  return useMutation({
    mutationFn: (concessaoId: string) => financeiroService.revogar(matriculaId, concessaoId),
    onSuccess: invalidar,
  })
}

/* ---- Portal ---- */

export function useMinhasCobrancas() {
  return useQuery({ queryKey: ['portal', 'cobrancas'], queryFn: () => portalService.cobrancas() })
}

export function useMinhaCobranca(id: string) {
  return useQuery({ queryKey: ['portal', 'cobrancas', id], queryFn: () => portalService.cobranca(id) })
}

export function useSimularPagamento() {
  const invalidar = useInvalidarFinanceiro()
  return useMutation({ mutationFn: (id: string) => portalService.simularPagamento(id), onSuccess: invalidar })
}
