import type { HttpClient } from '@/lib/HttpClient'
import type { PendenciaDocumento, ResumoPendencias } from '@/models/pendencia'
import { http } from './http'

/** Cliente de controller/pessoa/PendenciaController.java. */
export class PendenciaService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  documentos(): Promise<PendenciaDocumento[]> {
    return this.http.get<PendenciaDocumento[]>('/pendencias/documentos')
  }

  resumo(): Promise<ResumoPendencias> {
    return this.http.get<ResumoPendencias>('/pendencias/resumo')
  }
}

export const pendenciaService = new PendenciaService(http)
