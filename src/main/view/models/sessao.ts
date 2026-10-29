import type { Modulo, PerfilUsuario } from './enums'

/** Espelha dto/pessoa/MeResponse.java (GET /api/me). */
export interface PermissaoSessao {
  modulo: Modulo
  ler: boolean
  gravar: boolean
}

export interface Sessao {
  id: string
  email: string | null
  nome: string | null
  perfil: PerfilUsuario | null
  cadastroCompleto: boolean
  permissoes: PermissaoSessao[]
}
