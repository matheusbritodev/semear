import type { HttpClient } from '@/lib/HttpClient'
import type { Sessao } from '@/models/sessao'
import { http } from './http'

/** Cliente de controller/pessoa/MeController.java. */
export class MeService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  sessao(): Promise<Sessao> {
    return this.http.get<Sessao>('/me')
  }
}

export const meService = new MeService(http)
