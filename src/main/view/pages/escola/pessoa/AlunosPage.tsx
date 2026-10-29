import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Paginacao } from '@/components/comuns/Paginacao'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useAlunos } from '@/hooks/usePessoas'
import { useDebounce } from '@/hooks/useUtilitarios'

const TAMANHO = 20

export function AlunosPage() {
  const [parametros, setParametros] = useSearchParams()
  const [busca, setBusca] = useState(parametros.get('busca') ?? '')
  const buscaAtrasada = useDebounce(busca)
  const corrente = useAnoCorrente()
  const ano = parametros.get('ano') ? Number(parametros.get('ano')) : corrente.data?.ano
  const pagina = Number(parametros.get('pagina') ?? 0)

  const alunos = useAlunos({ busca: buscaAtrasada, ano, pagina, tamanho: TAMANHO }, ano !== undefined)

  function atualizar(mudancas: Record<string, string | undefined>) {
    const novos = new URLSearchParams(parametros)
    Object.entries(mudancas).forEach(([chave, valor]) => (valor ? novos.set(chave, valor) : novos.delete(chave)))
    setParametros(novos, { replace: true })
  }

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader titulo="Alunos" descricao="Alunos matriculados no ano letivo, com turma e responsável financeiro." />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <CampoBusca
          valor={busca}
          placeholder="Nome ou nº de cadastro"
          aoMudar={(valor) => {
            setBusca(valor)
            atualizar({ busca: valor || undefined, pagina: undefined })
          }}
        />
        <SeletorAno ano={ano} aoMudar={(novo) => atualizar({ ano: String(novo), pagina: undefined })} />
      </div>

      {alunos.isPending ? (
        <Carregando />
      ) : alunos.isError ? (
        <EstadoErro erro={alunos.error} aoTentarDeNovo={() => void alunos.refetch()} />
      ) : alunos.data.total === 0 ? (
        <EstadoVazio titulo="Nenhum aluno encontrado" descricao={busca ? 'Tente outro nome ou número.' : undefined} />
      ) : (
        <>
          <Tabela legenda="Lista de alunos">
            <thead>
              <tr>
                <Th className="w-20">Nº</Th>
                <Th>Nome</Th>
                <Th className="w-24">Idade</Th>
                <Th>Turma</Th>
                <Th>Matrícula</Th>
                <Th>Responsável financeiro</Th>
              </tr>
            </thead>
            <tbody className={alunos.isPlaceholderData ? 'opacity-60' : ''}>
              {alunos.data.itens.map((a) => (
                <Tr key={a.id}>
                  <Td className="text-slate-500 tabular-nums">{a.numeroCadastro}</Td>
                  <Td className="font-medium">
                    <Link to={`/escola/alunos/${a.id}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                      {a.nome}
                    </Link>
                  </Td>
                  <Td>{a.idade} anos</Td>
                  <Td>
                    <Link to={`/escola/turmas/${a.turmaId}`} className="hover:text-semear-700 hover:underline">
                      {a.turma}
                    </Link>
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-1">
                      <StatusMatriculaBadge status={a.statusMatricula} />
                      {a.tipoMatricula === 'NOVA' && <Badge cor="azul">Nova</Badge>}
                      {a.documentosPendentes > 0 && (
                        <Badge cor="amarelo">
                          {a.documentosPendentes} {a.documentosPendentes === 1 ? 'doc. pendente' : 'docs. pendentes'}
                        </Badge>
                      )}
                    </div>
                  </Td>
                  <Td className="text-slate-600">{a.responsavelFinanceiro ?? <Badge cor="vermelho">Não definido</Badge>}</Td>
                </Tr>
              ))}
            </tbody>
          </Tabela>
          <Paginacao
            pagina={pagina}
            tamanho={TAMANHO}
            total={alunos.data.total}
            aoMudar={(nova) => atualizar({ pagina: nova ? String(nova) : undefined })}
          />
        </>
      )}
    </div>
  )
}
