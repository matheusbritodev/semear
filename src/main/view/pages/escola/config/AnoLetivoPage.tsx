import { useState } from 'react'
import { Link } from 'react-router'
import { CalendarPlus, Copy, Pencil, Plus, Trash2 } from '@/components/ui/icones'
import { BarraOcupacao } from '@/components/comuns/BarraOcupacao'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente, useAnosLetivos, useTurmas } from '@/hooks/useAcademico'
import { CartaoSolicitacao } from '@/components/controle/CartaoSolicitacao'
import { useEditarAnoLetivo, useEditarPrecos, useEditarTurmas, usePrecos } from '@/hooks/useConfiguracao'
import { useAprovacoes } from '@/hooks/useControle'
import { useSeries } from '@/hooks/useFormalizacao'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import type { AnoLetivo } from '@/models/academico'
import type { PrecoSerie } from '@/models/configuracao'

function mensagem(erro: unknown, padrao: string) {
  return erro instanceof ApiError ? (erro.campos[0]?.mensagem ?? erro.message) : padrao
}

/** Configuração do ano letivo: período, tabela de preços por série e turmas (com capacidade de até 30). */
export function AnoLetivoPage() {
  const corrente = useAnoCorrente()
  const anos = useAnosLetivos()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const anoLetivo = anos.data?.find((a) => a.ano === ano)
  const podeConfigurar = usePermissao('CONFIGURACOES', 'GRAVAR')
  const [abrindo, setAbrindo] = useState(false)
  const proximo = (anos.data ?? []).reduce((m, a) => Math.max(m, a.ano), 0) + 1

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo="Ano letivo, preços e turmas"
        descricao="Prepare o ano: período, valores por série e turmas. Preço novo vale para cobranças e contratos novos."
        acoes={
          <>
            <SeletorAno ano={ano} aoMudar={setAno} />
            {podeConfigurar && anos.data && (
              <Button variante="secundario" onClick={() => setAbrindo(true)}>
                <CalendarPlus className="size-4" aria-hidden /> Abrir {proximo}
              </Button>
            )}
          </>
        }
      />
      {anos.isPending || ano === undefined ? (
        <Carregando />
      ) : anos.isError ? (
        <EstadoErro erro={anos.error} />
      ) : (
        <div className="space-y-6">
          {anoLetivo && <Periodo key={anoLetivo.ano} ano={anoLetivo} podeEditar={podeConfigurar} />}
          <Precos ano={ano} podeEditar={podeConfigurar} />
          <Turmas ano={ano} />
        </div>
      )}
      {abrindo && <AbrirAnoModal ano={proximo} aoFechar={() => setAbrindo(false)} aoCriar={(a) => (setAbrindo(false), setAno(a))} />}
    </div>
  )
}

function Periodo({ ano, podeEditar }: { ano: AnoLetivo; podeEditar: boolean }) {
  const { alterar } = useEditarAnoLetivo()
  const [inicio, setInicio] = useState(ano.dataInicio)
  const [fim, setFim] = useState(ano.dataFim)
  const mudou = inicio !== ano.dataInicio || fim !== ano.dataFim

  return (
    <Card titulo={`Período de ${ano.ano}`}>
      {podeEditar ? (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="sm:w-48">
            <Input rotulo="Início das aulas" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
          </div>
          <div className="sm:w-48">
            <Input rotulo="Fim das aulas" type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
          </div>
          <Button disabled={!mudou} carregando={alterar.isPending} onClick={() => alterar.mutate({ ano: ano.ano, inicio, fim })}>
            Salvar período
          </Button>
        </div>
      ) : (
        <p className="text-sm text-slate-700">
          De {formatarData(ano.dataInicio)} a {formatarData(ano.dataFim)}
        </p>
      )}
      {alterar.isError && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {mensagem(alterar.error, 'Não foi possível salvar o período.')}
        </p>
      )}
      <p className="mt-2 text-xs text-slate-500">Os feriados e recessos ficam no Calendário letivo (menu Pedagógico).</p>
    </Card>
  )
}

