import type { OrigemPagamento, StatusCobranca, TipoCobranca } from './enums'

// Espelham dto/financeiro/FinanceiroDtos.java

export type TipoDesconto = 'PERCENTUAL' | 'VALOR_FIXO'

export const ROTULO_TIPO_COBRANCA: Record<TipoCobranca, string> = {
  MATRICULA: 'Matrícula',
  TAXA_ASSOCIADO: 'Taxa de Associado',
  MENSALIDADE: 'Mensalidade',
}

export const ROTULO_ORIGEM: Record<OrigemPagamento, string> = {
  SITE: 'Site (Cora)',
  PRESENCIAL: 'Presencial',
}

export const ROTULO_TIPO_DESCONTO: Record<TipoDesconto, string> = {
  PERCENTUAL: 'Percentual',
  VALOR_FIXO: 'Valor fixo',
}

export interface Cobranca {
  id: string
  tipo: TipoCobranca
  competencia: string
  matriculaId: string
  alunoId: string
  numeroCadastro: number
  aluno: string
  ano: number
  turma: string
  responsavel: string
  telefone: string | null
  valorBruto: number
  descontoBolsa: number
  valorFinal: number
  vencimento: string
  /** Status real hoje: pendente que passou do vencimento já vem como VENCIDO. */
  status: StatusCobranca
  diasDeAtraso: number
  /** Valor para pagar hoje, com desconto de pontualidade (RN08) ou multa e juros (RN09). */
  valorHoje: number
  pagoEm: string | null
  valorPago: number | null
  origemPagamento: OrigemPagamento | null
  temBoleto: boolean
  linkPagamento: string | null
}

export interface ComposicaoValor {
  data: string
  valorFinal: number
  descontoPontualidade: number
  multa: number
  juros: number
  diasDeAtraso: number
  total: number
}

export interface CobrancaDetalhe {
  cobranca: Cobranca
  composicao: ComposicaoValor
}

export interface Geracao {
  criadas: number
  boletosEmitidos: number
}

export interface Inadimplente {
  alunoId: string
  numeroCadastro: number
  aluno: string
  turma: string
  responsavel: string
  telefone: string | null
  cobrancas: number
  maiorAtraso: number
  valorOriginal: number
  valorAtualizado: number
  itens: Cobranca[]
}

export interface Inadimplencia {
  alunos: number
  cobrancas: number
  valorOriginal: number
  valorAtualizado: number
  inadimplentes: Inadimplente[]
}

export interface MesResumo {
  competencia: string | null
  quantidade: number
  previsto: number
  descontosBolsa: number
  recebido: number
  emAtraso: number
}

export interface ResumoAno {
  ano: number
  meses: MesResumo[]
  total: MesResumo
}

export interface Bolsa {
  id: string
  nome: string
  descricao: string | null
  tipoDesconto: TipoDesconto
  valor: number
  ativa: boolean
  bolsistasVigentes: number
}

export interface DadosBolsa {
  nome: string
  descricao: string
  tipoDesconto: TipoDesconto
  valor: number
  ativa: boolean
}

export interface Concessao {
  id: string
  matriculaId: string
  bolsaId: string
  bolsa: string
  tipoDesconto: TipoDesconto
  valor: number
  bolsaAtiva: boolean
  alunoId: string
  aluno: string
  ano: number
  turma: string
  vigenciaInicio: string
  vigenciaFim: string | null
  vigente: boolean
}

export interface PortalCobrancas {
  pagamentoSimulado: boolean
  cobrancas: Cobranca[]
}

export function descreverDesconto(tipo: TipoDesconto, valor: number): string {
  return tipo === 'PERCENTUAL'
    ? `${Number(valor).toLocaleString('pt-BR')}%`
    : Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

/** "2026-11-01" -> "nov/2026" */
export function formatarCompetencia(iso: string): string {
  const [ano, mes] = iso.split('-')
  return `${MESES[Number(mes) - 1]}/${ano}`
}
