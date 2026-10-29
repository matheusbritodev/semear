import type { HttpClient } from '@/lib/HttpClient'
import type { Pagina } from '@/models/comum'
import type { AlunoDetalhe, AlunoResumo } from '@/models/pessoa'
import { ApiService } from './ApiService'
import { http } from './http'

export type FiltroAlunos = {
  busca?: string
  ano?: number
  turmaId?: string
  pagina?: number
  tamanho?: number
}

/** Cliente de controller/pessoa/AlunoController.java. */
export class AlunoService extends ApiService<AlunoDetalhe> {
  constructor(cliente: HttpClient) {
    super(cliente, 'alunos')
  }

  pesquisar(filtro: FiltroAlunos): Promise<Pagina<AlunoResumo>> {
    return this.http.get<Pagina<AlunoResumo>>('/alunos', this.limpar({ ...filtro }))
  }

  definirResponsavelFinanceiro(alunoId: string, responsavelId: string): Promise<AlunoDetalhe> {
    return this.http.put<AlunoDetalhe>(`/alunos/${alunoId}/responsavel-financeiro`, { responsavelId })
  }
}

export const alunoService = new AlunoService(http)
