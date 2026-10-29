import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { CamposFarda } from '@/components/comuns/CamposFarda'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useNovaPreMatricula, useOpcoesMatricula } from '@/hooks/usePortal'
import { ApiError } from '@/lib/ApiError'
import { formatarCpf } from '@/lib/formatters'
import type { NovaPreMatricula } from '@/models/matricula'

const VAZIO: NovaPreMatricula = {
  ano: 0,
  serieId: '',
  alunoNome: '',
  alunoNascimento: '',
  alunoCpf: '',
  parentesco: 'Mãe',
  fardaUniforme: '',
  fardaEdFisica: '',
}

/** RF01.1: pré-matrícula preenchida pelo responsável. */
export function NovaPreMatriculaPage() {
  const opcoes = useOpcoesMatricula()
  const enviar = useNovaPreMatricula()
  const navegar = useNavigate()
  const [dados, setDados] = useState<NovaPreMatricula>(VAZIO)

  useEffect(() => {
    const ultimo = opcoes.data?.anos.at(-1)
    if (ultimo && dados.ano === 0) setDados((d) => ({ ...d, ano: ultimo }))
  }, [opcoes.data, dados.ano])

  if (opcoes.isPending) return <Carregando />
  if (opcoes.isError) return <EstadoErro erro={opcoes.error} aoTentarDeNovo={() => void opcoes.refetch()} />

  const erro = enviar.error instanceof ApiError ? enviar.error : null
  const erroCampo = (campo: string) => erro?.campos.find((c) => c.campo === campo)?.mensagem
  const campo = <K extends keyof NovaPreMatricula>(chave: K, valor: NovaPreMatricula[K]) =>
    setDados((d) => ({ ...d, [chave]: valor }))
  const niveis = [...new Set(opcoes.data.series.map((s) => s.nivelRotulo))]

  function submeter(evento: FormEvent) {
    evento.preventDefault()
    enviar.mutate(dados, { onSuccess: () => navegar('/portal', { replace: true }) })
  }

  return (
    <div>
      <PageHeader
        titulo="Nova pré-matrícula"
        descricao="Preencha os dados da criança. A secretaria analisa o pedido e você acompanha a resposta aqui no portal."
        voltarPara={{ rota: '/portal', rotulo: 'Início' }}
      />
      <form onSubmit={submeter} noValidate className="space-y-4">
        <Card titulo="Ano e série">
          <div className="grid gap-4 sm:grid-cols-2">
            <Select rotulo="Ano letivo" value={dados.ano || ''} onChange={(e) => campo('ano', Number(e.target.value))} erro={erroCampo('ano')}>
              {opcoes.data.anos.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </Select>
            <Select rotulo="Série" value={dados.serieId} onChange={(e) => campo('serieId', e.target.value)} erro={erroCampo('serieId')}>
              <option value="">Escolha a série</option>
              {niveis.map((nivel) => (
                <optgroup key={nivel} label={nivel}>
                  {opcoes.data.series
                    .filter((s) => s.nivelRotulo === nivel)
                    .map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.nome}
                      </option>
                    ))}
                </optgroup>
              ))}
            </Select>
          </div>
        </Card>

        <Card titulo="Criança">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Input rotulo="Nome completo" value={dados.alunoNome} onChange={(e) => campo('alunoNome', e.target.value)} erro={erroCampo('alunoNome')} />
            </div>
            <Input
              rotulo="Data de nascimento"
              type="date"
              value={dados.alunoNascimento}
              onChange={(e) => campo('alunoNascimento', e.target.value)}
              erro={erroCampo('alunoNascimento')}
            />
            <Input
              rotulo="CPF da criança (opcional)"
              inputMode="numeric"
              value={dados.alunoCpf}
              onChange={(e) => campo('alunoCpf', formatarCpf(e.target.value))}
              erro={erroCampo('cpf')}
              placeholder="000.000.000-00"
            />
            <Select rotulo="Seu parentesco" value={dados.parentesco} onChange={(e) => campo('parentesco', e.target.value)} erro={erroCampo('parentesco')}>
              {['Mãe', 'Pai', 'Avó', 'Avô', 'Tia', 'Tio', 'Responsável legal'].map((p) => (
                <option key={p}>{p}</option>
              ))}
            </Select>
          </div>
        </Card>

        <Card titulo="Fardas">
          <CamposFarda
            uniforme={dados.fardaUniforme}
            edFisica={dados.fardaEdFisica}
            aoMudar={(c, valor) => campo(c === 'uniforme' ? 'fardaUniforme' : 'fardaEdFisica', valor)}
            erros={{ uniforme: erroCampo('fardaUniforme'), edFisica: erroCampo('fardaEdFisica') }}
          />
        </Card>

        {erro && erro.campos.length === 0 && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {erro.mensagemAmigavel}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variante="secundario" onClick={() => navegar('/portal')}>
            Cancelar
          </Button>
          <Button type="submit" carregando={enviar.isPending}>
            Enviar pedido
          </Button>
        </div>
      </form>
    </div>
  )
}
