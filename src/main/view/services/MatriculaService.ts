import type { HttpClient } from '@/lib/HttpClient'
import type { Pagina } from '@/models/comum'
import type { OrigemPagamento, StatusMatricula, StatusPreMatricula } from '@/models/enums'
import type {
  DecisaoPreMatricula,
  MatriculaDetalhe,
  MatriculaResumo,
  PreMatriculaDetalhe,
  PreMatriculaResumo,
  RematriculaOpcao,
} from '@/models/matricula'
import { ApiService } from './ApiService'
import { http } from './http'

export type FiltroMatriculas = { busca?: string; ano?: number; status?: StatusMatricula; pagina?: number; tamanho?: number }

/** Cliente de MatriculaController e PreMatriculaController (área da escola). */
export class MatriculaService extends ApiService<MatriculaDetalhe> {
  constructor(cliente: HttpClient) {
    super(cliente, 'matriculas')
  }

  pesquisar(filtro: FiltroMatriculas): Promise<Pagina<MatriculaResumo>> {
    return this.http.get<Pagina<MatriculaResumo>>('/matriculas', this.limpar({ ...filtro }))
  }

  cancelar(id: string, motivo: string): Promise<MatriculaDetalhe> {
    return this.http.post<MatriculaDetalhe>(`/matriculas/${id}/cancelar`, { motivo })
  }

  registrarPagamentoMatricula(id: string, origem: OrigemPagamento): Promise<MatriculaDetalhe> {
    return this.http.post<MatriculaDetalhe>(`/matriculas/${id}/pagamento-matricula`, { origem })
  }

  opcaoRematricula(alunoId: string): Promise<RematriculaOpcao> {
    return this.http.get<RematriculaOpcao>('/matriculas/rematricula/opcoes', { alunoId })
  }

  rematricular(alunoId: string, fardaUniforme: string, fardaEdFisica: string): Promise<MatriculaDetalhe> {
    return this.http.post<MatriculaDetalhe>('/matriculas/rematricula', { alunoId, fardaUniforme, fardaEdFisica })
  }

  /* ---- Pré-matrículas ---- */

  preMatriculas(status?: StatusPreMatricula, ano?: number): Promise<PreMatriculaResumo[]> {
    return this.http.get<PreMatriculaResumo[]>('/pre-matriculas', this.limpar({ status, ano }))
  }

  preMatricula(id: string): Promise<PreMatriculaDetalhe> {
    return this.http.get<PreMatriculaDetalhe>(`/pre-matriculas/${id}`)
  }

  aprovar(id: string, turmaId: string): Promise<DecisaoPreMatricula> {
    return this.http.post<DecisaoPreMatricula>(`/pre-matriculas/${id}/aprovar`, { turmaId })
  }

  rejeitar(id: string, motivo: string): Promise<DecisaoPreMatricula> {
    return this.http.post<DecisaoPreMatricula>(`/pre-matriculas/${id}/rejeitar`, { motivo })
  }
}

export const matriculaService = new MatriculaService(http)
