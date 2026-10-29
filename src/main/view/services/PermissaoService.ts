import type { HttpClient } from '@/lib/HttpClient'
import type { Modulo, PerfilUsuario } from '@/models/enums'
import type { MatrizPermissoes } from '@/models/pendencia'
import { http } from './http'

/** Cliente de controller/pessoa/PermissaoController.java (perfis de acesso). */
export class PermissaoService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  matriz(): Promise<MatrizPermissoes> {
    return this.http.get<MatrizPermissoes>('/permissoes')
  }

  alterar(perfil: PerfilUsuario, modulo: Modulo, ler: boolean, gravar: boolean): Promise<MatrizPermissoes> {
    return this.http.put<MatrizPermissoes>(`/permissoes/${perfil}/${modulo}`, { ler, gravar })
  }

  restaurarPadrao(): Promise<MatrizPermissoes> {
    return this.http.post<MatrizPermissoes>('/permissoes/restaurar-padrao')
  }
}

export const permissaoService = new PermissaoService(http)
