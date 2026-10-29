import { useMemo, useState, type FormEvent } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Badge, type CorBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAnoCorrente, useAnosLetivos } from '@/hooks/useAcademico'
import { turmaService } from '@/services/TurmaService'
import { Modal } from '@/components/ui/Modal'
import { formatarData } from '@/lib/formatters'
import { hojeIso } from '@/lib/datas'
import { useCalendario, useEditarCalendario } from '@/hooks/usePedagogico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { diaDaSemana, mesIso, nomeDoMes } from '@/lib/datas'
import { ROTULO_TIPO_CALENDARIO, type TipoCalendario } from '@/models/pedagogico'

const COR: Record<TipoCalendario, CorBadge> = { FERIADO: 'vermelho', RECESSO: 'amarelo', EVENTO: 'azul', AULA: 'verde' }

/** Calendário letivo (RF08.3). Feriado e recesso bloqueiam a chamada. Só a gestão altera. */
export function CalendarioPage() {
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const calendario = useCalendario(ano)
  const editar = useEditarCalendario(ano)
  const podeEditar = usePermissao('CONFIGURACOES', 'GRAVAR')
  const [data, setData] = useState('')
  const [tipo, setTipo] = useState<TipoCalendario>('EVENTO')
  const [descricao, setDescricao] = useState('')
  const anos = useAnosLetivos()
  const anoLetivo = anos.data?.find((a) => a.ano === ano)
  const terminou = anoLetivo !== undefined && anoLetivo.dataFim < hojeIso()
  const [confirmando, setConfirmando] = useState(false)
  const cliente = useQueryClient()
  const encerrar = useMutation({
    mutationFn: (alvo: number) => turmaService.encerrarAno(alvo),
    onSuccess: () => {
      for (const queryKey of [['matriculas'], ['turmas'], ['alunos'], ['rematricula']]) void cliente.invalidateQueries({ queryKey })
    },
  })

  const porMes = useMemo(() => {
    const grupos = new Map<string, NonNullable<typeof calendario.data>>()
    for (const e of calendario.data ?? []) {
      const chave = mesIso(e.data)
      grupos.set(chave, [...(grupos.get(chave) ?? []), e])
    }
    return [...grupos.entries()]
  }, [calendario.data])

  const erro = editar.criar.error ?? editar.remover.error

  function adicionar(e: FormEvent) {
    e.preventDefault()
    editar.criar.mutate({ data, tipo, descricao }, { onSuccess: () => (setData(''), setDescricao('')) })
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        titulo="Calendário letivo"
        descricao="Feriados e recessos não têm chamada. Sábados só contam como aula quando marcados como dia letivo especial."
        voltarPara={{ rota: '/escola/pedagogico', rotulo: 'Pedagógico' }}
        acoes={<SeletorAno ano={ano} aoMudar={setAno} />}
      />

      {podeEditar && terminou && anoLetivo && (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
          <p className="text-slate-700">
            O ano letivo {anoLetivo.ano} terminou em {formatarData(anoLetivo.dataFim)}. Encerrar passa as matrículas ativas para encerradas.
          </p>
          {encerrar.isSuccess ? (
            <span className="text-semear-700">{encerrar.data.encerradas} matrículas encerradas.</span>
          ) : (
            <Button variante="secundario" onClick={() => setConfirmando(true)}>
              Encerrar ano letivo {anoLetivo.ano}
            </Button>
          )}
        </div>
      )}

      {podeEditar && (
        <Card className="mb-6">
          <form onSubmit={adicionar} className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="sm:w-44">
              <Input rotulo="Data" type="date" value={data} onChange={(e) => setData(e.target.value)} />
            </div>
            <div className="sm:w-48">
              <Select rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoCalendario)}>
                {(Object.keys(ROTULO_TIPO_CALENDARIO) as TipoCalendario[]).map((t) => (
                  <option key={t} value={t}>
                    {ROTULO_TIPO_CALENDARIO[t]}
                  </option>
                ))}
              </Select>
            </div>
            <div className="flex-1">
              <Input rotulo="Descrição" value={descricao} maxLength={200} onChange={(e) => setDescricao(e.target.value)} />
            </div>
            <Button type="submit" disabled={!data || !descricao.trim()} carregando={editar.criar.isPending}>
              <Plus className="size-4" aria-hidden /> Adicionar
            </Button>
          </form>
          {erro && (
            <p role="alert" className="mt-3 text-sm text-red-600">
              {erro instanceof ApiError ? (erro.campos[0]?.mensagem ?? erro.message) : 'Não foi possível salvar.'}
            </p>
          )}
        </Card>
      )}

      {calendario.isPending ? (
        <Carregando />
      ) : calendario.isError ? (
        <EstadoErro erro={calendario.error} aoTentarDeNovo={() => void calendario.refetch()} />
      ) : porMes.length === 0 ? (
        <EstadoVazio titulo="Nenhum dia marcado neste ano" />
      ) : (
        <div className="space-y-6">
          {porMes.map(([mes, eventos]) => (
            <section key={mes} aria-label={nomeDoMes(mes)}>
              <h2 className="mb-2 text-sm font-semibold text-slate-700 capitalize">{nomeDoMes(mes)}</h2>
              <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
                {eventos.map((e) => (
                  <li key={e.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-2 text-sm">
                    <span className="flex items-center gap-3">
                      <span className="w-28 text-slate-500 capitalize">{diaDaSemana(e.data)}</span>
                      <span className="text-slate-800">{e.descricao}</span>
                    </span>
                    <span className="flex items-center gap-2">
                      <Badge cor={COR[e.tipo]}>{e.tipoRotulo}</Badge>
                      {podeEditar && (
                        <button
                          type="button"
                          onClick={() => editar.remover.mutate(e.id)}
                          className="rounded p-1 text-slate-400 hover:text-red-600"
                          aria-label={`Remover ${e.descricao}`}
                        >
                          <Trash2 className="size-4" aria-hidden />
                        </button>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}

      <Modal
        aberto={confirmando}
        titulo={`Encerrar ano letivo ${ano ?? ''}`}
        aoFechar={() => setConfirmando(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setConfirmando(false)}>
              Voltar
            </Button>
            <Button carregando={encerrar.isPending} onClick={() => ano && encerrar.mutate(ano, { onSettled: () => setConfirmando(false) })}>
              Encerrar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          As matrículas ativas de {ano} passam a encerradas. As rematrículas do ano seguinte não mudam. Isto não pode ser desfeito pela tela.
        </p>
      </Modal>
      {encerrar.isError && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {encerrar.error instanceof ApiError ? encerrar.error.message : 'Não foi possível encerrar o ano.'}
        </p>
      )}
    </div>
  )
}
