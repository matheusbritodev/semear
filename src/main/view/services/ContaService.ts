import type { HttpClient } from '@/lib/HttpClient'
import { http } from './http'

/** Cliente de controller/publico/ContaController.java (rota sem login). */
export class ContaService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  /** A aplicação Java cria a conta já confirmada (decisão D10: sem depender de servidor de e-mail). */
  criar(email: string, senha: string): Promise<{ usuarioId: string }> {
    return this.http.post<{ usuarioId: string }>('/publico/contas', { email, senha })
  }
}

export const contaService = new ContaService(http)
