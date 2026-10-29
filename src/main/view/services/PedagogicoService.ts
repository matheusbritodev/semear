import type { HttpClient } from '@/lib/HttpClient'
import type {
  Chamada,
  DadosPlano,
  DiarioResumo,
  EventoCalendario,
  Formacao,
  FrequenciaAluno,
  NotasTurma,
  Plano,
  TipoCalendario,
  TurmaPedagogica,
} from '@/models/pedagogico'
import { http } from './http'

type Parametros = Record<string, string | number | undefined>
const limpar = (p: Parametros) => Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== ''))

export interface Presenca {
  matriculaId: string
  presente: boolean
  justificativa: string | null
}

/** Cliente de controller/pedagogico/PedagogicoController.java. */
export class PedagogicoService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  turmas(ano?: number): Promise<TurmaPedagogica[]> {
    return this.http.get<TurmaPedagogica[]>('/pedagogico/turmas', limpar({ ano }))
  }

  chamada(turmaId: string, data?: string): Promise<Chamada> {
    return this.http.get<Chamada>(`/turmas/${turmaId}/chamada`, limpar({ data }))
  }

  registrarDiario(turmaId: string, data: string, conteudo: string, presencas: Presenca[]): Promise<Chamada> {
    return this.http.put<Chamada>(`/turmas/${turmaId}/diarios/${data}`, { conteudo, presencas })
  }

  diarios(turmaId: string, mes?: string): Promise<DiarioResumo[]> {
    return this.http.get<DiarioResumo[]>(`/turmas/${turmaId}/diarios`, limpar({ mes }))
  }

  frequencia(turmaId: string): Promise<FrequenciaAluno[]> {
    return this.http.get<FrequenciaAluno[]>(`/turmas/${turmaId}/frequencia`)
  }

  planos(turmaId: string, de?: string, ate?: string): Promise<Plano[]> {
    return this.http.get<Plano[]>(`/turmas/${turmaId}/planos`, limpar({ de, ate }))
  }

  criarPlano(turmaId: string, dados: DadosPlano): Promise<{ id: string }> {
    return this.http.post<{ id: string }>(`/turmas/${turmaId}/planos`, dados)
  }

  alterarPlano(id: string, dados: DadosPlano): Promise<void> {
    return this.http.put<void>(`/planos/${id}`, dados)
  }

  removerPlano(id: string): Promise<void> {
    return this.http.delete<void>(`/planos/${id}`)
  }

  notas(turmaId: string, disciplinaId?: string, bimestre?: number): Promise<NotasTurma> {
    return this.http.get<NotasTurma>(`/turmas/${turmaId}/notas`, limpar({ disciplinaId, bimestre }))
  }

  lancarNotas(
    turmaId: string,
    disciplinaId: string,
    bimestre: number,
    notas: { matriculaId: string; valor: number | null }[],
  ): Promise<NotasTurma> {
    return this.http.put<NotasTurma>(`/turmas/${turmaId}/notas`, { disciplinaId, bimestre, notas })
  }

  calendario(ano?: number): Promise<EventoCalendario[]> {
    return this.http.get<EventoCalendario[]>('/calendario', limpar({ ano }))
  }

  criarEvento(ano: number, data: string, tipo: TipoCalendario, descricao: string): Promise<EventoCalendario[]> {
    return this.http.post<EventoCalendario[]>('/calendario', { ano, data, tipo, descricao })
  }

  removerEvento(id: string): Promise<EventoCalendario[]> {
    return this.http.delete<EventoCalendario[]>(`/calendario/${id}`)
  }

  formacoes(funcionarioId: string): Promise<Formacao[]> {
    return this.http.get<Formacao[]>(`/funcionarios/${funcionarioId}/formacoes`)
  }

  registrarFormacao(funcionarioId: string, curso: string, cargaHoraria: number, concluidaEm: string | null): Promise<Formacao[]> {
    return this.http.post<Formacao[]>(`/funcionarios/${funcionarioId}/formacoes`, { curso, cargaHoraria, concluidaEm })
  }

  removerFormacao(funcionarioId: string, id: string): Promise<Formacao[]> {
    return this.http.delete<Formacao[]>(`/funcionarios/${funcionarioId}/formacoes/${id}`)
  }
}

export const pedagogicoService = new PedagogicoService(http)
