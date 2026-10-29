import type { HttpClient } from '@/lib/HttpClient'
import type { LinkArquivo } from '@/models/documento'
import type { DadosObrigacao, Obrigacao } from '@/models/obrigacao'
import { http } from './http'

/** Cliente de controller/institucional/ObrigacaoController.java. */
export class ObrigacaoService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  listar(): Promise<Obrigacao[]> {
    return this.http.get<Obrigacao[]>('/obrigacoes')
  }

  alertas(): Promise<Obrigacao[]> {
    return this.http.get<Obrigacao[]>('/obrigacoes/alertas')
  }

  responsaveis(): Promise<{ id: string; nome: string }[]> {
    return this.http.get<{ id: string; nome: string }[]>('/obrigacoes/responsaveis')
  }

  criar(dados: DadosObrigacao): Promise<Obrigacao> {
    return this.http.post<Obrigacao>('/obrigacoes', dados)
  }

  alterar(id: string, dados: DadosObrigacao): Promise<Obrigacao> {
    return this.http.put<Obrigacao>(`/obrigacoes/${id}`, dados)
  }

  remover(id: string): Promise<void> {
    return this.http.delete<void>(`/obrigacoes/${id}`)
  }

  cumprir(id: string): Promise<Obrigacao> {
    return this.http.post<Obrigacao>(`/obrigacoes/${id}/cumprir`)
  }

  enviarComprovante(id: string, arquivo: File): Promise<Obrigacao> {
    const formulario = new FormData()
    formulario.append('arquivo', arquivo)
    return this.http.enviarArquivo<Obrigacao>(`/obrigacoes/${id}/comprovantes`, formulario)
  }

  link(comprovanteId: string): Promise<LinkArquivo> {
    return this.http.get<LinkArquivo>(`/obrigacoes/comprovantes/${comprovanteId}/link`)
  }

  removerComprovante(comprovanteId: string): Promise<Obrigacao> {
    return this.http.delete<Obrigacao>(`/obrigacoes/comprovantes/${comprovanteId}`)
  }
}

export const obrigacaoService = new ObrigacaoService(http)
