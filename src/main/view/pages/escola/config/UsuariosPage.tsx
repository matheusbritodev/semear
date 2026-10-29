import { useMemo, useState } from 'react'
import { Copy, KeyRound, Plus, UserCheck, UserX } from 'lucide-react'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useContas, useEditarContas, useFuncionariosSemLogin } from '@/hooks/useConfiguracao'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { PERFIL_SUGERIDO, type Conta, type SenhaTemporaria } from '@/models/configuracao'
import { ROTULO_PERFIL, ROTULO_TIPO_FUNCIONARIO, usaPortal, type PerfilUsuario } from '@/models/enums'

const PERFIS_ESCOLA: PerfilUsuario[] = ['ADMIN', 'SECRETARIA', 'FINANCEIRO', 'PROFESSOR']

/**
 * Logins do sistema. Sem servidor de e-mail, a escola cria o acesso com uma senha provisória
 * e a repassa à pessoa; o mesmo serve para destravar quem esqueceu a senha.
 */
export function UsuariosPage() {
  const contas = useContas()
  const editar = useEditarContas()
  const podeEditar = usePermissao('USUARIOS', 'GRAVAR')
  const [grupo, setGrupo] = useState<'escola' | 'portal'>('escola')
  const [busca, setBusca] = useState('')
  const [criando, setCriando] = useState(false)
  const [senha, setSenha] = useState<SenhaTemporaria | null>(null)
  const [confirmarSenha, setConfirmarSenha] = useState<Conta | null>(null)

  const lista = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return (contas.data ?? []).filter(
      (c) =>
        (grupo === 'portal') === usaPortal(c.perfil) &&
        (!termo || (c.nome ?? '').toLowerCase().includes(termo) || c.email.includes(termo)),
    )
  }, [contas.data, grupo, busca])

  const erro = [editar.perfil, editar.ativo, editar.senha].find((m) => m.isError)?.error

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo="Usuários"
        descricao="Quem acessa o sistema. O perfil define o que cada pessoa pode ver e alterar (ver Perfis de acesso)."
        acoes={
          podeEditar && (
            <Button onClick={() => setCriando(true)}>
              <Plus className="size-4" aria-hidden /> Dar acesso a funcionário
            </Button>
          )
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div role="tablist" aria-label="Grupo de usuários" className="flex gap-1 rounded-lg bg-slate-100 p-1 text-sm">
          {(['escola', 'portal'] as const).map((g) => (
            <button
              key={g}
              type="button"
              role="tab"
              aria-selected={grupo === g}
              onClick={() => setGrupo(g)}
              className={`rounded-md px-3 py-1.5 font-medium ${grupo === g ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600'}`}
            >
              {g === 'escola' ? 'Equipe da escola' : 'Famílias (portal)'}
            </button>
          ))}
        </div>
        <CampoBusca valor={busca} aoMudar={setBusca} placeholder="Nome ou e-mail" />
      </div>

      {erro && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {erro instanceof ApiError ? erro.message : 'Não foi possível concluir.'}
        </p>
      )}

      {contas.isPending ? (
        <Carregando />
      ) : contas.isError ? (
        <EstadoErro erro={contas.error} aoTentarDeNovo={() => void contas.refetch()} />
      ) : lista.length === 0 ? (
        <EstadoVazio titulo="Ninguém encontrado" />
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {lista.map((c) => (
            <li key={c.id} className={`rounded-xl border bg-white p-4 ${c.ativo ? 'border-slate-200' : 'border-dashed border-slate-300'}`}>
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="truncate font-medium text-slate-900">
                    {c.nome ?? 'Cadastro não concluído'} {c.eu && <span className="text-xs font-normal text-slate-500">(você)</span>}
                  </p>
                  <p className="truncate text-sm text-slate-500">{c.email}</p>
                </div>
                {c.ativo ? <Badge cor="verde">Ativo</Badge> : <Badge cor="cinza">Desativado</Badge>}
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {c.tipoFuncionario ? ROTULO_TIPO_FUNCIONARIO[c.tipoFuncionario] : c.vinculo === 'RESPONSAVEL' ? 'Responsável' : 'Sem cadastro'}
                {' · '}
                {c.ultimoLogin ? `último acesso em ${formatarData(c.ultimoLogin)}` : `criado em ${formatarData(c.criadoEm)}`}
              </p>

              <div className="mt-3 flex flex-wrap items-end gap-2">
                <div className="w-44">
                  {usaPortal(c.perfil) || !podeEditar || c.eu ? (
                    <p className="text-sm">
                      <span className="text-slate-500">Perfil:</span> <strong className="font-medium">{ROTULO_PERFIL[c.perfil]}</strong>
                    </p>
                  ) : (
                    <Select
                      rotulo={`Perfil de ${c.nome ?? c.email}`}
                      ocultarRotulo
                      value={c.perfil}
                      disabled={editar.perfil.isPending}
                      onChange={(e) => editar.perfil.mutate({ id: c.id, perfil: e.target.value as PerfilUsuario })}
                    >
                      {PERFIS_ESCOLA.filter((p) => p !== 'PROFESSOR' || c.tipoFuncionario === 'PROFESSOR').map((p) => (
                        <option key={p} value={p}>
                          {ROTULO_PERFIL[p]}
                        </option>
                      ))}
                    </Select>
                  )}
                </div>
                {podeEditar && !c.eu && (
                  <>
                    <Button variante="fantasma" onClick={() => setConfirmarSenha(c)}>
                      <KeyRound className="size-4" aria-hidden /> Senha provisória
                    </Button>
                    <Button
                      variante="fantasma"
                      carregando={editar.ativo.isPending && editar.ativo.variables?.id === c.id}
                      onClick={() => editar.ativo.mutate({ id: c.id, ativo: !c.ativo })}
                    >
                      {c.ativo ? <UserX className="size-4" aria-hidden /> : <UserCheck className="size-4" aria-hidden />}
                      {c.ativo ? 'Desativar' : 'Reativar'}
                    </Button>
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {criando && <NovoAcessoModal aoFechar={() => setCriando(false)} aoCriar={(s) => (setCriando(false), setSenha(s))} />}

      <Modal
        aberto={confirmarSenha !== null}
        titulo="Gerar senha provisória"
        aoFechar={() => setConfirmarSenha(null)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setConfirmarSenha(null)}>
              Cancelar
            </Button>
            <Button
              carregando={editar.senha.isPending}
              onClick={() =>
                confirmarSenha &&
                editar.senha.mutate(confirmarSenha.id, { onSuccess: (s) => (setConfirmarSenha(null), setSenha(s)) })
              }
            >
              Gerar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          A senha atual de <strong>{confirmarSenha?.nome ?? confirmarSenha?.email}</strong> deixa de valer. Você verá a nova senha uma única
          vez para repassar à pessoa, que deve trocá-la depois em "Trocar senha", no topo da tela.
        </p>
      </Modal>

      {senha && <SenhaModal senha={senha} aoFechar={() => setSenha(null)} />}
    </div>
  )
}

function NovoAcessoModal({ aoFechar, aoCriar }: { aoFechar: () => void; aoCriar: (s: SenhaTemporaria) => void }) {
  const funcionarios = useFuncionariosSemLogin(true)
  const { criar } = useEditarContas()
  const [funcionarioId, setFuncionarioId] = useState('')
  const [email, setEmail] = useState('')
  const [perfil, setPerfil] = useState<PerfilUsuario>('SECRETARIA')
  const escolhido = funcionarios.data?.find((f) => f.id === funcionarioId)

  function escolher(id: string) {
    setFuncionarioId(id)
    const f = funcionarios.data?.find((x) => x.id === id)
    if (f) {
      setEmail(f.email ?? '')
      setPerfil(PERFIL_SUGERIDO[f.tipo])
    }
  }

  const erro = criar.error instanceof ApiError ? criar.error : null

  return (
    <Modal
      aberto
      titulo="Dar acesso a funcionário"
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button
            disabled={!funcionarioId || !email.trim()}
            carregando={criar.isPending}
            onClick={() => criar.mutate({ funcionarioId, email, perfil }, { onSuccess: aoCriar })}
          >
            Criar acesso
          </Button>
        </>
      }
    >
      {funcionarios.isPending ? (
        <Carregando />
      ) : funcionarios.data && funcionarios.data.length === 0 ? (
        <p className="text-sm text-slate-600">Todos os funcionários ativos já têm acesso. Cadastre o funcionário em Funcionários primeiro.</p>
      ) : (
        <div className="space-y-4">
          <Select rotulo="Funcionário" value={funcionarioId} onChange={(e) => escolher(e.target.value)}>
            <option value="">Escolha…</option>
            {funcionarios.data?.map((f) => (
              <option key={f.id} value={f.id}>
                {f.nome} · {ROTULO_TIPO_FUNCIONARIO[f.tipo]}
              </option>
            ))}
          </Select>
          <Input rotulo="E-mail de acesso" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <Select rotulo="Perfil" value={perfil} onChange={(e) => setPerfil(e.target.value as PerfilUsuario)}>
            {PERFIS_ESCOLA.filter((p) => p !== 'PROFESSOR' || escolhido?.tipo === 'PROFESSOR').map((p) => (
              <option key={p} value={p}>
                {ROTULO_PERFIL[p]}
              </option>
            ))}
          </Select>
          <p className="text-xs text-slate-500">
            O sistema gera uma senha provisória, mostrada uma única vez na próxima tela. O perfil Professor dá acesso só às turmas em que a
            pessoa leciona.
          </p>
          {erro && (
            <p role="alert" className="text-sm text-red-600">
              {erro.campos[0]?.mensagem ?? erro.message}
            </p>
          )}
        </div>
      )}
    </Modal>
  )
}

function SenhaModal({ senha, aoFechar }: { senha: SenhaTemporaria; aoFechar: () => void }) {
  const [copiado, setCopiado] = useState(false)
  async function copiar() {
    try {
      await navigator.clipboard.writeText(senha.senhaTemporaria)
      setCopiado(true)
    } catch {
      setCopiado(false)
    }
  }
  return (
    <Modal aberto titulo="Senha provisória" aoFechar={aoFechar} rodape={<Button onClick={aoFechar}>Já anotei</Button>}>
      <div className="space-y-3 text-sm text-slate-600">
        <p>
          Repasse à pessoa: login <strong className="text-slate-900">{senha.conta.email}</strong> e a senha abaixo. Ela não será mostrada de
          novo.
        </p>
        <div className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
          <code className="flex-1 font-mono text-lg tracking-wider text-slate-900">{senha.senhaTemporaria}</code>
          <Button variante="fantasma" onClick={() => void copiar()}>
            <Copy className="size-4" aria-hidden /> {copiado ? 'Copiada' : 'Copiar'}
          </Button>
        </div>
        <p className="text-xs">Por segurança, a pessoa deve trocar a senha no primeiro acesso, em "Trocar senha" (no topo da tela).</p>
      </div>
    </Modal>
  )
}
