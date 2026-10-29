import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Paginacao } from '@/components/comuns/Paginacao'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { useResponsaveis } from '@/hooks/usePessoas'
import { useDebounce } from '@/hooks/useUtilitarios'

const TAMANHO = 20

export function ResponsaveisPage() {
  const [parametros, setParametros] = useSearchParams()
  const [busca, setBusca] = useState(parametros.get('busca') ?? '')
  const buscaAtrasada = useDebounce(busca)
  const pagina = Number(parametros.get('pagina') ?? 0)
  const consulta = useResponsaveis(buscaAtrasada, pagina)

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader titulo="Responsáveis" descricao="Pais, mães e demais responsáveis pelos alunos." />

      <div className="mb-4">
        <CampoBusca
          valor={busca}
          placeholder="Nome ou CPF"
          aoMudar={(valor) => {
            setBusca(valor)
            setParametros(valor ? { busca: valor } : {}, { replace: true })
          }}
        />
      </div>

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.total === 0 ? (
        <EstadoVazio titulo="Nenhum responsável encontrado" />
      ) : (
        <>
          <Tabela legenda="Lista de responsáveis">
            <thead>
              <tr>
                <Th>Nome</Th>
                <Th>CPF</Th>
                <Th>Telefone</Th>
                <Th>E-mail</Th>
                <Th className="w-20 text-center">Alunos</Th>
                <Th>Portal</Th>
              </tr>
            </thead>
            <tbody className={consulta.isPlaceholderData ? 'opacity-60' : ''}>
              {consulta.data.itens.map((r) => (
                <Tr key={r.id}>
                  <Td className="font-medium">
                    <Link to={`/escola/responsaveis/${r.id}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                      {r.nome}
                    </Link>
                  </Td>
                  <Td className="whitespace-nowrap text-slate-600 tabular-nums">{r.cpf}</Td>
                  <Td className="whitespace-nowrap text-slate-600">{r.telefone ?? '—'}</Td>
                  <Td className="text-slate-600">{r.email ?? '—'}</Td>
                  <Td className="text-center tabular-nums">{r.quantidadeAlunos}</Td>
                  <Td>{r.possuiLogin ? <Badge cor="verde">Com acesso</Badge> : <Badge>Sem acesso</Badge>}</Td>
                </Tr>
              ))}
            </tbody>
          </Tabela>
          <Paginacao
            pagina={pagina}
            tamanho={TAMANHO}
            total={consulta.data.total}
            aoMudar={(nova) => {
              const novos = new URLSearchParams(parametros)
              if (nova) novos.set('pagina', String(nova))
              else novos.delete('pagina')
              setParametros(novos)
            }}
          />
        </>
      )}
    </div>
  )
}
