// Espelham dto/institucional/ObrigacaoDtos.java (RF09)

export type OrgaoExterno = 'CENSO_INEP' | 'SMTT' | 'SISTEMA_PRESENCA' | 'CORA'
export type SituacaoObrigacao = 'EM_DIA' | 'EM_ALERTA' | 'ATRASADA' | 'CUMPRIDA'

export const ROTULO_ORGAO: Record<OrgaoExterno, string> = {
  CENSO_INEP: 'Censo Escolar (INEP)',
  SMTT: 'SMTT',
  SISTEMA_PRESENCA: 'Sistema Presença (MEC)',
  CORA: 'Banco Cora',
}

export const ROTULO_SITUACAO_OBRIGACAO: Record<SituacaoObrigacao, string> = {
  EM_DIA: 'Em dia',
  EM_ALERTA: 'Vence em breve',
  ATRASADA: 'Atrasada',
  CUMPRIDA: 'Cumprida',
}

export interface Comprovante {
  id: string
  extensao: string
  enviadoEm: string
  /** Enviado depois do último cumprimento (conta para cumprir agora). */
  doCicloAtual: boolean
}

export interface Obrigacao {
  id: string
  orgao: OrgaoExterno
  orgaoRotulo: string
  descricao: string
  periodicidadeMeses: number | null
  proximoVencimento: string
  /** Negativo quando atrasada. */
  diasRestantes: number
  diasAlerta: number
  situacao: SituacaoObrigacao
  ultimoCumprimento: string | null
  /** Funcionário que marcou o último cumprimento. */
  ultimoCumprimentoPor: string | null
  responsavelId: string | null
  responsavel: string | null
  comprovantes: Comprovante[]
  podeCumprir: boolean
}

export interface DadosObrigacao {
  orgao: OrgaoExterno
  descricao: string
  periodicidadeMeses: number | null
  proximoVencimento: string
  diasAlerta: number
  responsavelId: string | null
}
