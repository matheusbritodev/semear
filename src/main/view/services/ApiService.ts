import type { HttpClient } from '@/lib/HttpClient'

export type Parametros = Record<string, string | number | boolean | undefined>

/**
 * Base dos services da View: cada Controller Java tem um service TypeScript
 * que herda as operações REST comuns e acrescenta as específicas.
 */
export abstract class ApiService<T> {
  protected readonly http: HttpClient
  protected readonly recurso: string

  protected constructor(http: HttpClient, recurso: string) {
    this.http = http
    this.recurso = recurso
  }

  buscar(id: string): Promise<T> {
    return this.http.get<T>(`/${this.recurso}/${id}`)
  }

  /** Remove parâmetros vazios para não poluir a URL. */
  protected limpar(parametros: Parametros): Parametros {
    return Object.fromEntries(Object.entries(parametros).filter(([, v]) => v !== undefined && v !== ''))
  }
}
