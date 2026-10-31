import { useMemo, useState } from 'react'
import { Link } from 'react-router'
import { MessageCircle, Phone } from '@/components/ui/icones'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { StatusMatriculaBadge } from '@/components/comuns/StatusMatriculaBadge'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { Select } from '@/components/ui/Select'
import { usePendenciasDocumentos } from '@/hooks/usePendencias'
import type { PendenciaDocumento } from '@/models/pendencia'

/**
 * Pendências de documentos (orientação do prof. Gusmão): a matrícula pode ser concluída sem tudo,
 * mas o gestor e a secretaria precisam ver quem falta para cobrar a família.
 */
export function PendenciasPage() {
  const consulta = usePendenciasDocumentos()
  const [busca, setBusca] = useState('')
  const [turma, setTurma] = useState('')

  const turmas = useMemo(
    () => [...new Set((consulta.data ?? []).map((p) => rotuloTurma(p)))].sort((a, b) => a.localeCompare(b, 'pt-BR')),
    [consulta.data],
  )
  const filtradas = useMemo(() => {
    const termo = normalizar(busca)
    return (consulta.data ?? []).filter(
      (p) =>
        (!turma || rotuloTurma(p) === turma) &&
        (!termo || normalizar(p.aluno).includes(termo) || normalizar(p.responsavel ?? '').includes(termo)),
    )
  }, [consulta.data, busca, turma])

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Pendências"
        descricao="Alunos com matrícula vigente e documentos obrigatórios faltando. A matrícula continua valendo, mas a escola precisa desses documentos (ex.: Censo Escolar)."
      />

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.length === 0 ? (
        <EstadoVazio titulo="Nenhuma pendência" descricao="Todos os alunos com matrícula vigente entregaram os documentos obrigatórios." />
      ) : (
        <>
          <dl className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-3">
            <Indicador rotulo="Alunos com pendência" valor={consulta.data.length} />
            <Indicador rotulo="Documentos faltando" valor={consulta.data.reduce((s, p) => s + p.pendentes, 0)} />
            <Indicador
              rotulo="Aguardando pagamento"
              valor={consulta.data.filter((p) => p.statusMatricula === 'AGUARDANDO_PAGAMENTO').length}
            />
          </dl>

          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end">
            <CampoBusca valor={busca} aoMudar={setBusca} placeholder="Buscar por aluno ou responsável" />
            <div className="w-full sm:w-56">
              <Select rotulo="Turma" ocultarRotulo value={turma} onChange={(e) => setTurma(e.target.value)}>
                <option value="">Todas as turmas</option>
                {turmas.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {filtradas.length === 0 ? (
            <EstadoVazio titulo="Nada encontrado" descricao="Nenhuma pendência com esses filtros." />
          ) : (
            <>
            <ul className="space-y-3 md:hidden" aria-label="Alunos com documentos pendentes">
              {filtradas.map((p) => (
                <li key={p.alunoId} className="rounded-xl border border-slate-200 bg-white p-4">
                  <div className="flex items-start justify-between gap-2">
                    <Link to={`/escola/alunos/${p.alunoId}#documentos`} className="font-medium text-slate-900 hover:text-semear-700">
                      {p.aluno}
                    </Link>
                    <StatusMatriculaBadge status={p.statusMatricula} />
                  </div>
                  <p className="text-xs text-slate-500">
                    Nº {p.numeroCadastro} · {rotuloTurma(p)}
                  </p>
                  <p className="mt-2 text-xs font-medium text-red-700">
                    Faltam {p.pendentes} {p.pendentes === 1 ? 'documento' : 'documentos'}
                  </p>
                  <ul className="mt-1 flex flex-wrap gap-1">
                    {p.faltando.map((doc) => (
                      <li key={doc}>
                        <Badge cor="amarelo">{doc}</Badge>
                      </li>
                    ))}
                  </ul>
                  <div className="mt-3 border-t border-slate-100 pt-2 text-sm">
                    {p.responsavel ? (
                      <>
                        <p className="text-slate-800">{p.responsavel}</p>
                        {p.telefone && <Contato pendencia={p} />}
                      </>
                    ) : (
                      <Badge cor="vermelho">Responsável financeiro não definido</Badge>
                    )}
                  </div>
                </li>
              ))}
            </ul>
            <div className="hidden md:block">
            <Tabela legenda="Alunos com documentos pendentes">
              <thead>
                <tr>
                  <Th>Aluno</Th>
                  <Th>Turma</Th>
                  <Th>Faltando</Th>
                  <Th>Responsável financeiro</Th>
                </tr>
              </thead>
              <tbody>
                {filtradas.map((p) => (
                  <Tr key={p.alunoId}>
                    <Td className="align-top">
                      <Link to={`/escola/alunos/${p.alunoId}#documentos`} className="font-medium text-slate-900 hover:text-semear-700 hover:underline">
                        {p.aluno}
                      </Link>
                      <p className="text-xs text-slate-500">Nº {p.numeroCadastro}</p>
                    </Td>
                    <Td className="align-top">
                      <Link to={`/escola/turmas/${p.turmaId}`} className="hover:text-semear-700 hover:underline">
                        {rotuloTurma(p)}
                      </Link>
                      <div className="mt-1">
                        <StatusMatriculaBadge status={p.statusMatricula} />
                      </div>
                    </Td>
                    <Td className="align-top">
                      <p className="mb-1 text-xs font-medium text-red-700">
                        {p.pendentes} {p.pendentes === 1 ? 'documento' : 'documentos'}
                      </p>
                      <ul className="flex max-w-md flex-wrap gap-1">
                        {p.faltando.map((doc) => (
                          <li key={doc}>
                            <Badge cor="amarelo">{doc}</Badge>
                          </li>
                        ))}
                      </ul>
                    </Td>
                    <Td className="align-top">
                      {p.responsavel ? (
                        <>
                          <p className="text-slate-800">{p.responsavel}</p>
                          {p.telefone && <Contato pendencia={p} />}
                        </>
                      ) : (
                        <Badge cor="vermelho">Não definido</Badge>
                      )}
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Tabela>
            </div>
            </>
          )}
        </>
      )}
    </div>
  )
}

/** Telefone e atalho de WhatsApp com a mensagem de cobrança já escrita. */
function Contato({ pendencia }: { pendencia: PendenciaDocumento }) {
  const digitos = (pendencia.telefone ?? '').replace(/\D/g, '')
  const primeiroNome = pendencia.responsavel?.split(' ')[0] ?? ''
  const mensagem =
    `Olá, ${primeiroNome}! Aqui é da secretaria do Centro Educacional Semeando. ` +
    `Ainda faltam documentos da matrícula de ${pendencia.aluno}: ${pendencia.faltando.join('; ')}. ` +
    'Você pode enviar pelo Portal do Responsável ou trazer na secretaria. Obrigado!'
  return (
    <div className="mt-1 flex flex-wrap items-center gap-3 text-sm">
      <a href={`tel:+55${digitos}`} className="inline-flex items-center gap-1 text-slate-600 hover:text-semear-700">
        <Phone className="size-3.5" aria-hidden /> {pendencia.telefone}
      </a>
      {digitos.length >= 10 && (
        <a
          href={`https://wa.me/55${digitos}?text=${encodeURIComponent(mensagem)}`}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 font-medium text-semear-700 hover:underline"
        >
          <MessageCircle className="size-3.5" aria-hidden /> Cobrar no WhatsApp
        </a>
      )}
    </div>
  )
}

function Indicador({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className="text-2xl font-semibold text-slate-900">{valor}</dd>
    </div>
  )
}

function rotuloTurma(p: PendenciaDocumento) {
  return `${p.turma} (${p.ano})`
}

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}
