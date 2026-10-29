import type { NivelEnsino, PerfilUsuario, TipoFuncionario } from './enums'

// Espelham dto/pessoa/UsuarioDtos.java e os DTOs de configuração em AcademicoDtos.java

export type VinculoConta = 'FUNCIONARIO' | 'RESPONSAVEL' | 'ALUNO' | 'NENHUM'

export interface Conta {
  id: string
  email: string
  perfil: PerfilUsuario
  ativo: boolean
  ultimoLogin: string | null
  criadoEm: string
  vinculo: VinculoConta
  nome: string | null
  tipoFuncionario: TipoFuncionario | null
  /** É o login de quem está vendo (não pode alterar a si mesmo). */
  eu: boolean
}

export interface SenhaTemporaria {
  conta: Conta
  senhaTemporaria: string
}

export interface FuncionarioSemLogin {
  id: string
  nome: string
  tipo: TipoFuncionario
  email: string | null
}

export interface PrecoSerie {
  serieId: string
  serie: string
  nivel: NivelEnsino
  ordem: number
  valorMatricula: number | null
  valorMensal: number | null
  diaVencimento: number | null
  diasPrazoMatricula: number | null
}

export interface Opcao {
  id: string
  nome: string
}

/** Perfil sugerido para o login de cada tipo de funcionário. */
export const PERFIL_SUGERIDO: Record<TipoFuncionario, PerfilUsuario> = {
  PROFESSOR: 'PROFESSOR',
  ADMINISTRATIVO: 'SECRETARIA',
  GESTOR: 'ADMIN',
  PRESTADOR_SERVICO: 'SECRETARIA',
}
