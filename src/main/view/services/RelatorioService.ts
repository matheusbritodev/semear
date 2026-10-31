import type { HttpClient } from '@/lib/HttpClient'
import type { RelatorioAlunos } from '@/models/relatorio'
import { http } from './http'

/** Cliente de controller/relatorio/RelatorioController.java. */
export class RelatorioService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  alunos(ano: number, turmaId?: string): Promise<RelatorioAlunos> {
    return this.http.get<RelatorioAlunos>('/relatorios/alunos', { ano, turmaId })
  }
}

export const relatorioService = new RelatorioService(http)
