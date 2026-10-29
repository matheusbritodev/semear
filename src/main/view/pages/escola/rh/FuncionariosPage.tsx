import { useState } from 'react'
import { Plus } from 'lucide-react'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { useAlterarAtivoFuncionario, useFuncionarios } from '@/hooks/usePessoas'
import { useDebounce, usePermissao } from '@/hooks/useUtilitarios'
import { formatarData } from '@/lib/formatters'
import { ROTULO_TIPO_FUNCIONARIO, type TipoFuncionario } from '@/models/enums'
import type { Funcionario } from '@/models/pessoa'
import { FormacoesModal } from './FormacoesModal'
import { FuncionarioModal } from './FuncionarioModal'

export function FuncionariosPage() {
  const [busca, setBusca] = useState('')
  const [tipo, setTipo] = useState<TipoFuncionario | ''>('')
  const buscaAtrasada = useDebounce(busca)
  const consulta = useFuncionarios(buscaAtrasada, tipo || undefined)
  const podeEditar = usePermissao('RH', 'GRAVAR')
  const alterarAtivo = useAlterarAtivoFuncionario()
  const [emEdicao, setEmEdicao] = useState<Funcionario | 'novo' | null>(null)
  const [formacoes, setFormacoes] = useState<Funcionario | null>(null)

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Funcionários"
        descricao="Administrativos, professores, gestão e prestadores de serviço (RF07)."
        acoes={
          podeEditar && (
            <Button onClick={() => setEmEdicao('novo')}>
              <Plus className="size-4" aria-hidden /> Novo funcionário
            </Button>
          )
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <CampoBusca valor={busca} aoMudar={setBusca} placeholder="Nome ou CPF" />
        <div className="w-56">
          <Select rotulo="Tipo" ocultarRotulo value={tipo} onChange={(e) => setTipo(e.target.value as TipoFuncionario | '')}>
            <option value="">Todos os tipos</option>
            {Object.entries(ROTULO_TIPO_FUNCIONARIO).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>
                {rotulo}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.length === 0 ? (
        <EstadoVazio titulo="Nenhum funcionário encontrado" />
      ) : (
        <Tabela legenda="Lista de funcionários">
          <thead>
            <tr>
              <Th>Nome</Th>
              <Th>Função</Th>
              <Th>Telefone</Th>
              <Th>Admissão</Th>
              <Th>Login</Th>
              <Th>Situação</Th>
              {podeEditar && <Th className="text-right">Ações</Th>}
            </tr>
          </thead>
          <tbody>
            {consulta.data.map((f) => (
              <Tr key={f.id} destacar={!f.ativo}>
                <Td className={`font-medium ${f.ativo ? 'text-slate-900' : 'text-slate-400'}`}>
                  {f.nome}
                  {f.email && <span className="block text-xs font-normal text-slate-500">{f.email}</span>}
                </Td>
                <Td>{f.funcao}</Td>
                <Td className="whitespace-nowrap text-slate-600">{f.telefone ?? '—'}</Td>
                <Td className="whitespace-nowrap text-slate-600">{f.dataAdmissao ? formatarData(f.dataAdmissao) : '—'}</Td>
                <Td>{f.possuiLogin ? <Badge cor="verde">Sim</Badge> : <Badge>Não</Badge>}</Td>
                <Td>{f.ativo ? <Badge cor="verde">Ativo</Badge> : <Badge cor="cinza">Inativo</Badge>}</Td>
                {podeEditar && (
                  <Td className="text-right whitespace-nowrap">
                    <Button variante="fantasma" onClick={() => setEmEdicao(f)}>
                      Editar
                    </Button>
                    <Button variante="fantasma" onClick={() => setFormacoes(f)}>
                      Formação
                    </Button>
                    <Button
                      variante="fantasma"
                      carregando={alterarAtivo.isPending && alterarAtivo.variables?.id === f.id}
                      onClick={() => alterarAtivo.mutate({ id: f.id, ativo: !f.ativo })}
                    >
                      {f.ativo ? 'Desativar' : 'Reativar'}
                    </Button>
                  </Td>
                )}
              </Tr>
            ))}
          </tbody>
        </Tabela>
      )}

      {formacoes && <FormacoesModal funcionarioId={formacoes.id} nome={formacoes.nome} aoFechar={() => setFormacoes(null)} />}
      {emEdicao !== null && (
        <FuncionarioModal funcionario={emEdicao === 'novo' ? null : emEdicao} aoFechar={() => setEmEdicao(null)} />
      )}
    </div>
  )
}
