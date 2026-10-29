import type { Modulo, PerfilUsuario, StatusMatricula } from './enums'

// Espelham dto/pessoa/PendenciaDtos.java e PermissaoDtos.java

export interface PendenciaDocumento {
  alunoId: string
  numeroCadastro: number
  aluno: string
  ano: number
  turmaId: string
  turma: string
  statusMatricula: StatusMatricula
  pendentes: number
  faltando: string[]
  responsavel: string | null
  telefone: string | null
}

export interface ResumoPendencias {
  alunosComPendencia: number
  documentosFaltando: number
  alunosConsiderados: number
}

export interface CelulaPermissao {
  modulo: Modulo
  ler: boolean
  gravar: boolean
}

export interface PerfilPermissoes {
  perfil: PerfilUsuario
  permissoes: CelulaPermissao[]
}

export interface MatrizPermissoes {
  modulos: Modulo[]
  perfis: PerfilPermissoes[]
}
