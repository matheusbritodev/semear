import type { HttpClient } from '@/lib/HttpClient'
import type { Alerta, AnoLetivo, Turma, TurmaDetalhe } from '@/models/academico'
import { ApiService } from './ApiService'
import { http } from './http'

/** Cliente de TurmaController, AnoLetivoController e AlertaController (módulo acadêmico). */
export class TurmaService extends ApiService<TurmaDetalhe> {
  constructor(cliente: HttpClient) {
    super(cliente, 'turmas')
  }

  listar(ano?: number): Promise<Turma[]> {
    return this.http.get<Turma[]>('/turmas', this.limpar({ ano }))
  }

  alterarCapacidade(id: string, capacidade: number): Promise<Turma> {
    return this.http.patch<Turma>(`/turmas/${id}/capacidade`, { capacidade })
  }

  anosLetivos(): Promise<AnoLetivo[]> {
    return this.http.get<AnoLetivo[]>('/anos-letivos')
  }

  /** Matrículas ATIVAS do ano viram ENCERRADAS (só depois do último dia letivo). */
  encerrarAno(ano: number): Promise<{ encerradas: number }> {
    return this.http.post<{ encerradas: number }>(`/anos-letivos/${ano}/encerrar`)
  }

  anoCorrente(): Promise<AnoLetivo> {
    return this.http.get<AnoLetivo>('/anos-letivos/corrente')
  }

  alertas(apenasNaoLidos = true): Promise<Alerta[]> {
    return this.http.get<Alerta[]>('/alertas', { apenasNaoLidos })
  }

  marcarAlertaComoLido(id: string): Promise<void> {
    return this.http.patch<void>(`/alertas/${id}/lido`)
  }
}

export const turmaService = new TurmaService(http)
