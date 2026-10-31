import type { HttpClient } from '@/lib/HttpClient'
import type { Solicitacao } from '@/models/controle'
import type { Pagina } from '@/models/comum'
import type { StatusCobranca, TipoCobranca } from '@/models/enums'
import type {
  AlteracaoBolsa,
  Bolsa,
  Cobranca,
  CobrancaDetalhe,
  Concessao,
  DadosBolsa,
  Geracao,
  Inadimplencia,
  ResumoAno,
} from '@/models/financeiro'
import { http } from './http'

export type FiltroCobrancas = {
  ano?: number
  mes?: number
  tipo?: TipoCobranca
  status?: StatusCobranca
  busca?: string
  matriculaId?: string
  pagina?: number
  tamanho?: number
}

type Parametros = Record<string, string | number | boolean | undefined>
const limpar = (p: Parametros) => Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== ''))

/** Cliente de controller/financeiro/CobrancaController.java e BolsaController.java. */
export class FinanceiroService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  cobrancas(filtro: FiltroCobrancas): Promise<Pagina<Cobranca>> {
    return this.http.get<Pagina<Cobranca>>('/cobrancas', limpar({ ...filtro }))
  }

  cobranca(id: string, data?: string): Promise<CobrancaDetalhe> {
    return this.http.get<CobrancaDetalhe>(`/cobrancas/${id}`, limpar({ data }))
  }

  pagarPresencial(id: string): Promise<CobrancaDetalhe> {
    return this.http.post<CobrancaDetalhe>(`/cobrancas/${id}/pagamento`, { origem: 'PRESENCIAL' })
  }

  gerar(ano: number, mesInicio: number, mesFim: number): Promise<Geracao> {
    return this.http.post<Geracao>('/cobrancas/geracao', { ano, mesInicio, mesFim })
  }

  inadimplencia(): Promise<Inadimplencia> {
    return this.http.get<Inadimplencia>('/financeiro/inadimplencia')
  }

  resumo(ano?: number): Promise<ResumoAno> {
    return this.http.get<ResumoAno>('/financeiro/resumo', limpar({ ano }))
  }

  /* ---- Bolsas ---- */

  bolsas(): Promise<Bolsa[]> {
    return this.http.get<Bolsa[]>('/bolsas')
  }

  criarBolsa(dados: DadosBolsa): Promise<Bolsa> {
    return this.http.post<Bolsa>('/bolsas', dados)
  }

  /** Mudar o desconto ou reativar volta com "solicitacao" (precisa da aprovação de outra pessoa). */
  alterarBolsa(id: string, dados: DadosBolsa): Promise<AlteracaoBolsa> {
    return this.http.put<AlteracaoBolsa>(`/bolsas/${id}`, dados)
  }

  bolsistas(ano?: number): Promise<Concessao[]> {
    return this.http.get<Concessao[]>('/bolsas/bolsistas', limpar({ ano }))
  }

  bolsasDaMatricula(matriculaId: string): Promise<Concessao[]> {
    return this.http.get<Concessao[]>(`/matriculas/${matriculaId}/bolsas`)
  }

  /** Pede a bolsa: só vale depois que outra pessoa aprovar. */
  conceder(matriculaId: string, bolsaId: string): Promise<Solicitacao> {
    return this.http.post<Solicitacao>(`/matriculas/${matriculaId}/bolsas`, { bolsaId })
  }

  revogar(matriculaId: string, concessaoId: string): Promise<Concessao[]> {
    return this.http.delete<Concessao[]>(`/matriculas/${matriculaId}/bolsas/${concessaoId}`)
  }
}

export const financeiroService = new FinanceiroService(http)
