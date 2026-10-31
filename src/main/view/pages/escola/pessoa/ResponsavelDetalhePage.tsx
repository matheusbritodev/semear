import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { Wallet } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { useAtualizarContato, useResponsavel } from '@/hooks/usePessoas'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarTelefone } from '@/lib/formatters'

export function ResponsavelDetalhePage() {
  const { id = '' } = useParams()
  const consulta = useResponsavel(id)
  const podeEditar = usePermissao('ALUNOS', 'GRAVAR')
  const salvar = useAtualizarContato(id)
  const [telefone, setTelefone] = useState('')
  const [email, setEmail] = useState('')
  const [salvo, setSalvo] = useState(false)

  useEffect(() => {
    if (consulta.data) {
      setTelefone(consulta.data.telefone ?? '')
      setEmail(consulta.data.email ?? '')
    }
  }, [consulta.data])

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const responsavel = consulta.data
  const erro = salvar.error instanceof ApiError ? salvar.error : null
  const erroCampo = (campo: string) => erro?.campos.find((c) => c.campo === campo)?.mensagem

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    setSalvo(false)
    salvar.mutate({ telefone, email }, { onSuccess: () => setSalvo(true) })
  }

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        titulo={responsavel.nome}
        descricao={<>CPF {responsavel.cpf}</>}
        voltarPara={{ rota: '/escola/responsaveis', rotulo: 'Responsáveis' }}
        acoes={responsavel.possuiLogin ? <Badge cor="verde">Acessa o portal</Badge> : <Badge>Sem acesso ao portal</Badge>}
      />

      <div className="grid gap-4 md:grid-cols-2">
        <Card titulo="Contato">
          <form onSubmit={enviar} className="space-y-4" noValidate>
            <Input
              rotulo="Telefone"
              inputMode="tel"
              value={telefone}
              onChange={(e) => setTelefone(formatarTelefone(e.target.value))}
              disabled={!podeEditar}
              erro={erroCampo('telefone')}
              placeholder="(98) 99999-9999"
            />
            <Input
              rotulo="E-mail"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={!podeEditar}
              erro={erroCampo('email')}
            />
            {erro && erro.campos.length === 0 && (
              <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                {erro.mensagemAmigavel}
              </p>
            )}
            {salvo && !salvar.isPending && (
              <p role="status" className="text-sm text-semear-700">
                Contato atualizado.
              </p>
            )}
            {podeEditar && (
              <Button type="submit" carregando={salvar.isPending}>
                Salvar contato
              </Button>
            )}
          </form>
        </Card>

        <Card titulo={`Alunos (${responsavel.alunos.length})`}>
          <ul className="divide-y divide-slate-100">
            {responsavel.alunos.map((a) => (
              <li key={a.alunoId} className="flex flex-wrap items-center justify-between gap-2 py-3">
                <div>
                  <Link to={`/escola/alunos/${a.alunoId}`} className="font-medium text-slate-900 hover:text-semear-700 hover:underline">
                    {a.nome}
                  </Link>
                  <p className="text-xs text-slate-500">
                    {a.parentesco ?? 'Responsável'} · {a.turmaAtual ?? 'sem matrícula'}
                  </p>
                </div>
                <div className="flex gap-1">
                  {a.financeiro && (
                    <Badge cor="verde">
                      <Wallet className="size-3" aria-hidden /> Financeiro
                    </Badge>
                  )}
                  {a.statusMatricula && <StatusMatriculaBadge status={a.statusMatricula} />}
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  )
}
