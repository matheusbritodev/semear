/** Espelha dto/PaginaResponse.java. */
export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
}
