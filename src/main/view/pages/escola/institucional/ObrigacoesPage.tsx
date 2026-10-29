import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { CheckCircle2, FileText, Pencil, Plus, Trash2, Upload } from 'lucide-react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Badge, type CorBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useResumoPendencias } from '@/hooks/usePendencias'
import { abrirComprovante, useEditarObrigacoes, useObrigacoes, useResponsaveisObrigacao } from '@/hooks/useObrigacoes'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { FORMATOS_ACEITOS, TAMANHO_MAXIMO_ARQUIVO } from '@/models/documento'
import { ROTULO_ORGAO, ROTULO_SITUACAO_OBRIGACAO, type Obrigacao, type OrgaoExterno, type SituacaoObrigacao } from '@/models/obrigacao'

const COR: Record<SituacaoObrigacao, CorBadge> = { EM_DIA: 'verde', EM_ALERTA: 'amarelo', ATRASADA: 'vermelho', CUMPRIDA: 'cinza' }

/** Obrigações institucionais (RF09): prazo com aviso, comprovante e cumprimento, que renova as periódicas. */
export function ObrigacoesPage() {
  const obrigacoes = useObrigacoes()
  const podeEditar = usePermissao('OBRIGACOES', 'GRAVAR')
  const [editando, setEditando] = useState<Obrigacao | 'nova' | null>(null)

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo="Obrigações institucionais"
        descricao="Censo Escolar, SMTT, Sistema Presença e Cora. Anexe o comprovante e marque como cumprida; as periódicas já agendam o próximo prazo."
        acoes={
          podeEditar && (
            <Button onClick={() => setEditando('nova')}>
              <Plus className="size-4" aria-hidden /> Nova obrigação
            </Button>
          )
        }
      />
      {obrigacoes.isPending ? (
        <Carregando />
      ) : obrigacoes.isError ? (
        <EstadoErro erro={obrigacoes.error} aoTentarDeNovo={() => void obrigacoes.refetch()} />
      ) : obrigacoes.data.length === 0 ? (
        <EstadoVazio titulo="Nenhuma obrigação cadastrada" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {obrigacoes.data.map((o) => (
            <CartaoObrigacao key={o.id} obrigacao={o} podeEditar={podeEditar} aoEditar={() => setEditando(o)} />
          ))}
        </div>
      )}
      {editando && <ObrigacaoModal obrigacao={editando === 'nova' ? null : editando} aoFechar={() => setEditando(null)} />}
    </div>
  )
}

