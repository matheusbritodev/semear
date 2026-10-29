import type { NivelEnsino, StatusMatricula, TipoMatricula } from './enums'

// Espelham dto/academico/AcademicoDtos.java

export interface AnoLetivo {
  id: string
  ano: number
  dataInicio: string
  dataFim: string
  emAndamento: boolean
}

export interface Turma {
  id: string
  ano: number
  serie: string
  nivel: NivelEnsino
  nivelRotulo: string
  ordem: number
  nome: string
  nomeCompleto: string
  capacidadeMax: number
  ocupadas: number
  vagas: number
  percentualOcupacao: number
  lotada: boolean
}

export interface AlunoDaTurma {
  matriculaId: string
  alunoId: string
  numeroCadastro: number
  nome: string
  idade: number
  tipo: TipoMatricula
  status: StatusMatricula
}

export interface ProfessorDaTurma {
  funcionarioId: string
  nome: string
  /** Nula = regente (polivalente). */
  disciplina: string | null
  vinculoId: string
  disciplinaId: string | null
}

export interface TurmaDetalhe {
  turma: Turma
  alunos: AlunoDaTurma[]
  professores: ProfessorDaTurma[]
}

export interface Alerta {
  id: string
  tipo: string
  mensagem: string
  turmaId: string | null
  lido: boolean
  criadoEm: string
}
