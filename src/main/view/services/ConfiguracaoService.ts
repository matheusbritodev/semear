import type { HttpClient } from '@/lib/HttpClient'
import type { AnoLetivo, ProfessorDaTurma, Turma } from '@/models/academico'
import type { Conta, FuncionarioSemLogin, Opcao, PrecoSerie, SenhaTemporaria } from '@/models/configuracao'
import type { PerfilUsuario } from '@/models/enums'
import { http } from './http'

/** Cliente de UsuarioController, AnoLetivoController, TabelaPrecoController e do cadastro de turmas. */
export class ConfiguracaoService {
  private readonly http: HttpClient

  constructor(cliente: HttpClient) {
    this.http = cliente
  }

  /* ---- Usuários ---- */

  contas(): Promise<Conta[]> {
    return this.http.get<Conta[]>('/usuarios')
  }

  funcionariosSemLogin(): Promise<FuncionarioSemLogin[]> {
    return this.http.get<FuncionarioSemLogin[]>('/usuarios/funcionarios-sem-login')
  }

  criarAcesso(funcionarioId: string, email: string, perfil: PerfilUsuario): Promise<SenhaTemporaria> {
    return this.http.post<SenhaTemporaria>('/usuarios', { funcionarioId, email, perfil })
  }

  alterarPerfil(id: string, perfil: PerfilUsuario): Promise<Conta> {
    return this.http.patch<Conta>(`/usuarios/${id}/perfil`, { perfil })
  }

  alterarAtivo(id: string, ativo: boolean): Promise<Conta> {
    return this.http.patch<Conta>(`/usuarios/${id}/ativo`, { ativo })
  }

  senhaTemporaria(id: string): Promise<SenhaTemporaria> {
    return this.http.post<SenhaTemporaria>(`/usuarios/${id}/senha-temporaria`)
  }

  /* ---- Ano letivo e preços ---- */

  criarAno(ano: number, dataInicio: string, dataFim: string): Promise<AnoLetivo> {
    return this.http.post<AnoLetivo>('/anos-letivos', { ano, dataInicio, dataFim })
  }

  alterarAno(ano: number, dataInicio: string, dataFim: string): Promise<AnoLetivo> {
    return this.http.put<AnoLetivo>(`/anos-letivos/${ano}`, { ano, dataInicio, dataFim })
  }

  precos(ano: number): Promise<PrecoSerie[]> {
    return this.http.get<PrecoSerie[]>('/tabela-precos', { ano })
  }

  salvarPreco(dados: {
    serieId: string
    ano: number
    valorMatricula: number
    valorMensal: number
    diaVencimento: number
    diasPrazoMatricula: number
  }): Promise<PrecoSerie[]> {
    return this.http.put<PrecoSerie[]>('/tabela-precos', dados)
  }

  copiarPrecos(deAno: number, paraAno: number, reajustePercentual: number): Promise<PrecoSerie[]> {
    return this.http.post<PrecoSerie[]>('/tabela-precos/copiar', { deAno, paraAno, reajustePercentual })
  }

  /* ---- Turmas e professores ---- */

  criarTurma(ano: number, serieId: string, nome: string, capacidade: number): Promise<Turma> {
    return this.http.post<Turma>('/turmas', { ano, serieId, nome, capacidade })
  }

  removerTurma(id: string): Promise<void> {
    return this.http.delete<void>(`/turmas/${id}`)
  }

  opcoesProfessores(): Promise<{ professores: Opcao[]; disciplinas: Opcao[] }> {
    return this.http.get<{ professores: Opcao[]; disciplinas: Opcao[] }>('/turmas/opcoes-professores')
  }

  vincularProfessor(turmaId: string, funcionarioId: string, disciplinaId: string | null): Promise<ProfessorDaTurma[]> {
    return this.http.post<ProfessorDaTurma[]>(`/turmas/${turmaId}/professores`, { funcionarioId, disciplinaId })
  }

  removerProfessor(turmaId: string, vinculoId: string): Promise<ProfessorDaTurma[]> {
    return this.http.delete<ProfessorDaTurma[]>(`/turmas/${turmaId}/professores/${vinculoId}`)
  }
}

export const configuracaoService = new ConfiguracaoService(http)
