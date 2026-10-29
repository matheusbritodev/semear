// Espelham dto/matricula/DocumentoDtos.java (RN12)

export type TipoDocumentoAluno =
  | 'IDENTIDADE_RESPONSAVEIS'
  | 'CERTIDAO_NASCIMENTO'
  | 'CARTEIRA_VACINACAO'
  | 'COMPROVANTE_RESIDENCIA'
  | 'DECLARACAO_ADIMPLENCIA'
  | 'FOTOS_3X4'
  | 'AUTORIZADOS_BUSCAR'

export interface Documento {
  id: string
  tipo: TipoDocumentoAluno
  extensao: string
  enviadoEm: string
}

export interface ItemChecklist {
  tipo: TipoDocumentoAluno
  rotulo: string
  obrigatorio: boolean
  enviado: boolean
  documentos: Documento[]
}

export interface Checklist {
  completo: boolean
  pendentes: number
  itens: ItemChecklist[]
}

export interface LinkArquivo {
  url: string
  expiraEm: string
}

/** Mesmo limite de ArquivoEnviado.TAMANHO_MAXIMO no Java. */
export const TAMANHO_MAXIMO_ARQUIVO = 5 * 1024 * 1024
export const FORMATOS_ACEITOS = 'application/pdf,image/jpeg,image/png'
