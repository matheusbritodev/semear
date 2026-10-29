import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { Pencil } from 'lucide-react'
import { BarraOcupacao } from '@/components/comuns/BarraOcupacao'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { useAlterarCapacidade, useTurma } from '@/hooks/useAcademico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import type { Turma } from '@/models/academico'
import { ROTULO_TIPO_MATRICULA } from '@/models/enums'

export function TurmaDetalhePage() {
  const { id = '' } = useParams()
  const consulta = useTurma(id)
  const podeEditar = usePermissao('TURMAS', 'GRAVAR')
  const podeVerAlunos = usePermissao('ALUNOS', 'LER')
  const [editando, setEditando] = useState(false)

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const { turma, alunos, professores } = consulta.data

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo={turma.nomeCompleto}
        descricao={`${turma.nivelRotulo} · ${turma.ano}`}
        voltarPara={{ rota: `/escola/turmas?ano=${turma.ano}`, rotulo: 'Turmas' }}
        acoes={
          podeEditar && (
            <Button variante="secundario" onClick={() => setEditando(true)}>
              <Pencil className="size-4" aria-hidden /> Alterar capacidade
            </Button>
          )
        }
      />

      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <Card titulo="Ocupação" className="md:col-span-1">
          <BarraOcupacao ocupadas={turma.ocupadas} capacidade={turma.capacidadeMax} />
          <p className="mt-3 text-sm text-slate-600">
            {turma.lotada ? (
              <Badge cor="vermelho">Lotada: novas matrículas bloqueadas</Badge>
            ) : (
              <>
                <strong>{turma.vagas}</strong> {turma.vagas === 1 ? 'vaga livre' : 'vagas livres'}
              </>
            )}
          </p>
        </Card>
        <Card titulo="Professores" className="md:col-span-2">
          {professores.length === 0 ? (
            <p className="text-sm text-slate-500">Nenhum professor vinculado.</p>
          ) : (
            <ul className="divide-y divide-slate-100 text-sm">
              {professores.map((p) => (
                <li key={`${p.funcionarioId}-${p.disciplina ?? 'pol'}`} className="flex justify-between py-2">
                  <span>{p.nome}</span>
                  <span className="text-slate-500">{p.disciplina ?? 'Polivalente'}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <h2 className="mb-3 text-base font-semibold text-slate-800">Alunos ({alunos.length})</h2>
      {alunos.length === 0 ? (
        <EstadoVazio titulo="Nenhum aluno matriculado" />
      ) : (
        <Tabela legenda={`Alunos da turma ${turma.nomeCompleto}`}>
          <thead>
            <tr>
              <Th className="w-20">Nº</Th>
              <Th>Nome</Th>
              <Th className="w-20">Idade</Th>
              <Th>Matrícula</Th>
              <Th>Situação</Th>
            </tr>
          </thead>
          <tbody>
            {alunos.map((a) => (
              <Tr key={a.matriculaId}>
                <Td className="text-slate-500 tabular-nums">{a.numeroCadastro}</Td>
                <Td className="font-medium">
                  {podeVerAlunos ? (
                    <Link to={`/escola/alunos/${a.alunoId}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                      {a.nome}
                    </Link>
                  ) : (
                    a.nome
                  )}
                </Td>
                <Td>{a.idade} anos</Td>
                <Td>{ROTULO_TIPO_MATRICULA[a.tipo]}</Td>
                <Td>
                  <StatusMatriculaBadge status={a.status} />
                </Td>
              </Tr>
            ))}
          </tbody>
        </Tabela>
      )}

      <ModalCapacidade turma={turma} aberto={editando} aoFechar={() => setEditando(false)} />
    </div>
  )
}

function ModalCapacidade({ turma, aberto, aoFechar }: { turma: Turma; aberto: boolean; aoFechar: () => void }) {
  const [valor, setValor] = useState(String(turma.capacidadeMax))
  const alterar = useAlterarCapacidade(turma.id)
  const erro = alterar.error instanceof ApiError ? (alterar.error.campos[0]?.mensagem ?? alterar.error.mensagemAmigavel) : undefined

  function fechar() {
    alterar.reset()
    setValor(String(turma.capacidadeMax))
    aoFechar()
  }

  return (
    <Modal
      aberto={aberto}
      titulo={`Capacidade da turma ${turma.nomeCompleto}`}
      aoFechar={fechar}
      rodape={
        <>
          <Button variante="secundario" onClick={fechar}>
            Cancelar
          </Button>
          <Button carregando={alterar.isPending} onClick={() => alterar.mutate(Number(valor), { onSuccess: fechar })}>
            Salvar
          </Button>
        </>
      }
    >
      <p className="mb-4 text-sm text-slate-600">
        A turma tem <strong>{turma.ocupadas}</strong> alunos. A capacidade pode ficar entre {Math.max(1, turma.ocupadas)} e 30.
      </p>
      <Input
        rotulo="Capacidade máxima"
        type="number"
        min={Math.max(1, turma.ocupadas)}
        max={30}
        value={valor}
        onChange={(e) => setValor(e.target.value)}
        erro={erro}
      />
    </Modal>
  )
}