function Precos({ ano, podeEditar }: { ano: number; podeEditar: boolean }) {
  const precos = usePrecos(ano)
  const { copiar } = useEditarPrecos(ano)
  const pedidos = useAprovacoes('PENDENTE', podeEditar)
  const pendentes = (pedidos.data ?? []).filter(
    (p) => (p.tipo === 'ALTERACAO_PRECO' || p.tipo === 'COPIA_PRECOS') && p.resumo.includes(String(ano)),
  )
  const [editando, setEditando] = useState<PrecoSerie | null>(null)
  const [reajuste, setReajuste] = useState('5')
  const faltando = (precos.data ?? []).filter((p) => p.valorMensal === null).length

  return (
    <Card titulo={`Tabela de preços ${ano}`}>
      {precos.isPending ? (
        <Carregando />
      ) : precos.isError ? (
        <EstadoErro erro={precos.error} />
      ) : (
        <>
          {faltando > 0 && podeEditar && (
            <div className="mb-4 flex flex-col gap-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900 sm:flex-row sm:items-end">
              <p className="flex-1">
                {faltando} {faltando === 1 ? 'série está' : 'séries estão'} sem preço em {ano}. Sem preço, não há como matricular nem gerar
                cobranças.
              </p>
              <div className="w-28">
                <Input rotulo="Reajuste (%)" inputMode="decimal" value={reajuste} onChange={(e) => setReajuste(e.target.value)} />
              </div>
              <Button
                variante="secundario"
                carregando={copiar.isPending}
                onClick={() => copiar.mutate({ deAno: ano - 1, reajuste: Number(reajuste.replace(',', '.')) || 0 })}
              >
                <Copy className="size-4" aria-hidden /> Copiar de {ano - 1}
              </Button>
            </div>
          )}
          {copiar.isError && (
            <p role="alert" className="mb-3 text-sm text-red-600">
              {mensagem(copiar.error, 'Não foi possível copiar.')}
            </p>
          )}
          {pendentes.length > 0 && (
            <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50/60 px-3">
              <p className="pt-2 text-xs font-semibold tracking-wide text-amber-800 uppercase">Aguardando aprovação</p>
              <ul className="divide-y divide-amber-100">
                {pendentes.map((p) => (
                  <CartaoSolicitacao key={p.id} s={p} compacto />
                ))}
              </ul>
            </div>
          )}
          <ul className="divide-y divide-slate-100">
            {precos.data.map((p) => (
              <li key={p.serieId} className="flex flex-wrap items-center justify-between gap-2 py-2.5 text-sm">
                <span className="w-32 font-medium text-slate-800">{p.serie}</span>
                {p.valorMensal === null ? (
                  <Badge cor="amarelo">Sem preço</Badge>
                ) : (
                  <span className="flex-1 text-slate-600">
                    {p.nivel === 'EDUCACAO_INFANTIL' ? 'Taxa de Associado' : 'Mensalidade'}{' '}
                    <strong className="text-slate-900">{formatarMoeda(p.valorMensal)}</strong> · matrícula {formatarMoeda(p.valorMatricula ?? 0)} ·
                    vence dia {p.diaVencimento}
                  </span>
                )}
                {podeEditar && (
                  <Button variante="fantasma" onClick={() => setEditando(p)} aria-label={`Editar preço de ${p.serie}`}>
                    <Pencil className="size-4" aria-hidden /> {p.valorMensal === null ? 'Definir' : 'Editar'}
                  </Button>
                )}
              </li>
            ))}
          </ul>
          <p className="mt-2 text-xs text-slate-500">
            Toda mudança de preço vai para aprovação de outra pessoa (quem pede não aprova). Depois de aprovada, não altera cobranças já
            geradas nem contratos já assinados (o texto assinado fica guardado).
          </p>
        </>
      )}
      {editando && <PrecoModal ano={ano} preco={editando} aoFechar={() => setEditando(null)} />}
    </Card>
  )
}

function PrecoModal({ ano, preco, aoFechar }: { ano: number; preco: PrecoSerie; aoFechar: () => void }) {
  const { salvar } = useEditarPrecos(ano)
  const [mensal, setMensal] = useState(preco.valorMensal?.toString().replace('.', ',') ?? '')
  const [matricula, setMatricula] = useState(preco.valorMatricula?.toString().replace('.', ',') ?? '')
  const [dia, setDia] = useState(String(preco.diaVencimento ?? 10))
  const [prazo, setPrazo] = useState(String(preco.diasPrazoMatricula ?? 7))
  const numero = (t: string) => Number(t.replace(/\./g, '').replace(',', '.'))

  return (
    <Modal
      aberto
      titulo={`Preço · ${preco.serie} · ${ano}`}
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button
            disabled={!(numero(mensal) > 0) || !(numero(matricula) >= 0) || matricula === ''}
            carregando={salvar.isPending}
            onClick={() =>
              salvar.mutate(
                {
                  serieId: preco.serieId,
                  ano,
                  valorMensal: numero(mensal),
                  valorMatricula: numero(matricula),
                  diaVencimento: Number(dia),
                  diasPrazoMatricula: Number(prazo),
                },
                { onSuccess: aoFechar },
              )
            }
          >
            Enviar para aprovação
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Input
            rotulo={preco.nivel === 'EDUCACAO_INFANTIL' ? 'Taxa de Associado (R$)' : 'Mensalidade (R$)'}
            inputMode="decimal"
            value={mensal}
            onChange={(e) => setMensal(e.target.value)}
          />
          <Input rotulo="Matrícula (R$)" inputMode="decimal" value={matricula} onChange={(e) => setMatricula(e.target.value)} />
          <Input rotulo="Dia do vencimento (1 a 28)" type="number" min={1} max={28} value={dia} onChange={(e) => setDia(e.target.value)} />
          <Input rotulo="Prazo da matrícula (dias)" type="number" min={1} max={60} value={prazo} onChange={(e) => setPrazo(e.target.value)} />
        </div>
        <p className="text-xs text-slate-500">Matrícula R$ 0,00 = gratuita: a matrícula já nasce ativa.</p>
        {salvar.isError && (
          <p role="alert" className="text-sm text-red-600">
            {mensagem(salvar.error, 'Não foi possível salvar o preço.')}
          </p>
        )}
      </div>
    </Modal>
  )
}

