import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Paginacao } from '@/components/comuns/Paginacao'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useMatriculas } from '@/hooks/useMatricula'
import { useDebounce } from '@/hooks/useUtilitarios'
import { formatarData } from '@/lib/formatters'
import { ROTULO_STATUS_MATRICULA, ROTULO_TIPO_MATRICULA, type StatusMatricula } from '@/models/enums'

const TAMANHO = 20

export function MatriculasPage() {
  const [parametros, setParametros] = useSearchParams()
  const [busca, setBusca] = useState(parametros.get('busca') ?? '')
  const buscaAtrasada = useDebounce(busca)
  const corrente = useAnoCorrente()
  const ano = parametros.get('ano') ? Number(parametros.get('ano')) : corrente.data?.ano
  const status = (parametros.get('status') as StatusMatricula | null) ?? undefined
  const pagina = Number(parametros.get('pagina') ?? 0)
  const consulta = useMatriculas({ busca: buscaAtrasada, ano, status, pagina, tamanho: TAMANHO }, ano !== undefined)

  function atualizar(mudancas: Record<string, string | undefined>) {
    const novos = new URLSearchParams(parametros)
    Object.entries(mudancas).forEach(([chave, valor]) => (valor ? novos.set(chave, valor) : novos.delete(chave)))
    setParametros(novos, { replace: true })
  }

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader titulo="Matrículas" descricao="Matrículas novas e rematrículas por ano letivo." />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <CampoBusca
          valor={busca}
          placeholder="Aluno ou nº de cadastro"
          aoMudar={(valor) => {
            setBusca(valor)
            atualizar({ busca: valor || undefined, pagina: undefined })
          }}
        />
        <SeletorAno ano={ano} aoMudar={(novo) => atualizar({ ano: String(novo), pagina: undefined })} />
        <div className="w-52">
          <Select
            rotulo="Situação"
            ocultarRotulo
            value={status ?? ''}
            onChange={(e) => atualizar({ status: e.target.value || undefined, pagina: undefined })}
          >
            <option value="">Todas as situações</option>
            {Object.entries(ROTULO_STATUS_MATRICULA).map(([valor, rotulo]) => (
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
      ) : consulta.data.total === 0 ? (
        <EstadoVazio titulo="Nenhuma matrícula encontrada" />
      ) : (
        <>
          <Tabela legenda="Matrículas">
            <thead>
              <tr>
                <Th>Aluno</Th>
                <Th>Turma</Th>
                <Th>Tipo</Th>
                <Th>Situação</Th>
                <Th>Data</Th>
              </tr>
            </thead>
            <tbody className={consulta.isPlaceholderData ? 'opacity-60' : ''}>
              {consulta.data.itens.map((m) => (
                <Tr key={m.id}>
                  <Td className="font-medium">
                    <Link to={`/escola/matriculas/${m.id}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                      {m.aluno}
                    </Link>
                    <span className="block text-xs font-normal text-slate-500">Nº {m.numeroCadastro}</span>
                  </Td>
                  <Td>{m.turma}</Td>
                  <Td>{ROTULO_TIPO_MATRICULA[m.tipo]}</Td>
                  <Td>
                    <StatusMatriculaBadge status={m.status} />
                  </Td>
                  <Td className="whitespace-nowrap text-slate-600">{formatarData(m.dataMatricula)}</Td>
                </Tr>
              ))}
            </tbody>
          </Tabela>
          <Paginacao
            pagina={pagina}
            tamanho={TAMANHO}
            total={consulta.data.total}
            aoMudar={(nova) => atualizar({ pagina: nova ? String(nova) : undefined })}
          />
        </>
      )}
    </div>
  )
}
