/** Formato de erro devolvido pelos Controllers (controller/advice/ErroResponse.java). */
export interface CampoErro {
  campo: string
  mensagem: string
}

export interface ErroResponse {
  status: number
  codigo: string
  mensagem: string
  campos: CampoErro[]
  instante?: string
}

/** Mensagens amigáveis por código. Código desconhecido usa a mensagem do Controller. */
const MENSAGENS: Record<string, string> = {
  NAO_AUTENTICADO: 'Sua sessão expirou. Entre novamente.',
  ACESSO_NEGADO: 'Você não tem permissão para esta ação.',
  CADASTRO_INCOMPLETO: 'Seu acesso ainda não foi liberado pela escola.',
  TURMA_LOTADA: 'Esta turma está lotada. Escolha outra turma ou ajuste a capacidade.',
  SEM_RESPONSAVEL_FINANCEIRO: 'Defina o responsável financeiro do aluno antes de continuar.',
  TABELA_PRECO_AUSENTE: 'Cadastre a tabela de preços da série e do ano letivo antes de matricular.',
  PRE_MATRICULA_JA_ANALISADA: 'Esta pré-matrícula já foi analisada. Atualize a página.',
  COBRANCA_JA_PAGA: 'Esta cobrança já está paga.',
  COBRANCA_IMUTAVEL: 'Cobranças pagas não podem ser alteradas.',
  DUPLICADO: 'Já existe um registro com esses dados.',
  ERRO_REDE: 'Não foi possível falar com o servidor. Verifique sua conexão.',
  ERRO_INTERNO: 'Ocorreu um erro inesperado. Tente novamente.',
}

export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly campos: CampoErro[]

  constructor(erro: ErroResponse) {
    super(erro.mensagem)
    this.name = 'ApiError'
    this.status = erro.status
    this.codigo = erro.codigo
    this.campos = erro.campos ?? []
  }

  get mensagemAmigavel(): string {
    return MENSAGENS[this.codigo] ?? this.message
  }

  static rede(): ApiError {
    return new ApiError({ status: 0, codigo: 'ERRO_REDE', mensagem: MENSAGENS.ERRO_REDE!, campos: [] })
  }

  static ehErroResponse(valor: unknown): valor is ErroResponse {
    return typeof valor === 'object' && valor !== null && 'codigo' in valor && 'status' in valor
  }
}
