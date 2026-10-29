import type { Turma } from './academico'
import type {
  NivelEnsino,
  OrigemPagamento,
  StatusCobranca,
  StatusContrato,
  StatusMatricula,
  StatusPreMatricula,
  TipoCobranca,
  TipoContrato,
  TipoMatricula,
} from './enums'

// Espelham dto/matricula/MatriculaDtos.java e dto/portal/PortalDtos.java

export interface PreMatriculaResumo {
  id: string
  criadoEm: string
  ano: number
  serie: string
  nivel: NivelEnsino
  alunoNome: string
  alunoNascimento: string
  idade: number
  responsavel: string
  telefone: string | null
  status: StatusPreMatricula
  motivoRejeicao: string | null
  matriculaId: string | null
}

export interface PreMatriculaDetalhe {
  preMatricula: PreMatriculaResumo
  serieId: string
  parentesco: string | null
  alunoCpf: string | null
  fardaUniforme: string | null
  fardaEdFisica: string | null
  responsavelId: string
  responsavelEmail: string | null
  turmasDisponiveis: Turma[]
}

export interface DecisaoPreMatricula {
  preMatriculaId: string
  status: StatusPreMatricula
  matriculaId: string | null
}

export interface MatriculaResumo {
  id: string
  alunoId: string
  numeroCadastro: number
  aluno: string
  ano: number
  turmaId: string
  turma: string
  tipo: TipoMatricula
  status: StatusMatricula
  dataMatricula: string
}

export interface Contrato {
  id: string
  tipo: TipoContrato
  status: StatusContrato
  assinadoPor: string | null
  assinadoEm: string | null
}

export interface CobrancaResumo {
  id: string
  tipo: TipoCobranca
  competencia: string
  valorFinal: number
  vencimento: string
  status: StatusCobranca
  pagoEm: string | null
  valorPago: number | null
  origemPagamento: OrigemPagamento | null
}

export interface MatriculaDetalhe {
  matricula: MatriculaResumo
  responsavelFinanceiro: string | null
  motivoCancelamento: string | null
  contratos: Contrato[]
  fardas: { tipo: string; tamanho: string }[]
  cobrancaMatricula: CobrancaResumo | null
  possuiDebitosVencidos: boolean
  documentacaoCompleta: boolean
  documentosPendentes: number
}

export interface RematriculaOpcao {
  alunoId: string
  aluno: string | null
  turmaAtual: string | null
  anoDestino: number
  situacao: string
  mensagem: string
  permitida: boolean
  turmaDestinoId: string | null
  turmaDestino: string | null
  vagasDestino: number | null
  matriculaDestinoId: string | null
  statusDestino: StatusMatricula | null
}

export interface Serie {
  id: string
  nome: string
  nivel: NivelEnsino
  nivelRotulo: string
  ordem: number
}

export interface OpcoesMatricula {
  anos: number[]
  series: Serie[]
}

export interface NovaPreMatricula {
  ano: number
  serieId: string
  alunoNome: string
  alunoNascimento: string
  alunoCpf: string
  parentesco: string
  fardaUniforme: string
  fardaEdFisica: string
}

export interface FilhoNoPortal {
  alunoId: string
  nome: string
  parentesco: string | null
  financeiro: boolean
  turmaAtual: string | null
  statusMatricula: StatusMatricula | null
  /** Documentos obrigatórios (RN12) ainda não entregues. */
  documentosPendentes: number
  /** Contrato ou Ficha de Sócio aguardando assinatura. */
  contratosPendentes: number
}