function CartaoObrigacao({ obrigacao: o, podeEditar, aoEditar }: { obrigacao: Obrigacao; podeEditar: boolean; aoEditar: () => void }) {
  const editar = useEditarObrigacoes()
  const arquivo = useRef<HTMLInputElement>(null)
  const [erroLocal, setErroLocal] = useState<string | null>(null)
  const vePendencias = usePermissao('PENDENCIAS', 'LER')
  const pendencias = useResumoPendencias(vePendencias && o.orgao === 'CENSO_INEP')
  const erro = [editar.enviar, editar.cumprir, editar.removerComprovante, editar.remover].find((m) => m.isError)?.error

  function escolher(lista: FileList | null) {
    const escolhido = lista?.[0]
    setErroLocal(null)
    if (!escolhido) return
    if (escolhido.size > TAMANHO_MAXIMO_ARQUIVO) {
      setErroLocal('O arquivo passa de 5 MB.')
      return
    }
    editar.enviar.mutate({ id: o.id, arquivo: escolhido })
    if (arquivo.current) arquivo.current.value = ''
  }

  const prazo =
    o.situacao === 'CUMPRIDA'
      ? 'Cumprida'
      : o.diasRestantes < 0
        ? `Atrasada há ${-o.diasRestantes} ${o.diasRestantes === -1 ? 'dia' : 'dias'}`
        : o.diasRestantes === 0
          ? 'Vence hoje'
          : `Faltam ${o.diasRestantes} ${o.diasRestantes === 1 ? 'dia' : 'dias'}`

  return (
    <article className={`flex flex-col rounded-xl border bg-white p-4 ${o.situacao === 'ATRASADA' ? 'border-red-200' : 'border-slate-200'}`}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <h2 className="font-semibold text-slate-900">{o.orgaoRotulo}</h2>
          <p className="text-sm text-slate-600">{o.descricao}</p>
        </div>
        <span className="flex items-center gap-1">
          <Badge cor={COR[o.situacao]}>{ROTULO_SITUACAO_OBRIGACAO[o.situacao]}</Badge>
          {podeEditar && (
            <button type="button" onClick={aoEditar} className="rounded p-1 text-slate-400 hover:text-semear-700" aria-label={`Editar ${o.orgaoRotulo}`}>
              <Pencil className="size-4" aria-hidden />
            </button>
          )}
        </span>
      </div>

      <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
        <dt className="text-slate-500">{o.situacao === 'CUMPRIDA' ? 'Vencimento' : 'Próximo prazo'}</dt>
        <dd className="text-slate-800">
          {formatarData(o.proximoVencimento)} <span className={o.diasRestantes < 0 && o.situacao !== 'CUMPRIDA' ? 'text-red-700' : 'text-slate-500'}>· {prazo}</span>
        </dd>
        <dt className="text-slate-500">Repetição</dt>
        <dd className="text-slate-800">{o.periodicidadeMeses ? (o.periodicidadeMeses === 12 ? 'Anual' : `A cada ${o.periodicidadeMeses} meses`) : 'Única'}</dd>
        <dt className="text-slate-500">Responsável</dt>
        <dd className="text-slate-800">{o.responsavel ?? '—'}</dd>
        <dt className="text-slate-500">Último cumprimento</dt>
        <dd className="text-slate-800">
          {o.ultimoCumprimento ? formatarData(o.ultimoCumprimento) : '—'}
          {o.ultimoCumprimentoPor && <span className="text-slate-500"> · {o.ultimoCumprimentoPor}</span>}
        </dd>
      </dl>

      {o.orgao === 'CENSO_INEP' && pendencias.data && pendencias.data.alunosComPendencia > 0 && (
        <Link to="/escola/pendencias" className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900 hover:bg-amber-100">
          {pendencias.data.alunosComPendencia} alunos com documentos pendentes — eles fazem falta no Censo. Ver pendências →
        </Link>
      )}

      <div className="mt-4 border-t border-slate-100 pt-3">
        <p className="mb-2 text-xs font-medium tracking-wide text-slate-500 uppercase">Comprovantes</p>
        {o.comprovantes.length === 0 ? (
          <p className="text-sm text-slate-500">Nenhum comprovante anexado.</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {o.comprovantes.map((c) => (
              <li key={c.id} className="flex items-center justify-between gap-2">
                <button type="button" onClick={() => void abrirComprovante(c.id)} className="inline-flex items-center gap-1 text-semear-700 hover:underline">
                  <FileText className="size-4" aria-hidden /> {c.extensao.toUpperCase()} de {formatarData(c.enviadoEm)}
                </button>
                <span className="flex items-center gap-1">
                  {!c.doCicloAtual && <Badge>Histórico</Badge>}
                  {podeEditar && c.doCicloAtual && (
                    <button
                      type="button"
                      onClick={() => editar.removerComprovante.mutate(c.id)}
                      className="rounded p-1 text-slate-400 hover:text-red-600"
                      aria-label="Remover comprovante"
                    >
                      <Trash2 className="size-4" aria-hidden />
                    </button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {podeEditar && o.situacao !== 'CUMPRIDA' && (
        <div className="mt-4 flex flex-wrap gap-2">
          <input ref={arquivo} type="file" accept={FORMATOS_ACEITOS} className="sr-only" onChange={(e) => escolher(e.target.files)} aria-label="Escolher comprovante" />
          <Button variante="secundario" carregando={editar.enviar.isPending} onClick={() => arquivo.current?.click()}>
            <Upload className="size-4" aria-hidden /> Anexar comprovante
          </Button>
          <Button disabled={!o.podeCumprir} carregando={editar.cumprir.isPending} onClick={() => editar.cumprir.mutate(o.id)}>
            <CheckCircle2 className="size-4" aria-hidden /> Marcar como cumprida
          </Button>
        </div>
      )}
      {podeEditar && o.situacao !== 'CUMPRIDA' && !o.podeCumprir && (
        <p className="mt-2 text-xs text-slate-500">Anexe o comprovante (PDF, JPG ou PNG até 5 MB) para marcar como cumprida.</p>
      )}
      {(erroLocal || erro) && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {erroLocal ?? (erro instanceof ApiError ? (erro.campos[0]?.mensagem ?? erro.message) : 'Não foi possível concluir.')}
        </p>
      )}
    </article>
  )
}

function ObrigacaoModal({ obrigacao, aoFechar }: { obrigacao: Obrigacao | null; aoFechar: () => void }) {
  const { salvar, remover } = useEditarObrigacoes()
  const responsaveis = useResponsaveisObrigacao()
  const [orgao, setOrgao] = useState<OrgaoExterno>(obrigacao?.orgao ?? 'CENSO_INEP')
  const [descricao, setDescricao] = useState(obrigacao?.descricao ?? '')
  const [periodicidade, setPeriodicidade] = useState(obrigacao ? (obrigacao.periodicidadeMeses ? String(obrigacao.periodicidadeMeses) : '') : '12')
  const [vencimento, setVencimento] = useState(obrigacao?.proximoVencimento ?? '')
  const [alerta, setAlerta] = useState(String(obrigacao?.diasAlerta ?? 30))
  const [responsavel, setResponsavel] = useState(obrigacao?.responsavelId ?? '')
  const erro = salvar.error ?? remover.error
  const podeApagar = obrigacao !== null && obrigacao.ultimoCumprimento === null && obrigacao.comprovantes.length === 0

  function enviar() {
    salvar.mutate(
      {
        id: obrigacao?.id,
        dados: {
          orgao,
          descricao,
          periodicidadeMeses: periodicidade === '' ? null : Number(periodicidade),
          proximoVencimento: vencimento,
          diasAlerta: Number(alerta),
          responsavelId: responsavel || null,
        },
      },
      { onSuccess: aoFechar },
    )
  }

  return (
    <Modal
      aberto
      titulo={obrigacao ? 'Editar obrigação' : 'Nova obrigação'}
      aoFechar={aoFechar}
      rodape={
        <>
          {podeApagar && (
            <Button variante="fantasma" className="mr-auto" carregando={remover.isPending} onClick={() => remover.mutate(obrigacao.id, { onSuccess: aoFechar })}>
              <Trash2 className="size-4" aria-hidden /> Apagar
            </Button>
          )}
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button disabled={!descricao.trim() || !vencimento} carregando={salvar.isPending} onClick={enviar}>
            Salvar
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Select rotulo="Órgão" value={orgao} onChange={(e) => setOrgao(e.target.value as OrgaoExterno)}>
          {(Object.keys(ROTULO_ORGAO) as OrgaoExterno[]).map((k) => (
            <option key={k} value={k}>
              {ROTULO_ORGAO[k]}
            </option>
          ))}
        </Select>
        <Input rotulo="Descrição" value={descricao} maxLength={255} onChange={(e) => setDescricao(e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <Input rotulo="Próximo prazo" type="date" value={vencimento} onChange={(e) => setVencimento(e.target.value)} />
          <Select rotulo="Repetição" value={periodicidade} onChange={(e) => setPeriodicidade(e.target.value)}>
            <option value="">Única</option>
            <option value="1">Mensal</option>
            <option value="2">A cada 2 meses</option>
            <option value="3">Trimestral</option>
            <option value="6">Semestral</option>
            <option value="12">Anual</option>
          </Select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input rotulo="Avisar com (dias)" type="number" min={0} max={365} value={alerta} onChange={(e) => setAlerta(e.target.value)} />
          <Select rotulo="Responsável" value={responsavel} onChange={(e) => setResponsavel(e.target.value)}>
            <option value="">Ninguém</option>
            {responsaveis.data?.map((f) => (
              <option key={f.id} value={f.id}>
                {f.nome}
              </option>
            ))}
          </Select>
        </div>
        {erro && (
          <p role="alert" className="text-sm text-red-600">
            {erro instanceof ApiError ? (erro.campos[0]?.mensagem ?? erro.message) : 'Não foi possível salvar.'}
          </p>
        )}
      </div>
    </Modal>
  )
}
