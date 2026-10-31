import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { FileWarning, Mail, Phone, Wallet } from '@/components/ui/icones'
import { useAuth } from '@/auth/useAuth'
import { CartaoRematricula } from '@/components/comuns/CartaoRematricula'
import { ChecklistDocumentos } from '@/components/comuns/ChecklistDocumentos'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { useDocumentos } from '@/hooks/useDocumentos'
import { useOpcaoRematricula, useRematricularPelaEscola } from '@/hooks/useMatricula'
import { useAluno, useDefinirResponsavelFinanceiro } from '@/hooks/usePessoas'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { ROTULO_TIPO_MATRICULA } from '@/models/enums'
import type { Vinculo } from '@/models/pessoa'

export function AlunoDetalhePage() {
  const { id = '' } = useParams()
  const consulta = useAluno(id)
  const { sessao } = useAuth()
  const podeEditar = usePermissao('ALUNOS', 'GRAVAR')
  const podeVerResponsaveis = sessao?.perfil !== 'PROFESSOR'
  const definir = useDefinirResponsavelFinanceiro(id)
  const [escolhido, setEscolhido] = useState<Vinculo | null>(null)
  const podeVerMatriculas = usePermissao('MATRICULA', 'LER')
  const podeRematricular = usePermissao('MATRICULA', 'GRAVAR')
  const rematricula = useOpcaoRematricula(id, podeVerMatriculas)
  const rematricular = useRematricularPelaEscola()
  const podeVerDocumentos = usePermissao('DOCUMENTOS', 'LER')
  const podeGravarDocumentos = usePermissao('DOCUMENTOS', 'GRAVAR')
  const { checklist } = useDocumentos(id, 'escola', podeVerDocumentos)
  const { hash } = useLocation()
  const carregado = consulta.isSuccess

  // Vindo da lista de pendências (#documentos), rola até a seção depois que a ficha carrega.
  useEffect(() => {
    if (carregado && hash === '#documentos') document.getElementById('documentos')?.scrollIntoView({ behavior: 'smooth' })
  }, [carregado, hash])

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />

  const aluno = consulta.data
  const atual = aluno.matriculas[0]

  function fechar() {
    definir.reset()
    setEscolhido(null)
  }

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo={aluno.nome}
        descricao={
          <>
            Nº de cadastro <strong className="text-slate-700">{aluno.numeroCadastro}</strong> · {aluno.idade} anos · nascido(a) em{' '}
            {formatarData(aluno.dataNascimento)}
          </>
        }
        voltarPara={{ rota: '/escola/alunos', rotulo: 'Alunos' }}
        acoes={atual && <StatusMatriculaBadge status={atual.status} />}
      />

      {checklist.data && !checklist.data.completo && (
        <div role="status" className="mb-6 flex flex-wrap items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm">
          <FileWarning className="size-5 shrink-0 text-amber-600" aria-hidden />
          <p className="flex-1 text-amber-900">
            <strong className="font-semibold">
              {checklist.data.pendentes} {checklist.data.pendentes === 1 ? 'documento pendente' : 'documentos pendentes'}.
            </strong>{' '}
            A matrícula vale, mas faltam:{' '}
            {checklist.data.itens
              .filter((i) => i.obrigatorio && !i.enviado)
              .map((i) => i.rotulo)
              .join('; ')}
            .
          </p>
          <a href="#documentos" className="font-medium text-amber-800 underline">
            Ver documentos
          </a>
        </div>
      )}

      <h2 className="mb-3 text-base font-semibold text-slate-800">Responsáveis</h2>
      <div className="mb-8 grid gap-3 sm:grid-cols-2">
        {aluno.responsaveis.map((r) => (
          <Card key={r.responsavelId}>
            <div className="mb-2 flex items-start justify-between gap-2">
              <div>
                <p className="font-semibold text-slate-900">
                  {podeVerResponsaveis ? (
                    <Link to={`/escola/responsaveis/${r.responsavelId}`} className="hover:text-semear-700 hover:underline">
                      {r.nome}
                    </Link>
                  ) : (
                    r.nome
                  )}
                </p>
                <p className="text-sm text-slate-500">{r.parentesco ?? 'Responsável'}</p>
              </div>
              {r.financeiro && (
                <Badge cor="verde">
                  <Wallet className="size-3" aria-hidden /> Financeiro
                </Badge>
              )}
            </div>
            <ul className="space-y-1 text-sm text-slate-600">
              {r.telefone && (
                <li className="flex items-center gap-2">
                  <Phone className="size-3.5 text-slate-400" aria-hidden /> {r.telefone}
                </li>
              )}
              {r.email && (
                <li className="flex items-center gap-2">
                  <Mail className="size-3.5 text-slate-400" aria-hidden /> {r.email}
                </li>
              )}
            </ul>
            {podeEditar && !r.financeiro && (
              <Button variante="fantasma" className="mt-3 -ml-2" onClick={() => setEscolhido(r)}>
                Tornar responsável financeiro
              </Button>
            )}
          </Card>
        ))}
      </div>

      {rematricula.data && (
        <div className="mb-8">
          <CartaoRematricula
            opcao={rematricula.data}
            podeRematricular={podeRematricular}
            emAndamento={rematricular.isPending}
            rematricular={(uniforme, edFisica) =>
              rematricular.mutateAsync({ alunoId: id, fardaUniforme: uniforme, fardaEdFisica: edFisica })
            }
          />
        </div>
      )}

      {podeVerDocumentos && (
        <section id="documentos" className="mb-8 scroll-mt-6" aria-labelledby="titulo-documentos">
          <h2 id="titulo-documentos" className="mb-3 text-base font-semibold text-slate-800">
            Documentos da matrícula
          </h2>
          <ChecklistDocumentos alunoId={id} area="escola" podeEnviar={podeGravarDocumentos} podeApagar={podeGravarDocumentos} />
        </section>
      )}

      <h2 className="mb-3 text-base font-semibold text-slate-800">Histórico de matrículas</h2>
      <Tabela legenda="Histórico de matrículas do aluno">
        <thead>
          <tr>
            <Th className="w-20">Ano</Th>
            <Th>Turma</Th>
            <Th>Tipo</Th>
            <Th>Situação</Th>
            <Th>Data</Th>
          </tr>
        </thead>
        <tbody>
          {aluno.matriculas.map((m) => (
            <Tr key={m.id}>
              <Td className="font-medium tabular-nums">{m.ano}</Td>
              <Td>
                <Link to={`/escola/turmas/${m.turmaId}`} className="hover:text-semear-700 hover:underline">
                  {m.turma}
                </Link>
              </Td>
              <Td>
                {podeVerMatriculas ? (
                  <Link to={`/escola/matriculas/${m.id}`} className="hover:text-semear-700 hover:underline">
                    {ROTULO_TIPO_MATRICULA[m.tipo]}
                  </Link>
                ) : (
                  ROTULO_TIPO_MATRICULA[m.tipo]
                )}
              </Td>
              <Td>
                <StatusMatriculaBadge status={m.status} />
              </Td>
              <Td className="text-slate-600">{formatarData(m.dataMatricula)}</Td>
            </Tr>
          ))}
        </tbody>
      </Tabela>

      <Modal
        aberto={escolhido !== null}
        titulo="Trocar responsável financeiro"
        aoFechar={fechar}
        rodape={
          <>
            <Button variante="secundario" onClick={fechar}>
              Cancelar
            </Button>
            <Button
              carregando={definir.isPending}
              onClick={() => escolhido && definir.mutate(escolhido.responsavelId, { onSuccess: fechar })}
            >
              Confirmar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          <strong>{escolhido?.nome}</strong> passará a receber as cobranças de <strong>{aluno.nome}</strong>. Cada aluno tem um
          único responsável financeiro; o atual deixa de ser.
        </p>
        {definir.error instanceof ApiError && (
          <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {definir.error.mensagemAmigavel}
          </p>
        )}
      </Modal>
    </div>
  )
}
