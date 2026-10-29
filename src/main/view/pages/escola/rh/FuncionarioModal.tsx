import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useSalvarFuncionario } from '@/hooks/usePessoas'
import { ApiError } from '@/lib/ApiError'
import { formatarCpf, formatarTelefone } from '@/lib/formatters'
import { ROTULO_TIPO_FUNCIONARIO, type TipoFuncionario } from '@/models/enums'
import type { Funcionario } from '@/models/pessoa'

interface Props {
  /** null = cadastro novo */
  funcionario: Funcionario | null
  aoFechar: () => void
}

/** Cadastro e edição. CPF e tipo só podem ser definidos na criação. */
export function FuncionarioModal({ funcionario, aoFechar }: Props) {
  const novo = funcionario === null
  const [nome, setNome] = useState(funcionario?.nome ?? '')
  const [cpf, setCpf] = useState(funcionario?.cpf ?? '')
  const [tipo, setTipo] = useState<TipoFuncionario>(funcionario?.tipo ?? 'PROFESSOR')
  const [telefone, setTelefone] = useState(funcionario?.telefone ?? '')
  const [email, setEmail] = useState(funcionario?.email ?? '')
  const [dataAdmissao, setDataAdmissao] = useState(funcionario?.dataAdmissao ?? '')
  const salvar = useSalvarFuncionario()

  const erro = salvar.error instanceof ApiError ? salvar.error : null
  const erroCampo = (campo: string) => erro?.campos.find((c) => c.campo === campo)?.mensagem

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const dados = { nome, telefone, email, dataAdmissao: dataAdmissao || null }
    salvar.mutate(novo ? { novo: { ...dados, cpf, tipo } } : { id: funcionario.id, dados }, { onSuccess: aoFechar })
  }

  return (
    <Modal
      aberto
      titulo={novo ? 'Novo funcionário' : `Editar ${funcionario.nome}`}
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button type="submit" form="form-funcionario" carregando={salvar.isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="form-funcionario" onSubmit={enviar} className="grid gap-4 sm:grid-cols-2" noValidate>
        <div className="sm:col-span-2">
          <Input rotulo="Nome completo" value={nome} onChange={(e) => setNome(e.target.value)} erro={erroCampo('nome')} required />
        </div>
        <Input
          rotulo="CPF"
          inputMode="numeric"
          value={cpf}
          onChange={(e) => setCpf(formatarCpf(e.target.value))}
          disabled={!novo}
          erro={erroCampo('cpf')}
          placeholder="000.000.000-00"
        />
        <Select
          rotulo="Tipo"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as TipoFuncionario)}
          disabled={!novo}
          erro={erroCampo('tipo')}
        >
          {Object.entries(ROTULO_TIPO_FUNCIONARIO).map(([valor, rotulo]) => (
            <option key={valor} value={valor}>
              {rotulo}
            </option>
          ))}
        </Select>
        <Input
          rotulo="Telefone"
          inputMode="tel"
          value={telefone}
          onChange={(e) => setTelefone(formatarTelefone(e.target.value))}
          erro={erroCampo('telefone')}
          placeholder="(98) 99999-9999"
        />
        <Input rotulo="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} erro={erroCampo('email')} />
        <Input
          rotulo="Data de admissão"
          type="date"
          value={dataAdmissao}
          onChange={(e) => setDataAdmissao(e.target.value)}
          erro={erroCampo('dataAdmissao')}
        />
        {erro && erro.campos.length === 0 && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 sm:col-span-2">
            {erro.codigo === 'DUPLICADO' ? 'Já existe um funcionário com este CPF.' : erro.mensagemAmigavel}
          </p>
        )}
      </form>
    </Modal>
  )
}
