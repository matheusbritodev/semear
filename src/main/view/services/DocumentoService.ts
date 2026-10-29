import type { HttpClient } from '@/lib/HttpClient'
import type { Checklist, Documento, LinkArquivo, TipoDocumentoAluno } from '@/models/documento'
import { http } from './http'

/**
 * Documentos de um aluno. A mesma classe atende a escola (DocumentoController) e o portal
 * (PortalController): só muda o prefixo das rotas — escolhido no construtor.
 */
export class DocumentoService {
  private readonly http: HttpClient
  private readonly prefixoAluno: string
  private readonly prefixoDocumento: string

  constructor(cliente: HttpClient, area: 'escola' | 'portal') {
    this.http = cliente
    this.prefixoAluno = area === 'portal' ? '/portal/alunos' : '/alunos'
    this.prefixoDocumento = area === 'portal' ? '/portal/documentos' : '/documentos'
  }

  checklist(alunoId: string): Promise<Checklist> {
    return this.http.get<Checklist>(`${this.prefixoAluno}/${alunoId}/documentos`)
  }

  enviar(alunoId: string, tipo: TipoDocumentoAluno, arquivo: File): Promise<Documento> {
    const formulario = new FormData()
    formulario.append('tipo', tipo)
    formulario.append('arquivo', arquivo)
    return this.http.enviarArquivo<Documento>(`${this.prefixoAluno}/${alunoId}/documentos`, formulario)
  }

  link(documentoId: string): Promise<LinkArquivo> {
    return this.http.get<LinkArquivo>(`${this.prefixoDocumento}/${documentoId}/link`)
  }

  /** Só a escola apaga documentos. */
  remover(documentoId: string): Promise<void> {
    return this.http.delete<void>(`/documentos/${documentoId}`)
  }
}

export const documentoServiceEscola = new DocumentoService(http, 'escola')
export const documentoServicePortal = new DocumentoService(http, 'portal')
