import { ApiError } from './ApiError'

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
type Parametros = Record<string, string | number | boolean | undefined>
type ObterToken = () => Promise<string | null>

/**
 * Cliente HTTP da View para os Controllers Java.
 * Envia o JWT do Supabase em toda requisição e converte erros em ApiError.
 */
export class HttpClient {
  private readonly base: string
  private readonly obterToken: ObterToken

  constructor(base: string, obterToken: ObterToken) {
    this.base = base.replace(/\/$/, '')
    this.obterToken = obterToken
  }

  get<T>(caminho: string, parametros?: Parametros): Promise<T> {
    return this.requisitar<T>('GET', caminho, undefined, parametros)
  }

  post<T>(caminho: string, corpo?: unknown): Promise<T> {
    return this.requisitar<T>('POST', caminho, corpo)
  }

  put<T>(caminho: string, corpo?: unknown): Promise<T> {
    return this.requisitar<T>('PUT', caminho, corpo)
  }

  patch<T>(caminho: string, corpo?: unknown): Promise<T> {
    return this.requisitar<T>('PATCH', caminho, corpo)
  }

  delete<T>(caminho: string): Promise<T> {
    return this.requisitar<T>('DELETE', caminho)
  }

  /** Upload multipart (o navegador define o Content-Type com o boundary). */
  enviarArquivo<T>(caminho: string, formulario: FormData): Promise<T> {
    return this.requisitar<T>('POST', caminho, formulario)
  }

  private async requisitar<T>(metodo: Metodo, caminho: string, corpo?: unknown, parametros?: Parametros): Promise<T> {
    const url = new URL(this.base + caminho, window.location.origin)
    Object.entries(parametros ?? {}).forEach(([chave, valor]) => {
      if (valor !== undefined) url.searchParams.set(chave, String(valor))
    })

    const cabecalhos: Record<string, string> = { Accept: 'application/json' }
    const token = await this.obterToken()
    if (token) cabecalhos.Authorization = `Bearer ${token}`
    const ehFormulario = corpo instanceof FormData
    if (corpo !== undefined && !ehFormulario) cabecalhos['Content-Type'] = 'application/json'

    let resposta: Response
    try {
      resposta = await fetch(url, {
        method: metodo,
        headers: cabecalhos,
        body: corpo === undefined ? undefined : ehFormulario ? corpo : JSON.stringify(corpo),
      })
    } catch {
      throw ApiError.rede()
    }

    if (resposta.status === 204) return undefined as T

    const dados: unknown = await resposta.json().catch(() => null)
    if (!resposta.ok) {
      throw new ApiError(
        ApiError.ehErroResponse(dados)
          ? dados
          : { status: resposta.status, codigo: 'ERRO_INTERNO', mensagem: 'Resposta inesperada do servidor', campos: [] },
      )
    }
    return dados as T
  }
}
