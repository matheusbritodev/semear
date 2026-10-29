import type { HttpClient } from '@/lib/HttpClient'
import type { Pagina } from '@/models/comum'
import type { ResponsavelDetalhe, ResponsavelResumo } from '@/models/pessoa'
import { ApiService } from './ApiService'
import { http } from './http'

/** Cliente de controller/pessoa/ResponsavelController.java. */
export class ResponsavelService extends ApiService<ResponsavelDetalhe> {
  constructor(cliente: HttpClient) {
    super(cliente, 'responsaveis')
  }

  pesquisar(busca: string, pagina: number, tamanho = 20): Promise<Pagina<ResponsavelResumo>> {
    return this.http.get<Pagina<ResponsavelResumo>>('/responsaveis', this.limpar({ busca, pagina, tamanho }))
  }

  /** Envia só dígitos no telefone; a máscara é responsabilidade da tela. */
  atualizarContato(id: string, telefone: string, email: string): Promise<ResponsavelDetalhe> {
    return this.http.put<ResponsavelDetalhe>(`/responsaveis/${id}/contato`, {
      telefone: telefone.replace(/\D/g, ''),
      email: email.trim(),
    })
  }
}

export const responsavelService = new ResponsavelService(http)
