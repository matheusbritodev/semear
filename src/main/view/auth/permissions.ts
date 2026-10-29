import type { Acao, Modulo } from '@/models/enums'
import type { Sessao } from '@/models/sessao'

/**
 * A View usa isto só para esconder menus e botões.
 * Quem garante a permissão de verdade é o Controller (@PreAuthorize).
 */
export function pode(sessao: Sessao | null, modulo: Modulo, acao: Acao): boolean {
  const permissao = sessao?.permissoes.find((p) => p.modulo === modulo)
  if (!permissao) return false
  return acao === 'GRAVAR' ? permissao.gravar : permissao.ler || permissao.gravar
}
