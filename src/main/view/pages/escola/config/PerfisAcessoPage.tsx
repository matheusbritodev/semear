import { useState } from 'react'
import { RotateCcw } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useAlterarPermissoes, useMatrizPermissoes } from '@/hooks/usePendencias'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { ROTULO_MODULO, ROTULO_PERFIL, type Modulo, type PerfilUsuario } from '@/models/enums'
import type { CelulaPermissao } from '@/models/pendencia'

type Nivel = 'nenhum' | 'consultar' | 'alterar'

const ROTULO_NIVEL: Record<Nivel, string> = { nenhum: 'Sem acesso', consultar: 'Consultar', alterar: 'Alterar' }

/** Mesma trava da MatrizPermissoes (Java): o ADMIN não pode perder estes, senão ninguém administra o sistema. */
const ESSENCIAIS_ADMIN: Modulo[] = ['CONFIGURACOES', 'USUARIOS']

/**
 * Perfis de acesso por cargo (orientação do prof. Gusmão): vem com um padrão
 * e o gestor liga ou desliga o que quiser, podendo voltar ao padrão a qualquer momento.
 */
export function PerfisAcessoPage() {
  const matriz = useMatrizPermissoes()
  const podeAlterar = usePermissao('CONFIGURACOES', 'GRAVAR')
  const { alterar, restaurar } = useAlterarPermissoes()
  const [confirmarPadrao, setConfirmarPadrao] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  /** No celular a matriz não cabe: mostra um perfil por vez. */
  const [perfilCelular, setPerfilCelular] = useState<PerfilUsuario>('SECRETARIA')

  if (matriz.isPending) return <Carregando />
  if (matriz.isError) return <EstadoErro erro={matriz.error} aoTentarDeNovo={() => void matriz.refetch()} />

  const { modulos, perfis } = matriz.data
  const celula = (perfil: PerfilUsuario, modulo: Modulo): CelulaPermissao =>
    perfis.find((p) => p.perfil === perfil)?.permissoes.find((c) => c.modulo === modulo) ?? { modulo, ler: false, gravar: false }

  async function mudar(perfil: PerfilUsuario, modulo: Modulo, nivel: Nivel) {
    setErro(null)
    try {
      await alterar.mutateAsync({ perfil, modulo, ler: nivel !== 'nenhum', gravar: nivel === 'alterar' })
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível salvar a alteração.')
    }
  }

  /** Caixa "Sem acesso / Consultar / Alterar" de um perfil num módulo (mesma na matriz e na lista do celular). */
  function seletor(perfil: PerfilUsuario, modulo: Modulo, prefixo: string) {
    const c = celula(perfil, modulo)
    const nivel: Nivel = c.gravar ? 'alterar' : c.ler ? 'consultar' : 'nenhum'
    const travada = perfil === 'ADMIN' && ESSENCIAIS_ADMIN.includes(modulo)
    const salvando = alterar.isPending && alterar.variables?.perfil === perfil && alterar.variables.modulo === modulo
    return (
      <select
        id={`${prefixo}-${perfil}-${modulo}`}
        value={nivel}
        disabled={!podeAlterar || travada || salvando}
        title={travada ? 'A administração sempre mantém este acesso' : undefined}
        onChange={(e) => void mudar(perfil, modulo, e.target.value as Nivel)}
        className={`w-full rounded-lg border px-2 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-70 ${corNivel(nivel)}`}
      >
        {(Object.keys(ROTULO_NIVEL) as Nivel[]).map((n) => (
          <option key={n} value={n}>
            {ROTULO_NIVEL[n]}
          </option>
        ))}
      </select>
    )
  }

  async function voltarAoPadrao() {
    setErro(null)
    try {
      await restaurar.mutateAsync()
      setConfirmarPadrao(false)
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível restaurar o padrão.')
      setConfirmarPadrao(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Perfis de acesso"
        descricao="O que cada cargo pode consultar ou alterar. As mudanças valem na hora para todos os usuários do perfil."
        acoes={
          podeAlterar && (
            <Button variante="secundario" onClick={() => setConfirmarPadrao(true)}>
              <RotateCcw className="size-4" aria-hidden /> Restaurar padrão
            </Button>
          )
        }
      />

      {erro && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {erro}
        </p>
      )}
      {!podeAlterar && (
        <p className="mb-4 rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-600">
          Você pode consultar os perfis, mas só a administração altera.
        </p>
      )}

      {/* Celular: escolhe o perfil e vê a lista de módulos */}
      <div className="md:hidden">
        <div className="mb-3">
          <Select rotulo="Perfil" value={perfilCelular} onChange={(e) => setPerfilCelular(e.target.value as PerfilUsuario)}>
            {perfis.map((p) => (
              <option key={p.perfil} value={p.perfil}>
                {ROTULO_PERFIL[p.perfil]}
              </option>
            ))}
          </Select>
        </div>
        <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
          {modulos.map((modulo) => (
            <li key={modulo} className="flex items-center justify-between gap-3 px-4 py-2.5">
              <label htmlFor={`cel-${perfilCelular}-${modulo}`} className="text-sm font-medium text-slate-800">
                {ROTULO_MODULO[modulo]}
              </label>
              <div className="w-36 shrink-0">{seletor(perfilCelular, modulo, 'cel')}</div>
            </li>
          ))}
        </ul>
      </div>

      {/* Computador: a matriz completa */}
      <div className="hidden md:block">
        <Tabela legenda="Permissões por perfil e módulo">
          <thead>
            <tr>
              <Th className="sticky left-0 bg-slate-50">Módulo</Th>
              {perfis.map((p) => (
                <Th key={p.perfil} className="min-w-36">
                  {ROTULO_PERFIL[p.perfil]}
                </Th>
              ))}
            </tr>
          </thead>
          <tbody>
            {modulos.map((modulo) => (
              <Tr key={modulo}>
                <Td className="sticky left-0 bg-white font-medium">{ROTULO_MODULO[modulo]}</Td>
                {perfis.map(({ perfil }) => (
                  <Td key={perfil}>
                    <label className="sr-only" htmlFor={`mat-${perfil}-${modulo}`}>
                      {ROTULO_PERFIL[perfil]} em {ROTULO_MODULO[modulo]}
                    </label>
                    {seletor(perfil, modulo, 'mat')}
                  </Td>
                ))}
              </Tr>
            ))}
          </tbody>
        </Tabela>
      </div>
      <p className="mt-3 text-xs text-slate-500">
        “Alterar” já inclui consultar. Configurações e Usuários ficam sempre liberados para a Administração.
      </p>

      <Modal
        aberto={confirmarPadrao}
        titulo="Restaurar o padrão?"
        aoFechar={() => setConfirmarPadrao(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setConfirmarPadrao(false)}>
              Cancelar
            </Button>
            <Button onClick={() => void voltarAoPadrao()} carregando={restaurar.isPending}>
              Restaurar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Todas as permissões voltam a ser como vieram no sistema. As mudanças feitas até agora serão desfeitas.
        </p>
      </Modal>
    </div>
  )
}

function corNivel(nivel: Nivel) {
  if (nivel === 'alterar') return 'border-semear-200 bg-semear-50 text-semear-800'
  if (nivel === 'consultar') return 'border-sky-200 bg-sky-50 text-sky-800'
  return 'border-slate-200 bg-white text-slate-500'
}
