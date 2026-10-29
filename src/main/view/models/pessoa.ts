import type { StatusMatricula, TipoFuncionario, TipoMatricula } from './enums'

// Espelham os records de dto/pessoa/*.java

export interface AlunoResumo {
  id: string
  numeroCadastro: number
  nome: string
  dataNascimento: string
  idade: number
  ativo: boolean
  matriculaId: string
  turmaId: string
  turma: string
  statusMatricula: StatusMatricula
  tipoMatricula: TipoMatricula
  responsavelFinanceiro: string | null
  /** Documentos obrigatórios (RN12) ainda não entregues. */
  documentosPendentes: number
}

export interface Vinculo {
  responsavelId: string
  nome: string
  parentesco: string | null
  financeiro: boolean
  telefone: string | null
  email: string | null
}

export interface MatriculaHistorico {
  id: string
  ano: number
  turmaId: string
  turma: string
  tipo: TipoMatricula
  status: StatusMatricula
  dataMatricula: string
}

export interface AlunoDetalhe {
  id: string
  numeroCadastro: number
  nome: string
  dataNascimento: string
  idade: number
  ativo: boolean
  responsaveis: Vinculo[]
  matriculas: MatriculaHistorico[]
}

export interface ResponsavelResumo {
  id: string
  nome: string
  cpf: string
  telefone: string | null
  email: string | null
  quantidadeAlunos: number
  possuiLogin: boolean
}

export interface AlunoDoResponsavel {
  alunoId: string
  nome: string
  parentesco: string | null
  financeiro: boolean
  turmaAtual: string | null
  statusMatricula: StatusMatricula | null
  documentosPendentes: number
}

export interface ResponsavelDetalhe {
  id: string
  nome: string
  cpf: string
  telefone: string | null
  email: string | null
  possuiLogin: boolean
  alunos: AlunoDoResponsavel[]
}

export interface Funcionario {
  id: string
  nome: string
  cpf: string
  tipo: TipoFuncionario
  funcao: string
  podeLecionar: boolean
  telefone: string | null
  email: string | null
  dataAdmissao: string | null
  ativo: boolean
  possuiLogin: boolean
}

export interface NovoFuncionario {
  nome: string
  cpf: string
  tipo: TipoFuncionario
  telefone: string
  email: string
  dataAdmissao: string | null
}

export type AtualizarFuncionario = Omit<NovoFuncionario, 'cpf' | 'tipo'>
