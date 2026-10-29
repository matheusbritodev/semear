import type { NivelEnsino } from './enums'

// Espelham dto/pedagogico/PedagogicoDtos.java

export type TipoPlano = 'PLANO_AULA' | 'PLANEJAMENTO_DIARIO'
export type TipoCalendario = 'AULA' | 'FERIADO' | 'RECESSO' | 'EVENTO'

export const ROTULO_TIPO_PLANO: Record<TipoPlano, string> = {
  PLANO_AULA: 'Plano de aula',
  PLANEJAMENTO_DIARIO: 'Planejamento diário',
}

export const ROTULO_TIPO_CALENDARIO: Record<TipoCalendario, string> = {
  AULA: 'Dia letivo especial',
  FERIADO: 'Feriado',
  RECESSO: 'Recesso',
  EVENTO: 'Evento',
}

export interface TurmaPedagogica {
  turmaId: string
  ano: number
  nomeCompleto: string
  nivel: NivelEnsino
  alunosAtivos: number
  /** O que o professor logado leciona ali; vazio para quem só consulta. */
  disciplinas: string[]
  ultimoDiario: string | null
  bimestreAtual: number
}

export interface ItemChamada {
  matriculaId: string
  alunoId: string
  numeroCadastro: number
  nome: string
  presente: boolean
  justificativa: string | null
}

export interface Chamada {
  turmaId: string
  turma: string
  data: string
  /** Preenchido quando não há aula no dia (feriado, fim de semana, futuro...). */
  motivoSemAula: string | null
  diarioId: string | null
  professor: string | null
  conteudo: string | null
  podeRegistrar: boolean
  alunos: ItemChamada[]
}

export interface DiarioResumo {
  id: string
  data: string
  professor: string
  conteudo: string
  presentes: number
  faltas: number
  meu: boolean
}

export interface FrequenciaAluno {
  matriculaId: string
  alunoId: string
  nome: string
  aulas: number
  presencas: number
  faltas: number
  percentual: number | null
  /** Abaixo dos 75% exigidos pela LDB. */
  abaixoDoMinimo: boolean
}

export interface Plano {
  id: string
  tipo: TipoPlano
  dataPrevista: string
  objetivo: string
  conteudo: string
  professor: string
  meu: boolean
}

export interface DadosPlano {
  tipo: TipoPlano
  dataPrevista: string
  objetivo: string
  conteudo: string
}

export interface NotasTurma {
  turmaId: string
  turma: string
  nivel: NivelEnsino
  disciplinaId: string
  disciplina: string
  bimestre: number
  podeLancar: boolean
  disciplinas: { id: string; nome: string; podeLancar: boolean }[]
  alunos: { matriculaId: string; alunoId: string; numeroCadastro: number; nome: string; valor: number | null }[]
}

export interface EventoCalendario {
  id: string
  data: string
  tipo: TipoCalendario
  tipoRotulo: string
  descricao: string
  temAula: boolean
}

export interface Formacao {
  id: string
  curso: string
  cargaHoraria: number
  concluidaEm: string | null
  concluida: boolean
}
