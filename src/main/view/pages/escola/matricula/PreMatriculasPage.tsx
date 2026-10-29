import { Link, useSearchParams } from 'react-router'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusPreMatriculaBadge } from '@/components/comuns/StatusBadges'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { usePreMatriculas } from '@/hooks/useMatricula'
import { formatarData } from '@/lib/formatters'
import type { StatusPreMatricula } from '@/models/enums'

const ABAS: { status: StatusPreMatricula; rotulo: string }[] = [
  { status: 'PENDENTE', rotulo: 'Em análise' },
  { status: 'APROVADA', rotulo: 'Aprovadas' },
  { status: 'REJEITADA', rotulo: 'Não aprovadas' },
]

/** Fila da secretaria (RF01.2): pedidos mais antigos primeiro. */
export function PreMatriculasPage() {
  const [parametros, setParametros] = useSearchParams()
  const status = (parametros.get('status') as StatusPreMatricula | null) ?? 'PENDENTE'
  const consulta = usePreMatriculas(status)

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader titulo="Pré-matrículas" descricao="Pedidos feitos pelas famílias no portal. Aprove escolhendo a turma ou recuse com o motivo." />

      <div role="tablist" aria-label="Situação" className="mb-4 flex gap-1 border-b border-slate-200">
        {ABAS.map((aba) => (
          <button
            key={aba.status}
            role="tab"
            aria-selected={status === aba.status}
            onClick={() => setParametros({ status: aba.status })}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${status === aba.status ? 'border-semear-600 text-semear-700' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
          >
            {aba.rotulo}
          </button>
        ))}
      </div>

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.length === 0 ? (
        <EstadoVazio titulo={status === 'PENDENTE' ? 'Nenhum pedido aguardando análise' : 'Nenhum pedido nesta situação'} />
      ) : (
        <Tabela legenda="Pré-matrículas">
          <thead>
            <tr>
              <Th>Pedido em</Th>
              <Th>Criança</Th>
              <Th>Série pedida</Th>
              <Th>Responsável</Th>
              <Th>Situação</Th>
            </tr>
          </thead>
          <tbody>
            {consulta.data.map((p) => (
              <Tr key={p.id}>
                <Td className="whitespace-nowrap text-slate-600">{formatarData(p.criadoEm)}</Td>
                <Td className="font-medium">
                  <Link to={`/escola/pre-matriculas/${p.id}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                    {p.alunoNome}
                  </Link>
                  <span className="block text-xs font-normal text-slate-500">{p.idade} anos</span>
                </Td>
                <Td>
                  {p.serie} · {p.ano}
                </Td>
                <Td className="text-slate-600">
                  {p.responsavel}
                  {p.telefone && <span className="block text-xs">{p.telefone}</span>}
                </Td>
                <Td>
                  <StatusPreMatriculaBadge status={p.status} />
                </Td>
              </Tr>
            ))}
          </tbody>
        </Tabela>
      )}
    </div>
  )
}
