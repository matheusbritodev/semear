import type { Modulo, PerfilUsuario } from './enums'

/** Espelha dto/controle/ControleDtos.java (controles internos: SoD e SOX). */

export type TipoSolicitacao = 'CONCESSAO_BOLSA' | 'ALTERACAO_BOLSA' | 'ALTERACAO_PRECO' | 'COPIA_PRECOS'
export type StatusSolicitacao = 'PENDENTE' | 'APROVADA' | 'REJEITADA' | 'CANCELADA'

export const ROTULO_STATUS_SOLICITACAO: Record<StatusSolicitacao, string> = {
  PENDENTE: 'Aguardando',
  APROVADA: 'Aprovado',
  REJEITADA: 'Recusado',
  CANCELADA: 'Cancelado',
}

export interface Solicitacao {
  id: string
  tipo: TipoSolicitacao
  tipoRotulo: string
  status: StatusSolicitacao
  resumo: string
  matriculaId: string | null
  solicitante: string
  solicitadaEm: string
  decisor: string | null
  decididaEm: string | null
  motivo: string | null
  /** Fui eu que pedi (posso cancelar enquanto estiver pendente). */
  minha: boolean
  /** Tenho o módulo Aprovações, não fui eu que pedi e ainda está pendente. */
  podeDecidir: boolean
}

export interface RegraSod {
  codigo: string
  moduloA: Modulo
  moduloB: Modulo
  risco: string
}

export interface ConflitoSod {
  perfil: PerfilUsuario
  codigo: string
  risco: string
  excecaoDocumentada: boolean
}

export interface PoliticaSod {
  regras: RegraSod[]
  conflitos: ConflitoSod[]
  perfilComExcecao: PerfilUsuario
  controlesCompensatorios: string
}

export interface AlteracaoCampo {
  campo: string
  antes: string | null
  depois: string | null
}

export interface RegistroAuditoria {
  id: number
  ocorridoEm: string
  autor: string
  papelBanco: string
  tabela: string
  tabelaRotulo: string
  operacao: 'INSERT' | 'UPDATE' | 'DELETE'
  registroId: string | null
  alteracoes: AlteracaoCampo[]
}

export const ROTULO_OPERACAO: Record<RegistroAuditoria['operacao'], string> = {
  INSERT: 'Inclusão',
  UPDATE: 'Alteração',
  DELETE: 'Exclusão',
}

export interface FiltroAuditoria {
  tabela?: string
  usuarioId?: string
  registroId?: string
  de?: string
  ate?: string
  pagina: number
}

export interface Verificacao {
  integra: boolean
  verificados: number
  ultimoId: number
  ultimoHash: string
  ancorasConferidas: number
  registroComProblema: number | null
  tipoProblema: string | null
  mensagem: string
  verificadaEm: string
}

export type TipoAlertaAcesso =
  | 'FUNCIONARIO_DESLIGADO'
  | 'SEM_VINCULO'
  | 'CONFLITO_SOD'
  | 'NUNCA_ACESSOU'
  | 'SEM_ACESSO_RECENTE'
  | 'PERFIL_PRIVILEGIADO'

export interface ContaRevisada {
  usuarioId: string
  email: string
  nome: string | null
  perfil: PerfilUsuario
  ativo: boolean
  ultimoAcesso: string | null
  alertas: { tipo: TipoAlertaAcesso; detalhe: string }[]
}

export interface RevisaoRealizada {
  id: string
  realizadaEm: string
  realizadaPor: string
  contasRevisadas: number
  alertas: number
  observacao: string | null
}

export interface RelatorioRevisao {
  contas: ContaRevisada[]
  totalAlertas: number
  contasComAlerta: number
  contasPortalAtivas: number
  conflitosPerfis: ConflitoSod[]
  ultimaRevisao: string | null
  proximaRevisao: string
  atrasada: boolean
  historico: RevisaoRealizada[]
}

/** "2026-10-31T17:23:38Z" -> "31/10/2026 14:23" no horário de São Luís. */
export function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', {
    timeZone: 'America/Fortaleza',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