function Turmas({ ano }: { ano: number }) {
  const turmas = useTurmas(ano)
  const series = useSeries()
  const { criar, remover } = useEditarTurmas()
  const podeEditar = usePermissao('TURMAS', 'GRAVAR')
  const [serieId, setSerieId] = useState('')
  const [nome, setNome] = useState('A')
  const [capacidade, setCapacidade] = useState('25')
  const erro = criar.error ?? remover.error

  return (
    <Card titulo={`Turmas de ${ano}`}>
      {turmas.isPending ? (
        <Carregando />
      ) : turmas.isError ? (
        <EstadoErro erro={turmas.error} />
      ) : turmas.data.length === 0 ? (
        <p className="text-sm text-slate-600">Nenhuma turma neste ano. Crie as turmas antes de abrir as matrículas.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {turmas.data.map((t) => (
            <li key={t.id} className="flex flex-wrap items-center gap-3 py-2.5 text-sm">
              <Link to={`/escola/turmas/${t.id}`} className="w-32 font-medium text-slate-900 hover:text-semear-700 hover:underline">
                {t.nomeCompleto}
              </Link>
              <div className="min-w-40 flex-1">
                <BarraOcupacao ocupadas={t.ocupadas} capacidade={t.capacidadeMax} />
              </div>
              {podeEditar && t.ocupadas === 0 && (
                <button
                  type="button"
                  onClick={() => remover.mutate(t.id)}
                  className="rounded p-1 text-slate-400 hover:text-red-600"
                  aria-label={`Apagar ${t.nomeCompleto}`}
                >
                  <Trash2 className="size-4" aria-hidden />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {podeEditar && (
        <div className="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:items-end">
          <div className="sm:w-44">
            <Select rotulo="Série" value={serieId} onChange={(e) => setSerieId(e.target.value)}>
              <option value="">Escolha…</option>
              {series.data?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nome}
                </option>
              ))}
            </Select>
          </div>
          <div className="sm:w-24">
            <Input rotulo="Turma" value={nome} maxLength={20} onChange={(e) => setNome(e.target.value)} />
          </div>
          <div className="sm:w-28">
            <Input rotulo="Vagas (até 30)" type="number" min={1} max={30} value={capacidade} onChange={(e) => setCapacidade(e.target.value)} />
          </div>
          <Button
            disabled={!serieId || !nome.trim()}
            carregando={criar.isPending}
            onClick={() => criar.mutate({ ano, serieId, nome, capacidade: Number(capacidade) })}
          >
            <Plus className="size-4" aria-hidden /> Criar turma
          </Button>
        </div>
      )}
      {erro && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {mensagem(erro, 'Não foi possível concluir.')}
        </p>
      )}
      <p className="mt-2 text-xs text-slate-500">Os professores de cada turma são definidos na página da turma.</p>
    </Card>
  )
}

function AbrirAnoModal({ ano, aoFechar, aoCriar }: { ano: number; aoFechar: () => void; aoCriar: (ano: number) => void }) {
  const { criar } = useEditarAnoLetivo()
  const [inicio, setInicio] = useState(`${ano}-02-01`)
  const [fim, setFim] = useState(`${ano}-12-10`)
  return (
    <Modal
      aberto
      titulo={`Abrir o ano letivo ${ano}`}
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button carregando={criar.isPending} onClick={() => criar.mutate({ ano, inicio, fim }, { onSuccess: () => aoCriar(ano) })}>
            Abrir {ano}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Input rotulo="Início das aulas" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
          <Input rotulo="Fim das aulas" type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
        </div>
        <p className="text-xs text-slate-500">Depois: defina os preços (dá para copiar do ano anterior com reajuste) e crie as turmas.</p>
        {criar.isError && (
          <p role="alert" className="text-sm text-red-600">
            {mensagem(criar.error, 'Não foi possível abrir o ano.')}
          </p>
        )}
      </div>
    </Modal>
  )
}
