import type { HttpClient } from '@/lib/HttpClient'
import type { Pagina } from '@/models/comum'
import type {
  FiltroAuditoria,
  PoliticaSod,
  RegistroAuditoria,
  RelatorioRevisao,
  Solicitacao,
  StatusSolicitacao,
  Verificacao,
} from '@/models/controle'
import { http } from './http'

/** Cliente de controller/controle/ControleInternoController.java e de /permissoes/segregacao. */
export class ControleService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  /* ---- Aprovações ---- */

  aprovacoes(status?: StatusSolicitacao): Promise<Solicitacao[]> {
    return this.http.get<Solicitacao[]>('/aprovacoes', status ? { status } : undefined)
  }

  resumoAprovacoes(): Promise<{ pendentes: number }> {
    return this.http.get<{ pendentes: number }>('/aprovacoes/resumo')
  }

  aprovar(id: string): Promise<Solicitacao> {
    return this.http.post<Solicitacao>(`/aprovacoes/${id}/aprovar`)
  }

  rejeitar(id: string, motivo: string): Promise<Solicitacao> {
    return this.http.post<Solicitacao>(`/aprovacoes/${id}/rejeitar`, { motivo })
  }

  cancelar(id: string): Promise<Solicitacao> {
    return this.http.post<Solicitacao>(`/aprovacoes/${id}/cancelar`)
  }

  /* ---- SoD ---- */

  politicaSod(): Promise<PoliticaSod> {
    return this.http.get<PoliticaSod>('/permissoes/segregacao')
  }

  /* ---- Auditoria ---- */

  auditoria(f: FiltroAuditoria): Promise<Pagina<RegistroAuditoria>> {
    const parametros = Object.fromEntries(Object.entries({ ...f, tamanho: 30 }).filter(([, v]) => v !== undefined && v !== ''))
    return this.http.get<Pagina<RegistroAuditoria>>('/auditoria', parametros as Record<string, string | number>)
  }

  autoresAuditoria(): Promise<{ usuarioId: string; nome: string }[]> {
    return this.http.get('/auditoria/autores')
  }

  tabelasAuditadas(): Promise<{ tabela: string; rotulo: string }[]> {
    return this.http.get('/auditoria/tabelas')
  }

  verificar(): Promise<Verificacao> {
    return this.http.get<Verificacao>('/auditoria/verificacao')
  }

  /* ---- Revisão de acessos ---- */

  revisaoAcessos(): Promise<RelatorioRevisao> {
    return this.http.get<RelatorioRevisao>('/revisao-acessos')
  }

  registrarRevisao(observacao: string): Promise<RelatorioRevisao> {
    return this.http.post<RelatorioRevisao>('/revisao-acessos', { observacao })
  }
}

export const controleService = new ControleService(http)
