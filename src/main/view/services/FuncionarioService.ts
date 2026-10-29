import type { HttpClient } from '@/lib/HttpClient'
import type { TipoFuncionario } from '@/models/enums'
import type { AtualizarFuncionario, Funcionario, NovoFuncionario } from '@/models/pessoa'
import { ApiService } from './ApiService'
import { http } from './http'

const somenteDigitos = (valor: string) => valor.replace(/\D/g, '')

/** Cliente de controller/pessoa/FuncionarioController.java. */
export class FuncionarioService extends ApiService<Funcionario> {
  constructor(cliente: HttpClient) {
    super(cliente, 'funcionarios')
  }

  pesquisar(busca?: string, tipo?: TipoFuncionario): Promise<Funcionario[]> {
    return this.http.get<Funcionario[]>('/funcionarios', this.limpar({ busca, tipo }))
  }

  criar(dados: NovoFuncionario): Promise<Funcionario> {
    return this.http.post<Funcionario>('/funcionarios', {
      ...dados,
      cpf: somenteDigitos(dados.cpf),
      telefone: somenteDigitos(dados.telefone),
      dataAdmissao: dados.dataAdmissao || null,
    })
  }

  atualizar(id: string, dados: AtualizarFuncionario): Promise<Funcionario> {
    return this.http.put<Funcionario>(`/funcionarios/${id}`, {
      ...dados,
      telefone: somenteDigitos(dados.telefone),
      dataAdmissao: dados.dataAdmissao || null,
    })
  }

  alterarAtivo(id: string, ativo: boolean): Promise<Funcionario> {
    return this.http.patch<Funcionario>(`/funcionarios/${id}/ativo`, { ativo })
  }
}

export const funcionarioService = new FuncionarioService(http)
