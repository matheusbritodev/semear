import type { StatusMatricula } from './enums'

/** Espelha dto/relatorio/RelatorioDtos.java. */
export interface LinhaRelatorioAlunos {
  numeroCadastro: number
  aluno: string
  dataNascimento: string
  idade: number
  turma: string
  situacao: StatusMatricula
  responsavelFinanceiro: string | null
  documentosPendentes: number
}

export interface RelatorioAlunos {
  ano: number
  turma: string | null
  emitidoEm: string
  emitidoPor: string
  total: number
  vigentes: number
  comDocumentosPendentes: number
  porSituacao: Record<StatusMatricula, number>
  porTurma: { turma: string; alunos: number; vigentes: number }[]
  linhas: LinhaRelatorioAlunos[]
}
