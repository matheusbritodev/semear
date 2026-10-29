import type { HttpClient } from '@/lib/HttpClient'
import type { CobrancaDetalhe, PortalCobrancas } from '@/models/financeiro'
import type { Boletim, ContratoResumo, ContratoTermo, ListaMaterial } from '@/models/formalizacao'
import type { FilhoNoPortal, NovaPreMatricula, OpcoesMatricula, PreMatriculaResumo, RematriculaOpcao } from '@/models/matricula'
import { http } from './http'

const somenteDigitos = (valor: string) => valor.replace(/\D/g, '')

/** Cliente de controller/portal/PortalController.java (responsável logado). */
export class PortalService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  completarCadastro(nome: string, cpf: string, telefone: string): Promise<void> {
    return this.http.post<void>('/portal/cadastro', { nome, cpf: somenteDigitos(cpf), telefone: somenteDigitos(telefone) })
  }

  contratosDoFilho(alunoId: string): Promise<ContratoResumo[]> {
    return this.http.get<ContratoResumo[]>(`/portal/alunos/${alunoId}/contratos`)
  }

  contrato(id: string): Promise<ContratoTermo> {
    return this.http.get<ContratoTermo>(`/portal/contratos/${id}`)
  }

  /** Aceite eletrônico: quem assina é o responsável do login. */
  assinarContrato(id: string): Promise<ContratoTermo> {
    return this.http.post<ContratoTermo>(`/portal/contratos/${id}/assinar`, { aceite: true })
  }

  materiaisDoFilho(alunoId: string): Promise<ListaMaterial> {
    return this.http.get<ListaMaterial>(`/portal/alunos/${alunoId}/materiais`)
  }

  boletimDoFilho(alunoId: string, ano?: number): Promise<Boletim> {
    return this.http.get<Boletim>(`/portal/alunos/${alunoId}/boletim`, ano ? { ano } : undefined)
  }

  /** Cobranças em que o logado é o pagador. */
  cobrancas(): Promise<PortalCobrancas> {
    return this.http.get<PortalCobrancas>('/portal/cobrancas')
  }

  cobranca(id: string): Promise<CobrancaDetalhe> {
    return this.http.get<CobrancaDetalhe>(`/portal/cobrancas/${id}`)
  }

  /** Só com o Cora simulado (pagamentoSimulado = true). */
  simularPagamento(id: string): Promise<CobrancaDetalhe> {
    return this.http.post<CobrancaDetalhe>(`/portal/cobrancas/${id}/pagamento-simulado`)
  }

  filhos(): Promise<FilhoNoPortal[]> {
    return this.http.get<FilhoNoPortal[]>('/portal/alunos')
  }

  opcoesMatricula(): Promise<OpcoesMatricula> {
    return this.http.get<OpcoesMatricula>('/portal/opcoes-matricula')
  }

  preMatriculas(): Promise<PreMatriculaResumo[]> {
    return this.http.get<PreMatriculaResumo[]>('/portal/pre-matriculas')
  }

  novaPreMatricula(dados: NovaPreMatricula): Promise<PreMatriculaResumo> {
    // Campos vazios vão como null para o Controller apontar o campo certo (em vez de "formato inválido")
    return this.http.post<PreMatriculaResumo>('/portal/pre-matriculas', {
      ...dados,
      serieId: dados.serieId || null,
      alunoNascimento: dados.alunoNascimento || null,
      alunoCpf: somenteDigitos(dados.alunoCpf) || null,
    })
  }

  opcoesRematricula(): Promise<RematriculaOpcao[]> {
    return this.http.get<RematriculaOpcao[]>('/portal/rematricula')
  }

  rematricular(alunoId: string, fardaUniforme: string, fardaEdFisica: string): Promise<{ matriculaId: string }> {
    return this.http.post<{ matriculaId: string }>('/portal/rematricula', { alunoId, fardaUniforme, fardaEdFisica })
  }
}

export const portalService = new PortalService(http)
