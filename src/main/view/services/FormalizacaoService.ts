import type { HttpClient } from '@/lib/HttpClient'
import type { ContratoTermo, ListaMaterial, Serie } from '@/models/formalizacao'
import { http } from './http'

/** Cliente de ContratoController, MaterialController e SerieController (área da escola). */
export class FormalizacaoService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  contrato(id: string): Promise<ContratoTermo> {
    return this.http.get<ContratoTermo>(`/contratos/${id}`)
  }

  registrarAssinatura(id: string, responsavelId: string): Promise<ContratoTermo> {
    return this.http.post<ContratoTermo>(`/contratos/${id}/assinatura`, { responsavelId })
  }

  series(): Promise<Serie[]> {
    return this.http.get<Serie[]>('/series')
  }

  materiais(serieId: string, ano: number): Promise<ListaMaterial> {
    return this.http.get<ListaMaterial>('/materiais', { serieId, ano })
  }

  adicionarItem(serieId: string, ano: number, descricao: string, quantidade: number): Promise<ListaMaterial> {
    return this.http.post<ListaMaterial>('/materiais/itens', { serieId, ano, descricao, quantidade })
  }

  alterarItem(id: string, descricao: string, quantidade: number): Promise<ListaMaterial> {
    return this.http.put<ListaMaterial>(`/materiais/itens/${id}`, { descricao, quantidade })
  }

  removerItem(id: string): Promise<ListaMaterial> {
    return this.http.delete<ListaMaterial>(`/materiais/itens/${id}`)
  }

  copiarLista(serieId: string, deAno: number, paraAno: number): Promise<ListaMaterial> {
    return this.http.post<ListaMaterial>('/materiais/copiar', { serieId, deAno, paraAno })
  }
}

export const formalizacaoService = new FormalizacaoService(http)
