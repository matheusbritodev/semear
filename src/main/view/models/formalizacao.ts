import type { NivelEnsino, StatusContrato, TipoContrato } from './enums'

// Espelham dto/matricula/ContratoDtos.java, dto/academico/MaterialDtos.java e dto/pedagogico/BoletimDtos.java

export interface Clausula {
  titulo: string
  texto: string
}

export interface Assinante {
  id: string
  nome: string
  parentesco: string | null
  financeiro: boolean
}

export interface ContratoTermo {
  id: string
  tipo: TipoContrato
  titulo: string
  status: StatusContrato
  matriculaId: string
  alunoId: string
  aluno: string
  ano: number
  serie: string
  turma: string
  assinadoPor: string | null
  assinadoEm: string | null
  /** Pendente e com matrícula vigente (o servidor confere de novo). */
  podeAssinar: boolean
  clausulas: Clausula[]
  responsaveis: Assinante[]
}

export interface ContratoResumo {
  id: string
  tipo: TipoContrato
  titulo: string
  status: StatusContrato
  ano: number
  serie: string
  assinadoPor: string | null
  assinadoEm: string | null
  podeAssinar: boolean
}

export interface Serie {
  id: string
  nome: string
  nivel: NivelEnsino
  nivelRotulo: string
  ordem: number
}

export interface ItemMaterial {
  id: string
  descricao: string
  quantidade: number
}

export interface ListaMaterial {
  serieId: string
  /** Preenchida no portal. */
  serie: string | null
  ano: number
  itens: ItemMaterial[]
}

export interface DisciplinaBoletim {
  disciplina: string
  /** Sempre 4 posições (1º ao 4º bimestre); null = não lançada. */
  notas: (number | null)[]
  mediaParcial: number | null
}

export interface Boletim {
  alunoId: string
  matriculaId: string
  ano: number
  serie: string
  turma: string
  nivel: NivelEnsino
  anosDisponiveis: number[]
  bimestresLancados: number
  disciplinas: DisciplinaBoletim[]
  mediaGeral: number | null
  frequencia: { aulas: number; presencas: number; faltas: number; percentual: number | null }
}
