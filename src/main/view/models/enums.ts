// Espelham os enums do banco e do Model Java (mesmos valores).

export type PerfilUsuario = 'ADMIN' | 'SECRETARIA' | 'FINANCEIRO' | 'PROFESSOR' | 'RESPONSAVEL' | 'ALUNO'

export type Modulo =
  | 'CONFIGURACOES'
  | 'USUARIOS'
  | 'MATRICULA'
  | 'ALUNOS'
  | 'TURMAS'
  | 'DOCUMENTOS'
  | 'FINANCEIRO'
  | 'BOLSAS'
  | 'RH'
  | 'PEDAGOGICO'
  | 'OBRIGACOES'
  | 'PORTAL'
  | 'PENDENCIAS'
  | 'APROVACOES'
  | 'AUDITORIA'

export type Acao = 'LER' | 'GRAVAR'

export const ROTULO_PERFIL: Record<PerfilUsuario, string> = {
  ADMIN: 'Administração',
  SECRETARIA: 'Secretaria',
  FINANCEIRO: 'Financeiro',
  PROFESSOR: 'Professor(a)',
  RESPONSAVEL: 'Responsável',
  ALUNO: 'Aluno(a)',
}

export const ROTULO_MODULO: Record<Modulo, string> = {
  CONFIGURACOES: 'Configurações',
  USUARIOS: 'Usuários',
  MATRICULA: 'Matrículas',
  ALUNOS: 'Alunos',
  TURMAS: 'Turmas',
  DOCUMENTOS: 'Documentos',
  FINANCEIRO: 'Financeiro',
  BOLSAS: 'Bolsas',
  RH: 'Funcionários',
  PEDAGOGICO: 'Pedagógico',
  OBRIGACOES: 'Obrigações',
  PORTAL: 'Portal',
  PENDENCIAS: 'Pendências',
  APROVACOES: 'Aprovações',
  AUDITORIA: 'Auditoria',
}

export type StatusMatricula = 'AGUARDANDO_PAGAMENTO' | 'ATIVA' | 'CANCELADA' | 'ENCERRADA'
export type TipoMatricula = 'NOVA' | 'REMATRICULA'
export type TipoFuncionario = 'ADMINISTRATIVO' | 'PROFESSOR' | 'GESTOR' | 'PRESTADOR_SERVICO'
export type NivelEnsino = 'EDUCACAO_INFANTIL' | 'ENSINO_FUNDAMENTAL'

export const ROTULO_STATUS_MATRICULA: Record<StatusMatricula, string> = {
  AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
  ATIVA: 'Ativa',
  CANCELADA: 'Cancelada',
  ENCERRADA: 'Encerrada',
}

export const ROTULO_TIPO_MATRICULA: Record<TipoMatricula, string> = {
  NOVA: 'Nova',
  REMATRICULA: 'Rematrícula',
}

export const ROTULO_TIPO_FUNCIONARIO: Record<TipoFuncionario, string> = {
  ADMINISTRATIVO: 'Administrativo',
  PROFESSOR: 'Professor(a)',
  GESTOR: 'Gestão escolar',
  PRESTADOR_SERVICO: 'Prestador(a) de serviço',
}

export type StatusPreMatricula = 'PENDENTE' | 'APROVADA' | 'REJEITADA'
export type TipoContrato = 'CONTRATO_MATRICULA' | 'FICHA_SOCIO'
export type StatusContrato = 'PENDENTE' | 'ASSINADO' | 'CANCELADO'
export type StatusCobranca = 'PENDENTE' | 'PAGO' | 'VENCIDO' | 'CANCELADO'
export type TipoCobranca = 'MATRICULA' | 'TAXA_ASSOCIADO' | 'MENSALIDADE'
export type OrigemPagamento = 'SITE' | 'PRESENCIAL'

export const ROTULO_STATUS_PRE_MATRICULA: Record<StatusPreMatricula, string> = {
  PENDENTE: 'Em análise',
  APROVADA: 'Aprovada',
  REJEITADA: 'Não aprovada',
}

export const ROTULO_TIPO_CONTRATO: Record<TipoContrato, string> = {
  CONTRATO_MATRICULA: 'Contrato de Matrícula',
  FICHA_SOCIO: 'Ficha de Sócio',
}

export const ROTULO_STATUS_CONTRATO: Record<StatusContrato, string> = {
  PENDENTE: 'Aguardando assinatura',
  ASSINADO: 'Assinado',
  CANCELADO: 'Cancelado',
}

export const ROTULO_STATUS_COBRANCA: Record<StatusCobranca, string> = {
  PENDENTE: 'Em aberto',
  PAGO: 'Pago',
  VENCIDO: 'Vencido',
  CANCELADO: 'Cancelado',
}

/** Mesma lista de TipoFarda.TAMANHOS no Java (RN06). */
export const TAMANHOS_FARDA = ['2', '4', '6', '8', '10', '12', '14', '16', 'PP', 'P', 'M', 'G', 'GG'] as const

/** RESPONSAVEL e ALUNO usam o Portal; os demais, a Área da escola (igual a PerfilUsuario.usaPortal no Java). */
export function usaPortal(perfil: PerfilUsuario): boolean {
  return perfil === 'RESPONSAVEL' || perfil === 'ALUNO'
}
